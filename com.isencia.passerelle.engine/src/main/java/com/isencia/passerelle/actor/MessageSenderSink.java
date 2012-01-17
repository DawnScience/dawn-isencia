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

import java.util.Collection;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.message.IMessageSender;
import com.isencia.message.ISenderChannel;
import com.isencia.message.interceptor.IMessageInterceptorChain;
import com.isencia.message.interceptor.MessageInterceptorChain;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.interceptor.MessageToTextConverter;


/**
 * @version 1.0
 * @author edeley
 */
public abstract class MessageSenderSink extends Sink {
    //~ Instance/static variables ..............................................................................................................................

    private static Logger logger = LoggerFactory.getLogger(MessageSenderSink.class);
    private IMessageSender messageSender = null;

    //~ Constructors ...........................................................................................................................................

    /**
     * Constructor for ChannelSink.
     * @param container
     * @param name
     * @throws NameDuplicationException
     * @throws IllegalActionException
     */
    public MessageSenderSink(CompositeEntity container, String name)
                      throws NameDuplicationException, IllegalActionException {
        super(container, name);
    }

    //~ Methods ................................................................................................................................................

    /**
     * DOCUMENT ME !
     * 
     * @throws IllegalActionException
     */
    protected void sendMessage(ManagedMessage message) throws ProcessingException {
        if (logger.isTraceEnabled())
            logger.trace(getInfo());

		if(message!=null) {
			messageSender.sendMessage(message);
		} else {
			requestFinish();
		}
			
        if (logger.isTraceEnabled())
            logger.trace(getInfo()+" - exit ");
    }

    protected void doInitialize() throws InitializationException {
        if (logger.isTraceEnabled())
            logger.trace(getInfo());

        super.doInitialize();
        
        messageSender = createMessageSender();

        if (messageSender == null) {
			throw new InitializationException(PasserelleException.Severity.FATAL,"MessageSender for " + getInfo() + " not created correctly.",this,null);
        } else {
            IMessageInterceptorChain interceptors = createInterceptorChainOnEnter();

            if (interceptors == null) {
                // default implementation
                if (!isPassThrough()) {
                    interceptors = new MessageInterceptorChain();
                    interceptors.add(new MessageToTextConverter());
                }
            }

            Collection channels = messageSender.getChannels();
            Iterator iter = channels.iterator();

            while (iter.hasNext()) {
                ISenderChannel element = (ISenderChannel)iter.next();
                element.setInterceptorChainOnEnter(interceptors);
            }

            messageSender.open();
			if(logger.isInfoEnabled()) {
				logger.info(getInfo()+" - Opened :"+getMessageSender());
			}
        }

        if (logger.isTraceEnabled())
            logger.trace(getInfo()+" - exit ");
    }

    protected void doWrapUp() throws TerminationException {
        if (logger.isTraceEnabled())
            logger.trace(getInfo());

        super.doWrapUp();
        getMessageSender().close();
		if(logger.isInfoEnabled()) {
			logger.info(getInfo()+" - Closed :"+getMessageSender());
		}

        if (logger.isTraceEnabled())
            logger.trace(getInfo()+" - exit ");
    }

    /**
     * DOCUMENT ME !
     * 
     * @return  
     */
    protected abstract IMessageInterceptorChain createInterceptorChainOnEnter();

    /**
     * DOCUMENT ME !
     * 
     * @return  
     */
    protected abstract IMessageSender createMessageSender();
	/**
	 * Returns the messageSender.
	 * @return IMessageSender
	 */
	public IMessageSender getMessageSender() {
		return messageSender;
	}


}