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
import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ptolemy.data.IntToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;

import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.ValidationException;
import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.actor.v5.ActorContext;
import com.isencia.passerelle.actor.v5.ProcessRequest;
import com.isencia.passerelle.actor.v5.ProcessResponse;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.runtime.process.ProcessStatus;
import com.isencia.passerelle.runtime.repos.impl.filesystem.FlowHandleImpl;
import com.isencia.passerelle.runtime.repository.VersionSpecification;
import com.isencia.passerelle.util.ptolemy.FileParameter;

/**
 * This actor can execute another workflow N times in parallel on the available/configured number of CPU cores.
 * <p>
 * Implementation info : it uses the JDK 7 ForkJoin support.
 * </p>
 * 
 * @author erwindl
 * 
 */
public class ParallelWorkflowExecutor extends Actor {
  private static final String SUB_WORKFLOW_PARAMNAME = "Sub Workflow";

  private static final String MAX_PARALLELISM_PARAMNAME = "Max parallelism";

  private static final long serialVersionUID = 9425431903735722L;

  private final static Logger LOGGER = LoggerFactory.getLogger(ParallelWorkflowExecutor.class);

  // ForkJoinPool has a private MAX_ID = 0x7fff.
  // But we can not access it, and I think it's a bit too muxh for our purposes.
  private final static int ABSOLUTE_MAX_PARALLELISM = 1000;

  public Parameter maxParallelismParam;
  public FileParameter modelParam;

  public Port input;
  public Port output;

  public ParallelWorkflowExecutor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);
    // TODO limit body content to DLS DataMessageComponent
    input = PortFactory.getInstance().createInputPort(this, null);
    output = PortFactory.getInstance().createOutputPort(this);

    maxParallelismParam = new Parameter(this, MAX_PARALLELISM_PARAMNAME);
    maxParallelismParam.setTypeEquals(BaseType.INT);
    modelParam = new FileParameter(this, SUB_WORKFLOW_PARAMNAME, "Workflows", "xml", "moml");
  }

  @Override
  protected void validateInitialization() throws ValidationException {
    super.validateInitialization();
    try {
      if (!modelParam.asFile().exists()) {
        throw new ValidationException(ErrorCode.ACTOR_INITIALISATION_ERROR, modelParam.getValueAsString() + " not found", modelParam, null);
      }
    } catch (ValidationException e) {
      throw e;
    } catch (Exception e) {
      throw new ValidationException(ErrorCode.ACTOR_INITIALISATION_ERROR, "Unable to obtain " + SUB_WORKFLOW_PARAMNAME, modelParam, e);
    }
    try {
      IntToken t = (IntToken) maxParallelismParam.getToken();
      if (t != null) {
        int mp = t.intValue();
        if (mp <= 0 || mp > ABSOLUTE_MAX_PARALLELISM) {
          throw new ValidationException(ErrorCode.ACTOR_INITIALISATION_ERROR, mp + " is invalid: should be in range 1-" + ABSOLUTE_MAX_PARALLELISM,
              maxParallelismParam, null);
        }
      }
    } catch (Exception e) {
      throw new ValidationException(ErrorCode.ACTOR_INITIALISATION_ERROR, "Unable to obtain " + MAX_PARALLELISM_PARAMNAME, maxParallelismParam, e);
    }
  }

  @Override
  protected void process(ActorContext ctxt, ProcessRequest request, ProcessResponse response) throws ProcessingException {
    try {
      ManagedMessage message = request.getMessage(input);
//      DataMessageComponent dataMsgComp = null;
//      try {
//        dataMsgComp = MessageUtils.coerceMessage(message);
//        IMetaData metaData = dataMsgComp.getMeta();
//        System.out.println(metaData);
//        
//        Map<String, Serializable> dataSetMap = dataMsgComp.getList();
//        System.out.println(dataSetMap);
//        IDataset ds = (IDataset) dataMsgComp.getList("image");
//        System.out.println("Shape of the image : "+Arrays.toString(ds.getShape()));
//
//        // how to obtain e.g. the paths/names of all images in a dynamic way?
//        
//      } catch (Exception e) {
//        throw new DataMessageException(ErrorCode.ERROR, "Cannot add data", this, message, dataMsgComp, e);
//      }
      
      // FlowRepositoryService localReposSvc = Activator.getInstance().getLocalReposSvc();
      FlowHandleImpl subFlowHandle = new FlowHandleImpl("test", modelParam.asFile(), VersionSpecification.parse("1.0.0"));
      ForkJoinPool fjPool = new ForkJoinPool(5);
      Map<String, String>[] paramOverridesArray = new Map[5];
      for(int i=0;i<5;++i) {
        Map<String, String> paramOverrides = new HashMap<>();
        paramOverrides.put("inputPath", "c:/temp/testinput"+i+".txt");
        paramOverridesArray[i] = paramOverrides;
      }
      ModelRunnerTask task = new ModelRunnerTask(subFlowHandle, null, 10, TimeUnit.SECONDS, paramOverridesArray);
      ProcessStatus processStatus = fjPool.invoke(task);
      System.out.println(processStatus);
      response.addOutputMessage(output, message);
    } catch (IllegalActionException e) {
      // should not happen as all params were validated during initialization validation
      throw new ProcessingException(ErrorCode.ACTOR_EXECUTION_ERROR, "Error getting actor configuration", this, e);
    }
  }

  @Override
  public Logger getLogger() {
    return LOGGER;
  }
}
