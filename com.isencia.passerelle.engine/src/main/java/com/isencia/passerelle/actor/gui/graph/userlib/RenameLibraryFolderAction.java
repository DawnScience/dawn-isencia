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

import java.awt.event.ActionEvent;
import com.isencia.passerelle.actor.gui.graph.ModelGraphPanel;

import ptolemy.kernel.util.NamedObj;
import ptolemy.moml.EntityLibrary;
import ptolemy.vergil.toolbox.FigureAction;

public class RenameLibraryFolderAction extends FigureAction {

	private ModelGraphPanel panel;

	public RenameLibraryFolderAction(ModelGraphPanel panel) {
        super("Rename");
        this.panel=panel;
	}


	public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        NamedObj target = getTarget();
        if(target instanceof EntityLibrary) {
        	EntityLibrary lib = (EntityLibrary) target;
        	new RenameDialog(getFrame(), panel.getConfiguration(), lib);
        }

	}

}
