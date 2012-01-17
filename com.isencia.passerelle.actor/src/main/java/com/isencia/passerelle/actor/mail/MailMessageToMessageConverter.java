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

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import javax.mail.BodyPart;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.message.interceptor.IMessageInterceptor;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.message.MessageHelper;
import com.isencia.passerelle.message.internal.MessageContainer;
import com.isencia.passerelle.message.internal.PasserelleBodyPart;

/**
 * @author dirk To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates.
 */
public class MailMessageToMessageConverter implements IMessageInterceptor {

  private final static Logger logger = LoggerFactory.getLogger(MailMessageToMessageConverter.class);

  private String[] contentTypes = null;
  private boolean attachInMessage = false;

  public Object accept(Object message) throws Exception {
    if (logger.isTraceEnabled()) logger.trace("Accepting ~" + message.toString() + "~");

    ManagedMessage container = null;

    if (message != null) {

      container = MessageFactory.getInstance().createMessage();

      MimeMessage mailMessage = (MimeMessage) message;

      logger.debug("Creating new MessageContainer");

      Enumeration hdrEnum = mailMessage.getAllHeaders();
      while (hdrEnum.hasMoreElements()) {
        Header header = (Header) hdrEnum.nextElement();
        logger.debug("Name : " + header.getName() + ", Value : " + header.getValue());
        container.addBodyHeader(header.getName(), header.getValue());
      }

      // return immediately if only header info is required
      if (contentTypes == null && !attachInMessage) return container;

      Object content = mailMessage.getContent();

      if (content instanceof String) {
        logger.debug("Content is String");
        if (contentTypes != null) {
          if (MessageHelper.filterContent(mailMessage, contentTypes)) {
            logger.debug("Content is Valid Content Type");
            container.setBodyContentPlainText((String) content);
          }
        }
      } else if (content instanceof Multipart) {
        logger.debug("Content is Multipart");
        MultipartContentBuilder builder = new MultipartContentBuilder();
        Multipart mp = builder.build((Multipart) content, contentTypes, attachInMessage);
        if (mp != null) {
          logger.debug("Multipart added to BodyContent");
          ((MessageContainer) container).setBodyContent(mp);
        } else
          logger.debug("No Multipart added to BodyContent");

      } else if (content instanceof InputStream) logger.debug("Content is Inputstream");
    }
    return container;
  }

  private class MultipartContentBuilder {

    public Multipart build(Multipart mp, String[] contentTypeFilter, boolean attachments) throws IOException, MessagingException {

      logger.debug("Start building multipart");

      Multipart newMp = new MimeMultipart();

      int count = mp.getCount();
      logger.debug("Building Multipart, found " + count + " parts");
      for (int i = 0; i < count; i++) {
        BodyPart body = mp.getBodyPart(i);
        logger.debug("Bodypart " + i);
        BodyPartContentBuilder bodyPartBuilder = new BodyPartContentBuilder();
        logger.debug("Start Bodypart build");
        Part part = bodyPartBuilder.build(body, contentTypeFilter, attachments);
        if (part != null) {
          logger.debug("Bodypart is added");
          // cast from Part to BodyPart iso PasserelleBodyPart
          newMp.addBodyPart((BodyPart) part);
        }
      }
      // If no bodyparts where added, just return null
      if (newMp.getCount() == 0) return null;
      return newMp;
    }
  }

  private class BodyPartContentBuilder {

    public Part build(Part part, String[] contentTypeFilter, boolean attachments) throws IOException, MessagingException {
      Object content = part.getContent();

      if (content instanceof String) {
        boolean isAttach = !MessageHelper.isContent(part);
        if (isAttach) {
          logger.debug("Is attach");
          if (!attachments)
            return null;
          else {
            return part;
          }
        }

        logger.debug("Is no attach");
        if (!MessageHelper.filterContent(part, contentTypeFilter)) {
          logger.debug("Not a supported type");
          return null;
        }
        logger.debug("Supported type");
        return part;
      } else if (content instanceof InputStream) {
        logger.debug("Bodypart contains an inputstram");
        boolean isAttach = !MessageHelper.isContent(part);
        if (isAttach) {
          logger.debug("Is attach");
          if (!attachments)
            return null;
          else {
            logger.debug("Should send attach of Inputstream");
            PasserelleBodyPart newPart = new PasserelleBodyPart();
            newPart.setText("");
            MessageHelper.copyHeaders(part, newPart);
            newPart.saveChanges();
            logger.debug("return just the headers");
            return newPart;
          }
        }
        logger.debug("Is no attach");
        if (!MessageHelper.filterContent(part, contentTypeFilter)) {
          logger.debug("Not a supported type");
          return null;
        }
        logger.debug("Supported type");
        logger.debug("return inputstream in future");
      } else if (content instanceof Multipart) {
        logger.debug("Bodypart contains a multipart");
        MultipartContentBuilder builder = new MultipartContentBuilder();
        Multipart mp = builder.build((Multipart) content, contentTypeFilter, attachments);
        if (mp != null && mp.getCount() > 0) {
          PasserelleBodyPart newPart = new PasserelleBodyPart();
          newPart.setText("");
          MessageHelper.copyHeaders(part, newPart);
          newPart.setContent(mp);
          newPart.saveChanges();
          return newPart;
        }
      }
      return null;
    }
  }

  /**
   * Returns the contentTypes.
   * 
   * @return String[]
   */
  public String[] getContentTypes() {
    return contentTypes;
  }

  /**
   * Sets the contentTypes.
   * 
   * @param contentTypes The contentTypes to set
   */
  public void setContentTypes(String[] contentTypes) {
    this.contentTypes = contentTypes;
  }

  /**
   * Returns the attachInMessage.
   * 
   * @return boolean
   */
  public boolean isAttachInMessage() {
    return attachInMessage;
  }

  /**
   * Sets the attachInMessage.
   * 
   * @param attachInMessage The attachInMessage to set
   */
  public void setAttachInMessage(boolean attachInMessage) {
    this.attachInMessage = attachInMessage;
  }

}
