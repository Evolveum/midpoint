/* 
 * Copyright 2001-2009 Terracotta, Inc. 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

package com.evolveum.midpoint.task.quartzimpl.jobstore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

import org.quartz.Calendar;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobPersistenceException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.Trigger.TriggerState;
import org.quartz.Trigger.TriggerTimeComparator;
import org.quartz.TriggerKey;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.ClassLoadHelper;
import org.quartz.spi.JobStore;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.SchedulerSignaler;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.spi.TriggerFiredResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskExclusivityStatus;
import com.evolveum.midpoint.task.api.TaskExecutionStatus;
import com.evolveum.midpoint.task.quartzimpl.JobExecutor;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExclusivityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

/**
 * 
 * This class implements a <code>{@link org.quartz.spi.JobStore}</code> that
 * utilizes midPoint repository as its storage device.
 * 
 * It is derived from original Quartz RAMJobStore.
 * 
 * @author Pavol Mederly
 * 
 * Original RAMJobStore authors:
 * 
 * @author James House
 * @author Sharada Jambula
 * @author Eric Mueller
 */

public class RepoJobStore implements JobStore {

	private RepositoryService repositoryService;
	
	private PrismContext prismContext;

	private static TaskManagerQuartzImpl taskManagerImpl;
	
	/*
	 * Ugly hack but don't know how to implement it better
	 * (this class is instantiated not by Spring but explicity
	 * by Quartz).
	 */
	public static void setTaskManagerQuartzImpl(TaskManagerQuartzImpl tmqi) {
		taskManagerImpl = tmqi;
	}
	
	private void initServices() {
		repositoryService = taskManagerImpl.getRepositoryService();
		prismContext = taskManagerImpl.getPrismContext();
	}

	private static final transient Trace LOGGER = TraceManager.getTrace(RepoJobStore.class);
	
//    private HashMap<JobKey, JobWrapper> jobsByKey = new HashMap<JobKey, JobWrapper>(1000);
//
//    private HashMap<TriggerKey, TriggerWrapper> triggersByKey = new HashMap<TriggerKey, TriggerWrapper>(1000);
//
//    private HashMap<String, HashMap<JobKey, JobWrapper>> jobsByGroup = new HashMap<String, HashMap<JobKey, JobWrapper>>(25);
//
//    private HashMap<String, HashMap<TriggerKey, TriggerWrapper>> triggersByGroup = new HashMap<String, HashMap<TriggerKey, TriggerWrapper>>(25);
//
//    private TreeSet<TriggerWrapper> timeTriggers = new TreeSet<TriggerWrapper>(new TriggerWrapperComparator());

    private HashMap<String, Calendar> calendarsByName = new HashMap<String, Calendar>(25);
//
//    private ArrayList<TriggerWrapper> triggers = new ArrayList<TriggerWrapper>(1000);

    private final Object lock = new Object();

//    private HashSet<String> pausedTriggerGroups = new HashSet<String>();
//
//    private HashSet<String> pausedJobGroups = new HashSet<String>();
//
//    private HashSet<JobKey> blockedJobs = new HashSet<JobKey>();
    
    private long misfireThreshold = 5000l;

    private SchedulerSignaler signaler;

