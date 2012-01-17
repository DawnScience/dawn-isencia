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
package com.isencia.passerelle.actor.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.message.ChannelException;
import com.isencia.message.IReceiverChannel;
import com.isencia.message.NoMoreMessagesException;
import com.isencia.message.db.DatabaseReceiverChannel;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.message.MessageFactory;

/**
 * <p>
 * This actor reads records from a JDBC source, using a query that can be
 * configured using a custom cfg panel containing a query builder wizard.
 * </p>
 * <p>
 * Result records are represented by a Map<String, Object>, where the keys
 * contain the field names and the values have the default Java type matching
 * the DB column type.
 * </p>
 * <p>
 * This actor is a source that sends all result records during its one and only
 * iteration before it wraps up.
 * </p>
 * <p>
 * In order to be able to actually connect to a DB, a corresponding JDBC client
 * driver jar-file must be installed on the classpath, typically under
 * PASSERELLE_HOME/lib/drivers.
 * </p>
 * REMARK : this is an initial version of this actor to provide DB connectivity
 * and demonstrate the usage/configuration of a custom cfg panel. <br>
 * Implementation details may still evolve, e.g. the way a result record is
 * represented in an output message etc.
 * 
 * @author wim
 */
public class TableReader extends Actor {

  private static final long serialVersionUID = 1L;

  private static Logger logger = LoggerFactory.getLogger(TableReader.class);

  public final static String USER_PARAM = "user";
  public final static String PASSWORD_PARAM = "password";
  public final static String DRIVER_PARAM = "driver";
  public final static String URL_PARAM = "url";
  public final static String QUERY_PARAM = "query";
  public final static String DB_PARAM = "db";
  public final static String SCHEMA_PARAM = "schema";
  public final static String DATABASE_PARAM = "database";
  public final static String HOST_PARAM = "host";
  public final static String PORT_PARAM = "param";

  public Parameter userParam;
  private String user = null;
  private String password;
  public Parameter passwordParam;
  private String driver;
  public Parameter driverParam;
  private String url;
  public Parameter urlParam;
  private String query;
  public Parameter queryParam;
  private String schema;
  public Parameter schemaParam;
  private IReceiverChannel channel;

  public Port output;

  private Long sequenceID;

  public TableReader(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);
    output = PortFactory.getInstance().createOutputPort(this);
    userParam = new StringParameter(this, USER_PARAM);
    passwordParam = new StringParameter(this, PASSWORD_PARAM);
    driverParam = new StringParameter(this, DRIVER_PARAM);
    urlParam = new StringParameter(this, URL_PARAM);
    queryParam = new StringParameter(this, QUERY_PARAM);
    schemaParam = new StringParameter(this, SCHEMA_PARAM);

    registerConfigurableParameter(userParam);
    registerConfigurableParameter(passwordParam);
    registerConfigurableParameter(driverParam);
    registerConfigurableParameter(urlParam);
    registerConfigurableParameter(queryParam);
    registerConfigurableParameter(schemaParam);

