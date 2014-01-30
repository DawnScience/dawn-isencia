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
import com.isencia.passerelle.runtime.process.FlowProcessingService;
import com.isencia.passerelle.runtime.repository.FlowRepositoryService;

public class Activator implements BundleActivator {

  static final String FLOWREPOS_SERVICE_FILTER = "(&("+Constants.OBJECTCLASS+"="+FlowRepositoryService.class.getName()+")(type=FILE))";
  static final String FLOWPROC_SERVICE_FILTER = "("+Constants.OBJECTCLASS+"="+FlowProcessingService.class.getName()+")";
  
  static final String FLOWREPOS_MXBEAN_NAME = "com.isencia.passerelle.runtime:type=FlowRepository";
  static final String FLOWPROCESSOR_MXBEAN_NAME = "com.isencia.passerelle.runtime:type=FlowProcessor";
  
  private static BundleContext context;
  private static Activator instance;
  
  private ServiceTracker<Object, Object> flowRepositorySvcTracker;
  private ServiceTracker<Object, Object> flowProcessingSvcTracker;

  @SuppressWarnings("rawtypes")
  private ServiceRegistration apSvcReg;

  private FlowRepositoryService flowRepositorySvc;
  private FlowProcessingService flowProcessingSvc;

  @SuppressWarnings("unchecked")
  public void start(BundleContext bundleContext) throws Exception {
    Activator.context = bundleContext;
    Activator.instance = this;
    Filter reposSvcFilter = context.createFilter(FLOWREPOS_SERVICE_FILTER);
    Filter processSvcFilter = context.createFilter(FLOWPROC_SERVICE_FILTER);
    flowRepositorySvcTracker = new ServiceTracker<Object, Object>(bundleContext, reposSvcFilter, createSvcTrackerCustomizer());
    flowProcessingSvcTracker = new ServiceTracker<Object, Object>(bundleContext, processSvcFilter, createSvcTrackerCustomizer());
    flowRepositorySvcTracker.open();
    flowProcessingSvcTracker.open();
    
    apSvcReg = context.registerService(ModelElementClassProvider.class.getName(), 
        new DefaultModelElementClassProvider(ParallelWorkflowExecutor.class), null);
  }

  public void stop(BundleContext bundleContext) throws Exception {
    apSvcReg.unregister();
    flowRepositorySvcTracker.close();
    flowProcessingSvcTracker.close();
    Activator.context = null;
    Activator.instance = null;
  }
  
  public static Activator getInstance() {
    return instance;
  }
  
  public FlowRepositoryService getFlowReposSvc() {
    return flowRepositorySvc;
  }

  public FlowProcessingService getFlowProcessingSvc() {
    return flowProcessingSvc;
  }

  private ServiceTrackerCustomizer<Object, Object> createSvcTrackerCustomizer() {
    return new ServiceTrackerCustomizer<Object, Object>() {
      public void removedService(ServiceReference<Object> ref, Object svc) {
        synchronized (Activator.this) {
          if (svc == Activator.this.flowRepositorySvc) {
            Activator.this.flowRepositorySvc = null;
          } else if(svc == Activator.this.flowProcessingSvc) {
            Activator.this.flowProcessingSvc = null;
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
          } else if ((svc instanceof FlowProcessingService) && (Activator.this.flowProcessingSvc == null)) {
            Activator.this.flowProcessingSvc = (FlowProcessingService) svc;
          } 
        }
        return svc;
      }
    };
  }
}
