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
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.message.interceptor.TextToMessageConverter;
import com.isencia.passerelle.message.xml.XmlMessageHelper;


/**
 * 
 * A TriggeredChannelSource is typically used to read messages from streams
 * with limited data feeds. E.g. reading from a file, a DB query etc, where the length
 * of the stream, or the nr of items is limited.
 * 
 * When the feed is exhausted, the source can be "re-activated" by sending a trigger pulse.
 * If the trigger port is not connected, the source will wrapup and terminate after exhausting its feed.
 * 
 * Messages from a TriggeredChannelSource are generated in a sequence.
 * Each time the source is re-triggered, the sequence ID is incremented.
 * Sequence IDs and positions follow the Java index conventions, i.e. they start at 0.
 * 
 * @author erwin
 */
public abstract class TriggeredChannelSource extends TriggeredSource {
    //~ Static variables/initializers __________________________________________________________________________________________________________________________

    private static Logger logger = LoggerFactory.getLogger(TriggeredChannelSource.class);

    //~ Instance variables _____________________________________________________________________________________________________________________________________

    private IReceiverChannel receiverChannel = null;
	public Parameter passThroughParam = null;
	private boolean passThrough = true;
	
	// start at -1 as it is incremented each time openChannel is invoked
	// and that happens before creating new messages
	private Long sequenceID;
	private long sequencePosition = 0;
	
	// message buffer
	// We always read 1 msg further in the stream than we're sending out.
	// This buffer maintains the previous msg we read, which will be sent out
	// on the next getMessage() call. As we're trying to obtain the next message
	// at the same time, we can check if there is still one available and if not,
	// mark the current outgoing message as last-in-sequence...
	private Object messageBuffer = null;
	private boolean readAheadOK = false;

	private boolean isEndOfSequence = false;

	// need to maintain this ourselves, as the channel can not correctly build
	// the message 1 step in advance...
	private IMessageInterceptorChain interceptorChain = null;

    //~ Constructors ___________________________________________________________________________________________________________________________________________

    /**
     * Constructor for Source.
     * @param container
     * @param name
     * @throws NameDuplicationException
     * @throws IllegalActionException
     */
    public TriggeredChannelSource(CompositeEntity container, String name)
                           throws NameDuplicationException, IllegalActionException {
        super(container, name);
		passThroughParam = new Parameter(this, "PassThrough", new BooleanToken(false));
		passThroughParam.setTypeEquals(BaseType.BOOLEAN);
		registerExpertParameter(passThroughParam);

		new CheckBoxStyle(passThroughParam, "style");
    }

    /**
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     */
    public IReceiverChannel getChannel() {
        return receiverChannel;
    }

	/**
	 * Triggered whenever e.g. a parameter has been modified.
	 * 
	 * @param attribute The attribute that changed.
	 * @exception IllegalActionException
	 */
	public void attributeChanged(Attribute attribute)
		throws IllegalActionException {

		if(logger.isTraceEnabled())
			logger.trace(getInfo()+" :"+attribute);
			
		if (attribute == passThroughParam) {
			passThrough = ((BooleanToken) passThroughParam.getToken()).booleanValue();
		} else
			super.attributeChanged(attribute);

		if(logger.isTraceEnabled())
			logger.trace(getInfo()+" - exit ");
	}
	
