/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.remediation;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Execution of a certification campaign remediation.
 */
public final class AccessCertificationRemediationRun
        extends SearchBasedActivityRun
        <AccessCertificationCaseType, AccessCertificationRemediationWorkDefinition,
                AccessCertificationRemediationActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationRemediationRun.class);


    private AccessCertificationCampaignType campaign;
    private int iteration;
    private ObjectQuery query;

    private CertificationHandler handler;

    AccessCertificationRemediationRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationRemediationWorkDefinition, AccessCertificationRemediationActivityHandler> context) {
        super(context, "");
        setInstanceReady();
    }


    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        String campaignOid = getWorkDefinition().getCertificationCampaignRef().getOid();
        campaign = getBeans().repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, null, result).asObjectable();

        iteration = norm(campaign.getIteration());
        query = prepareObjectQuery();


        if (!CertCampaignTypeUtil.isRemediationAutomatic(campaign)) { //TODO probably the task should not be started even?
            LOGGER.error("Automatic remediation is not configured.");
//            opResult.recordFatalError("Automatic remediation is not configured.");
//            result.setStatus(OperationResultStatus.FATAL_ERROR);
//            return standardRunResult(runResult.getStatus());
        }

        handler = getActivityHandler().getCertificationManager().findCertificationHandler(campaign);

        return super.beforeRun(result);
    }

    private ObjectQuery prepareObjectQuery() {
        return PrismContext.get().queryFor(AccessCertificationCaseType.class)
                .ownerId(campaign.getOid())
                .and().item(AccessCertificationCaseType.F_ITERATION).eq(iteration)
                .build();
    }



    @Override
    public boolean processItem(@NotNull AccessCertificationCaseType item, @NotNull ItemProcessingRequest<AccessCertificationCaseType> request, RunningTask workerTask, OperationResult result) throws CommonException, ActivityRunException {
        final Long caseId = item.asPrismContainerValue().getId();
        if (OutcomeUtils.isRevoke(item, campaign)) {
            OperationResult caseResult = result.createMinorSubresult(result.getOperation() + ".revoke");
            caseResult.addContext("caseId", caseId);
            try {
                handler.doRevoke(item, campaign, getRunningTask(), caseResult);
                getActivityHandler().getCaseHelper().markCaseAsRemedied(campaign.getOid(), caseId, getRunningTask(), caseResult);
                caseResult.computeStatus();
            } catch (CommonException | RuntimeException e) {
                String message = "Couldn't revoke case " + caseId + ": " + e.getMessage();
                LoggingUtils.logUnexpectedException(LOGGER, message, e);
                caseResult.recordPartialError(message, e);
            }
            result.summarize();
        }
        return true;
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return getWorkDefinition().getCertificationCampaignRef();
    }

    @Override
    public @Nullable SearchSpecification<AccessCertificationCaseType> createCustomSearchSpecification(OperationResult result) {
        return new SearchSpecification<>(AccessCertificationCaseType.class, query, null, false);
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException, ActivityRunException {
        getActivityHandler().getCertificationManager().closeCampaign(campaign.getOid(), getRunningTask(), result);
    }

}