    _attachText("_iconDescription", "<svg>\n" + "<rect x=\"-20\" y=\"-20\" width=\"40\" " + "height=\"40\" style=\"fill:orange;stroke:orange\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"19\" y2=\"-19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"-19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"20\" y1=\"-19\" x2=\"20\" y2=\"20\" " + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"-19\" y1=\"20\" x2=\"20\" y2=\"20\" "
        + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"19\" y1=\"-18\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n"
        + "<line x1=\"-18\" y1=\"19\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n" + "<circle cx=\"0\" cy=\"0\" r=\"10\""
        + "style=\"fill:white;stroke-width:2.0\"/>\n" + "<line x1=\"-15\" y1=\"0\" x2=\"0\" y2=\"0\" " + "style=\"stroke-width:2.0\"/>\n"
        + "<line x1=\"-3\" y1=\"-3\" x2=\"0\" y2=\"0\" " + "style=\"stroke-width:2.0\"/>\n" + "<line x1=\"-3\" y1=\"3\" x2=\"0\" y2=\"0\" "
        + "style=\"stroke-width:2.0\"/>\n" + "</svg>\n");
  }

  /**
   * @param attribute The attribute that changed.
   * @exception IllegalActionException
   */
  @Override
  public void attributeChanged(Attribute attribute) throws IllegalActionException {

    if (attribute == userParam) {
      String paramStr = userParam.getExpression();
      if (paramStr != null && paramStr.length() > 0) {
        user = paramStr;
      }
    } else if (attribute == passwordParam) {
      String paramStr = passwordParam.getExpression();
      if (paramStr != null && paramStr.length() > 0) {
        password = paramStr;
      }
    } else if (attribute == driverParam) {
      String paramStr = driverParam.getExpression();
      if (paramStr != null && paramStr.length() > 0) {
        driver = paramStr;
      }
    } else if (attribute == urlParam) {
      String paramStr = urlParam.getExpression();
      if (paramStr != null && paramStr.length() > 0) {
        url = paramStr;
      }
    } else if (attribute == queryParam) {
      String paramStr = queryParam.getExpression();
      if (paramStr != null && paramStr.length() > 0) {
        query = paramStr;
      }
    } else if (attribute == schemaParam) {
      String paramStr = schemaParam.getExpression();
      if (paramStr != null && paramStr.length() > 0) {
        schema = paramStr;
      }
    } else
      super.attributeChanged(attribute);

  }

  protected void closeChannel(IReceiverChannel channel) throws ChannelException {
    if ((channel != null) && channel.isOpen()) {
      channel.close();
    }
  }

  protected IReceiverChannel createChannel() throws Exception {

    return new DatabaseReceiverChannel(getDBConnection(url, user, password, driver));
  }

  protected Connection getDBConnection(String url, String user, String password, String driver) throws ClassNotFoundException, SQLException {
    Class.forName(driver);
    return DriverManager.getConnection(url, user, password);
  }

  @Override
  protected boolean doPostFire() throws ProcessingException {
    try {
      closeChannel(channel);
      requestFinish();
    } catch (ChannelException e) {
      throw new ProcessingException("Receiver channel for " + getInfo() + " not closed correctly.", getChannel(), e);
    }

    boolean res = super.doPostFire();

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit " + " :" + res);
    }

    return res;
  }

  @Override
  protected boolean doPreFire() throws ProcessingException {

    if (!super.doPreFire()) {
      return false;
    }

    sequenceID = MessageFactory.getInstance().createSequenceID();
    try {
      channel = createChannel();
      ((DatabaseReceiverChannel) channel).setQuery(query);
      openChannel(channel);
    } catch (Exception e) {
      throw new ProcessingException(PasserelleException.Severity.FATAL, "Receiver channel for " + getInfo() + " not opened correctly.", channel, e);
    }
    return true;
  }

  protected IReceiverChannel getChannel() {
    return channel;
  }

  @Override
  protected String getExtendedInfo() {
    return url;
  }

  protected void openChannel(IReceiverChannel channel) throws ChannelException {
    if ((channel != null) && !channel.isOpen()) {
      channel.open();
    }
  }

  @Override
  protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
    try {
      Object channelMsg = null;
      try {
        channelMsg = channel.getMessage();
      } catch (NoMoreMessagesException e1) {
      }
      int i = 0;
      while (channelMsg != null) {
        Object nextMessage;
        try {
          nextMessage = channel.getMessage();
        } catch (NoMoreMessagesException e) {
          nextMessage = null;
        }
        ManagedMessage message = MessageFactory.getInstance().createMessageInSequence(sequenceID, new Long(i++), nextMessage != null);
        message.setBodyContent(channelMsg, "Map");
        response.addOutputMessage(output, message);
        channelMsg = nextMessage;
      }
    } catch (ChannelException e) {
      throw new ProcessingException("Error retrieving message from channel", "", e);
    } catch (MessageException e) {
      throw new ProcessingException("Error creating message", "", e);
    }
  }

}