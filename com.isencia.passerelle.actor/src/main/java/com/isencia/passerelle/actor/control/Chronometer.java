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

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.Actor;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * A Chronometer with 2 blocking ports: set and reset. The actor will always
 * first wait for a message on the set port. Then it will look for a message on
 * the reset port. The time interval (in ms) between the two is sent out in the
 * body of the outgoing message. If a reset message has already arrived before a
 * set message, the reset will be immediately noticed after the set, and the
 * resulting time interval will be approximately 0. If another set message
 * arrives before the reset, it means that immediately after consuming the
 * reset, in the next fire() iteration, the actor will be set again. So, the
 * Chronometer does not drop any messages and it will almost continuously be
 * blocked on one of the 2 input ports.
 * 
 * @author erwin
 */
public class Chronometer extends Actor {

  private static Logger logger = LoggerFactory.getLogger(Chronometer.class);

  public Chronometer(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);

    setInputPort = PortFactory.getInstance().createInputPort(this, "set", null);

    resetInputPort = PortFactory.getInstance().createInputPort(this, "reset", null);

    output = PortFactory.getInstance().createOutputPort(this, "resetOk");

    _attachText("_iconDescription", "<svg>\n" + "<rect x=\"-20\" y=\"-20\" width=\"40\" " + "height=\"40\" style=\"fill:lightgrey;stroke:lightgrey\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"19\" y2=\"-19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"-19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"20\" y1=\"-19\" x2=\"20\" y2=\"20\" " + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"-19\" y1=\"20\" x2=\"20\" y2=\"20\" "
        + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"19\" y1=\"-18\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n"
        + "<line x1=\"-18\" y1=\"19\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n"

        + "<circle cx=\"-8\" cy=\"-8\" r=\"16\"" + "style=\"fill:white\"/>\n" + "<line x1=\"0\" y1=\"-14\" x2=\"0\" y2=\"-12\"/>\n"
        + "<line x1=\"0\" y1=\"12\" x2=\"0\" y2=\"14\"/>\n" + "<line x1=\"-14\" y1=\"0\" x2=\"-12\" y2=\"0\"/>\n"
        + "<line x1=\"12\" y1=\"0\" x2=\"14\" y2=\"0\"/>\n" + "<line x1=\"0\" y1=\"-7\" x2=\"0\" y2=\"0\"/>\n"
        + "<line x1=\"0\" y1=\"0\" x2=\"11.26\" y2=\"-6.5\"/>\n" + "</svg>\n");
  }

  // /////////////////////////////////////////////////////////////////
  // // ports and parameters ////

  public Port setInputPort = null;
  public Port resetInputPort = null;
  public Port output = null;

  // /////////////////////////////////////////////////////////////////
  // // variables ////

  public void doFire() throws ProcessingException {
    if (logger.isTraceEnabled()) logger.trace(getInfo() + " doFire() - entry");

    ManagedMessage message = null;
    long setTime = 0;
    long resetTime = 0;
    try {
      setInputPort.get(0);
      setTime = new Date().getTime();
      resetInputPort.get(0);
      resetTime = new Date().getTime();

      message = createMessage(Double.toString(resetTime - setTime), "text/plain");
    } catch (Exception e) {
      throw new ProcessingException(getInfo() + " - doFire() generated exception " + e, this, e);
    }

    try {
      sendOutputMsg(output, message);
    } catch (IllegalArgumentException e) {
      throw new ProcessingException(getInfo() + " - doFire() generated exception " + e, message, e);
    }

    if (logger.isTraceEnabled()) logger.trace(getInfo() + " doFire() - exit");
  }

  protected String getAuditTrailMessage(ManagedMessage message, Port port) throws Exception {
    return "sent chronometer message with time " + message.getBodyContentAsString();
  }

  protected String getExtendedInfo() {
    return "";
  }

}