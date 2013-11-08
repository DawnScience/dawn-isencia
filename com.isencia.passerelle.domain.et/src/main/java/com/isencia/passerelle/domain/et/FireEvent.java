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
package com.isencia.passerelle.domain.et;

import java.text.DateFormat;
import java.util.Date;
import ptolemy.actor.Actor;

public class FireEvent extends AbstractEvent {

  private Actor target;

  public FireEvent(Actor target) {
    this(target, new Date());
  }

  public FireEvent(Actor target, Date timeStamp) {
    super(timeStamp);
    this.target = target;
  }

  public FireEvent copy() {
    return new FireEvent(target);
  }
  
  public Actor getTarget() {
    return target;
  }

  public String toString(DateFormat dateFormat) {
    return dateFormat.format(getTimestamp()) + " " + getId() + " FireEvent [target=" + target.getFullName() + "]";
  }
}
