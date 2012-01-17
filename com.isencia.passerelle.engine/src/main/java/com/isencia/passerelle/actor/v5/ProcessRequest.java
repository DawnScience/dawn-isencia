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
package com.isencia.passerelle.actor.v5;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.isencia.passerelle.message.MessageInputContext;

import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * ProcessRequest is a generic container for request data delivered to an actor.
 * It contains (inputport,message) pairs.
 *
 * @author erwin
 */
public class ProcessRequest {

	private long iterationCount=0;
	private Map<String, MessageInputContext> inputContexts = new HashMap<String, MessageInputContext>();

	/**
	 *
	 */
	public ProcessRequest() {
		super();
	}

	/**
	 * @return Returns the iterationCount.
	 */
	public long getIterationCount() {
		return iterationCount;
	}

	/**
	 * @param iterationCount The iterationCount to set.
	 */
	public void setIterationCount(long iterationCount) {
		this.iterationCount = iterationCount;
	}

	public void addInputMessage(int inputIndex, String inputName, ManagedMessage inputMsg) {
		MessageInputContext presentCtxt = inputContexts.get(inputName);
		if(presentCtxt==null) {
			inputContexts.put(inputName, new MessageInputContext(inputIndex,inputName,inputMsg));
		} else {
			presentCtxt.addMsg(inputMsg);
		}
	}

	public void addInputContext(MessageInputContext msgCtxt) {
		if(msgCtxt!=null) {
			MessageInputContext presentCtxt = inputContexts.get(msgCtxt.getPortName());
			if(presentCtxt==null) {
				inputContexts.put(msgCtxt.getPortName(), msgCtxt);
			} else {
				presentCtxt.addMsg(msgCtxt.getMsg());
			}
		}
	}

	public ManagedMessage getMessage(Port inputPort) {
		if(inputPort!=null)
			return getMessage(inputPort.getName());
		else
			return null;
	}

	public ManagedMessage getMessage(String inputName) {
		if(inputName!=null) {
			MessageInputContext ctxt = inputContexts.get(inputName);
			return ctxt!=null?ctxt.getMsg():null;
		} else
			return null;
	}
	
	/**
	 * 
	 * @since Passerelle v4.1.1
	 * 
	 * @return all received input contexts
	 */
	public Iterator<MessageInputContext> getAllInputContexts() {
		return inputContexts.values().iterator();
	}
	
	/**
	 * 
	 * @return an indication whether this request contains at least one MessageInputContext
	 */
	public boolean isEmpty() {
		return inputContexts.isEmpty();
	}
	
	/**
	 * 
	 * @return an indication whether this request contains unprocessed MessageInputContexts
	 */
	public boolean hasSomethingToProcess() {
		boolean result = false;
		Collection<MessageInputContext> inpContexts = inputContexts.values();
		for (MessageInputContext messageInputContext : inpContexts) {
			if(result=!messageInputContext.isProcessed())
				break;
		}
		return result;
	}

	public String toString() {
		StringBuffer bfr = new StringBuffer();
		Collection<MessageInputContext> c = inputContexts.values();
		MessageInputContext[] inputs = c.toArray(new MessageInputContext[inputContexts.size()]);
		bfr.append("\n\tInput msgs:");
		for (int i = 0; i < inputs.length; i++) {
			MessageInputContext context = inputs[i];
			if(context!=null) {
				bfr.append("\n\t\t"+context.getPortName()+
						": msgID="+((context.getMsg()!=null && context.getMsg().getID()!=null )?context.getMsg().getID().toString():"null"));
			}
		}
		return bfr.toString();
	}

}
