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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.IntToken;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.Sink;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.util.ExecutionTracerService;

/**
 * Dump a message in an execution trace message
 * 
 * @author erwin
 * @version 1.0
 */
public class TracerConsole extends Sink {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5950430024278609950L;

  private static Logger logger = LoggerFactory.getLogger(TracerConsole.class);

  public Parameter chopLengthParam;
  private int chopLength = 80;

  /**
   * @param container The container.
   * @param name The name of this actor.
   * @exception IllegalActionException If the entity cannot be contained by the
   *              proposed container.
   * @exception NameDuplicationException If the container already has an actor
   *              with this name.
   */
  public TracerConsole(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
    super(container, name);

    chopLengthParam = new Parameter(this, "Chop output at #chars", new IntToken(chopLength));
  }

  public void attributeChanged(Attribute attribute) throws IllegalActionException {

    if (logger.isTraceEnabled()) logger.trace(getInfo() + " :" + attribute);

    if (attribute == chopLengthParam) {
      IntToken chopLengthToken = (IntToken) chopLengthParam.getToken();
      if (chopLengthToken != null) {
        chopLength = chopLengthToken.intValue();
        logger.debug("Chop length changed to : " + chopLength);
      }
    } else
      super.attributeChanged(attribute);

    if (logger.isTraceEnabled()) logger.trace(getInfo() + " - exit ");
  }

  protected void sendMessage(ManagedMessage message) throws ProcessingException {
    if (logger.isTraceEnabled()) logger.trace(getInfo());

    if (message != null) {
      if (isPassThrough()) {
        ExecutionTracerService.trace(this, message.toString());
      } else {
        String content = null;
        try {
          content = message.getBodyContentAsString();
          if (chopLength < content.length()) {
            content = content.substring(0, chopLength) + " !! CHOPPED !! ";
          }
        } catch (MessageException e) {
          throw new ProcessingException(PasserelleException.Severity.NON_FATAL, "", message, e);
        }
        if (content != null) {
          ExecutionTracerService.trace(this, content);
        }
      }
    }

    if (logger.isTraceEnabled()) logger.trace(getInfo() + " - exit ");
  }

  public int getChopLength() {
    return chopLength;
  }

}