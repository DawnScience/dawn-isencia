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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.IntToken;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.ValidationException;
import com.isencia.passerelle.core.ControlPort;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortHandler;
import com.isencia.passerelle.core.PortMode;
import com.isencia.passerelle.core.PasserelleException.Severity;
import com.isencia.passerelle.domain.cap.Director;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.message.MessageException;
import com.isencia.passerelle.message.MessageFactory;
import com.isencia.passerelle.message.MessageHelper;

import com.isencia.passerelle.message.MessageBuffer;
import com.isencia.passerelle.message.MessageInputContext;
import com.isencia.passerelle.message.MessageOutputContext;

/**
 * <p>
 * Continuing on the track started with the "v3" Actor API, the v5 API offers
 * further enhancements in Actor development features, combined with runtime enhancements.
 * </p>
 * <p>
 * The v5 Actor solves the general issue of multi-input-port-actors
 * in the underlying Ptolemy PN domain, where it is not possible to obtain flexible
 * behaviour for an actor with multiple "blocking" input ports.
 * <p>
 * With the "v3 actor api" of Passerelle, it is still the case that multiple "pull"/"blocking"
 * input ports imply that each port must have received a message before the actor will fire.
 * <br/>
 * And when only "push"/"non-blocking" ports are used, the actor iteration loops continuously in an uncontrollable way.
 * </p>
 * <p>
 * Often it is required to have an actor with multiple inputs, where the actor should fire
 * when a message was received on at least one of the inputs.
 * <br/>
 * This is implemented here by adding an internal buffer/queue which is fed by the messages
 * arriving at the input ports. A non-empty buffer will trigger an actor iteration.
 * The actor implementation must typically be ready to accept multiple input msgs in one shot. 
 * Such behaviour can be useful when high-throughput msg streams must be processed 
 * in combination with non-negligible processing times, and i.c.o. possible optimizations 
 * when processing msg batches in one shot.
 * </p>
 * <p>
 * Optionally one can set a configurable buffer time in ms. If this is set,
 * the actor will wait at least this time before processing any received (pushed) messages.
 * When no messages have been received when the configured buffered time has passed,
 * the actor keeps on waiting till the first received message.
 * Remark that even then, due to the asynchronous and concurrent behaviour, 
 * the processing could already see more than one received message when it is finally triggered.
 * </p>
 * <p>
 * With this v5 Actor, it is in principle no longer relevant to have PULL input ports,
 * although they are still supported. Remark that for PULL ports, each Actor fire iteration
 * can only handle 1 received message at a time, as the PN semantics do not allow to find out
 * if more than 1 msg is available on a PULL port. Consequently, also the concepts of buffer
 * and of buffer time are not applicable for PULL ports. I.e. the buffer time only comes into play,
 * in the presence of active PUSH ports.
 * </p>
 * 
 * @author erwin
 */
public abstract class Actor extends com.isencia.passerelle.actor.Actor implements MessageBuffer {
	private final static Logger logger = LoggerFactory.getLogger(Actor.class);

	// a flag to indicate the special case of a source actor
	// as these do not have any input ports, so the std algorithm
	// to automatically deduce that the actor can requestFinish()
	// is not valid
	private boolean isSource = true;

	// List of (blocking) handlers we need to poll each cycle
	// to check for new input messages.
	protected List<PortHandler> blockingInputHandlers = new ArrayList<PortHandler>();
	protected List<Boolean> blockingInputFinishRequests = new ArrayList<Boolean>();

	// Collection of msg providers, i.e. typically receivers on input ports
	// that directly feed their received msgs into this MessageBuffer's queue.
	private Collection<Object> msgProviders = new HashSet<Object>();

	// Queue of messages that have been pushed to us, incl info on the input port on which
	// they have been received.
	private Queue<MessageInputContext> pushedMessages = new ConcurrentLinkedQueue<MessageInputContext>();
	// lock to manage blocking on empty pushedMessages queue
	private ReentrantLock msgQLock = new ReentrantLock();
	private Condition msgQNonEmpty = msgQLock.newCondition();

