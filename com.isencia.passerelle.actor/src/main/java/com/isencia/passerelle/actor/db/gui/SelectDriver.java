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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import javax.swing.JComboBox;

public class SelectDriver extends JComboBox {

  private Vector v;

  public SelectDriver(String selected) {
    selected = selected.toLowerCase();
    String line = "";
    String tmp = "";
    v = new Vector();
    int sel = 0;
    int idx = -1;
    try {
      BufferedReader in = new BufferedReader(new InputStreamReader(SelectDriver.class.getResourceAsStream("rdbms.dat")));
      while (line != null) {
        try {
          line = in.readLine();
          if (line != null) {
            if (!line.startsWith("#") && (line.trim().length() != 0)) {
              idx = idx + 1;
              JDBCStuff js = new JDBCStuff();
              js.setObject(line);
              v.addElement(js);
              this.addItem(js.getName());
              tmp = js.getName().toLowerCase();
              if ((idx == -1) && tmp.startsWith(selected)) sel = idx;
            }
          }
        } catch (IOException iox) {
          line = null;
        }
      }
    } catch (Exception excpt) {
    }
    if (sel > 0) this.setSelectedIndex(sel);
  }

  public JDBCStuff getData(int i) {
    return (JDBCStuff) v.elementAt(i);
  }

  public String getSelectedDb() {
    return (String) this.getSelectedItem();
  }

  public void setSelectedDb(String db) {
    // try this simple trick to avoid loosing data in the other fields
    // there's an action listener registered on SelectDriver,
    // and every time something's changed in here, it puts the default
    // values for the given DB vendor in the form...
    if (db != null && !db.equalsIgnoreCase(getSelectedDb())) {
      this.setSelectedItem(db);
    }
  }
}