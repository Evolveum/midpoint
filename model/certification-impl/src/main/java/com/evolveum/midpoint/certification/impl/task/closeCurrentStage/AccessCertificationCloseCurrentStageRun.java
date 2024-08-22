/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.closeCurrentStage;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.*;
import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAccessCertificationDefinitionType.F_LAST_CAMPAIGN_STARTED_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CLOSE_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.REVIEW_STAGE_DONE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType.F_END_TIMESTAMP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.common.Clock;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.certification.impl.*;
import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
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
////    private String campaignOid;


//    private AccessCertificationStageType stage;
//    private int stageToBe;
//
//    private CertificationHandler handler;
//    private AccessCertificationReviewerSpecificationType reviewerSpec;
//    private int casesEnteringStage;


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

        stage = CertCampaignTypeUtil.findStage(campaign, campaign.getStageNumber());
        now = getActivityHandler().getModelBeans().clock.currentTimeXMLGregorianCalendar();
//        //TODO is repository service OK here?
//        casesEnteringStage = 0;

//        handler = getActivityHandler().getCertificationManager().findCertificationHandler(campaign);
//
//        int requestedStageNumber = campaign.getStageNumber() + 1;
//        stage = createStage(campaign, requestedStageNumber);
//        stageToBe = stage.getNumber();
//
//
//        String campaignShortName = toShortString(campaign);
//        AccessCertificationScopeType scope = campaign.getScopeDefinition();
//        LOGGER.trace("Creating cases for scope {} in campaign {}", scope, campaignShortName);
//        if (scope != null && !(scope instanceof AccessCertificationObjectBasedScopeType)) {
//            throw new IllegalStateException("Unsupported access certification scope type: " + scope.getClass() + " for campaign " + campaignShortName);
//        }
//

