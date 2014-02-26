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
package org.dawnsci.drmaa.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggf.drmaa.DrmaaException;
import org.ggf.drmaa.FileTransferMode;
import org.ggf.drmaa.InternalException;
import org.ggf.drmaa.InvalidAttributeValueException;
import org.ggf.drmaa.JobTemplate;
import org.ggf.drmaa.PartialTimestamp;
import org.ggf.drmaa.PartialTimestampFormat;
import org.ggf.drmaa.UnsupportedAttributeException;

/**
 * 
 * @author erwindl
 * 
 */
public class JobTemplateImpl implements JobTemplate {
  private static final String REMOTE_COMMAND = "drmaa_remote_command";
  private static final String INPUT_PARAMETERS = "drmaa_v_argv";
  private static final String JOB_SUBMISSION_STATE = "drmaa_js_state";
  private static final String JOB_ENVIRONMENT = "drmaa_v_env";
  private static final String WORKING_DIRECTORY = "drmaa_wd";
  private static final String JOB_CATEGORY = "drmaa_job_category";
  private static final String NATIVE_SPECIFICATION = "drmaa_native_specification";
  private static final String EMAIL_ADDRESS = "drmaa_v_email";
  private static final String BLOCK_EMAIL = "drmaa_block_email";
  private static final String START_TIME = "drmaa_start_time";
  private static final String JOB_NAME = "drmaa_job_name";
  private static final String INPUT_PATH = "drmaa_input_path";
  private static final String OUTPUT_PATH = "drmaa_output_path";
  private static final String ERROR_PATH = "drmaa_error_path";
  private static final String JOIN_FILES = "drmaa_join_files";
  private static final String TRANSFER_FILES = "drmaa_transfer_files";
  private static Set<String> supportedAttributeNames = new HashSet<>(Arrays.asList(
          REMOTE_COMMAND,
          INPUT_PARAMETERS,
          JOB_SUBMISSION_STATE,
          JOB_ENVIRONMENT,
          WORKING_DIRECTORY,
          JOB_CATEGORY,
          NATIVE_SPECIFICATION,
          EMAIL_ADDRESS,
          BLOCK_EMAIL,
          START_TIME,
          JOB_NAME,
          INPUT_PATH,
          OUTPUT_PATH,
          ERROR_PATH,
          JOIN_FILES,
          TRANSFER_FILES
          ));

  /* Not supported
  private static final String DEADLINE_TIME = "drmaa_deadline_time"
  private static final String HARD_WALLCLOCK_TIME_LIMIT = "drmaa_wct_hlimit"
  private static final String SOFT_WALLCLOCK_TIME_LIMIT = "drmaa_wct_slimit"
  private static final String HARD_RUN_DURATION_LIMIT = "drmaa_run_duration_hlimit"
  private static final String SOFT_RUN_DURATION_LIMIT = "drmaa_run_duration_slimit"
  */
  private static final String HOLD_STRING = "drmaa_hold";
  private static final String ACTIVE_STRING = "drmaa_active";
  private static PartialTimestampFormat ptf = new PartialTimestampFormat();
  
  private String id;
  private String remoteCommand;
  private List<String> args = new ArrayList<>();
  private String stateString;
  private Map<String, String> env = new HashMap<>();
  private String workingDirectory;
  private String jobCategory;
  private String nativeSpecification;
  private Set<String> emailAddresses = new HashSet<>();
  private boolean blockEmail;
  private String startTime;
  private String jobName;
  private String inputPath;
  private String outputPath;
  private String errorPath;
  private boolean joinFiles;
  private String transferFiles;

