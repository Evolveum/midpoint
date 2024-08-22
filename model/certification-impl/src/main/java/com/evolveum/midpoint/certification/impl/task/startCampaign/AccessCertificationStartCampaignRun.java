/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task.startCampaign;

import com.evolveum.midpoint.certification.impl.*;
import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypedObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.run.processing.ItemProcessingRequest;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.*;
import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAccessCertificationDefinitionType.F_LAST_CAMPAIGN_STARTED_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STAGE;

/**
 * Execution of a certification campaign creation.
 */
public final class AccessCertificationStartCampaignRun
        extends SearchBasedActivityRun
        <AssignmentHolderType, AccessCertificationStartCampaignWorkDefinition,
                AccessCertificationStartCampaignActivityHandler,
                AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationStartCampaignRun.class);

    private AccessCertificationCampaignType campaign;
    private TypedObjectQuery<AssignmentHolderType> query;
    private CertificationHandler handler;
    private AccessCertificationReviewerSpecificationType reviewerSpec;
    private AccessCertificationStageType stage;

    AccessCertificationStartCampaignRun(
            @NotNull ActivityRunInstantiationContext<AccessCertificationStartCampaignWorkDefinition, AccessCertificationStartCampaignActivityHandler> context) {
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
        //campaignoid; TODO nacitat vsetky objekty, ktore budem potrebovat, asi aj resolvnutie handlera
        String campaignOid = getWorkDefinition().getCertificationCampaignRef().getOid();
        //TODO is repository service OK here?
        campaign = getBeans().repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, null, result).asObjectable();
        handler = getActivityHandler().getCertificationManager().findCertificationHandler(campaign);

        String campaignShortName = toShortString(campaign);
        AccessCertificationScopeType scope = campaign.getScopeDefinition();
        LOGGER.trace("Creating cases for scope {} in campaign {}", scope, campaignShortName);
        if (scope != null && !(scope instanceof AccessCertificationObjectBasedScopeType)) {
            throw new IllegalStateException("Unsupported access certification scope type: " + scope.getClass() + " for campaign " + campaignShortName);
        }
        AccessCertificationObjectBasedScopeType objectBasedScope = (AccessCertificationObjectBasedScopeType) scope;

        query = prepareObjectQuery(objectBasedScope, handler, campaignShortName);

        //TODO coppied from findReviewersSpecification. is stage 1 ok?
        reviewerSpec = CertCampaignTypeUtil.findStageDefinition(campaign, 1)
                .getReviewerSpecification();

        int requestedStageNumber = campaign.getStageNumber() + 1;
        stage = createStage(campaign, requestedStageNumber);
        super.beforeRun(result);
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException, ActivityRunException {

        int stageNumber = campaign.getStageNumber();
        int newStageNumber = stage.getNumber();

        ModificationsToExecute rv = new ModificationsToExecute();
        rv.add(createStageAddDelta(stage));
        rv.add(createDeltasToRecordStageOpen(campaign, stage));
        rv.add(getActivityHandler().getUpdateHelper().getDeltasToCreateTriggersForTimedActions(campaign.getOid(), 0,
                XmlTypeConverter.toDate(stage.getStartTimestamp()), XmlTypeConverter.toDate(stage.getDeadline()),
                CertCampaignTypeUtil.findStageDefinition(campaign, newStageNumber).getTimedActions()));

//        if (!skipEmptyStages || openingContext.casesEnteringStage > 0) {
            getActivityHandler().getUpdateHelper().modifyCampaignPreAuthorized(campaign.getOid(), rv, getRunningTask(), result);
//            afterStageOpen(campaign.getOid(), stage, task, result);       // notifications, bookkeeping, ...
//            return;
//
//        }

        Task task = getRunningTask();
        AccCertEventHelper eventHelper = getActivityHandler().getEventHelper();
        eventHelper.onCampaignStart(campaign, task, result);
        eventHelper.onCampaignStageStart(campaign, task, result);

        AccCertUpdateHelper updateHelper = getActivityHandler().getUpdateHelper();
        updateHelper.notifyReviewers(campaign, false, task, result);

        if (stage.getNumber() == 1 && norm(campaign.getIteration()) == 1 && campaign.getDefinitionRef() != null) {
            List<ItemDelta<?,?>> deltas = PrismContext.get().deltaFor(AccessCertificationDefinitionType.class)
                    .item(F_LAST_CAMPAIGN_STARTED_TIMESTAMP).replace(getActivityHandler().getModelBeans().clock.currentTimeXMLGregorianCalendar())
                    .asItemDeltas();
            updateHelper.modifyObjectPreAuthorized(AccessCertificationDefinitionType.class, campaign.getDefinitionRef().getOid(), deltas, task, result);
        }

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
    public @Nullable SearchSpecification<AssignmentHolderType> createCustomSearchSpecification(OperationResult result) {
        return SearchSpecification.fromTypedQuery(query);
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
    public boolean processItem(@NotNull AssignmentHolderType item, @NotNull ItemProcessingRequest<AssignmentHolderType> request, RunningTask workerTask, OperationResult result) throws CommonException, ActivityRunException {
        List<AccessCertificationCaseType> caseList = new ArrayList<>();
        Task task = getRunningTask();
        caseList.addAll(handler.createCasesForObject(request.getItem().asPrismObject(), campaign, task, result));

        Collection<ItemDelta<?, ?>> modifications = new ArrayList<>();
        for (AccessCertificationCaseType aCase : caseList) {
            ContainerDelta<AccessCertificationCaseType> caseDelta = PrismContext.get().deltaFactory().container().createDelta(F_CASE,
                    AccessCertificationCampaignType.class);
            aCase.setIteration(1);
            aCase.setStageNumber(1);
            aCase.setCurrentStageCreateTimestamp(stage.getStartTimestamp());
            aCase.setCurrentStageDeadline(stage.getDeadline());


            List<ObjectReferenceType> reviewers = getActivityHandler().getReviewersHelper().getReviewersForCase(aCase, campaign, reviewerSpec, task, result);
            aCase.getWorkItem().addAll(createWorkItems(reviewers, 1, 1, aCase));

//            openingContext.workItemsCreated += aCase.getWorkItem().size();
//            openingContext.casesEnteringStage++;

            AccessCertificationResponseType currentStageOutcome = getActivityHandler().getComputationHelper().computeOutcomeForStage(aCase, campaign, 1);
            aCase.setCurrentStageOutcome(toUri(currentStageOutcome));
            aCase.setOutcome(toUri(getActivityHandler().getComputationHelper().computeOverallOutcome(aCase, campaign, 1, currentStageOutcome)));

            @SuppressWarnings({ "raw", "unchecked" })
            PrismContainerValue<AccessCertificationCaseType> caseCVal = aCase.asPrismContainerValue();
            caseDelta.addValueToAdd(caseCVal);
            LOGGER.trace("Adding certification case:\n{}", caseCVal.debugDumpLazily());

//            modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaignOid, batch, task, result);
            modifications.add(caseDelta);
        }
        getActivityHandler().getUpdateHelper().modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaign.getOid(), modifications, task, result);
//        getBeans().repositoryService.modifyObject(AccessCertificationCampaignType.class, campaign.getOid(), caseDelta, result);

        return true;
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
    private TypedObjectQuery<AssignmentHolderType> prepareObjectQuery(AccessCertificationObjectBasedScopeType objectBasedScope,
            CertificationHandler handler, String campaignShortName) throws SchemaException {
        QName scopeDeclaredObjectType;
        if (objectBasedScope != null) {
            scopeDeclaredObjectType = objectBasedScope.getObjectType();
        } else {
            scopeDeclaredObjectType = null;
        }
        QName objectType;
        if (scopeDeclaredObjectType != null) {
            objectType = scopeDeclaredObjectType;
        } else {
            objectType = handler.getDefaultObjectType();
        }
        if (objectType == null) {
            throw new IllegalStateException("Unspecified object type (and no default one provided) for campaign " + campaignShortName);
        }
        @SuppressWarnings({ "unchecked", "raw" })
        Class<AssignmentHolderType> objectClass = (Class<AssignmentHolderType>) PrismContext.get().getSchemaRegistry().getCompileTimeClassForObjectTypeRequired(objectType);

        // TODO derive search filter from certification handler (e.g. select only objects having assignments with the proper policySituation)
        // It is only an optimization but potentially a very strong one. Workaround: enter query filter manually into scope definition.
        final SearchFilterType searchFilter = objectBasedScope != null ? objectBasedScope.getSearchFilter() : null;
        ObjectQuery query = PrismContext.get().queryFactory().createQuery();
        if (searchFilter != null) {
            query.setFilter(PrismContext.get().getQueryConverter().parseFilter(searchFilter, objectClass));
        }
        return new TypedObjectQuery<>(objectClass, query);
    }
}
