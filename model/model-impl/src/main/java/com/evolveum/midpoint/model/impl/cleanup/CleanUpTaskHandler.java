/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.model.impl.cleanup;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Component
public class CleanUpTaskHandler implements TaskHandler {

	public static final String HANDLER_URI = ModelPublicConstants.CLEANUP_TASK_HANDLER_URI;

    @Autowired private TaskManager taskManager;
	@Autowired private RepositoryService repositoryService;
    @Autowired private AuditService auditService;

    @Autowired(required = false)
    private ReportManager reportManager;
	
	private static final transient Trace LOGGER = TraceManager.getTrace(CleanUpTaskHandler.class);

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	@NotNull
	@Override
	public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
		return new StatisticsCollectionStrategy()
				.fromZero()
				.maintainIterationStatistics();
	}

	@Override
	public TaskRunResult run(Task task) {
		LOGGER.trace("CleanUpTaskHandler.run starting");
		
		OperationResult opResult = new OperationResult(OperationConstants.CLEANUP);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);

		CleanupPoliciesType cleanupPolicies = task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_CLEANUP_POLICIES);

		if (cleanupPolicies != null) {
			LOGGER.info("Using task-specific cleanupPolicies: {}", cleanupPolicies);
		} else {
			PrismObject<SystemConfigurationType> systemConfig;
			try {
				systemConfig = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, opResult);
			} catch (ObjectNotFoundException ex) {
				LOGGER.error("Cleanup: Object does not exist: {}", ex.getMessage(), ex);
				opResult.recordFatalError("Object does not exist: " + ex.getMessage(), ex);
				runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
				return runResult;
			} catch (SchemaException ex) {
				LOGGER.error("Cleanup: Error dealing with schema: {}", ex.getMessage(), ex);
				opResult.recordFatalError("Error dealing with schema: " + ex.getMessage(), ex);
				runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
				return runResult;
			}
			SystemConfigurationType systemConfigType = systemConfig.asObjectable();
			cleanupPolicies = systemConfigType.getCleanupPolicy();
		}
		
		if (cleanupPolicies == null) {
			LOGGER.trace("Cleanup: No clean up polices specified. Finishing clean up task.");
			opResult.computeStatus();
			runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
			return runResult;
		}
		
		CleanupPolicyType auditCleanupPolicy = cleanupPolicies.getAuditRecords();
		if (auditCleanupPolicy != null) {
			try {
				// TODO report progress
				auditService.cleanupAudit(auditCleanupPolicy, opResult);
			} catch (Exception ex) {
				LOGGER.error("Cleanup: {}", ex.getMessage(), ex);
				opResult.recordFatalError(ex.getMessage(), ex);
				runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			}
		} else {
			LOGGER.trace("Cleanup: No clean up policy for audit specified. Finishing clean up task.");
		}
		
		CleanupPolicyType closedTasksPolicy = cleanupPolicies.getClosedTasks();
		if (closedTasksPolicy != null) {
			try {
				taskManager.cleanupTasks(closedTasksPolicy, task, opResult);
			} catch (Exception ex) {
				LOGGER.error("Cleanup: {}", ex.getMessage(), ex);
				opResult.recordFatalError(ex.getMessage(), ex);
				runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			}
		} else {
			LOGGER.trace("Cleanup: No clean up policy for closed tasks specified. Finishing clean up task.");
		}
		
		CleanupPolicyType reportCleanupPolicy = cleanupPolicies.getOutputReports();
		if (reportCleanupPolicy != null) {
			try {
                if (reportManager == null) {
                    //TODO improve dependencies for report-impl (probably for tests) and set autowire to required
                    LOGGER.error("Report manager was not autowired, reports cleanup will be skipped.");
                } else {
	                // TODO report progress
				    reportManager.cleanupReports(reportCleanupPolicy, opResult);
                }
			} catch (Exception ex) {
				LOGGER.error("Cleanup: {}", ex.getMessage(), ex);
				opResult.recordFatalError(ex.getMessage(), ex);
				runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			}
		} else {
			LOGGER.trace("Cleanup: No clean up policy for report specified. Finishing clean up task.");
		}
		opResult.computeStatus();
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		LOGGER.trace("CleanUpTaskHandler.run stopping");
		return runResult;
	}

	@Override
	public Long heartbeat(Task task) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void refreshStatus(Task task) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getCategoryName(Task task) {
		if (task != null && task.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_CLEANUP_POLICIES) != null) {
			return TaskCategory.UTIL;			// this is run on-demand just like other utility tasks (e.g. delete task handler)
		} else {
			return TaskCategory.SYSTEM;			// this is the default instance, always running
		}
	}

	@Override
	public List<String> getCategoryNames() {
		return Arrays.asList(TaskCategory.UTIL, TaskCategory.SYSTEM);
	}

}
