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

import ptolemy.data.Token;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.core.PortHandler;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageHelper;

/**
 * @version 1.1
 * @author edeley
 */
public abstract class Transformer extends Actor {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -956542172812952106L;

	private final static Logger logger = LoggerFactory.getLogger(Transformer.class);
	
	/**
	 * Holds the last received message
	 */
	protected ManagedMessage message = null;
	
	/** The input port.  This base class imposes no type constraints except
	 *  that the type of the input cannot be greater than the type of the
	 *  output.
	 *  
	 *  NOTE Ports must be public for composites to work.
	 */
	public Port input;
	private PortHandler inputHandler = null;

	/** The output port. By default, the type of this output is constrained
	 *  to be at least that of the input.
	 */
	public Port output;


    /** Construct an actor with the given container and name.
     *  @param container The container.
     *  @param name The name of this actor.
     *  @exception IllegalActionException If the actor cannot be contained
     *   by the proposed container.
     *  @exception NameDuplicationException If the container already has an
     *   actor with this name.
     */
    public Transformer(CompositeEntity container, String name)
            throws NameDuplicationException, IllegalActionException  {
        super(container, name);

        input = PortFactory.getInstance().createInputPort(this, null);
        output = PortFactory.getInstance().createOutputPort(this);

		_attachText("_iconDescription", "<svg>\n" +
                "<rect x=\"-20\" y=\"-20\" width=\"40\" " +
                "height=\"40\" style=\"fill:lightgrey;stroke:lightgrey\"/>\n" +
                "<line x1=\"-19\" y1=\"-19\" x2=\"19\" y2=\"-19\" " +
                "style=\"stroke-width:1.0;stroke:white\"/>\n" +
                "<line x1=\"-19\" y1=\"-19\" x2=\"-19\" y2=\"19\" " +
                "style=\"stroke-width:1.0;stroke:white\"/>\n" +
                "<line x1=\"20\" y1=\"-19\" x2=\"20\" y2=\"20\" " +
                "style=\"stroke-width:1.0;stroke:black\"/>\n" +
                "<line x1=\"-19\" y1=\"20\" x2=\"20\" y2=\"20\" " +
                "style=\"stroke-width:1.0;stroke:black\"/>\n" +
                "<line x1=\"19\" y1=\"-18\" x2=\"19\" y2=\"19\" " +
                "style=\"stroke-width:1.0;stroke:grey\"/>\n" +
                "<line x1=\"-18\" y1=\"19\" x2=\"19\" y2=\"19\" " +
                "style=\"stroke-width:1.0;stroke:grey\"/>\n" +
                
                "<circle cx=\"0\" cy=\"0\" r=\"10\"" +
                "style=\"fill:white;stroke-width:2.0\"/>\n" +
                "<line x1=\"-15\" y1=\"0\" x2=\"15\" y2=\"0\" " +
                "style=\"stroke-width:2.0\"/>\n" +
                "<line x1=\"12\" y1=\"-3\" x2=\"15\" y2=\"0\" " +
                "style=\"stroke-width:2.0\"/>\n" +
                "<line x1=\"12\" y1=\"3\" x2=\"15\" y2=\"0\" " +
                "style=\"stroke-width:2.0\"/>\n" +
                "</svg>\n");
    }

	protected void doInitialize() throws InitializationException {
		if (logger.isTraceEnabled())
			logger.trace(getInfo());
			
		inputHandler = new PortHandler(input);
		if(input.getWidth()>0) {
			inputHandler.start();
		}
		
		if(logger.isTraceEnabled())
			logger.trace(getInfo()+" - exit ");

	}

	protected boolean doPreFire() throws ProcessingException {
		if (logger.isTraceEnabled())
			logger.trace(getInfo()+" doPreFire() - entry");
		
		Token token = inputHandler.getToken();
		if (token != null) {
			try {
				message = MessageHelper.getMessageFromToken(token);
			} catch (PasserelleException e) {
						throw new ProcessingException("Error handling token", token, e);
			}
		} else {
			message = null;
		}

		if (logger.isTraceEnabled())
			logger.trace(getInfo()+" doPreFire() - exit");
		return super.doPreFire();
	}

	protected void doFire() throws ProcessingException {
		if (logger.isTraceEnabled())
			logger.trace(getInfo());
		
		if (message != null) {
			notifyStartingFireProcessing();
			try {
				if (logger.isDebugEnabled()) {
					logger.debug(getInfo() + " - Transformer received message :" + message);
				}
				doFire(message);
			} catch(ProcessingException e) {
				throw e;
			} finally {
				notifyFinishedFireProcessing();
			}
		} else if(!isFinishRequested()){
			requestFinish();
		} else {
			// just make sure we don't get any wild loops
			// while the actor is still making up its mind about wrapping up
			try {
				Thread.sleep(3000);
			} catch (InterruptedException e) {
			}
		}
		
		if(logger.isTraceEnabled())
			logger.trace(getInfo()+" - exit ");
	}

	/**
	 * @param message
	 */
	protected abstract void doFire(ManagedMessage message) throws ProcessingException;

	protected String getAuditTrailMessage(ManagedMessage message, Port port) throws Exception {
		return " sent converted message";
	}
}

