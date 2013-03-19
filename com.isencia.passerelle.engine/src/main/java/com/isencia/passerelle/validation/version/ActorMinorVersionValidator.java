/* Copyright 2012 - iSencia Belgium NV

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

package com.isencia.passerelle.validation.version;

import java.util.SortedSet;
import ptolemy.kernel.util.NamedObj;
import com.isencia.passerelle.actor.ValidationException;
import com.isencia.passerelle.core.ErrorCode;

/**
 * This validator checks whether the minor version nr of an actor present in a model, is the same as the minor version number as registered for the actor
 * implementation present in the runtime.
 * <p>
 * If the minor version is different, a validation warning is thrown.
 * </p>
 * <p>
 * Alternative versioning can be based on a code/tag. This can be the case e.g. when models are stored/used in Passerelle's asset repository. 
 * In such a case, or if no version is registered in the runtime, we assume that no version constraint checking must be performed, so the validator will accept any
 * <code>versionToBeValidated</code> in such cases.
 * </p>
 * 
 * @author erwin
 */
public class ActorMinorVersionValidator implements ModelElementVersionValidationStrategy {

  public void validate(NamedObj versionedElement, VersionSpecification versionToBeValidated) throws ValidationException {
    if (versionedElement != null && versionToBeValidated != null) {
      String versionedElementClassName = versionedElement.getClass().getName();
      // first check with the most recent version
      VersionSpecification mostRecentVersion = ActorVersionRegistry.getInstance().getMostRecentVersion(versionedElementClassName);
      // if no registered version constraint, any entered version is valid. Else need to check it!
      if (mostRecentVersion != null) {
        int res = compareVersions(mostRecentVersion, versionToBeValidated);
        if (res<0) {
          throw new ValidationException(ErrorCode.FLOW_VALIDATION_WARNING, "Required version " + versionToBeValidated + " -- Available version " + mostRecentVersion + " too old.",
              versionedElement, null);
        } else if (res>0) {
          // This means the runtime has a more recent minor version than what's required for the element.
          // This may also lead to incompatibilities.
          // So need to check if any compatible version is available.
          // (remark : for the moment only one version is available though!)
          SortedSet<VersionSpecification> availableVersions = ActorVersionRegistry.getInstance().getAvailableVersions(versionedElementClassName);
          boolean foundCompatibleVersion = false;
          for (VersionSpecification availableVersionSpec : availableVersions) {
            if (compareVersions(availableVersionSpec, versionToBeValidated)==0) {
              foundCompatibleVersion = true;
              break;
            }
          }
          if (!foundCompatibleVersion) {
            throw new ValidationException(ErrorCode.FLOW_VALIDATION_WARNING, "Required version " + versionToBeValidated + " -- Available version " + mostRecentVersion + " more recent.",
                versionedElement, null);
          }
        }
      }
    }
  }

  private int compareVersions(VersionSpecification runtimeVersion, VersionSpecification modelElementVersion) throws ValidationException {
    if (runtimeVersion instanceof ThreeDigitVersionSpecification && modelElementVersion instanceof ThreeDigitVersionSpecification) {
      Integer runtimeVersionMinor = ((ThreeDigitVersionSpecification) runtimeVersion).getMinor();
      Integer modelElementVersionMinor = ((ThreeDigitVersionSpecification) modelElementVersion).getMinor();
      return runtimeVersionMinor.compareTo(modelElementVersionMinor);
    } else {
      return 0;
    }
  }
}
