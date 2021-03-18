/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation.Operation;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * The task handler for automatic campaign start.
 *
 * @author mederly
 */
@Component
public class AccessCertificationCampaignCreationTaskHandler implements TaskHandler {

    private static final String HANDLER_URI = AccessCertificationConstants.NS_CERTIFICATION_TASK_PREFIX + "/campaign-creation/handler-3";
    private static final String CLASS_DOT = AccessCertificationCampaignCreationTaskHandler.class.getName() + ".";

    @Autowired private TaskManager taskManager;
    @Autowired private CertificationManagerImpl certificationManager;

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationCampaignCreationTaskHandler.class);

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(HANDLER_URI, this);
    }

    @NotNull
    @Override
    public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
        return new StatisticsCollectionStrategy()
                .fromStoredValues();
    }

    @Override
    public TaskRunResult run(RunningTask task, TaskPartitionDefinitionType partition) {
        LOGGER.trace("Task run starting");

        OperationResult opResult = new OperationResult(CLASS_DOT+"run");
        opResult.setSummarizeSuccesses(true);
        TaskRunResult runResult = new TaskRunResult();
        runResult.setOperationResult(opResult);

        String definitionOid = task.getObjectOid();
        if (definitionOid == null) {
            LOGGER.error("No definition OID specified in the task");
            opResult.recordFatalError("No definition OID specified in the task");
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        opResult.addContext("definitionOid", definitionOid);

        AccessCertificationCampaignType campaign;
        try {
            LOGGER.info("Creating campaign with definition of {}", definitionOid);
            campaign = certificationManager.createCampaign(definitionOid, task, opResult);
            LOGGER.info("Campaign {} was created.", ObjectTypeUtil.toShortString(campaign));
        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Error while executing 'create campaign' task handler", e);
            opResult.recordFatalError("Error while executing 'create campaign' task handler: " + e.getMessage(), e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }

        Operation op = task.recordIterativeOperationStart(campaign.asPrismObject());
        try {
            certificationManager.openNextStage(campaign.getOid(), task, opResult);
            LOGGER.info("Campaign {} was started.", ObjectTypeUtil.toShortString(campaign));

            op.succeeded();
            opResult.computeStatus();
            runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
            runResult.setProgress(task.getProgress()+1);
            return runResult;

        } catch (CommonException | RuntimeException e) {
            op.failed(e);
            LoggingUtils.logException(LOGGER, "Error while executing 'create campaign' task handler", e);
            opResult.recordFatalError("Error while executing 'create campaign' task handler: "+e.getMessage(), e);
            runResult.setRunResultStatus(TaskRunResultStatus.PERMANENT_ERROR);
            return runResult;
        }
    }

    @Override
    public String getCategoryName(Task task) {
        return TaskCategory.ACCESS_CERTIFICATION;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_CERTIFICATION_TASK.value();
    }
}
