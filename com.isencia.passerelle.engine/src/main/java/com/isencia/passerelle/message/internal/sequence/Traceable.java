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
package com.isencia.passerelle.message.internal.sequence;

/**
 * Allows to trace whether a thing has been handled in a Passerelle model.
 * What "handled" exactly means is not clearly defined.
 * It is typically determined by receiving some kind of feedback message
 * from some part of a model that performs some crucial function on the thing.
 * 
 * @author erwin
 */
public interface Traceable {
	/**
	 * @return Returns whether the thing has been handled.
	 */
	boolean isHandled();
}
