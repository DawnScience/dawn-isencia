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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.ext.ErrorCollector;
import com.isencia.passerelle.ext.ErrorControlStrategy;
import com.isencia.passerelle.ext.ExecutionControlStrategy;
import com.isencia.passerelle.ext.ExecutionPrePostProcessor;
import com.isencia.passerelle.ext.FiringEventListener;
import com.isencia.passerelle.ext.impl.DefaultActorErrorControlStrategy;
import com.isencia.passerelle.ext.impl.DefaultExecutionControlStrategy;
import com.isencia.passerelle.ext.impl.DefaultExecutionPrePostProcessor;

import ptolemy.actor.Actor;
import ptolemy.actor.FiringEvent;
import ptolemy.actor.process.CompositeProcessDirector;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import ptolemy.kernel.util.Workspace;

/**
 * Besides the std Ptolemy director stuff, and creation of Passerelle's ProcessThreads, this director adds support for :
 * <ul>
 * <li> error collectors and error notifications
 * <li> ErrorControlStrategies
 * <li> FiringEventListeners
 * </ul>
 * 
 * @author erwin
 */
public abstract class ProcessDirector extends CompositeProcessDirector implements ExecutionControlStrategy {
	private static Logger logger = LoggerFactory.getLogger(ProcessDirector.class);

	/**
	 * The collection of parameters that are meant to be available to a model configurer tool.
	 * The actor's parameters that are not in this collection are not meant to be configurable,
	 * but are only meant to be used during model assembly (in addition to the public ones). 
	 */
	private Collection<Parameter> configurableParameters = new HashSet<Parameter>();
    
    /**
     * The collection of listeners for FiringEvents.
     * If the collection is empty, no events are generated.
     * If non-empty, inside the ProcessThread.run(), lots of events
     * are generated for each transition in the iteration of an actor.
     */
    private Collection<FiringEventListener> firingEventListeners = new HashSet<FiringEventListener>();

    /**
     * The collection of error collectors, to which the Director forwards
     * any reported errors.
     * If the collection is empty, reported errors are logged.
     */
	private Collection<ErrorCollector> errorCollectors = new HashSet<ErrorCollector>();
	
	private DefaultExecutionControlStrategy execCtrlStrategy = new DefaultExecutionControlStrategy();
	private ExecutionPrePostProcessor execPrePostProcessor = new DefaultExecutionPrePostProcessor();
	
	
	private ErrorControlStrategy errorCtrlStrategy = new DefaultActorErrorControlStrategy();
	// flag to allow
	private boolean enforcedErrorCtrlStrategy;
	
	// annoyingly need to maintaina copy here of the activeThreads in the Ptolemy ProcessDirector baseclass,
	// as it is not reachable from subclasses....
	private Collection<ProcessThread> myThreads = new HashSet<ProcessThread>();
	
	/**
	 * 
	 */
	public ProcessDirector() throws IllegalActionException, NameDuplicationException  {
		this(null);
	}

	/**
	 * @param workspace
	 */
	public ProcessDirector(Workspace workspace) throws IllegalActionException, NameDuplicationException  {
		super(workspace);
	}

	/**
	 * @param container
	 * @param name
	 * @throws ptolemy.kernel.util.IllegalActionException
	 * @throws ptolemy.kernel.util.NameDuplicationException
	 */
	public ProcessDirector(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
		super(container, name);
	}
	
	
	@Override
	public synchronized void addThread(Thread thread) {
		super.addThread(thread);
		if(thread instanceof ProcessThread) {
			myThreads.add((ProcessThread) thread);
		}
	}

	@Override
	public void preinitialize() throws IllegalActionException {
		super.preinitialize();
		myThreads.clear();
	}

	@Override
	public synchronized void removeThread(Thread thread) {
		super.removeThread(thread);
		if(thread instanceof ProcessThread) {
			myThreads.remove((ProcessThread) thread);
		}
	}
	
	public Collection<ProcessThread> getThreads() {
		return myThreads;
	}

	/**
	 * @return all configurable parameters
	 */
	public Parameter[] getConfigurableParameters() {
		return (Parameter[]) configurableParameters.toArray(new Parameter[0]);
	}

	/**
	 * Register an actor parameter as configurable.
	 * Such parameters will be available in the Passerelle model configuration tools.
	 * All other actor parameters are only available in model assembly tools.
	 * 
	 * @param newParameter
	 */	
	protected void registerConfigurableParameter(Parameter newParameter) {
		if(newParameter!=null && !configurableParameters.contains(newParameter)
				&& newParameter.getContainer().equals(this)) {
			configurableParameters.add(newParameter);
		}
	}
    
    /**
     * Register a listener that will be notified of ALL 
     * actor iteration transitions.
     * 
     * @see ptolemy.actor.FiringEvent
     * 
     * @param listener
     */
    public void registerFiringEventListener(FiringEventListener listener) {
        if(listener!=null)
            firingEventListeners.add(listener);
    }
    
    /**
     * 
     * @param listener
     * @return true if the listener was registered (and is now removed)
     */
    public boolean removeFiringEventListener(FiringEventListener listener) {
        return firingEventListeners.remove(listener);
    }
    
    /**
     * 
     * @return true if at least 1 listener is registered
     */
    public boolean hasFiringEventListeners() {
        return !firingEventListeners.isEmpty();
    }
    
