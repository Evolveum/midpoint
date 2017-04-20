/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.wf.impl.activiti.dao;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.activiti.ActivitiEngine;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricProcessInstanceQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author mederly
 */

@Component
public class ProcessInstanceManager {

    private static final transient Trace LOGGER = TraceManager.getTrace(ProcessInstanceManager.class);

    @Autowired private ActivitiEngine activitiEngine;
	@Autowired private TaskManager taskManager;
	@Autowired private PrismContext prismContext;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_STOP_PROCESS_INSTANCE = DOT_INTERFACE + "stopProcessInstance";
    private static final String OPERATION_DELETE_PROCESS_INSTANCE = DOT_INTERFACE + "deleteProcessInstance";
    private static final String OPERATION_SYNCHRONIZE_WORKFLOW_REQUESTS = DOT_INTERFACE + "synchronizeWorkflowRequests";

    public void stopProcessInstance(String instanceId, String username, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_STOP_PROCESS_INSTANCE);
        result.addParam("instanceId", instanceId);

        RuntimeService rs = activitiEngine.getRuntimeService();
        try {
            LOGGER.trace("Stopping process instance {} on the request of {}", instanceId, username);
            String deletionMessage = "Process instance stopped on the request of " + username;
            rs.setVariable(instanceId, CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_IS_STOPPING, Boolean.TRUE);
            rs.deleteProcessInstance(instanceId, deletionMessage);
        } catch (RuntimeException e) {
            result.recordFatalError("Process instance couldn't be stopped: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void deleteProcessInstance(String instanceId, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_PROCESS_INSTANCE);
        result.addParam("instanceId", instanceId);

        HistoryService hs = activitiEngine.getHistoryService();
        try {
            hs.deleteHistoricProcessInstance(instanceId);
        } catch (RuntimeException e) {
            result.recordFatalError("Process instance couldn't be deleted: " + e.getMessage(), e);
			throw e;
        } finally {
			result.computeStatusIfUnknown();
		}
    }

	public void onTaskDelete(Task task, OperationResult result) {
		try {
			WfContextType wfc = task.getWorkflowContext();
			if (wfc == null || wfc.getProcessInstanceId() == null) {
				return;
			}
			String instanceId = wfc.getProcessInstanceId();
			if (wfc.getEndTimestamp() == null) {
				try {
					stopProcessInstance(instanceId, "task delete action", result);
				} catch (RuntimeException e) {
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't stop workflow process instance {} while processing task deletion event for task {}", e, instanceId, task);
				}
			}
			deleteProcessInstance(instanceId, result);
		} catch (RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't process task deletion event for task {}", e, task);
		}
	}

	class Statistics {
		int processes = 0;
		int processesWithNonExistingTaskOid = 0;
		int processesWithNonWorkflowTaskOid = 0;
		int processesWithWrongWorkflowTaskOid = 0;
		int processesWithoutTaskOid = 0;
		int processesRemoved = 0;
		int wrongProcessesRemaining = 0;
		int tasks = 0;
		int tasksWithNonExistingPid = 0;
		int tasksWithNonMidpointProcesses = 0;
		int tasksWithWrongProcesses = 0;
		int tasksRemoved = 0;
		int wrongTasksRemaining = 0;
	}

	public void synchronizeWorkflowRequests(OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(OPERATION_SYNCHRONIZE_WORKFLOW_REQUESTS);
		try {
			LOGGER.info("Starting synchronization of workflow requests between repository and Activiti");
			final Set<String> activeProcessInstances = new HashSet<>();
			final Map<String,String> activitiToMidpoint = getActivitiToMidpoint(activeProcessInstances, result);
			final Map<String,String> midpointToActiviti = getMidpointToActiviti(result);
			Statistics s = new Statistics();
			doPhase1(activitiToMidpoint, midpointToActiviti, activeProcessInstances, s, result);
			doPhase2(activitiToMidpoint, midpointToActiviti, s, result);
			String message = s.processes + " processes found; out of these, removed " + s.processesRemoved + " ones; "
					+ "remaining " + s.wrongProcessesRemaining + " wrong ones. " +
					s.tasks + " tasks found; out of these, removed " + s.tasksRemoved + " ones; "
					+ "remaining " + s.wrongTasksRemaining + " wrong ones.";
			result.recordStatus(OperationResultStatus.SUCCESS, message);
		} catch (RuntimeException|SchemaException e) {
			result.recordFatalError("Workflow requests cannot be synchronized: " + e.getMessage());
		} finally {
			result.computeStatusIfUnknown();
			LOGGER.info("Synchronization of workflow requests between repository and Activiti finished with the status of {}: {}",
					result.getStatus(), result.getMessage());
		}
	}

	protected void doPhase1(Map<String, String> activitiToMidpoint, Map<String, String> midpointToActiviti,
			Set<String> activeProcessInstances, Statistics s, OperationResult result) {
		s.processes = activitiToMidpoint.size();
		final Iterator<Map.Entry<String,String>> iterator = activitiToMidpoint.entrySet().iterator();

		while (iterator.hasNext()) {
			final Map.Entry<String,String> entry = iterator.next();
			final String pid = entry.getKey();
			final String oid = entry.getValue();
			if (oid != null) {
				if (!midpointToActiviti.containsKey(oid)) {
					LOGGER.warn("Activiti process {} points to non-existing task OID {} -- deleting it", pid, oid);
					deleteProcessInstanceChecked(pid, activeProcessInstances, s, result);
					iterator.remove();
					s.processesWithNonExistingTaskOid++;
				} else {
					String pid2 = midpointToActiviti.get(oid);
					if (pid2 == null) {
						LOGGER.warn("Activiti process {} points to non-workflow task OID {} -- deleting it", pid, oid);
						deleteProcessInstanceChecked(pid, activeProcessInstances, s, result);
						iterator.remove();
						s.processesWithNonWorkflowTaskOid++;
					} else if (!pid2.equals(pid)) {
						LOGGER.error(
								"Activiti process {} points to task OID {} that points back to different process: {} -- please resolve manually",
								pid, oid, pid2);
						s.processesWithWrongWorkflowTaskOid++;
						s.wrongProcessesRemaining++;
					}
				}
			} else {
				// probably no problem - this is an activiti process with no midPoint task attached
				LOGGER.trace("Activiti process with no midPoint task attached: {}", pid);
				s.processesWithoutTaskOid++;
			}
		}
		LOGGER.info("Results of phase 1:\n"
				+ "- processes with non-existing task OID: {}\n"
				+ "- processes with non-workflow task OID: {}\n"
				+ "- processes with wrong task OID (of such that points to other process instance): {}\n"
				+ "- processes with no task OID: {}\n"
				+ "- successfully deleted processes: {}", s.processesWithNonExistingTaskOid, s.processesWithNonWorkflowTaskOid,
				s.processesWithWrongWorkflowTaskOid, s.processesWithoutTaskOid, s.processesRemoved);
	}

	protected void doPhase2(Map<String, String> activitiToMidpoint, Map<String, String> midpointToActiviti, Statistics s,
			OperationResult result) {
		s.tasks = midpointToActiviti.size();
		final Iterator<Map.Entry<String,String>> iterator = midpointToActiviti.entrySet().iterator();

		while (iterator.hasNext()) {
			final Map.Entry<String,String> entry = iterator.next();
			final String oid = entry.getKey();
			final String pid = entry.getValue();
			if (pid == null) {
				// not a workflow-enabled task
				continue;
			}
			if (!activitiToMidpoint.containsKey(pid)) {
				LOGGER.warn("Task {} points to non-existing activiti process ID {} -- deleting it", oid, pid);
				deleteTaskChecked(oid, s, result);
				iterator.remove();
				s.tasksWithNonExistingPid++;
				continue;
			}
			String oid2 = activitiToMidpoint.get(pid);
			if (oid2 == null) {
				LOGGER.warn("Task {} points to non-midPoint activiti process ID {} -- deleting it", pid, oid);
				deleteTaskChecked(oid, s, result);
				iterator.remove();
				s.tasksWithNonMidpointProcesses++;
				continue;
			}
			if (!oid2.equals(oid)) {
				LOGGER.warn("Task {} points to activiti process ID {} that points back to different task -- please resolve manually", pid, oid);
				s.tasksWithWrongProcesses++;
				s.wrongTasksRemaining++;
			}
		}
		LOGGER.info("Results of phase 2:\n"
						+ "- tasks with non-existing process ID: {}\n"
						+ "- tasks with non-midPoint process ID: {}\n"
						+ "- tasks with wrong process ID (such that points to other task): {}\n"
				        + "- successfully deleted tasks: {}",
						s.tasksWithNonExistingPid, s.tasksWithNonMidpointProcesses, s.tasksWithWrongProcesses,
						s.tasksRemoved);
	}

	private void deleteProcessInstanceChecked(String pid, Set<String> activeProcessInstances, Statistics s, OperationResult result) {
		try {
			if (activeProcessInstances.contains(pid)) {
				try {
					stopProcessInstance(pid, "workflow requests synchronization process", result);
				} catch (RuntimeException e) {
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't stop process instance {}", e, pid);
				}
			}
			deleteProcessInstance(pid, result);
			s.processesRemoved++;
		} catch (RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove process instance {}", e, pid);
			s.wrongProcessesRemaining++;
		}
	}

	private void deleteTaskChecked(String oid, Statistics s, OperationResult result) {
		try {
			taskManager.deleteTask(oid, result);
			s.tasksRemoved++;
		} catch (ObjectNotFoundException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove task {} as it seems to be no longer existing", e, oid);
		} catch (RuntimeException|SchemaException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove task {}", e, oid);
			s.wrongTasksRemaining++;
		}
	}


	private Map<String, String> getMidpointToActiviti(OperationResult result) throws SchemaException {
		final Map<String,String> rv = new HashMap<>();
		final List<PrismObject<TaskType>> tasks = taskManager.searchObjects(TaskType.class, null, null, result);
		int tasksWithProcessId = 0;
		for (PrismObject<TaskType> taskObject : tasks) {
			final TaskType task = taskObject.asObjectable();
			final WfContextType wfc = task.getWorkflowContext();
			final String pid = wfc != null ? wfc.getProcessInstanceId() : null;
			rv.put(task.getOid(), pid);
			if (pid != null) {
				tasksWithProcessId++;
			}
		}
		LOGGER.info("Found {} tasks; among these, {} have a pointer to process instance id", rv.size(), tasksWithProcessId);
		return rv;
	}

	private Map<String, String> getActivitiToMidpoint(Set<String> activeProcessInstances, OperationResult result) {
		Map<String,String> rv = new HashMap<>();
		int processWithoutTaskOidCount = 0;
		HistoricProcessInstanceQuery query = activitiEngine.getHistoryService().createHistoricProcessInstanceQuery()
				.includeProcessVariables()
				.excludeSubprocesses(true);
		List<HistoricProcessInstance> processes = query.list();
		for (HistoricProcessInstance process : processes) {
			String taskOid = (String) process.getProcessVariables().get(CommonProcessVariableNames.VARIABLE_MIDPOINT_TASK_OID);
			rv.put(process.getId(), taskOid);
			if (taskOid == null) {
				processWithoutTaskOidCount++;
			}
			if (process.getEndTime() == null) {
				activeProcessInstances.add(process.getId());
			}
		}
		LOGGER.info("Found {} processes; among these, {} have no task OID. Active processes: {}",
				rv.size(), processWithoutTaskOidCount, activeProcessInstances.size());
		return rv;
	}

	public void cleanupActivitiProcesses(OperationResult result) throws SchemaException {
		RuntimeService runtimeService = activitiEngine.getRuntimeService();
		TaskService taskService = activitiEngine.getTaskService();

		LOGGER.info("Starting cleanup of Activiti processes");
		Collection<String> processInstancesToKeep = getProcessInstancesToKeep(result);
		LOGGER.info("Process instances to keep: {}", processInstancesToKeep);

		List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery().list();
		LOGGER.info("Existing process instances in Activiti: {}", instances.size());
		int ok = 0, fail = 0;
		for (ProcessInstance instance : instances) {
			String instanceId = instance.getId();
			if (processInstancesToKeep.contains(instanceId)) {
				continue;
			}
			LOGGER.debug("Deleting process instance {}", instance);
			try {
				runtimeService.setVariable(instanceId, CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_IS_STOPPING, Boolean.TRUE);
				runtimeService.deleteProcessInstance(instanceId, "Deleted as part of activiti processes cleanup");
				ok++;
			} catch (Throwable t) {
				LOGGER.info("Couldn't delete process instance {}, retrying with explicit deletion of some variables for its tasks", instanceId);
				List<org.activiti.engine.task.Task> tasks = taskService.createTaskQuery()
						.processInstanceId(instanceId).list();
				LOGGER.debug("Tasks: {}", tasks);
				for (org.activiti.engine.task.Task task : tasks) {
					taskService.removeVariables(task.getId(), Arrays.asList("approvalSchema", "level"));
				}
				try {
					runtimeService.deleteProcessInstance(instanceId, "Deleted as part of activiti processes cleanup");
					ok++;
				} catch (Throwable t2) {
					result.createSubresult(ProcessInstanceManager.class.getName() + ".cleanupActivitiProcess")
							.recordPartialError("Couldn't delete Activiti process instance " + instanceId + ": " + t2.getMessage(),
									t2);
					fail++;
				}
			}
		}
		String message = "Successfully deleted "+ok+" instances; failed "+fail+" times";
		LOGGER.info(message);
		result.recordStatus(fail > 0 ? OperationResultStatus.PARTIAL_ERROR : OperationResultStatus.SUCCESS, message);
	}

	private Set<String> getProcessInstancesToKeep(OperationResult result) throws SchemaException {
		ObjectQuery query = QueryBuilder.queryFor(TaskType.class, prismContext)
				.not().item(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_PROCESS_INSTANCE_ID).isNull()
				.build();
		SearchResultList<PrismObject<TaskType>> tasks = taskManager.searchObjects(TaskType.class, query, null, result);
		return tasks.stream()
				.map(t -> t.asObjectable().getWorkflowContext().getProcessInstanceId())
				.collect(Collectors.toSet());
	}

}