//
//        //TODO coppied from findReviewersSpecification. is stage 1 ok?
//        reviewerSpec = CertCampaignTypeUtil.findStageDefinition(campaign, stageToBe)
//                .getReviewerSpecification();
////        reviewersHelper.findReviewersSpecification(campaign, 1);
//
////        AccCertOpenerHelper.OpeningContext openingContext = new AccCertOpenerHelper.OpeningContext();
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
//        int stageNumber = campaign.getStageNumber();
//        int newStageNumber = stage.getNumber();
//
//        ModificationsToExecute rv = new ModificationsToExecute();
//        rv.add(createStageAddDelta(stage));
//        rv.add(createDeltasToRecordStageOpen(campaign, stage));
//        rv.add(getActivityHandler().getUpdateHelper().getDeltasToCreateTriggersForTimedActions(campaign.getOid(), 0,
//                XmlTypeConverter.toDate(stage.getStartTimestamp()), XmlTypeConverter.toDate(stage.getDeadline()),
//                CertCampaignTypeUtil.findStageDefinition(campaign, newStageNumber).getTimedActions()));
//
//        boolean skipEmptyStages = norm(campaign.getIteration()) > 1;
////        if (!skipEmptyStages || casesEnteringStage > 0) {
//            getActivityHandler().getUpdateHelper().modifyCampaignPreAuthorized(campaign.getOid(), rv, getRunningTask(), result);
////        }
//
//        Task task = getRunningTask();
//        AccCertEventHelper eventHelper = getActivityHandler().getEventHelper();
//        eventHelper.onCampaignStageStart(campaign, task, result);
//
//        AccCertUpdateHelper updateHelper = getActivityHandler().getUpdateHelper();
//        updateHelper.notifyReviewers(campaign, false, task, result);
//
//        if (stage.getNumber() == 1 && norm(campaign.getIteration()) == 1 && campaign.getDefinitionRef() != null) {
//            List<ItemDelta<?,?>> deltas = PrismContext.get().deltaFor(AccessCertificationDefinitionType.class)
//                    .item(F_LAST_CAMPAIGN_STARTED_TIMESTAMP).replace(getActivityHandler().getModelBeans().clock.currentTimeXMLGregorianCalendar())
//                    .asItemDeltas();
//            updateHelper.modifyObjectPreAuthorized(AccessCertificationDefinitionType.class, campaign.getDefinitionRef().getOid(), deltas, task, result);
//        }
//
////            afterStageOpen(campaign.getOid(), stage, task, result);       // notifications, bookkeeping, ...
////            return;
////        }


        super.afterRun(result);
    }

    private ItemDelta<?, ?> createStageAddDelta(AccessCertificationStageType stage) throws SchemaException {
        return PrismContext.get().deltaFor(AccessCertificationCampaignType.class)
                .item(F_STAGE).add(stage)
                .asItemDelta();
    }

    // some bureaucracy... stage#, state, start time, triggers
    private List<ItemDelta<?,?>> createDeltasToRecordStageOpen(AccessCertificationCampaignType campaign,
            AccessCertificationStageType newStage) throws SchemaException {

        List<ItemDelta<?,?>> itemDeltaList = new ArrayList<>();

        itemDeltaList.add(getActivityHandler().getUpdateHelper().createStageNumberDelta(newStage.getNumber()));
        itemDeltaList.add(getActivityHandler().getUpdateHelper().createStateDelta(IN_REVIEW_STAGE));

        boolean campaignJustStarted = campaign.getStageNumber() == 0;
        if (campaignJustStarted) {
            itemDeltaList.add(getActivityHandler().getUpdateHelper().createStartTimeDelta(getActivityHandler().getModelBeans().clock.currentTimeXMLGregorianCalendar()));
        }

        XMLGregorianCalendar stageDeadline = newStage.getDeadline();
        if (stageDeadline != null) {
            // auto-closing and notifications triggers
            final AccessCertificationStageDefinitionType stageDef =
                    CertCampaignTypeUtil.findStageDefinition(campaign, newStage.getNumber());
            List<TriggerType> triggers = new ArrayList<>();

            // pseudo-random ID so this trigger will not be deleted by trigger task handler (if this code itself is executed as part of previous trigger firing)
            // TODO implement this more seriously!
            long lastId = (long) (Math.random() * 1000000000);

            final TriggerType triggerClose = new TriggerType();
            triggerClose.setHandlerUri(AccessCertificationCloseStageTriggerHandler.HANDLER_URI);
            triggerClose.setTimestamp(stageDeadline);
            triggerClose.setId(lastId);
            triggers.add(triggerClose);

            for (Duration beforeDeadline : stageDef.getNotifyBeforeDeadline()) {
                final XMLGregorianCalendar beforeEnd = CloneUtil.clone(stageDeadline);
                beforeEnd.add(beforeDeadline.negate());
                if (XmlTypeConverter.toMillis(beforeEnd) > System.currentTimeMillis()) {
                    final TriggerType triggerBeforeEnd = new TriggerType();
                    triggerBeforeEnd.setHandlerUri(AccessCertificationCloseStageApproachingTriggerHandler.HANDLER_URI);
                    triggerBeforeEnd.setTimestamp(beforeEnd);
                    triggerBeforeEnd.setId(++lastId);
                    triggers.add(triggerBeforeEnd);
                }
            }

            ContainerDelta<TriggerType> triggerDelta = PrismContext.get().deltaFactory().container()
                    .createModificationReplace(ObjectType.F_TRIGGER, AccessCertificationCampaignType.class, triggers);
            itemDeltaList.add(triggerDelta);
        }
        return itemDeltaList;
    }

    private AccessCertificationStageType createStage(AccessCertificationCampaignType campaign, int requestedStageNumber) {
        AccessCertificationStageType stage = new AccessCertificationStageType();
        stage.setIteration(norm(campaign.getIteration()));
        stage.setNumber(requestedStageNumber);
        stage.setStartTimestamp(getActivityHandler().getModelBeans().clock.currentTimeXMLGregorianCalendar());

        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, stage.getNumber());
        XMLGregorianCalendar deadline = computeDeadline(stage.getStartTimestamp(), stageDef.getDuration(), stageDef.getDeadlineRounding());
        stage.setDeadline(deadline);

        stage.setName(stageDef.getName());
        stage.setDescription(stageDef.getDescription());

        return stage;
    }

    private XMLGregorianCalendar computeDeadline(XMLGregorianCalendar start, Duration duration, DeadlineRoundingType deadlineRounding) {
        XMLGregorianCalendar deadline = (XMLGregorianCalendar) start.clone();
        if (duration != null) {
            deadline.add(duration);
        }
        DeadlineRoundingType rounding = deadlineRounding != null ?
                deadlineRounding : DeadlineRoundingType.DAY;
        switch (rounding) {
            case DAY:
                deadline.setHour(23);
            case HOUR:
                deadline.setMinute(59);
                deadline.setSecond(59);
                deadline.setMillisecond(999);
            case NONE:
                // nothing here
        }
        return deadline;
    }

    @Override
    public @Nullable SearchSpecification<AccessCertificationCaseType> createCustomSearchSpecification(OperationResult result) {
        return new SearchSpecification<>(AccessCertificationCaseType.class, query, null, null);
//        return super.createCustomSearchSpecification(result);
    }

    //    @Override
