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
package com.isencia.passerelle.actor.ftp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.message.ftp.FtpSenderChannel;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * This actor writes all received msgs to a file on an ftp-server.
 * 
 * @author Bram Bogaert
 */
public class FtpFileWriter extends FtpWriter {
  private static Logger logger = LoggerFactory.getLogger(FtpFileWriter.class);

  public Parameter overwriteParam = null;
  private boolean overwrite = false;
  private static final String OVERWRITE_PARAM = "Overwrite existing file";

  /**
   * Construct an actor with the given container and name.
   * 
   * @param container The container.
   * @param name The name of this actor.
   * @exception IllegalActionException If the actor cannot be contained by the
   *              proposed container.
   * @exception NameDuplicationException If the container already has an actor
   *              with this name.
   */
  public FtpFileWriter(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);

    overwriteParam = new Parameter(this, OVERWRITE_PARAM, new BooleanToken(overwrite));
    overwriteParam.setTypeEquals(BaseType.BOOLEAN);
  }

  public void attributeChanged(Attribute attribute) throws IllegalActionException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " :" + attribute);
    }

    // Define actions to be taken when a parameter/atribute has been changed
    if (attribute == overwriteParam) { // Change 1x
      BooleanToken aToken = (BooleanToken) overwriteParam.getToken(); // Change
                                                                      // 1x
      if (aToken != null) {
        overwrite = aToken.booleanValue(); // Change 1x
        logger.debug("Overwrite changed to : " + overwrite); // Change 2x
      }
    } else {
      super.attributeChanged(attribute);
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }
  }

  protected void doFire(ManagedMessage message) throws ProcessingException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo());
    }

    try {
      if (message.getBodyHeader("toFile") != null) {
        if (getChannel().isOpen()) {
          getChannel().close();
        }
        String filename = message.getBodyHeader("toFile")[0];
        ((FtpSenderChannel) getChannel()).setRemoteFileName(filename);
        getChannel().open();
      }

      this.getChannel().sendMessage(message);

      if (logger.isInfoEnabled()) {
        logger.info(getInfo() + " - Sent message :" + message);
      }
    } catch (InterruptedException e) {
      // do nothing, just means we've got to stop
    } catch (Exception e) {
      throw new ProcessingException(getInfo() + " - doFire() generated exception " + e, message, e);
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }
  }
}
