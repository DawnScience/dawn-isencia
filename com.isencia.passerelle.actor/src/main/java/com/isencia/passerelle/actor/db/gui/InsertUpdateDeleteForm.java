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
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.util.swing.layout.AbsoluteConstraints;
import com.isencia.util.swing.layout.AbsoluteLayout;

public class InsertUpdateDeleteForm extends JPanel {

  private static Logger logger = LoggerFactory.getLogger(InsertUpdateDeleteForm.class);
  public final static int QUERYTYPE_INSERT = 1;
  public final static int QUERYTYPE_UPDATE = 2;
  public final static int QUERYTYPE_DELETE = 3;
  private JList tables;
  private String[] columnName;
  private String[] columnType;
  private int[] columnAllowNull;
  private int[] columnLength;
  private JTextField[] conditionValue;
  private JComboBox[] conditionType;
  private JTextField[] fieldSelector;
  private JTabbedPane mainPanel;
  private JPanel panel0;
  private JPanel panel0_1;
  private JPanel panel1;
  private JPanel panel2;
  private JPanel panel2_1;
  private Connection connection = null;
  private String currentTable = null;
  private boolean initializeColumns = true;
  private boolean initializeConditions = true;
  private JPanel queryTypePane;
  private JRadioButton[] queryTypes;
  private int queryType = 0;
  private int previousQueryType = 0;
  private HashMap collumnMappings = new HashMap();
  private HashMap conditionTypeMappings = new HashMap();
  private HashMap conditionValueMappings = new HashMap();
  private boolean busy = false;

  public InsertUpdateDeleteForm(Connection connection) {
    this.connection = connection;
    setSchema("%");
    initializeColumns = false;
    initializeConditions = false;
    doInit();
  }

  public InsertUpdateDeleteForm(String table, int queryType, String columnMapping, String conditionMapping, Connection connection) {
    this.connection = connection;
    buildColumnMappingsFromXml(columnMapping);
    buildConditionMappingsFromXml(conditionMapping);
    setSchema("%");
    doInit();
    setSelectedTable(table);
    setSelectedQueryType(queryType);
  }

  public InsertUpdateDeleteForm(Connection connection, String schema) {
    this.connection = connection;
    if (schema != null && schema.length() > 0)
      setSchema(schema);
    else
      setSchema("%");
    initializeColumns = false;
    initializeConditions = false;
    doInit();
  }

  public InsertUpdateDeleteForm(String table, int queryType, String columnMapping, String conditionMapping, Connection connection, String schema) {
    this.connection = connection;
    buildColumnMappingsFromXml(columnMapping);
    buildConditionMappingsFromXml(conditionMapping);
    if (schema != null && schema.length() > 0)
      setSchema(schema);
    else
      setSchema("%");
    doInit();
    setSelectedTable(table);
    setSelectedQueryType(queryType);

  }

  /**
   * Get the value of dbTable.
   * 
   * @return Value of dbTable.
   */
  public String getSelectedTable() {
    return (String) tables.getSelectedValue();
  }

  private void setSelectedTable(String table) {
    tables.setSelectedValue(table, true);
  }

  private void setSelectedQueryType(int newQueryType) {
    queryTypes[newQueryType - 1].setSelected(true);
    updateQueryTypeSelection(newQueryType);
  }

  /**
   * Set the value of dbTable.
   * 
   * @param v Value to assign to dbTable.
   */
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

  private void doInit() {
    mainPanel = new JTabbedPane();
    ChangeListener changeListener = new ChangeListener() {
      public void stateChanged(ChangeEvent changeEvent) {
        JTabbedPane sourceTabbedPane = (JTabbedPane) changeEvent.getSource();
        int index = sourceTabbedPane.getSelectedIndex();
        if (index != 0) updatePanes(index);
      }
    };
    queryTypePane = queryTypePane();
    mainPanel.addChangeListener(changeListener);
    mainPanel.add("Table", initTablePane());
    this.setLayout(new BorderLayout());
    this.add("Center", mainPanel);
  }

  private void removePanes() {
    if (mainPanel.getTabCount() > 1) {
      for (int i = mainPanel.getTabCount(); i > 1; i--) {
        mainPanel.removeTabAt(i - 1);
      }
    }
  }

