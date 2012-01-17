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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.util.swing.components.FfAlignerLayoutPanel;
import com.isencia.util.swing.components.LoadImage;

public class DataBaseLogin extends JPanel {
  private static Logger logger = LoggerFactory.getLogger(DataBaseLogin.class);
  Connection connection;
  String user;
  String password;
  String driver;
  String url;
  String schema;
  public JButton connect;
  private JTextField lUser;
  private JTextField lSchema;
  private JTextField lJDBCDriver;
  private JTextField lUrl;
  public JPasswordField lPassword;

  public DataBaseLogin() {
    init();
  }

  /**
   * Get the value of user.
   * 
   * @return Value of user.
   */
  public String getUser() {
    user = lUser.getText();

    return user;
  }

  /**
   * Set the value of user.
   * 
   * @param v Value to assign to user.
   */
  public void setUser(String v) {
    this.user = v;
    lUser.setText(v);
  }

  /**
   * Get the value of password.
   * 
   * @return Value of password.
   */
  public String getPassword() {
    password = String.valueOf(lPassword.getPassword());

    return password;
  }

  /**
   * Set the value of password.
   * 
   * @param v Value to assign to password.
   */
  public void setPassword(String v) {
    this.password = v;
    lPassword.setText(v);
  }

  /**
   * Get the value of JDBCdriver.
   * 
   * @return Value of JDBCdriver.
   */
  public String getDriver() {
    driver = lJDBCDriver.getText();

    return driver;
  }

  /**
   * Set the value of JDBCdriver.
   * 
   * @param v Value to assign to JDBCdriver.
   */
  public void setDriver(String v) {
    this.driver = v;
    lJDBCDriver.setText(v);
  }

  /**
   * Get the value of JDBCurl.
   * 
   * @return Value of JDBCurl.
   */
  public String getUrl() {
    url = lUrl.getText();

    return url;
  }

  /**
   * Set the value of JDBCurl.
   * 
   * @param v Value to assign to JDBCurl.
   */
  public void setUrl(String v) {
    this.url = v;
    lUrl.setText(v);
  }

  public String getSchema() {
    schema = lSchema.getText();
    return schema;
  }

  public void setSchema(String schema) {
    this.schema = schema;
    lSchema.setText(schema);
  }

  /**
   * propFile is the File containing all the Properties
   **/
  private void init() {
    this.setLayout(new BorderLayout());

    FfAlignerLayoutPanel ffp = new FfAlignerLayoutPanel();
    ffp.setBorder(BorderFactory.createEtchedBorder());
    lUser = new JTextField(20);
    lJDBCDriver = new JTextField(20);
    lUrl = new JTextField(20);
    lSchema = new JTextField(20);
    lPassword = new JPasswordField(20);
    ffp.addComponent(new JLabel("User"));
    ffp.addComponent(lUser);
    ffp.addComponent(new JLabel("Password"));
    ffp.addComponent(lPassword);

    FfAlignerLayoutPanel ffp2 = new FfAlignerLayoutPanel();
    ffp2.setBorder(BorderFactory.createEtchedBorder());
    ffp2.addComponent(new JLabel("JDBC-Driver"));
    ffp2.addComponent(lJDBCDriver);
    ffp2.addComponent(new JLabel("Database URL"));
    ffp2.addComponent(lUrl);
    ffp2.addComponent(new JLabel("Schema Filter"));
    ffp2.addComponent(lSchema);

    this.add("North", ffp);
    this.add("Center", ffp2);
    lPassword.requestFocus();

    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout());
    buttonPanel.setBorder(BorderFactory.createEtchedBorder());
    connect = new JButton("Connect", LoadImage.getImage("plug.gif"));
    connect.setToolTipText("This stores the Info needed to connect to the Database");
    buttonPanel.add(connect);
    this.add("South", buttonPanel);
  }

  private ImageIcon loadImage(String image) {
    return LoadImage.getImage(image);
  }

  public static void main(String[] args) {
    JFrame f = new JFrame("TestWindow");
    f.getContentPane().add(new DataBaseLogin());
    f.addWindowListener(new WindowAdapter() {
      public void windowActivated(WindowEvent e) {
      }

      public void windowClosed(WindowEvent e) {
      }

      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }

      public void windowDeactivated(WindowEvent e) {
      }

      public void windowDeiconified(WindowEvent e) {
      }

      public void windowIconified(WindowEvent e) {
      }

      public void windowOpened(WindowEvent e) {
      }
    });
    f.pack();
    f.setVisible(true);
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

}