	protected void doInitialize() throws InitializationException {
		if (logger.isTraceEnabled()) {
			logger.trace(getInfo());
		}

		super.doInitialize();

		IReceiverChannel res = null;
        readAheadOK = false;

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

			if (!isPassThrough()) {
                interceptorChain  = createInterceptorChain();
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

		// this will cause the actor to wait for a trigger msg,
		// each time the channel is closed, and if the trigger port
		// is connected
		boolean res = true;
		if (mustWaitForTrigger()) {
			waitForTrigger();
			if( isFinishRequested() )
				res = false;
		}

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
			if( res )
				res = super.doPreFire();

		} catch (ChannelException e) {
			throw new ProcessingException(
				PasserelleException.Severity.FATAL,
				"Receiver channel for " + getInfo() + " not opened correctly.",
				getChannel(),
				e);
		}
		
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
     * DOCUMENT ME!
     *
     * @return DOCUMENT ME!
     *
     * @throws ChannelException DOCUMENT ME!
     */
    protected abstract IReceiverChannel createChannel()
                                               throws ChannelException;
    
    /**
     * This method can be overridden to define some custom mechanism
     * to convert incoming messages of some specific format into standard passerelle messages.
     * 
     * The method is called by this base class, 
     * only when it is not configured in "pass-through" mode.
     * In pass-through mode, we assume that the incoming message is a xml serialization of
     * a passerelle message, as generated by a passerelle sink in pass-through mode.
     * 
     * By default, creates an interceptor chain with a text2message converter. 
     * Because of the need to identify end-of-sequence
     * we can not let the channel create the ManagedMessage, but we need to do it after getting
     * the next msg from the channel, in order to determine if we have to create an outgoing message
     * with/without end-of-sequence flag.
     * This actor will use the interceptor chain itself at the right moment...
     * 
     * @see XmlMessageHelper.getXMLFromMessage() for the expected xml format in pass-through mode
     * 
     * @return
     */
    protected IMessageInterceptorChain createInterceptorChain() {
        IMessageInterceptorChain interceptors = new MessageInterceptorChain();
        interceptors.add(new TextToMessageConverter(this));
        return interceptors;
    }
    


	protected ManagedMessage getMessage() throws ProcessingException {
		if (logger.isTraceEnabled()) {
			logger.trace(getInfo());
		}
		
		ManagedMessage res = null;

		// TODO need to find a cleaner and more reusable way for this...
		
		if(!readAheadOK) {
			// read an extra first msg to start the read-ahead mechanism
			readAheadOK = true;
			try {
				messageBuffer = getChannel().getMessage();
			} catch (NoMoreMessagesException e) {
		        // ignore, just return null and the source will finish
		        // its life-cycle automatically
		    } catch (Exception e) {
				throw new ProcessingException(getInfo()+" - getMessage() generated exception while reading from channel",getChannel(),e);
			}
		}

		if(messageBuffer!=null) {
			// we still got a message previous time,
			// so try to get another one
			Object msg = null;
			try {
	            msg = getChannel().getMessage();
    			if( isPassThrough() )
    				// no way to add a sequence treatment here
    				// the resulting msg is constructed from its XML,
    				// including all system headers
    				res = XmlMessageHelper.getMessageFromXML((String)messageBuffer);
    			else {
    				// need to set this first, so the interceptor chain's call
    				// to createMessage() does it right...
    				isEndOfSequence = (msg==null);
    				if(interceptorChain!=null)
    					res = (ManagedMessage) interceptorChain.accept(messageBuffer);
    				else
    					res = (ManagedMessage) messageBuffer;
    				
	            }
			} catch (NoMoreMessagesException e) {
	            // ignore, just return null and the source will finish
	            // its life-cycle automatically
	        } catch (Exception e) {
				throw new ProcessingException(getInfo()+" - getMessage() generated exception",res,e);
			} finally {
    			messageBuffer = msg;
			}
		}

		if (logger.isTraceEnabled()) {
			logger.trace(getInfo()+" - exit " + " - Received msg :" + res);
		}

		return res;
	}

    /**
     * DOCUMENT ME!
     *
     * @param ch DOCUMENT ME!
     *
     * @throws ChannelException DOCUMENT ME!
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
     * Keeps the source waiting for an incoming trigger message,
     * when the source channel is closed.
     * <br>
     * The source channel is closed in 2 cases:
     * <ul>
     * <li> In the first fire iteration
     * <li> When the source channel has been exhausted
     * </ul>
     *
     * @return 
     */
    protected boolean mustWaitForTrigger() {
        return (!getChannel().isOpen());
    }

    /**
     * DOCUMENT ME!
     *
     * @param ch DOCUMENT ME!
     *
     * @throws ChannelException DOCUMENT ME!
     */
    protected void openChannel(IReceiverChannel ch) throws ChannelException {
        if (logger.isTraceEnabled()) {
            logger.trace(getInfo() + " :" + ch);
        }

        if ((ch != null) && !ch.isOpen()) {
            ch.open();
            sequenceID=MessageFactory.getInstance().createSequenceID();
        }

        if (logger.isTraceEnabled()) {
            logger.trace(getInfo()+" - exit ");
        }
    }
	/**
		 * Returns the passThrough.
		 * @return boolean
		 */
	public boolean isPassThrough() {
		return passThrough;
	}

	/**
		 * Sets the passThrough.
		 * @param passThrough The passThrough to set
		 */
	public void setPassThrough(boolean passThrough) {
		this.passThrough = passThrough;
	}

	public ManagedMessage createMessage() {
		return MessageFactory.getInstance().createMessageInSequence(sequenceID, new Long(sequencePosition++),isEndOfSequence ,getStandardMessageHeaders());
	}
}