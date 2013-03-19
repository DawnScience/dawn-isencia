/* Copyright 2012 - iSencia Belgium NV

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

package com.isencia.passerelle.director;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.actor.Director;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.Actor;
import com.isencia.passerelle.ext.DirectorAdapter;
import com.isencia.passerelle.ext.impl.DefaultDirectorAdapter;
import com.isencia.passerelle.ext.impl.NullDirectorAdapter;

/**
 * @author erwin
 */
public class DirectorUtils {
  private static Logger LOGGER = LoggerFactory.getLogger(DirectorUtils.class);

  /**
   * Tries to obtain the configured adapter with the given name, on the given director. If the name is null, it is treated as the default name
   * <code>DirectorAdapter.DEFAULT_ADAPTER_NAME</code>.
   * <p>
   * For the default name, if no adapter was present yet, a default adapter is set. For other names, null is returned in that case.
   * </p>
   * <p>
   * I.e. there is theoretical support to optionally attach multiple adapters to a same director, for very specific cases. But a default adapter is presumed
   * present.
   * </p>
   * 
   * @param director
   *          not null!
   * @param adapterName
   * @return
   */
  public static DirectorAdapter getAdapter(Director director, String adapterName) {
    try {
      DirectorAdapter adapter = null;
      if (adapterName == null || DirectorAdapter.DEFAULT_ADAPTER_NAME.equals(adapterName)) {
        adapter = (DirectorAdapter) director.getAttribute(DirectorAdapter.DEFAULT_ADAPTER_NAME, DirectorAdapter.class);
        if (adapter == null) {
          adapter = new DefaultDirectorAdapter(director, DirectorAdapter.DEFAULT_ADAPTER_NAME);
        }
        return adapter;
      } else {
        return (DirectorAdapter) director.getAttribute(adapterName, DirectorAdapter.class);
      }
    } catch (IllegalActionException e) {
      LOGGER.error("Internal error - failed to get DirectorAdapter", e);
      return NullDirectorAdapter.getInstance();
    } catch (NameDuplicationException e) {
      LOGGER.error("Internal error - failed to create DirectorAdapter", e);
      return NullDirectorAdapter.getInstance();
    }
  }

  /**
   * @param director
   * @return
   */
  public static Set<Actor> getActiveActorsWithoutInputs(Director director) {
    Set<Actor> result = new HashSet<Actor>();
    DirectorAdapter adapter = DirectorUtils.getAdapter(director, null);
    for (ptolemy.actor.Actor actor : adapter.getActiveActors()) {
      if (actor instanceof Actor) {
        Actor a = (Actor) actor;
        List<Port> portList = a.inputPortList();
        boolean actorHasInputs = false;
        for (Port port : portList) {
          if (port instanceof com.isencia.passerelle.core.Port) {
            com.isencia.passerelle.core.Port p = (com.isencia.passerelle.core.Port) port;
            if (!p.getActiveSources().isEmpty()) {
              actorHasInputs = true;
            }
          }
        }
        if(!actorHasInputs) {
          result.add(a);
        }
      } else {
        LOGGER.warn("Model contains non-Passerelle actor "+actor.getFullName());
      }
    }
    return result;
  }
}
