/*
 * Copyright 2014 Diamond Light Source Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dawnsci.drmaa.executor.impl;

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extends the <code>Commandline</code> class to provide a means to manipulate
 * the OS environment under which the command will run.
 * 
 * @author <a href="mailto:rjmpsmith@hotmail.com">Robert J. Smith</a>
 */
public class EnvCommandline extends Commandline {

  private static final Logger LOG = LoggerFactory.getLogger(EnvCommandline.class);

  /**
   * Provides the OS environment under which the command will run.
   */
  private OSEnvironment env = new OSEnvironment();

  /**
   * Constructor which takes a command line string and attempts to parse it into
   * it's various components.
   * 
   * @param command The command
   */
  public EnvCommandline(String command) {
    super(command);
  }

  /**
   * Default constructor
   */
  public EnvCommandline() {
    super();
  }

  /**
   * Sets a variable within the environment under which the command will be run.
   * 
   * @param var The environment variable to set
   * @param value The value of the variable
   */
  public void setVariable(String var, String value) {
    env.add(var, value);
  }

  /**
   * Gets the value of an environment variable. The variable name is case
   * sensitive.
   * 
   * @param var The variable for which you wish the value
   * @return The value of the variable, or <code>null</code> if not found
   */
  public String getVariable(String var) {
    return env.getVariable(var);
  }

  /**
   * Executes the command.
   */
  public Process execute() throws IOException {
    Process process;

    // Let the user know what's happening
    File workingDir = getWorkingDir();
    if (workingDir == null) {
      LOG.debug("Executing \"" + this + "\"");
      process = Runtime.getRuntime().exec(getCommandline(), env.toArray());
    } else {
      LOG.debug("Executing \"" + this + "\" in directory " + workingDir.getAbsolutePath());
      process = Runtime.getRuntime().exec(getCommandline(), env.toArray(), workingDir);
    }

    return process;
  }
}
