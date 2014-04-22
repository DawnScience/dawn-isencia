/* Copyright 2013 - iSencia Belgium NV

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
package com.isencia.passerelle.testsupport;

import java.util.ArrayList;
import java.util.Collection;
import junit.framework.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.actor.Actor;
import ptolemy.kernel.Port;
import ptolemy.kernel.util.Attribute;
import com.isencia.passerelle.model.Flow;
import com.isencia.passerelle.model.util.ModelUtils;

/**
 * A builder class to specify expected elements of a Flow definition :
 * <ul>
 * <li>directors, actors, parameters and other NamedObj instances : define expected presence by name or by instance</li>
 * <li>parameters : define expected value</li>
 * <li>relations : define expected presence between ports</li>
 * </ul>
 * 
 * @author erwin
 *
 */
public class FlowDefinitionAssertion {
  private static final Logger LOGGER = LoggerFactory.getLogger(FlowDefinitionAssertion.class);
  
  private static class Relation {
    String from;
    String to;
    public Relation(String from, String to) {
      this.from = from;
      this.to = to;
    }
  }
  
  private Collection<String> expectedActorNames = new ArrayList<String>();
  private Collection<String> expectedParameterNames = new ArrayList<String>();
  private Collection<Relation> expectedRelations = new ArrayList<Relation>();
  
  public FlowDefinitionAssertion clear() {
    expectedActorNames.clear();
    expectedParameterNames.clear();
    expectedRelations.clear();
    return this;
  }
  
  /**
   * Assert all configured expectations on the given flow. The assertions are done using JUnit's <code>Assert.assert...()</code> methods, so any discovered
   * deviation will result in a JUnit test failure.
   * <p>
   * If all expectations are ok, further tests can be chained through the returned reference to this <code>FlowDefinitionAssertion</code> instance.
   * </p>
   * 
   * @param flow the flow that has been executed and for which test result expectations must be asserted.
   * @return this FlowDefinitionAssertion instance to allow fluent method chaining
   */
  public FlowDefinitionAssertion assertFlow(Flow flow) {
    assertActorNames(flow, expectedActorNames);
    assertParameterNames(flow, expectedParameterNames);
    assertRelations(flow, expectedRelations);
    return this;
  }
  
  protected void assertActorNames(Flow flow, Collection<String> expectedActorNames) {
    for (String name : expectedActorNames) {
      Object actor = flow.getEntity(ModelUtils.getFullNameButWithoutModelName(flow, name));
      Assert.assertNotNull("No actor "+name+" found in flow "+flow.getFullName(), actor);
      Assert.assertTrue(name + " is not an Actor in flow "+flow.getFullName(), (actor instanceof Actor));
    }
  }

  protected void assertParameterNames(Flow flow, Collection<String> expectedParameterNames) {
    for (String name : expectedParameterNames) {
      Object parameter = flow.getAttribute(ModelUtils.getFullNameButWithoutModelName(flow, name));
      Assert.assertNotNull("No parameter "+name+" found in flow "+flow.getFullName(), parameter);
      Assert.assertTrue(name + " is not an Attribute in flow "+flow.getFullName(), (parameter instanceof Attribute));
    }
  }

  protected void assertRelations(Flow flow, Collection<Relation> expectedRelations) {
    for (Relation relation : expectedRelations) {
      Port outputPort = flow.getPort(ModelUtils.getFullNameButWithoutModelName(flow, relation.from));
      Port inputPort = flow.getPort(ModelUtils.getFullNameButWithoutModelName(flow, relation.to));
      Assert.assertNotNull("No port "+relation.from+" found in flow "+flow.getFullName(), outputPort);
      Assert.assertNotNull("No port "+relation.to+" found in flow "+flow.getFullName(), inputPort);
      Assert.assertTrue(relation.from + " not connected to " + relation.to + " in flow "+flow.getFullName(), outputPort.connectedPortList().contains(inputPort));
    }
  }

  /**
   * 
   * @param actorName the NamedObj.getFullName() of the actor
   * @return
   */
  public FlowDefinitionAssertion expectActor(String actorName) {
    expectedActorNames.add(actorName);
    return this;
  }
  
  /**
   * 
   * @param parameterName the NamedObj.getFullName() of the parameter
   * @return
   */
  public FlowDefinitionAssertion expectParameter(String parameterName) {
    expectedParameterNames.add(parameterName);
    return this;
  }
  
  public FlowDefinitionAssertion expectRelation(String from, String to) {
    expectedRelations.add(new Relation(from, to));
    return this;
  }
}
