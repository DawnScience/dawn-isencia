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

import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.core.PortHandler;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageHelper;

/**
 * Base class for all Sources with a trigger port.
 * 
 * @author dirk
 */
public abstract class TriggeredSource extends Source {

	private static Logger logger = LoggerFactory.getLogger(TriggeredSource.class);

	public final static String TRIGGER_PORT = "trigger";
	
	public Port trigger = null;
	private boolean triggerConnected = false;
	private PortHandler triggerHandler = null;

	/**
	 * Constructor for Source.
	 * @param container
	 * @param name
	 * @throws NameDuplicationException
	 * @throws IllegalActionException
	 */
	public TriggeredSource(CompositeEntity container, String name) throws NameDuplicationException, IllegalActionException {
		super(container, name);

		/** The trigger port. 
		 */
		trigger = PortFactory.getInstance().createInputPort(this, TRIGGER_PORT, null);

	}

	protected void doInitialize() throws InitializationException {
		if (logger.isTraceEnabled())
			logger.trace(getInfo());

		super.doInitialize();

		triggerConnected = trigger.getWidth() > 0;
		if (triggerConnected) {
			if(logger.isDebugEnabled())
				logger.debug(getInfo()+" - Trigger(s) connected");
			triggerHandler = new PortHandler(trigger);
			triggerHandler.start();
		}

		if (logger.isTraceEnabled())
			logger.trace(getInfo()+" - exit ");

	}

	protected boolean doPreFire() throws ProcessingException {
		boolean res = true;

		if (logger.isTraceEnabled())
			logger.trace(getInfo());

		if (mustWaitForTrigger()) {
			waitForTrigger();
			if( isFinishRequested() )
				res = false;
		}
		if( res )
			res = super.doPreFire();
		
		if (logger.isTraceEnabled())
			logger.trace(getInfo()+" - exit "+" :"+res);

		return res;
	}

	protected boolean doPostFire() throws ProcessingException {
		if (logger.isTraceEnabled())
			logger.trace(getInfo()+" doPostFire() - entry");

		boolean res = !hasNoMoreMessages() || isTriggerConnected();
        if(!res) {
            // just to make sure base class knows that we're finished
            requestFinish();
        } else {
			res = super.doPostFire();
		}
		
		if (logger.isTraceEnabled())
			logger.trace(getInfo()+" doPostFire() - exit :"+res);

		return res;
	}

	/**
	 * This method blocks until a trigger message has been received
	 * on the trigger port.
	 * @throws ProcessingException 
	 *
	 */
	public void waitForTrigger() throws ProcessingException {
		if (triggerConnected) {
			if(logger.isDebugEnabled()) {
				logger.debug(getInfo()+" - Waiting for trigger");
			}
			Token token = triggerHandler.getToken();
			if( token==null) {
                // no more triggers will arrive so let's call it quits
				requestFinish();
            } else {
				try {
					ManagedMessage message = MessageHelper.getMessageFromToken(token);
					acceptTriggerMessage(message);
				} catch (PasserelleException e) {
					throw new ProcessingException("Error handling token", token, e);
				}
            }
				
			if(logger.isInfoEnabled()) {
				logger.info(getInfo()+" - Trigger received");
			}
		}
	}

	/**
	 * Returns the triggerConnected.
	 * @return boolean
	 */
	public boolean isTriggerConnected() {
		return triggerConnected;
	}
	
	/**
	 * "callback"-method that can be overridden by TriggeredSource implementations,
	 * if they want to act e.g. on the contents of a received trigger message.
	 * 
	 * @param triggerMsg
	 */
	protected void acceptTriggerMessage(ManagedMessage triggerMsg) {
		
	}

	protected abstract boolean mustWaitForTrigger();
	
}
