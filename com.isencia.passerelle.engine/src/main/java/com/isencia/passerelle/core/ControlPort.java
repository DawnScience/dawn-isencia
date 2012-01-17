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
package com.isencia.passerelle.core;

import ptolemy.kernel.Entity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.StringAttribute;

/**
 * ControlPort
 * 
 * TODO: class comment
 * 
 * @author erwin
 */
public class ControlPort extends Port {

	/**
	 * @param container
	 * @param name
	 * @throws ptolemy.kernel.util.IllegalActionException
	 * @throws ptolemy.kernel.util.NameDuplicationException
	 */
	public ControlPort(Entity container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
		try {
			// hack to get port layout a bit nicer
			// works in combination with a modified 
			// - ptolemy.vergil.toolbox.PortSite
			// - ptolemy.vergil.actor.ActorController.EntityLayout
			new StringAttribute(this,"control");
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * @param container
	 * @param name
	 * @param isInput
	 * @param isOutput
	 * @throws ptolemy.kernel.util.IllegalActionException
	 * @throws ptolemy.kernel.util.NameDuplicationException
	 */
	public ControlPort(Entity container, String name, boolean isInput, boolean isOutput)
		throws IllegalActionException, NameDuplicationException {
		super(container, name, isInput, isOutput);
		try {
			// hack to get port layout a bit nicer
			// works in combination with a modified 
			// - ptolemy.vergil.toolbox.PortSite
			// - ptolemy.vergil.actor.ActorController.EntityLayout
			new StringAttribute(this,"control");
		} catch (Exception e) {
			// ignore
		}
	}

}
