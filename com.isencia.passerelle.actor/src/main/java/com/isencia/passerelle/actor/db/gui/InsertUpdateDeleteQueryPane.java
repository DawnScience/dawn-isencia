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
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsertUpdateDeleteQueryPane extends JPanel {
  private JTabbedPane jTabbedPane1 = new JTabbedPane(JTabbedPane.TOP);
  private Connection connection = null;
  private DataBaseLogin login;
  private InsertUpdateDeleteForm form;
  private String initCollumnMapping;
  private String initConditionMapping;
  private String initTable;
  private int initQueryType;
  private static Logger logger = LoggerFactory.getLogger(InsertUpdateDeleteQueryPane.class);

  public InsertUpdateDeleteQueryPane() {
    try {
      jbInit();
    } catch (Exception e) {
      logger.error("Error creating pane", e);
    }
  }

  private boolean testConnection() {
    String msg = "Connection established";
    boolean ok = true;
    try {
      getConnection().getMetaData();
    } catch (Exception exception) {
      // msg=exception.getMessage().toString();
      logger.error(msg, exception);
      ok = false;
    }
    return ok;
  }

  private void jbInit() throws Exception {
    this.setLayout(null);
    login = new DataBaseLogin();
    jTabbedPane1.addTab("Connection", login);
    login.connect.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        button_pressed();
      }
    });
    jTabbedPane1.setBounds(5, 4, 550, 350);
    this.add(jTabbedPane1, null);
  }

  public void button_pressed() {
    if (jTabbedPane1.getTabCount() > 1) {
      for (int i = jTabbedPane1.getTabCount(); i > 1; i--) {
        jTabbedPane1.removeTabAt(i - 1);
      }
    }
    if (!testConnection()) return;
    if (initCollumnMapping != null && initCollumnMapping.length() > 0) {
      form = new InsertUpdateDeleteForm(initTable, initQueryType, initCollumnMapping, initConditionMapping, connection, getSchema());
    } else {
      form = new InsertUpdateDeleteForm(connection, getSchema());
    }
    jTabbedPane1.addTab("Mapping", form);
    jTabbedPane1.setSelectedIndex(1);
  }

  /**
   * Returns the connection.
   * 
   * @return DbConnection
   */
  public Connection getConnection() {
    return connection;
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
    WindowListener l = new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    };
    try {
      JFrame frame = new JFrame("");
      frame.addWindowListener(l);
      InsertUpdateDeleteQueryPane demo = new InsertUpdateDeleteQueryPane();
      frame.getContentPane().add(demo);
      frame.pack();
      frame.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getUser() {
    return login.getUser();
  }

  public String getPassword() {
    return login.getPassword();
  }

  public String getDriver() {
    return login.getDriver();
  }

  public String getUrl() {
    return login.getUrl();
  }

  public String getSchema() {
    return login.getSchema();
  }

  public String getCollumnMappingAsXml() {
    if (form != null)
      return form.getColumnMappingAsXml();
    else
      return "";
  }

  public String getConditionMappingAsXml() {
    if (form != null)
      return form.getConditionMappingAsXml();
    else
      return "";
  }

  public String getTable() {
    if (form != null)
      return form.getSelectedTable();
    else
      return "";
  }

  public int getQueryType() {
    if (form != null)
      return form.getQueryType();
    else
      return -1;
  }

  public void setUser(String user) {
    login.setUser(user);
  }

  public void setPassword(String password) {
    login.setPassword(password);
  }

  public void setDriver(String driver) {
    login.setDriver(driver);
  }

  public void setUrl(String url) {
    login.setUrl(url);
  }

  public void setSchema(String schema) {
    login.setSchema(schema);
  }

  /**
   * Returns the initTable.
   * 
   * @return String
   */
  public String getInitTable() {
    return initTable;
  }

  /**
   * Sets the initTable.
   * 
   * @param initTable The initTable to set
   */
  public void setInitTable(String initTable) {
    this.initTable = initTable;
  }

  /**
   * Returns the initColumnMapping.
   * 
   * @return String
   */
  public String getInitCollumnMapping() {
    return initCollumnMapping;
  }

  /**
   * Returns the initConditionMapping.
   * 
   * @return String
   */
  public String getInitConditionMapping() {
    return initConditionMapping;
  }

  /**
   * Sets the initColumnMapping.
   * 
   * @param initColumnMapping The initColumnMapping to set
   */
  public void setInitCollumnMapping(String initCollumnMapping) {
    this.initCollumnMapping = initCollumnMapping;
  }

  /**
   * Sets the initConditionMapping.
   * 
   * @param initConditionMapping The initConditionMapping to set
   */
  public void setInitConditionMapping(String initConditionMapping) {
    this.initConditionMapping = initConditionMapping;
  }

  /**
   * Returns the initQueryType.
   * 
   * @return String
   */
  public int getInitQueryType() {
    return initQueryType;
  }

  /**
   * Sets the initQueryType.
   * 
   * @param initQueryType The initQueryType to set
   */
  public void setInitQueryType(int initQueryType) {
    this.initQueryType = initQueryType;
  }

}