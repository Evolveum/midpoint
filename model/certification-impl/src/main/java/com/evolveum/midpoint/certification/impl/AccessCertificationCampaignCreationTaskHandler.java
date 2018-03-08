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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.task.api.TaskRunResult.TaskRunResultStatus;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

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

	private static final transient Trace LOGGER = TraceManager.getTrace(AccessCertificationCampaignCreationTaskHandler.class);

	@PostConstruct
	private void initialize() {
		taskManager.registerHandler(HANDLER_URI, this);
	}

	@NotNull
	@Override
	public StatisticsCollectionStrategy getStatisticsCollectionStrategy() {
		return new StatisticsCollectionStrategy()
				.fromStoredValues()
				.maintainIterationStatistics();
	}

	@Override
	public TaskRunResult run(Task task) {
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

        long started = 0;
        String campaignName = null;
        String campaignOid = null;
        try {
            LOGGER.info("Creating campaign with definition of {}", definitionOid);
            AccessCertificationCampaignType campaign = certificationManager.createCampaign(definitionOid, task, opResult);
            LOGGER.info("Campaign {} was created.", ObjectTypeUtil.toShortString(campaign));

            // TODO split this try-catch to two pieces in order to correctly work with iterative op failure recording
            started = System.currentTimeMillis();
            campaignName = campaign.getName().getOrig();
            campaignOid = campaign.getOid();
            task.recordIterativeOperationStart(campaignName, campaignName, AccessCertificationCampaignType.COMPLEX_TYPE, campaignOid);

            certificationManager.openNextStage(campaign.getOid(), 1, task, opResult);
            LOGGER.info("Campaign {} was started.", ObjectTypeUtil.toShortString(campaign));

            task.recordIterativeOperationEnd(campaignName, campaignName, AccessCertificationCampaignType.COMPLEX_TYPE, campaignOid, started, null);

            opResult.computeStatus();
            runResult.setRunResultStatus(TaskRunResultStatus.FINISHED);
            runResult.setProgress(task.getProgress()+1);
            return runResult;

        } catch (Exception e) {     // TODO better error handling
            if (campaignOid != null) {
                task.recordIterativeOperationEnd(campaignName, campaignName, AccessCertificationCampaignType.COMPLEX_TYPE, campaignOid, started, e);
            }
            LoggingUtils.logException(LOGGER, "Error while executing 'create campaign' task handler", e);
            opResult.recordFatalError("Error while executing 'create campaign' task handler: "+e.getMessage(), e);
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
}
