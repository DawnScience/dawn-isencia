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

package com.isencia.passerelle.actor.flow;

import java.util.ArrayList;
import java.util.List;
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
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.core.PortHandler;
import com.isencia.passerelle.core.PortListenerAdapter;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageHelper;

/**
 * DOCUMENT ME!
 * 
 * @version $Id: Switch.java,v 1.7 2006/02/15 16:34:48 erwin Exp $
 * @author Dirk Jacobs
 */
public class Switch extends Actor {
  // ~ Static variables/initializers
  // __________________________________________________________________________________________________________________________

  private static Logger logger = LoggerFactory.getLogger(Switch.class);

  // ~ Instance variables
  // _____________________________________________________________________________________________________________________________________

  private List<Port> outputPorts = null;
  private PortHandler selectHandler = null;

  // /////////////////////////////////////////////////////////////////
  // // ports and parameters ////
  public Parameter numberOfOutputs = null;
  public Port input;
  public Port select = null;

  // /////////////////////////////////////////////////////////////////
  // // variables ////
  private int outputCount = 0;
  private int selected = 0;

  // ~ Constructors
  // ___________________________________________________________________________________________________________________________________________

  public Switch(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);

    numberOfOutputs = new Parameter(this, "count", new IntToken(1));
    numberOfOutputs.setTypeEquals(BaseType.INT);

    input = PortFactory.getInstance().createInputPort(this, null);
    input.setMultiport(false);
    select = PortFactory.getInstance().createInputPort(this, "select", Integer.class);
    input.setMultiport(false);

    _attachText("_iconDescription", "<svg>\n" + "<rect x=\"-20\" y=\"-20\" width=\"40\" " + "height=\"40\" style=\"fill:lightgrey;stroke:lightgrey\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"19\" y2=\"-19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"-19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"20\" y1=\"-19\" x2=\"20\" y2=\"20\" " + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"-19\" y1=\"20\" x2=\"20\" y2=\"20\" "
        + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"19\" y1=\"-18\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n"
        + "<line x1=\"-18\" y1=\"19\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n" + "<circle cx=\"-2\" cy=\"-7\" r=\"4\""
        + "style=\"fill:black\"/>\n" + "<line x1=\"-15\" y1=\"-5\" x2=\"15\" y2=\"-5\" " + "style=\"stroke-width:2.0\"/>\n"
        + "<line x1=\"0\" y1=\"-5\" x2=\"15\" y2=\"-15\" " + "style=\"stroke-width:2.0\"/>\n" + "<line x1=\"0\" y1=\"-5\" x2=\"15\" y2=\"5\" "
        + "style=\"stroke-width:2.0\"/>\n" + "<line x1=\"-15\" y1=\"10\" x2=\"0\" y2=\"10\" " + "style=\"stroke-width:1.0;stroke:gray\"/>\n"
        + "<line x1=\"0\" y1=\"10\" x2=\"0\" y2=\"-5\" " + "style=\"stroke-width:1.0;stroke:gray\"/>\n" + "</svg>\n");
  }

  // ~ Methods
  // ________________________________________________________________________________________________________________________________________________

  /**
   * DOCUMENT ME!
   * 
   * @param attribute DOCUMENT ME!
   * @throws IllegalActionException DOCUMENT ME!
   */
  public void attributeChanged(Attribute attribute) throws IllegalActionException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " :" + attribute);
    }

    if (attribute == numberOfOutputs) {
      int newOutputCount = ((IntToken) numberOfOutputs.getToken()).intValue();
      logger.debug("change number of outputs from :  " + outputCount + " to : " + newOutputCount);

      if (outputPorts == null) {
        logger.debug("Create a new list");
        outputPorts = new ArrayList(5);

        for (int i = 0; i < newOutputCount; i++) {
          try {
            Port outputPort = (Port) getPort("output " + i);

            if (outputPort == null) {
              outputPort = PortFactory.getInstance().createOutputPort(this, "output " + i);
            }

            outputPorts.add(i, outputPort);
            logger.debug("created output : " + i);
          } catch (NameDuplicationException e) {
            throw new IllegalActionException(e.toString());
          }
        }
      } else if (newOutputCount < outputCount) {
        logger.debug("Decrement number of outputs");

        for (int i = outputCount - 1; (i >= 0) && (i >= newOutputCount); i--) {
          try {
            ((Port) outputPorts.get(i)).setContainer(null);
            outputPorts.remove(i);
            logger.debug("removed output : " + i);
          } catch (NameDuplicationException e) {
            throw new IllegalActionException(e.toString());
          }
        }
      } else if (newOutputCount > outputCount) {
        logger.debug("Increment number of outputs");

        for (int i = outputCount; i < newOutputCount; i++) {
          try {
            Port outputPort = (Port) getPort("output " + i);

            if (outputPort == null) {
              outputPort = PortFactory.getInstance().createOutputPort(this, "output " + i);
            }

            outputPorts.add(i, outputPort);
            logger.debug("created output : " + i);
          } catch (NameDuplicationException e) {
            throw new IllegalActionException(e.toString());
          }
        }
      }

      outputCount = newOutputCount;

      if (selected >= outputCount) {
        selected = outputCount - 1;
      }
    } else {
      super.attributeChanged(attribute);
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }
  }

  protected void doFire() throws ProcessingException {
    int outNr = 0;

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo());
    }

    Token token = null;

    try {
      token = MessageHelper.getMessageAsToken(input);
    } catch (PasserelleException e) {
      throw new ProcessingException(getInfo() + " - doFire() generated exception in MessageHelper.getMessageAsToken() " + e, token, e);
    }

    if (token == null) {
      requestFinish();
    } else {
      outNr = selected;

      if (selected < 0) {
        outNr = 0;
        logger.debug(getInfo() + " : Selected port = " + selected + ". Using port " + outNr + ".");
      } else if (selected >= outputCount) {
        outNr = outputCount - 1;
        logger.debug(getInfo() + " : Selected port = " + selected + ". Using port " + outNr + ".");
      }

      try {
        ((Port) outputPorts.get(outNr)).broadcast(token);
      } catch (Exception e) {
        throw new ProcessingException(getInfo() + " - doFire() generated exception in outputPorts...broadcast() " + e, token, e);
      }
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit " + " - Output " + outNr + " has sent message " + token);
    }
  }

  protected void doInitialize() throws InitializationException {

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo());
    }

    super.doInitialize();

    if (select.getWidth() > 0) {
      selectHandler = new PortHandler(select, new PortListenerAdapter() {
        public void tokenReceived() {
          Token selectToken = selectHandler.getToken();

          try {
            ManagedMessage msg = MessageHelper.getMessageFromToken(selectToken);
            selected = ((Number) msg.getBodyContent()).intValue();
          } catch (NumberFormatException e) {
            // Do nothing. selected is unchanged
          } catch (Exception e) {
            // Do nothing. selected is unchanged
            logger.error("", e);
          }

          logger.debug("Event received : " + selected);
        }
      });
      selectHandler.start();
    }
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }

  }

  protected String getExtendedInfo() {
    return outputCount + " output ports";
  }
}