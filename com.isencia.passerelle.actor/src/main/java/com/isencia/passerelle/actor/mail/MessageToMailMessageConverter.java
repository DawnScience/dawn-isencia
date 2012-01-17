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
package com.isencia.passerelle.actor.mail;

import java.util.Properties;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.message.interceptor.IMessageInterceptor;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * @author dirk To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates.
 */
public class MessageToMailMessageConverter implements IMessageInterceptor {

  private final static Logger logger = LoggerFactory.getLogger(MessageToMailMessageConverter.class);

  private boolean passThrough = false;

  /**
   * Constructor for MessageToMailMessageConverter.
   */
  public MessageToMailMessageConverter(boolean passThrough) {
    super();

    this.passThrough = passThrough;
  }

  public Object accept(Object message) throws Exception {
    if (logger.isTraceEnabled()) logger.trace("Accepting ~" + message + "~");

    ManagedMessage managedMsg = (ManagedMessage) message;

    // Setup the session
    logger.debug("Getting Mailhost");
    Properties properties = new Properties();
    String[] mailhost = managedMsg.getBodyHeader(SMTPSender.MAILHOST_HEADER);
    if (mailhost != null && mailhost.length > 0 && mailhost[0].length() > 0) {
      logger.debug("Mailhost : " + mailhost[0]);
      properties.put("mail.host", mailhost[0]);
    }
    Session session = Session.getDefaultInstance(properties, null);

    Message mailMessage = new MimeMessage(session);

    // Set from address
    logger.debug("Getting from addresses");
    String[] from = managedMsg.getBodyHeader(SMTPSender.FROM_HEADER);
    if (from != null && from.length > 0 && from[0].length() > 0) {
      logger.debug("From : " + from[0]);
      mailMessage.setFrom(new InternetAddress(from[0]));
    }

    // Set to addresses
    logger.debug("Getting to addresses");
    String[] toRecipients = managedMsg.getBodyHeader(SMTPSender.TO_HEADER);
    if (toRecipients != null && toRecipients.length > 0) {
      logger.debug("To addresses count : " + toRecipients.length);
      InternetAddress[] addresses = new InternetAddress[toRecipients.length];
      for (int i = 0; i < toRecipients.length; i++) {
        logger.debug("Send to : " + toRecipients[i]);
        addresses[i] = new InternetAddress(toRecipients[i]);
      }
      mailMessage.setRecipients(javax.mail.Message.RecipientType.TO, addresses);
    }

    // Set cc addresses
    logger.debug("Getting cc addresses");
    String[] ccRecipients = managedMsg.getBodyHeader(SMTPSender.CC_HEADER);
    if (ccRecipients != null && ccRecipients.length > 0) {
      logger.debug("Cc addresses count : " + ccRecipients.length);
      InternetAddress[] addresses = new InternetAddress[ccRecipients.length];
      for (int i = 0; i < ccRecipients.length; i++) {
        logger.debug("Send cc : " + ccRecipients[i]);
        addresses[i] = new InternetAddress(ccRecipients[i]);
      }
      mailMessage.setRecipients(javax.mail.Message.RecipientType.CC, addresses);
    }

    // Set bcc addresses
    logger.debug("Getting bcc addresses");
    String[] bccRecipients = managedMsg.getBodyHeader(SMTPSender.BCC_HEADER);
    if (bccRecipients != null && bccRecipients.length > 0) {
      logger.debug("Bcc addresses count : " + bccRecipients.length);
      InternetAddress[] addresses = new InternetAddress[bccRecipients.length];
      for (int i = 0; i < bccRecipients.length; i++) {
        logger.debug("Send bcc : " + bccRecipients[i]);
        addresses[i] = new InternetAddress(bccRecipients[i]);
      }
      mailMessage.setRecipients(javax.mail.Message.RecipientType.BCC, addresses);
    }

    // Set Subject
    logger.debug("Getting subject");
    String[] subject = managedMsg.getBodyHeader(SMTPSender.SUBJECT_HEADER);
    if (subject != null && subject.length > 0 && subject[0].length() > 0) {
      logger.debug("Set Subject : " + subject[0]);
      mailMessage.setSubject(subject[0]);
    }

    if (passThrough) {
      mailMessage.setText(managedMsg.toString());
    } else {
      Object content = managedMsg.getBodyContent();
      if (content instanceof Multipart) {
        mailMessage.setContent((MimeMultipart) content);
      } else if (content instanceof String) {
        String[] contentType = managedMsg.getBodyHeader("Content-Type");
        if (contentType == null || contentType.length == 0)
          mailMessage.setText((String) content);
        else
          mailMessage.setContent(content, contentType[0]);
      }

    }
    mailMessage.saveChanges();

    if (logger.isTraceEnabled()) logger.trace("exit");

    return mailMessage;
  }

}
