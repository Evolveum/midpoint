/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.impl.task.remediation;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.certification.impl.*;
import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommonException;
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

import jakarta.annotation.PostConstruct;
import java.util.List;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;

/**
 * The task handler for automatic remediation.
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

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationRemediationTaskHandler.class);

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public CertificationManagerImpl getCertificationManager() {
        return certificationManager;
    }

    public AccCertGeneralHelper getHelper() {
        return helper;
    }

    public AccCertCaseOperationsHelper getCaseHelper() {
        return caseHelper;
    }

    public AccCertQueryHelper getQueryHelper() {
        return queryHelper;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @Override
    public TaskRunResult run(@NotNull RunningTask task) {
        LOGGER.trace("Task run starting");

        OperationResult opResult = task.getResult().createSubresult(CLASS_DOT+"run");
        opResult.setSummarizeSuccesses(true);
        TaskRunResult runResult = new TaskRunResult();

        String campaignOid = task.getObjectOid();
        if (campaignOid == null) {
            LOGGER.error("No campaign OID specified in the task");
            opResult.recordFatalError("No campaign OID specified in the task");
            runResult.setOperationResultStatus(OperationResultStatus.FATAL_ERROR);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        opResult.addContext("campaignOid", campaignOid);

        try {
            AccessCertificationCampaignType campaign = helper.getCampaign(campaignOid, null, task, opResult);
            if (!CertCampaignTypeUtil.isRemediationAutomatic(campaign)) {
                LOGGER.error("Automatic remediation is not configured.");
                opResult.recordFatalError("Automatic remediation is not configured.");
                runResult.setOperationResultStatus(OperationResultStatus.FATAL_ERROR);
                runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
                return runResult;
            }

            CertificationHandler handler = certificationManager.findCertificationHandler(campaign);

            int revokedOk = 0;
            int revokedError = 0;

            List<AccessCertificationCaseType> caseList =
                    queryHelper.getAllCurrentIterationCases(campaignOid, norm(campaign.getIteration()), opResult);
            for (AccessCertificationCaseType acase : caseList) {
                if (OutcomeUtils.isRevoke(acase, campaign)) {
                    OperationResult caseResult = opResult.createMinorSubresult(opResult.getOperation()+".revoke");
                    final Long caseId = acase.asPrismContainerValue().getId();
                    caseResult.addContext("caseId", caseId);
                    try {
                        handler.doRevoke(acase, campaign, task, caseResult);
                        caseHelper.markCaseAsRemedied(campaignOid, caseId, task, caseResult);
                        caseResult.computeStatus();
                        revokedOk++;
                        task.incrementLegacyProgressAndStoreStatisticsIfTimePassed(opResult);
                    } catch (CommonException | RuntimeException e) {
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

            runResult.setOperationResultStatus(OperationResultStatus.SUCCESS);
            runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
            LOGGER.trace("Task run stopping (campaign {})", ObjectTypeUtil.toShortString(campaign));
            return runResult;

        } catch (Exception e) {     // TODO better error handling
            LoggingUtils.logException(LOGGER, "Error while executing remediation task handler", e);
            opResult.recordFatalError("Error while executing remediation task handler: "+e.getMessage(), e);
            runResult.setOperationResultStatus(OperationResultStatus.FATAL_ERROR);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }
    }

    @Override
    public Long heartbeat(Task task) {
        return null;    // not to reset progress information
    }

    @Override
    public void refreshStatus(Task task) {
        // Do nothing. Everything is fresh already.
    }

    @Override
    public String getDefaultChannel() {
        return Channel.REMEDIATION.getUri();
    }
}
