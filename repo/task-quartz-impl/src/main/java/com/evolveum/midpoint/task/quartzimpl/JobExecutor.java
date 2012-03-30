package com.evolveum.midpoint.task.quartzimpl;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.InterruptableJob;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;

import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@DisallowConcurrentExecution
public class JobExecutor implements InterruptableJob {

	private static TaskManagerQuartzImpl taskManagerImpl;
	
	/*
	 * Ugly hack - this class is instantiated not by Spring but explicity by Quartz.
	 */
	public static void setTaskManagerQuartzImpl(TaskManagerQuartzImpl tmqi) {
		taskManagerImpl = tmqi;
	}
	
	private static final transient Trace LOGGER = TraceManager.getTrace(JobExecutor.class);
	
	/*
	 * JobExecutor is instantiated at each execution of the task, so we can store
	 * the task here.
	 * 
	 * http://quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/tutorial-lesson-03
	 * "Each (and every) time the scheduler executes the job, it creates a new instance of 
	 * the class before calling its execute(..) method."
	 */
	private TaskQuartzImpl task;
	private Thread executingThread;				// used for interruptions
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		OperationResult executionResult = createOperationResult("execute");

		// get the task instance
		String oid = context.getJobDetail().getKey().getName();
        try {
			task = (TaskQuartzImpl) taskManagerImpl.getTask(oid, executionResult);
		} catch (Exception e) {
			LoggingUtils.logException(LOGGER, "Task with OID {} could not be retrieved, exiting the execution routine.", e, oid);
			return;
		}
		
        executingThread = Thread.currentThread();
        
		LOGGER.trace("execute called; task = " + task + ", thread = " + executingThread);
		logRunStart();
		
