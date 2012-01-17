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
import com.isencia.message.ChannelException;
import com.isencia.message.ISenderChannel;
import com.isencia.message.interceptor.IMessageInterceptorChain;
import com.isencia.message.interceptor.MessageInterceptorChain;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.interceptor.MessageToTextConverter;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;


/**
 * Base class for all Passerelle Sinks that use a  ISenderChannel. Sub-classes must implement createChannel(), returning a completely defined
 * ISenderChannel of the desired type.  Typically, sub-classes will also define their specific Passerelle Parameters, and will then override the default
 * attributeChanged() method to handle changes in parameter values.  openChannel() and closeChannel() may sometimes be overriden to add specific open/close
 * processing.  ChannelSink provides a fully functional doFire() implementation that uses the ISenderChannel to send out any messages received on the actor's
 * input.
 * 
 * @author Erwin De Ley
 */
public abstract class ChannelSink extends Sink {
    //~ Static variables/initializers __________________________________________________________________________________________________________________________

    private static Logger logger = LoggerFactory.getLogger(ChannelSink.class);

    //~ Instance variables _____________________________________________________________________________________________________________________________________

    private ISenderChannel sendChannel = null;

    //~ Constructors ___________________________________________________________________________________________________________________________________________

    /**
     * Constructor for ChannelSink.
     * 
     * @param container
     * @param name
     * 
     * @throws NameDuplicationException
     * @throws IllegalActionException
     */
    public ChannelSink(CompositeEntity container, String name)
                throws NameDuplicationException, IllegalActionException {
        super(container, name);
    }

    //~ Methods ________________________________________________________________________________________________________________________________________________

    /**
     * Returns the sender channel.
     * 
     * @return ISenderChannel
     */
    public ISenderChannel getChannel() {
        return sendChannel;
    }

    protected void sendMessage(ManagedMessage message) throws ProcessingException {
        if (logger.isTraceEnabled()) {
            logger.trace(getInfo());
        }

        try {
            if (message != null) {
                getChannel().sendMessage(message);
            } else {
                requestFinish();
            }
        } catch (InterruptedException e) {
            // do nothing, just means we've got to stop
        } catch (Exception e) {
            throw new ProcessingException(getInfo()+" - getChannel().sendMessage() caused exception :"+e,message,e);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(getInfo()+" - exit ");
        }
    }

    protected void doInitialize() throws InitializationException {
        if (logger.isTraceEnabled()) {
            logger.trace(getInfo());
        }

		super.doInitialize();
		
        ISenderChannel res = null;

        try {
            res = createChannel();
        } catch (ChannelException e) {
            throw new InitializationException(PasserelleException.Severity.FATAL,"Sender channel for " + getInfo() + " not created correctly.", this, e);
        }

        if (res == null) {
			throw new InitializationException(PasserelleException.Severity.FATAL,"Sender channel for " + getInfo() + " not created correctly.", this, null);
        } else {
            // just to make sure...
            try {
                closeChannel(getChannel());
            } catch (ChannelException e) {
				throw new InitializationException("Sender channel for " + getInfo() + " not initialized correctly.", getChannel(), e);
            }

            sendChannel = res;

            if (!isPassThrough()) {
            	IMessageInterceptorChain interceptors = createInterceptorChain();
                sendChannel.setInterceptorChainOnEnter(interceptors);
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
            throw new ProcessingException(PasserelleException.Severity.FATAL,"Sender channel for " + getInfo() + " not opened correctly.",getChannel(),e);
        }
        
        res = res && super.doPreFire();

        if (logger.isTraceEnabled()) {
            logger.trace(getInfo() +" - exit :" + res);
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
            throw new TerminationException(getInfo() + " - Error closing channel.",getChannel(), e);
        }

        if (logger.isTraceEnabled()) {
            logger.trace(getInfo()+" - exit ");
        }
    }

    /**
     * Factory method to be implemented per specific type of sink
     * 
     * @return a new instance of a sender channel of the relevant type
     * 
     * @throws ChannelException
     * @throws InitializationException 
     */
    protected abstract ISenderChannel createChannel() throws ChannelException, InitializationException;

    /**
     * This method can be overridden to define some custom mechanism
     * to convert outgoing passerelle messages into some desired format.
     * 
     * The method is called by this base class, 
     * only when it is not configured in "pass-through" mode.
     * 
     * By default, creates an interceptor chain containing
     * a simple message-body-to-text conversion.
     * 
     * @return
     */
    protected IMessageInterceptorChain createInterceptorChain() {
        if(logger.isDebugEnabled())
            logger.debug(getInfo() + "Converting message to text");
        IMessageInterceptorChain interceptors = new MessageInterceptorChain();
        interceptors.add(new MessageToTextConverter());
        return interceptors;
    }

    protected String getExtendedInfo() {
        return null;
    }

    /**
     * Overridable method to allow modification of channel closing.
     * 
     * @param ch DOCUMENT ME!
     * 
     * @throws ChannelException
     */
    protected void closeChannel(ISenderChannel ch) throws ChannelException {
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
     * @param ch DOCUMENT ME!
     * 
     * @throws ChannelException
     */
    protected void openChannel(ISenderChannel ch) throws ChannelException {
        if (logger.isTraceEnabled()) {
            logger.trace(getInfo() + " :" + ch);
        }

        if (ch != null) {
            ch.open();
        }

        if (logger.isTraceEnabled()) {
            logger.trace(getInfo()+" - exit ");
        }
    }
}