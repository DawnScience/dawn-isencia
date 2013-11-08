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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public abstract class AbstractEvent implements Event {
  
  private static volatile AtomicLong idCounter = new AtomicLong(0);

  private Date timeStamp;
  private long id;
  
  protected AbstractEvent(Date timeStamp) {
    this.timeStamp = timeStamp;
    this.id = idCounter.incrementAndGet();
  }

  /**
   * 
   * @return a new Event with copied info, but new timestamp
   */
  public abstract Event copy();
  

  public Date getTimestamp() {
    return timeStamp;
  }
  
  protected long getId() {
    return id;
  }

  @Override
  public String toString() {
    return toString(new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS"));
  }
}