    private final Logger log = LoggerFactory.getLogger(getClass());

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * <p>
     * Create a new <code>RepoJobStore</code>.
     * </p>
     */
    public RepoJobStore() {
    	LOGGER.trace("Instantiating RepoJobStore");
    	initServices();
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private Logger getLog() {
        return log;
    }

    /**
     * <p>
     * Called by the QuartzScheduler before the <code>JobStore</code> is
     * used, in order to give the it a chance to initialize.
     * </p>
     */
    public void initialize(ClassLoadHelper loadHelper, SchedulerSignaler schedSignaler) {

        this.signaler = schedSignaler;

        getLog().info("RepoJobStore initialized.");
    }

    public void schedulerStarted() {
        // nothing to do
    }

    public void schedulerPaused() {
    	// nothing to do
    }
    
    public void schedulerResumed() {
    	// nothing to do
    }
    
    public long getMisfireThreshold() {
        return misfireThreshold;
    }

    /**
     * The number of milliseconds by which a trigger must have missed its
     * next-fire-time, in order for it to be considered "misfired" and thus
     * have its misfire instruction applied.
     * 
     * @param misfireThreshold
     */
    public void setMisfireThreshold(long misfireThreshold) {
        if (misfireThreshold < 1) {
            throw new IllegalArgumentException("Misfirethreshold must be larger than 0");
        }
        this.misfireThreshold = misfireThreshold;
    }

    /**
     * <p>
     * Called by the QuartzScheduler to inform the <code>JobStore</code> that
     * it should free up all of it's resources because the scheduler is
     * shutting down.
     * </p>
     */
    public void shutdown() {
    }

    public boolean supportsPersistence() {
        return true;
    }

    /**
     * Clear (delete!) all scheduling data - all {@link Job}s, {@link Trigger}s
     * {@link Calendar}s.
     * 
     * @throws JobPersistenceException
     */
    public void clearAllSchedulingData() throws JobPersistenceException {

        synchronized (lock) {
            // unschedule jobs (delete triggers)
            List<String> lst = getTriggerGroupNames();
            for (String group: lst) {
                Set<TriggerKey> keys = getTriggerKeys(GroupMatcher.triggerGroupEquals(group));
                for (TriggerKey key: keys) {
                    removeTrigger(key);
                }
            }
            // delete jobs
            lst = getJobGroupNames();
            for (String group: lst) {
                Set<JobKey> keys = getJobKeys(GroupMatcher.jobGroupEquals(group));
                for (JobKey key: keys) {
                    removeJob(key);
                }
            }
            // delete calendars
            lst = getCalendarNames();
            for(String name: lst) {
                removeCalendar(name);
            }
        }
    }
    
    /**
     * <p>
     * Store the given <code>{@link org.quartz.JobDetail}</code> and <code>{@link org.quartz.Trigger}</code>.
     * </p>
     * 
     * @param newJob
     *          The <code>JobDetail</code> to be stored.
     * @param newTrigger
     *          The <code>Trigger</code> to be stored.
     * @throws ObjectAlreadyExistsException
     *           if a <code>Job</code> with the same name/group already
     *           exists.
     */
    public void storeJobAndTrigger(JobDetail newJob,
            OperableTrigger newTrigger) throws JobPersistenceException {
        storeJob(newJob, false);
        storeTrigger(newTrigger, false);
    }

    /**
     * <p>
     * Store the given <code>{@link org.quartz.Job}</code>.
     * </p>
     * 
     * @param newJob
     *          The <code>Job</code> to be stored.
     * @param replaceExisting
     *          If <code>true</code>, any <code>Job</code> existing in the
     *          <code>JobStore</code> with the same name & group should be
     *          over-written.
     * @throws ObjectAlreadyExistsException
     *           if a <code>Job</code> with the same name/group already
     *           exists, and replaceExisting is set to false.
     */
    public void storeJob(JobDetail newJob,
            boolean replaceExisting) throws ObjectAlreadyExistsException {
    	
    	LOGGER.error("storeJob is not yet supported");
   		throw new UnsupportedOperationException("storeJob is not yet supported");		// FIXME: implement
    	
//    	
//        JobWrapper jw = new JobWrapper((JobDetail)newJob.clone());
//
//        boolean repl = false;
//
//        synchronized (lock) {
//            if (jobsByKey.get(jw.key) != null) {
//                if (!replaceExisting) {
//                    throw new ObjectAlreadyExistsException(newJob);
//                }
//                repl = true;
//            }
//
//            if (!repl) {
//                // get job group
//                HashMap<JobKey, JobWrapper> grpMap = jobsByGroup.get(newJob.getKey().getGroup());
//                if (grpMap == null) {
//                    grpMap = new HashMap<JobKey, JobWrapper>(100);
//                    jobsByGroup.put(newJob.getKey().getGroup(), grpMap);
//                }
//                // add to jobs by group
//                grpMap.put(newJob.getKey(), jw);
//                // add to jobs by FQN map
//                jobsByKey.put(jw.key, jw);
//            } else {
//                // update job detail
//                JobWrapper orig = (JobWrapper) jobsByKey.get(jw.key);
//                orig.jobDetail = jw.jobDetail; // already cloned
//            }
//        }
    }

    /**
     * <p>
     * Remove (delete) the <code>{@link org.quartz.Job}</code> with the given
     * name, and any <code>{@link org.quartz.Trigger}</code> s that reference
     * it.
     * </p>
     *
     * @return <code>true</code> if a <code>Job</code> with the given name &
     *         group was found and removed from the store.
     */
    public boolean removeJob(JobKey jobKey) {
    	
    	LOGGER.error("removeJob is not yet supported");
   		throw new UnsupportedOperationException("removeJob is not yet supported");		// FIXME: implement
//
//
//        boolean found = false;
//
//        synchronized (lock) {
//            List<OperableTrigger> triggersOfJob = getTriggersForJob(jobKey);
//            for (OperableTrigger trig: triggersOfJob) {
//                this.removeTrigger(trig.getKey());
//                found = true;
//            }
//            
//            found = (jobsByKey.remove(jobKey) != null) | found;
//            if (found) {
//
//                HashMap<JobKey, JobWrapper> grpMap = jobsByGroup.get(jobKey.getGroup());
//                if (grpMap != null) {
//                    grpMap.remove(jobKey);
//                    if (grpMap.size() == 0) {
//                        jobsByGroup.remove(jobKey.getGroup());
//                    }
//                }
//            }
//        }
//
//        return found;
    }

    public boolean removeJobs(List<JobKey> jobKeys)
            throws JobPersistenceException {
        boolean allFound = true;

        synchronized (lock) {
            for(JobKey key: jobKeys)
                allFound = removeJob(key) && allFound;
        }

        return allFound;
    }

    public boolean removeTriggers(List<TriggerKey> triggerKeys)
            throws JobPersistenceException {
        boolean allFound = true;

        synchronized (lock) {
            for(TriggerKey key: triggerKeys)
                allFound = removeTrigger(key) && allFound;
        }

        return allFound;
    }

    public void storeJobsAndTriggers(
            Map<JobDetail, List<Trigger>> triggersAndJobs, boolean replace)
            throws ObjectAlreadyExistsException, JobPersistenceException {

        synchronized (lock) {
            // make sure there are no collisions...
            if(!replace) {
                for(Entry<JobDetail, List<Trigger>> e: triggersAndJobs.entrySet()) {
                    if(checkExists(e.getKey().getKey()))
                        throw new ObjectAlreadyExistsException(e.getKey());
                    for(Trigger trigger: e.getValue()) {
                        if(checkExists(trigger.getKey()))
                            throw new ObjectAlreadyExistsException(trigger);
                    }
                }
            }
            // do bulk add...
            for(Entry<JobDetail, List<Trigger>> e: triggersAndJobs.entrySet()) {
                storeJob(e.getKey(), true);
                for(Trigger trigger: e.getValue()) {
                    storeTrigger((OperableTrigger) trigger, true);
                }
            }
        }
        
    }

    /**
     * <p>
     * Store the given <code>{@link org.quartz.Trigger}</code>.
     * </p>
     *
     * @param newTrigger
     *          The <code>Trigger</code> to be stored.
     * @param replaceExisting
     *          If <code>true</code>, any <code>Trigger</code> existing in
     *          the <code>JobStore</code> with the same name & group should
     *          be over-written.
     * @throws ObjectAlreadyExistsException
     *           if a <code>Trigger</code> with the same name/group already
     *           exists, and replaceExisting is set to false.
     *
     * @see #pauseTriggerGroup(SchedulingContext, String)
     */
    public void storeTrigger(OperableTrigger newTrigger,
            boolean replaceExisting) throws JobPersistenceException {
    	
    	LOGGER.error("storeTrigger is not supported");
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
    	
//        TriggerWrapper tw = new TriggerWrapper((OperableTrigger)newTrigger.clone());
//
//        synchronized (lock) {
//            if (triggersByKey.get(tw.key) != null) {
//                if (!replaceExisting) {
//                    throw new ObjectAlreadyExistsException(newTrigger);
//                }
//    
//                removeTrigger(newTrigger.getKey(), false);
//            }
//    
//            if (retrieveJob(newTrigger.getJobKey()) == null) {
//                throw new JobPersistenceException("The job ("
//                        + newTrigger.getJobKey()
//                        + ") referenced by the trigger does not exist.");
//            }
//
//            // add to triggers array
//            triggers.add(tw);
//            // add to triggers by group
//            HashMap<TriggerKey, TriggerWrapper> grpMap = triggersByGroup.get(newTrigger.getKey().getGroup());
//            if (grpMap == null) {
//                grpMap = new HashMap<TriggerKey, TriggerWrapper>(100);
//                triggersByGroup.put(newTrigger.getKey().getGroup(), grpMap);
//            }
//            grpMap.put(newTrigger.getKey(), tw);
//            // add to triggers by FQN map
//            triggersByKey.put(tw.key, tw);
//
//            if (pausedTriggerGroups.contains(newTrigger.getKey().getGroup())
//            		|| pausedJobGroups.contains(newTrigger.getJobKey().getGroup())) {
//                tw.state = TriggerWrapper.STATE_PAUSED;
//                if (blockedJobs.contains(tw.jobKey)) {
//                    tw.state = TriggerWrapper.STATE_PAUSED_BLOCKED;
//                }
//            } else if (blockedJobs.contains(tw.jobKey)) {
//                tw.state = TriggerWrapper.STATE_BLOCKED;
//            } else {
//                timeTriggers.add(tw);
//            }
//        }
    }

    /**
     * <p>
     * Remove (delete) the <code>{@link org.quartz.Trigger}</code> with the
     * given name.
     * </p>
     *
     * @return <code>true</code> if a <code>Trigger</code> with the given
     *         name & group was found and removed from the store.
     */
    public boolean removeTrigger(TriggerKey triggerKey) {
        return removeTrigger(triggerKey, true);
    }
    
    private boolean removeTrigger(TriggerKey key, boolean removeOrphanedJob) {

    	LOGGER.error("removeTrigger is not yet supported");
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement

//        boolean found = false;
//
//        synchronized (lock) {
//            // remove from triggers by FQN map
//            found = (triggersByKey.remove(key) == null) ? false : true;
//            if (found) {
//                TriggerWrapper tw = null;
//                // remove from triggers by group
//                HashMap<TriggerKey, TriggerWrapper> grpMap = triggersByGroup.get(key.getGroup());
//                if (grpMap != null) {
//                    grpMap.remove(key);
//                    if (grpMap.size() == 0) {
//                        triggersByGroup.remove(key.getGroup());
//                    }
//                }
//                // remove from triggers array
//                Iterator<TriggerWrapper> tgs = triggers.iterator();
//                while (tgs.hasNext()) {
//                    tw = tgs.next();
//                    if (key.equals(tw.key)) {
//                        tgs.remove();
//                        break;
//                    }
//                }
//                timeTriggers.remove(tw);
//
//                if (removeOrphanedJob) {
//                    JobWrapper jw = (JobWrapper) jobsByKey.get(tw.jobKey);
//                    List<OperableTrigger> trigs = getTriggersForJob(tw.jobKey);
//                    if ((trigs == null || trigs.size() == 0) && !jw.jobDetail.isDurable()) {
//                        if (removeJob(jw.key)) {
//                            signaler.notifySchedulerListenersJobDeleted(jw.key);
//                        }
//                    }
//                }
//            }
//        }
//
//        return found;
    }


    /**
     * @see org.quartz.spi.JobStore#replaceTrigger(org.quartz.core.SchedulingContext, java.lang.String, java.lang.String, org.quartz.Trigger)
     */
    public boolean replaceTrigger(TriggerKey triggerKey, OperableTrigger newTrigger) throws JobPersistenceException {

    	LOGGER.error("replaceTrigger is not yet supported");
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement

//        boolean found = false;
//
//        synchronized (lock) {
//            // remove from triggers by FQN map
//            TriggerWrapper tw = (TriggerWrapper) triggersByKey.remove(triggerKey);
//            found = ( tw == null) ? false : true;
//
//            if (found) {
//
//                if (!tw.getTrigger().getJobKey().equals(newTrigger.getJobKey())) {
//                    throw new JobPersistenceException("New trigger is not related to the same job as the old trigger.");
//                }
//
//                tw = null;
//                // remove from triggers by group
//                HashMap<TriggerKey, TriggerWrapper> grpMap = triggersByGroup.get(triggerKey.getGroup());
//                if (grpMap != null) {
//                    grpMap.remove(triggerKey);
//                    if (grpMap.size() == 0) {
//                        triggersByGroup.remove(triggerKey.getGroup());
//                    }
//                }
//                // remove from triggers array
//                Iterator<TriggerWrapper> tgs = triggers.iterator();
//                while (tgs.hasNext()) {
//                    tw = tgs.next();
//                    if (triggerKey.equals(tw.key)) {
//                        tgs.remove();
//                        break;
//                    }
//                }
//                timeTriggers.remove(tw);
//
//                try {
//                    storeTrigger(newTrigger, false);
//                } catch(JobPersistenceException jpe) {
//                    storeTrigger(tw.getTrigger(), false); // put previous trigger back...
//                    throw jpe;
//                }
//            }
//        }
//
//        return found;
    }

    /**
     * <p>
     * Retrieve the <code>{@link org.quartz.JobDetail}</code> for the given
     * <code>{@link org.quartz.Job}</code>.
     * </p>
     *
     * @return The desired <code>Job</code>, or null if there is no match.
     */

    public JobDetail retrieveJob(JobKey jobKey) throws JobPersistenceException {
    	
   		OperationResult result = createOperationResult("retrieveJob"); 

       	String oid = jobKey.getName();
        Task task;
		try {
			task = taskManagerImpl.getTask(oid, result);
		} catch (Exception e) {	// TODO: implement correctly after clarification
			LoggingUtils.logException(LOGGER, "Cannot get task with OID {}", e, oid);
			throw new JobPersistenceException("Cannot get task", e);
		}
		
		return RepoJobStoreUtil.createJobForTask(task);
    }

    /**
     * <p>
     * Retrieve the given <code>{@link org.quartz.Trigger}</code>.
     * </p>
     *
     * @return The desired <code>Trigger</code>, or null if there is no
     *         match.
     */
    public OperableTrigger retrieveTrigger(TriggerKey triggerKey) {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement

//        synchronized(lock) {
//            TriggerWrapper tw = (TriggerWrapper) triggersByKey.get(triggerKey);
//    
//            return (tw != null) ? (OperableTrigger)tw.getTrigger().clone() : null;
//        }
    }
    
    /**
     * Determine whether a {@link Job} with the given identifier already 
     * exists within the scheduler.
     * 
     * @param jobKey the identifier to check for
     * @return true if a Job exists with the given identifier
     * @throws SchedulerException 
     */
    public boolean checkExists(JobKey jobKey)  {
    	
//    	if (true)
    		throw new UnsupportedOperationException("checkExists is not yet supported");		// FIXME: implement

//        synchronized(lock) {
//            JobWrapper jw = (JobWrapper) jobsByKey.get(jobKey);
//            return (jw != null);
//        }
    }
    
    /**
     * Determine whether a {@link Trigger} with the given identifier already 
     * exists within the scheduler.
     * 
     * @param triggerKey the identifier to check for
     * @return true if a Trigger exists with the given identifier
     * @throws SchedulerException 
     */
    public boolean checkExists(TriggerKey triggerKey) {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement

//        synchronized(lock) {
//            TriggerWrapper tw = (TriggerWrapper) triggersByKey.get(triggerKey);
//    
//            return (tw != null);
//        }
    }
 
    /**
     * <p>
     * Get the current state of the identified <code>{@link Trigger}</code>.
     * </p>
     *
     * @see Trigger#NORMAL
     * @see Trigger#PAUSED
     * @see Trigger#COMPLETE
     * @see Trigger#ERROR
     * @see Trigger#BLOCKED
     * @see Trigger#NONE
     */
    public TriggerState getTriggerState(TriggerKey triggerKey) throws JobPersistenceException {
    	
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement

//        synchronized(lock) {
//            TriggerWrapper tw = (TriggerWrapper) triggersByKey.get(triggerKey);
//            
//            if (tw == null) {
//                return TriggerState.NONE;
//            }
//    
//            if (tw.state == TriggerWrapper.STATE_COMPLETE) {
//                return TriggerState.COMPLETE;
//            }
//    
//            if (tw.state == TriggerWrapper.STATE_PAUSED) {
//                return TriggerState.PAUSED;
//            }
//    
//            if (tw.state == TriggerWrapper.STATE_PAUSED_BLOCKED) {
//                return TriggerState.PAUSED;
//            }
//    
//            if (tw.state == TriggerWrapper.STATE_BLOCKED) {
//                return TriggerState.BLOCKED;
//            }
//    
//            if (tw.state == TriggerWrapper.STATE_ERROR) {
//                return TriggerState.ERROR;
//            }
//    
//            return TriggerState.NORMAL;
//        }
    }

    /**
     * <p>
     * Store the given <code>{@link org.quartz.Calendar}</code>.
     * </p>
     *
     * @param calendar
     *          The <code>Calendar</code> to be stored.
     * @param replaceExisting
     *          If <code>true</code>, any <code>Calendar</code> existing
     *          in the <code>JobStore</code> with the same name & group
     *          should be over-written.
     * @param updateTriggers
     *          If <code>true</code>, any <code>Trigger</code>s existing
     *          in the <code>JobStore</code> that reference an existing
     *          Calendar with the same name with have their next fire time
     *          re-computed with the new <code>Calendar</code>.
     * @throws ObjectAlreadyExistsException
     *           if a <code>Calendar</code> with the same name already
     *           exists, and replaceExisting is set to false.
     */
    public void storeCalendar(String name,
            Calendar calendar, boolean replaceExisting, boolean updateTriggers)
        throws ObjectAlreadyExistsException {

   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement

//        calendar = (Calendar) calendar.clone();
//        
//        synchronized (lock) {
//    
//            Object obj = calendarsByName.get(name);
//    
//            if (obj != null && replaceExisting == false) {
//                throw new ObjectAlreadyExistsException(
//                    "Calendar with name '" + name + "' already exists.");
//            } else if (obj != null) {
//                calendarsByName.remove(name);
//            }
//    
//            calendarsByName.put(name, calendar);
//    
//            if(obj != null && updateTriggers) {
//                Iterator<TriggerWrapper> trigs = getTriggerWrappersForCalendar(name).iterator();
//                while (trigs.hasNext()) {
//                    TriggerWrapper tw = trigs.next();
//                    OperableTrigger trig = tw.getTrigger();
//                    boolean removed = timeTriggers.remove(tw);
//
//                    trig.updateWithNewCalendar(calendar, getMisfireThreshold());
//
//                    if(removed) {
//                        timeTriggers.add(tw);
//                    }
//                }
//            }
//        }
    }

    /**
     * <p>
     * Remove (delete) the <code>{@link org.quartz.Calendar}</code> with the
     * given name.
     * </p>
     *
     * <p>
     * If removal of the <code>Calendar</code> would result in
     * <code>Trigger</code>s pointing to non-existent calendars, then a
     * <code>JobPersistenceException</code> will be thrown.</p>
     *       *
     * @param calName The name of the <code>Calendar</code> to be removed.
     * @return <code>true</code> if a <code>Calendar</code> with the given name
     * was found and removed from the store.
     */
    public boolean removeCalendar(String calName)
        throws JobPersistenceException {

   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement

//    	int numRefs = 0;
//
//        synchronized (lock) {
//            Iterator<TriggerWrapper> itr = triggers.iterator();
//            while (itr.hasNext()) {
//                OperableTrigger trigg = itr.next().trigger;
//                if (trigg.getCalendarName() != null
//                        && trigg.getCalendarName().equals(calName)) {
//                    numRefs++;
//                }
//            }
//        }
//
//        if (numRefs > 0) {
//            throw new JobPersistenceException(
//                    "Calender cannot be removed if it referenced by a Trigger!");
//        }
//
//        return (calendarsByName.remove(calName) != null);
    }

    /**
     * <p>
     * Retrieve the given <code>{@link org.quartz.Trigger}</code>.
     * </p>
     *
     * @param calName
     *          The name of the <code>Calendar</code> to be retrieved.
     * @return The desired <code>Calendar</code>, or null if there is no
     *         match.
     */
    public Calendar retrieveCalendar(String calName) {
        synchronized (lock) {
            Calendar cal = (Calendar) calendarsByName.get(calName);
            if(cal != null)
                return (Calendar) cal.clone();
            return null;
        }
    }

    /**
     * <p>
     * Get the number of <code>{@link org.quartz.JobDetail}</code> s that are
     * stored in the <code>JobsStore</code>.
     * </p>
     */
    public int getNumberOfJobs() {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//        synchronized (lock) {
//            return jobsByKey.size();
//        }
    }

    /**
     * <p>
     * Get the number of <code>{@link org.quartz.Trigger}</code> s that are
     * stored in the <code>JobsStore</code>.
     * </p>
     */
    public int getNumberOfTriggers() {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//        synchronized (lock) {
//            return triggers.size();
//        }
    }

    /**
     * <p>
     * Get the number of <code>{@link org.quartz.Calendar}</code> s that are
     * stored in the <code>JobsStore</code>.
     * </p>
     */
    public int getNumberOfCalendars() {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//        synchronized (lock) {
//            return calendarsByName.size();
//        }
    }

    /**
     * <p>
     * Get the names of all of the <code>{@link org.quartz.Job}</code> s that
     * match the given groupMatcher.
     * </p>
     */
    public Set<JobKey> getJobKeys(GroupMatcher<JobKey> matcher) {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//        Set<JobKey> outList = null;
//        synchronized (lock) {
//
//            StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator();
//            String compareToValue = matcher.getCompareToValue();
//
//            switch(operator) {
//                case EQUALS:
//                    HashMap<JobKey, JobWrapper> grpMap = jobsByGroup.get(compareToValue);
//                    if (grpMap != null) {
//                        outList = new HashSet<JobKey>();
//
//                        for (JobWrapper jw : grpMap.values()) {
//
//                            if (jw != null) {
//                                outList.add(jw.jobDetail.getKey());
//                            }
//                        }
//                    }
//                    break;
//
//                default:
//                    for (Map.Entry<String, HashMap<JobKey, JobWrapper>> entry : jobsByGroup.entrySet()) {
//                        if(operator.evaluate(entry.getKey(), compareToValue) && entry.getValue() != null) {
//                            if(outList == null) {
//                                outList = new HashSet<JobKey>();
//                            }
//                            for (JobWrapper jobWrapper : entry.getValue().values()) {
//                                if(jobWrapper != null) {
//                                    outList.add(jobWrapper.jobDetail.getKey());
//                                }
//                            }
//                        }
//                    }
//            }
//        }
//
//        return outList == null ? java.util.Collections.<JobKey>emptySet() : outList;
    }

    /**
     * <p>
     * Get the names of all of the <code>{@link org.quartz.Calendar}</code> s
     * in the <code>JobStore</code>.
     * </p>
     *
     * <p>
     * If there are no Calendars in the given group name, the result should be
     * a zero-length array (not <code>null</code>).
     * </p>
     */
    public List<String> getCalendarNames() {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//        synchronized(lock) {
//            return new LinkedList<String>(calendarsByName.keySet());
//        }
    }

    /**
     * <p>
     * Get the names of all of the <code>{@link org.quartz.Trigger}</code> s
     * that match the given groupMatcher.
     * </p>
     */
    public Set<TriggerKey> getTriggerKeys(GroupMatcher<TriggerKey> matcher) {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//        Set<TriggerKey> outList = null;
//        synchronized (lock) {
//
//            StringMatcher.StringOperatorName operator = matcher.getCompareWithOperator();
//            String compareToValue = matcher.getCompareToValue();
//
//            switch(operator) {
//                case EQUALS:
//                    HashMap<TriggerKey, TriggerWrapper> grpMap = triggersByGroup.get(compareToValue);
//                    if (grpMap != null) {
//                        outList = new HashSet<TriggerKey>();
//
//                        for (TriggerWrapper tw : grpMap.values()) {
//
//                            if (tw != null) {
//                                outList.add(tw.trigger.getKey());
//                            }
//                        }
//                    }
//                    break;
//
//                default:
//                    for (Map.Entry<String, HashMap<TriggerKey, TriggerWrapper>> entry : triggersByGroup.entrySet()) {
//                        if(operator.evaluate(entry.getKey(), compareToValue) && entry.getValue() != null) {
//                            if(outList == null) {
//                                outList = new HashSet<TriggerKey>();
//                            }
//                            for (TriggerWrapper triggerWrapper : entry.getValue().values()) {
//                                if(triggerWrapper != null) {
//                                    outList.add(triggerWrapper.trigger.getKey());
//                                }
//                            }
//                        }
//                    }
//            }
//        }
//
//        return outList == null ? Collections.<TriggerKey>emptySet() : outList;
    }

    /**
     * <p>
     * Get the names of all of the <code>{@link org.quartz.Job}</code>
     * groups.
     * </p>
     */
    public List<String> getJobGroupNames() {
        List<String> outList = new ArrayList<String>(1);
        outList.add(Scheduler.DEFAULT_GROUP);
        return outList;
    }

    /**
     * <p>
     * Get the names of all of the <code>{@link org.quartz.Trigger}</code>
     * groups.
     * </p>
     */
    public List<String> getTriggerGroupNames() {
        List<String> outList = new ArrayList<String>(1);
        outList.add(Scheduler.DEFAULT_GROUP);
        return outList;
    }

    /**
     * <p>
     * Get all of the Triggers that are associated to the given Job.
     * </p>
     *
     * <p>
     * If there are no matches, a zero-length array should be returned.
     * </p>
     */
    public List<OperableTrigger> getTriggersForJob(JobKey jobKey) throws JobPersistenceException {
    	return getTriggersForJobInternal(jobKey, createOperationResult("getTriggersForJob"));
    }
    
    private List<OperableTrigger> getTriggersForJobInternal(JobKey jobKey, OperationResult result) throws JobPersistenceException {    	
        Task task = getTask(jobKey, result);
        
        ArrayList<OperableTrigger> trigList = new ArrayList<OperableTrigger>();
        trigList.add(RepoJobStoreUtil.createTriggerForTask(task));
        return trigList;
    }

	protected ArrayList<TriggerWrapper> getTriggerWrappersForJob(JobKey jobKey) {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//        ArrayList<TriggerWrapper> trigList = new ArrayList<TriggerWrapper>();
//
//        synchronized (lock) {
//            for (int i = 0; i < triggers.size(); i++) {
//                TriggerWrapper tw = (TriggerWrapper) triggers.get(i);
//                if (tw.jobKey.equals(jobKey)) {
//                    trigList.add(tw);
//                }
//            }
//        }
//
//        return trigList;
    }

    protected ArrayList<TriggerWrapper> getTriggerWrappersForCalendar(String calName) {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//        ArrayList<TriggerWrapper> trigList = new ArrayList<TriggerWrapper>();
//
//        synchronized (lock) {
//            for (int i = 0; i < triggers.size(); i++) {
//                TriggerWrapper tw = (TriggerWrapper) triggers.get(i);
//                String tcalName = tw.getTrigger().getCalendarName();
//                if (tcalName != null && tcalName.equals(calName)) {
//                    trigList.add(tw);
//                }
//            }
//        }
//
//        return trigList;
    }

    /**
     * <p>
     * Pause the <code>{@link Trigger}</code> with the given name.
     * </p>
     *
     */
    public void pauseTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    	OperationResult result = createOperationResult("pauseTrigger");
    	Task task = getTask(triggerKey, result);			// TODO: eliminate this (by setting status directly through OID)
    	pauseTaskInternal(task, result);
    }

