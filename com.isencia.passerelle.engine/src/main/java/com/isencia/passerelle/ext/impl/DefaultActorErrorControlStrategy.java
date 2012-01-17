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

package com.isencia.passerelle.ext.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.isencia.passerelle.actor.Actor;
import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.actor.ValidationException;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.ext.ErrorControlStrategy;

import ptolemy.kernel.util.IllegalActionException;

/**
 * Default implementation of an error control strategy for an actor:
 * <ul>
 * <li> Check if the exception is FATAL or NON_FATAL
 * <li> FATALs and RuntimeExceptions are escalated as IllegalActionExceptions
 * <li> NON_FATALs are sent via the error port, if it is connected
 * <li> if the error port is not connected, the error is reported to the director
 * </ul>
 * @author erwin
 *
 */
public class DefaultActorErrorControlStrategy implements ErrorControlStrategy {

	public void handleInitializationException(Actor a, InitializationException e) throws IllegalActionException {
		getLoggerForActor(a).error(a.getInfo() + " initialize() - generated exception during doInitialize()", e);
		Actor.getAuditLogger().error(a.getInfo() + " INITIALIZATION FAILED");
		throw new IllegalActionException(a, e, "");
	}

	public void handleInitializationValidationException(Actor a, ValidationException e) throws IllegalActionException {
		getLoggerForActor(a).error(a.getInfo() + " initialize() - generated exception during validateInitialization()", e);
		Actor.getAuditLogger().error(a.getInfo() + " INITIALIZATION VALIDATION FAILED");
		throw new IllegalActionException(a, e, "");
	}

	public void handleIterationValidationException(Actor a, ValidationException e) throws IllegalActionException {
		getLoggerForActor(a).error(a.getInfo() + " generated exception during iterationValidation()", e);
		Actor.getAuditLogger().error(a.getInfo() + " ITERATION VALIDATION FAILED");
		throw new IllegalActionException(a, e, "");
	}

	public void handlePreFireException(Actor a, ProcessingException e) throws IllegalActionException {
		if (e.getSeverity() != PasserelleException.Severity.FATAL) {
			a.sendErrorMessage(e);
		} else {
			getLoggerForActor(a).error(a.getInfo() + " prefire() - generated exception", e);
			throw new IllegalActionException(a, e, "");
		}
	}

	protected static Logger getLoggerForActor(Actor a) {
		return LoggerFactory.getLogger(a.getClass());
	}

	public void handleFireException(Actor a, ProcessingException e) throws IllegalActionException {
		if (e.getSeverity() != PasserelleException.Severity.FATAL) {
			a.sendErrorMessage(e);
		} else {
			getLoggerForActor(a).error(a.getInfo() + " fire() - generated exception", e);
			throw new IllegalActionException(a, e, "");
		}
	}

	public void handlePostFireException(Actor a, ProcessingException e) throws IllegalActionException {
		if (e.getSeverity() != PasserelleException.Severity.FATAL) {
			a.sendErrorMessage(e);
		} else {
			getLoggerForActor(a).error(a.getInfo() + " postfire() -generated exception", e);
			throw new IllegalActionException(a, e, "");
		}
	}

	public void handleTerminationException(Actor a, TerminationException e) throws IllegalActionException {
		getLoggerForActor(a).error(a.getInfo() + " wrapup() - generated exception during doWrapUp()", e);
		throw new IllegalActionException(a, e, "");
	}

	public void handleFireRuntimeException(Actor a, RuntimeException e) throws IllegalActionException {
		getLoggerForActor(a).error(a.getInfo() + " fire() - generated exception during doFire()", e);
		throw new IllegalActionException(a, e, "");
	}

	public void handlePostFireRuntimeException(Actor a, RuntimeException e) throws IllegalActionException {
		getLoggerForActor(a).error(a.getInfo() + " postfire() -generated exception during doPostFire()", e);
		throw new IllegalActionException(a, e, "");
	}

	public void handlePreFireRuntimeException(Actor a, RuntimeException e) throws IllegalActionException {
		getLoggerForActor(a).error(a.getInfo() + " prefire() - generated exception during doPreFire()", e);
		throw new IllegalActionException(a, e, "");
	}

}
