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
package com.isencia.passerelle.core;

/**
 * PasserelleException
 * 
 * Base class for all exceptions in Passerelle.
 * 
 * @author erwin
 */
public class PasserelleException extends Exception {
	public static class Severity {
		public final static Severity NON_FATAL = new Severity("NON_FATAL");
		public final static Severity FATAL = new Severity("FATAL");
		
		private String description;
		
		private Severity(String description) {
			this.description = description;
		}
		
		public String toString() {
			return description;
		}
	}

	private Severity severity=null;	
	private Object context=null;
	private Throwable rootException=null;

	public PasserelleException(String message, Object context, Throwable rootException) {
		this(Severity.NON_FATAL, message,context,rootException);
	}
	/**
	 * @param severity
	 * @param message
	 * @param context
	 * @param rootException
	 */
	public PasserelleException(Severity severity, String message, Object context, Throwable rootException) {
		super(message,rootException);
		this.severity=severity;
		this.rootException = rootException;
		this.context = context;
	}
	/**
	 * @return the context object that was specified for this exception (can be null)
	 */
	public Object getContext() {
		return context;
	}

	/**
	 * @return the root exception that caused this exception (can be null)
	 */
	public Throwable getRootException() {
		return rootException;
	}


	/**
	 * @return the severity of the exception
	 */
	public Severity getSeverity() {
		return severity;
	}

	/* (non-Javadoc)
	 * @see java.lang.Throwable#getMessage()
	 */
	public String getMessage() {
		return getSeverity()+" - "+super.getMessage()+"\n - Context:"+getContext()+"\n - RootException:"+getRootException();
	}

}
