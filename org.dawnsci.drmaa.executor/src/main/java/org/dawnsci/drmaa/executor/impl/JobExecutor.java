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
package org.dawnsci.drmaa.executor.impl;

import java.util.concurrent.Callable;

import org.dawnsci.drmaa.executor.JobTemplateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobExecutor implements Callable<Void> {
  private final static Logger LOGGER = LoggerFactory.getLogger(JobExecutor.class);
  private final static String LINE_SEPARATOR = System.getProperty("line.separator");

  private JobTemplateImpl jobTemplate;
  private Integer bulkJobIndex;

  public JobExecutor(JobTemplateImpl jt) {
    this.jobTemplate = jt;
  }
  
  public JobExecutor(JobTemplateImpl jt, int bulkJobIndex) {
    this.jobTemplate = jt;
    this.bulkJobIndex = bulkJobIndex;
  }
  
  public String getId() {
    return jobTemplate.getId();
  }
  
  public boolean isBulkJob() {
    return bulkJobIndex!=null;
  }
  
  public Integer getBulkJobIndex() {
    return bulkJobIndex;
  }

  @Override
  public Void call() throws Exception {
    ManagedCommandline cmdLine = new ManagedCommandline(jobTemplate.getRemoteCommand());
    cmdLine.setWorkingDirectory(jobTemplate.getWorkingDirectory());
    for(String arg : jobTemplate.getArgs()) {
      cmdLine.createArgument().setValue(arg);
    }
    try {
      Process process = cmdLine.execute();
      LOGGER.info("Started Job {} , using {}", jobTemplate.getJobName(), cmdLine.getExecutable());
      process = cmdLine.waitForProcessFinished();
      int waitFor = process.waitFor();
      LOGGER.info("exit code " + waitFor);
      LOGGER.debug("Process output" + LINE_SEPARATOR + "\t" + cmdLine.getStdoutAsString());
      LOGGER.debug("Process error" + LINE_SEPARATOR + "\t" + cmdLine.getStderrAsString());
    } catch (Exception e) {
      LOGGER.error("Error starting Job "+jobTemplate.getJobName(), e);
    }
    return null;
  }

}
