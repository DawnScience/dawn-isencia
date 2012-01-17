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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.util.swing.components.JListDataExchangePanel;
import com.isencia.util.swing.components.MutableListModel;
import com.isencia.util.swing.components.SortedMutableListModel;

public class QueryByExample extends JPanel {

  private JListDataExchangePanel tableList;
  private JListDataExchangePanel columnList;
  private String[] columnName;
  private String[] columnType;
  private int[] columnLength;
  private JTextField[] conditionValue;
  private JComboBox[] conditionType;
  public String query;
  private JTextArea queryArea;
  private JTabbedPane mainPanel;
  private JPanel panel1;
  private JPanel panel2;
  private JPanel panel3;
  private JPanel panel4;
  private JPanel panel2_1;
  private JPanel panel3_1_1;
  private JScrollPane scrollPane;
  private Connection connection = null;
  private String currentTable = null;
  private boolean initializeColumns = true;
  private boolean initializeConditions = true;
  private String[] queryTables = null;
  private String[] queryColumns = null;
  private Map conditions = null;
  private String originaltableSelection = null;
  private static Logger logger = LoggerFactory.getLogger(QueryByExample.class);

  public QueryByExample(Connection connection) {
    this.connection = connection;
    setSchema("%");
    initializeColumns = false;
    initializeConditions = false;
    doInit();
  }

  public QueryByExample(String query, Connection connection) {
    this.connection = connection;
    setSchema("%");
    SqlParser parser = new SqlParser(query);
    queryTables = parser.getTables();
    queryColumns = parser.getColumns();
    conditions = parser.getConditions();
    doInit();
  }

  public QueryByExample(Connection connection, String schema) {
    this.connection = connection;
    if (schema != null && schema.length() > 0)
      setSchema(schema);
    else
      setSchema("%");
    initializeColumns = false;
    initializeConditions = false;
    doInit();
  }

  public QueryByExample(String query, Connection connection, String schema) {
    this.connection = connection;
    if (schema != null && schema.length() > 0)
      setSchema(schema);
    else
      setSchema("%");
    SqlParser parser = new SqlParser(query);
    queryTables = parser.getTables();
    queryColumns = parser.getColumns();
    conditions = parser.getConditions();
    doInit();
  }

  /**
   * Get the value of dbTable.
   * 
   * @return Value of dbTable.
   */
  public String getSelectedTable() {

    if (tableList.getDestModel().getSize() > 0) {

      return tableList.getDestModel().getElementAt(0).toString();
    } else

      return null;
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

    mainPanel.addChangeListener(changeListener);
    mainPanel.add("Table", initTablePane());
    mainPanel.add("Columns", initColumnPane());
    mainPanel.add("Conditions", initConditionPane());
    mainPanel.add("Query", initQueryPane());
    this.setLayout(new BorderLayout());
    this.add("Center", mainPanel);
  }

  private void updatePanes(int index) {

    if (index == 3) {
      clearQueryPane();
      updateQueryPane();
    }
    if (getSelectedTable() == null) {

      if (currentTable != null) {
        clearColumnPane();
        clearConditionPane();
        clearQueryPane();
      }
    } else {

      if (currentTable == null) {
        updateColumnPane(getSelectedTable());
        updateConditionPane();
        updateQueryPane();
      } else if (!currentTable.equals(getSelectedTable())) {
        clearColumnPane();
        clearConditionPane();
        clearQueryPane();
        updateColumnPane(getSelectedTable());
        updateConditionPane();
        updateQueryPane();
      }
    }

    currentTable = getSelectedTable();
  }

  /**
   * within this panel the user can select the table he/she would like to query
   * from
   */
  private JPanel initTablePane() {
    panel1 = new JPanel();
    panel1.setLayout(new BorderLayout());
    panel1.add("Center", selectTable());

    return panel1;
  }

  private JPanel initColumnPane() {
    panel2 = new JPanel();
    panel2.setLayout(new BorderLayout());

    return panel2;
  }

  private void updateColumnPane(String table) {

    try {
      panel2_1 = new JPanel();
      panel2_1.setLayout(new BorderLayout());
      panel2_1.add("Center", selectColumns(table));
      panel2.add(panel2_1);
    } catch (SQLException e) {
    }
  }

