package com.isencia.passerelle.actor.error;

import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.core.PortMode;
import com.isencia.passerelle.core.PasserelleException.Severity;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.util.ExecutionTracerService;

/**
 * Generate a configurable exception, thrown during the actor's process() iteration,
 * i.e. each time when a message is received on the input port.
 * <br/>
 * The received message is set as the exception's error context.
 * 
 * @author erwin
 *
 */
@SuppressWarnings("serial")
public class ErrorGenerator extends Actor {

  public Parameter severityParam;
  public String severity = Severity.FATAL.toString();

  public Port input;
  public Port output;

  public Parameter messageParam;
  public String message;

  public Parameter exceptionClassParam;
  public Throwable exception;

  public ErrorGenerator(final CompositeEntity container, final String name) throws NameDuplicationException, IllegalActionException {
    super(container, name);

    exceptionClassParam = new StringParameter(this, "Throwable class");
    exceptionClassParam.setExpression("java.lang.NullPointerException");

    messageParam = new StringParameter(this, "message");
    messageParam.setExpression("An error occured");

    severityParam = new StringParameter(this, "severity");
    severityParam.setExpression(severity);
    severityParam.addChoice(Severity.FATAL.toString());
    severityParam.addChoice(Severity.NON_FATAL.toString());

    input = PortFactory.getInstance().createInputPort(this, "in", null);
    input.setMode(PortMode.PUSH);
    output = PortFactory.getInstance().createOutputPort(this, "out");
  }

  @Override
  protected void process(final ActorContext ctxt, final ProcessRequest request, final ProcessResponse response) throws ProcessingException {

    ManagedMessage receivedMsg = request.getMessage(input);

    ExecutionTracerService.trace(this, message);

    Severity s = Severity.NON_FATAL;
    if (Severity.FATAL.toString().equals(severity)) {
      s = Severity.FATAL;
    }
    throw new ProcessingException(s, message, receivedMsg, exception);

  }

  @Override
  public void attributeChanged(final Attribute attribute) throws IllegalActionException {
    if (attribute == messageParam) {
      message = ((StringToken) messageParam.getToken()).stringValue();
    } else if (attribute == severityParam) {
      severity = ((StringToken) severityParam.getToken()).stringValue();
    } else if (attribute == exceptionClassParam) {
      Class throwableClass = NullPointerException.class;
      String className = exceptionClassParam.getExpression();
      exception = null;
      try {
        throwableClass = Class.forName(className);
        exception = (Throwable) throwableClass.newInstance();
      } catch (Exception e) {
        getLogger().warn("Error constructing exception instance for :" + className + " -- using NPE as default", e);
        exception = new NullPointerException();
      }
    } else {
      super.attributeChanged(attribute);
    }
  }

  @Override
  protected String getExtendedInfo() {
    return message;
  }

}