    private void pauseTaskInternal(Task task, OperationResult result) throws JobPersistenceException {
		if (task.getExecutionStatus() != TaskExecutionStatus.CLOSED) {
			try {
				task.setExecutionStatusImmediate(TaskExecutionStatus.SUSPENDED, result);
			} catch(Exception e) {
				LoggingUtils.logException(LOGGER, "Cannot suspend the task {}", e, task);
				throw new JobPersistenceException("Cannot suspend the task " + task, e);
			}
		}
    }


	/**
     * <p>
     * Pause all of the known <code>{@link Trigger}s</code> matching.
     * </p>
     *
     * <p>
     * The JobStore should "remember" the groups paused, and impose the
     * pause on any new triggers that are added to one of these groups while the group is
     * paused.
     * </p>
     *
     */
    public List<String> pauseTriggers(GroupMatcher<TriggerKey> matcher) {
   		throw new UnsupportedOperationException("Not supported");
    }

    /**
     * <p>
     * Pause the <code>{@link org.quartz.JobDetail}</code> with the given
     * name - by pausing all of its current <code>Trigger</code>s.
     * </p>
     *
     */
    public void pauseJob(JobKey jobKey) throws JobPersistenceException {
    	OperationResult result = createOperationResult("pauseJob");

    	List<OperableTrigger> triggersOfJob = getTriggersForJobInternal(jobKey, result);
    	for (OperableTrigger trigger: triggersOfJob) {
    		pauseTaskInternal(RepoJobStoreUtil.getTaskFromTrigger(trigger), result);
    	}
    }

