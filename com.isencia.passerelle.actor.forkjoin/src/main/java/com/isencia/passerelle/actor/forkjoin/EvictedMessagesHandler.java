/* Copyright 2013 - iSencia Belgium NV

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
package com.isencia.passerelle.actor.forkjoin;

import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * An <code>EvictedMessagesHandler</code> instance can be plugged in <code>MessageSequenceGenerator</code>s and other things maintaining message sets. 
 * The purpose is to be able to configure what must be done with message sets that have been evicted from the source's state storage,
 * before they had been completely processed, joined etc.
 * 
 * @author erwin
 */
public interface EvictedMessagesHandler {
  
  /**
   * @param initialMsg
   * @param otherMessages
   * 
   * @throws PasserelleException
   */
  void handleEvictedMessages(ManagedMessage initialMsg, ManagedMessage... otherMessages) throws PasserelleException;

}
