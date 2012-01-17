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
package com.isencia.passerelle.actor.dynaport;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.expr.StringParameter;
import ptolemy.kernel.CompositeEntity;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import ptolemy.kernel.util.NameDuplicationException;
import com.isencia.passerelle.actor.v5.Actor;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;

/**
 * Remark : for these kinds of actors, it is not allowed to modify the names of
 * the dynamically generated ports, outside of the actor parameter config panel.
 * Otherwise the lookup of the ports can fail...
 * 
 * @author Erwin De Ley
 */
public abstract class DynamicNamedOutputPortsActor extends Actor {

  // ~ Static variables/initializers ____________________________________
  public static final String OUTPUT_PORTNAMES = "Output port names (comma-separated)";

  private static Logger logger = LoggerFactory.getLogger(DynamicNamedOutputPortsActor.class);

  public StringParameter outputPortNamesParameter = null;
  public Set<String> outputPortNames = new HashSet<String>();

  /**
   * Construct an actor in the specified container with the specified name.
   * 
   * @param container The container.
   * @param name The name of this actor within the container.
   * @exception IllegalActionException If the actor cannot be contained by the
   *              proposed container.
   * @exception NameDuplicationException If the name coincides with an actor
   *              already in the container.
   */
  public DynamicNamedOutputPortsActor(CompositeEntity container, String name) throws IllegalActionException, NameDuplicationException {
    super(container, name);

    outputPortNamesParameter = new StringParameter(this, OUTPUT_PORTNAMES);
  }

  // ~ Methods __________________________________________________________

  /**
   * @param attribute The attribute that changed.
   * @exception IllegalActionException
   */
  public void attributeChanged(Attribute attribute) throws IllegalActionException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " attributeChanged() - entry - attribute :" + attribute);
    }

    // Change numberOfOutputs
    if (attribute == outputPortNamesParameter) {
      String outputPortNames = outputPortNamesParameter.getExpression();
      changeOutputPorts(outputPortNames);
    } else {
      super.attributeChanged(attribute);
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " attributeChanged() - exit");
    }
  }

  /**
   * @return Returns the outputPorts.
   */
  @SuppressWarnings("unchecked")
  public List<Port> getOutputPorts() {
    // in order to avoid cloning issues
    // when we would maintain the list of dynamically cfg-ed
    // output ports in an instance variable,
    // we build this list dynamically here from
    // Ptolemy's internal port list
    List<Port> ports = new ArrayList<Port>();
    for (String portName : outputPortNames) {
      Port p = (Port) super.getPort(portName);
      if (p != null)
        ports.add(p);
      else {
        logger.error(getInfo() + " - internal error - configured port not found with name " + portName);
      }
    }
    return ports;
  }

  /**
   * @param portNames comma-separated
   * @throws IllegalActionException
   * @throws IllegalArgumentException
   */
  protected void changeOutputPorts(String portNames) throws IllegalActionException, IllegalArgumentException {

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " changeOutputPorts() - entry - portNames : " + portNames);
    }

    Set<String> previousPortNames = outputPortNames;
    outputPortNames = new HashSet<String>();
    String[] newPortNames = portNames.split(",");

    // first add new ports
    for (String portName : newPortNames) {
      Port aPort = (Port) getPort(portName);
      if (aPort == null) {
        // create a new one
        createPort(portName);
      }
      previousPortNames.remove(portName);
      outputPortNames.add(portName);
    }
    // then remove removed ports, based on remaining names in the old port names
    // list
    for (String portName : previousPortNames) {
      try {
        this.getPort(portName).setContainer(null);
      } catch (Exception e) {
        throw new IllegalActionException(this, e, "failed to remove port " + portName);
      }
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " changeNumberOfPorts() - exit");
    }
  }

  /**
   * @param portName
   * @return
   * @throws IllegalActionException
   */
  protected Port createPort(String portName) throws IllegalActionException {

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " createPort() - entry - name : " + portName);
    }
    Port aPort = null;
    try {
      aPort = (Port) getPort(portName);

      if (aPort == null) {
        logger.debug(getInfo() + " createPort() - port " + portName + " will be constructed");
        aPort = PortFactory.getInstance().createOutputPort(this, portName);
        aPort.setMultiport(true);
      } else {
        throw new IllegalActionException(this, "port " + portName + " already exists");
      }
    } catch (Exception e) {
      throw new IllegalActionException(this, e, "failed to create port " + portName);
    }
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " createPort() - exit - port : " + aPort);
    }
    return aPort;
  }

}