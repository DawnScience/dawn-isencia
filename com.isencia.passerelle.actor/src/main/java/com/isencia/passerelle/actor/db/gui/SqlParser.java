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

import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

public class SqlParser {
  private ArrayList tables = new ArrayList();
  private ArrayList columns = new ArrayList();
  private Map conditions = new TreeMap();
  private String query = null;

  public SqlParser(String query) {
    this.query = query;
    parseSelectQuery();
  }

  private void parseSelectQuery() {
    StringTokenizer st = new StringTokenizer(query, " ,\t\n\r\f");
    // select
    String select = st.nextToken();
    boolean continu = true;
    if (!select.equalsIgnoreCase("SELECT")) return;
    // columns
    while (st.hasMoreTokens() && continu) {
      String column = st.nextToken();
      if (!column.equalsIgnoreCase("FROM")) {
        columns.add(column);
      } else
        continu = false;
    }
    continu = true;
    // tables
    while (st.hasMoreTokens() && continu) {
      String table = st.nextToken();
      if (!table.equalsIgnoreCase("WHERE")) {
        tables.add(table);
      } else
        continu = false;
    }
    // conditions
    parseSelectConditions(st);
  }

  private void parseSelectConditions(StringTokenizer selectCondition) {
    boolean continu = true;
    while (selectCondition.hasMoreTokens() && continu) {
      String column = selectCondition.nextToken();
      String operator = selectCondition.nextToken();
      String value = selectCondition.nextToken();
      if (value != null) {
        if (value.startsWith("'") && value.endsWith("'")) {
          value = value.substring(1, value.length() - 1);
        }
      }
      conditions.put(column, new SelectCondition(column, operator, value));
      if (!selectCondition.hasMoreTokens() || !"and".equalsIgnoreCase(selectCondition.nextToken())) {
        continu = false;
      }
    }
  }

  /**
   * Returns the columns.
   * 
   * @return ArrayList
   */
  public String[] getColumns() {
    return (String[]) columns.toArray(new String[0]);
  }

  /**
   * Returns the conditions.
   * 
   * @return ArrayList
   */
  public Map getConditions() {
    return conditions;
  }

  /**
   * Returns the tables.
   * 
   * @return ArrayList
   */
  public String[] getTables() {
    return (String[]) tables.toArray(new String[0]);
  }

  public class SelectCondition {
    String[] conditionString = { "equal to", "not equal to", "smaller than or equal", "smaller than", "bigger than or equal", "bigger than" };
    // "starts with", "ends with", "contains"
    String[] conditionOperator = { "=", "!=", "<=", "<", ">=", ">" };

    SelectCondition(String column, String operator, String value) {
      this.column = column;
      this.operator = operator;
      this.value = value;
    }

    SelectCondition() {
    }

    String column = null;
    String operator = null;
    String value = null;

    /**
     * Returns the column.
     * 
     * @return String
     */
    public String getColumn() {
      return column;
    }

    /**
     * Returns the operator.
     * 
     * @return String
     */
    public String getOperator() {
      if (operator.indexOf("like") > -1) {
        if (value.startsWith("%")) {
          if (value.endsWith("%")) {
            return "contains";
          } else {
            return "starts width";
          }
        } else {
          return "ends width";
        }
      }
      for (int i = 0; i < conditionOperator.length; i++) {
        if (conditionOperator[i].equalsIgnoreCase(operator)) {
          return conditionString[i];
        }
      }
      return null;
    }

    /**
     * Returns the value.
     * 
     * @return String
     */
    public String getValue() {
      if (value != null) {
        if (value.indexOf("%") > -1) if (value.startsWith("%")) {
          if (value.endsWith("%")) {
            return value.substring(1, value.length() - 1);
          } else {
            return value.substring(0, value.length());
          }
        } else {
          return value.substring(0, value.length() - 1);
        }
      }
      return value;
    }

    /**
     * Sets the column.
     * 
     * @param column The column to set
     */
    public void setColumn(String column) {
      this.column = column;
    }

    /**
     * Sets the operator.
     * 
     * @param operator The operator to set
     */
    public void setOperator(String operator) {
      this.operator = operator;
    }

    /**
     * Sets the value.
     * 
     * @param value The value to set
     */
    public void setValue(String value) {
      this.value = value;
    }
  }
}