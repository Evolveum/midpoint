/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.closeCurrentStage;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.toUri;
import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CLOSE_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.REVIEW_STAGE_DONE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType.F_END_TIMESTAMP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.certification.impl.AccCertResponseComputationHelper;
import com.evolveum.midpoint.certification.impl.AccCertUpdateHelper;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Execution of a certification campaign creation.
 */
public final class AccessCertificationCloseCurrentStageRun
        extends SearchBasedActivityRun
        <AccessCertificationCaseType, AccessCertificationCloseCurrentStageWorkDefinition,
                AccessCertificationCloseCurrentStageActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationCloseCurrentStageRun.class);

    private List<AccessCertificationResponseType> outcomesToStopOn;
    private AccessCertificationCampaignType campaign;
    private int iteration;
    private ObjectQuery query;
    private AccessCertificationStageType stage;
    private XMLGregorianCalendar now;

    AccessCertificationCloseCurrentStageRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationCloseCurrentStageWorkDefinition, AccessCertificationCloseCurrentStageActivityHandler> context) {
        super(context, "");
        setInstanceReady();
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true)
                .skipWritingOperationExecutionRecords(true);
    }

    @Override
    public void beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        String campaignOid = getWorkDefinition().getCertificationCampaignRef().getOid();
        campaign = getBeans().repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, null, result).asObjectable();
        outcomesToStopOn = getActivityHandler().getComputationHelper().getOutcomesToStopOn(campaign);
        iteration = norm(campaign.getIteration());
        query = prepareObjectQuery();

        stage = CertCampaignTypeUtil.findStage(campaign, or0(campaign.getStageNumber()));
        now = getActivityHandler().getModelBeans().clock.currentTimeXMLGregorianCalendar();

        super.beforeRun(result);
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException, ActivityRunException {

        Collection<ItemDelta<?, ?>> deltas  = new ArrayList<>();
        AccCertUpdateHelper updateHelper = getActivityHandler().getUpdateHelper();
        deltas.add(updateHelper.createStateDelta(REVIEW_STAGE_DONE));
        Long stageId = stage.asPrismContainerValue().getId();
        assert stageId != null;
        deltas.add(PrismContext.get().deltaFor(AccessCertificationCampaignType.class)
                .item(F_STAGE, stageId, F_END_TIMESTAMP).replace(now)
                .asItemDelta());

        deltas.add(updateHelper.createTriggerDeleteDelta());
        updateHelper.modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaign.getOid(), deltas, getRunningTask(), result);

        getActivityHandler().getEventHelper().onCampaignStageEnd(campaign, getRunningTask(), result);
        super.afterRun(result);
    }


    @Override
    public @Nullable SearchSpecification<AccessCertificationCaseType> createCustomSearchSpecification(OperationResult result) {
        return new SearchSpecification<>(AccessCertificationCaseType.class, query, null, false);
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return getWorkDefinition().getCertificationCampaignRef();
    }

    @Override
    public boolean processItem(@NotNull AccessCertificationCaseType item, @NotNull ItemProcessingRequest<AccessCertificationCaseType> request, RunningTask workerTask, OperationResult result) throws CommonException {
        long caseId = item.getId();
        if (item.getReviewFinishedTimestamp() != null) {
            LOGGER.trace("Review process of case {} has already finished, skipping to the next one", caseId);
            return true;
        }
        Clock clock = getActivityHandler().getModelBeans().clock;
        LOGGER.trace("Updating current outcome for case {}", caseId);
        AccCertResponseComputationHelper computationHelper = getActivityHandler().getComputationHelper();
        AccessCertificationResponseType newStageOutcome = computationHelper.computeOutcomeForStage(item, campaign, or0(campaign.getStageNumber()));
        String newStageOutcomeUri = toUri(newStageOutcome);
        String newOverallOutcomeUri = toUri(computationHelper.computeOverallOutcome(item, campaign, or0(campaign.getStageNumber()), newStageOutcome));
        List<ItemDelta<?, ?>> deltas = new ArrayList<>(
                PrismContext.get().deltaFor(AccessCertificationCampaignType.class)
                        .item(F_CASE, caseId, F_CURRENT_STAGE_OUTCOME).replace(newStageOutcomeUri)
                        .item(F_CASE, caseId, F_OUTCOME).replace(newOverallOutcomeUri)
                        .item(F_CASE, caseId, F_EVENT).add(
                                new StageCompletionEventType()
                                        .timestamp(clock.currentTimeXMLGregorianCalendar())
                                        .stageNumber(or0(campaign.getStageNumber()))
                                        .iteration(campaign.getIteration())
                                        .outcome(newStageOutcomeUri))
                        .asItemDeltas());
        LOGGER.trace("Stage outcome = {}, overall outcome = {}", newStageOutcome, newOverallOutcomeUri);
        if (outcomesToStopOn.contains(newStageOutcome)) {
            deltas.add(PrismContext.get().deltaFor(AccessCertificationCampaignType.class)
                    .item(F_CASE, caseId, F_REVIEW_FINISHED_TIMESTAMP).replace(now)
                    .asItemDelta());
            LOGGER.debug("Marking case {} as review-finished because stage outcome = {}", caseId, newStageOutcome);
        }

        List<AccessCertificationWorkItemType> openWorkItems = item.getWorkItem().stream().filter(this::isWorkItemOpen).toList();
        LOGGER.debug("There are {} open work items for {}", openWorkItems.size(), ObjectTypeUtil.toShortString(campaign));
        for (AccessCertificationWorkItemType workItem : openWorkItems) {
            deltas.add(PrismContext.get().deltaFor(AccessCertificationCampaignType.class)
                            .item(F_CASE, item.getId(), F_WORK_ITEM, workItem.getId(), F_CLOSE_TIMESTAMP)
                            .replace(now)
                            .asItemDelta());
        }

        getActivityHandler().getUpdateHelper().modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaign.getOid(), deltas, getRunningTask(), result);
        return true;
    }

    private boolean isWorkItemOpen(AccessCertificationWorkItemType workItem) {
        return workItem.getCloseTimestamp() == null;
    }


    @NotNull
    private ObjectQuery prepareObjectQuery() {
        return PrismContext.get().queryFor(AccessCertificationCaseType.class)
                .ownerId(campaign.getOid())
                .and().item(AccessCertificationCaseType.F_ITERATION).eq(iteration)
                .build();
    }
}