    /**
     * <p>
     * Pause all of the <code>{@link org.quartz.JobDetail}s</code> in the
     * given group - by pausing all of their <code>Trigger</code>s.
     * </p>
     *
     *
     * <p>
     * The JobStore should "remember" that the group is paused, and impose the
     * pause on any new jobs that are added to the group while the group is
     * paused.
     * </p>
     */
    public List<String> pauseJobs(GroupMatcher<JobKey> matcher) {
   		throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Resume (un-pause) the <code>{@link Trigger}</code> with the given
     * key.
     * </p>
     *
     * <p>
     * If the <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     *
     */
    
    public void resumeTrigger(TriggerKey triggerKey) throws JobPersistenceException {
    	OperationResult result = createOperationResult("resumeTrigger");
    	Task task = getTask(triggerKey, result);			// TODO: eliminate this (by setting status directly through OID)
    	resumeTaskInternal(task, result);
    }

    private void resumeTaskInternal(Task task, OperationResult result) throws JobPersistenceException {
		if (task.getExecutionStatus() == TaskExecutionStatus.SUSPENDED) {
			try {
				task.setExecutionStatusImmediate(TaskExecutionStatus.RUNNING, result);
			} catch(Exception e) {
				LoggingUtils.logException(LOGGER, "Cannot resume the task {}", e, task);
				throw new JobPersistenceException("Cannot resume the task " + task, e);
			}
		} else {
			LOGGER.error("Cannot resume the task {}, because it was not suspended.", task);			// however, we will continue
		}
    }


    /**
     * <p>
     * Resume (un-pause) all of the <code>{@link Trigger}s</code> in the
     * given group.
     * </p>
     *
     * <p>
     * If any <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     *
     */
    public List<String> resumeTriggers(GroupMatcher<TriggerKey> matcher) {
   		throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Resume (un-pause) the <code>{@link org.quartz.JobDetail}</code> with
     * the given name.
     * </p>
     *
     * <p>
     * If any of the <code>Job</code>'s<code>Trigger</code> s missed one
     * or more fire-times, then the <code>Trigger</code>'s misfire
     * instruction will be applied.
     * </p>
     *
     */
    public void resumeJob(JobKey jobKey) throws JobPersistenceException {

    	OperationResult result = createOperationResult("resumeJob");

    	List<OperableTrigger> triggersOfJob = getTriggersForJobInternal(jobKey, result);
    	for (OperableTrigger trigger: triggersOfJob) {
    		resumeTaskInternal(RepoJobStoreUtil.getTaskFromTrigger(trigger), result);
    	}
    }

    /**
     * <p>
     * Resume (un-pause) all of the <code>{@link org.quartz.JobDetail}s</code>
     * in the given group.
     * </p>
     *
     * <p>
     * If any of the <code>Job</code> s had <code>Trigger</code> s that
     * missed one or more fire-times, then the <code>Trigger</code>'s
     * misfire instruction will be applied.
     * </p>
     *
     */
    public Collection<String> resumeJobs(GroupMatcher<JobKey> matcher) {
   		throw new UnsupportedOperationException();
    }

    /**
     * <p>
     * Pause all triggers - equivalent of calling <code>pauseTriggerGroup(group)</code>
     * on every group.
     * </p>
     *
     * <p>
     * When <code>resumeAll()</code> is called (to un-pause), trigger misfire
     * instructions WILL be applied.
     * </p>
     *
     * @see #resumeAll(SchedulingContext)
     * @see #pauseTriggerGroup(SchedulingContext, String)
     */
    public void pauseAll() {			// FIXME: implement this

        synchronized (lock) {
            List<String> names = getTriggerGroupNames();

            for (String name: names) {
                pauseTriggers(GroupMatcher.triggerGroupEquals(name));
            }
        }
    }

    /**
     * <p>
     * Resume (un-pause) all triggers - equivalent of calling <code>resumeTriggerGroup(group)</code>
     * on every group.
     * </p>
     *
     * <p>
     * If any <code>Trigger</code> missed one or more fire-times, then the
     * <code>Trigger</code>'s misfire instruction will be applied.
     * </p>
     *
     * @see #pauseAll(SchedulingContext)
     */
    public void resumeAll() {			// FIXME: implement this
//   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//
//        synchronized (lock) {
//            // TODO need a match all here!
//        	pausedJobGroups.clear();
//            List<String> names = getTriggerGroupNames();
//
//            for (String name: names) {
//                resumeTriggers(GroupMatcher.triggerGroupEquals(name));
//            }
//        }
    }

    protected boolean applyMisfire(TriggerWrapper tw, OperationResult result) throws JobPersistenceException {

        long misfireTime = System.currentTimeMillis();
        if (getMisfireThreshold() > 0) {
            misfireTime -= getMisfireThreshold();
        }

        Date tnft = tw.trigger.getNextFireTime();
        if (tnft == null || tnft.getTime() > misfireTime 
                || tw.trigger.getMisfireInstruction() == Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY) { 
            return false; 
        }

        Calendar cal = null;
        if (tw.trigger.getCalendarName() != null) {
            cal = retrieveCalendar(tw.trigger.getCalendarName());
        }

        signaler.notifyTriggerListenersMisfired((OperableTrigger)tw.trigger.clone());

        tw.trigger.updateAfterMisfire(cal);

        if (tw.trigger.getNextFireTime() == null) {
            RepoJobStoreUtil.markTriggerWrapperComplete(tw, result);
            signaler.notifySchedulerListenersFinalized(tw.trigger);
        } else if (tnft.equals(tw.trigger.getNextFireTime())) {
            return false;
        }

        return true;
    }

    private static final AtomicLong ftrCtr = new AtomicLong(System.currentTimeMillis());

    protected String getFiredTriggerRecordId() {
        return String.valueOf(ftrCtr.incrementAndGet());
    }

    /**
     * <p>
     * Get a handle to the next trigger to be fired, and mark it as 'reserved'
     * by the calling scheduler.
     * </p>
     *
     * @see #releaseAcquiredTrigger(SchedulingContext, Trigger)
     */
    public List<OperableTrigger> acquireNextTriggers(long noLaterThan, int maxCount, long timeWindow) throws JobPersistenceException {
    	
    	LOGGER.trace("acquireNextTriggers starting (noLaterThan = " + noLaterThan + " (" + new Date(noLaterThan) + "), maxCount = " + maxCount + ", timeWindow = " + timeWindow + ")");
    	try {
    		List<OperableTrigger> retval = acquireNextTriggersInternal(noLaterThan, maxCount, timeWindow);
    		LOGGER.trace("acquireNextTriggers ending, returning " + retval.size() + " item(s)");
    		return retval;
    	} catch(Throwable e) {
    		LoggingUtils.logException(LOGGER, "acquireNextTriggers failed", e);
    		throw new JobPersistenceException("AcquireNextTriggers got an exception", e);
    	}

    }
    
    private List<OperableTrigger> acquireNextTriggersInternal(long noLaterThan, int maxCount, long timeWindow) throws JobPersistenceException {
    	
    	OperationResult result = createOperationResult("acquireNextTriggers");

        synchronized (lock) {
        	
        	List<PrismObject<TaskType>> runnableTasks = getRunnableTasks();
        	
        	TreeSet<TriggerWrapper> timeTriggers = new TreeSet<TriggerWrapper>(new TriggerWrapperComparator());
        	for (PrismObject<TaskType> taskPrism : runnableTasks) {
        		Task task = RepoJobStoreUtil.createTaskFromTaskPrism(taskManagerImpl, taskPrism, result);
        		timeTriggers.add(TriggerWrapper.createFromTask(task));
        	}
        	LOGGER.trace("Found " + timeTriggers.size() + " eligible trigger(s), subject to further filtering.");
        	
            List<OperableTrigger> retval = new ArrayList<OperableTrigger>();
//            Set<JobKey> acquiredJobKeysForNoConcurrentExec = new HashSet<JobKey>();
//            Set<TriggerWrapper> excludedTriggers = new HashSet<TriggerWrapper>();
            long firstAcquiredTriggerFireTime = 0;
            
            // return empty list if store has no triggers.
            if (timeTriggers.size() == 0) {
            	return retval;
            }
            
            while (true) {
                TriggerWrapper tw;

                try {
                    tw = (TriggerWrapper) timeTriggers.first();
                    if (tw == null)
                    	break;
                    timeTriggers.remove(tw);
                } catch (java.util.NoSuchElementException nsee) {
                    break;
                }

                if (tw.trigger.getNextFireTime() == null) {
                	LOGGER.trace(tw.trigger.getDescription() + " nextFireTime is null, taking next one.");
                    continue;
                }
                
                // it's possible that we've selected triggers way outside of the max fire ahead time for batches 
                // (up to idleWaitTime + fireAheadTime) so we need to make sure not to include such triggers.  
                // So we select from the first next trigger to fire up until the max fire ahead time after that...
                // which will perfectly honor the fireAheadTime window because the no firing will occur until
                // the first acquired trigger's fire time arrives.
                if(firstAcquiredTriggerFireTime > 0 && 
                        tw.trigger.getNextFireTime().getTime() > (firstAcquiredTriggerFireTime + timeWindow)) {
                	LOGGER.trace(tw.trigger.getDescription() + " nextFireTime is not in timeWindow (" + timeWindow + "), exiting.");
                    break;
                }

                if (applyMisfire(tw, result)) {
                    if (tw.trigger.getNextFireTime() != null) {
                    	LOGGER.trace(tw.trigger.getDescription() + ": applied misfire, repeating.");
                        timeTriggers.add(tw);
                    } else {
                    	LOGGER.trace(tw.trigger.getDescription() + ": applied misfire, taking next one.");
                    }
                    continue;
                }

                if (tw.getTrigger().getNextFireTime().getTime() > noLaterThan + timeWindow) {
                	LOGGER.trace(tw.trigger.getDescription() + ": next fire time (" + tw.getTrigger().getNextFireTime() + ") greater than noLaterThan+timeWindow (" + new Date(noLaterThan+timeWindow) + "), exiting.");
                    break;
                }
            	LOGGER.trace(tw.trigger.getDescription() + ": next fire time (" + tw.getTrigger().getNextFireTime() + ") is less than or equals noLaterThan+timeWindow (" + new Date(noLaterThan+timeWindow) + "), continuing.");
            	
            	// If trigger's job is set as @DisallowConcurrentExecution, and it has already been added to result, then
                // put it back into the timeTriggers set and continue to search for next trigger.
//            	JobKey jobKey = tw.trigger.getJobKey();
//            	JobDetail job = jobsByKey.get(tw.trigger.getJobKey()).jobDetail;
//            	if (job.isConcurrentExectionDisallowed()) {
//            		if (acquiredJobKeysForNoConcurrentExec.contains(jobKey)) {
//	            		excludedTriggers.add(tw);
//	            		continue; // go to next trigger in store.
//            		} else {
//                        acquiredJobKeysForNoConcurrentExec.add(jobKey);
//            		}
//            	}

        		LOGGER.trace("Claiming (acquiring) task " + RepoJobStoreUtil.getTaskFromTrigger(tw.trigger));
                RepoJobStoreUtil.markTriggerWrapperAcquired(tw, result);
                
                tw.trigger.setFireInstanceId(getFiredTriggerRecordId());
                retval.add(tw.trigger);
                
                if(firstAcquiredTriggerFireTime == 0)
                    firstAcquiredTriggerFireTime = tw.trigger.getNextFireTime().getTime();

                if (retval.size() == maxCount)
                	break;
            }
            
            // If we did excluded triggers to prevent ACQUIRE state due to DisallowConcurrentExecution, we need to add them back to store.
//            if (excludedTriggers.size() > 0)
//            	timeTriggers.addAll(excludedTriggers);

            return retval;
        }
    }
    
    private OperationResult createOperationResult(String methodName) {
		return new OperationResult(RepoJobStore.class.getName() + "." + methodName);
	}

	private List<PrismObject<TaskType>> getRunnableTasks() throws JobPersistenceException {
		
    	OperationResult loopResult = new OperationResult(RepoJobStore.class.getName() + ".getRunnableTasks");
		PagingType paging = new PagingType();
		try {
			return repositoryService.searchObjects(TaskType.class, createQuery(), paging, loopResult);
		} catch (SchemaException e) {
			throw new JobPersistenceException("Task scanner cannot search for tasks", e);
		}

    }
    
	// Look for runnable tasks that are not claimed
	private QueryType createQuery() throws SchemaException {

		Document doc = DOMUtil.getDocument();

		Element executionStatusElement = doc.createElementNS(SchemaConstants.C_TASK_EXECUTION_STATUS.getNamespaceURI(),
				SchemaConstants.C_TASK_EXECUTION_STATUS.getLocalPart());
		executionStatusElement.setTextContent(TaskExecutionStatusType.RUNNING.value());
		Element exclusivityStatusElement = doc.createElementNS(SchemaConstants.C_TASK_EXECLUSIVITY_STATUS.getNamespaceURI(),
				SchemaConstants.C_TASK_EXECLUSIVITY_STATUS.getLocalPart());
		exclusivityStatusElement.setTextContent(TaskExclusivityStatusType.RELEASED.value());

		// We have all the data, we can construct the filter now
		Element filter = QueryUtil.createAndFilter(
				doc,
				QueryUtil.createEqualFilter(doc, null, executionStatusElement),
				QueryUtil.createEqualFilter(doc, null, exclusivityStatusElement));

		QueryType query = new QueryType();
		query.setFilter(filter);
		return query;
	}


    /**
     * <p>
     * Inform the <code>JobStore</code> that the scheduler no longer plans to
     * fire the given <code>Trigger</code>, that it had previously acquired
     * (reserved).
     * </p>
     */
    public void releaseAcquiredTrigger(OperableTrigger trigger) throws JobPersistenceException {

    	OperationResult result = createOperationResult("releaseAcquiredTrigger");
    	Task task = RepoJobStoreUtil.getTaskFromTrigger(trigger);
    	LOGGER.trace("releaseAcquiredTrigger for task " + task);
    	
    	try {
			task.setExclusivityStatusImmediate(TaskExclusivityStatus.RELEASED, result);
		} catch (Exception e) {			// FIXME
			LoggingUtils.logException(LOGGER, "Cannot release the task {}", e, task);
			throw new JobPersistenceException("Cannot release the task " + task, e);
		}
    	
    }

    /**
     * <p>
     * Inform the <code>JobStore</code> that the scheduler is now firing the
     * given <code>Trigger</code> (executing its associated <code>Job</code>),
     * that it had previously acquired (reserved).
     * </p>
     */
    public List<TriggerFiredResult> triggersFired(List<OperableTrigger> firedTriggers) throws JobPersistenceException {
    	
    	OperationResult result = createOperationResult("triggersFired");

        List<TriggerFiredResult> results = new ArrayList<TriggerFiredResult>();

        for (OperableTrigger trigger : firedTriggers) {
            	
        	TriggerWrapper tw = new TriggerWrapper(trigger);

            Calendar cal = null;
            if (tw.trigger.getCalendarName() != null) {
            	cal = retrieveCalendar(tw.trigger.getCalendarName());
            	if(cal == null)
            		continue;
            }

            Date prevFireTime = trigger.getPreviousFireTime();

            LOGGER.trace("Original previousFireTime/nextFireTime: " + trigger.getPreviousFireTime() + " / " + trigger.getNextFireTime());
            trigger.triggered(cal);								// compute next fire time
            LOGGER.trace("New previousFireTime/nextFireTime: " + trigger.getPreviousFireTime() + " / " + trigger.getNextFireTime());
            
            //tw.state = TriggerWrapper.STATE_WAITING;			// perhaps not so important ...
            
            TaskQuartzImpl task = (TaskQuartzImpl) RepoJobStoreUtil.getTaskFromTrigger(trigger);
            try {
				task.setNextRunStartTimeImmediate(trigger.getNextFireTime() != null ? trigger.getNextFireTime().getTime() : null, result);
			} catch (Exception e) {		// TODO: write correctly
				LoggingUtils.logException(LOGGER, "Cannot set next run start time for task {}", e, task);
				throw new JobPersistenceException("Cannot set next run start time for task " + task, e);
			}

            TriggerFiredBundle bndle = new TriggerFiredBundle(
            		retrieveJob(tw.jobKey),						// job 
            		trigger,									// trigger 
            		cal,										// calendar
            		false, 										// job is recovering
            		new Date(),									// fire time 
            		trigger.getPreviousFireTime(),				// scheduled fire time 
            		prevFireTime,								// previous fire time
            		trigger.getNextFireTime());					// next fire time
                
            results.add(new TriggerFiredResult(bndle));
        }
        return results;
    }

    /**
     * <p>
     * Inform the <code>JobStore</code> that the scheduler has completed the
     * firing of the given <code>Trigger</code> (and the execution its
     * associated <code>Job</code>), and that the <code>{@link org.quartz.JobDataMap}</code>
     * in the given <code>JobDetail</code> should be updated if the <code>Job</code>
     * is stateful.
     * </p>
     */
    public void triggeredJobComplete(OperableTrigger trigger,
            JobDetail jobDetail, CompletedExecutionInstruction triggerInstCode) throws JobPersistenceException {
    	
    	OperationResult result = createOperationResult("triggeredJobComplete");
    	
    	Task task = (Task) trigger.getJobDataMap().get(RepoJobStoreUtil.TASK_PROPERTY);
    	
    	LOGGER.trace("triggeredJobComplete called for task " + task + "; completed execution instruction = " + triggerInstCode.toString());
    	try {
			task.refresh(result);
		} catch (Exception e) {	// TODO: implement correctly after clarification
			LoggingUtils.logException(LOGGER, "Cannot refresh task {}", e, task);
			throw new JobPersistenceException("Cannot refresh task", e);
		}
    	
		task.setExclusivityStatus(TaskExclusivityStatus.RELEASED);
		if (task.isSingle() && task.getExecutionStatus() != TaskExecutionStatus.SUSPENDED)
			task.setExecutionStatus(TaskExecutionStatus.CLOSED);
		
		try {
			task.savePendingModifications(result);
		} catch (Exception e) { // TODO: implement correctly after clarification
			LoggingUtils.logException(LOGGER, "Cannot release or close task {}", e, task);
			throw new JobPersistenceException("Cannot release or close task", e);
		}
		signaler.signalSchedulingChange(0L);		// ??? :)
    	
//    	if (triggerInstCode == CompletedExecutionInstruction.DELETE_TRIGGER) {
//    		LOGGER.error("DELETE_TRIGGER instruction is not supported.");
//        } else if (triggerInstCode == CompletedExecutionInstruction.SET_TRIGGER_COMPLETE) {
//                    tw.state = TriggerWrapper.STATE_COMPLETE;
//                    timeTriggers.remove(tw);
//                    signaler.signalSchedulingChange(0L);
//                } else if(triggerInstCode == CompletedExecutionInstruction.SET_TRIGGER_ERROR) {
//                    getLog().info("Trigger " + trigger.getKey() + " set to ERROR state.");
//                    tw.state = TriggerWrapper.STATE_ERROR;
//                    signaler.signalSchedulingChange(0L);
//                } else if (triggerInstCode == CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_ERROR) {
//                    getLog().info("All triggers of Job " 
//                            + trigger.getJobKey() + " set to ERROR state.");
//                    setAllTriggersOfJobToState(trigger.getJobKey(), TriggerWrapper.STATE_ERROR);
//                    signaler.signalSchedulingChange(0L);
//                } else if (triggerInstCode == CompletedExecutionInstruction.SET_ALL_JOB_TRIGGERS_COMPLETE) {
//                    setAllTriggersOfJobToState(trigger.getJobKey(), TriggerWrapper.STATE_COMPLETE);
//                    signaler.signalSchedulingChange(0L);
//                }
//            }
//        }
    }

    protected void setAllTriggersOfJobToState(JobKey jobKey, int state) {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//        ArrayList<TriggerWrapper> tws = getTriggerWrappersForJob(jobKey);
//        Iterator<TriggerWrapper> itr = tws.iterator();
//        while (itr.hasNext()) {
//            TriggerWrapper tw = itr.next();
//            tw.state = state;
//            if(state != TriggerWrapper.STATE_WAITING) {
//                timeTriggers.remove(tw);
//            }
//        }
    }
    
    protected String peekTriggers() {

   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//        StringBuffer str = new StringBuffer();
//        TriggerWrapper tw = null;
//        synchronized (lock) {
//            for (Iterator<TriggerWrapper> valueIter = triggersByKey.values().iterator(); valueIter.hasNext();) {
//                tw = valueIter.next();
//                str.append(tw.trigger.getKey().getName());
//                str.append("/");
//            }
//        }
//        str.append(" | ");
//
//        synchronized (lock) {
//            Iterator<TriggerWrapper> itr = timeTriggers.iterator();
//            while (itr.hasNext()) {
//                tw = itr.next();
//                str.append(tw.trigger.getKey().getName());
//                str.append("->");
//            }
//        }
//
//        return str.toString();
    }

    /** 
     * @see org.quartz.spi.JobStore#getPausedTriggerGroups(org.quartz.core.SchedulingContext)
     */
    public Set<String> getPausedTriggerGroups() throws JobPersistenceException {
   		throw new UnsupportedOperationException("Not yet supported");		// FIXME: implement
//        HashSet<String> set = new HashSet<String>();
//        
//        set.addAll(pausedTriggerGroups);
//        
//        return set;
    }

    public void setInstanceId(String schedInstId) {
        //
    }

    public void setInstanceName(String schedName) {
        //
    }

	public void setThreadPoolSize(final int poolSize) {
		//
	}

	public long getEstimatedTimeToReleaseAndAcquireTrigger() {
        return 5;
    }

    public boolean isClustered() {
        return false;
    }
    
    
    /*
     * Helper methods
     */
    
    private Task getTask(TriggerKey triggerKey, OperationResult result) throws JobPersistenceException {
		return getTask(triggerKey.getName(), result);
	}
    
    private Task getTask(JobKey jobKey, OperationResult result) throws JobPersistenceException {
    	return getTask(jobKey.getName(), result);
    }

	private Task getTask(String oid, OperationResult result) throws JobPersistenceException {
		try {
			return taskManagerImpl.getTask(oid, result);
		} catch (Exception e) { // TODO: implement correctly after clarification
    		LoggingUtils.logException(LOGGER, "Cannot get the task {}", e, oid);
    		throw new JobPersistenceException("Cannot get the task " + oid, e);
		}
	}


}

/*******************************************************************************
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * 
 * Helper Classes. * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 */

class TriggerWrapperComparator implements Comparator<TriggerWrapper>, java.io.Serializable {

    TriggerTimeComparator ttc = new TriggerTimeComparator();
    
    public int compare(TriggerWrapper trig1, TriggerWrapper trig2) {
        return ttc.compare(trig1.trigger, trig2.trigger);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof TriggerWrapperComparator);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}

class JobWrapper {

    public JobKey key;

    public JobDetail jobDetail;

    JobWrapper(JobDetail jobDetail) {
        this.jobDetail = jobDetail;
        key = jobDetail.getKey();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JobWrapper) {
            JobWrapper jw = (JobWrapper) obj;
            if (jw.key.equals(this.key)) {
                return true;
            }
        }

        return false;
    }
    
    @Override
    public int hashCode() {
        return key.hashCode(); 
    }
}

class TriggerWrapper {

    public TriggerKey key;

    public JobKey jobKey;

    public OperableTrigger trigger;

    public int state = STATE_WAITING;

    public static final int STATE_WAITING = 0;

    public static final int STATE_ACQUIRED = 1;

    public static final int STATE_EXECUTING = 2;

    public static final int STATE_COMPLETE = 3;

    public static final int STATE_PAUSED = 4;

    public static final int STATE_BLOCKED = 5;

    public static final int STATE_PAUSED_BLOCKED = 6;

    public static final int STATE_ERROR = 7;
    
    TriggerWrapper(OperableTrigger trigger) {
        this.trigger = trigger;
        key = trigger.getKey();
        this.jobKey = trigger.getJobKey();
    }

    public static TriggerWrapper createFromTask(Task task) {
    	
    	OperableTrigger trigger = RepoJobStoreUtil.createTriggerForTask(task);
    	return new TriggerWrapper(trigger);
	}

	@Override
    public boolean equals(Object obj) {
        if (obj instanceof TriggerWrapper) {
            TriggerWrapper tw = (TriggerWrapper) obj;
            if (tw.key.equals(this.key)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode(); 
    }

    
    public OperableTrigger getTrigger() {
        return this.trigger;
    }
}
