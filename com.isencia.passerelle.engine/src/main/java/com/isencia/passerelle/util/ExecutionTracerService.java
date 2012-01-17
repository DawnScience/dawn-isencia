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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import com.isencia.passerelle.actor.Actor;
import com.isencia.passerelle.domain.cap.Director;
import com.isencia.passerelle.ext.ExecutionTracer;
import com.isencia.passerelle.ext.impl.DefaultExecutionTracer;


/**
 * A utility to easily be able to use the Passerelle ExecutionTracer features
 * from anywhere in a Passerelle application.
 * 
 * Remark that by default, no ExecutionTracers are registered. Each Passerelle
 * application should decide for itself what ExecutionTracer implementation
 * should be used/registered.
 * 
 * In many cases, the com.isencia.passerelle.ext.impl.DefaultExecutionTracer is a
 * good option.
 * 
 * @author erwin
 */
public class ExecutionTracerService {

	private static Set<ExecutionTracer> defaultTracers = new HashSet<ExecutionTracer>();
	private static ExecutionTracer defaultTracer = new DefaultExecutionTracer();

	public static boolean registerTracer(ExecutionTracer tracer) {
		return defaultTracers.add(tracer);
	}

	public static boolean removeTracer(ExecutionTracer tracer) {
		return defaultTracers.remove(tracer);
	}

	public static void trace(Director source, String message) {
		try {
			ExecutionTracer tracer2 = (ExecutionTracer) source.getAttribute("_userTracer");
			tracer2.trace(source, message);
		} catch (Throwable t) {
			// t.printStackTrace();
		}

		try {
			if (defaultTracers.isEmpty()) {
				ExecutionTracer tracer = (ExecutionTracer) source.getAttribute("_userTracerInIDE");
				if (tracer == null) {
					// no registered tracer found at all, but we don't want to
					// loose traces,
					// so use the default tracer, which logs the traces to a
					// separate trace category
					tracer = defaultTracer;
				}
				tracer.trace(source, message);
			} else {
				for (Iterator<ExecutionTracer> traceItr = defaultTracers.iterator(); traceItr.hasNext();) {
					ExecutionTracer tracer = (ExecutionTracer) traceItr.next();
					tracer.trace(source, message);
				}
			}
		} catch (Throwable t) {
			// t.printStackTrace();
		}
	}

	public static void trace(Actor source, String message) {
		try {
			ExecutionTracer tracer2 = (ExecutionTracer) source.getDirector().getAttribute("_userTracer");
			tracer2.trace(source, message);
		} catch (Throwable t) {
			// t.printStackTrace();
		}
		try {
			// if a trace is already registered, we are not in the IDE, so no
			// need to trace a second time
			if (defaultTracers.isEmpty()) {
				ExecutionTracer tracer = (ExecutionTracer) source.getDirector().getAttribute("_userTracerInIDE");
				if (tracer == null) {
					// no registered tracer found at all, but we don't want to
					// loose traces,
					// so use the default tracer, which logs the traces to a
					// separate trace category
					tracer = defaultTracer;
				}
				tracer.trace(source, message);
			} else {
				for (Iterator<ExecutionTracer> traceItr = defaultTracers.iterator(); traceItr.hasNext();) {
					ExecutionTracer tracer = (ExecutionTracer) traceItr.next();
					tracer.trace(source, message);
				}
			}
		} catch (Throwable t) {
			// t.printStackTrace();
		}
	}
}
