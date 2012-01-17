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

package com.isencia.passerelle.actor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.gui.style.CheckBoxStyle;
import ptolemy.data.BooleanToken;
import ptolemy.data.Token;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.message.ChannelException;
import com.isencia.message.IReceiverChannel;
import com.isencia.message.NoMoreMessagesException;
import com.isencia.message.interceptor.IMessageInterceptorChain;
import com.isencia.message.interceptor.MessageInterceptorChain;
import com.isencia.message.requestreply.IMessage;
import com.isencia.message.requestreply.IRequestReplyChannel;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.core.PortHandler;
import com.isencia.passerelle.core.PortListenerAdapter;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.message.MessageHelper;
import com.isencia.passerelle.message.interceptor.MessageToTextConverter;
import com.isencia.passerelle.message.xml.XmlMessageHelper;

/**
 * Base class for all Passerelle Sources that use a  IReceiverChannel. Sub-classes must implement createChannel(), returning a completely
 * defined IReceiverChannel of the desired type.  Typically, sub-classes will also define their specific Passerelle Parameters, and will then override the
 * default attributeChanged() method to handle changes in parameter values.  openChannel() and closeChannel() may sometimes be overriden to add specific
 * open/close processing.
 * 
 * @version 1.0
 * @author edeley
 */
public abstract class ReqReplyChannelSource extends Source {
	//~ Static variables/initializers __________________________________________________________________________________________________________________________

	private static Logger logger = LoggerFactory.getLogger(ReqReplyChannelSource.class);

	//~ Instance variables _____________________________________________________________________________________________________________________________________

	private IRequestReplyChannel receiverChannel = null;
	public Parameter passThroughParam = null;
	private boolean passThrough = true;

    public Port replyPort = null;
    private PortHandler replyHandler = null;


	//~ Constructors ___________________________________________________________________________________________________________________________________________

	/**
	 * Constructor for Source.
	 * 
	 * @param container
	 * @param name
	 * 
	 * @throws NameDuplicationException
	 * @throws IllegalActionException
	 */
	public ReqReplyChannelSource(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);
        // Ports
        replyPort = PortFactory.getInstance().createInputPort(this,"reply", null);

		passThroughParam = new Parameter(this, "PassThrough", new BooleanToken(false));
		passThroughParam.setTypeEquals(BaseType.BOOLEAN);
		registerExpertParameter(passThroughParam);

