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

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.passerelle.actor.gui.IPasserelleComponent;
import com.isencia.passerelle.actor.gui.IPasserelleComponentCloseListener;
import com.isencia.util.swing.layout.AbsoluteConstraints;
import com.isencia.util.swing.layout.AbsoluteLayout;

/**
 * @author wim
 */
public class DataBaseSelectBuilder extends javax.swing.JPanel implements IPasserelleComponent {

  private static final long serialVersionUID = 1L;

  private static Logger logger = LoggerFactory.getLogger(DataBaseSelectBuilder.class);

  private SelectQueryPane queryPane = null;
  private List<IPasserelleComponentCloseListener> passerelleQueryCloseListeners = new ArrayList<IPasserelleComponentCloseListener>();

  public DataBaseSelectBuilder(String name) {
    setLayout(new AbsoluteLayout());
    queryPane = new SelectQueryPane();
    add(queryPane, new AbsoluteConstraints(10, 15, 550, 400));
  }

  public void setUser(String s) {
    queryPane.setUser(s);
  }

  public void setPassword(String s) {
    queryPane.setPassword(s);
  }

  public void setDriver(String s) {
    queryPane.setDriver(s);
  }

  public void setUrl(String s) {
    queryPane.setUrl(s);
  }

  public void setSchema(String s) {
    queryPane.setSchema(s);
  }

  public void setQuery(String s) {
    queryPane.setInitQuery(s);
  }

  public String getUser() {
    return queryPane.getUser();
  }

  public String getPassword() {
    return queryPane.getPassword();
  }

  public String getDriver() {
    return queryPane.getDriver();
  }

  public String getUrl() {
    return queryPane.getUrl();
  }

  public String getSchema() {
    return queryPane.getSchema();
  }

  public String getQuery() {
    return queryPane.getQuery();
  }

  public void windowClosed(Window window, String button) {
    for (IPasserelleComponentCloseListener closeListener : passerelleQueryCloseListeners) {
      closeListener.onClose(button);
    }
    queryPane.close();
  }

  public void addListener(IPasserelleComponentCloseListener closeListener) {
    passerelleQueryCloseListeners.add(closeListener);
  }

}