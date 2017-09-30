/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.task.quartzimpl.execution;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.task.quartzimpl.TaskManagerQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImpl;
import com.evolveum.midpoint.task.quartzimpl.TaskQuartzImplUtil;
import com.evolveum.midpoint.task.quartzimpl.cluster.ClusterStatusInformation;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.quartz.*;
import org.springframework.security.core.Authentication;

import javax.xml.datatype.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@DisallowConcurrentExecution
public class JobExecutor implements InterruptableJob {

	private static TaskManagerQuartzImpl taskManagerImpl;
    private static final String DOT_CLASS = JobExecutor.class.getName() + ".";

    /*
     * Ugly hack - this class is instantiated not by Spring but explicitly by Quartz.
     */
	public static void setTaskManagerQuartzImpl(TaskManagerQuartzImpl tmqi) {
		taskManagerImpl = tmqi;
	}
	
	private static final transient Trace LOGGER = TraceManager.getTrace(JobExecutor.class);

    private static final long WATCHFUL_SLEEP_INCREMENT = 500;

    private static final int DEFAULT_RESCHEDULE_TIME_FOR_GROUP_LIMIT = 60;
    private static final int RESCHEDULE_TIME_RANDOMIZATION_INTERVAL = 3;
    private static final int RESCHEDULE_TIME_FOR_NO_SUITABLE_NODE = 60;

	/*
	 * JobExecutor is instantiated at each execution of the task, so we can store
	 * the task here.
	 * 
	 * http://quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/tutorial-lesson-03
	 * "Each (and every) time the scheduler executes the job, it creates a new instance of 
	 * the class before calling its execute(..) method."
	 */
	private volatile TaskQuartzImpl task;
	private volatile Thread executingThread;				// used for interruptions
	
	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		
		OperationResult executionResult = createOperationResult("execute");

        if (taskManagerImpl == null) {
            LOGGER.error("TaskManager not correctly set for JobExecutor, exiting the execution routine.");
            return;
        }