    /**
     * Forward the event to all registered listeners, 
     * iff the event is not-null and its director is me.
     * 
     * The listener-related methods are NOT synchronized, to ensure that the
     * model execution does not block completely because of
     * a blocking/long action of a listener...
     * 
     * So there's no guarantee against race conditions when someone starts
     * modifying the listener set during model execution!
     * 
     * @param event
     */
    public void notifyFiringEventListeners(FiringEvent event) {
        if(event!=null && event.getDirector().equals(this)) {
            for (Iterator<FiringEventListener> listenerItr = firingEventListeners.iterator(); listenerItr.hasNext();) {
                FiringEventListener listener = listenerItr.next();
                listener.onEvent(event);
            }
        }
    }

    @Override
	protected ptolemy.actor.process.ProcessThread _newProcessThread(Actor actor, ptolemy.actor.process.ProcessDirector director)
		throws IllegalActionException {
		return new ProcessThread(actor, (ProcessDirector) director);
	}

    /**
     * Facade method, used by actor fire-iteration logic to determine
     * whether they can continue or should wait a bit...
     * 
     * The Director's ExecutionControlStrategy handles this request.
     * @param actor
     * @return an object identifying the current permission for the actor
	 * to do 1 iteration.
     */
    public synchronized IterationPermission requestNextIteration(Actor actor) {
    	return execCtrlStrategy.requestNextIteration(actor);
    }
    
	public void iterationFinished(Actor actor, IterationPermission itPerm) {
		execCtrlStrategy.iterationFinished(actor, itPerm);
	}
	
	/**
	 * just an alias for stopFire()...
	 */
	public void pauseAllActors() {
		stopFire();
	}
    
	public void resumeAllActors() {
        Iterator<ProcessThread> threads = myThreads.iterator();

        while (threads.hasNext()) {
        	ProcessThread thread = threads.next();

        	if(thread.getActor() instanceof com.isencia.passerelle.actor.Actor) {
        		((com.isencia.passerelle.actor.Actor)thread.getActor()).resumeFire();
        	}
        }
	}
    
    public void setExecutionControlStrategy(ExecutionControlStrategy execCtrlStrategy) {
    	this.execCtrlStrategy.setDelegate(execCtrlStrategy);
    }
    
    public ExecutionControlStrategy getExecutionControlStrategy() {
    	return execCtrlStrategy.getDelegate();
    }
    
    public ErrorControlStrategy getErrorControlStrategy() {
		return errorCtrlStrategy;
	}

	public void setErrorControlStrategy(ErrorControlStrategy errorCtrlStrategy, boolean enforceThisOne) {
		if(enforceThisOne || !this.enforcedErrorCtrlStrategy) {
			this.errorCtrlStrategy = errorCtrlStrategy;
			this.enforcedErrorCtrlStrategy = enforceThisOne;
		}
	}

	public void setExecutionPrePostProcessor(ExecutionPrePostProcessor execPrePostProcessor) {
    	this.execPrePostProcessor = execPrePostProcessor;
    }
    
    public ExecutionPrePostProcessor getExecutionPrePostProcessor() {
    	return execPrePostProcessor;
    }
    
	public void addErrorCollector(ErrorCollector errCollector) {
		if(logger.isTraceEnabled())
			logger.trace("addErrorCollector() - Adding error collector "+errCollector);
			
		if(errCollector!=null) {
			errorCollectors.add(errCollector);
		}
		
		if(logger.isTraceEnabled())
			logger.trace("addErrorCollector() - exit");
	}

	public boolean removeErrorCollector(ErrorCollector errCollector) {
		if(logger.isTraceEnabled())
			logger.trace("removeErrorCollector() - entry - Removing error collector "+errCollector);
		boolean res = false;
		if(errCollector!=null) {
			res = errorCollectors.remove(errCollector);
		} 
		if(logger.isTraceEnabled())
			logger.trace("removeErrorCollector() - exit :"+res);
		return res;
	}
	
	public void removeAllErrorCollectors() {
		if(logger.isTraceEnabled())
			logger.trace("removeAllErrorCollectors() - entry - Removing all error collectors");
		errorCollectors.clear();
		if(logger.isTraceEnabled())
			logger.trace("removeAllErrorCollectors() - exit");
	}

	public void reportError(PasserelleException e) {
		if(logger.isTraceEnabled())
			logger.trace("reportError() - entry - Reporting error :"+e);
			
		if(!errorCollectors.isEmpty()) {
			for (Iterator<ErrorCollector> errCollItr = errorCollectors.iterator(); errCollItr.hasNext();) {
				ErrorCollector element = errCollItr.next();
				element.acceptError(e);
				if(logger.isDebugEnabled())
					logger.debug("Reported error to "+element);
			}
		} else {
			logger.error("reportError() - no errorCollectors but received exception",e);
		}
		
		if(logger.isTraceEnabled())
			logger.trace("reportError() - exit");
	}

	public void initialize() throws IllegalActionException {
		if(logger.isTraceEnabled())
			logger.trace(getName()+" initialize() - entry");
		getExecutionPrePostProcessor().preProcess();
		super.initialize();
		if(logger.isTraceEnabled())
			logger.trace(getName()+" initialize() - exit");
	}

	public void wrapup() throws IllegalActionException {
		if(logger.isTraceEnabled())
			logger.trace(getName()+" wrapup() - entry");
		getExecutionPrePostProcessor().postProcess();
		super.wrapup();
		if(logger.isTraceEnabled())
			logger.trace(getName()+" wrapup() - exit");
	}

	public void terminate() {
		if(logger.isTraceEnabled())
			logger.trace(getName()+" terminate() - entry");
		getExecutionPrePostProcessor().postProcess();
		super.terminate();
		if(logger.isTraceEnabled())
			logger.trace(getName()+" terminate() - exit");
	}
}
