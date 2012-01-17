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

import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Settable;
import com.isencia.passerelle.actor.db.TableReader;
import com.isencia.passerelle.actor.gui.BasePasserelleQuery;
import com.isencia.passerelle.actor.gui.IPasserelleQuery;
import com.isencia.passerelle.actor.gui.PasserelleEditorPaneFactory;
import com.isencia.passerelle.actor.gui.PasserelleQuery.QueryLabelProvider;

public class DBEditorPaneFactory extends PasserelleEditorPaneFactory {

  private static final long serialVersionUID = 1L;

  public DBEditorPaneFactory() throws IllegalActionException, NameDuplicationException {
    super();
  }

  public DBEditorPaneFactory(NamedObj container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);
  }

  public IPasserelleQuery _createEditorPane(NamedObj object, Iterable<Settable> parameters, QueryLabelProvider labelProvider,
      ParameterEditorAuthorizer authorizer) {
    if (object instanceof TableReader) {
      return new BasePasserelleQuery((TableReader) object, new DataBaseSelectBuilder(object.getName()));
    }
    return null;
  }
}
