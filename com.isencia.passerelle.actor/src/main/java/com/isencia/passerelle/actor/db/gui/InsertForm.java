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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

public class InsertForm extends JPanel {
  private JList tables;
  private String[] columnName;
  private String[] columnType;
  private int[] columnLength;
  private JTextField[] fieldSelector;
  private String host;
  private JPanel mainpanel;
  private JScrollPane formpanel;
  private Connection con = null;
  private String dbTable;
  private String initTable;
  private String initMapping;
  private boolean initializeMapping = true;
  private boolean initializeTable = true;
  private HashMap mappings = new HashMap();

  public InsertForm(Connection connection, String schema) {
    con = connection;
    setSchema(schema);
    initializeMapping = false;
    initializeTable = false;
    this.setLayout(new BorderLayout());
    this.add("West", selectTable());
    this.add("Center", mainPanel());
  }

  public InsertForm(String initTable, String mappingAsXml, Connection connection, String schema) {
    con = connection;
    this.initTable = initTable;
    this.initMapping = mappingAsXml;
    setSchema(schema);
    this.setLayout(new BorderLayout());
    this.add("West", selectTable());
    this.add("Center", mainPanel());
  }

  /**
   * Get the value of dbTable.
   * 
   * @return Value of dbTable.
   */
  public String getDbTable() {
    dbTable = tables.getSelectedValue().toString();
    return dbTable;
  }

  /**
   * Set the value of dbTable.
   * 
   * @param v Value to assign to dbTable.
   */
  public void setDbTable(String v) {
    this.dbTable = v;
  }

  String schema;

  /**
   * Get the value of schema.
   * 
   * @return Value of schema.
   */
  public String getSchema() {
    return schema;
  }

  /**
   * Set the value of schema.
   * 
   * @param v Value to assign to schema.
   */
  public void setSchema(String v) {
    this.schema = v;
  }

  private JPanel mainPanel() {
    mainpanel = new JPanel();
    JLabel header = new JLabel("COLUMNS - MESSAGEFIELDS");
    mainpanel.setLayout(new BoxLayout(mainpanel, BoxLayout.Y_AXIS));
    formpanel = formPanel();
    mainpanel.add(header);
    mainpanel.add(formpanel);
    return mainpanel;
  }

