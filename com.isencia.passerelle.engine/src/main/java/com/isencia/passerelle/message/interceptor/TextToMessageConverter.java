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
package com.isencia.passerelle.message.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.message.generator.IMessageGenerator;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;


/**
 * @version 	1.0
 * @author		erwin
 */
public class TextToMessageConverter extends ManagedMessageConverter {

	/**
	 * @param msgCreator
	 */
	public TextToMessageConverter(IMessageCreator msgCreator) {
		super(msgCreator);
	}

	private final static Logger logger = LoggerFactory.getLogger(TextToMessageConverter.class);
	
	/**
	 * @see IMessageGenerator#acceptMessage(Object)
	 */
	public Object accept(Object message) throws Exception {
		if (logger.isTraceEnabled())
			logger.trace("Accepting ~" + message + "~");
			
		ManagedMessage messageContainer = null;
		if (message != null) {

			messageContainer = createMessage();
			try {
				messageContainer.setBodyContentPlainText((String) message);
			} catch (MessageException e) {
				logger.error(e.getMessage());
				messageContainer = null;
			}
		}

		if (logger.isTraceEnabled())
			logger.trace("exit :" + messageContainer);
			
		return messageContainer;
	}

	public ManagedMessageConverter cloneConverter(IMessageCreator msgCreator) {
		return new TextToMessageConverter(msgCreator);
	}

}
