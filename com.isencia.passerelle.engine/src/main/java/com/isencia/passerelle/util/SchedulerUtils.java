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
package com.isencia.passerelle.util;

import java.util.Properties;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SchedulerUtils
 * 
 * TODO: class comment
 * 
 * @author erwin
 */
public class SchedulerUtils {
	private final static Logger logger = LoggerFactory.getLogger(SchedulerUtils.class);
	
	private static final String ORG_QUARTZ_SCHEDULER_INSTANCENAME = "org.quartz.scheduler.instanceName";
	private static final String PASSERELLE_QUARTZ_PROPERTIES = "passerelle-quartz.properties";
	
	private static Properties schedProps=new Properties();
	static {
		try {
			schedProps.load(SchedulerUtils.class.getClassLoader().getResourceAsStream(PASSERELLE_QUARTZ_PROPERTIES));
		} catch (Exception e) {
			schedProps=null;
			logger.warn("Error reading scheduler cfg file "+PASSERELLE_QUARTZ_PROPERTIES,e);
		}

	}

	/**
	 * Obtain a scheduler instance with the given name.
	 * When a scheduler with this name exists already, this one is returned.
	 * When the name is new, a new scheduler instance is created and returned.
	 * 
	 * @param name
	 * @return
	 */
	public static synchronized Scheduler getQuartzScheduler(String name) throws SchedulerException {
		Scheduler res = null;
		if(schedProps!=null) {
			schedProps.setProperty(ORG_QUARTZ_SCHEDULER_INSTANCENAME,name);
		} else {
			throw new SchedulerException("Initialization of scheduler properties failed");
		}
		StdSchedulerFactory schedFact = new StdSchedulerFactory(schedProps);
		res = schedFact.getScheduler();
		
		return res;
	}
}
