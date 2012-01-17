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

/*
 * @(#)JDBCAdapter.java    1.6 98/02/10
 *
 * Copyright (c) 1997 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of Sun
 * Microsystems, Inc. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Sun.
 *
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 *
 */

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDBCAdapter {
  Connection connection;
  Statement statement;
  ResultSet resultSet;
  String[] columnNames = {};
  Class[] columnTpyes = {};
  ResultSetMetaData metaData;
  String executedQuery;
  private static Logger logger = LoggerFactory.getLogger(JDBCAdapter.class);

  public JDBCAdapter(Connection connection) {
    try {
      this.connection = connection;
      statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
    } catch (SQLException e) {
      logger.error("", e);
    }
  }

  public void executeQuery(String query) {
    if (connection == null || statement == null) {
      logger.error("There is no database to execute the query.");
      return;
    }
    try {
      resultSet = statement.executeQuery(query);
      metaData = resultSet.getMetaData();
      int numberOfColumns = metaData.getColumnCount();
      columnNames = new String[numberOfColumns];
      // Get the column names and cache them.
      // Then we can close the connection.
      for (int column = 0; column < numberOfColumns; column++) {
        columnNames[column] = metaData.getColumnLabel(column + 1);
      }
      executedQuery = query;
    } catch (SQLException ex) {
      logger.error("", ex);
    }
  }

  public void close() throws SQLException {
    if (resultSet != null) {
      resultSet.close();
      resultSet = null;
    }
    if (statement != null) {
      statement.close();
      statement = null;
    }
  }

  protected void finalize() throws Throwable {
    close();
    super.finalize();
  }

  /**
   * getting the object from the resultSet, because there seems to be something
   * strange with the resultSet.getObject() of my JDBC-Driver
   **/
  public Object getObject(int i) {
    try {
      // the upperCase stuff is something because of Postgres...
      // as well as the startsWith-thing....
      // System.out.println(metaData.getColumnTypeName(i).toUpperCase());
      if (metaData.getColumnTypeName(i).toUpperCase().startsWith("INTEGER")) {
        Integer k = new Integer(resultSet.getInt(i));
        return k;
      }
      if (metaData.getColumnTypeName(i).toUpperCase().startsWith("INT")) {
        Integer k = new Integer(resultSet.getInt(i));
        return k;
      } else if (metaData.getColumnTypeName(i).toUpperCase().startsWith("VARCHAR")) {
        return resultSet.getString(i);
      } else if (metaData.getColumnTypeName(i).toUpperCase().startsWith("VARBINARY")) {
        return resultSet.getString(i);
      } else if (metaData.getColumnTypeName(i).toUpperCase().startsWith("LONGVARBINARY")) {
        return resultSet.getString(i);
      } else if (metaData.getColumnTypeName(i).toUpperCase().startsWith("BINARY")) {
        return resultSet.getString(i);
      } else if (metaData.getColumnTypeName(i).toUpperCase().startsWith("CHAR")) {
        return resultSet.getString(i);
      } else if (metaData.getColumnTypeName(i).toUpperCase().startsWith("LONGVARBINARY")) {
        return resultSet.getString(i);
      } else if (metaData.getColumnTypeName(i).toUpperCase().startsWith("BIT")) {
        resultSet.getBoolean(i);
      } else if (metaData.getColumnTypeName(i).toUpperCase().startsWith("DOUBLE")) {
        Double d = new Double(resultSet.getDouble(i));
        return d;
      } else if (metaData.getColumnTypeName(i).toUpperCase().startsWith("FLOAT")) {
        Float f = new Float(resultSet.getFloat(i));
        return f;
      } else if (metaData.getColumnTypeName(i).toUpperCase().startsWith("DATE")) {
        return resultSet.getDate(i);
      } else if (metaData.getColumnTypeName(i).toUpperCase().endsWith("TIMESTAMP")) {
        Timestamp f = new Timestamp(System.currentTimeMillis());
        f = (Timestamp) resultSet.getObject(i);
        return f;
      } else if (metaData.getColumnTypeName(i).toUpperCase().endsWith("NUMBER")) {
        Float f = new Float(resultSet.getFloat(i));
        // { NUMBER f= new NUMBER((NUMBER)resultSet.getObject(i));
        return f;
      } else if (metaData.getColumnTypeName(i).toUpperCase().startsWith("BLOB")) {
        return resultSet.getString(i);
      }
      return resultSet.getObject(i);
    } catch (SQLException e) {
      return null;
    }
  }

  public String getColumnName(int column) {
    if (columnNames[column] != null) {
      return columnNames[column];
    } else {
      return "";
    }
  }

  public int getColumnCount() {
    return columnNames.length;
  }

  public String getNextRecordAsXml() {
    Vector newRow = new Vector();
    try {
      if (resultSet.next()) {
        for (int i = 1; i <= getColumnCount(); i++) {
          newRow.addElement(getObject(i));
        }
      } else
        return null;
    } catch (SQLException e) {
      logger.error("", e);
    }
    StringBuffer result = new StringBuffer();
    SqlParser parser = new SqlParser(executedQuery);
    String tablename = parser.getTables()[0];
    result.append("<?xml version=\"1.0\"?>");
    result.append(getStartElement("ResultSet"));
    result.append(getStartElement(tablename));
    for (int i = 0; i < getColumnCount(); i++) {
      result.append(getStartElement(getColumnName(i)));
      result.append(newRow.get(i));
      result.append(getEndElement(getColumnName(i)));
    }
    result.append(getEndElement(tablename));
    result.append(getEndElement("ResultSet"));
    return result.toString();
  }

  private String getStartElement(String name) {
    return "<" + name + ">";
  }

  private String getEndElement(String name) {
    return "</" + name + ">";
  }
}