		// get the task instance
		String oid = context.getJobDetail().getKey().getName();
        try {
			task = (TaskQuartzImpl) taskManagerImpl.getTask(oid, executionResult);
		} catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Task with OID {} no longer exists, removing Quartz job and exiting the execution routine.", e, oid);
            taskManagerImpl.getExecutionManager().removeTaskFromQuartz(oid, executionResult);
            return;
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Task with OID {} cannot be retrieved because of schema exception. Please correct the problem or resynchronize midPoint repository with Quartz job store using 'xxxxxxx' function. Now exiting the execution routine.", e, oid);
            return;
        } catch (RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Task with OID {} could not be retrieved, exiting the execution routine.", e, oid);
			return;
		}

        if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
            LOGGER.warn("Task is not in RUNNABLE state (its state is {}), exiting its execution and removing its Quartz trigger. Task = {}", task.getExecutionStatus(), task);
            try {
                context.getScheduler().unscheduleJob(context.getTrigger().getKey());
            } catch (SchedulerException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot unschedule job for a non-RUNNABLE task {}", e, task);
            }
            return;
        }

        // if task manager is stopping or stopped, stop this task immediately
        // this can occur in rare situations, see https://jira.evolveum.com/browse/MID-1167
        if (!taskManagerImpl.isRunning()) {
            LOGGER.warn("Task was started while task manager is not running: exiting and rescheduling (if needed)");
            processTaskStop(executionResult);
            return;
        }

        boolean isRecovering = false;

        // if this is a restart, check whether the task is resilient
        if (context.isRecovering()) {

            // reset task node (there's potentially the old information from crashed node)
            try {
                if (task.getNode() != null) {
                    task.setNodeImmediate(null, executionResult);
                }
            } catch (ObjectNotFoundException | SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot reset executing-at-node information for recovering task {}", e, task);
            }

			if (!processTaskRecovery(executionResult)) {
                return;
            }
            isRecovering = true;
        }

		if (!checkExecutionConstraints(task, context, executionResult)) {
			return;			// rescheduling is done within the checker method
		}

        executingThread = Thread.currentThread();

		LOGGER.trace("execute called; task = {}, thread = {}, isRecovering = {}", task, executingThread, isRecovering);

        TaskHandler handler = null;
		try {

            taskManagerImpl.registerRunningTask(task);
        
			handler = taskManagerImpl.getHandler(task.getHandlerUri());
            logThreadRunStart(handler);
            taskManagerImpl.notifyTaskThreadStart(task, isRecovering);
		
			if (handler==null) {
				LOGGER.error("No handler for URI '{}', task {} - closing it.", task.getHandlerUri(), task);
                executionResult.recordFatalError("No handler for URI '" + task.getHandlerUri() + "', closing the task.");
                closeFlawedTask(task, executionResult);
                return;
				//throw new JobExecutionException("No handler for URI '" + task.getHandlerUri() + "'");
                // actually there is no point in throwing JEE; the only thing that is done with it is
                // that it is logged by Quartz
			}
			
			// Setup Spring Security context
			PrismObject<UserType> taskOwner = task.getOwner();
			try {
				// just to be sure we won't run the owner-setting login with any garbage security context (see MID-4160)
				taskManagerImpl.getSecurityEnforcer().setupPreAuthenticatedSecurityContext((Authentication) null);
				taskManagerImpl.getSecurityEnforcer().setupPreAuthenticatedSecurityContext(taskOwner);
			} catch (SchemaException e) {
	            LoggingUtils.logUnexpectedException(LOGGER, "Task with OID {} cannot be executed: error setting security context", e, oid);
	            return;
			}
		
			if (task.isCycle()) {
				executeRecurrentTask(handler);
			} else if (task.isSingle()) {
				executeSingleTask(handler, executionResult);
			} else {
				LOGGER.error("Tasks must be either recurrent or single-run. This one is neither. Sorry.");
                executionResult.recordFatalError("Tasks must be either recurrent or single-run. This one is neither. Closing it.");
                closeFlawedTask(task, executionResult);
			}
		
		} finally {

			try {
				waitForTransientChildrenAndCloseThem(executionResult);              // this is only a safety net; because we've waited for children just after executing a handler

				taskManagerImpl.unregisterRunningTask(task);
				executingThread = null;

				if (!task.canRun()) {
					processTaskStop(executionResult);
				}

				logThreadRunFinish(handler);
				taskManagerImpl.notifyTaskThreadFinish(task);
			} finally {
				// "logout" this thread
				taskManagerImpl.getSecurityEnforcer().setupPreAuthenticatedSecurityContext((Authentication) null);
			}
		}

	}

	// returns false if constraints are not met (i.e. execution should finish immediately)
	private boolean checkExecutionConstraints(TaskQuartzImpl task, JobExecutionContext context, OperationResult result) throws JobExecutionException {
		TaskExecutionConstraintsType executionConstraints = task.getExecutionConstraints();
		if (executionConstraints == null) {
			return true;
		}

		// group limit
		String group = executionConstraints.getGroup();
		if (group != null) {
			List<Task> tasksInGroup = new ArrayList<>();
			ClusterStatusInformation clusterStatusInformation = taskManagerImpl.getExecutionManager()
					.getClusterStatusInformation(true, false, result);
			for (ClusterStatusInformation.TaskInfo taskInfo : clusterStatusInformation.getTasks()) {
				Task runningTask;
				try {
					runningTask = taskManagerImpl.getTask(taskInfo.getOid(), result);
				} catch (ObjectNotFoundException e) {
					LOGGER.debug("Couldn't find running task {} when checking execution constraints: {}", taskInfo.getOid(), e.getMessage());
					continue;
				} catch (SchemaException e) {
					LoggingUtils.logUnexpectedException(LOGGER,
							"Couldn't retrieve running task {} when checking execution constraints", e, taskInfo.getOid());
					continue;
				}
				if (group.equals(runningTask.getGroup()) && !task.getOid().equals(runningTask.getOid())) {
					tasksInGroup.add(runningTask);
				}
			}
			int limit = executionConstraints.getGroupTaskLimit() != null ? executionConstraints.getGroupTaskLimit() : 1;
			LOGGER.trace("Tasks in group {}: {}", group, tasksInGroup);
			if (tasksInGroup.size() >= limit) {
				RescheduleTime rescheduleTime = getRescheduleTime(executionConstraints,
						DEFAULT_RESCHEDULE_TIME_FOR_GROUP_LIMIT, task.getNextRunStartTime(result));
				LOGGER.info("Limit of {} task(s) in group {} would be exceeded if task {} would start. Existing tasks: {}."
								+ " Will try again at {}{}.", limit, group, task, tasksInGroup, rescheduleTime.asDate(),
						rescheduleTime.regular ? " (i.e. at the next regular run time)" : "");
				if (!rescheduleTime.regular) {
					rescheduleLater(task, rescheduleTime.timestamp);
				}
				return false;
			}
		}

		// node restrictions
		String currentNode = taskManagerImpl.getNodeId();
		List<String> allowedNodes = executionConstraints.getAllowedNode();
		List<String> disallowedNodes = executionConstraints.getDisallowedNode();
		if (!passesAllowed(currentNode, allowedNodes)) {
			rescheduleToAllowedNode(task, "is not among allowed nodes (" + allowedNodes + ")", allowedNodes,
					disallowedNodes, context, result);
			return false;
		}
		if (!passesDisallowed(currentNode, disallowedNodes)) {
			rescheduleToAllowedNode(task, "is among disallowed nodes (" + disallowedNodes + ")", allowedNodes,
					disallowedNodes, context, result);
			return false;
		}

		return true;
	}

	private boolean passesAllowed(String currentNode, List<String> allowedNodes) {
		return allowedNodes.isEmpty() || allowedNodes.contains(currentNode);
	}

	private boolean passesDisallowed(String currentNode, List<String> disallowedNodes) {
		return !disallowedNodes.contains(currentNode);
	}

	private boolean passes(String node, List<String> allowedNodes, List<String> disallowedNodes) {
		return passesAllowed(node, allowedNodes) && passesDisallowed(node, disallowedNodes);
	}

	private void rescheduleToAllowedNode(TaskQuartzImpl task, String reason, List<String> allowedNodes,
			List<String> disallowedNodes, JobExecutionContext context, OperationResult result) throws JobExecutionException {
		String currentNode = taskManagerImpl.getNodeId();
		NodeType node = getAvailableNode(allowedNodes, disallowedNodes, result);
		if (node == null) {
			RescheduleTime rescheduleTime = getRescheduleTime(null,
					RESCHEDULE_TIME_FOR_NO_SUITABLE_NODE, task.getNextRunStartTime(result));
			LOGGER.info("Task {} cannot be executed on current node ({}) because it {}. But there is currently no suitable"
					+ " node to run it on. Will try again at {}{}.", task, currentNode, reason, rescheduleTime.asDate(),
					rescheduleTime.regular ? " (i.e. at the next regular run time)" : "");
			if (!rescheduleTime.regular) {
				rescheduleLater(task, rescheduleTime.timestamp);
			}
			return;
		}
		LOGGER.debug("Task {} cannot be executed on current node ({}) because it {}, trying to run it at {}.",
				task, currentNode, reason, node.getNodeIdentifier());
		taskManagerImpl.getExecutionManager().redirectTaskToNode(task, node, result);
	}

	private NodeType getAvailableNode(List<String> allowedNodes, List<String> disallowedNodes, OperationResult result) {
		ClusterStatusInformation clusterStatusInformation = taskManagerImpl.getExecutionManager()
				.getClusterStatusInformation(true, false, result);
		List<NodeType> matching = clusterStatusInformation.getNodes().stream()
				.filter(node -> passes(node.getNodeIdentifier(), allowedNodes, disallowedNodes)
						&& node.getExecutionStatus() == NodeExecutionStatusType.RUNNING)
				.collect(Collectors.toList());
		if (matching.isEmpty()) {
			return null;
		} else {
			return matching.get((int) (Math.random() * matching.size()));
		}
	}

	private class RescheduleTime {
		private final long timestamp;
		private final boolean regular;
		private RescheduleTime(long timestamp, boolean regular) {
			this.timestamp = timestamp;
			this.regular = regular;
		}
		public Date asDate() {
			return new Date(timestamp);
		}
	}

	private RescheduleTime getRescheduleTime(TaskExecutionConstraintsType executionConstraints, int defaultInterval, Long nextTaskRunTime) {
		long retryAt;
		Duration retryAfter = executionConstraints != null ? executionConstraints.getRetryAfter() : null;
		if (retryAfter != null) {
			retryAt = XmlTypeConverter.toMillis(
							XmlTypeConverter.addDuration(
									XmlTypeConverter.createXMLGregorianCalendar(new Date()), retryAfter));
		} else {
			retryAt = System.currentTimeMillis() + defaultInterval * 1000L;
		}
		retryAt += Math.random() * RESCHEDULE_TIME_RANDOMIZATION_INTERVAL * 1000.0;     // to avoid endless collisions
		if (nextTaskRunTime != null && nextTaskRunTime < retryAt) {
			return new RescheduleTime(nextTaskRunTime, true);
		} else {
			return new RescheduleTime(retryAt, false);
		}
	}

	private void rescheduleLater(TaskQuartzImpl task, long startAt) throws JobExecutionException {
		Trigger trigger = TaskQuartzImplUtil.createTriggerForTask(task, startAt);
		try {
			taskManagerImpl.getExecutionManager().getQuartzScheduler().scheduleJob(trigger);
		} catch (SchedulerException e) {
			// TODO or handle it somehow?
			throw new JobExecutionException("Couldn't reschedule task " + task + " (rescheduled because" +
					" of execution constraints): " + e.getMessage(), e);
		}
	}

