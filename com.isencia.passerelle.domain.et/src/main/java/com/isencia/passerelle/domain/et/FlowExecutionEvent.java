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
import ptolemy.actor.CompositeActor;

public class FlowExecutionEvent extends AbstractEvent {

  public enum FlowExecutionEventType {
    START,FINISH;
  }
  
  private CompositeActor target;
  private FlowExecutionEventType eventType;
  
  public FlowExecutionEvent(CompositeActor target, FlowExecutionEventType eventType) {
    this(target, eventType, new Date());
  }

  public FlowExecutionEvent(CompositeActor target, FlowExecutionEventType eventType, Date timeStamp) {
    super(timeStamp);
    this.target = target;
    this.eventType = eventType;
  }

  public FlowExecutionEvent copy() {
    return new FlowExecutionEvent(target, eventType);
  }
  
  public CompositeActor getTarget() {
    return target;
  }

  public FlowExecutionEventType getEventType() {
    return eventType;
  }

  public String toString(DateFormat dateFormat) {
    return dateFormat.format(getTimestamp()) + " " + getId() + " FlowExecutionEvent [eventType=" + eventType + ", target=" + target.getFullName() + "]";
  }
}
