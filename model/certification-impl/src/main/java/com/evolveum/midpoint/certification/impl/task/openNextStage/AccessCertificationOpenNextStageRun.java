/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.openNextStage;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.toUri;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.certification.impl.AccCertResponseComputationHelper;
import com.evolveum.midpoint.certification.impl.task.AccessCertificationStageManagementRun;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Execution of a certification campaign creation.
 */
public final class AccessCertificationOpenNextStageRun
        extends AccessCertificationStageManagementRun
        <AccessCertificationCaseType, AccessCertificationOpenNextStageWorkDefinition,
                        AccessCertificationOpenNextStageActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationOpenNextStageRun.class);

    AccessCertificationOpenNextStageRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationOpenNextStageWorkDefinition, AccessCertificationOpenNextStageActivityHandler> context) {
        super(context, "");
        setInstanceReady();
    }


    @Override
    public boolean processItem(@NotNull AccessCertificationCaseType item, @NotNull ItemProcessingRequest<AccessCertificationCaseType> request, RunningTask workerTask, OperationResult result) throws CommonException {
        LOGGER.trace("----------------------------------------------------------------------------------------");
        LOGGER.trace("Considering case: {}", item);
        Long caseId = item.asPrismContainerValue().getId();
        assert caseId != null;
        if (item.getReviewFinishedTimestamp() != null) {
            LOGGER.trace("Case {} review process has already finished", caseId);
            return true;
        }
        AccCertResponseComputationHelper computationHelper = getComputationHelper();
        AccessCertificationResponseType stageOutcome = computationHelper.getStageOutcome(item, getStageToBe());
        if (OutcomeUtils.normalizeToNull(stageOutcome) != null) {
            LOGGER.trace("Case {} already has an outcome for stage {} - it will not be reviewed in this stage in iteration {}",
                    caseId, getStageToBe(), getIteration());
            return true;
        }

        List<ObjectReferenceType> reviewers = getReviewersHelper().getReviewersForCase(item, getCampaign(), getReviewerSpec(), getRunningTask(), result);
        List<AccessCertificationWorkItemType> workItems = createWorkItems(reviewers, getStageToBe(), getIteration(), item);

        item.getWorkItem().addAll(CloneUtil.cloneCollectionMembers(workItems));
        AccessCertificationResponseType currentStageOutcome = computationHelper.computeOutcomeForStage(item, getCampaign(), getStageToBe());
        AccessCertificationResponseType overallOutcome = computationHelper.computeOverallOutcome(item, getCampaign(), getStageToBe(), currentStageOutcome);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Computed: reviewers: {}, workItems: {}, currentStageOutcome: {}, overallOutcome: {}",
                    PrettyPrinter.prettyPrint(reviewers), workItems.size(), currentStageOutcome, overallOutcome);
        }
        List<ItemDelta<?, ?>> modifications = PrismContext.get().deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE, caseId, F_WORK_ITEM).add(PrismContainerValue.toPcvList(workItems))
                .item(F_CASE, caseId, F_CURRENT_STAGE_CREATE_TIMESTAMP).replace(getStage().getStartTimestamp())
                .item(F_CASE, caseId, F_CURRENT_STAGE_DEADLINE).replace(getStage().getDeadline())
                .item(F_CASE, caseId, F_CURRENT_STAGE_OUTCOME).replace(toUri(currentStageOutcome))
                .item(F_CASE, caseId, F_OUTCOME).replace(toUri(overallOutcome))
                .item(F_CASE, caseId, F_STAGE_NUMBER).replace(getStageToBe())
                .item(F_CASE, caseId, F_ITERATION).replace(getIteration())
                .asItemDeltas();

        getActivityHandler().getUpdateHelper().modifyObjectPreAuthorized(AccessCertificationCampaignType.class, getCampaign().getOid(), modifications, getRunningTask(), result);
        return true;
    }

    @NotNull
    protected ObjectQuery prepareObjectQuery() {
        return PrismContext.get().queryFor(AccessCertificationCaseType.class)
                .ownerId(getCampaign().getOid())
                .and().item(AccessCertificationCaseType.F_ITERATION).eq(getIteration())
                .build();
    }

    @Override
    protected Class<AccessCertificationCaseType> getType() {
        return AccessCertificationCaseType.class;
    }
}
