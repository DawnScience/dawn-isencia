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

import java.util.Arrays;
import java.util.List;
import javax.mail.URLName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.actor.gui.style.PasswordStyle;
import ptolemy.data.StringToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.message.IReceiverChannel;
import com.isencia.message.interceptor.IMessageInterceptorChain;
import com.isencia.message.interceptor.MessageInterceptorChain;
import com.isencia.message.mail.MailReceiverChannel;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TriggeredChannelSource;
import com.isencia.passerelle.message.interceptor.TextToMessageConverter;

/**
 * A source that uses the POP or the IMAP protocol to read e-mail data from a
 * server. The source does not remove the e-mails from the server!
 * 
 * @author dirk
 */
public class MailReceiver extends TriggeredChannelSource {

  private static final String NOT_APPLICABLE = "N/A";
  private static Logger logger = LoggerFactory.getLogger(MailReceiver.class);

  private final static String[] protocolChoices = new String[] { "pop3", "imap" };
  private final static String[] msgTypeChoices = new String[] { "Messages", "Message Count" };
  private final static String[] msgStructChoices = new String[] { "Content", "Content & Attachments" };
  private final static String[] countTypeChoices = new String[] { "All", "Unread", "New" };
  private final static String[] contentTypeChoices = new String[] { "Text", "HTML", "All" };

  private final static List protocols = Arrays.asList(protocolChoices);
  private final static List msgTypes = Arrays.asList(msgTypeChoices);
  private final static List msgStructs = Arrays.asList(msgStructChoices);
  private final static List countTypes = Arrays.asList(countTypeChoices);
  private final static List contentTypes = Arrays.asList(contentTypeChoices);

  public final static int MESSAGETYPE_MESSAGE = 0;
  public final static int MESSAGETYPE_COUNT = 1;

  public final static int MESSAGETYPE_CONTENT = 0;
  public final static int MESSAGETYPE_CONTENT_ATTACHMENTS = 1;

  public final static int CONTENTTYPE_ALL = 0;
  public final static int CONTENTTYPE_TEXT = 1;
  public final static int CONTENTTYPE_HTML = 2;

  public final static int MESSAGECOUNTTYPE_ALL = 0;
  public final static int MESSAGECOUNTTYPE_UNREAD = 1;
  public final static int MESSAGECOUNTTYPE_NEW = 2;

  public final static String USER_PARAM = "User";
  public final static String PASSWORD_PARAM = "Password";
  public final static String HOST_PARAM = "Host";
  public final static String PROTOCOL_PARAM = "Protocol";
  public final static String MESSAGETYPE_PARAM = "What to Read";
  public final static String MESSAGESUBTYPE_PARAM = "Filter";
  public final static String CONTENTTYPE_PARAM = "Content Filter";

  // /////////////////////////////////////////////////////////////////
  // // ports and parameters ////

  public Parameter userParam;
  public Parameter passwordParam;
  public Parameter hostParam;
  public Parameter protocolParam;

  public Parameter messageTypeParam;
  public Parameter messageSubTypeParam;
  public Parameter contentTypeParam;

  private URLName server = null;
  private String user = "";
  private String password = "";
  private String host = "";
  private String protocol = protocolChoices[0];
  private boolean attachment = false;
  private boolean content = true; // currently we always pop content!
  private int contentType = 0;
  private int messageType = 0;
  private int messageCountType = 0;

  /**
   * MailReceiver constructor.
   * 
   * @param container ptolemy.kernel.CompositeEntity
   * @param name java.lang.String
   * @exception ptolemy.kernel.util.IllegalActionException The exception
   *              description.
   * @exception ptolemy.kernel.util.NameDuplicationException The exception
   *              description.
   */
  public MailReceiver(ptolemy.kernel.CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);

    // User
    String user = System.getProperty("mail.user", "");
    if (user == null || user.length() == 0) user = System.getProperty("user.name", "user");
    userParam = new StringParameter(this, USER_PARAM);
    userParam.setExpression(user);

