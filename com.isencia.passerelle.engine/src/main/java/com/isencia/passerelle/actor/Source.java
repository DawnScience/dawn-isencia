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

package com.isencia.passerelle.actor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.message.ManagedMessage;

/**
 * 
 * Source
 * 
 * A base class for all actors that connect to datafeeds in the "outside" world
 * and then feed the data as messages into a Passerelle application.
 * 
 * @author erwin
 */
public abstract class Source extends Actor {
    /**
	 * 
	 */
	private static final long serialVersionUID = -791693375286296102L;

    private static Logger logger = LoggerFactory.getLogger(Source.class);

    /**
     * Holds the last received message
     */
    private ManagedMessage message = null;
    
	/**
	 * The output port used by a source to send messages with the data
	 * received from external data feeds.
	 */
    public Port output = null;
    /**
     * A boolean flag indicating whether the external data
     * feed for this actor has been exhausted or not.
     */
    private boolean noMoreMessages = false;
    
    /**
     * Constructor for Source.
     * @param container
     * @param name
     * @throws NameDuplicationException
     * @throws IllegalActionException
     */
    public Source(CompositeEntity container, String name)
           throws NameDuplicationException, IllegalActionException {
        super(container, name);

        output = PortFactory.getInstance().createOutputPort(this);

        _attachText("_iconDescription", 
                    "<svg>\n" + "<rect x=\"-20\" y=\"-20\" width=\"40\" " + "height=\"40\" style=\"fill:orange;stroke:orange\"/>\n" + 
                    "<line x1=\"-19\" y1=\"-19\" x2=\"19\" y2=\"-19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n" + 
                    "<line x1=\"-19\" y1=\"-19\" x2=\"-19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n" + 
                    "<line x1=\"20\" y1=\"-19\" x2=\"20\" y2=\"20\" " + "style=\"stroke-width:1.0;stroke:black\"/>\n" + 
                    "<line x1=\"-19\" y1=\"20\" x2=\"20\" y2=\"20\" " + "style=\"stroke-width:1.0;stroke:black\"/>\n" + 
                    "<line x1=\"19\" y1=\"-18\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n" + 
                    "<line x1=\"-18\" y1=\"19\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n" + 
                    "<circle cx=\"0\" cy=\"0\" r=\"10\"" + "style=\"fill:white;stroke-width:2.0\"/>\n" + "<line x1=\"-15\" y1=\"0\" x2=\"0\" y2=\"0\" " + 
                    "style=\"stroke-width:2.0\"/>\n" + "<line x1=\"-3\" y1=\"-3\" x2=\"0\" y2=\"0\" " + "style=\"stroke-width:2.0\"/>\n" + 
                    "<line x1=\"-3\" y1=\"3\" x2=\"0\" y2=\"0\" " + "style=\"stroke-width:2.0\"/>\n" + "</svg>\n");
    }

	protected boolean doPreFire() throws ProcessingException {
		if (logger.isTraceEnabled())
			logger.trace(getInfo()+" doPreFire() - entry");
		
		message = getMessage();

		if (logger.isTraceEnabled())
			logger.trace(getInfo()+" doPreFire() - exit");
		return super.doPreFire();
	}


    protected void doFire() throws ProcessingException {
        if (logger.isTraceEnabled()) {
            logger.trace(getInfo()+" doFire() - entry");
        }

        if (message != null) {
        	notifyStartingFireProcessing();
            noMoreMessages = false;
    		try {
    			sendOutputMsg(output,message);
    		} catch (IllegalArgumentException e) {
    			throw new ProcessingException(getInfo() + " - doFire() generated exception "+e,message,e);
    		} finally {
    			notifyFinishedFireProcessing();
    		}
        } else {
            noMoreMessages = true;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(getInfo()+" doFire() - exit");
        }
    }

    
	protected boolean doPostFire() throws ProcessingException {
        if (logger.isTraceEnabled()) {
            logger.trace(getInfo()+" doPostFire() - entry");
        }
        boolean res = !hasNoMoreMessages();
        if(!res) {
            // just to make sure base class knows that we're finished
            requestFinish();
        } else {
			res = super.doPostFire();
		}
       if (logger.isTraceEnabled()) {
            logger.trace(getInfo()+" doPostFire() - exit :"+res);
        }
		return res;
	}

	/**
	 * 
	 * @return
	 * @throws ProcessingException
	 */
	protected abstract ManagedMessage getMessage() throws ProcessingException;

    /**
     *
     * @return a flag indicating whether this source has exhausted its datafeed.
     */
    public boolean hasNoMoreMessages() {
        return noMoreMessages;
    }

	protected void doInitialize() throws InitializationException{
		if (logger.isTraceEnabled()) {
			logger.trace(getInfo());
		}
		
		noMoreMessages = false;
		
		if (logger.isTraceEnabled()) {
			logger.trace(getInfo()+" - exit ");
		}
	}

}