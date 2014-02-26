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
package org.dawnsci.drmaa.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.dawnsci.drmaa.executor.impl.JobExecutor;
import org.ggf.drmaa.AlreadyActiveSessionException;
import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.JobInfo;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.NoActiveSessionException;
import org.ggf.drmaa.Session;
import org.ggf.drmaa.Version;

/**
 * A Session implementation that executes jobs on a JDK ExecutorService.
 * 
 * @author erwindl
 * 
 */
public class SessionImpl implements Session {

  private static AtomicInteger idCounter = new AtomicInteger();

  private boolean initialized = false;

  private static Map<String, JobTemplate> jobTemplates = new HashMap<>();
  private Map<String, JobExecutor> jobExecutors = new HashMap<>();

  private ExecutorService jobExecutorService = Executors.newFixedThreadPool(3);

  @Override
  public void init(String contact) throws DrmaaException {
    if (initialized) {
      throw new AlreadyActiveSessionException();
    } else {
      initialized = true;
    }
  }

  @Override
  public void exit() throws DrmaaException {
    if (!initialized) {
      throw new NoActiveSessionException();
    } else {
      initialized = false;
    }
  }

  @Override
  public JobTemplate createJobTemplate() throws DrmaaException {
    if (!initialized) {
      throw new NoActiveSessionException();
    } else {
      String jobId = Integer.toString(idCounter.getAndIncrement());
      JobTemplate jobTemplate = new JobTemplateImpl(jobId);
      jobTemplates.put(jobId, jobTemplate);
      return jobTemplate;
    }
  }

  @Override
  public void deleteJobTemplate(JobTemplate jt) throws DrmaaException {
    if (!initialized) {
      throw new NoActiveSessionException();
    } else {
      jobTemplates.remove(((JobTemplateImpl) jt).getId());
    }
  }

  @Override
  public String runJob(JobTemplate jt) throws DrmaaException {
    if (!initialized) {
      throw new NoActiveSessionException();
    } else {
      JobExecutor jobExecutor = new JobExecutor((JobTemplateImpl) jt);
      jobExecutors.put(jobExecutor.getId(), jobExecutor);
      jobExecutorService.submit(jobExecutor);
      return jobExecutor.getId();
    }
  }

  @Override
  public List<String> runBulkJobs(JobTemplate jt, int start, int end, int incr) throws DrmaaException {
    if (!initialized) {
      throw new NoActiveSessionException();
    } else {
      List<String> results = new ArrayList<>();
      for(int i = start; i < end; i += incr) {
        JobExecutor jobExecutor = new JobExecutor((JobTemplateImpl) jt, i);
        jobExecutors.put(jobExecutor.getId(), jobExecutor);
        jobExecutorService.submit(jobExecutor);
        results.add(jobExecutor.getId());
      }
      return results;
    }
  }

  @Override
  public void control(String jobId, int action) throws DrmaaException {
    // TODO Auto-generated method stub

  }

  @Override
  public void synchronize(List<String> jobIds, long timeout, boolean dispose) throws DrmaaException {
    // TODO Auto-generated method stub

  }

  @Override
  public JobInfo wait(String jobId, long timeout) throws DrmaaException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int getJobProgramStatus(String jobId) throws DrmaaException {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public String getContact() {
    return "localhost";
  }

  @Override
  public Version getVersion() {
    return new Version(1, 0);
  }

  @Override
  public String getDrmSystem() {
    return "DAWNSCI local Executor DRM";
  }

  @Override
  public String getDrmaaImplementation() {
    return "org.dawnsci.drmaa.executor";
  }

}
