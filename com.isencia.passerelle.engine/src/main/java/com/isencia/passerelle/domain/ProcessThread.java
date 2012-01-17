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
package com.isencia.passerelle.domain;

import java.io.InterruptedIOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.passerelle.executor.ExecutionContext;
import com.isencia.passerelle.ext.ExecutionControlStrategy;
import com.isencia.passerelle.util.LoggerManager;

import ptolemy.actor.Actor;
import ptolemy.actor.FiringEvent;
import ptolemy.actor.Manager;
import ptolemy.actor.process.TerminateProcessException;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NamedObj;
import ptolemy.kernel.util.Workspace;

/**
 * A Passerelle extension to the std Ptolemy ProcessThread to add extra
 * features:
 * <ul>
 * <li>logging
 * <li>firing events during actor iteration
 * <ul>
 * 
 * <p>
 * implementation remark : updated to ptolemy 7.0.1, but ignoring their
 * FiringsRecordable stuff, as it seems to be incomplete anyway. I.e. it's not
 * compatible with the more complete event logging in ...Actor.iterate(). So we
 * just maintain our own FiringEvent handling for now.
 * </p>
 * 
 * @author erwin
 */
public class ProcessThread extends ptolemy.actor.process.ProcessThread {
	private final static Logger logger = LoggerFactory.getLogger(ProcessThread.class);

	public static final String ACTOR_MDC_NAME = "actor";
	private ExecutionContext ctxt = null;

	/**
	 * @param actor
	 * @param director
	 */
	public ProcessThread(Actor actor, ProcessDirector director) {
		super(actor, director);
		_director = director;
		_manager = actor.getManager();

		if (actor != null) {
			String actorInfo = "none";
			if (actor instanceof NamedObj) {
				actorInfo = actor.getManager().getName() + ((NamedObj) actor).getFullName();
			} else {
				actorInfo = actor.getManager().getName() + "." + actor.getClass().getName();
			}

			ctxt = ExecutionContext.getExecutionContext(actor.getManager());
			if (ctxt != null)
				ctxt.setAttribute(ACTOR_MDC_NAME, actorInfo);
		}

		firingEventCache = new FiringEvent[] { new FiringEvent(_director, actor, FiringEvent.BEFORE_ITERATE),
				new FiringEvent(_director, actor, FiringEvent.BEFORE_PREFIRE), new FiringEvent(_director, actor, FiringEvent.AFTER_PREFIRE),
				new FiringEvent(_director, actor, FiringEvent.BEFORE_FIRE), new FiringEvent(_director, actor, FiringEvent.AFTER_FIRE),
				new FiringEvent(_director, actor, FiringEvent.BEFORE_POSTFIRE), new FiringEvent(_director, actor, FiringEvent.AFTER_POSTFIRE),
				new FiringEvent(_director, actor, FiringEvent.AFTER_ITERATE), };
	}

