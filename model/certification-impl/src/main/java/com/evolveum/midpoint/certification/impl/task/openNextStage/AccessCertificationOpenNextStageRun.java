/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.openNextStage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Execution of a certification campaign creation.
 */
public final class AccessCertificationOpenNextStageRun
        extends LocalActivityRun
        <AccessCertificationOpenNextStageWorkDefinition,
                AccessCertificationOpenNextStageActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationOpenNextStageRun.class);

    AccessCertificationOpenNextStageRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationOpenNextStageWorkDefinition, AccessCertificationOpenNextStageActivityHandler> context) {
        super(context);
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true);
    }

    @Override
    protected @NotNull ActivityRunResult runLocally(OperationResult result) throws ActivityRunException, CommonException {
        @NotNull AccessCertificationOpenNextStageActivityHandler handler = getActivityHandler();

        LOGGER.trace("Task run starting");

        OperationResult runResult = result.createSubresult("Campaign next stage");

        String campaignOid = getWorkDefinition().getCertificationCampaignRef().getOid();
        if (campaignOid == null) {
            LOGGER.error("No campaign OID specified in the task");
            runResult.recordFatalError("No campaign OID specified in the task");
            return standardRunResult(runResult.getStatus());
        }

        runResult.addContext("campaignOid", campaignOid);

        LOGGER.info("opening campaign next stage for certification campaign {}.", campaignOid);

        try {

            handler.getCertificationManager().openNextStage(campaignOid, getRunningTask(), runResult);

            runResult.computeStatus();
            runResult.setStatus(OperationResultStatus.SUCCESS);
            LOGGER.trace("Task run stopping (campaign {})", campaignOid);
            return standardRunResult(runResult.getStatus());

        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Error while opening campaign next stage", e);
            runResult.recordFatalError("Error while opening campaign next stage, error: " + e.getMessage(), e);
            return standardRunResult(runResult.getStatus());
        }
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return getWorkDefinition().getCertificationCampaignRef();
    }
}
