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

package com.evolveum.midpoint.wf.impl.engine.dao;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfContextType;
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

	@Autowired private TaskManager taskManager;
	@Autowired private PrismContext prismContext;
	@Autowired private WorkflowEngine workflowEngine;
	@Autowired private RepositoryService repositoryService;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_STOP_PROCESS_INSTANCE = DOT_INTERFACE + "stopProcessInstance";
    private static final String OPERATION_DELETE_PROCESS_INSTANCE = DOT_INTERFACE + "deleteProcessInstance";
    private static final String OPERATION_SYNCHRONIZE_WORKFLOW_REQUESTS = DOT_INTERFACE + "synchronizeWorkflowRequests";

    public void closeCase(String caseOid, String username, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_STOP_PROCESS_INSTANCE);
        result.addParam("caseOid", caseOid);

        try {
            LOGGER.trace("Closing case {} on the request of {}", caseOid, username);
            workflowEngine.closeCase(caseOid, username, result);
        } catch (RuntimeException e) {
            result.recordFatalError("Case couldn't be stopped: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void deleteCase(String caseOid, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_PROCESS_INSTANCE);
        result.addParam("instanceId", caseOid);

        try {
            workflowEngine.deleteCase(caseOid, parentResult);
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
			if (wfc == null || wfc.getCaseOid() == null) {
				return;
			}
			String caseOid = wfc.getCaseOid();
			if (wfc.getEndTimestamp() == null) {
				try {
					closeCase(caseOid, "task delete action", result);
				} catch (RuntimeException e) {
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't stop workflow process instance {} while processing task deletion event for task {}", e, caseOid, task);
				}
			}
			deleteCase(caseOid, result);
		} catch (RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't process task deletion event for task {}", e, task);
		}
	}

	static class Statistics {
		int cases = 0;
		int openCasesWithNonExistingTaskOid = 0;
		int casesWithNonWorkflowTaskOid = 0;
		int casesWithWrongWorkflowTaskOid = 0;
		int casesWithoutTaskOid = 0;
		int casesRemoved = 0;
		int wrongCasesRemaining = 0;
		int tasks = 0;
		int tasksWithNonExistingCaseOids = 0;
		int tasksWithNonWfCases = 0;
		int tasksWithWrongCases = 0;
		int tasksRemoved = 0;
		int wrongTasksRemaining = 0;
	}

	public void synchronizeWorkflowRequests(OperationResult parentResult) {
		OperationResult result = parentResult.createSubresult(OPERATION_SYNCHRONIZE_WORKFLOW_REQUESTS);
		try {
			LOGGER.info("Starting synchronization of workflow requests between repository and Activiti");
			final Set<String> activeProcessInstances = new HashSet<>();
			final Map<String,String> casesToTasks = getCasesToTasks(activeProcessInstances, result);
			final Map<String,String> tasksToCases = getTasksToCases(result);
			Statistics s = new Statistics();
			doPhase1(casesToTasks, tasksToCases, activeProcessInstances, s, result);
			doPhase2(casesToTasks, tasksToCases, s, result);
			String message = s.cases + " wf cases found; out of these, removed " + s.casesRemoved + " ones; "
					+ "remaining " + s.wrongCasesRemaining + " wrong ones. " +
					s.tasks + " tasks found; out of these, removed " + s.tasksRemoved + " ones; "
					+ "remaining " + s.wrongTasksRemaining + " wrong ones.";
			result.recordStatus(OperationResultStatus.SUCCESS, message);
		} catch (RuntimeException|SchemaException e) {
			result.recordFatalError("Workflow requests cannot be synchronized: " + e.getMessage());
		} finally {
			result.computeStatusIfUnknown();
			LOGGER.info("Synchronization of workflow requests between tasks and cases finished with the status of {}: {}",
					result.getStatus(), result.getMessage());
		}
	}

	private void doPhase1(Map<String, String> casesToTasks, Map<String, String> tasksToCases,
			Set<String> openCases, Statistics s, OperationResult result) {
		s.cases = casesToTasks.size();
		final Iterator<Map.Entry<String,String>> iterator = casesToTasks.entrySet().iterator();

		while (iterator.hasNext()) {
			final Map.Entry<String,String> entry = iterator.next();
			final String caseOid = entry.getKey();
			final String taskOid = entry.getValue();
			if (taskOid != null) {
				if (!tasksToCases.containsKey(taskOid)) {
					LOGGER.warn("Open case {} points to non-existing task OID {} -- deleting it", caseOid, taskOid);
					deleteCaseChecked(caseOid, openCases, s, result);
					iterator.remove();
					s.openCasesWithNonExistingTaskOid++;
				} else {
					String caseOid2 = tasksToCases.get(taskOid);
					if (caseOid2 == null) {
						LOGGER.warn("Open case {} points to non-workflow task OID {} -- deleting it", caseOid, taskOid);
						deleteCaseChecked(caseOid, openCases, s, result);
						iterator.remove();
						s.casesWithNonWorkflowTaskOid++;
					} else if (!caseOid2.equals(caseOid)) {
						LOGGER.error(
								"Case {} points to task OID {} that points back to different case: {} -- please resolve manually",
								caseOid, taskOid, caseOid2);
						s.casesWithWrongWorkflowTaskOid++;
						s.wrongCasesRemaining++;
					}
				}
			} else {
				// probably no problem - this is a case with no midPoint task attached
				LOGGER.trace("Case with no midPoint task attached: {}", caseOid);
				s.casesWithoutTaskOid++;
			}
		}
		LOGGER.info("Results of phase 1:\n"
				+ "- cases with non-existing task OID: {}\n"
				+ "- cases with non-workflow task OID: {}\n"
				+ "- cases with wrong task OID (of such that points to other case): {}\n"
				+ "- cases with no task OID: {}\n"
				+ "- successfully deleted cases: {}", s.openCasesWithNonExistingTaskOid, s.casesWithNonWorkflowTaskOid,
				s.casesWithWrongWorkflowTaskOid, s.casesWithoutTaskOid, s.casesRemoved);
	}

	private void doPhase2(Map<String, String> casesToTasks, Map<String, String> tasksToCases, Statistics s, OperationResult result) {
		s.tasks = tasksToCases.size();
		final Iterator<Map.Entry<String,String>> iterator = tasksToCases.entrySet().iterator();

		while (iterator.hasNext()) {
			final Map.Entry<String,String> entry = iterator.next();
			final String taskOid = entry.getKey();
			final String caseOid = entry.getValue();
			if (caseOid == null) {
				// not a workflow-enabled task
				continue;
			}
			if (!casesToTasks.containsKey(caseOid)) {
				LOGGER.warn("Task {} points to non-existing case OID {} -- deleting it", taskOid, caseOid);
				deleteTaskChecked(taskOid, s, result);
				iterator.remove();
				s.tasksWithNonExistingCaseOids++;
				continue;
			}
			String taskOid2 = casesToTasks.get(caseOid);
			if (taskOid2 == null) {
				LOGGER.warn("Task {} points to non-wf case {} -- deleting it", caseOid, taskOid);
				deleteTaskChecked(taskOid, s, result);
				iterator.remove();
				s.tasksWithNonWfCases++;
				continue;
			}
			if (!taskOid2.equals(taskOid)) {
				LOGGER.warn("Task {} points to case {} that points back to different task -- please resolve manually", caseOid, taskOid);
				s.tasksWithWrongCases++;
				s.wrongTasksRemaining++;
			}
		}
		LOGGER.info("Results of phase 2:\n"
						+ "- tasks with non-existing cases: {}\n"
						+ "- tasks with non-wf cases: {}\n"
						+ "- tasks with wrong case OID (such that points to other task): {}\n"
				        + "- successfully deleted tasks: {}",
						s.tasksWithNonExistingCaseOids, s.tasksWithNonWfCases, s.tasksWithWrongCases,
						s.tasksRemoved);
	}

	private void deleteCaseChecked(String caseOid, Set<String> activeProcessInstances, Statistics s, OperationResult result) {
		try {
			if (activeProcessInstances.contains(caseOid)) {
				try {
					closeCase(caseOid, "workflow requests synchronization process", result);
				} catch (RuntimeException e) {
					LoggingUtils.logUnexpectedException(LOGGER, "Couldn't close case {}", e, caseOid);
				}
			}
			deleteCase(caseOid, result);
			s.casesRemoved++;
		} catch (RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't remove case {}", e, caseOid);
			s.wrongCasesRemaining++;
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

	private Map<String, String> getTasksToCases(OperationResult result) throws SchemaException {
		final Map<String,String> rv = new HashMap<>();
		final List<PrismObject<TaskType>> tasks = taskManager.searchObjects(TaskType.class, null, null, result);
		int tasksWithCaseOids = 0;
		for (PrismObject<TaskType> taskObject : tasks) {
			final TaskType task = taskObject.asObjectable();
			final WfContextType wfc = task.getWorkflowContext();
			final String caseOid = wfc != null ? wfc.getCaseOid() : null;
			rv.put(task.getOid(), caseOid);
			if (caseOid != null) {
				tasksWithCaseOids++;
			}
		}
		LOGGER.info("Found {} tasks; among these, {} have a pointer to case OID", rv.size(), tasksWithCaseOids);
		return rv;
	}

	private Map<String, String> getCasesToTasks(Set<String> activeCaseOids, OperationResult result) throws SchemaException {
		Map<String,String> rv = new HashMap<>();
		SearchResultList<PrismObject<CaseType>> cases = repositoryService.searchObjects(CaseType.class, null, null, result);
		for (PrismObject<CaseType> aCase : cases) {
			if (SchemaConstants.CASE_STATE_OPEN.equals(aCase.asObjectable().getState())) {
				String taskOid = WorkflowEngine.getTaskOidFromCaseName(aCase.getName().getOrig());
				if (taskOid != null) {
					activeCaseOids.add(aCase.getOid());
					rv.put(aCase.getOid(), taskOid);
				}
			}
		}
		LOGGER.info("Found {} open wf-related cases: {}", rv.size(), activeCaseOids.size());
		return rv;
	}

	public void cleanupWfCases(OperationResult result) throws SchemaException {
		LOGGER.info("Starting cleanup of workflow-related cases");
		Collection<String> casesToKeep = getCasesToKeep(result);
		LOGGER.info("Workflow-related cases to keep: {}", casesToKeep);

		SearchResultList<PrismObject<CaseType>> allCases = repositoryService.searchObjects(CaseType.class, null, null, result);
		LOGGER.info("All cases: {}", allCases.size());
		int ok = 0, fail = 0;
		for (PrismObject<CaseType> aCase : allCases) {
			String caseOid = aCase.getOid();
			if (casesToKeep.contains(caseOid) || WorkflowEngine.getTaskOidFromCaseName(aCase.asObjectable().getName().getOrig()) == null) {
				continue;
			}
			LOGGER.debug("Deleting case {}", aCase);
			try {
				deleteCase(caseOid, result);
				ok++;
			} catch (Throwable t) {
				LOGGER.error("Couldn't delete case {}", caseOid, t);
				fail++;
			}
		}
		String message = "Successfully deleted "+ok+" workflow-related cases; failed "+fail+" times";
		LOGGER.info(message);
		result.recordStatus(fail > 0 ? OperationResultStatus.PARTIAL_ERROR : OperationResultStatus.SUCCESS, message);
	}

	private Set<String> getCasesToKeep(OperationResult result) throws SchemaException {
		ObjectQuery query = prismContext.queryFor(TaskType.class)
				.not().item(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_CASE_OID).isNull()
				.build();
		SearchResultList<PrismObject<TaskType>> tasks = taskManager.searchObjects(TaskType.class, query, null, result);
		return tasks.stream()
				.map(t -> t.asObjectable().getWorkflowContext().getCaseOid())
				.collect(Collectors.toSet());
	}

}
