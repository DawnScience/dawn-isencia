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

package com.isencia.passerelle.actor.control;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.helpers.TriggerUtils;
import org.quartz.jobs.NoOpJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ptolemy.data.BooleanToken;
import ptolemy.data.expr.Parameter;
import ptolemy.data.expr.StringParameter;
import ptolemy.data.type.BaseType;
import ptolemy.kernel.util.Attribute;
import ptolemy.kernel.util.IllegalActionException;
import com.isencia.passerelle.actor.Actor;
import com.isencia.passerelle.actor.InitializationException;
import com.isencia.passerelle.actor.ProcessingException;
import com.isencia.passerelle.actor.TerminationException;
import com.isencia.passerelle.core.PasserelleException;
import com.isencia.passerelle.core.Port;
import com.isencia.passerelle.core.PortFactory;
import com.isencia.passerelle.domain.cap.Director;
import com.isencia.passerelle.message.ManagedMessage;
import com.isencia.passerelle.util.SchedulerUtils;
import com.isencia.passerelle.util.ptolemy.DateTimeParameter;
import com.isencia.util.BlockingReaderQueue;
import com.isencia.util.EmptyQueueException;
import com.isencia.util.FIFOQueue;

/**
 * A Passerelle scheduler actor, based on the well-known open-source Quartz
 * scheduling framework.
 * 
 * @author erwin
 */
public class QuartzScheduler extends Actor {
  // ~ Static variables/initializers
  // __________________________________________________________________________________________________________________________

  // /////////////////////////////////////////////////////////////////
  // // private variables ////
  private static final String END_OF_MONTH = "EndOfMonth";
  private static final String ILLEGAL_ENTRY = "--";

  private static Logger logger = LoggerFactory.getLogger(QuartzScheduler.class);