    passwordParam = new StringParameter(this, PASSWORD_PARAM);
    new PasswordStyle(passwordParam, "_passwordStyle");

    hostParam = new StringParameter(this, HOST_PARAM);
    hostParam.setExpression(System.getProperty("mail.pop3.host", "your mail host"));

    protocolParam = new StringParameter(this, PROTOCOL_PARAM);
    protocolParam.setExpression((String) protocols.get(0));
    for (int i = 0; i < protocols.size(); ++i) {
      protocolParam.addChoice((String) protocols.get(i));
    }

    messageTypeParam = new StringParameter(this, MESSAGETYPE_PARAM);
    messageTypeParam.setExpression((String) msgTypes.get(0));
    for (int i = 0; i < msgTypes.size(); ++i) {
      messageTypeParam.addChoice((String) msgTypes.get(i));
    }

    messageSubTypeParam = new StringParameter(this, MESSAGESUBTYPE_PARAM);
    // show choices for default type above, i.e. "Messages"
    messageSubTypeParam.setExpression((String) msgStructs.get(0));
    for (int i = 0; i < msgStructs.size(); ++i) {
      messageSubTypeParam.addChoice((String) msgStructs.get(i));
    }

    contentTypeParam = new StringParameter(this, CONTENTTYPE_PARAM);
    // show choices for default type above, i.e. "Content"
    contentTypeParam.setExpression((String) contentTypes.get(0));
    for (int i = 0; i < contentTypes.size(); ++i) {
      contentTypeParam.addChoice((String) contentTypes.get(i));
    }

  }

  /**
   * @param attribute The attribute that changed.
   * @exception IllegalActionException
   */
  public void attributeChanged(Attribute attribute) throws IllegalActionException {

    if (logger.isTraceEnabled()) logger.trace(getInfo() + " :" + attribute);

    if (attribute == userParam) {
      StringToken userToken = (StringToken) userParam.getToken();
      if (userToken != null) {
        user = userToken.stringValue();
        logger.debug("User changed to : " + user);
        setServerURL();
      }
    } else if (attribute == passwordParam) {
      StringToken passwordToken = (StringToken) passwordParam.getToken();
      if (passwordToken != null) {
        password = passwordToken.stringValue();
        setServerURL();
      }
    } else if (attribute == hostParam) {
      StringToken hostToken = (StringToken) hostParam.getToken();
      if (hostToken != null) {
        host = hostToken.stringValue();
        logger.debug("Host changed to : " + host);
        setServerURL();
      }
    } else if (attribute == protocolParam) {
      String protocolChoice = protocolParam.getExpression();
      String prevProtocol = getProtocol();
      int protocolIndex = protocols.indexOf(protocolChoice);
      if (protocolIndex < 0) {
        contentTypeParam.setExpression(prevProtocol);
      } else {
        setProtocol(protocolChoice);
        logger.debug("Protocol changed to : " + protocolChoice);
        setServerURL();
      }
    } else if (attribute == contentTypeParam) {
      if (getMessageType() == MESSAGETYPE_MESSAGE) {
        String contentTypeChoice = contentTypeParam.getExpression();
        int prevContentType = getMessageType();
        int contentType = contentTypes.indexOf(contentTypeChoice);
        if (contentType < 0) {
          contentTypeParam.setExpression((String) contentTypes.get(prevContentType));
        } else {
          setContentType(contentType);
          logger.debug("Content Type changed to : " + contentTypeChoice);
        }
      } else {
        // don't bother, it's not relevant anyway
        contentTypeParam.setExpression(NOT_APPLICABLE);
        setContentType(-1);
      }
    } else if (attribute == messageSubTypeParam) {
      String msgSubTypeChoice = messageSubTypeParam.getExpression();
      int msgType = getMessageType();
      if (msgType == MESSAGETYPE_MESSAGE) {
        boolean prevNeedAttachments = getAttachments();
        int msgSubType = contentTypes.indexOf(msgSubTypeChoice);
        boolean needAttachments = (msgSubType == MESSAGETYPE_CONTENT_ATTACHMENTS);
        if (msgSubType < 0) {
          messageSubTypeParam.setExpression((String) contentTypes.get(0));
        } else if (needAttachments != prevNeedAttachments) {
          setAttachments(needAttachments);
          logger.debug("Attachments changed to : " + needAttachments);
        }
      } else if (msgType == MESSAGETYPE_COUNT) {
        int prevMsgSubType = getMessageCountType();
        int msgSubType = countTypes.indexOf(msgSubTypeChoice);
        if (msgSubType < 0) {
          messageSubTypeParam.setExpression((String) countTypes.get(0));
        } else if (msgSubType != prevMsgSubType) {
          setMessageCountType(msgSubType);
          logger.debug("CountType changed to : " + msgSubTypeChoice);
        }
      }
    } else if (attribute == messageTypeParam) {
      String msgTypeChoice = messageTypeParam.getExpression();
      int prevMsgType = getMessageType();
      int msgType = msgTypes.indexOf(msgTypeChoice);
      if (msgType < 0) {
        messageTypeParam.setExpression((String) msgTypes.get(prevMsgType));
      } else if (msgType != prevMsgType) {
        setMessageType(msgType);
        logger.debug("MessageType changed to : " + msgTypeChoice);
        // need to ensure that correct subtype choices are shown
        if (msgType == MESSAGETYPE_MESSAGE) {
          String prevMsgSubTypeChoice = messageSubTypeParam.getExpression();
          String prevContentTypeChoice = contentTypeParam.getExpression();
          messageSubTypeParam.removeAllChoices();
          for (int i = 0; i < msgStructs.size(); ++i) {
            messageSubTypeParam.addChoice((String) msgStructs.get(i));
          }
          // hack needed to ensure correct initialization during load from
          // file.....
          // and need to change the expression in order to trigger the limited
          // Ptolemy UI refresh support
          messageSubTypeParam.setExpression((String) msgStructs.get(0));
          messageSubTypeParam.setExpression(prevMsgSubTypeChoice);

          contentTypeParam.removeAllChoices();
          for (int i = 0; i < contentTypes.size(); ++i) {
            contentTypeParam.addChoice((String) contentTypes.get(i));
          }
          // hack needed to ensure correct initialization during load from
          // file.....
          // and need to change the expression in order to trigger the limited
          // Ptolemy UI refresh support
          contentTypeParam.setExpression((String) contentTypes.get(0));
          contentTypeParam.setExpression(prevContentTypeChoice);

        } else if (msgType == MESSAGETYPE_COUNT) {
          String prevMsgSubTypeChoice = messageSubTypeParam.getExpression();
          messageSubTypeParam.removeAllChoices();
          for (int i = 0; i < countTypes.size(); ++i) {
            messageSubTypeParam.addChoice((String) countTypes.get(i));
          }
          // hack needed to ensure correct initialization during load from
          // file.....
          // and need to change the expression in order to trigger the limited
          // Ptolemy UI refresh support
          messageSubTypeParam.setExpression((String) countTypes.get(0));
          messageSubTypeParam.setExpression(prevMsgSubTypeChoice);

          contentTypeParam.removeAllChoices();
          contentTypeParam.addChoice(NOT_APPLICABLE);
          // hack needed to ensure correct initialization during load from
          // file.....
          // and need to change the expression in order to trigger the limited
          // Ptolemy UI refresh support
          contentTypeParam.setExpression("tmp");
          contentTypeParam.setExpression(NOT_APPLICABLE);
        }
      }
    } else
      super.attributeChanged(attribute);

    if (logger.isTraceEnabled()) logger.trace(getInfo() + " - exit ");
  }

  private void setServerURL() {
    server = new URLName(getProtocol() + "://" + user + ":" + password + "@" + host + "/INBOX");
  }

  protected IMessageInterceptorChain createInterceptorChain() {

    IMessageInterceptorChain interceptors = new MessageInterceptorChain();
    if (getMessageType() == MESSAGETYPE_MESSAGE) {
      MailMessageToMessageConverter convertor = new MailMessageToMessageConverter();
      if (getContent()) {
        switch (contentType) {
        case CONTENTTYPE_TEXT:
          convertor.setContentTypes(new String[] { "text/plain" });
          logger.debug("Content type filter set to text/plain");
          break;
        case CONTENTTYPE_HTML:
          convertor.setContentTypes(new String[] { "text/html" });
          logger.debug("Content type filter set to text/html");
          break;
        case CONTENTTYPE_ALL:
          convertor.setContentTypes(new String[] { "text/plain", "text/html" });
          logger.debug("Content type filter set to all");
          break;
        }
      } else
        convertor.setContentTypes(null);

      interceptors.add(convertor);
    } else
      interceptors.add(new TextToMessageConverter(this));

    return interceptors;
  }

  protected boolean doPreFire() throws ProcessingException {
    if (logger.isTraceEnabled()) logger.trace(getInfo());

    boolean res = true;
    if (!getChannel().isOpen()) {
      if (server == null) {
        requestFinish();
        res = false;
      }
    }

    if (res) res = super.doPreFire();

    if (logger.isTraceEnabled()) logger.trace(getInfo() + " - exit " + " :" + res);

    return res;
  }

  protected String getExtendedInfo() {
    return user + "@" + host;
  }

  /**
   * Returns the attachments.
   * 
   * @return boolean
   */
  public boolean getAttachments() {
    return attachment;
  }

  /**
   * Returns the content.
   * 
   * @return boolean
   */
  public boolean getContent() {
    return content;
  }

  /**
   * Returns the contentType.
   * 
   * @return int
   */
  public int getContentType() {
    return contentType;
  }

  /**
   * Returns the messageType.
   * 
   * @return int
   */
  public int getMessageType() {
    return messageType;
  }

  /**
   * Sets the attachments.
   * 
   * @param attachments The attachments to set
   */
  public void setAttachments(boolean attachment) {
    this.attachment = attachment;
  }

  /**
   * Sets the content.
   * 
   * @param content The content to set
   */
  public void setContent(boolean content) {
    this.content = content;
  }

  /**
   * Sets the contentType.
   * 
   * @param contentType The contentType to set
   */
  public void setContentType(int contentType) {
    this.contentType = contentType;
  }

  /**
   * Sets the messageType.
   * 
   * @param messageType The messageType to set
   */
  public void setMessageType(int messageType) {
    this.messageType = messageType;
  }

  /**
   * Returns the messageCountType.
   * 
   * @return int
   */
  public int getMessageCountType() {
    return messageCountType;
  }

  /**
   * Sets the messageCountType.
   * 
   * @param messageCountType The messageCountType to set
   */
  public void setMessageCountType(int messageCountType) {
    this.messageCountType = messageCountType;
  }

  /**
   * @return Returns the protocol.
   */
  public String getProtocol() {
    return protocol;
  }

  /**
   * @param protocol The protocol to set.
   */
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  protected IReceiverChannel createChannel() {
    MailReceiverChannel channel = new MailReceiverChannel(server);
    if (getMessageType() == MESSAGETYPE_MESSAGE)
      channel.setMessageType(MailReceiverChannel.MESSAGES);
    else {
      switch (getMessageCountType()) {
      case MESSAGECOUNTTYPE_NEW:
        channel.setMessageType(MailReceiverChannel.NEW_MESSAGE_COUNT);
        break;
      case MESSAGECOUNTTYPE_UNREAD:
        channel.setMessageType(MailReceiverChannel.UNREAD_MESSAGE_COUNT);
        break;
      default:
        channel.setMessageType(MailReceiverChannel.MESSAGE_COUNT);
        break;
      }
    }

    return channel;
  }

}