//	private void rescheduleElsewhere(TaskQuartzImpl task, String node) throws JobExecutionException {
//		Trigger trigger = TaskQuartzImplUtil.createTriggerNowForTask(task);
//		try {
//			taskManagerImpl.getExecutionManager().scheduleTaskNow(task, node, );
//			//taskManagerImpl.getExecutionManager().getQuartzScheduler().scheduleJob(trigger);
//		} catch (SchedulerException e) {
//			// TODO or handle it somehow?
//			throw new JobExecutionException("Couldn't reschedule task " + task + " (rescheduled because" +
//					" of execution constraints): " + e.getMessage(), e);
//		}
//	}

	private void waitForTransientChildrenAndCloseThem(OperationResult result) {
        taskManagerImpl.waitForTransientChildren(task, result);

        // at this moment, there should be no executing child tasks... we just clean-up all runnables that had not started
        for (Task subtask : task.getLightweightAsynchronousSubtasks()) {
            if (subtask.getExecutionStatus() == TaskExecutionStatus.RUNNABLE) {
                if (((TaskQuartzImpl) subtask).getLightweightHandlerFuture() == null) {
                    LOGGER.trace("Lightweight task handler for subtask {} has not started yet; closing the task.", subtask);
                    closeTask(task, result);
                }
            }
        }
    }

    // returns true if the execution of the task should continue
    private boolean processTaskRecovery(OperationResult executionResult) {
        if (task.getThreadStopAction() == ThreadStopActionType.CLOSE) {
            LOGGER.info("Closing recovered non-resilient task {}", task);
            closeTask(task, executionResult);
            return false;
        } else if (task.getThreadStopAction() == ThreadStopActionType.SUSPEND) {
            LOGGER.info("Suspending recovered non-resilient task {}", task);
            taskManagerImpl.suspendTask(task, TaskManager.DO_NOT_STOP, executionResult);        // we must NOT wait here, as we would wait infinitely -- we do not have to stop the task neither, because we are that task :)
            return false;
        } else if (task.getThreadStopAction() == null || task.getThreadStopAction() == ThreadStopActionType.RESTART) {
            LOGGER.info("Recovering resilient task {}", task);
            return true;
        } else if (task.getThreadStopAction() == ThreadStopActionType.RESCHEDULE) {
            if (task.getRecurrenceStatus() == TaskRecurrence.RECURRING && task.isLooselyBound()) {
                LOGGER.info("Recovering resilient task with RESCHEDULE thread stop action - exiting the execution, the task will be rescheduled; task = {}", task);
                return false;
            } else {
                LOGGER.info("Recovering resilient task {}", task);
                return true;
            }
        } else {
            throw new SystemException("Unknown value of ThreadStopAction: " + task.getThreadStopAction() + " for task " + task);
        }
    }

    // called when task is externally stopped (can occur on node shutdown, node scheduler stop, node threads deactivation, or task suspension)
    // we have to act (i.e. reschedule resilient tasks or close/suspend non-resilient tasks) in all cases, except task suspension
    // we recognize it by looking at task status: RUNNABLE means that the task is stopped as part of node shutdown
    private void processTaskStop(OperationResult executionResult) {

        try {
            task.refresh(executionResult);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "ThreadStopAction cannot be applied, because the task no longer exists: " + task, e);
            return;
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "ThreadStopAction cannot be applied, because of schema exception. Task = " + task, e);
            return;
        }

        if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
            LOGGER.trace("processTaskStop: task execution status is not RUNNABLE (it is " + task.getExecutionStatus() + "), so ThreadStopAction does not apply; task = " + task);
            return;
        }

        if (task.getThreadStopAction() == ThreadStopActionType.CLOSE) {
            LOGGER.info("Closing non-resilient task on node shutdown; task = {}", task);
            closeTask(task, executionResult);
        } else if (task.getThreadStopAction() == ThreadStopActionType.SUSPEND) {
            LOGGER.info("Suspending non-resilient task on node shutdown; task = {}", task);
            taskManagerImpl.suspendTask(task, TaskManager.DO_NOT_STOP, executionResult);            // we must NOT wait here, as we would wait infinitely -- we do not have to stop the task neither, because we are that task
        } else if (task.getThreadStopAction() == null || task.getThreadStopAction() == ThreadStopActionType.RESTART) {
            LOGGER.info("Node going down: Rescheduling resilient task to run immediately; task = {}", task);
            taskManagerImpl.scheduleRunnableTaskNow(task, executionResult);
        } else if (task.getThreadStopAction() == ThreadStopActionType.RESCHEDULE) {
            if (task.getRecurrenceStatus() == TaskRecurrence.RECURRING && task.isLooselyBound()) {
				// nothing to do, task will be automatically started by Quartz on next trigger fire time
            } else {
                taskManagerImpl.scheduleRunnableTaskNow(task, executionResult);     // for tightly-bound tasks we do not know next schedule time, so we run them immediately
            }
        } else {
            throw new SystemException("Unknown value of ThreadStopAction: " + task.getThreadStopAction() + " for task " + task);
        }
    }

    private void closeFlawedTask(TaskQuartzImpl task, OperationResult result) {
        LOGGER.info("Closing flawed task {}", task);
        try {
            task.setResultImmediate(result, result);
        } catch (ObjectNotFoundException  e) {
            LoggingUtils.logException(LOGGER, "Couldn't store operation result into the task {}", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't store operation result into the task {}", e, task);
        }
		closeTask(task, result);
    }

    private void closeTask(TaskQuartzImpl task, OperationResult result) {
        try {
            taskManagerImpl.closeTask(task, result);
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Cannot close task {}, because it does not exist in repository.", e, task);
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot close task {} due to schema exception", e, task);
        } catch (SystemException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot close task {} due to system exception", e, task);
        }
    }

	private void executeSingleTask(TaskHandler handler, OperationResult executionResult) throws JobExecutionException {

        Validate.notNull(handler, "Task handler is null");

		try {
			
			TaskRunResult runResult;

			recordCycleRunStart(executionResult, handler);
            runResult = executeHandler(handler, executionResult);        // exceptions thrown by handler are handled in executeHandler()

            // we should record finish-related information before dealing with (potential) task closure/restart
            // so we place this method call before the following block
            recordCycleRunFinish(runResult, handler, executionResult);

            // should be after recordCycleRunFinish, e.g. not to overwrite task result
            task.refresh(executionResult);

            // let us treat various exit situations here...

            if (!task.canRun() || runResult.getRunResultStatus() == TaskRunResultStatus.INTERRUPTED) {
                // first, if a task was interrupted, we do not want to change its status
                LOGGER.trace("Task was interrupted, exiting the execution routine. Task = {}", task);
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.TEMPORARY_ERROR) {
                // in case of temporary error, we want to suspend the task and exit
                LOGGER.info("Task encountered temporary error, suspending it. Task = {}", task);
                taskManagerImpl.suspendTask(task, TaskManager.DO_NOT_STOP, executionResult);
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.RESTART_REQUESTED) {
                // in case of RESTART_REQUESTED we have to get (new) current handler and restart it
                // this is implemented by pushHandler and by Quartz
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.PERMANENT_ERROR) {
                // PERMANENT ERROR means we do not continue executing other handlers, we just close this task
                taskManagerImpl.closeTask(task, executionResult);
            } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED || runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED_HANDLER) {
                // FINISHED/FINISHED_HANDLER means we continue with other handlers, if there are any
                task.finishHandler(executionResult);			// this also closes the task, if there are no remaining handlers
                // if there are remaining handlers, task will be re-executed by Quartz
            } else {
                throw new IllegalStateException("Invalid value for Task's runResultStatus: " + runResult.getRunResultStatus() + " for task " + task);
            }

		} catch (Throwable t) {
			LoggingUtils.logUnexpectedException(LOGGER, "An exception occurred during processing of task {}", t, task);
			//throw new JobExecutionException("An exception occurred during processing of task " + task, t);
		}
	}
	
	private void executeRecurrentTask(TaskHandler handler) throws JobExecutionException {

		try {

mainCycle:

			while (task.canRun()) {

                // executionResult should be initialized here (inside the loop), because for long-running tightly-bound
                // recurring tasks it would otherwise bloat indefinitely
                OperationResult executionResult = createOperationResult("executeTaskRun");

                if (!task.stillCanStart()) {
                    LOGGER.trace("CycleRunner loop: task latest start time ({}) has elapsed, exiting the execution cycle. Task = {}", task.getSchedule().getLatestStartTime(), task);
                    break;
                }
				
				LOGGER.trace("CycleRunner loop: start");

                TaskRunResult runResult;
                recordCycleRunStart(executionResult, handler);
                runResult = executeHandler(handler, executionResult);
                boolean canContinue = recordCycleRunFinish(runResult, handler, executionResult);
                if (!canContinue) { // in case of task disappeared
                    break;
                }

                // let us treat various exit situations here...

                if (!task.canRun() || runResult.getRunResultStatus() == TaskRunResultStatus.INTERRUPTED) {
                    // first, if a task was interrupted, we do not want to change its status
                    LOGGER.trace("Task was interrupted, exiting the execution cycle. Task = {}", task);
                    break;
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.TEMPORARY_ERROR) {
                    LOGGER.trace("Task encountered temporary error, continuing with the execution cycle. Task = {}", task);
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.RESTART_REQUESTED) {
                    // in case of RESTART_REQUESTED we have to get (new) current handler and restart it
                    // this is implemented by pushHandler and by Quartz
                    LOGGER.trace("Task returned RESTART_REQUESTED state, exiting the execution cycle. Task = {}", task);
                    break;
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.PERMANENT_ERROR) {
                    LOGGER.info("Task encountered permanent error, suspending the task. Task = {}", task);
                    taskManagerImpl.suspendTask(task, TaskManager.DO_NOT_STOP, executionResult);
                    break;
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED) {
                    LOGGER.trace("Task handler finished, continuing with the execution cycle. Task = {}", task);
                } else if (runResult.getRunResultStatus() == TaskRunResultStatus.FINISHED_HANDLER) {
                    LOGGER.trace("Task handler finished with FINISHED_HANDLER, calling task.finishHandler() and exiting the execution cycle. Task = {}", task);
                    task.finishHandler(executionResult);			// this also closes the task, if there are no remaining handlers
                                                                    // if there are remaining handlers, task will be re-executed by Quartz
                    break;
                } else {
                    throw new IllegalStateException("Invalid value for Task's runResultStatus: " + runResult.getRunResultStatus() + " for task " + task);
                }

                // if the task is loosely-bound, exit the loop here
				if (task.isLooselyBound()) {
					LOGGER.trace("CycleRunner loop: task is loosely bound, exiting the execution cycle");
					break;
				}

                // or, was the task suspended (closed, ...) remotely?
                LOGGER.trace("CycleRunner loop: refreshing task after one iteration, task = {}", task);
                try {
                    task.refresh(executionResult);
                } catch (ObjectNotFoundException ex) {
                    LOGGER.error("Error refreshing task "+task+": Object not found: "+ex.getMessage(),ex);
                    return;			// The task object in repo is gone. Therefore this task should not run any more.
                }

                if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
                    LOGGER.info("Task not in the RUNNABLE state, exiting the execution routing. State = {}, Task = {}", task.getExecutionStatus(), task);
                    break;
                }

                // Determine how long we need to sleep and hit the bed

				Integer interval = task.getSchedule() != null ? task.getSchedule().getInterval() : null;
				if (interval == null) {
					LOGGER.error("Tightly bound task " + task + " has no scheduling interval specified.");
					break;
				}

                long lastRunStartTime = task.getLastRunStartTimestamp() == null ? 0 : task.getLastRunStartTimestamp();
				long sleepFor = lastRunStartTime + (interval * 1000) - System.currentTimeMillis();
				if (sleepFor < 0)
					sleepFor = 0;
				
				LOGGER.trace("CycleRunner loop: sleep ({})", sleepFor);

                for (long time = 0; time < sleepFor + WATCHFUL_SLEEP_INCREMENT; time += WATCHFUL_SLEEP_INCREMENT) {
                    try {
                        Thread.sleep(WATCHFUL_SLEEP_INCREMENT);
                    } catch (InterruptedException e) {
                        // safely ignored
                    }
                    if (!task.canRun()) {
                        LOGGER.trace("CycleRunner loop: sleep interrupted, task.canRun == false");
                        break mainCycle;
                    }
                }

                LOGGER.trace("CycleRunner loop: refreshing task after sleep, task = {}", task);
				try {
					task.refresh(executionResult);
				} catch (ObjectNotFoundException ex) {
					LOGGER.error("Error refreshing task "+task+": Object not found: "+ex.getMessage(),ex);
					return;			// The task object in repo is gone. Therefore this task should not run any more. Therefore commit seppuku
				}

                if (task.getExecutionStatus() != TaskExecutionStatus.RUNNABLE) {
                    LOGGER.info("Task not in the RUNNABLE state, exiting the execution routine. State = {}, Task = {}", task.getExecutionStatus(), task);
                    break;
                }

                LOGGER.trace("CycleRunner loop: end");
			}

		} catch (Throwable t) {
			// This is supposed to run in a thread, so this kind of heavy artillery is needed. If throwable won't be
			// caught here, nobody will catch it and it won't even get logged.
			if (task.canRun()) {
				LOGGER.error("CycleRunner got unexpected exception: {}: {}; task = {}", t.getClass().getName(), t.getMessage(), task, t);
			} else {
				LOGGER.info("CycleRunner got unexpected exception while shutting down: {}: {}; task = {}", t.getClass().getName(), t.getMessage(), task);
				LOGGER.trace("CycleRunner got unexpected exception while shutting down: {}: {}; task = {}", t.getClass().getName(), t.getMessage(), task, t);
			}
			//throw new JobExecutionException("An exception occurred during processing of task " + task, t);
		}
	}
	
    private TaskRunResult executeHandler(TaskHandler handler, OperationResult executionResult) {

    	TaskRunResult runResult;
    	try {
			LOGGER.trace("Executing handler {}", handler.getClass().getName());
    		runResult = handler.run(task);
    		if (runResult == null) {				// Obviously an error in task handler
                LOGGER.error("Unable to record run finish: task returned null result");
				runResult = createFailureTaskRunResult("Unable to record run finish: task returned null result", null);
			}
    	} catch (Throwable t) {
			LOGGER.error("Task handler threw unexpected exception: {}: {}; task = {}", t.getClass().getName(), t.getMessage(), task, t);
            runResult = createFailureTaskRunResult("Task handler threw unexpected exception: " + t.getMessage(), t);
    	}

        waitForTransientChildrenAndCloseThem(executionResult);

        return runResult;
	}

    private TaskRunResult createFailureTaskRunResult(String message, Throwable t) {
        TaskRunResult runResult = new TaskRunResult();
        OperationResult opResult;
        if (task.getResult() != null) {
            opResult = task.getResult();
        } else {
            opResult = createOperationResult(DOT_CLASS + "executeHandler");
        }
        if (t != null) {
            opResult.recordFatalError(message, t);
        } else {
            opResult.recordFatalError(message);
        }
        runResult.setOperationResult(opResult);
        runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
        runResult.setProgress(task.getProgress());
        return runResult;
    }


    private OperationResult createOperationResult(String methodName) {
		return new OperationResult(DOT_CLASS + methodName);
	}
	
	private void logThreadRunStart(TaskHandler handler) {
		LOGGER.trace("Task thread run STARTING  " + task + ", handler = " + handler);
	}
	
	private void logThreadRunFinish(TaskHandler handler) {
		LOGGER.trace("Task thread run FINISHED " + task + ", handler = " + handler);
	}
    
	private void recordCycleRunStart(OperationResult result, TaskHandler handler) {
		LOGGER.trace("Task cycle run STARTING " + task + ", handler = " + handler);
        taskManagerImpl.notifyTaskStart(task);
        try {
            task.setLastRunStartTimestamp(System.currentTimeMillis());
            if (task.getCategory() == null) {
                task.setCategory(task.getCategoryFromHandler());
            }
            task.setNode(taskManagerImpl.getNodeId());
            OperationResult newResult = new OperationResult("run");
			newResult.setStatus(OperationResultStatus.IN_PROGRESS);
            task.setResult(newResult);										// MID-4033
            task.savePendingModifications(result);
        } catch (Exception e) {	// TODO: implement correctly after clarification
			LoggingUtils.logUnexpectedException(LOGGER, "Cannot record run start for task {}", e, task);
		}
	}

	/*
	 * Returns a flag whether to continue (false if the task has disappeared)
	 */
	private boolean recordCycleRunFinish(TaskRunResult runResult, TaskHandler handler, OperationResult result) {
		LOGGER.trace("Task cycle run FINISHED " + task + ", handler = " + handler);
        taskManagerImpl.notifyTaskFinish(task, runResult);
		try {
            task.setProgress(runResult.getProgress());
            task.setLastRunFinishTimestamp(System.currentTimeMillis());
            if (runResult.getOperationResult() != null) {
                try {
					OperationResult taskResult = runResult.getOperationResult().clone();
                    taskResult.cleanupResult();
					taskResult.summarize(true);
					task.setResult(taskResult);
                } catch (Throwable ex) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Problem with task result cleanup/summarize - continuing with raw result", ex);
					task.setResult(runResult.getOperationResult());
                }
            }
            task.setNode(null);
            task.savePendingModifications(result);

			return true;
		} catch (ObjectNotFoundException ex) {
			LoggingUtils.logException(LOGGER, "Cannot record run finish for task {}", ex, task);
			return false;
        } catch (com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot record run finish for task {}", ex, task);
            return true;
		} catch (SchemaException ex) {
			LoggingUtils.logUnexpectedException(LOGGER, "Unable to record run finish and close the task: {}", ex, task);
			return true;
		}
	}

	@Override
	public void interrupt() throws UnableToInterruptJobException {
		LOGGER.trace("Trying to shut down the task " + task + ", executing in thread " + executingThread);

        boolean interruptsAlways = taskManagerImpl.getConfiguration().getUseThreadInterrupt() == UseThreadInterrupt.ALWAYS;
        boolean interruptsMaybe = taskManagerImpl.getConfiguration().getUseThreadInterrupt() != UseThreadInterrupt.NEVER;
        if (task != null) {
            task.unsetCanRun();
            for (Task subtask : task.getRunningLightweightAsynchronousSubtasks()) {
                TaskQuartzImpl subtaskq = (TaskQuartzImpl) subtask;
                subtaskq.unsetCanRun();
                // if we want to cancel the Future using interrupts, we have to do it now
                // because after calling cancel(false) subsequent calls to cancel(true) have no effect whatsoever
                subtaskq.getLightweightHandlerFuture().cancel(interruptsMaybe);
            }
        }
        if (interruptsAlways) {
            sendThreadInterrupt(false);         // subtasks were interrupted by their futures
        }
	}

    void sendThreadInterrupt() {
        sendThreadInterrupt(true);
    }

    // beware: Do not touch task prism here, because this method can be called asynchronously
	private void sendThreadInterrupt(boolean alsoSubtasks) {
        if (executingThread != null) {			// in case this method would be (mistakenly?) called after the execution is over
            LOGGER.trace("Calling Thread.interrupt on thread {}.", executingThread);
            executingThread.interrupt();
            LOGGER.trace("Thread.interrupt was called on thread {}.", executingThread);
        }
        if (alsoSubtasks) {
            for (Task subtask : task.getRunningLightweightAsynchronousSubtasks()) {
                //LOGGER.trace("Calling Future.cancel(mayInterruptIfRunning:=true) on a future for LAT subtask {}", subtask);
                ((TaskQuartzImpl) subtask).getLightweightHandlerFuture().cancel(true);
            }
        }
    }

    // should be used only for testing
    public Thread getExecutingThread() {
        return executingThread;
    }
}
