/* Copyright 2011 - iSencia Belgium NV

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package com.isencia.passerelle.actor.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.IntToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.Actor;
import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.core.PortHandler;
import com.isencia.passerelle.core.PortListenerAdapter;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * @author dirk jacobs
 */
public class Timer extends Actor {

  private static Logger logger = LoggerFactory.getLogger(Timer.class);

  private boolean setPresent = false;
  private boolean set = false;
  private boolean reset = false;

  public Timer(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);

    timeParameter = new Parameter(this, "time", new IntToken(10));
    timeParameter.setTypeEquals(BaseType.INT);
    registerConfigurableParameter(timeParameter);

    setInputPort = PortFactory.getInstance().createInputPort(this, "set", null);

    resetInputPort = PortFactory.getInstance().createInputPort(this, "reset", null);

    outputPort = PortFactory.getInstance().createOutputPort(this, "output");

    _attachText("_iconDescription", "<svg>\n" + "<rect x=\"-20\" y=\"-20\" width=\"40\" " + "height=\"40\" style=\"fill:lightgrey;stroke:lightgrey\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"19\" y2=\"-19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"-19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"20\" y1=\"-19\" x2=\"20\" y2=\"20\" " + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"-19\" y1=\"20\" x2=\"20\" y2=\"20\" "
        + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"19\" y1=\"-18\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n"
        + "<line x1=\"-18\" y1=\"19\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n"

        + "<circle cx=\"0\" cy=\"0\" r=\"16\"" + "style=\"fill:white\"/>\n" + "<line x1=\"0\" y1=\"-14\" x2=\"0\" y2=\"-12\"/>\n"
        + "<line x1=\"0\" y1=\"12\" x2=\"0\" y2=\"14\"/>\n" + "<line x1=\"-14\" y1=\"0\" x2=\"-12\" y2=\"0\"/>\n"
        + "<line x1=\"12\" y1=\"0\" x2=\"14\" y2=\"0\"/>\n" + "<line x1=\"0\" y1=\"-7\" x2=\"0\" y2=\"0\"/>\n"
        + "<line x1=\"0\" y1=\"0\" x2=\"11.26\" y2=\"-6.5\"/>\n" + "</svg>\n");
  }

  // /////////////////////////////////////////////////////////////////
  // // ports and parameters ////

  private Parameter timeParameter = null;
  private Port setInputPort = null;
  private Port resetInputPort = null;
  private Port outputPort = null;

  // /////////////////////////////////////////////////////////////////
  // // variables ////

  private int time = 0;
  private PortHandler setHandler = null;
  private PortHandler resetHandler = null;

  public void attributeChanged(Attribute attribute) throws IllegalActionException {
    if (logger.isTraceEnabled()) logger.trace(getInfo() + " :" + attribute);

    if (attribute == timeParameter) {
      time = ((IntToken) timeParameter.getToken()).intValue() * 1000;
    } else {
      super.attributeChanged(attribute);
    }

    if (logger.isTraceEnabled()) logger.trace(getInfo() + " - exit ");
  }

  protected void doInitialize() throws InitializationException {
    if (logger.isTraceEnabled()) logger.trace(getInfo());

    super.doInitialize();

    // If something connected to the set port, install a handler
    if (setInputPort.getWidth() > 0) {

      setHandler = new PortHandler(setInputPort, new PortListenerAdapter() {
        public void tokenReceived() {
          if (logger.isDebugEnabled()) logger.debug(getInfo() + " - Set Event received");

          Token token = setHandler.getToken();
          if (token != null) {
            set = true;
            performNotify();
          }
        }
      });
      if (setHandler != null) {
        setPresent = true;
        setHandler.start();
      }
    }
    // If something connected to the reset port, install a handler
    if (resetInputPort.getWidth() > 0) {
      resetHandler = new PortHandler(resetInputPort, new PortListenerAdapter() {
        public void tokenReceived() {
          Token token = resetHandler.getToken();
          if (logger.isDebugEnabled()) logger.debug(getInfo() + " - Reset Event received");

          if (token != null) {
            reset = true;
            performNotify();
          }
        }
      });
      if (resetHandler != null) {
        resetHandler.start();
      }
    }

  }

  private synchronized void performNotify() {
    if (logger.isTraceEnabled()) logger.trace(getInfo());
    notify();
    if (logger.isTraceEnabled()) logger.trace(getInfo() + " - exit ");
  }

  private synchronized void performWait(int time) {
    if (logger.isTraceEnabled()) logger.trace(getInfo() + " :" + time);
    try {
      if (time == -1)
        wait();
      else
        wait(time);
    } catch (InterruptedException e) {
      requestFinish();
    }
    if (logger.isTraceEnabled()) logger.trace(getInfo() + " - exit ");
  }

  protected void doFire() throws ProcessingException {
    if (logger.isTraceEnabled()) logger.trace(getInfo());

    if (!setPresent) {
      performWait(time);
      if (reset || isFinishRequested()) {
        reset = false;
        return;
      }

    } else {
      // Wait until set
      isFiring = false;
      while (!set && !isFinishRequested()) {
        performWait(-1);
      }

      if (isFinishRequested()) return;

      isFiring = true;
      set = false;
      performWait(time);
      if (reset || isFinishRequested()) {
        reset = false;
        return;
      }

    }

    // Send trigger message
    ManagedMessage message = createTriggerMessage();
    try {
      message.setBodyContent(Long.toString(time), "text/plain");
    } catch (Exception e) {
      throw new ProcessingException(getInfo() + " - doFire() generated exception " + e, message, e);
    }
    try {
      sendOutputMsg(outputPort, message);
    } catch (IllegalArgumentException e) {
      throw new ProcessingException(getInfo() + " - doFire() generated exception " + e, message, e);
    }

    if (logger.isTraceEnabled()) logger.trace(getInfo() + " - exit ");
  }

  protected String getAuditTrailMessage(ManagedMessage message, Port port) {
    return "generated timed trigger.";
  }

  protected String getExtendedInfo() {
    return "period: " + time + " s";
  }

  protected void doStopFire() {
    performNotify();
  }

}