	public String toString() {
		if(getActor()!=null)
			return getActor().getFullName();
		else
			return super.toString();
	}
	/**
	 * Overriding the ptolemy ProcessThread.run() to add events
	 */
	public void run() {
		LoggerManager.setContext(ctxt);
		if (logger.isInfoEnabled()) {
			logger.info(getActor().getFullName() + " - Starting ProcessThread");
		}
		_debug("-- Starting thread.");
		Workspace workspace = _director.workspace();
		boolean iterate = true;
		Throwable thrownWhenIterate = null;
		Throwable thrownWhenWrapup = null;
		try {
			// Initialize the actor.
			getActor().initialize();

			// While postfire() returns true and stop() is not called.
			while (iterate) {

				// check for synchronization/stepping/... of this actor's
				// iterations
//				ExecutionControlStrategy.IterationPermission itrPerm = _director.requestNextIteration(getActor());
				try {
					// NOTE: Possible race condition... actor.stop()
					// might be called before we get to this.
					// This will cause postfire() on the actor
					// to return false, which will stop its execution.
					if (_director.isStopFireRequested()) {
						// And wait until the flag has been cleared.
						_debug("-- Thread pause requested. Get lock on director.");
						synchronized (_director) {
							// Tell the director we're stopped (necessary
							// for deadlock detection).
							_director.threadHasPaused(this);

							while (_director.isStopFireRequested()) {
								// If a stop has been requested, in addition
								// to a stopFire, then stop execution
								// altogether and skip to wrapup().
								if (_director.isStopRequested()) {
									_debug("-- Thread stop requested, so cancel iteration.");
									break;
								}
								_debug("-- Thread waiting for canceled pause request.");
								try {
									workspace.wait(_director);
								} catch (InterruptedException ex) {
									_debug("-- Thread interrupted, so cancel iteration.");
									break;
								}
							}
							// NOTE: Do we need to indicate that actor has
							// restarted, with something like
							_director.threadHasResumed(this);
						}
						_debug("-- Thread resuming.");
					}

					if (_director.isStopRequested()) {
						break;
					}

					// container is checked for null to detect the
					// deletion of the actor from the topology.
					if (((Entity) getActor()).getContainer() != null) {
						if (_director.hasFiringEventListeners()) {
							iterate = doActorIterationWithEvents();
						} else {
							iterate = doActorIterationWithoutEvents();
						}
					}
				} finally {
//					_director.iterationFinished(getActor(), itrPerm);
				}
			}
			if (logger.isInfoEnabled()) {
				logger.info(getActor().getFullName() + " - Clean termination of ProcessThread");
			}
		} catch (Throwable t) {
			thrownWhenIterate = t;
			logger.error(getActor().getFullName() + " - Error in ProcessThread", t);
			t.printStackTrace();
		} finally {
			// Let the director know that this thread stopped.
			// This is synchronized to prevent a race condition
			// where the director might conclude before the
			// call to wrapup() below.
			synchronized (_director) {
				_director.removeThread(this);

			try {
				// NOTE: Deadlock risk here if wrapup is done inside
				// a block synchronized on the _director, as it used to be.
				// Holding a lock on the _director during wrapup()
				// might cause deadlock with hierarchical models where
				// wrapup() waits for internal actors to conclude,
				// doing a wait() on its own internal director,
				// or trying to acquire a write lock on the workspace.
				// Meanwhile, this thread will hold a lock on this
				// outside director, which may prevent the other
				// threads from releasing their write lock!
				wrapup();
			} catch (IllegalActionException e) {
				thrownWhenWrapup = e;
			} finally {
				_debug("-- Thread stopped.");

				boolean rethrow = false;

				if (thrownWhenIterate instanceof TerminateProcessException) {
					// Process was terminated.
					_debug("-- Blocked Receiver call threw TerminateProcessException.");
				} else if (thrownWhenIterate instanceof InterruptedException) {
					// Process was terminated by call to stop();
					_debug("-- Thread was interrupted: " + thrownWhenIterate);
				} else if (thrownWhenIterate instanceof InterruptedIOException
						|| ((thrownWhenIterate != null) && thrownWhenIterate.getCause() instanceof InterruptedIOException)) {
					// PSDF has problems here when run with JavaScope
					_debug("-- IO was interrupted: " + thrownWhenIterate);
				} else if (thrownWhenIterate instanceof IllegalActionException) {
					_debug("-- Exception: " + thrownWhenIterate);
					_manager.notifyListenersOfException((IllegalActionException) thrownWhenIterate);
				} else if (thrownWhenIterate != null) {
					rethrow = true;
				}

				if (thrownWhenWrapup instanceof IllegalActionException) {
					_debug("-- Exception: " + thrownWhenWrapup);
					_manager.notifyListenersOfException((IllegalActionException) thrownWhenWrapup);
				} else if (thrownWhenWrapup != null) {
					// Must be a runtime exception.
					// Call notifyListenerOfThrowable() here so that
					// the stacktrace appears in the UI and not in stderr.
					_manager.notifyListenersOfThrowable(thrownWhenWrapup);
				} else if (rethrow) {
					_manager.notifyListenersOfThrowable(thrownWhenIterate);
				}
			}
		}
		LoggerManager.clearContext(ctxt);
	}
	}

	/**
	 * @return
	 * @throws IllegalActionException
	 */
	private boolean doActorIterationWithoutEvents() throws IllegalActionException {
		boolean iterate = true;
		boolean preFireOK = getActor().prefire();
		if (preFireOK) {
			getActor().fire();
			iterate = getActor().postfire();
		}
		return iterate;
	}

	/**
	 * @return
	 * @throws IllegalActionException
	 */
	private boolean doActorIterationWithEvents() throws IllegalActionException {
		boolean iterate = true;
		_director.notifyFiringEventListeners(firingEventCache[0]);
		_director.notifyFiringEventListeners(firingEventCache[1]);
		boolean preFireOK = getActor().prefire();
		_director.notifyFiringEventListeners(firingEventCache[2]);
		if (preFireOK) {
			_director.notifyFiringEventListeners(firingEventCache[3]);
			getActor().fire();
			_director.notifyFiringEventListeners(firingEventCache[4]);
			_director.notifyFiringEventListeners(firingEventCache[5]);
			iterate = getActor().postfire();
			_director.notifyFiringEventListeners(firingEventCache[6]);
			_director.notifyFiringEventListeners(firingEventCache[7]);
		}
		return iterate;
	}

	// /////////////////////////////////////////////////////////////////
	// // private variables ////

	private ProcessDirector _director;
	private Manager _manager;

	// a simple cache for all event types, so we don't need to construct new
	// ones for every iteration
	private FiringEvent[] firingEventCache;
}
