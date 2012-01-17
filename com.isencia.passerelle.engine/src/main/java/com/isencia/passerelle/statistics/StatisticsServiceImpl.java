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

package com.isencia.passerelle.statistics;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;

/**
 * 
 * A utility class to provide a simple access to the Passerelle engine's
 * JMX server, used to publish execution statistics and to allow external
 * management of an executing Passerelle solution assembly.
 * 
 * @author erwin
 *
 */
class StatisticsServiceImpl extends StatisticsServiceDummyImpl implements StatisticsService {
	
	private MBeanServer svr;
//	private ObjectName adapterName;
	private Set<ObjectName> registeredNames = new HashSet<ObjectName>();
//	private CommunicatorServer adapter;
	
	protected StatisticsServiceImpl() {
		try {
			svr = ManagementFactory.getPlatformMBeanServer();
			
			start();
		} catch (Exception e) {
			// todo Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String getServerName() {
		return "TestSvr";
	}

//	private int getServerPort() {
//		return 8000;
//	}
//	
	public void registerStatistics(NamedStatistics s) {
		try {
			ObjectName objName = new ObjectName(getServerName()+":name="+s.getName());
			registerMBean(s, objName);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void registerMBean(Object s, ObjectName objName) throws MBeanRegistrationException, NotCompliantMBeanException {
		try {
			if(svr.isRegistered(objName)) {
				try {
					svr.unregisterMBean(objName);
				} catch (InstanceNotFoundException e) {
					// should not happen...
				}
			}
			svr.registerMBean(s,objName);
			registeredNames.add(objName);
		} catch (InstanceAlreadyExistsException e) {
			// should not happen...
		}
	}
	
	public synchronized void start() {
//		if(adapter==null || !adapter.isActive()) {
//			try {
//				adapterName = new ObjectName(getServerName()+":name=htmladapter,port="+getServerPort());
//	
//				adapter = new HtmlAdaptorServer();
//				adapter.setPort(getServerPort());
//				registerMBean(adapter, adapterName);
//				adapter.start();
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}

	public synchronized void stop() {
//		if(adapter!=null) {
//			try {
//				adapter.stop();
//				svr.unregisterMBean(adapterName);
//				adapterName=null;
//				adapter=null;
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
	}
	
	public void reset() {
		stop();
		for (Iterator<ObjectName> mbNameItr = registeredNames.iterator(); mbNameItr.hasNext();) {
			ObjectName mbName = mbNameItr.next();
			try {
				svr.unregisterMBean(mbName);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
