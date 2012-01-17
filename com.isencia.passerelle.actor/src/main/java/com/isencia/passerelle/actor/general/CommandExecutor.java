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

package com.isencia.passerelle.actor.general;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.StringToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
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
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.message.MessageHelper;

/**
 * Executes a configurable shell command when receiving a trigger message.
 * 
 * @author erwin
 */
public class CommandExecutor extends Actor {
  // ~ Static variables/initializers
  // __________________________________________________________________________________________________________________________

  private static Logger logger = LoggerFactory.getLogger(CommandExecutor.class);
  public static final String COMMAND_HEADER = "Command";
  public static final String TRIGGER_PORT = "trigger";
  public static final String COMMAND_PARAMETER = "command";
  public static final String PARAMETERS_PARAMETER = "params";

  // ~ Instance variables
  // _____________________________________________________________________________________________________________________________________

  public Parameter commandParameter;
  public Parameter paramsParameter;
  public Port trigger = null;
  private PortHandler triggerHandler = null;
  private String defaultSourcePath = null;
  private boolean triggerConnected = false;

  // ~ Constructors
  // ___________________________________________________________________________________________________________________________________________

  public CommandExecutor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);

    commandParameter = new StringParameter(this, COMMAND_PARAMETER);
    commandParameter.setExpression("");
    registerConfigurableParameter(commandParameter);
    paramsParameter = new StringParameter(this, PARAMETERS_PARAMETER);
    paramsParameter.setExpression("");
    registerConfigurableParameter(paramsParameter);

    trigger = PortFactory.getInstance().createInputPort(this, TRIGGER_PORT, null);

    _attachText("_iconDescription", "<svg>\n"
        + "<rect x=\"-20\" y=\"-20\" width=\"40\" "
        + "height=\"40\" style=\"fill:lightgrey;stroke:lightgrey\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"19\" y2=\"-19\" "
        + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"-19\" y2=\"19\" "
        + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"20\" y1=\"-19\" x2=\"20\" y2=\"20\" "
        + "style=\"stroke-width:1.0;stroke:black\"/>\n"
        + "<line x1=\"-19\" y1=\"20\" x2=\"20\" y2=\"20\" "
        + "style=\"stroke-width:1.0;stroke:black\"/>\n"
        + "<line x1=\"19\" y1=\"-18\" x2=\"19\" y2=\"19\" "
        + "style=\"stroke-width:1.0;stroke:grey\"/>\n"
        + "<line x1=\"-18\" y1=\"19\" x2=\"19\" y2=\"19\" "
        + "style=\"stroke-width:1.0;stroke:grey\"/>\n"
        +

        // body
        "<line x1=\"-9\" y1=\"-16\" x2=\"-12\" y2=\"-8\" "
        + "style=\"stroke-width:2.0\"/>\n"
        +
        // backwards leg
        "<line x1=\"-11\" y1=\"-7\" x2=\"-16\" y2=\"-7\" "
        + "style=\"stroke-width:1.0\"/>\n"
        + "<line x1=\"-13\" y1=\"-8\" x2=\"-15\" y2=\"-8\" "
        + "style=\"stroke-width:1.0;stroke:grey\"/>\n"
        + "<line x1=\"-16\" y1=\"-7\" x2=\"-16\" y2=\"-5\" "
        + "style=\"stroke-width:1.0\"/>\n"
        +

        // forward leg
        "<line x1=\"-11\" y1=\"-11\" x2=\"-8\" y2=\"-8\" "
        + "style=\"stroke-width:1.5\"/>\n"
        + "<line x1=\"-8\" y1=\"-8\" x2=\"-8\" y2=\"-6\" "
        + "style=\"stroke-width:1.0\"/>\n"
        + "<line x1=\"-8\" y1=\"-5\" x2=\"-6\" y2=\"-5\" "
        + "style=\"stroke-width:1.0\"/>\n"
        +

        // forward arm
        "<line x1=\"-10\" y1=\"-14\" x2=\"-7\" y2=\"-11\" "
        + "style=\"stroke-width:1.0\"/>\n"
        + "<line x1=\"-7\" y1=\"-11\" x2=\"-5\" y2=\"-14\" "
        + "style=\"stroke-width:1.0\"/>\n"
        +
        // backward arm
        "<line x1=\"-11\" y1=\"-14\" x2=\"-14\" y2=\"-14\" "
        + "style=\"stroke-width:1.0\"/>\n"
        + "<line x1=\"-14\" y1=\"-14\" x2=\"-12\" y2=\"-11\" "
        + "style=\"stroke-width:1.0\"/>\n"
        +
        // cmd field
        "<rect x=\"-15\" y=\"-3\" width=\"28\" " + "height=\"12\" style=\"fill:white;stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"-14\" y1=\"-2\" x2=\"13\" y2=\"-2\" " + "style=\"stroke-width:1.5;stroke:grey\"/>\n" + "<line x1=\"-14\" y1=\"-2\" x2=\"-14\" y2=\"10\" "
        + "style=\"stroke-width:1.5;stroke:grey\"/>\n" + "<line x1=\"15\" y1=\"-2\" x2=\"15\" y2=\"11\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"-15\" y1=\"11\" x2=\"15\" y2=\"11\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<text x=\"-15\" y=\"5\" style=\"font-size:8\"> cmd </text>\n" + "</svg>\n");
  }

  /*
   * (non-Javadoc)
   * @see
   * ptolemy.kernel.util.NamedObj#attributeChanged(ptolemy.kernel.util.Attribute
   * )
   */
  public void attributeChanged(Attribute attribute) throws IllegalActionException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " :" + attribute);
    }

    if ((attribute == commandParameter) || (attribute == paramsParameter)) {
      StringToken commandToken = (StringToken) commandParameter.getToken();
      StringToken paramsToken = (StringToken) paramsParameter.getToken();

      if ((commandToken != null) && (commandToken.stringValue().length() > 0)) {
        String cmd = commandToken.stringValue();
        defaultSourcePath = cmd + " " + paramsToken.stringValue();
      }
    } else {
      super.attributeChanged(attribute);
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }
  }

  protected void doFire() throws ProcessingException {
    ManagedMessage msg = null;
    String[] sourcePath = null;

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo());
    }

    if (triggerConnected) {
      if (logger.isDebugEnabled()) {
        logger.debug(getInfo() + " - Waiting for trigger");
      }

      Token token = triggerHandler.getToken();

      if (token == null) {
        requestFinish();
      } else {
        try {
          msg = MessageHelper.getMessageFromToken(token);
        } catch (PasserelleException e) {
          throw new ProcessingException(getInfo() + " - doFire() generated an exception while reading message", token, e);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Received msg :" + msg);
        }
      }
    }

    if (!isFinishRequested()) {
      try {
        // Check for command in header
        if ((msg != null) && msg.hasBodyHeader(COMMAND_HEADER)) {
          sourcePath = msg.getBodyHeader(COMMAND_HEADER);
        }
      } catch (MessageException e) {
        // just log it for completeness sake
        logger.error("", e);
      }

      if ((sourcePath == null) || (sourcePath.length == 0)) {
        sourcePath = new String[] { defaultSourcePath };
      }

      if ((sourcePath != null) && (sourcePath.length > 0)) {
        for (int i = 0; i < sourcePath.length; i++) {
          try {
            if (getAuditLogger().isInfoEnabled()) {
              getAuditLogger().info("Executing " + sourcePath[i]);
            }
            Runtime.getRuntime().exec(sourcePath[i]);
          } catch (IOException e) {
            logger.error("Unable to execute command : " + sourcePath[i]);
          }
        }
      }
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }
  }

  protected void doInitialize() throws InitializationException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo());
    }

    super.doInitialize();

    triggerConnected = trigger.getWidth() > 0;

    if (triggerConnected) {
      if (logger.isDebugEnabled()) logger.debug(getInfo() + " - Trigger(s) connected");
      triggerHandler = new PortHandler(trigger);
      triggerHandler.start();
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }
  }

  protected boolean doPostFire() throws ProcessingException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo());
    }

    boolean res = triggerConnected;

    if (res) {
      res = super.doPostFire();
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit " + " :" + res);
    }

    return res;
  }

  protected String getExtendedInfo() {
    return defaultSourcePath;
  }

}