  private final static String[] WEEK_DAYS = { ILLEGAL_ENTRY, "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };
  private final static String[] MONTH_DAYS = { END_OF_MONTH, ILLEGAL_ENTRY, "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15",
      "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31" };
  private final static String[] HOURS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
      "21", "22", "23" };
  private final static String[] SECONDS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
      "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45",
      "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59" };
  private final static String[] MINUTES = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20",
      "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45",
      "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59" };

  static abstract class ExecutionType {
    static Map entries = new HashMap();

    private String description;
    private List valueRange;

    final static ExecutionType ONCE = new ExecutionType("Execute once", new String[] { "Not periodic" }) {
      public Trigger createTrigger(int periodSpec, String name) {
        Trigger t = new SimpleTrigger("SingleShotTrigger", name);
        return t;
      }
    };
    final static ExecutionType PERIODIC_SECONDS = new ExecutionType("Periodic - seconds", SECONDS) {
      public Trigger createTrigger(int periodSpec, String name) {
        Trigger t = TriggerUtils.makeSecondlyTrigger(periodSpec);
        t.setName("PeriodicalTriggerBySeconds");
        return t;
      }
    };
    final static ExecutionType PERIODIC_MINUTES = new ExecutionType("Periodic - minutes", MINUTES) {
      public Trigger createTrigger(int periodSpec, String name) {
        Trigger t = TriggerUtils.makeMinutelyTrigger(periodSpec);
        t.setName("PeriodicalTriggerByMinutes");
        return t;
      }
    };
    final static ExecutionType PERIODIC_HOURS = new ExecutionType("Periodic - hours", HOURS) {
      public Trigger createTrigger(int periodSpec, String name) {
        Trigger t = TriggerUtils.makeHourlyTrigger(periodSpec);
        t.setName("PeriodicalTriggerByHours");
        return t;
      }
    };
    final static ExecutionType PERIODIC_WEEKLY = new ExecutionType("Periodic - weekly", WEEK_DAYS) {
      public Trigger createTrigger(int periodSpec, String name) {
        Trigger t = TriggerUtils.makeWeeklyTrigger(periodSpec, 0, 0);
        t.setName("PeriodicalTriggerByWeekDay");
        return t;
      }
    };
    final static ExecutionType PERIODIC_MONTHLY = new ExecutionType("Periodic - monthly", MONTH_DAYS) {
      public Trigger createTrigger(int periodSpec, String name) {
        Trigger t = TriggerUtils.makeMonthlyTrigger(periodSpec, 0, 0);
        t.setName("PeriodicalTriggerByMonthDay");
        return t;
      }
    };

    private ExecutionType(String desc, String[] values) {
      this.description = desc;
      this.valueRange = Arrays.asList(values);
      entries.put(desc, this);
    }

    public String getDescription() {
      return description;
    }

    public String[] getValueRange() {
      return (String[]) valueRange.toArray(new String[0]);
    }

    public int getValueIdentifier(String value) {
      if (END_OF_MONTH.equalsIgnoreCase(value)) {
        return -1;
      } else if (!ILLEGAL_ENTRY.equals(value)) {
        return valueRange.indexOf(value);
      } else {
        return -1;
      }
    }

    void fillChoices(Parameter ptolemyParameter) {
      if (ptolemyParameter != null) {
        ptolemyParameter.removeAllChoices();
        for (Iterator iter = valueRange.iterator(); iter.hasNext();) {
          String element = (String) iter.next();
          ptolemyParameter.addChoice(element);
        }
      }
    }

    public static ExecutionType getByDescription(String desc) {
      return (ExecutionType) entries.get(desc);
    }

    public abstract Trigger createTrigger(int periodSpec, String name);
  }

  // ~ Instance variables
  // _____________________________________________________________________________________________________________________________________

  public Parameter paramPeriodSpec;
  public Parameter paramExecutionType;
  public DateTimeParameter paramStartDate;
  public DateTimeParameter paramEndDate;
  public Parameter paramRecoverTriggers;

  private Date startDate = null;
  private Date endDate = null;
  private int periodSpec = -1;
  private ExecutionType executionType = ExecutionType.ONCE;

  private boolean recoverTriggers = false;

  public Port output = null;

  private BlockingReaderQueue queue = null;

  private Scheduler scheduler = null;
  private Trigger myTrigger = null;
  // flag to track if this actor is using
  // its own private scheduler instance or a shared one
  private boolean itsMyScheduler = false;
	private JobDetail jobDetail;
	private JobListener jobListener;
	private SchedulerActorManager schedulerActorManager;

  // ~ Constructors
  // ___________________________________________________________________________________________________________________________________________

  /**
   * QuartzScheduler constructor comment.
   * 
   * @param container ptolemy.kernel.CompositeEntity
   * @param name java.lang.String
   * @exception ptolemy.kernel.util.IllegalActionException The exception
   *              description.
   * @exception ptolemy.kernel.util.NameDuplicationException The exception
   *              description.
   */
  public QuartzScheduler(ptolemy.kernel.CompositeEntity container, String name) throws ptolemy.kernel.util.IllegalActionException,
      ptolemy.kernel.util.NameDuplicationException {
    super(container, name);

    // Ports
    output = PortFactory.getInstance().createOutputPort(this, "output");

    // Parameters
    paramRecoverTriggers = new Parameter(this, "recoverTriggers", new BooleanToken(false));
    paramRecoverTriggers.setTypeEquals(BaseType.BOOLEAN);
    registerConfigurableParameter(paramRecoverTriggers);

    startDate = new Date();
    paramStartDate = new DateTimeParameter(this, "startDate");
    paramStartDate.setDateValue(startDate);
    registerConfigurableParameter(paramStartDate);
    endDate = new Date();
    paramEndDate = new DateTimeParameter(this, "endDate");
    paramEndDate.setDateValue(endDate);
    registerConfigurableParameter(paramEndDate);

    paramExecutionType = new StringParameter(this, "executionType");
    paramExecutionType.setExpression(ExecutionType.ONCE.getDescription());
    paramExecutionType.addChoice(ExecutionType.ONCE.getDescription());
    paramExecutionType.addChoice(ExecutionType.PERIODIC_SECONDS.getDescription());
    paramExecutionType.addChoice(ExecutionType.PERIODIC_MINUTES.getDescription());
    paramExecutionType.addChoice(ExecutionType.PERIODIC_HOURS.getDescription());
    paramExecutionType.addChoice(ExecutionType.PERIODIC_WEEKLY.getDescription());
    paramExecutionType.addChoice(ExecutionType.PERIODIC_MONTHLY.getDescription());
    registerConfigurableParameter(paramExecutionType);

    paramPeriodSpec = new StringParameter(this, "periodSpecifier");
    paramPeriodSpec.setExpression("");
    ExecutionType.ONCE.fillChoices(paramPeriodSpec);
    registerConfigurableParameter(paramPeriodSpec);

    queue = new BlockingReaderQueue(new FIFOQueue());

    _attachText("_iconDescription", "<svg>\n" + "<rect x=\"-20\" y=\"-20\" width=\"40\" " + "height=\"40\" style=\"fill:lightgrey;stroke:lightgrey\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"19\" y2=\"-19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"-19\" y1=\"-19\" x2=\"-19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:white\"/>\n"
        + "<line x1=\"20\" y1=\"-19\" x2=\"20\" y2=\"20\" " + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"-19\" y1=\"20\" x2=\"20\" y2=\"20\" "
        + "style=\"stroke-width:1.0;stroke:black\"/>\n" + "<line x1=\"19\" y1=\"-18\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n"
        + "<line x1=\"-18\" y1=\"19\" x2=\"19\" y2=\"19\" " + "style=\"stroke-width:1.0;stroke:grey\"/>\n" + "<circle cx=\"0\" cy=\"0\" r=\"16\""
        + "style=\"fill:white\"/>\n" + "<line x1=\"0\" y1=\"-14\" x2=\"0\" y2=\"-12\"/>\n" + "<line x1=\"0\" y1=\"12\" x2=\"0\" y2=\"14\"/>\n"
        + "<line x1=\"-14\" y1=\"0\" x2=\"-12\" y2=\"0\"/>\n" + "<line x1=\"12\" y1=\"0\" x2=\"14\" y2=\"0\"/>\n"
        + "<line x1=\"0\" y1=\"-7\" x2=\"0\" y2=\"0\"/>\n" + "<line x1=\"0\" y1=\"0\" x2=\"11.26\" y2=\"-6.5\"/>\n" + "</svg>\n");
  }

  /*
   * (non-Javadoc)
   * @see
   * ptolemy.kernel.util.NamedObj#attributeChanged(ptolemy.kernel.util.Attribute
   * )
   */
  public void attributeChanged(Attribute attribute) throws IllegalActionException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " Attribute :" + attribute);
    }
    if (attribute == paramRecoverTriggers) {
      BooleanToken recoverToken = (BooleanToken) paramRecoverTriggers.getToken();

      if (recoverToken != null) {
        recoverTriggers = recoverToken.booleanValue();
      }
    } else if (attribute == paramStartDate) {
      startDate = ((DateTimeParameter) paramStartDate).getDateValue();
    } else if (attribute == paramEndDate) {
      endDate = ((DateTimeParameter) paramEndDate).getDateValue();
    } else if (attribute == paramExecutionType) {
      ExecutionType prevExecType = executionType;
      executionType = ExecutionType.getByDescription(paramExecutionType.getExpression());
      if (!prevExecType.equals(executionType)) {
        String oldPeriodSpec = paramPeriodSpec.getExpression();
        executionType.fillChoices(paramPeriodSpec);
        // hack needed to ensure correct initialization during load from
        // file.....
        // and need to change the expression in order to trigger the limited
        // Ptolemy UI refresh support
        paramPeriodSpec.setExpression("Pick one...");
        paramPeriodSpec.setExpression(oldPeriodSpec);
      }
    } else if (attribute == paramPeriodSpec) {
      periodSpec = executionType.getValueIdentifier(paramPeriodSpec.getExpression());
    } else {
      super.attributeChanged(attribute);
    }
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit");
    }
  }

  protected void doFire() throws ProcessingException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo());
    }

    ManagedMessage message = null;
    try {
      message = (ManagedMessage) queue.get();
    } catch (EmptyQueueException e) {
      if (logger.isInfoEnabled()) {
        logger.info(getInfo() + " - No more triggers");
      }
      requestFinish();
    } catch (Exception e) {
      throw new ProcessingException(getInfo() + " - doFire() generated exception " + e, message, e);
    }

	    if(message!=null) {
    try {
      sendOutputMsg(output, message);
    } catch (IllegalArgumentException e) {
      throw new ProcessingException(getInfo() + " - doFire() generated exception " + e, message, e);
    }
	    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }
  }

  protected String getAuditTrailMessage(ManagedMessage message, Port port) {
    return "generated scheduled trigger.";
  }

  protected void doInitialize() throws InitializationException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - Parameters :" + attributeList(Parameter.class));
    }

    super.doInitialize();
    // HACK : need to set them here, as they might not have been changed in the
    // cfg pane
    // and then attributeChanged will not have been called for them...
    // This can be an indication that we need to reconsider our approach
    // for attributeChanged() and instance variables!?
    startDate = ((DateTimeParameter) paramStartDate).getDateValue();
    endDate = ((DateTimeParameter) paramEndDate).getDateValue();
    String someUniqueName = getManager().getName() + "_" + getName();
    if (scheduler == null) {
      if (getDirector() instanceof Director) {
        itsMyScheduler = false;
        Director passerelleDirector = (Director) getDirector();
        scheduler = passerelleDirector.getScheduler();
      } else {
        itsMyScheduler = true;
        // the actor is used in a non-Passerelle domain,
        // so it needs to take care about scheduler set-up itself
        try {
          // obtain a unique scheduler instance per scheduler actor
          scheduler = SchedulerUtils.getQuartzScheduler(someUniqueName);
          scheduler.start();
        } catch (SchedulerException e) {
          throw new InitializationException(PasserelleException.Severity.FATAL, getInfo() + " - Error starting the scheduler", this, e);
        }
      }
      try {
                schedulerActorManager = new SchedulerActorManager();
				scheduler.addSchedulerListener(schedulerActorManager);
      } catch (SchedulerException e) {
    			if(itsMyScheduler) {
        try {
          scheduler.shutdown();
        } catch (SchedulerException e1) {
          // ignore
        }
    			}
        throw new InitializationException(PasserelleException.Severity.FATAL, getInfo() + " - Error registering with the scheduler", this, e);
      }
    }
    try {
      myTrigger = createTrigger();
	        jobDetail = new JobDetail(someUniqueName+"_Job", getName(),NoOpJob.class);

	        jobListener = new SchedulerActorJob(someUniqueName + "_JobListener");
			scheduler.addJobListener(jobListener);
	        jobDetail.addJobListener(jobListener.getName());
            scheduler.scheduleJob(jobDetail, myTrigger);
    } catch (Exception e) {
      throw new InitializationException(PasserelleException.Severity.FATAL, getInfo() + " - Error configuring the scheduler", this, e);
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }
  }

  protected void doWrapUp() throws TerminationException {
    if (logger.isTraceEnabled()) {
      logger.trace(getInfo());
    }
    try {
      if (scheduler != null) {
        try {
					boolean removed = scheduler.removeJobListener(jobListener.getName());
					removed = scheduler.removeSchedulerListener(schedulerActorManager);
					removed = scheduler.interrupt(jobDetail.getName(), jobDetail.getGroup());
					removed = scheduler.deleteJob(jobDetail.getName(), jobDetail.getGroup());
          if (itsMyScheduler) {
            // i'm responsible for the scheduler cleanup
//						scheduler.shutdown(true);
//						for (String group : scheduler.getJobGroupNames()) {
//		                    for (String jobName : scheduler.getJobNames(group)) {
//		                        try {
//		                            scheduler.interrupt(jobName, group);
//		                            scheduler.deleteJob(jobName, group);
//		                        } catch (Exception e) {
//
//		                        }
//		                    }
//		                }
          }
          // to get initialize() working correctly if the actor is
          // used in consecutive model runs in Vergil
          scheduler = null;
        } catch (SchedulerException e) {
          throw new TerminationException(getInfo() + " - Error closing the scheduler", this, e);
        }
      }
    } finally {
      super.doWrapUp();
    }

    if (logger.isTraceEnabled()) {
      logger.trace(getInfo() + " - exit ");
    }
  }

  protected void doStopFire() {
    queue.trigger();
  }

	protected void doStop() {
		queue.trigger();
	}

  protected String getExtendedInfo() {
    return "";
  }

  /**
   * @return a new Quartz trigger, configured according to the cfg info
   * @throws Exception
   */
  private Trigger createTrigger() throws Exception {
    Trigger t = executionType.createTrigger(periodSpec, getManager().getName() + "_" + getName() + "_Trigger");
    if (!recoverTriggers) {
      // make sure that scheduler does not try to recover triggers from the
      // start date onwards
      // but only creates new triggers after (re-)launching a Passerelle
      // application with a QuartzScheduler.
      if (t instanceof CronTrigger) {
        t.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
      } else {
        t.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
      }
    } else {
      if (t instanceof CronTrigger) {
        t.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_SMART_POLICY);
      } else {
        t.setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_SMART_POLICY);
      }
    }

    // use actor name as scheduler group name
    t.setGroup(getName());
    t.setStartTime(startDate);
    t.setEndTime(endDate);

    return t;
  }

  // ~ Classes
  // ________________________________________________________________________________________________________________________________________________
  class SchedulerActorManager implements SchedulerListener {
    public void jobScheduled(Trigger trigger) {
    }

    public void jobUnscheduled(String triggerName, String triggerGroup) {
    }

    public void triggerFinalized(Trigger trigger) {
      if (trigger.equals(myTrigger)) {
        if (logger.isTraceEnabled()) logger.trace("triggerFinalized() - entry");
        requestFinish();
        if (logger.isTraceEnabled()) logger.trace("triggerFinalized() - exit");
      }
    }

    public void triggersPaused(String triggerName, String triggerGroup) {
    }

    public void triggersResumed(String triggerName, String triggerGroup) {
    }

    public void jobsPaused(String jobName, String jobGroup) {
    }

    public void jobsResumed(String jobName, String jobGroup) {
    }

    public void schedulerError(String msg, SchedulerException cause) {
    }

    public void schedulerShutdown() {
    }
  }

  class SchedulerActorJob implements JobListener {
    private String name = "SchedulerActorJob";

    public SchedulerActorJob(String name) {
      this.name = name;
    }

    /*
     * (non-Javadoc)
     * @see org.quartz.JobListener#getName()
     */
    public String getName() {
      return name;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.quartz.JobListener#jobToBeExecuted(org.quartz.JobExecutionContext)
     */
    public void jobToBeExecuted(JobExecutionContext context) {
    }

    /*
     * (non-Javadoc)
     * @see
     * org.quartz.JobListener#jobExecutionVetoed(org.quartz.JobExecutionContext)
     */
    public void jobExecutionVetoed(JobExecutionContext context) {
    }

    /*
     * (non-Javadoc)
     * @see
     * org.quartz.JobListener#jobWasExecuted(org.quartz.JobExecutionContext,
     * org.quartz.JobExecutionException)
     */
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {

      if (context.getTrigger().equals(myTrigger)) {
        if (logger.isTraceEnabled()) {
          logger.trace("jobWasExecuted() - entry");
        }
        QuartzScheduler.this.queue.put(createTriggerMessage());

        if (logger.isTraceEnabled()) {
          logger.trace("jobWasExecuted() - exit");
        }
      }
    }
  }
}