	// Just a counter for the fire cycles.
	// We're using this to be able to show
	// for each input msg on which fire cycle
	// it arrived.
	private long iterationCount = 0;

	// parameter to specify an optionl buffer time between actor processing iterations.
	public Parameter bufferTimeParameter;

	/**
	 * @param container
	 * @param name
	 * @throws IllegalActionException
	 * @throws NameDuplicationException
	 */
	public Actor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);

		bufferTimeParameter = new Parameter(this,"Buffer time (ms)", new IntToken(0));
		registerExpertParameter(bufferTimeParameter);
	}

	@Override
	protected String getExtendedInfo() {
		return "";
	}

	public Queue<MessageInputContext> getMessageQueue() {
		return pushedMessages;
	}
	
	public boolean acceptInputPort(Port p) {
		if(p==null || p.getContainer()!=this) {
			return false;
		}
		if(p instanceof ControlPort || !p.isInput()) {
			return false;
		}
		return PortMode.PUSH.equals(p.getMode());
	}	

	public boolean registerMessageProvider(Object provider) {
		getLogger().debug("Registered msgprovider {}",provider);
		return msgProviders.add(provider);
	}

	public boolean unregisterMessageProvider(Object provider) {
		getLogger().debug("Unregistered msgprovider {}",provider);
		return msgProviders.remove(provider);
	}

	public void offer(MessageInputContext ctxt) throws PasserelleException {
		try {
			if(!msgQLock.tryLock(10, TimeUnit.SECONDS)) {
				// if we did not get the lock, something is getting overcharged,
				// so refuse the task
				throw new Exception("Msg Queue lock overcharged...");
			}
			pushedMessages.offer(ctxt);
			msgQNonEmpty.signal();
		} catch (Exception e) {
			throw new PasserelleException("Error storing received msg", ctxt, e);
		} finally {
			try {msgQLock.unlock();} catch (Exception e) {}
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void doInitialize() throws InitializationException {
		if (getLogger().isTraceEnabled())
			getLogger().trace(getInfo() + " doInitialize() - entry");

		blockingInputHandlers.clear();
		blockingInputFinishRequests.clear();

		super.doInitialize();
		iterationCount = 0;

		List<Port> inputPortList = this.inputPortList();
		for (Port _p : inputPortList) {
			if (_p.isInput() && !(_p instanceof ControlPort)) {
				if (PortMode.PULL.equals(_p.getMode())) {
					blockingInputHandlers.add(new PortHandler(_p));
					blockingInputFinishRequests.add(Boolean.FALSE);
				}
				isSource = false;
			}
		}

		for (int i = 0; i < blockingInputHandlers.size(); ++i) {
			PortHandler h = blockingInputHandlers.get(i);
			if (h.getWidth() > 0) {
				blockingInputFinishRequests.set(i, Boolean.FALSE);
				h.start();
			} else {
				blockingInputFinishRequests.set(i, Boolean.TRUE);
			}
		}

		if (getLogger().isTraceEnabled())
			getLogger().trace(getInfo() + " doInitialize() - exit ");
	}

	/**
	 * Overridable method to construct customizable port handlers,
	 * that will be registered on each push-input-port.
	 * @param p
	 * @return new PortHandler
	 */
	protected PortHandler createPortHandler(Port p) {
		return new PortHandler(p);
	}

	public long getIterationCount() {
		return iterationCount;
	}

	protected void doFire() throws ProcessingException {
		if (getLogger().isTraceEnabled())
			getLogger().trace(getInfo() + " doFire() - entry");

		ProcessRequest req = new ProcessRequest();
		req.setIterationCount(iterationCount++);

		if (!isSource) {
			// first read from all blocking inputs
			for (int i = 0; i < blockingInputHandlers.size(); i++) {
				PortHandler handler = blockingInputHandlers.get(i);
				ManagedMessage msg = null;
				// if a port is exhausted, we just pass a null msg to the
				// request
				// if not, we try to read another msg from it
				// a null msg indicates that the port is exhausted
				if (!blockingInputFinishRequests.get(i).booleanValue()) {
					try {
						msg = MessageHelper.getMessage(handler);
					} catch (ProcessingException e) {
						throw e;
					} catch (PasserelleException e) {
						throw new ProcessingException("", handler, e);
					}
					if (msg == null) {
						blockingInputFinishRequests.set(i, Boolean.TRUE);
						if (getLogger().isDebugEnabled())
							getLogger().debug(getInfo() + " doFire() - found exhausted port " + handler.getName());
					} else {
						if (getLogger().isDebugEnabled())
							getLogger().debug(getInfo() + " doFire() - msg " + msg.getID() + " received on port " + handler.getName());
					}
				}
				req.addInputMessage(i, handler.getName(), msg);
			}
			if(!msgProviders.isEmpty() || !pushedMessages.isEmpty()) {
				try {
					int bufferTime = ((IntToken)bufferTimeParameter.getToken()).intValue();
					if(bufferTime>0) {
						getLogger().debug("{} doFire() - sleeping for buffer time {}",getInfo(),bufferTime);
						Thread.sleep(bufferTime);
					}
				} catch (Exception e) {
					getLogger().warn(getInfo()+" - Failed to enforce buffer time",e);
				}

				// we've got at least one PUSH port that registered a msg provider
				// so we need to include all pushed msgs in the request as well
				addPushedMessages(req);
			}

			// when all ports are exhausted, we can stop this actor
			if (areAllInputsFinished()) {
				requestFinish();
			}
		}

		if (isSource || req.hasSomethingToProcess()) {
			ActorContext ctxt = new ActorContext();
			if (mustValidateIteration()) {
				try {
					if (getLogger().isTraceEnabled())
						getLogger().trace("doFire() - validating iteration for request " + req);
					validateIteration(ctxt, req);
					if (getAuditLogger().isDebugEnabled())
						getAuditLogger().debug("ITERATION VALIDATED");
					if (getLogger().isTraceEnabled())
						getLogger().trace("doFire() - validation done");
				} catch (ValidationException e) {
					try {
						getErrorControlStrategy().handleIterationValidationException(this, e);
					} catch (IllegalActionException e1) {
						// interpret this is a FATAL error
						throw new ProcessingException(Severity.FATAL, "", this, e);
					}
				}
			}

			// now let the actor do it's real work
			ProcessResponse response = new ProcessResponse(req);
			if (getLogger().isTraceEnabled())
				getLogger().trace("doFire() - processing request " + req);
			process(ctxt, req, response);
			if (getLogger().isTraceEnabled())
				getLogger().trace("doFire() - obtained response " + response);

			// Mark the contexts as processed.
			// Not sure if this is still relevant for v5 actors,
			// as even PUSHed messages are assumed to be handled once, in the iteration when they are offered to process().
			Iterator<MessageInputContext> allInputContexts = req.getAllInputContexts();
			while (allInputContexts.hasNext()) {
				MessageInputContext msgInputCtxt = allInputContexts.next();
				msgInputCtxt.setProcessed(true);
			}

			// and now send out the results
			MessageOutputContext[] outputs = response.getOutputs();
			if (outputs != null) {
				for (MessageOutputContext output : outputs) {
					sendOutputMsg(output.getPort(), output.getMessage());
				}
			}
			outputs = response.getOutputsInSequence();
			if (outputs != null && outputs.length > 0) {
				Long seqID = MessageFactory.getInstance().createSequenceID();
				for (int i = 0; i < outputs.length; i++) {
					MessageOutputContext context = outputs[i];
					boolean isLastMsg = (i == (outputs.length - 1));
					try {
						ManagedMessage msgInSeq = MessageFactory.getInstance().createMessageCopyInSequence(context.getMessage(), seqID, new Long(i), isLastMsg);
						sendOutputMsg(context.getPort(), msgInSeq);
					} catch (MessageException e) {
						throw new ProcessingException("Error creating output sequence msg for msg " + context.getMessage().getID(), context.getMessage(), e);
					}
				}
			}
		}

		if (getLogger().isTraceEnabled())
			getLogger().trace(getInfo() + " doFire() - exit ");
	}

	/**
	 * Overridable method to allow custom collecting and addition
	 * of received pushed input messages.
	 * By default, this just reads whatever's received (possibly nothing).
	 * But alternatively, this could e.g. block till at least one pushed msg was received etc.
	 * (check e.g. BufferedInputsActor).
	 * 
	 * @param req
	 * @throws ProcessingException 
	 */
	protected void addPushedMessages(ProcessRequest req) throws ProcessingException {
		getLogger().debug("addPushedMessages() - entry");
		try {
			if(!msgQLock.tryLock(10, TimeUnit.SECONDS)) {
				// if we did not get the lock, something is getting overcharged,
				// so refuse the task
				throw new ProcessingException("Msg Queue lock overcharged...", this, null);
			}
			while (!isFinishRequested() && !areAllInputsFinished() && pushedMessages.isEmpty()) {
				msgQNonEmpty.await(100, TimeUnit.MILLISECONDS);
			}

			while(!pushedMessages.isEmpty()) {
				req.addInputContext(pushedMessages.poll());
			};
		} catch (InterruptedException e) {
			throw new ProcessingException("Msg Queue lock interrupted...", this, null);
		} finally {
			try {msgQLock.unlock();} catch (Exception e) {}
			getLogger().debug("addPushedMessages() - exit");
		}
	}

	/**
	 * 
	 * @return true when all input ports are exhausted
	 */
	protected boolean areAllInputsFinished() {
		boolean result = true;
		for (int i = 0; i < blockingInputFinishRequests.size(); ++i) {
			result = result && blockingInputFinishRequests.get(i).booleanValue();
		}
		return result && msgProviders.isEmpty();
	}

	/**
	 * 
	 * @param ctxt
	 * @param request
	 * @param response
	 * @throws ProcessingException
	 */
	protected abstract void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException;

	/**
	 * <p>
	 * Method that should be overridden for actors that need to be able to
	 * validate their state before processing a next fire-iteration.
	 * </p>
	 * <p>
	 * E.g. it can typically be used to validate dynamic parameter settings,
	 * and/or messages received on their input ports.
	 * </p>
	 * 
	 * @param ctxt
	 * @param request
	 *            contains all messages received on the actor's input ports for
	 *            the current iteration.
	 * 
	 * @throws ValidationException
	 */
	protected void validateIteration(ActorContext ctxt, ProcessRequest request) throws ValidationException {
	}

	/**
	 * Overridable method to determine if an actor should do a validation of its
	 * state and incoming request for each iteration. <br>
	 * By default, checks on its Passerelle director what must be done. If no
	 * Passerelle director is used (but e.g. a plain Ptolemy one), it returns
	 * false.
	 * 
	 * @see validateIteration()
	 * @see doFire()
	 * @return
	 */
	protected boolean mustValidateIteration() {
		try {
			return ((Director) getDirector()).mustValidateIteration();
		} catch (ClassCastException e) {
			return false;
		}
	}

	@Override
	public Object clone(Workspace workspace) throws CloneNotSupportedException {
		final Actor actor = (Actor) super.clone(workspace);
		actor.blockingInputHandlers = new ArrayList<PortHandler>();
		actor.blockingInputFinishRequests = new ArrayList<Boolean>();
		actor.pushedMessages = new ConcurrentLinkedQueue<MessageInputContext>();
		actor.msgProviders = new HashSet<Object>();
		return actor;
	}
	
	protected Logger getLogger() {
		return logger;
	}
}