  private void updatePanes(int index) {

    if (getSelectedTable() == null) {
      if (currentTable != null) {
        if (queryType == QUERYTYPE_UPDATE || queryType == QUERYTYPE_INSERT) clearColumnPane();
        if (queryType == QUERYTYPE_UPDATE || queryType == QUERYTYPE_DELETE) clearConditionPane();
      }
    } else {
      if (currentTable == null) {
        if (queryType == QUERYTYPE_UPDATE || queryType == QUERYTYPE_INSERT) {
          setColumns(getSelectedTable());
          updateColumnPane(getSelectedTable());
        }
        if (queryType == QUERYTYPE_UPDATE || queryType == QUERYTYPE_DELETE) updateConditionPane();
      } else if (!currentTable.equals(getSelectedTable()) || queryType != previousQueryType) {
        if (queryType == QUERYTYPE_UPDATE || queryType == QUERYTYPE_INSERT) {
          clearColumnPane();
          updateColumnPane(getSelectedTable());
        }
        if (queryType == QUERYTYPE_UPDATE || queryType == QUERYTYPE_DELETE) {
          clearConditionPane();
          updateConditionPane();
        }
      }
    }
    currentTable = getSelectedTable();
    previousQueryType = queryType;
  }

  private JPanel queryTypePane() {
    JPanel queryType = new JPanel();
    ButtonGroup qtGroup = new ButtonGroup();
    queryType.setLayout(new AbsoluteLayout());
    queryTypes = new JRadioButton[3];
    queryTypes[0] = new JRadioButton("Insert");
    queryTypes[0].addActionListener(new ActionListener() {

      /**
       * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
       */
      public void actionPerformed(ActionEvent arg0) {
        updateQueryTypeSelection(1);
      }
    });
    queryTypes[1] = new JRadioButton("Update");
    queryTypes[1].addActionListener(new ActionListener() {

      /**
       * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
       */
      public void actionPerformed(ActionEvent arg0) {
        updateQueryTypeSelection(2);
      }
    });
    queryTypes[2] = new JRadioButton("Delete");
    queryTypes[2].addActionListener(new ActionListener() {

      /**
       * @see java.awt.event.ActionListener#actionPerformed(ActionEvent)
       */
      public void actionPerformed(ActionEvent arg0) {
        updateQueryTypeSelection(3);
      }
    });
    queryType.add(queryTypes[0], new AbsoluteConstraints(100, 127, 124, 19));
    queryType.add(queryTypes[1], new AbsoluteConstraints(100, 149, 124, 19));
    queryType.add(queryTypes[2], new AbsoluteConstraints(100, 173, 124, 19));
    qtGroup.add(queryTypes[0]);
    qtGroup.add(queryTypes[1]);
    qtGroup.add(queryTypes[2]);
    return queryType;
  }

  private void updateQueryTypeSelection(int radioButton) {
    removePanes();
    queryType = radioButton;
    switch (radioButton) {
    case 1: // insert
      mainPanel.add("Columns", initColumnPane());
      break;
    case 2: // update
      mainPanel.add("Columns", initColumnPane());
      mainPanel.add("Conditions", initConditionPane());
      break;
    case 3: // delete
      mainPanel.add("Conditions", initConditionPane());
      break;
    default:
      break;
    }
  }

  /**
   * within this panel the user can select the table he/she would like to query
   * from
   */
  private JPanel initTablePane() {
    panel1 = new JPanel();
    panel1.setLayout(new GridLayout(1, 2));
    panel1.add(queryTypePane);
    panel1.add(tablePane());
    return panel1;
  }

  private JPanel initColumnPane() {
    panel0 = new JPanel();
    panel0.setLayout(new BorderLayout());
    return panel0;
  }

  private JPanel initConditionPane() {
    panel2 = new JPanel();
    panel2.setLayout(new BorderLayout());
    return panel2;
  }

  private void clearColumnPane() {
    if (panel0_1 != null) panel0.remove(panel0_1);
  }

  private void clearConditionPane() {
    if (panel2_1 != null) panel2.remove(panel2_1);
  }

  private void updateColumnPane(String table) {
    panel0_1 = new JPanel();
    panel0_1.setLayout(new BorderLayout());
    panel0_1.add("Center", columnPane());
    panel0.add(panel0_1);
  }

  private void updateConditionPane() {
    panel2_1 = new JPanel();
    panel2_1.setLayout(new BorderLayout());
    panel2_1.add("Center", conditionPane());
    panel2.add(panel2_1);
  }

