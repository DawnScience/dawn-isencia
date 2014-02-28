/*
 * Copyright 2014 Diamond Light Source Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dawnsci.passerelle.parallel.actor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.actor.CompositeActor;
import ptolemy.actor.ExecutionListener;
import ptolemy.actor.Manager.State;
import ptolemy.data.expr.Parameter;
import ptolemy.kernel.Entity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.Workspace;

import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.Manager;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.model.Flow;
import com.isencia.passerelle.runtime.FlowHandle;
import com.isencia.passerelle.runtime.process.ProcessStatus;

/**
 * 
 * @author erwindl
 * 
 */
public class ModelRunnerTask extends RecursiveTask<ProcessStatus> implements ExecutionListener {
  private static final long serialVersionUID = -8745908641627156889L;
  private static final Logger LOGGER = LoggerFactory.getLogger(ModelRunnerTask.class);

  private final static Map<State, ProcessStatus> STATUS_MAPPING = new HashMap<Manager.State, ProcessStatus>();

  private final Map<String, String>[] parameterOverrides;
  private final FlowHandle flowHandle;
  private final String processContextId;
  private volatile ProcessStatus status;
  private final long timeOut;
  private final TimeUnit timeOutUnit;
  private Manager manager;

  @SafeVarargs
  public ModelRunnerTask(final FlowHandle flowHandle, final String processContextId, final long timeOut, final TimeUnit timeOutUnit,
      final Map<String, String>... parameterOverrides) {
    if (flowHandle == null)
      throw new IllegalArgumentException("FlowHandle can not be null");
    this.flowHandle = flowHandle;
    if (processContextId == null || processContextId.trim().length() == 0) {
      this.processContextId = UUID.randomUUID().toString();
    } else {
      this.processContextId = processContextId;
    }
    this.parameterOverrides = parameterOverrides;
    this.timeOut = timeOut;
    this.timeOutUnit = timeOutUnit;
    this.status = ProcessStatus.IDLE;
  }

  public FlowHandle getFlowHandle() {
    return flowHandle;
  }

  /**
   * @return the process context ID for this execution
   */
  public String getProcessContextId() {
    return processContextId;
  }

  /**
   * @return the current flow execution status
   */
  public ProcessStatus getStatus() {
    return status;
  }