  /**
   * Creates a new instance of JobTemplateImpl
   * 
   * @param session the associated SessionImpl object
   * @param id the table index of the native job template
   */
  JobTemplateImpl(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
  
  public void setRemoteCommand(String remoteCommand) throws DrmaaException {
    this.remoteCommand = remoteCommand;
  }

  public String getRemoteCommand() throws DrmaaException {
    return remoteCommand;
  }

  public void setArgs(List<String> args) throws DrmaaException {
    this.args.clear();
    this.args.addAll(args);
  }

  public List<String> getArgs() throws DrmaaException {
    return args;
  }

  public void setJobSubmissionState(int state) throws DrmaaException {
    if (state == HOLD_STATE) {
      stateString = HOLD_STRING;
    } else if (state == ACTIVE_STATE) {
      stateString = ACTIVE_STRING;
    } else {
      throw new InvalidAttributeValueException("jobSubmissionState attribute is invalid");
    }
  }

  public int getJobSubmissionState() throws DrmaaException {
    if ((stateString == null) || stateString.equals(ACTIVE_STRING)) {
      return ACTIVE_STATE;
    } else if (stateString.equals(HOLD_STRING)) {
      return HOLD_STATE;
    } else {
      /* This should never happen */
      throw new InternalException("jobSubmissionState property is unparsable");
    }
  }

  public void setJobEnvironment(Map<String, String> env) throws DrmaaException {
    this.env.clear();
    this.env.putAll(env);
  }

  public Map<String, String> getJobEnvironment() throws DrmaaException {
    return env;
  }

  public void setWorkingDirectory(String wd) throws DrmaaException {
    this.workingDirectory = wd;
  }

  public String getWorkingDirectory() throws DrmaaException {
    return workingDirectory;
  }

  public void setJobCategory(String category) throws DrmaaException {
    this.jobCategory = category;
  }

  public String getJobCategory() throws DrmaaException {
    return jobCategory;
  }

  public void setNativeSpecification(String spec) throws DrmaaException {
    this.nativeSpecification = spec;
  }

  public String getNativeSpecification() throws DrmaaException {
    return nativeSpecification;
  }

  public void setEmail(Set<String> email) throws DrmaaException {
    this.emailAddresses.clear();
    this.emailAddresses.addAll(email);
  }

  public Set<String> getEmail() throws DrmaaException {
    return emailAddresses;
  }

  public void setBlockEmail(boolean blockEmail) throws DrmaaException {
    this.blockEmail = blockEmail;
  }

  public boolean getBlockEmail() throws DrmaaException {
    return this.blockEmail;
  }

  public void setStartTime(PartialTimestamp startTime) throws DrmaaException {
    this.startTime = ptf.format(startTime);
  }

  public PartialTimestamp getStartTime() throws DrmaaException {
    if (startTime != null) {
      try {
        return ptf.parse(this.startTime);
      } catch (java.text.ParseException e) {
        throw new InternalException("startTime property is unparsable");
      }
    } else {
      return null;
    }
  }

  public void setJobName(String name) throws DrmaaException {
    this.jobName = name;
  }

  public String getJobName() throws DrmaaException {
    return jobName;
  }

  public void setInputPath(String inputPath) throws DrmaaException {
    this.inputPath = inputPath;
  }

  public String getInputPath() throws DrmaaException {
    return inputPath;
  }

  public void setOutputPath(String outputPath) throws DrmaaException {
    this.outputPath = outputPath;
  }

  public String getOutputPath() throws DrmaaException {
    return outputPath;
  }

  public void setErrorPath(String errorPath) throws DrmaaException {
    this.errorPath = errorPath;
  }

  public String getErrorPath() throws DrmaaException {
    return errorPath;
  }

  public void setJoinFiles(boolean join) throws DrmaaException {
    this.joinFiles = join;
  }

  public boolean getJoinFiles() throws DrmaaException {
    return joinFiles;
  }

  public void setTransferFiles(FileTransferMode mode) throws DrmaaException {
    StringBuffer buf = new StringBuffer();

    if (mode.getInputStream()) {
      buf.append('i');
    }

    if (mode.getOutputStream()) {
      buf.append('o');
    }

    if (mode.getErrorStream()) {
      buf.append('e');
    }

    this.transferFiles = buf.toString();
  }

  public FileTransferMode getTransferFiles() throws DrmaaException {
    if (transferFiles != null) {
      return new FileTransferMode((transferFiles.indexOf('i') != -1), (transferFiles.indexOf('o') != -1), (transferFiles.indexOf('e') != -1));
    } else {
      return null;
    }
  }

  /**
   * Unsupported property. Will throw an UnsupportedAttributeException if called.
   * 
   * @throws UnsupportedAttributeException unsupported property
   */
  public void setDeadlineTime(PartialTimestamp deadline) throws UnsupportedAttributeException {
    throw new UnsupportedAttributeException("The deadlineTime attribute " + "is not supported.");
  }

  /**
   * Unsupported property. Will throw an UnsupportedAttributeException if called.
   * 
   * @throws UnsupportedAttributeException unsupported property
   */
  public PartialTimestamp getDeadlineTime() throws UnsupportedAttributeException {
    throw new UnsupportedAttributeException("The deadlineTime attribute " + "is not supported.");
  }

  /**
   * Unsupported property. Will throw an UnsupportedAttributeException if called.
   * 
   * @throws UnsupportedAttributeException unsupported property
   */
  public void setHardWallclockTimeLimit(long hardWallclockLimit) throws UnsupportedAttributeException {
    throw new UnsupportedAttributeException("The hardWallclockTimeLimit " + "attribute is not supported.");
  }

  /**
   * Unsupported property. Will throw an UnsupportedAttributeException if called.
   * 
   * @throws UnsupportedAttributeException unsupported property
   */
  public long getHardWallclockTimeLimit() throws UnsupportedAttributeException {
    throw new UnsupportedAttributeException("The hardWallclockTimeLimit " + "attribute is not supported.");
  }

  /**
   * Unsupported property. Will throw an UnsupportedAttributeException if called.
   * 
   * @throws UnsupportedAttributeException unsupported property
   */
  public void setSoftWallclockTimeLimit(long softWallclockLimit) throws UnsupportedAttributeException {
    throw new UnsupportedAttributeException("The softWallclockTimeLimit " + "attribute is not supported.");
  }

  /**
   * Unsupported property. Will throw an UnsupportedAttributeException if called.
   * 
   * @throws UnsupportedAttributeException unsupported property
   */
  public long getSoftWallclockTimeLimit() throws UnsupportedAttributeException {
    throw new UnsupportedAttributeException("The softWallclockTimeLimit " + "attribute is not supported.");
  }

  /**
   * Unsupported property. Will throw an UnsupportedAttributeException if called.
   * 
   * @throws UnsupportedAttributeException unsupported property
   */
  public void setHardRunDurationLimit(long hardRunLimit) throws UnsupportedAttributeException {
    throw new UnsupportedAttributeException("The hardRunDurationLimit " + "attribute is not supported.");
  }

  /**
   * Unsupported property. Will throw an UnsupportedAttributeException if called.
   * 
   * @throws UnsupportedAttributeException unsupported property
   */
  public long getHardRunDurationLimit() throws UnsupportedAttributeException {
    throw new UnsupportedAttributeException("The hardRunDurationLimit " + "attribute is not supported.");
  }

  /**
   * Unsupported property. Will throw an UnsupportedAttributeException if called.
   * 
   * @throws UnsupportedAttributeException unsupported property
   */
  public void setSoftRunDurationLimit(long softRunLimit) throws UnsupportedAttributeException {
    throw new UnsupportedAttributeException("The softRunDurationLimit " + "attribute is not supported.");
  }

  /**
   * Unsupported property. Will throw an UnsupportedAttributeException if called.
   * 
   * @throws UnsupportedAttributeException unsupported property
   */
  public long getSoftRunDurationLimit() throws UnsupportedAttributeException {
    throw new UnsupportedAttributeException("The softRunDurationLimit " + "attribute is not supported.");
  }

  /**
   * Returns the list of supported properties names. With the execd param, delegated_file_staging, set to false, this
   * list includes only the list of DRMAA required properties. With delegated_file_staging set to true, the list also
   * includes the transferFiles property.</p>
   * 
   * @return {@inheritDoc}
   */
  public Set<String> getAttributeNames() throws DrmaaException {
    return supportedAttributeNames;
  }

  /**
   * Tests whether this JobTemplateImpl represents the same native job template as the given object. This implementation
   * means that even if two JobTemplateImpl instance's have all the same settings, they are not equal, because they are
   * associated with different native job templates.
   * 
   * @param obj the object against which to compare
   * @return whether the the given object is the same as this object
   */
  public boolean equals(Object obj) {
    if (obj instanceof JobTemplateImpl) {
      return (this.getId() == ((JobTemplateImpl) obj).getId());
    } else {
      return false;
    }
  }

  /**
   * Returns a hash code based on the associated native job template's table index.
   * 
   * @return the hash code
   */
  public int hashCode() {
    return this.getId().hashCode();
  }
}