  private JScrollPane conditionPane() {
    JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    GridBagConstraints constraint = null;
    Insets insets = new Insets(2, 2, 2, 2);
    JLabel[] labels = new JLabel[columnName.length];
    String[] items = { "equal to", "not equal to", "smaller than or equal", "smaller than", "bigger than or equal", "bigger than", "starts with", "ends with",
        "contains" };
    for (int i = 0; i < conditionType.length; i++) {
      constraint = new GridBagConstraints();
      constraint.insets = insets;
      constraint.anchor = GridBagConstraints.NORTHEAST;
      labels[i] = new JLabel(columnName[i]);
      panel.add(labels[i], constraint);
      constraint = new GridBagConstraints();
      constraint.insets = insets;
      constraint.anchor = GridBagConstraints.CENTER;
      conditionType[i] = new JComboBox(items);
      conditionValue[i] = new JTextField();
      conditionValue[i].setText(null);
      conditionValue[i].setColumns(15);
      if (initializeConditions && conditionValueMappings.containsKey(columnName[i])) {

        conditionValue[i].setText((String) conditionValueMappings.get(columnName[i]));
        conditionType[i].setSelectedItem((String) conditionTypeMappings.get(columnName[i]));
      }
      panel.add(conditionType[i], constraint);
      constraint = new GridBagConstraints();
      constraint.gridwidth = 0;
      constraint.insets = insets;
      constraint.anchor = GridBagConstraints.WEST;
      panel.add(conditionValue[i], constraint);
    }
    JScrollPane scrollPane = new JScrollPane();
    scrollPane.getViewport().add(panel);
    return scrollPane;
    // panel3.add("West", scrollPane);
  }

  private JScrollPane columnPane() {
    Insets insets = new Insets(2, 2, 2, 2);
    JPanel panel = new JPanel();
    panel.setLayout(new GridBagLayout());
    GridBagConstraints gridBagConstraints1;

    for (int i = 0; i < columnName.length; i++) {
      gridBagConstraints1 = new GridBagConstraints();
      gridBagConstraints1.anchor = GridBagConstraints.NORTHEAST;
      gridBagConstraints1.insets = insets;
      if (columnAllowNull[i] == DatabaseMetaData.columnNoNulls)
        panel.add((JLabel) new JLabel(columnName[i] + " *"), gridBagConstraints1);
      else
        panel.add((JLabel) new JLabel(columnName[i]), gridBagConstraints1);
      gridBagConstraints1 = new GridBagConstraints();
      gridBagConstraints1.gridwidth = 0;
      gridBagConstraints1.anchor = GridBagConstraints.WEST;
      gridBagConstraints1.insets = insets;
      fieldSelector[i] = new JTextField();
      fieldSelector[i].setColumns(15);
      if (initializeColumns && collumnMappings.containsKey(columnName[i])) {
        fieldSelector[i].setText((String) collumnMappings.get(columnName[i]));
      }
      fieldSelector[i].setAlignmentY(Component.LEFT_ALIGNMENT);
      panel.add(fieldSelector[i], gridBagConstraints1);
    }
    initializeColumns = false;
    JScrollPane scrollFpane = new JScrollPane();
    scrollFpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollFpane.getViewport().setExtentSize(new Dimension(150, 150));
    scrollFpane.setSize(150, 150);
    scrollFpane.getViewport().add(panel);
    return scrollFpane;
  }

  private boolean checkConditions() {
    boolean v = false;
    for (int i = 0; i < conditionValue.length; i++) {
      if (conditionValue[i].getText().length() > 0) v = true;
    }
    return v;
  }

