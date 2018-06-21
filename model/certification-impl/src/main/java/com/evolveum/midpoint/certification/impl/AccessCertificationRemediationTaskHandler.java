/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * The task handler for automatic remediation.
 *
 * @author mederly
 */
@Component
public class AccessCertificationRemediationTaskHandler implements TaskHandler {

	private static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TASK_PREFIX + "/remediation/handler-3";

    private static final String CLASS_DOT = AccessCertificationRemediationTaskHandler.class.getName() + ".";

    @Autowired private TaskManager taskManager;
    @Autowired private CertificationManagerImpl certificationManager;
    @Autowired private AccCertGeneralHelper helper;
    @Autowired private AccCertCaseOperationsHelper caseHelper;
    @Autowired private AccCertQueryHelper queryHelper;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    private static final transient Trace LOGGER = TraceManager.getTrace(AccessCertificationRemediationTaskHandler.class);

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	@NotNull
	@Override
	public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
		return new StatisticsCollectionStrategy()
				.fromZero();
		// implement iteration stats when needed
	}

	@Override
	public TaskRunResult run(Task task) {
		LOGGER.trace("Task run starting");

		OperationResult opResult = new OperationResult(CLASS_DOT+"run");
        opResult.setSummarizeSuccesses(true);
		TaskRunResult runResult = new TaskRunResult();
		runResult.setOperationResult(opResult);

		if (task.getChannel() == null) {
			task.setChannel(SchemaConstants.CHANNEL_REMEDIATION_URI);
		}

        String campaignOid = task.getObjectOid();
        if (campaignOid == null) {
            LOGGER.error("No campaign OID specified in the task");
            opResult.recordFatalError("No campaign OID specified in the task");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        opResult.addContext("campaignOid", campaignOid);

        try {
            AccessCertificationCampaignType campaign = helper.getCampaign(campaignOid, null, task, opResult);
            if (!CertCampaignTypeUtil.isRemediationAutomatic(campaign)) {
                LOGGER.error("Automatic remediation is not configured.");
                opResult.recordFatalError("Automatic remediation is not configured.");
                runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                return runResult;
            }

            CertificationHandler handler = certificationManager.findCertificationHandler(campaign);

            int revokedOk = 0;
            int revokedError = 0;

            List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, opResult);
            for (AccessCertificationCaseType _case : caseList) {
                if (helper.isRevoke(_case, campaign)) {
                    OperationResult caseResult = opResult.createMinorSubresult(opResult.getOperation()+".revoke");
                    final Long caseId = _case.asPrismContainerValue().getId();
                    caseResult.addContext("caseId", caseId);
                    try {
                        handler.doRevoke(_case, campaign, task, caseResult);
                        caseHelper.markCaseAsRemedied(campaignOid, caseId, task, caseResult);
                        caseResult.computeStatus();
                        revokedOk++;
						task.incrementProgressAndStoreStatsIfNeeded();
                    } catch (Exception e) {     // TODO
                        String message = "Couldn't revoke case " + caseId + ": " + e.getMessage();
                        LoggingUtils.logUnexpectedException(LOGGER, message, e);
                        caseResult.recordPartialError(message, e);
                        revokedError++;
                    }
                    opResult.summarize();
                }
            }
            opResult.createSubresult(CLASS_DOT+"run.statistics")
                    .recordStatus(OperationResultStatus.NOT_APPLICABLE, "Successfully revoked items: "+revokedOk+", tried to revoke but failed: "+revokedError);
            opResult.computeStatus();

            certificationManager.closeCampaign(campaignOid, task, opResult);

            runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
            LOGGER.trace("Task run stopping (campaign {})", ObjectTypeUtil.toShortString(campaign));
            return runResult;

        } catch (Exception e) {     // TODO better error handling
            LoggingUtils.logException(LOGGER, "Error while executing remediation task handler", e);
            opResult.recordFatalError("Error while executing remediation task handler: "+e.getMessage(), e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }
	}

	@Override
	public Long heartbeat(Task task) {
		return null;	// not to reset progress information
	}

	@Override
	public void refreshStatus(Task task) {
		// Do nothing. Everything is fresh already.
	}

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.ACCESS_CERTIFICATION;
    }

    public void launch(AccessCertificationCampaignType campaign, OperationResult parentResult) throws SchemaException, ObjectNotFoundException {

        LOGGER.info("Launching remediation task handler for campaign {} as asynchronous task", ObjectTypeUtil.toShortString(campaign));

        OperationResult result = parentResult.createSubresult(CLASS_DOT + "launch");
        result.addParam("campaignOid", campaign.getOid());

        Task task = taskManager.createTaskInstance();

        // Set handler URI so we will be called back
        task.setHandlerUri(HANDLER_URI);

        // Readable task name
        PolyStringType polyString = new PolyStringType("Remediation for " + campaign.getName());
        task.setName(polyString);

        // Set reference to the resource
        task.setObjectRef(ObjectTypeUtil.createObjectRef(campaign));

        task.setOwner(repositoryService.getObject(UserType.class, SystemObjectsType.USER_ADMINISTRATOR.value(), null, result));

        taskManager.switchToBackground(task, result);
		result.setBackgroundTaskOid(task.getOid());
        if (result.isInProgress()) {
            result.recordStatus(OperationResultStatus.IN_PROGRESS, "Remediation task "+task+" was successfully started, please use Server Tasks to see its status.");
        }

        LOGGER.trace("Remediation for {} switched to background, control thread returning with task {}", ObjectTypeUtil.toShortString(campaign), task);
    }

}
