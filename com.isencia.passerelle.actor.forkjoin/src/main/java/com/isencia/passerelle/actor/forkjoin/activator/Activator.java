package com.isencia.passerelle.actor.forkjoin.activator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import com.isencia.passerelle.actor.forkjoin.Fork;
import com.isencia.passerelle.actor.forkjoin.Join;
import com.isencia.passerelle.ext.ModelElementClassProvider;
import com.isencia.passerelle.ext.impl.DefaultModelElementClassProvider;

public class Activator implements BundleActivator {

  @SuppressWarnings("rawtypes")
  private ServiceRegistration apSvcReg;
  
  @SuppressWarnings("unchecked")
  public void start(BundleContext context) throws Exception {
    apSvcReg = context.registerService(ModelElementClassProvider.class.getName(), 
        new DefaultModelElementClassProvider(
              Fork.class, 
              Join.class), 
        null);
  }

  public void stop(BundleContext context) throws Exception {
    apSvcReg.unregister();
  }
}
