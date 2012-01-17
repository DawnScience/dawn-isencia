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
package com.isencia.passerelle.message.xml;

import java.io.IOException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;

/**
 * XmlMessageHelper
 * 
 * TODO: class comment
 * 
 * @author erwin
 */
public class XmlMessageHelper {
	
    private static Logger logger = LoggerFactory.getLogger(XmlMessageHelper.class);

	
	public static ManagedMessage getMessageFromXML(String xmlText) throws MessageException {
		return MessageBuilder.buildFromXML(xmlText);
	}
	public static ManagedMessage fillMessageContentFromXML(ManagedMessage message, String xmlText) throws MessageException {
		return MessageBuilder.fillFromXML(message, xmlText);
	}
	public static String getXMLFromMessage(ManagedMessage msg) throws MessageException {
		return MessageBuilder.buildToXML(msg);
	}
	public static String getXMLFromMessageContent(Multipart content) throws MessageException {
        if( content == null )
        	return null;
        	
		try {
			return MessageBuilder.buildToXML(content);
		} catch (IOException e) {
			throw new MessageException(PasserelleException.Severity.NON_FATAL,"",content,e);
		} catch (MessagingException e) {
			throw new MessageException(PasserelleException.Severity.NON_FATAL,"",content,e);
		}
	}
}