  private void clearTablePane() {
    tableList.getDestList().removeAll();
  }

  private void clearColumnPane() {
    columnList.getDestList().removeAll();
    panel2.remove(panel2_1);
  }

  private JScrollPane initConditionPane() {
    scrollPane = new JScrollPane();
    // panel3.setLayout(new BorderLayout());
    return scrollPane;
  }

  private void updateConditionPane() {
    // scrollPane=new JScrollPane();
    scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    panel3_1_1 = new JPanel();
    panel3_1_1.setLayout(new GridBagLayout());
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
      panel3_1_1.add(labels[i], constraint);
      constraint = new GridBagConstraints();
      constraint.insets = insets;
      constraint.anchor = GridBagConstraints.CENTER;
      conditionType[i] = new JComboBox(items);
      conditionValue[i] = new JTextField();
      conditionValue[i].setText(null);
      conditionValue[i].setColumns(15);
      if (initializeConditions && conditions != null && conditions.containsKey(columnName[i])) {
        SqlParser.SelectCondition condition = (SqlParser.SelectCondition) conditions.get(columnName[i]);
        conditionValue[i].setText(condition.getValue());
        conditionType[i].setSelectedItem(condition.getOperator());
      }

      panel3_1_1.add(conditionType[i], constraint);
      constraint = new GridBagConstraints();
      constraint.gridwidth = 0;
      constraint.insets = insets;
      constraint.anchor = GridBagConstraints.WEST;
      panel3_1_1.add(conditionValue[i], constraint);
    }

