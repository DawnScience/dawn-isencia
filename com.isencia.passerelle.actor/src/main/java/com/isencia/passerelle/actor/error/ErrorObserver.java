/**
 * 
 */
package com.isencia.passerelle.actor.error;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.domain.cap.Director;
import com.isencia.passerelle.ext.ErrorCollector;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;

/**
 * Registers itself as an ErrorCollector.
 * <p>
 * For each received exception :
 * <ul>
 * <li>log the exception
 * <li>send out the exception msg via errorText output
 * <li>if the error context is a ManagedMessage, send it out via the
 * messageInError output
 * </ul>
 * </p>
 * 
 * @author delerw
 */
public class ErrorObserver extends Actor implements ErrorCollector {

  private final static Logger logger = LoggerFactory.getLogger(ErrorObserver.class);

  private BlockingQueue<PasserelleException> errors = new LinkedBlockingQueue<PasserelleException>();

  /**
   * For each received exception/error, the error is sent out in a new Passerelle ManagedMessage
   * via this output port.
   */
  public Port errorOutput;
  
  /**
   * For each received exception/error, the error text message is sent out via
   * this output port.
   */
  public Port errorTextOutput;

  /**
   * For each received exception/error, if the error context is a
   * ManagedMessage, send it out via the messageInError output
   */
  public Port messageInErrorOutput;

  public ErrorObserver(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);
    errorOutput = PortFactory.getInstance().createOutputPort(this, "errorMsg");
    errorTextOutput = PortFactory.getInstance().createOutputPort(this, "errorText");
    messageInErrorOutput = PortFactory.getInstance().createOutputPort(this, "messageInError");

    _attachText("_iconDescription", "<svg>\n" + "<rect x=\"-20\" y=\"-20\" width=\"40\" height=\"40\" style=\"fill:red;stroke:red\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"19\" y2=\"-19\" style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"-19\" y2=\"19\" style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"20\" y1=\"-19\" x2=\"20\" y2=\"20\" style=\"stroke-width:1.0;stroke:black\"/>\n"
        + "<line x1=\"-19\" y1=\"20\" x2=\"20\" y2=\"20\" style=\"stroke-width:1.0;stroke:black\"/>\n"
        + "<line x1=\"19\" y1=\"-18\" x2=\"19\" y2=\"19\" style=\"stroke-width:1.0;stroke:grey\"/>\n"
        + "<line x1=\"-18\" y1=\"19\" x2=\"19\" y2=\"19\" style=\"stroke-width:1.0;stroke:grey\"/>\n"
        + "<circle cx=\"0\" cy=\"0\" r=\"10\" style=\"fill:white;stroke-width:2.0\"/>\n"
        + "<line x1=\"0\" y1=\"-15\" x2=\"0\" y2=\"0\" style=\"stroke-width:2.0\"/>\n"
        + "<line x1=\"-3\" y1=\"-3\" x2=\"0\" y2=\"0\" style=\"stroke-width:2.0\"/>\n"
        + "<line x1=\"3\" y1=\"-3\" x2=\"0\" y2=\"0\" style=\"stroke-width:2.0\"/>\n" + "</svg>\n");

  }

  @Override
  protected void doInitialize() throws InitializationException {
    super.doInitialize();

    try {
      ((Director) getDirector()).addErrorCollector(this);
    } catch (ClassCastException e) {
      // means the actor is used without a Passerelle Director
      // just log this. Only consequence is that we'll never receive
      // any error messages via acceptError
      getLogger().info(getInfo() + " - used without Passerelle Director!!");
    } catch (Exception e) {
      getLogger().error("Unexpected error while trying to register as error collector", e);
    }
  }

  @Override
  protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
    // ErrorReceiver has no data input ports,
    // so it's like a Source in the days of the original Actor API.
    // The BlockingQueue (errors) is our data feed.
    try {
      PasserelleException e = errors.poll(1, TimeUnit.SECONDS);
      if (e != null) {
        sendOutErrorInfo(response, e);
        drainErrorsQueueTo(response);
      }
    } catch (InterruptedException e) {
      // should not happen,
      // or if it does only when terminating the model execution
      // and with an empty queue, so we can just finish then
      requestFinish();
    }
  }

  private void sendOutErrorInfo(ProcessResponse response, PasserelleException e) throws ProcessingException {
    ManagedMessage errorMsg = createErrorMessage(e);
    response.addOutputMessage(errorOutput, errorMsg);

    if (e.getContext() instanceof ManagedMessage) {
      ManagedMessage msg = (ManagedMessage) e.getContext();
      response.addOutputMessage(messageInErrorOutput, msg);
    }
    try {
      response.addOutputMessage(errorTextOutput, createMessage(e.getMessage(), "text/plain"));
    } catch (MessageException e1) {
      // should not happen, but...
      throw new ProcessingException("Error generating error text output", e.getContext(), e1);
    }
  }

  private void drainErrorsQueueTo(ProcessResponse response) throws ProcessingException {
    while (!errors.isEmpty()) {
      PasserelleException e = errors.poll();
      if (e != null) {
        sendOutErrorInfo(response, e);
      } else {
        break;
      }
    }
  }

  public void acceptError(PasserelleException e) {
    try {
      errors.put(e);
      getLogger().error("Error reported ", e);
    } catch (InterruptedException e1) {
      // should not happen,
      // or if it does only when terminating the model execution
      getLogger().error("Receipt interrupted for ", e);
    }
  }

  @Override
  protected void doWrapUp() throws TerminationException {
    try {
      ((Director) getDirector()).removeErrorCollector(this);
    } catch (ClassCastException e) {
    }
    try {
      drainErrorsQueueTo(null);
    } catch (Exception e) {
      throw new TerminationException(getInfo() + " - doWrapUp() generated exception " + e, errors, e);
    }

    super.doWrapUp();
  }

  @Override
  protected Logger getLogger() {
    return logger;
  }
}
