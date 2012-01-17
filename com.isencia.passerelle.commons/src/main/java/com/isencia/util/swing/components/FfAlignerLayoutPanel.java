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
package com.isencia.util.swing.components;

/** 
 FfAlignerLayoutPanel is a Panel that allows me to arrange
 the be.isencia.util.swing.components inside the Panels for Admin much more easy



 Admin is a Tool around mySQL to do basic jobs
 for DB-Administrations, like:
 - create/ drop tables
 - create  indices
 - perform sql-statements
 - simple form
 - a guided query
 and a other usefull things in DB-arena

 Admin V1.0 
 Copyright (c) 1999 Fredy Fischer
 se-afs@dial.eunet.ch

 Fredy Fischer
 Hulmenweg 36
 8405 Winterthur
 Switzerland

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 **/

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JPanel;

public class FfAlignerLayoutPanel extends JPanel {

  private int actPos = 1;
  private GridBagConstraints gbc;
  public int numberOfRows = 2;

  /**
   * Get the value of numberOfRows.
   * 
   * @return Value of numberOfRows.
   */
  public int getNumberOfRows() {
    return numberOfRows;
  }

  /**
   * Set the value of numberOfRows.
   * 
   * @param v Value to assign to numberOfRows.
   */
  public void setNumberOfRows(int v) {
    this.numberOfRows = v;
  }

  Insets insets;

  /**
   * Get the value of insets.
   * 
   * @return Value of insets.
   */
  public Insets getInsets() {
    return insets;
  }

  /**
   * Set the value of insets.
   * 
   * @param top = the inset from the top
   * @param left = the inset from the left
   * @param bottom = the inset from the bottom
   * @param right = the inset from the right
   */
  public void setInsets(int top, int left, int bottom, int right) {
    Insets i = new Insets(top, left, bottom, right);
    this.insets = i;
  }

  public FfAlignerLayoutPanel() {
    insets = new Insets(5, 5, 5, 5);
    gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    this.setLayout(new GridBagLayout());
  }

  /**
   * Adds a component to the Panel
   * 
   * @param c = Component to add
   */

  public void addComponent(Component c) {

    gbc.insets = getInsets();
    if (actPos < getNumberOfRows()) {
      gbc.anchor = GridBagConstraints.NORTHEAST;
      gbc.gridwidth = GridBagConstraints.RELATIVE;
    } else {
      gbc.anchor = GridBagConstraints.WEST;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
    }

    gbc.fill = GridBagConstraints.BOTH;

    this.add(c, gbc);
    actPos = actPos + 1;
    if (actPos > numberOfRows) actPos = 1;
  }
}