    scrollPane.getViewport().add(panel3_1_1);
    // panel3.add("West", scrollPane);

  }

  private void clearConditionPane() {
    // panel3.remove(scrollPane);
    scrollPane.remove(panel3_1_1);
  }

  private JPanel initQueryPane() {
    panel4 = new JPanel();
    panel4.setLayout(new BorderLayout());
    queryArea = new JTextArea(12, 50);
    queryArea.setWrapStyleWord(true);
    queryArea.setLineWrap(true);

    JScrollPane scrollpane = new JScrollPane();
    scrollpane.getViewport().add(queryArea);
    queryArea.setEditable(true);

    JPanel panel4_2 = new JPanel();
    panel4_2.setLayout(new GridBagLayout());

    GridBagConstraints gbc;
    Insets insets = new Insets(1, 1, 1, 1);
    gbc = new GridBagConstraints();
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.anchor = GridBagConstraints.WEST;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = insets;
    gbc.gridx = 0;
    gbc.gridy = 0;
    panel4_2.add(scrollpane, gbc);

    JPanel panel4_1 = new JPanel();
    panel4_1.setBorder(BorderFactory.createEtchedBorder());
    panel4.add("Center", panel4_2);
    panel4.add("South", panel4_1);

    return panel4;
  }

  private void clearQueryPane() {
    queryArea.setText(null);
  }

  private void updateQueryPane() {
    generateQuery();
  }

  public String getQuery() {
    query = "";

    if (columnName != null) {
      query = "Select ";
      // first fill in the fields to display
      for (int i = 0; i < columnName.length; i++) {

        if (isColumnSelected(i)) {

          if (query.length() > 7) query = query + ",\n\t";

          query = query + columnName[i];
        }
      }

      query = query + "\nfrom " + getSelectedTable() + "\n";

      if (checkConditions()) {
        query = query + "\nwhere\n\t " + generateConditions();
      }
    }

    return query;
  }

  private void generateQuery() {
    queryArea.setText(getQuery());
    queryArea.updateUI();
  }

  private boolean isColumnSelected(int index) {
    for (int i = 0; i < columnList.getDestModel().getSize(); i++) {
      if (columnList.getDestModel().getElementAt(i).toString().equals(columnName[index])) return true;
    }
    return false;
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
  private JPanel selectTable() {

    String[] tableTypes = { "TABLE", "VIEW", "ALIAS", "SYNONYM" };
    SortedSet tableSet = new TreeSet();
    try {
      ResultSet rs = getConnection().getMetaData().getTables(null, schema, "%", tableTypes);

      while (rs.next()) {
        String schema = rs.getString(2);
        String table = rs.getString(3);
        if (table != null && table.length() > 0) {
          if (schema != null && schema.length() > 0) {
            tableSet.add(schema + "." + table);
          } else {
            // probably DB that does not support schema (eg HSQLDB)
            tableSet.add(table);
          }
        } else {
          logger.error("Weird result: undefined table name");
        }
      }
      rs.close();
    } catch (SQLException sqe) {
      logger.error("", sqe);
    }

    SortedSet so2 = new TreeSet();
    for (int i = 0; queryTables != null && i < queryTables.length; i++) {
      so2.add(queryTables[i]);
      originaltableSelection = queryTables[0];
    }

    MutableListModel s1 = new SortedMutableListModel(tableSet);
    MutableListModel s2 = new SortedMutableListModel(so2);
    tableList = new JListDataExchangePanel(s1, s2, "source", "dest");
    tableList.getSrcList().setVisibleRowCount(12);
    tableList.getSrcList().setFixedCellWidth(200);
    tableList.getSrcList().setSelectedIndex(0);

    tableList.getDestList().setVisibleRowCount(12);
    tableList.getDestList().setFixedCellWidth(200);

    JPanel listPane = new JPanel();
    listPane.add(tableList);

    return listPane;
  }

  private JPanel selectColumns(String table) throws SQLException {

    TreeSet columnSet = new TreeSet();
    int i = 0;
    try {
      // this is to count the columns to create the array...
      DatabaseMetaData md = getConnection().getMetaData();
      table = table.substring(table.indexOf(".") + 1, table.length());
      ResultSet cc = md.getColumns(null, "%", table, "%");

      while (cc.next()) {
        i++;
      }
      cc.close();
      ResultSet cols = md.getColumns(null, "%", table, "%");
      columnName = new String[i];
      columnType = new String[i];
      columnLength = new int[i];
      conditionValue = new JTextField[i];
      conditionType = new JComboBox[i];
      i = 0;

      while (cols.next()) {
        columnName[i] = getSelectedTable() + "." + cols.getString(4);
        columnSet.add(columnName[i]);
        columnType[i] = cols.getString(5);
        columnLength[i] = cols.getInt(7);
        i++;
      }
      cols.close();
    } catch (Exception exception) {
      logger.error("", exception);
    }

    SortedSet so2 = new TreeSet();
    for (int ii = 0; initializeColumns && originaltableSelection == getSelectedTable() && ii < queryColumns.length; ii++) {
      so2.add(queryColumns[ii]);

    }
    initializeColumns = false;
    MutableListModel s1 = new SortedMutableListModel(columnSet);
    MutableListModel s2 = new SortedMutableListModel(so2);
    columnList = new JListDataExchangePanel(s1, s2, "source", "dest");
    columnList.getSrcList().setVisibleRowCount(12);
    columnList.getSrcList().setFixedCellWidth(200);
    columnList.getSrcList().setSelectedIndex(0);
    columnList.getDestList().setVisibleRowCount(12);
    columnList.getDestList().setFixedCellWidth(200);

    JPanel listPane = new JPanel();
    listPane.add(columnList);

    return listPane;
  }

  private void message(String msg) {
    JOptionPane.showMessageDialog(null, msg, "Message", JOptionPane.WARNING_MESSAGE);
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

  public static void main(String[] args) {
    Connection connection = null;
    final QueryByExample qbe = new QueryByExample("select USER.CUSTOMER_ID from USER where USER.CUSTOMER_ID like '%45%'", connection);
    // final Qbe qbe=new Qbe(connection);
    final JFrame frame = new JFrame("QBE");
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(qbe, BorderLayout.CENTER);

    frame.addWindowListener(new WindowAdapter() {

      public void windowClosing(WindowEvent e) {
        System.exit(0);
      }
    });
    frame.pack();
    frame.setVisible(true);
  }

  public void closeConnection() {
    try {
      if (connection != null) {
        connection.close();
      }
    } catch (Exception e) {
      logger.error("Error closing the DB connection", e);
    }
  }
}