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

import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.report.api.ReportManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.ChangeExecutor;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.sync.RecomputeTaskHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.task.api.TaskHandler;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPoliciesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

@Component
public class CleanUpTaskHandler implements TaskHandler{

	public static final String HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/model/cleanup/handler-3";

    @Autowired(required=true)
	private TaskManager taskManager;
	
	@Autowired(required=true)
	private RepositoryService repositoryService;

    @Autowired(required=true)
    private AuditService auditService;

    @Autowired(required = false)
    private ReportManager reportManager;
	
	@Autowired(required=true)
	private PrismContext prismContext;
	
	
	private static final transient Trace LOGGER = TraceManager.getTrace(CleanUpTaskHandler.class);

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	
	@Override
	public TaskRunResult run(Task task) {
		LOGGER.trace("CleanUpTaskHandler.run starting");
		
		long progress = task.getProgress();
		OperationResult opResult = new OperationResult(OperationConstants.CLEANUP);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);
		
		PrismObject<SystemConfigurationType> systemConfig = null;
		try {
			systemConfig = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, opResult);
		} catch (ObjectNotFoundException ex) {
			LOGGER.error("Cleanup: Object does not exist: {}",ex.getMessage(),ex);
			opResult.recordFatalError("Object does not exist: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		} catch (SchemaException ex) {
			LOGGER.error("Cleanup: Error dealing with schema: {}",ex.getMessage(),ex);
			opResult.recordFatalError("Error dealing with schema: "+ex.getMessage(),ex);
			runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
			runResult.setProgress(progress);
			return runResult;
		}
		SystemConfigurationType systemConfigType = systemConfig.asObjectable();		
		
		CleanupPoliciesType cleanupPolicies = systemConfigType.getCleanupPolicy();
		
		if (cleanupPolicies == null){
			LOGGER.trace("Cleanup: No clean up polices specified. Finishing clean up task.");
			opResult.computeStatus();
			runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
			runResult.setProgress(progress);
			return runResult;
		}
		
		CleanupPolicyType auditCleanupPolicy = cleanupPolicies.getAuditRecords();
		if (auditCleanupPolicy != null) {
			try {
				auditService.cleanupAudit(auditCleanupPolicy, opResult);
			} catch (Exception ex) {
				LOGGER.error("Cleanup: {}", ex.getMessage(), ex);
				opResult.recordFatalError(ex.getMessage(), ex);
				runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
				runResult.setProgress(progress);
			}
		} else{
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
				runResult.setProgress(progress);
			}
		} else{
			LOGGER.trace("Cleanup: No clean up policy for closed tasks specified. Finishing clean up task.");
		}
		
		CleanupPolicyType reportCleanupPolicy = cleanupPolicies.getOutputReports();
		if (reportCleanupPolicy != null) {
			try {
                if (reportManager == null) {
                    //TODO improve dependencies for report-impl (probably for tests) and set autowire to required
                    LOGGER.error("Report manager was not autowired, reports cleanup will be skipped.");
                } else {
				    reportManager.cleanupReports(reportCleanupPolicy, opResult);
                }
			} catch (Exception ex) {
				LOGGER.error("Cleanup: {}", ex.getMessage(), ex);
				opResult.recordFatalError(ex.getMessage(), ex);
				runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
				runResult.setProgress(progress);
			}
		} else{
			LOGGER.trace("Cleanup: No clean up policy for report specified. Finishing clean up task.");
		}
		opResult.computeStatus();
		// This "run" is finished. But the task goes on ...
		runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
		runResult.setProgress(progress);
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
		 return TaskCategory.SYSTEM;
	}

	@Override
	public List<String> getCategoryNames() {
		// TODO Auto-generated method stub
		return null;
	}

}