//    protected @NotNull ActivityRunResult runLocally(OperationResult result) throws ActivityRunException, CommonException {
//        @NotNull AccessCertificationCloseCurrentStageActivityHandler handler = getActivityHandler();
//
//        LOGGER.trace("Task run starting");
//
//        OperationResult runResult = result.createSubresult("Campaign next stage");
//
//        String campaignOid = getWorkDefinition().getCertificationCampaignRef().getOid();
//        if (campaignOid == null) {
//            LOGGER.error("No campaign OID specified in the task");
//            runResult.recordFatalError("No campaign OID specified in the task");
//            return standardRunResult(runResult.getStatus());
//        }
//
//        runResult.addContext("campaignOid", campaignOid);
//
//        LOGGER.info("opening campaign next stage for certification campaign {}.", campaignOid);
//
//        try {
//
//            handler.getCertificationManager().openNextStage(campaignOid, getRunningTask(), runResult);
//
//            runResult.computeStatus();
//            runResult.setStatus(OperationResultStatus.SUCCESS);
//            LOGGER.trace("Task run stopping (campaign {})", campaignOid);
//            return standardRunResult(runResult.getStatus());
//
//        } catch (Exception e) {
//            LoggingUtils.logException(LOGGER, "Error while opening campaign next stage", e);
//            runResult.recordFatalError("Error while opening campaign next stage, error: " + e.getMessage(), e);
//            return standardRunResult(runResult.getStatus());
//        }
//    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return getWorkDefinition().getCertificationCampaignRef();
    }

    @Override
    public boolean processItem(@NotNull AccessCertificationCaseType item, @NotNull ItemProcessingRequest<AccessCertificationCaseType> request, RunningTask workerTask, OperationResult result) throws CommonException, ActivityRunException {
        long caseId = item.getId();
        if (item.getReviewFinishedTimestamp() != null) {
            LOGGER.trace("Review process of case {} has already finished, skipping to the next one", caseId);
            return true;
        }
        Clock clock = getActivityHandler().getModelBeans().clock;
        LOGGER.trace("Updating current outcome for case {}", caseId);
        AccCertResponseComputationHelper computationHelper = getActivityHandler().getComputationHelper();
        AccessCertificationResponseType newStageOutcome = computationHelper.computeOutcomeForStage(item, campaign, campaign.getStageNumber());
        String newStageOutcomeUri = toUri(newStageOutcome);
        String newOverallOutcomeUri = toUri(computationHelper.computeOverallOutcome(item, campaign, campaign.getStageNumber(), newStageOutcome));
        List<ItemDelta<?, ?>> deltas = new ArrayList<>(
                PrismContext.get().deltaFor(AccessCertificationCampaignType.class)
                        .item(F_CASE, caseId, F_CURRENT_STAGE_OUTCOME).replace(newStageOutcomeUri)
                        .item(F_CASE, caseId, F_OUTCOME).replace(newOverallOutcomeUri)
                        .item(F_CASE, caseId, F_EVENT).add(
                                new StageCompletionEventType()
                                        .timestamp(clock.currentTimeXMLGregorianCalendar())
                                        .stageNumber(campaign.getStageNumber())
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

//        ObjectQuery query = CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaign.getOid());
//        List<AccessCertificationWorkItemType> openWorkItems = queryHelper.searchOpenWorkItems(query, false, result);
        List<AccessCertificationWorkItemType> openWorkItems = item.getWorkItem().stream().filter(wi -> isWorkItemOpen(wi)).toList();
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
        return workItem.getCloseTimestamp() == null && (workItem.getOutput() == null || workItem.getOutput().getOutcome() == null);
    }

    private List<AccessCertificationWorkItemType> createWorkItems(
            List<ObjectReferenceType> forReviewers, int forStage, int forIteration, AccessCertificationCaseType _case) {
        assert forIteration > 0;
        boolean avoidRedundantWorkItems = forIteration > 1;           // TODO make configurable
        List<AccessCertificationWorkItemType> workItems = new ArrayList<>();
        for (ObjectReferenceType reviewer : forReviewers) {
            boolean skipCreation = false;
            if (avoidRedundantWorkItems) {
                for (AccessCertificationWorkItemType existing : _case.getWorkItem()) {
                    if (existing.getStageNumber() == forStage
                            && existing.getOriginalAssigneeRef() != null
                            && Objects.equals(existing.getOriginalAssigneeRef().getOid(), reviewer.getOid())
                            && existing.getOutput() != null && normalizeToNull(fromUri(existing.getOutput().getOutcome())) != null) {
                        skipCreation = true;
                        LOGGER.trace("Skipping creation of a work item for {}, because the relevant outcome already exists in {}",
                                PrettyPrinter.prettyPrint(reviewer), existing);
                        break;
                    }
                }
            }
            if (!skipCreation) {
                AccessCertificationWorkItemType workItem = new AccessCertificationWorkItemType()
                        .stageNumber(forStage)
                        .iteration(forIteration)
                        .assigneeRef(reviewer.clone())
                        .originalAssigneeRef(reviewer.clone());
                workItems.add(workItem);
            }
        }
        return workItems;
    }

    @NotNull
    private ObjectQuery prepareObjectQuery() throws SchemaException {
        return PrismContext.get().queryFor(AccessCertificationCaseType.class)
                .ownerId(campaign.getOid())
                .and().item(AccessCertificationCaseType.F_ITERATION).eq(iteration)
                .build();
//        return new TypedObjectQuery<>(AccessCertificationCaseType.class, query);
//        QName scopeDeclaredObjectType;
//        if (objectBasedScope != null) {
//            scopeDeclaredObjectType = objectBasedScope.getObjectType();
//        } else {
//            scopeDeclaredObjectType = null;
//        }
//        QName objectType;
//        if (scopeDeclaredObjectType != null) {
//            objectType = scopeDeclaredObjectType;
//        } else {
//            objectType = handler.getDefaultObjectType();
//        }
//        if (objectType == null) {
//            throw new IllegalStateException("Unspecified object type (and no default one provided) for campaign " + campaignShortName);
//        }
//        @SuppressWarnings({ "unchecked", "raw" })
//        Class<AssignmentHolderType> objectClass = (Class<AssignmentHolderType>) PrismContext.get().getSchemaRegistry().getCompileTimeClassForObjectTypeRequired(objectType);
//
//        // TODO derive search filter from certification handler (e.g. select only objects having assignments with the proper policySituation)
//        // It is only an optimization but potentially a very strong one. Workaround: enter query filter manually into scope definition.
//        final SearchFilterType searchFilter = objectBasedScope != null ? objectBasedScope.getSearchFilter() : null;
//        ObjectQuery query = PrismContext.get().queryFactory().createQuery();
//        if (searchFilter != null) {
//            query.setFilter(PrismContext.get().getQueryConverter().parseFilter(searchFilter, objectClass));
//        }
//        return new TypedObjectQuery<>(objectClass, query);
    }
}
