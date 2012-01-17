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

package com.isencia.passerelle.actor.net;

import java.io.IOException;
import java.net.ServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.IntToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import com.isencia.message.ChannelException;
import com.isencia.message.IReceiverChannel;
import com.isencia.message.extractor.IMessageExtractor;
import com.isencia.message.net.SocketServerReceiverChannel;
import com.isencia.passerelle.actor.ChannelSource;
import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.gui.IOptionsFactory.Option;
import com.isencia.passerelle.core.PasserelleException;

/**
 * DOCUMENT ME!
 * 
 * @version $Id: SocketServerReceiver.java,v 1.5 2006/02/06 20:08:50 erwin Exp $
 * @author Dirk Jacobs
 */
public class SocketServerReceiver extends ChannelSource {
  // ~ Static variables/initializers
  // __________________________________________________________________________________________________________________________

  // /////////////////////////////////////////////////////////////////
  // // private variables ////
  private static Logger logger = LoggerFactory.getLogger(SocketServerReceiver.class);

  // ~ Instance variables
  // _____________________________________________________________________________________________________________________________________

  // /////////////////////////////////////////////////////////////////
  // // ports and parameters ////

  /** The server listen socket port. */
  public Parameter socketPort;
  private int port = 3333;

  public Parameter msgExtractorType;
  final static String MSG_EXTRACTOR_PARAM_NAME = "Msg End";

  // ~ Constructors
  // ___________________________________________________________________________________________________________________________________________

  /**
   * SocketServerSource constructor comment.
   * 
   * @param container ptolemy.kernel.CompositeEntity
   * @param name java.lang.String
   * @exception ptolemy.kernel.util.IllegalActionException The exception
   *              description.
   * @exception ptolemy.kernel.util.NameDuplicationException The exception
   *              description.
   */
  public SocketServerReceiver(ptolemy.kernel.CompositeEntity container, String name) throws ptolemy.kernel.util.IllegalActionException,
      ptolemy.kernel.util.NameDuplicationException {
    super(container, name);

    // Parameters
    socketPort = new Parameter(this, "port", new IntToken(getPort()));
    socketPort.setTypeEquals(BaseType.INT);
    msgExtractorType = new StringParameter(this, MSG_EXTRACTOR_PARAM_NAME);
  }

  // ~ Methods
  // ________________________________________________________________________________________________________________________________________________

  /**
   * Sets the port.
   * 
   * @param port The port to set
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Gets the port.
   * 
   * @return Returns a int
   */
  public int getPort() {
    return port;
  }

  /**
   * @param attribute The attribute that changed.
   * @exception IllegalActionException
   */
  public void attributeChanged(Attribute attribute) throws IllegalActionException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " :" + attribute);
    }

    if (attribute == socketPort) {
      IntToken portToken = (IntToken) socketPort.getToken();

      if ((portToken != null) && (portToken.intValue() > 0)) {
        setPort(portToken.intValue());
      }
    } else {
      super.attributeChanged(attribute);
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }
  }

  protected String getExtendedInfo() {
    return "localhost:" + getPort();
  }

  protected IReceiverChannel createChannel() throws ChannelException, InitializationException {
    IMessageExtractor extractor = getExtractorFromSelectedOption();
    IReceiverChannel res = null;
    try {
      ServerSocket sSocket = new ServerSocket(getPort());
      res = new SocketServerReceiverChannel(sSocket, extractor);
    } catch (IOException e) {
      throw new InitializationException(PasserelleException.Severity.FATAL, "Error opening server socket on port" + getPort(), this, e);
    }

    return res;
  }

  /**
   * @param extractor
   * @return
   * @throws InitializationException
   */
  private IMessageExtractor getExtractorFromSelectedOption() throws InitializationException {
    IMessageExtractor extractor = null;
    // we wait as long as possible before checking
    // the options factory settings, so don't do it
    // in the constructor or attributeChanged or...
    try {
      if (getOptionsFactory() == null) {
        setOptionsFactory(new SocketSvrRcvOptionsFactory(this, OPTIONS_FACTORY_CFG_NAME));
      }
      Option o = getOptionsFactory().getOption(msgExtractorType, msgExtractorType.getExpression());
      if (o == null) {
        o = getOptionsFactory().getDefaultOption(msgExtractorType);
      }
      extractor = ((IMessageExtractor) o.getAssociatedObject()).cloneExtractor();
    } catch (Exception e) {
      throw new InitializationException(PasserelleException.Severity.FATAL, "Error setting Parameter options factory", this, e);
    }

    return extractor;
  }
}