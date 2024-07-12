/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.campaignCreation;

import com.evolveum.midpoint.certification.impl.AccCertOpenerHelper;
import com.evolveum.midpoint.certification.impl.AccessCertificationConstants;
import com.evolveum.midpoint.certification.impl.CertificationManagerImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * The task handler for automatic campaign start.
 */
@Component
public class AccessCertificationCampaignCreationTaskHandler implements TaskHandler {

    private static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TASK_PREFIX + "/campaign-creation/handler-3";
    private static final String CLASS_DOT = AccessCertificationCampaignCreationTaskHandler.class.getName() + ".";

    @Autowired private TaskManager taskManager;
    @Autowired private CertificationManagerImpl certificationManager;
    @Autowired private RepositoryService repositoryService;
    @Autowired private AccCertOpenerHelper openerHelper;

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationCampaignCreationTaskHandler.class);

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @NotNull
    @Override
    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy()
                .fromStoredValues(); // Why from stored values?
    }

    @Override
    public TaskRunResult run(@NotNull RunningTask task) {
        LOGGER.trace("Task run starting");

        OperationResult opResult = task.getResult().createSubresult(CLASS_DOT+"run");
        opResult.setSummarizeSuccesses(true);

        String definitionOid = task.getObjectOid();
        if (definitionOid == null) {
            LOGGER.error("No definition OID specified in the task");
            opResult.recordFatalError("No definition OID specified in the task");
            return TaskRunResult.permanentFatalError();
        }

        opResult.addContext("definitionOid", definitionOid);

        AccessCertificationCampaignType campaign;
        try {
            LOGGER.debug("Creating campaign with definition of {}", definitionOid);
            PrismObject<AccessCertificationDefinitionType> definition =
                    repositoryService.getObject(AccessCertificationDefinitionType.class, definitionOid, null, opResult);
            campaign = openerHelper.createCampaign(definition, opResult, task);
            LOGGER.info("Campaign {} was created.", ObjectTypeUtil.toShortString(campaign));
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Error while executing 'create campaign' task handler", e);
            opResult.recordFatalError("Error while executing 'create campaign' task handler: " + e.getMessage(), e);
            return TaskRunResult.permanentFatalError();
        }

        Operation op = task.recordIterativeOperationStart(campaign.asPrismObject());
        try {
            certificationManager.openNextStage(campaign.getOid(), task, opResult);
            LOGGER.info("Campaign {} was started.", ObjectTypeUtil.toShortString(campaign));

            op.succeeded();
            opResult.computeStatus();
            TaskRunResult runResult = new TaskRunResult();
            runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
            runResult.setOperationResultStatus(OperationResultStatus.SUCCESS);
            runResult.setProgress(task.getLegacyProgress()+1);
            return runResult;

        } catch (CommonException | RuntimeException e) {
            op.failed(e);
            LoggingUtils.logException(LOGGER, "Error while executing 'create campaign' task handler", e);
            opResult.recordFatalError("Error while executing 'create campaign' task handler: "+e.getMessage(), e);
            return TaskRunResult.permanentFatalError();
        }
    }
}
