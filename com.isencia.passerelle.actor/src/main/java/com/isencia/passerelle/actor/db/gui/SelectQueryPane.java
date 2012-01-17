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
package com.isencia.passerelle.actor.db.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SelectQueryPane extends JPanel {

  private JTabbedPane jTabbedPane1 = new JTabbedPane(JTabbedPane.TOP);
  private Connection connection = null;
  private DataBaseLogin dataBaseLogin;
  private QueryByExample qbe;
  private String initQuery;
  private static Logger logger = LoggerFactory.getLogger(SelectQueryPane.class);
  private ClassLoader classLoader = null;

  public SelectQueryPane() {

    try {
      classLoader = Thread.currentThread().getContextClassLoader();
      jbInit();
    } catch (Exception e) {
      logger.error("", e);
    }
  }

  private boolean testConnection() {

    String msg = "Connection established";
    boolean ok = true;

    try {

      if (getConnection().getWarnings() != null) {
        msg = getConnection().getWarnings().getMessage();
        ok = true;
        logger.info(msg);
      } else {
        try {
          getConnection().getMetaData();
        } catch (Exception exception) {
          msg = exception.getMessage().toString();
          ok = false;
        }
      }
    } catch (Exception e) {
      logger.error("", e);
      return false;
    }
    return ok;
  }

  private void jbInit() throws Exception {
    this.setLayout(null);
    dataBaseLogin = new DataBaseLogin();
    jTabbedPane1.addTab("Connection", dataBaseLogin);

    dataBaseLogin.connect.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        connect();
      }
    });
    jTabbedPane1.setBounds(5, 10, 500, 350);
    this.add(jTabbedPane1, null);
  }

  public void connect() {
    if (jTabbedPane1.getTabCount() > 1) {
      for (int i = jTabbedPane1.getTabCount(); i > 1; i--) {
        jTabbedPane1.removeTabAt(i - 1);
      }
    }
    if (!testConnection()) return;
    if (initQuery != null && initQuery.length() > 0) {
      qbe = new QueryByExample(initQuery, connection, dataBaseLogin.getSchema());
    } else {
      qbe = new QueryByExample(connection, dataBaseLogin.getSchema());
    }
    jTabbedPane1.addTab("Query", qbe);
    jTabbedPane1.setSelectedIndex(1);
  }

  /**
   * Returns the connection.
   * 
   * @return DbConnection
   */
  public Connection getConnection() {
    if (connection == null) {
      try {
        connection = getDBConnection(dataBaseLogin.getUrl(), dataBaseLogin.getUser(), dataBaseLogin.getPassword(), dataBaseLogin.getDriver());
      } catch (Exception e) {
        logger.error("", e);
      }
    }
    return connection;
  }

  protected Connection getDBConnection(String url, String user, String password, String driver) throws ClassNotFoundException, SQLException,
      InstantiationException, IllegalAccessException {
    // This special construction to load the driver class is needed
    // to ensure that the driver class can be found while using the passerelle
    // eclipse IDE.
    // I.e. to ensure that the class is loaded from an "application-level"
    // classloader and
    // not by the class loader of our eclipse plugin(s).
    // This last one is in general not able to find the driver jars, as they are
    // typically placed
    // in a project in the passerelle IDE.

    Driver sqlDriver = (Driver) classLoader.loadClass(getDriver()).newInstance();
    Properties properties = new Properties();
    properties.put("user", getUser());
    properties.put("password", getPassword());
    return sqlDriver.connect(getUrl(), properties);
  }

  /**
   * Sets the connection.
   * 
   * @param connection The connection to set
   */
  public void setConnection(Connection connection) {
    this.connection = connection;
  }

  public static void main(String[] args) {

    WindowListener listener = new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    };

    try {
      JFrame frame = new JFrame("");
      frame.addWindowListener(listener);

      SelectQueryPane demo = new SelectQueryPane();
      frame.getContentPane().add(demo);
      // frame.pack();
      frame.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getUser() {
    return dataBaseLogin.getUser();
  }

  public String getPassword() {
    return dataBaseLogin.getPassword();
  }

  public String getDriver() {
    return dataBaseLogin.getDriver();
  }

  public String getUrl() {
    return dataBaseLogin.getUrl();
  }

  public String getSchema() {
    return dataBaseLogin.getSchema();
  }

  public String getQuery() {
    if (qbe != null)
      return qbe.getQuery();
    else
      return "";
  }

  public void setSchema(String schema) {
    dataBaseLogin.setSchema(schema);
  }

  public void setUser(String user) {
    dataBaseLogin.setUser(user);
  }

  public void setPassword(String password) {
    dataBaseLogin.setPassword(password);
  }

  public void setDriver(String driver) {
    dataBaseLogin.setDriver(driver);
  }

  public void setUrl(String url) {
    dataBaseLogin.setUrl(url);
  }

  /**
   * Returns the initQuery.
   * 
   * @return String
   */
  public String getInitQuery() {
    return initQuery;
  }

  /**
   * Sets the initQuery.
   * 
   * @param initQuery The initQuery to set
   */
  public void setInitQuery(String initQuery) {
    this.initQuery = initQuery;
  }

  public void close() {
    if (qbe != null) qbe.closeConnection();
  }

}