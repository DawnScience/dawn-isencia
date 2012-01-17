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
package com.isencia.passerelle.actor.gui.graph.userlib;

import javax.swing.Action;
import javax.swing.JMenuItem;
import com.isencia.passerelle.actor.gui.graph.ModelGraphPanel;


import ptolemy.kernel.Entity;
import ptolemy.kernel.util.NamedObj;
import ptolemy.vergil.toolbox.MenuItemFactory;
import diva.gui.toolbox.JContextMenu;

/**
 * Create a menu item that will delete an entity from the User Library.
 */
public class DeleteFromLibraryMenuItemFactory implements MenuItemFactory {
    private ModelGraphPanel panel;

	public DeleteFromLibraryMenuItemFactory(ModelGraphPanel panel) {
		this.panel = panel;
	}

	/**
	 * Add an item to the given context menu that will delete the selected item
	 * for the user library
	 */
    public JMenuItem create(final JContextMenu menu, final NamedObj object) {
    	if(object instanceof Entity) {
    		Entity e = (Entity) object;
    		if(panel.getUserLibrary()!=null && panel.getUserLibrary().deepContains(e)) {
	            Action action = new DeleteFromLibraryAction(panel);
	            action.putValue(Action.SHORT_DESCRIPTION,
	                    "Delete from Library");
	            return menu.add(action, (String)action.getValue(Action.NAME));
    		}
    	} 
   		return null;
    }
}