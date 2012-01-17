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

/**
 * @author wim To change this generated comment edit the template variable
 *         "typecomment": Window>Preferences>Java>Templates. To enable and
 *         disable the creation of type comments go to
 *         Window>Preferences>Java>Code Generation.
 */
public class JDBCHelper {

  public static String buildUrl(String dbType, String url, String host, String port, String dataBase) {

    String result = null;

    if (dbType.equalsIgnoreCase("IBM-DB2")) {
      if (port != null && !port.equals("0")) {
        result = url + "//" + host + ":" + port + "/" + dataBase;
      } else {
        result = url + "//" + host + "/" + dataBase;
      }
    }
    if (dbType.equalsIgnoreCase("ODBC")) {
      result = url + dataBase;
    }
    if (dbType.equalsIgnoreCase("SQLSERVER")) {
      result = url + host + ":" + port + ";DatabaseName=" + dataBase + ";SelectMethod=cursor";
    }
    if (dbType.equalsIgnoreCase("ORACLE")) {
      result = url + "@" + host + ":" + port + ":" + dataBase;
    }
    if (dbType.equalsIgnoreCase("OTHER")) {
      result = url;
    }

    return result;
  }

}