  /**
   * this is the List that contains the available tables in the Database
   */
  private JPanel selectTable() {
    String[] tableTypes = { "TABLE", "VIEW", "ALIAS", "SYNONYM" };
    Vector tableVector = new Vector();
    try {
      ResultSet rs = con.getMetaData().getTables(null, getSchema(), "%", tableTypes);
      while (rs.next()) {
        tableVector.addElement(rs.getString(3));
      }
    } catch (SQLException sqe) {
      System.out.println("\nError while reading Tables from MetaData\n");
      System.out.println("Exception: \n" + sqe.getMessage());
      System.out.println("\nError-Code: " + sqe.getErrorCode());
      System.out.println("\nSQL-State: " + sqe.getSQLState() + "\n");
    }
    JLabel header = new JLabel("TABLES");
    JPanel tableList = new JPanel();
    tableList.setLayout(new BoxLayout(tableList, BoxLayout.Y_AXIS));
    tableList.add(header);
    tables = new JList();
    tables.setListData(tableVector);
    if (initializeTable) {
      tables.setSelectedValue(initTable, true);
      initializeTable = false;
    } else {
      tables.setSelectedIndex(0);
    }
    // set the Listener onto the table-List
    ListSelectionListener selectionListener = new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent arg0) {
        doUpdate();
      }
    };
    tables.addListSelectionListener(selectionListener);
    tables.setVisibleRowCount(15);
    tables.setFixedCellWidth(200);
    JScrollPane listPane = new JScrollPane();
    listPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    listPane.getViewport().add(tables);

    tableList.add(listPane);
    return tableList;
  }

  private void doUpdate() {
    mainpanel.remove(formpanel);
    formpanel = formPanel();
    mainpanel.add("Center", formpanel);
    mainpanel.updateUI();
  }

  /**
   * this fills the columns with the actual selected table
   */
  private void setColumns() {

    int i = 0;
    try {
      // this is to count the columns to create the array...
      DatabaseMetaData md = con.getMetaData();
      ResultSet cc = md.getColumns(null, null, getDbTable(), "%");
      while (cc.next()) {
        i++;
      }
      ResultSet cols = md.getColumns(null, null, getDbTable(), "%");
      columnName = new String[i];
      columnType = new String[i];
      columnLength = new int[i];
      fieldSelector = new JTextField[i];
      i = 0;
      while (cols.next()) {
        columnName[i] = cols.getString(4);
        columnType[i] = cols.getString(5);
        columnLength[i] = cols.getInt(7);
        i++;
      }
    } catch (Exception exception) {
      exception.printStackTrace();
    }
  }

  private void buildMappingsFromXml() {
    Document mappingsDoc = null;
    SAXBuilder builder = null;
    builder = new SAXBuilder(false);
    try {
      mappingsDoc = builder.build(new StringReader("<?xml version=\"1.0\"?>" + initMapping));
      List mappingsList = mappingsDoc.getRootElement().getChildren();
      Iterator it = mappingsList.iterator();
      while (it.hasNext()) {
        Element mappingElem = (Element) it.next();
        Element dataBaseField = mappingElem.getChild("DatabaseField");
        Element messageField = mappingElem.getChild("MessageField");
        mappings.put(dataBaseField.getText(), messageField.getText());
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String getFieldMapping(String dataBaseField) {

    return (String) mappings.get(dataBaseField);
  }

  private JScrollPane formPanel() {

    setColumns();
    Insets insets = new Insets(2, 2, 2, 2);
    JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints1;

    if (initializeMapping) {
      buildMappingsFromXml();
    }
    for (int i = 0; i < columnName.length; i++) {
      gridBagConstraints1 = new GridBagConstraints();
      gridBagConstraints1.anchor = GridBagConstraints.NORTHEAST;
      gridBagConstraints1.insets = insets;
      panel.add((JLabel) new JLabel(columnName[i]), gridBagConstraints1);
      gridBagConstraints1 = new GridBagConstraints();
      gridBagConstraints1.gridwidth = 0;
      gridBagConstraints1.anchor = GridBagConstraints.WEST;
      gridBagConstraints1.insets = insets;
      fieldSelector[i] = new JTextField();
      fieldSelector[i].setColumns(15);
      if (initializeMapping) {
        fieldSelector[i].setText(getFieldMapping(columnName[i]));
      }
      fieldSelector[i].setAlignmentY(Component.LEFT_ALIGNMENT);
      panel.add(fieldSelector[i], gridBagConstraints1);
    }
    initializeMapping = false;
    JScrollPane scrollFpane = new JScrollPane();
    scrollFpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollFpane.getViewport().setExtentSize(new Dimension(150, 150));
    scrollFpane.setSize(150, 150);
    scrollFpane.getViewport().add(panel);

    return scrollFpane;
  }

  private void execQuery(String query) {
    try {
      con.createStatement().executeUpdate(query);
      con.commit();
    } catch (Exception excpt) {
    }
  }

  public String createInsertQuery() {
    String query;
    query = "insert into " + getDbTable() + " (";
    for (int i = 0; i < columnName.length; i++) {
      if (i > 0) query = query + ", ";
      query = query + columnName[i];
    }
    query = query + ") values (";
    for (int j = 0; j < columnName.length; j++) {
      if (j > 0) query = query + ", ";
      query = query + fieldType(j);
      query = query + fieldSelector[j].getText();
      query = query + fieldType(j);
    }
    query = query + ")";
    return query;
  }

  private int getTableIndex(String name) {
    int ind = 0;
    for (int i = 0; i < columnName.length; i++) {
      if (columnName[i].compareTo(name) == 0) ind = i;
    }
    return ind;
  }

  private String fieldType(int i) {
    String ft = " ";
    if (Integer.parseInt(columnType[i]) == java.sql.Types.CHAR) ft = "'";
    if (Integer.parseInt(columnType[i]) == java.sql.Types.VARCHAR) ft = "'";
    if (Integer.parseInt(columnType[i]) == java.sql.Types.LONGVARCHAR) ft = "'";
    if (Integer.parseInt(columnType[i]) == java.sql.Types.BINARY) ft = "'";
    if (Integer.parseInt(columnType[i]) == java.sql.Types.LONGVARBINARY) ft = "'";
    if (Integer.parseInt(columnType[i]) == java.sql.Types.VARBINARY) ft = "'";
    if (Integer.parseInt(columnType[i]) == java.sql.Types.DATE) ft = "'";
    if (Integer.parseInt(columnType[i]) == java.sql.Types.TIME) ft = "'";
    if (Integer.parseInt(columnType[i]) == java.sql.Types.TIMESTAMP) ft = "'";
    if (Integer.parseInt(columnType[i]) == java.sql.Types.OTHER) ft = "'";
    return ft;
  }

  private void setPanelReserved(boolean b) {
    this.setEnabled(b);
  }

  public String getMappingAsXml() {
    StringBuffer xmlString = new StringBuffer("");
    xmlString.append(getStartElement("FieldMappings"));

    for (int i = 0; i < columnName.length; i++) {
      if (fieldSelector[i].getText() != null) {
        xmlString.append(getStartElement("Mapping"));
        xmlString.append(getStartElement("DatabaseField"));
        xmlString.append(columnName[i]);
        xmlString.append(getEndElement("DatabaseField"));
        xmlString.append(getStartElement("DatabaseFieldType"));
        xmlString.append(columnType[i]);
        xmlString.append(getEndElement("DatabaseFieldType"));
        xmlString.append(getStartElement("MessageField"));
        xmlString.append(fieldSelector[i].getText());
        xmlString.append(getEndElement("MessageField"));
        xmlString.append(getEndElement("Mapping"));
      }
    }
    xmlString.append(getEndElement("FieldMappings"));
    return xmlString.toString();
  }

  private String getStartElement(String name) {
    return "<" + name + ">";
  }

  private String getEndElement(String name) {
    return "</" + name + ">";
  }

  public static void main(String[] args) {

    Connection connection = null;
    final InsertForm af = new InsertForm(connection, "%");
    JFrame frame = new JFrame("InsertForm");
    frame.getContentPane().add(af);
    frame.pack();
    frame.setVisible(true);
    frame.addWindowListener(new WindowAdapter() {
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
  }
}