		new CheckBoxStyle(passThroughParam, "style");

	}

	/**
	 * Triggered whenever e.g. a parameter has been modified.
	 * 
	 * @param attribute The attribute that changed.
	 * @exception IllegalActionException
	 */
	public void attributeChanged(Attribute attribute) throws IllegalActionException {

		if (logger.isTraceEnabled())
			logger.trace(getInfo() + " :" + attribute);

		if (attribute == passThroughParam) {
			passThrough = ((BooleanToken) passThroughParam.getToken()).booleanValue();
		} else
			super.attributeChanged(attribute);

		if (logger.isTraceEnabled())
			logger.trace(getInfo()+" - exit ");
	}


	/**
	 * Returns the current receiver channel.
	 * 
	 * @return IReceiverChannel
	 */
	public IRequestReplyChannel getChannel() {
		return receiverChannel;
	}

	/**
     * @todo improve termination handling with a combination of
     * end of source and end of sink processing 
	 */
	protected void doInitialize() throws InitializationException {
		if (logger.isTraceEnabled()) {
			logger.trace(getInfo());
		}

		super.doInitialize();

        if(replyPort.getWidth()>0) {
            replyHandler = new PortHandler(replyPort,
                    new PortListenerAdapter() {
                        public void tokenReceived() {
                            if(logger.isTraceEnabled()) {
                                logger.trace(getInfo()+" - inputHandler.tokenReceived() - entry");
                            }
                            Token token = replyHandler.getToken();
                            if (token != null) {
                                try {
									ManagedMessage message = MessageHelper.getMessageFromToken(token);
									if (logger.isInfoEnabled()) {
									    logger.info(getInfo() + " - received reply msg :" + message);
									}
									try {
									    getChannel().sendResponse(message,message.getCorrelationID());
									} catch (ChannelException e) {
									    try {
									        sendErrorMessage(new ProcessingException("Failed to send reply msg",message,e));
									    } catch (IllegalActionException e1) {
									        logger.error("",e);
									        logger.error("",e1);
									    }
									}
								} catch (Exception e) {
							        logger.error("",e);
								}
                            }
                            if(logger.isTraceEnabled()) {
                                logger.trace(getInfo()+" - inputHandler.tokenReceived() - exit");
                            }
                        }
                    }
            );
            // Start handling the port
            replyHandler.start();
        }

		IRequestReplyChannel res = null;

		try {
			res = createChannel();
		} catch (ChannelException e) {
			throw new InitializationException(
				PasserelleException.Severity.FATAL,
				"Receiver channel for " + getInfo() + " not created correctly.",
				this,
				e);
		}

		if (res == null) {
			throw new InitializationException(
				PasserelleException.Severity.FATAL,
				"Receiver channel for " + getInfo() + " not created correctly.",
				this,
				null);
		} else {
			// just to make sure...
			try {
				closeChannel(getChannel());
			} catch (ChannelException e) {
				throw new InitializationException(
					"Receiver channel for " + getInfo() + " not initialized correctly.",
					getChannel(),
					e);
			}

			receiverChannel = res;
			// This does not work for request reply:
            // - the converter is called by the regular receiver channel when it invokes its interceptor chain, 
            //   BEFORE the ReqReplyChannel wrapper has the chance to define the correlation ID
            // - we can not add the correlation ID after the msg construction, as this is a protected system header field
            // -> so we need to receive the fully defined request msg, and THEN invoke the message construction, passing the
            //   correlation ID into the MessageFactory.
            // This is done inside the getMessage()
			if (!isPassThrough()) {
//				IMessageInterceptorChain interceptors = new MessageInterceptorChain();
//				interceptors.add(new TextToMessageConverter());
//				getChannel().setInterceptorChainOnLeave(interceptors);
                if(logger.isDebugEnabled())
                    logger.debug(getInfo() + "Converting message to text");
                IMessageInterceptorChain interceptors = new MessageInterceptorChain();
                interceptors.add(new MessageToTextConverter());
                getChannel().setInterceptorChainForResponse(interceptors);
            }

		}

		if (logger.isTraceEnabled()) {
			logger.trace(getInfo()+" - exit ");
		}
	}
	protected boolean doPreFire() throws ProcessingException {
		if (logger.isTraceEnabled()) {
			logger.trace(getInfo());
		}

		boolean res = true;

		// Channel open must not be done in initialize()
		// All initialize() invocations on the actors in a model
		// are done sequentially in 1 thread. So if a certain channel
		// depends on another channel's open status, this might fail if
		// the respective initialize() methods are invoked in the wrong order.
		// prefire() is called in separate threads per actor. Then a channel 
		// can implement retries during open(), giving the other actors the time
		// to open their channels as well.
		try {
			if (!getChannel().isOpen()) {
				openChannel(getChannel());

				if (logger.isInfoEnabled()) {
					logger.info(getInfo() + " - Opened :" + getChannel());
				}
			}
		} catch (ChannelException e) {
			throw new ProcessingException(
				PasserelleException.Severity.FATAL,
				"Receiver channel for " + getInfo() + " not opened correctly.",
				getChannel(),
				e);
		}
		
		res = res && super.doPreFire();

		if (logger.isTraceEnabled()) {
			logger.trace(getInfo()+" - exit " + " :" + res);
		}

		return res;
	}
	protected boolean doPostFire() throws ProcessingException {
		if (logger.isTraceEnabled()) {
			logger.trace(getInfo());
		}

		try {
			if (hasNoMoreMessages()) {
				closeChannel(getChannel());
			}
		} catch (ChannelException e) {
			throw new ProcessingException("Receiver channel for " + getInfo() + " not closed correctly.", getChannel(), e);
		}

		boolean res = super.doPostFire();

		if (logger.isTraceEnabled()) {
			logger.trace(getInfo()+" - exit " + " :" + res);
		}

		return res;
	}

	protected void doWrapUp() throws TerminationException {
		if (logger.isTraceEnabled()) {
			logger.trace(getInfo());
		}

		try {
			closeChannel(getChannel());

			if (logger.isInfoEnabled()) {
				logger.info(getInfo() + " - Closed :" + getChannel());
			}
		} catch (ChannelException e) {
			throw new TerminationException("Receiver channel for " + getInfo() + " not closed correctly.", getChannel(), e);
		}

		super.doWrapUp();

		if (logger.isTraceEnabled()) {
			logger.trace(getInfo()+" - exit ");
		}
	}

	/**
	 * Factory method to be implemented per specific type of source
	 * 
	 * @return a new instance of a receiver channel of the relevant type
	 * @throws ChannelException 
     * @throws InitializationException
	 */
	protected abstract IRequestReplyChannel createChannel() throws ChannelException, InitializationException;
	
	protected ManagedMessage getMessage() throws ProcessingException {
		if (logger.isTraceEnabled()) {
			logger.trace(getInfo());
		}

		ManagedMessage res = null;

        try {
            IMessage msg = getChannel().receiveRequest();
			if (isPassThrough())
				res = XmlMessageHelper.fillMessageContentFromXML(createMessage(),(String) msg.getMessage());
			else {
                // this allows to directly link the output port to the reply port
                // as the msg contains its own ID as correlationID
				res = MessageFactory.getInstance().createCorrelatedMessage(msg.getCorrelationID().toString(),getStandardMessageHeaders());
                try {
                    res.setBodyContentPlainText(msg.getMessage().toString());
                } catch (MessageException e) {
                    logger.error(e.getMessage());
                    res = null;
                }
            }
            
		} catch (NoMoreMessagesException e) {
			// ignore, just return null and the source will finish
			// its life-cycle automatically
		} catch (Exception e) {
			throw new ProcessingException(getInfo() + " - getMessage() generated exception", res, e);
		}

		if (logger.isTraceEnabled()) {
			logger.trace(getInfo()+" - exit " + " - Received msg :" + res);
		}

		return res;
	}

	/**
	 * Overridable method to allow modification of channel closing.
	 * 
	 * @param ch the channel to be closed
	 * 
	 * @throws ChannelException
	 */
	protected void closeChannel(IReceiverChannel ch) throws ChannelException {
		if (logger.isTraceEnabled()) {
			logger.trace(getInfo() + " :" + ch);
		}

		if ((ch != null) && ch.isOpen()) {
			ch.close();
		}

		if (logger.isTraceEnabled()) {
			logger.trace(getInfo()+" - exit ");
		}
	}

	/**
	 * Overridable method to allow modification of channel opening.
	 * 
	 * @param ch the channel to be opened
	 * 
	 * @throws ChannelException
	 */
	protected void openChannel(IReceiverChannel ch) throws ChannelException {
		if (logger.isTraceEnabled()) {
			logger.trace(getInfo() + " :" + ch);
		}

		if ((ch != null) && !ch.isOpen()) {
			ch.open();
		}

		if (logger.isTraceEnabled()) {
			logger.trace(getInfo()+" - exit ");
		}
	}

	/**
	 * Returns the passThrough flag, indicating whether messages should be sent on as received
	 * or should be wrapped in a std Passerelle envelope.
	 * 
	 * @return the passThrough flag
	 */
	public boolean isPassThrough() {
		return passThrough;
	}

	/**
	 * Sets the passThrough, indicating whether messages should be sent on as received
	 * or should be wrapped in a std Passerelle envelope.
	 * 
	 * @param the passThrough value
	 */
	public void setPassThrough(boolean passThrough) {
		this.passThrough = passThrough;
	}

}