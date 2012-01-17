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

import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class JDBCStuff {
  public String name;
  public String jdbcDriver;
  public String dbUrl;
  public String port;

  public JDBCStuff() {
    ;
  }

  public void setObject(String line) {
    try {
      StringTokenizer st = new StringTokenizer(line, "^");
      setName(st.nextToken());
      setJDBCDriver(st.nextToken());
      setDbUrl(st.nextToken());
      setPort(st.nextToken());
    } catch (NoSuchElementException nse) {
      ;
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setJDBCDriver(String jdbcDriver) {
    this.jdbcDriver = jdbcDriver;
  }

  public void setDbUrl(String dbUrl) {
    this.dbUrl = dbUrl;
  }

  public void setPort(String port) {
    this.port = port;
  }

  public String getName() {
    return name;
  }

  public String getJDBCDriver() {
    return jdbcDriver;
  }

  public String getDbUrl() {
    return dbUrl;
  }

  public String getPort() {
    return port;
  }

  public int getPortAsInt() {
    int i = 0;
    try {
      i = Integer.parseInt(port);
    } catch (NumberFormatException nfe) {
      ;
    }
    return i;
  }
}