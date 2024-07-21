/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.remediation;

import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.repo.common.activity.run.*;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.schema.statistics.Operation;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;

import java.util.List;

/**
 * Execution of a certification campaign remediation.
 */
public final class AccessCertificationRemediationRun
        extends LocalActivityRun
        <AccessCertificationRemediationWorkDefinition,
                AccessCertificationRemediationActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationRemediationRun.class);

    private static final String CLASS_DOT = AccessCertificationRemediationRun.class.getName() + ".";

    AccessCertificationRemediationRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationRemediationWorkDefinition, AccessCertificationRemediationActivityHandler> context) {
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
        @NotNull AccessCertificationRemediationActivityHandler handler = getActivityHandler();

        LOGGER.trace("Task run starting");

        OperationResult opResult = result.createSubresult(CLASS_DOT + "run");
        opResult.setSummarizeSuccesses(true);
        OperationResult runResult = result.createSubresult("Campaign remediation");

        String campaignOid = getWorkDefinition().getCertificationCampaignRef().getOid();
        if (campaignOid == null) {
            LOGGER.error("No campaign OID specified in the task");
            opResult.recordFatalError("No campaign OID specified in the task");
            runResult.setStatus(OperationResultStatus.FATAL_ERROR);
            return standardRunResult(runResult.getStatus());
        }

        opResult.addContext("campaignOid", campaignOid);

        try {
            AccessCertificationCampaignType campaign = handler.getHelper().getCampaign(campaignOid, null, getRunningTask(), opResult);
            if (!CertCampaignTypeUtil.isRemediationAutomatic(campaign)) {
                LOGGER.error("Automatic remediation is not configured.");
                opResult.recordFatalError("Automatic remediation is not configured.");
                runResult.setStatus(OperationResultStatus.FATAL_ERROR);
                return standardRunResult(runResult.getStatus());
            }

            CertificationHandler manager = handler.getCertificationManager().findCertificationHandler(campaign);

            int revokedOk = 0;
            int revokedError = 0;

            List<AccessCertificationCaseType> caseList =
                    handler.getQueryHelper().getAllCurrentIterationCases(campaignOid, norm(campaign.getIteration()), opResult);

            activityState.getLiveProgress().setExpectedTotal(caseList.size());
            activityState.updateProgressNoCommit();
            activityState.flushPendingTaskModifications(opResult);

            for (AccessCertificationCaseType acase : caseList) {
                final Long caseId = acase.asPrismContainerValue().getId();
                String caseName = handler.getLocalizationService().translate(
                        "AccessCertificationRemediationRun.case",
                        new Object[]{caseId},
                        handler.getLocalizationService().getDefaultLocale());
                IterativeOperationStartInfo startInfo = new IterativeOperationStartInfo(
                        new IterationItemInformation(String.valueOf(caseId), caseName, AccessCertificationCaseType.COMPLEX_TYPE, null));
                startInfo.setSimpleCaller(true);
                Operation op = recordIterativeOperationStart(startInfo);
                if (OutcomeUtils.isRevoke(acase, campaign)) {
                    OperationResult caseResult = opResult.createMinorSubresult(opResult.getOperation()+".revoke");
                    caseResult.addContext("caseId", caseId);
                    try {
                        manager.doRevoke(acase, campaign, getRunningTask(), caseResult);
                        handler.getCaseHelper().markCaseAsRemedied(campaignOid, caseId, getRunningTask(), caseResult);
                        caseResult.computeStatus();
                        revokedOk++;
                        getRunningTask().incrementLegacyProgressAndStoreStatisticsIfTimePassed(opResult);
                        op.succeeded();
                    } catch (CommonException | RuntimeException e) {
                        String message = "Couldn't revoke case " + caseId + ": " + e.getMessage();
                        LoggingUtils.logUnexpectedException(LOGGER, message, e);
                        caseResult.recordPartialError(message, e);
                        revokedError++;
                        op.failed(e);
                    }
                    opResult.summarize();
                } else {
                    op.skipped();
                }
            }
            opResult.computeStatus();
            runResult.createSubresult(CLASS_DOT+"run.statistics")
                    .recordStatus(OperationResultStatus.NOT_APPLICABLE, "Successfully revoked items: "+revokedOk+", tried to revoke but failed: "+revokedError);

            handler.getCertificationManager().closeCampaign(campaignOid, getRunningTask(), opResult);

            runResult.setStatus(OperationResultStatus.SUCCESS);
            LOGGER.trace("Task run stopping (campaign {})", ObjectTypeUtil.toShortString(campaign));
            return standardRunResult(runResult.getStatus());

        } catch (Exception e) {     // TODO better error handling
            LoggingUtils.logException(LOGGER, "Error while executing remediation task handler", e);
            opResult.recordFatalError("Error while executing remediation task handler: "+e.getMessage(), e);
            runResult.setStatus(OperationResultStatus.FATAL_ERROR);
            return standardRunResult(runResult.getStatus());
        }
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return getWorkDefinition().getCertificationCampaignRef();
    }
}
