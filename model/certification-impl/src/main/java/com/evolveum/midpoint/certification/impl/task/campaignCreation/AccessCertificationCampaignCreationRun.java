/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.campaignCreation;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.schema.util.ObjectReferenceTypeUtil;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.TaskRunResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Execution of a certification campaign creation.
 */
public final class AccessCertificationCampaignCreationRun
        extends LocalActivityRun
        <AccessCertificationCampaignCreationWorkDefinition,
                AccessCertificationCampaignCreationActivityHandler,
                CertificationCampaignCreationWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationCampaignCreationRun.class);

    private static final String CLASS_DOT = AccessCertificationCampaignCreationRun.class.getName() + ".";

    AccessCertificationCampaignCreationRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationCampaignCreationWorkDefinition, AccessCertificationCampaignCreationActivityHandler> context) {
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
        @NotNull AccessCertificationCampaignCreationActivityHandler handler = getActivityHandler();

        LOGGER.trace("Task run starting");

        OperationResult runResult = result.createSubresult(CLASS_DOT + "run");

        String definitionOid = getWorkDefinition().getCertificationCampaignDefRef().getOid();
        if (definitionOid == null) {
            LOGGER.error("No definition OID specified in the task");
            runResult.recordFatalError("No definition OID specified in the task");
            return standardRunResult(runResult.getStatus());
        }

        runResult.addContext("definitionOid", definitionOid);

        AccessCertificationCampaignType campaign;
        try {
            LOGGER.debug("Creating campaign with definition of {}", definitionOid);
            try {
                PrismObject<AccessCertificationDefinitionType> definition =
                        handler.getRepositoryService().getObject(AccessCertificationDefinitionType.class, definitionOid, null, result);
                campaign = handler.getOpenerHelper().createCampaign(definition, result, getRunningTask());
            } catch (RuntimeException e) {
                result.recordFatalError("Couldn't create certification campaign: unexpected exception: " + e.getMessage(), e);
                throw e;
            } finally {
                result.computeStatusIfUnknown();
            }

            LOGGER.info("Campaign {} was created.", ObjectTypeUtil.toShortString(campaign));
            getActivityState().setWorkStateItemRealValues(
                    CertificationCampaignCreationWorkStateType.F_CREATED_CAMPAIGN_REF,
                    ObjectTypeUtil.createObjectRef(campaign));

            Collection<? extends ItemDelta<?, ?>> deltas = handler.getTaskOperationalDataManager().getModifyDeltaForAffectedObjects(
                    getRunningTask().getRawTaskObjectClone().asObjectable(),
                    result);
            getRunningTask().modify((Collection<ItemDelta<?, ?>>) deltas);

            getRunningTask().flushPendingModifications(result);

            runResult.computeStatus();
            runResult.setStatus(OperationResultStatus.SUCCESS);
            LOGGER.trace("Task run stopping (campaign {})", ObjectTypeUtil.toShortString(campaign));
            return standardRunResult(runResult.getStatus());

        } catch (Exception e) {
            LoggingUtils.logException(LOGGER, "Error while creating campaign", e);
            runResult.recordFatalError("Error while creating campaign, error: " + e.getMessage(), e);
            return standardRunResult(runResult.getStatus());
        }
    }
}