  private String generateConditions() {
    String s = "";
    for (int i = 0; i < conditionValue.length; i++) {
      if (conditionValue[i].getText().length() > 0) {
        if (s.length() > 0) s = s + "\nand\n\t ";
        if (conditionType[i].getSelectedItem().toString().startsWith("equal to"))
          s = s + columnName[i] + " = " + fieldType(i) + conditionValue[i].getText() + fieldType(i);
        if (conditionType[i].getSelectedItem().toString().startsWith("not equal to"))
          s = s + columnName[i] + " != " + fieldType(i) + conditionValue[i].getText() + fieldType(i);
        if (conditionType[i].getSelectedItem().toString().startsWith("smaller than or equal"))
          s = s + columnName[i] + " <= " + fieldType(i) + conditionValue[i].getText() + fieldType(i);
        if (conditionType[i].getSelectedItem().toString().endsWith("smaller than"))
          s = s + columnName[i] + " < " + fieldType(i) + conditionValue[i].getText() + fieldType(i);
        if (conditionType[i].getSelectedItem().toString().startsWith("bigger than or equal"))
          s = s + columnName[i] + " >= " + fieldType(i) + conditionValue[i].getText() + fieldType(i);
        if (conditionType[i].getSelectedItem().toString().endsWith("bigger than"))
          s = s + columnName[i] + " > " + fieldType(i) + conditionValue[i].getText() + fieldType(i);
        if (conditionType[i].getSelectedItem().toString().startsWith("starts with"))
          s = s + columnName[i] + " like " + fieldType(i) + conditionValue[i].getText() + "%" + fieldType(i);
        if (conditionType[i].getSelectedItem().toString().startsWith("ends with"))
          s = s + columnName[i] + " like " + fieldType(i) + "%" + conditionValue[i].getText() + fieldType(i);
        if (conditionType[i].getSelectedItem().toString().startsWith("contains"))
          s = s + columnName[i] + " like " + fieldType(i) + "%" + conditionValue[i].getText() + "%" + fieldType(i);
      }
    }
    return s;
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

  /**
   * this is the List that contains the available tables in the Database
   */
  private JPanel tablePane() {
    String[] tableTypes = { "TABLE", "VIEW", "ALIAS", "SYNONYM" };
    Vector tableVector = new Vector();
    try {
      ResultSet rs = getConnection().getMetaData().getTables(null, getSchema(), "%", tableTypes);
      while (rs.next()) {
        String schema = rs.getString(2);
        String table = rs.getString(3);
        if (table != null && table.length() > 0) {
          if (schema != null && schema.length() > 0) {
            tableVector.add(schema + "." + table);
          } else {
            // probably DB that does not support schema (eg HSQLDB)
            tableVector.add(table);
          }
        } else {
          logger.error("Weird result: undefined table name");
        }
      }
      rs.close();
    } catch (SQLException sqe) {
      logger.error("Error retrieving tables", sqe);
    }

    JLabel header = new JLabel("TABLES");
    JPanel tableList = new JPanel();
    tableList.setLayout(new BoxLayout(tableList, BoxLayout.Y_AXIS));
    tableList.add(header);
    tables = new JList();
    tables.addListSelectionListener(new ListSelectionListener() {
      /**
       * @see javax.swing.event.ListSelectionListener#valueChanged(ListSelectionEvent)
       */
      public void valueChanged(ListSelectionEvent arg0) {

        setColumns(getSelectedTable());
      }
    });
    tables.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    tables.setListData(tableVector);
    // if (initializeTable){
    // tables.setSelectedValue(initTable,true);
    // initializeTable=false;
    // }
    // else{

    tables.setSelectedIndex(0);

    // }
    // set the Listener onto the table-List
    tables.setVisibleRowCount(15);
    tables.setFixedCellWidth(200);
    JScrollPane listPane = new JScrollPane();
    listPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    listPane.getViewport().add(tables);
    tableList.add(listPane);
    return tableList;
  }

  /**
   * this is to update the whole stuff if the table changed
   **/
  /**
   * this is to display all available columns of the selected table there are
   * shadow arrays that contains additional infos: - columnType = the SQL-Type
   * of the field - columnLength = the length of this field
   */
  private void setColumns(String table) {
    int i = 0;

    try {
      // this is to count the columns to create the array...
      DatabaseMetaData md = getConnection().getMetaData();
      int schemaEndIndex = table.indexOf(".");
      String schema = null;
      String tableOnly = table;
      if (schemaEndIndex > -1) {
        schema = table.substring(0, schemaEndIndex);
        tableOnly = table.substring(schemaEndIndex + 1, table.length());
      }
      ResultSet cc = md.getColumns(null, schema, tableOnly, "%");
      while (cc.next()) {
        i++;
      }
      cc.close();
      ResultSet cols = md.getColumns(null, null, table, "%");
      columnName = new String[i];
      columnType = new String[i];
      columnLength = new int[i];
      conditionValue = new JTextField[i];
      conditionType = new JComboBox[i];
      fieldSelector = new JTextField[i];
      columnAllowNull = new int[i];
      i = 0;
      while (cols.next()) {
        if (schema != null) {
          // need to be careful to use full field name
          // to ensure using correct schema
          columnName[i] = table + "." + cols.getString(4);
        } else {
          // schema unknown anyway, using short field names
          // increases support for simple DB implementations
          // e.g. HSQLDB
          columnName[i] = cols.getString(4);
        }
        columnType[i] = cols.getString(5);
        columnLength[i] = cols.getInt(7);
        columnAllowNull[i] = cols.getInt(11);
        i++;
      }
      cols.close();
    } catch (Exception exception) {
      logger.error("Error retrieving tables", exception);
    }

  }

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

  private String getStartElement(String name) {
    return "<" + name + ">";
  }

  private String getEndElement(String name) {
    return "</" + name + ">";
  }

  public String getColumnMappingAsXml() {
    StringBuffer xmlString = new StringBuffer("");
    xmlString.append(getStartElement("FieldMappings"));

    for (int i = 0; i < columnName.length; i++) {
      if (fieldSelector[i] != null && fieldSelector[i].getText() != null && fieldSelector[i].getText().length() > 0) {
        xmlString.append(getStartElement("Mapping"));
        xmlString.append(getStartElement("DatabaseField"));
        xmlString.append(columnName[i]);
        xmlString.append(getEndElement("DatabaseField"));
        xmlString.append(getStartElement("DatabaseFieldType"));
        xmlString.append(columnType[i]);
        xmlString.append(getEndElement("DatabaseFieldType"));
        xmlString.append(getStartElement("DatabaseFieldAllowNull"));
        xmlString.append(columnAllowNull[i]);
        xmlString.append(getEndElement("DatabaseFieldAllowNull"));
        xmlString.append(getStartElement("MessageField"));
        xmlString.append(fieldSelector[i].getText());
        xmlString.append(getEndElement("MessageField"));
        xmlString.append(getEndElement("Mapping"));
      }
    }
    xmlString.append(getEndElement("FieldMappings"));
    return xmlString.toString();
  }

  public String getConditionMappingAsXml() {
    StringBuffer xmlString = new StringBuffer("");
    xmlString.append(getStartElement("ConditionMappings"));

    for (int i = 0; i < columnName.length; i++) {
      if (conditionValue[i] != null && conditionValue[i].getText() != null && conditionValue[i].getText().length() > 0) {
        xmlString.append(getStartElement("Mapping"));
        xmlString.append(getStartElement("DatabaseCondition"));
        xmlString.append(columnName[i]);
        xmlString.append(getEndElement("DatabaseCondition"));
        xmlString.append(getStartElement("DatabaseConditionType"));
        xmlString.append(conditionType[i].getSelectedItem().toString());
        xmlString.append(getEndElement("DatabaseConditionType"));
        xmlString.append(getStartElement("DatabaseFieldType"));
        xmlString.append(columnType[i]);
        xmlString.append(getEndElement("DatabaseFieldType"));
        xmlString.append(getStartElement("MessageField"));
        xmlString.append(conditionValue[i].getText());
        xmlString.append(getEndElement("MessageField"));
        xmlString.append(getEndElement("Mapping"));
      }
    }
    xmlString.append(getEndElement("ConditionMappings"));
    return xmlString.toString();
  }

  private void buildColumnMappingsFromXml(String initColumnMapping) {
    Document mappingsDoc = null;
    SAXBuilder builder = null;
    builder = new SAXBuilder(false);
    try {
      mappingsDoc = builder.build(new StringReader("<?xml version=\"1.0\"?>" + initColumnMapping));
      List mappingsList = mappingsDoc.getRootElement().getChildren();
      Iterator it = mappingsList.iterator();
      while (it.hasNext()) {
        Element mappingElem = (Element) it.next();
        Element databaseCondition = mappingElem.getChild("DatabaseField");
        Element messageField = mappingElem.getChild("MessageField");
        collumnMappings.put(databaseCondition.getText(), messageField.getText());
      }

    } catch (Exception e) {
      logger.error("Error building Jdom Document", e);
    }
  }

  private void buildConditionMappingsFromXml(String initColumnMapping) {
    Document mappingsDoc = null;
    SAXBuilder builder = null;
    builder = new SAXBuilder(false);
    try {
      mappingsDoc = builder.build(new StringReader("<?xml version=\"1.0\"?>" + initColumnMapping));
      List mappingsList = mappingsDoc.getRootElement().getChildren();
      Iterator it = mappingsList.iterator();
      while (it.hasNext()) {
        Element mappingElem = (Element) it.next();
        Element dataBaseField = mappingElem.getChild("DatabaseCondition");
        Element conditionType = mappingElem.getChild("DatabaseConditionType");
        Element messageField = mappingElem.getChild("MessageField");
        conditionTypeMappings.put(dataBaseField.getText(), conditionType.getText());
        conditionValueMappings.put(dataBaseField.getText(), messageField.getText());
      }

    } catch (Exception e) {
      logger.error("Error building Jdom Document", e);
    }
  }

  /**
   * Returns the queryType.
   * 
   * @return int
   */
  public int getQueryType() {
    return queryType;
  }

}