		try {
        
			TaskHandler handler = taskManagerImpl.getHandler(task.getHandlerUri());
		
			if (handler==null) {
				LOGGER.error("No handler for URI {}, task {}",task.getHandlerUri(),task);
				throw new JobExecutionException("No handler for URI "+task.getHandlerUri());
			}
		
			if (task.isCycle()) {
				if (((TaskQuartzImpl) task).getHandlersCount() > 1) {
					LOGGER.error("Recurrent tasks cannot have more than one task handler; task = {}", task);
					throw new JobExecutionException("Recurrent tasks cannot have more than one task handler; task = " + task);
				}
				executeRecurrentTask(handler, executionResult);
			} else if (task.isSingle()) {
				executeSingleTask(handler, executionResult);
			} else {
				LOGGER.error("Tasks must be either recurrent or single-run. This one is neither. Sorry.");
			}
		
		} finally {
			executingThread = null;
			logRunFinish();
		}

	}

	private void executeSingleTask(TaskHandler handler, OperationResult executionResult) throws JobExecutionException {

		try {
			
			RepositoryCache.enter();
			
			TaskRunResult runResult = null;

			recordCycleRunStart(executionResult);
			
			// here we execute the whole handler stack
			// FIXME do a better run result reporting! (currently only the last runResult gets reported)
			
			while (handler != null && task.canRun()) {
				runResult = executeHandler(handler);
				
				if (task.canRun())
					task.finishHandler(executionResult);			// closes the task, if there are no remaining handlers
				else
					break;											// in case of suspension/shutdown, we expect that the task will be restarted in the future, so we will not close it
				
				if (runResult.getOperationResult().isError())
					break;
				
				handler = taskManagerImpl.getHandler(task.getHandlerUri());
			}

			recordCycleRunFinish(runResult, executionResult);

		} catch (Throwable t) {
			LoggingUtils.logException(LOGGER, "An exception occurred during processing of task {}", t, task);
			throw new JobExecutionException("An exception occurred during processing of task " + task, t);
		} finally {
			RepositoryCache.exit();
		}
	}
	
	private void executeRecurrentTask(TaskHandler handler, OperationResult executionResult) throws JobExecutionException {

		try {

			while (task.canRun()) {
				
				LOGGER.trace("CycleRunner loop: start");

				RepositoryCache.enter();

				recordCycleRunStart(executionResult);
				TaskRunResult runResult = executeHandler(handler);
				boolean canContinue = recordCycleRunFinish(runResult, executionResult);
					
				RepositoryCache.exit();

				// in case of task disappeared
				if (!canContinue)
					break;
				
				// if the task is loosely-bound, exit the loop here
				if (task.isLooselyBound()) {
					LOGGER.trace("CycleRunner loop: task is loosely bound, exiting the execution cycle");
					break;
				}

				// in case the task thread was shut down (e.g. in case of task suspension) while processing...
				if (!task.canRun())			
					break;
				
				// Determine how long we need to sleep and hit the bed
				// TODO: consider the PERMANENT_ERROR state of the last run. in this case we should "suspend" the task

				Integer interval = task.getSchedule() != null ? task.getSchedule().getInterval() : null;
				if (interval == null) {
					LOGGER.error("Tightly bound task " + task + " has no scheduling interval specified.");
					break;
				}
				
				long sleepFor = task.getLastRunStartTimestamp() + (interval.intValue() * 1000) - System.currentTimeMillis();
				if (sleepFor < 0)
					sleepFor = 0;
				
				LOGGER.trace("CycleRunner loop: sleep ({})", sleepFor);
				try {
					Thread.sleep(sleepFor);
				} catch (InterruptedException e) {
					// Safe to ignore. Next loop iteration will check enabled status.
				}

				try {
					task.refresh(executionResult);
				} catch (ObjectNotFoundException ex) {
					LOGGER.error("Error refreshing task "+task+": Object not found: "+ex.getMessage(),ex);
					return;			// The task object in repo is gone. Therefore this task should not run any more. Therefore commit seppuku
				}
				LOGGER.trace("CycleRunner loop: end");
			}

		} catch (Throwable t) {
			// This is supposed to run in a thread, so this kind of heavy artillery is needed. If throwable won't be
			// caught here, nobody will catch it and it won't even get logged.
			if (task.canRun()) {
				LOGGER.error("CycleRunner got unexpected exception: {}: {}",new Object[] { t.getClass().getName(),t.getMessage(),t});
			} else {
				LOGGER.debug("CycleRunner got unexpected exception while shutting down: {}: {}",new Object[] { t.getClass().getName(),t.getMessage()});
				LOGGER.trace("CycleRunner got unexpected exception while shutting down: {}: {}",new Object[] { t.getClass().getName(),t.getMessage(),t});
			}
			throw new JobExecutionException("An exception occurred during processing of task " + task, t);
		}
	}

	
    private TaskRunResult executeHandler(TaskHandler handler) {

    	TaskRunResult runResult;
    	try {
    		runResult = handler.run(task);
    		if (runResult == null) {
				// Obviously an error in task handler
				LOGGER.error("Unable to record run finish: task returned null result");
				runResult = new TaskRunResult();
				OperationResult dummyResult = createOperationResult("error");
				dummyResult.recordFatalError("Unable to record run finish: task returned null result");
				runResult.setOperationResult(dummyResult);
				runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			}
    		return runResult;
    	} catch (Throwable t) {
			LOGGER.error("Task handler threw unexpected exception: {}: {}",new Object[] { t.getClass().getName(),t.getMessage(),t});
			runResult = new TaskRunResult();
			OperationResult dummyResult = createOperationResult("error");
			dummyResult.recordFatalError("Task handler threw unexpected exception: "+t.getMessage(),t);
			runResult.setOperationResult(dummyResult);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			return runResult;
    	}
	}

	private OperationResult createOperationResult(String methodName) {
		return new OperationResult(JobExecutor.class.getName() + "." + methodName);
	}
	
	private void logRunStart() {
		LOGGER.info("Task run STARTING "+task);
	}
	
	private void logRunFinish() {
		LOGGER.info("Task run FINISHED "+task);
	}
    
	private void recordCycleRunStart(OperationResult result) {
		LOGGER.debug("Task cycle run STARTING "+task);
        try {
        	task.recordRunStart(result);
		} catch (Exception e) {	// TODO: implement correctly after clarification
			LoggingUtils.logException(LOGGER, "Cannot record run start for task {}", e, task);
		}
	}

	/*
	 * Returns a flag whether to continue (false if the task has disappeared)
	 */
	private boolean recordCycleRunFinish(TaskRunResult runResult, OperationResult result) {
		LOGGER.debug("Task cycle run FINISHED "+task);
		try {
			task.recordRunFinish(runResult, result);
			return true;
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Cannot record run finish for task {}", ex, task);
			return false;
		} catch (SchemaException ex) {
			LOGGER.error("Unable to record run finish and close the task: {}", ex.getMessage(), ex);
			return true;
		}

	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		LOGGER.trace("Signalling shutdown to task " + task + ", executing in thread " + executingThread);
		task.signalShutdown();
		if (executingThread != null)			// in case this method would be (mistakenly?) called after the execution is over
			executingThread.interrupt();
	}

}
