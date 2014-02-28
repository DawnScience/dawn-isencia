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
package org.dawnsci.passerelle.parallel.actor.activator;

import org.dawnsci.passerelle.parallel.actor.ParallelWorkflowExecutor;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.isencia.passerelle.ext.ModelElementClassProvider;
import com.isencia.passerelle.ext.impl.DefaultModelElementClassProvider;
import com.isencia.passerelle.runtime.repository.FlowRepositoryService;

public class Activator implements BundleActivator {

  static final String FLOWREPOS_SERVICE_FILTER = "(&("+Constants.OBJECTCLASS+"="+FlowRepositoryService.class.getName()+")(type=FILE))";
  
  private static BundleContext context;
  private static Activator instance;
  
  private ServiceTracker<Object, Object> flowRepositorySvcTracker;

  @SuppressWarnings("rawtypes")
  private ServiceRegistration apSvcReg;

  private FlowRepositoryService flowRepositorySvc;

  @SuppressWarnings("unchecked")
  public void start(BundleContext bundleContext) throws Exception {
    Activator.context = bundleContext;
    Activator.instance = this;
    Filter reposSvcFilter = context.createFilter(FLOWREPOS_SERVICE_FILTER);
    flowRepositorySvcTracker = new ServiceTracker<Object, Object>(bundleContext, reposSvcFilter, createSvcTrackerCustomizer());
    flowRepositorySvcTracker.open();
    
    apSvcReg = context.registerService(ModelElementClassProvider.class.getName(), 
        new DefaultModelElementClassProvider(ParallelWorkflowExecutor.class), null);
  }

  public void stop(BundleContext bundleContext) throws Exception {
    apSvcReg.unregister();
    flowRepositorySvcTracker.close();
    Activator.context = null;
    Activator.instance = null;
  }
  
  public static Activator getInstance() {
    return instance;
  }
  
  public FlowRepositoryService getFlowReposSvc() {
    return flowRepositorySvc;
  }

  private ServiceTrackerCustomizer<Object, Object> createSvcTrackerCustomizer() {
    return new ServiceTrackerCustomizer<Object, Object>() {
      public void removedService(ServiceReference<Object> ref, Object svc) {
        synchronized (Activator.this) {
          if (svc == Activator.this.flowRepositorySvc) {
            Activator.this.flowRepositorySvc = null;
          } else {
            return;
          }
          context.ungetService(ref);
        }
      }

      public void modifiedService(ServiceReference<Object> ref, Object svc) {
      }

      public Object addingService(ServiceReference<Object> ref) {
        Object svc = context.getService(ref);
        synchronized (Activator.this) {
          if ((svc instanceof FlowRepositoryService) && (Activator.this.flowRepositorySvc == null)) {
            Activator.this.flowRepositorySvc = (FlowRepositoryService) svc;
          } 
        }
        return svc;
      }
    };
  }
}
