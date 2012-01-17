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
import java.sql.SQLException;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InsertQueryPane extends JPanel {

  private static Logger logger = LoggerFactory.getLogger(InsertQueryPane.class);
  private JTabbedPane jTabbedPane1 = new JTabbedPane(JTabbedPane.TOP);
  private Connection connection = null;
  private DataBaseLogin p1;
  private InsertForm insertForm;
  private String initMapping;
  private String initTable;

  public InsertQueryPane(Connection connection) {
    try {
      this.connection = connection;
      jbInit();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private boolean testConnection() {

    boolean ok = true;

    try {
      if (getConnection().getWarnings() != null) {
        ok = false;
      } else {
        try {
          getConnection().getMetaData();
        } catch (Exception exception) {
          ok = false;
        }
      }
    } catch (SQLException e) {
      logger.error("", e);
      return false;
    }
    return ok;
  }

  private void jbInit() throws Exception {
    this.setLayout(null);
    p1 = new DataBaseLogin();
    jTabbedPane1.addTab("Connection", p1);
    p1.connect.addActionListener(new ActionListener() {
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
    if (initMapping != null && initMapping.length() > 0) {
      insertForm = new InsertForm(initTable, initMapping, connection, getSchema());
    } else {
      insertForm = new InsertForm(connection, getSchema());
    }
    jTabbedPane1.addTab("Mapping", insertForm);
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

      InsertQueryPane demo = new InsertQueryPane(null);
      frame.getContentPane().add(demo);
      frame.pack();
      frame.setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getUser() {
    return p1.getUser();
  }

  public String getPassword() {
    return p1.getPassword();
  }

  public String getDriver() {
    return p1.getDriver();
  }

  public String getUrl() {
    return p1.getUrl();
  }

  public String getSchema() {
    return p1.getSchema();
  }

  public String getMappingAsXml() {
    if (insertForm != null)
      return insertForm.getMappingAsXml();
    else
      return "";
  }

  public String getTable() {
    if (insertForm != null)
      return insertForm.getDbTable();
    else
      return "";
  }

  public void setUser(String user) {
    p1.setUser(user);
  }

  public void setPassword(String password) {
    p1.setPassword(password);
  }

  public void setDriver(String driver) {
    p1.setDriver(driver);
  }

  public void setUrl(String url) {
    p1.setUrl(url);
  }

  public void setSchema(String schema) {
    p1.setSchema(schema);
  }

  /**
   * Returns the initQuery.
   * 
   * @return String
   */
  public String getInitMapping() {
    return initMapping;
  }

  /**
   * Sets the initQuery.
   * 
   * @param initQuery The initQuery to set
   */
  public void setInitMapping(String initMapping) {
    this.initMapping = initMapping;
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

}