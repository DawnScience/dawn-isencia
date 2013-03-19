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

package com.isencia.passerelle.validation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ptolemy.kernel.attributes.VersionAttribute;
import ptolemy.kernel.util.IllegalActionException;

import com.isencia.passerelle.actor.Actor;
import com.isencia.passerelle.actor.ValidationException;
import com.isencia.passerelle.core.ErrorCode;
import com.isencia.passerelle.model.Flow;
import com.isencia.passerelle.validation.version.ActorMajorVersionValidator;
import com.isencia.passerelle.validation.version.ActorMinorVersionValidator;
import com.isencia.passerelle.validation.version.ModelElementVersionValidationStrategy;
import com.isencia.passerelle.validation.version.VersionSpecification;

/**
 * This service provides a facade on a combination of different possible validation strategies on Passerelle models.
 * <p>
 * Currently, only a validation of actor versions is implemented. In the future more advanced validation logic can be added e.g. on correct actor
 * interconnectivity etc.
 * </p>
 * 
 * @author erwin
 */
public class ModelValidationService {

  private final static ModelValidationService instance = new ModelValidationService();

  private Set<ModelElementVersionValidationStrategy> versionValidationStrategies = new HashSet<ModelElementVersionValidationStrategy>();

  private ModelValidationService() {
    versionValidationStrategies.add(new ActorMajorVersionValidator());
    versionValidationStrategies.add(new ActorMinorVersionValidator());
  }

  public static ModelValidationService getInstance() {
    return instance;
  }

  public void validate(Flow model, ValidationContext context) {
    List deepEntityList = model.deepEntityList();
    for (Object e : deepEntityList) {
      if (e instanceof Actor) {
        Actor a = (Actor) e;
        try {
          VersionAttribute versionAttr = (VersionAttribute) a.getAttribute("_version", VersionAttribute.class);
          if(versionAttr!=null) {
            VersionSpecification versionToBeValidated = VersionSpecification.parse(versionAttr.getValueAsString());
            for (ModelElementVersionValidationStrategy validationStrategy : versionValidationStrategies) {
              try {
                validationStrategy.validate(a, versionToBeValidated);
              } catch (ValidationException e1) {
                context.addError(e1);
              }
            }
          }
        } catch (IllegalActionException iae) {
          context.addError(new ValidationException(ErrorCode.FLOW_CONFIGURATION_ERROR, "Invalid _version attribute", a, iae));
        }
      }
    }
  }
}