  @Override
  protected ProcessStatus compute() {
    try {
      if (parameterOverrides.length == 1) {
        return executeOneModel(parameterOverrides[0]);
      } else if (parameterOverrides.length == 0) {
        return executeOneModel(null);
      } else {
        Set<ModelRunnerTask> tasks = new HashSet<>();
        for (Map<String, String> oneParameterMap : parameterOverrides) {
          ModelRunnerTask t = new ModelRunnerTask(flowHandle, null, timeOut, timeOutUnit, oneParameterMap);
          tasks.add((ModelRunnerTask) t.fork());
        }
        for (ModelRunnerTask t : tasks) {
          System.out.println(t.getFlowHandle().getCode() + ":" + t.join());
        }
        return ProcessStatus.FINISHED;
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private ProcessStatus executeOneModel(Map<String, String> parameters) throws PasserelleException {
    LOGGER.trace("executeOneModel() - Context {} - Flow {}", processContextId, flowHandle.getCode());
    try {
      synchronized (this) {
        Flow flow = (Flow) flowHandle.getFlow().clone(new Workspace());
        applyParameterSettings(flowHandle, flow, parameters);
        manager = new Manager(flow.workspace(), processContextId);
        manager.addExecutionListener(this);
        flow.setManager(manager);
      }
      LOGGER.info("Context {} - Starting execution of flow {}", processContextId, flowHandle.getCode());
      manager.execute();
      // Just to be sure that for blocking executes,
      // we don't miss the final manager state changes before returning.
      managerStateChanged(manager);
    } catch (Exception e) {
      executionError(manager, e);
      if (e.getCause() instanceof PasserelleException) {
        throw ((PasserelleException) e.getCause());
      } else {
        throw new PasserelleException(ErrorCode.FLOW_EXECUTION_ERROR, flowHandle.toString(), e);
      }
    }
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace("executeOneModel() exit - Context {} - Flow {} - Final Status {}", new Object[] { processContextId, flowHandle.getCode(), status });
    }
    return status;
  }

  /**
   * Updates the flow execution status to <code>ProcessStatus.ERROR</code>
   */
  @Override
  public void executionError(ptolemy.actor.Manager manager, Throwable throwable) {
    LOGGER.warn("Context " + processContextId + " - Execution error of flow " + getFlowHandle().getCode(), throwable);
    status = ProcessStatus.ERROR;
  }

  /**
   * Updates the flow execution status to <code>ProcessStatus.FINISHED</code>, or <code>ProcessStatus.INTERRUPTED</code>
   * if the execution finished due to a cancel.
   */
  @Override
  public void executionFinished(ptolemy.actor.Manager manager) {
    if (status == null || !status.isFinalStatus()) {
      LOGGER.info("Context {} - Execution finished of flow {}", processContextId, getFlowHandle().getCode());
      status = ProcessStatus.FINISHED;
    }
  }

  /**
   * Changes the flow execution status according to the new manager state.
   */
  @Override
  public void managerStateChanged(ptolemy.actor.Manager manager) {
    State state = manager.getState();
    LOGGER.trace("Context {} - Manager state change of flow {} : {}", new Object[] { processContextId, getFlowHandle().getCode(), state });
    if (status == null || !status.isFinalStatus()) {
      ProcessStatus oldStatus = status;
      status = STATUS_MAPPING.get(state);
      if (oldStatus != status) {
        LOGGER.info("Context {} - Execution state change of flow {} : {}", new Object[] { processContextId, getFlowHandle().getCode(), status });
      }
    }
  }

  protected void applyParameterSettings(FlowHandle flowHandle, Flow flow, Map<String, String> props) throws PasserelleException {
    if (props != null) {
      Iterator<Entry<String, String>> propsItr = props.entrySet().iterator();
      while (propsItr.hasNext()) {
        Entry<String, String> element = propsItr.next();
        String propName = element.getKey();
        String propValue = element.getValue();
        String[] nameParts = propName.split("[\\.]");

        // set model parameters
        if (nameParts.length == 1 && !flow.attributeList().isEmpty()) {
          try {
            final Parameter p = (Parameter) flow.getAttribute(nameParts[0], Parameter.class);
            setParameter(flowHandle, p, propName, propValue);
          } catch (final IllegalActionException e1) {
            throw new PasserelleException(ErrorCode.FLOW_CONFIGURATION_ERROR, "Inconsistent parameter definition " + propName, flow, e1);
          }
        }
        // parts[parts.length-1] is the parameter name
        // all the parts[] before that are part of the nested Parameter name
        Entity e = flow;
        for (int j = 0; j < nameParts.length - 1; j++) {
          if (e instanceof CompositeActor) {
            Entity test = ((CompositeActor) e).getEntity(nameParts[j]);
            if (test == null) {
              try {
                // maybe it is a director
                ptolemy.actor.Director d = ((CompositeActor) e).getDirector();
                if (d != null) {
                  Parameter p = (Parameter) d.getAttribute(nameParts[nameParts.length - 1], Parameter.class);
                  setParameter(flowHandle, p, propName, propValue);
                }
              } catch (IllegalActionException e1) {
                throw new PasserelleException(ErrorCode.FLOW_CONFIGURATION_ERROR, "Inconsistent parameter definition " + propName, flow, e1);
              }
            } else {
              e = ((CompositeActor) e).getEntity(nameParts[j]);
              if (e != null) {
                try {
                  Parameter p = (Parameter) e.getAttribute(nameParts[nameParts.length - 1], Parameter.class);
                  setParameter(flowHandle, p, propName, propValue);
                } catch (IllegalActionException e1) {
                  throw new PasserelleException(ErrorCode.FLOW_CONFIGURATION_ERROR, "Inconsistent parameter definition " + propName, flow, e1);
                }
              }
            }
          } else {
            break;
          }
        }
      }
    }
  }

  private void setParameter(FlowHandle flowHandle, final Parameter p, String propName, String propValue) throws PasserelleException {
    if (p != null) {
      p.setExpression(propValue);
      p.setPersistent(true);
      LOGGER.info("Context {} - Flow {} - Override parameter {} : {}", new Object[] { processContextId, flowHandle.getCode(), propName, propValue });
    } else if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Context {} - Flow {} - Unknown parameter, no override : {} ", new Object[] { processContextId, flowHandle.getCode(), propName });
    }
  }

  static {
    STATUS_MAPPING.put(Manager.IDLE, ProcessStatus.IDLE);
    STATUS_MAPPING.put(Manager.INITIALIZING, ProcessStatus.STARTING);
    STATUS_MAPPING.put(Manager.PREINITIALIZING, ProcessStatus.STARTING);
    STATUS_MAPPING.put(Manager.RESOLVING_TYPES, ProcessStatus.STARTING);
    STATUS_MAPPING.put(Manager.ITERATING, ProcessStatus.ACTIVE);
    STATUS_MAPPING.put(Manager.PAUSED, ProcessStatus.SUSPENDED);
    STATUS_MAPPING.put(Manager.PAUSED_ON_BREAKPOINT, ProcessStatus.SUSPENDED);
    STATUS_MAPPING.put(Manager.WRAPPING_UP, ProcessStatus.STOPPING);
    STATUS_MAPPING.put(Manager.EXITING, ProcessStatus.STOPPING);
    STATUS_MAPPING.put(Manager.CORRUPTED, ProcessStatus.ERROR);
    STATUS_MAPPING.put(Manager.THROWING_A_THROWABLE, ProcessStatus.ERROR);
  }
}
