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
import com.isencia.message.IMessageReceiver;
import com.isencia.message.IReceiverChannel;
import com.isencia.message.interceptor.IMessageInterceptorChain;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;


/**
 * @version 1.0
 * @author edeley
 */
public abstract class MessageReceiverSource extends Source {
    //~ Instance/static variables ..............................................................................................................................

    private static Logger logger = LoggerFactory.getLogger(MessageReceiverSource.class);
    private IMessageReceiver messageReceiver = null;

    //~ Constructors ...........................................................................................................................................

    /**
     * Creates a new MessageReceiverSource object.
     * 
     * @param container
     * @param name
     * @throws NameDuplicationException
     * @throws IllegalActionException
     */
    public MessageReceiverSource(CompositeEntity container, String name)
                          throws NameDuplicationException, IllegalActionException {
        super(container, name);
    }

    //~ Methods ................................................................................................................................................

    /**
     * Returns the messageReceiver.
     * @return IMessageReceiver
     */
    public IMessageReceiver getMessageReceiver() {
        return messageReceiver;
    }

    protected void doInitialize() throws InitializationException {
        if (logger.isTraceEnabled())
            logger.trace(getInfo());

        super.doInitialize();
        messageReceiver = createMessageReceiver();
        if (messageReceiver == null) {
            throw new InitializationException(PasserelleException.Severity.FATAL,"MessageReceiver for " + getInfo() + " not created correctly.",this,null);
        } else {
            IMessageInterceptorChain interceptors = createInterceptorChainOnLeave();
            Collection channels = messageReceiver.getChannels();
            synchronized (channels) {
                Iterator iter = channels.iterator();
                while (iter.hasNext()) {
                    IReceiverChannel element = (IReceiverChannel)iter.next();
                    element.setInterceptorChainOnLeave(interceptors);
                }
            }

            messageReceiver.open();
			if(logger.isInfoEnabled()) {
				logger.info(getInfo()+" - Opened :"+getMessageReceiver());
			}
        }

        if (logger.isTraceEnabled())
            logger.trace(getInfo()+" - exit ");
    }

    protected void doWrapUp() throws TerminationException {
        if (logger.isTraceEnabled())
            logger.trace(getInfo());

        super.doWrapUp();
        getMessageReceiver().close();
		if(logger.isInfoEnabled()) {
			logger.info(getInfo()+" - Closed :"+getMessageReceiver());
		}
		
        if (logger.isTraceEnabled())
            logger.trace(getInfo()+" - exit ");
    }

    /**
     * DOCUMENT ME !
     * 
     * @return  
     */
    protected abstract IMessageInterceptorChain createInterceptorChainOnLeave();

    /**
     * DOCUMENT ME !
     * 
     * @return  
     */
    protected abstract IMessageReceiver createMessageReceiver();

    protected ManagedMessage getMessage() throws ProcessingException {
        if (logger.isTraceEnabled())
            logger.trace(getInfo());

        ManagedMessage res = null;
        try {
            if (messageReceiver != null)
                res = (ManagedMessage)messageReceiver.getMessage();
        } catch (Exception e) {
            throw new ProcessingException(getInfo() + " - getMessage() generated an exception in messageReceiver.getMessage() :"+e,res,e);
        }

        if (logger.isTraceEnabled())
            logger.trace(getInfo()+" - exit " + " - Received :" + res);

        return res;
    }
}