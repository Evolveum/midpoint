/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl.task;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.fromUri;
import static com.evolveum.midpoint.certification.api.OutcomeUtils.normalizeToNull;
import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAccessCertificationDefinitionType.F_LAST_CAMPAIGN_STARTED_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STAGE;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.certification.impl.*;
import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.run.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * T might be eiter focus, e.g. when new campaign is started (first stage is opening)
 * or it migth be AccessCertificationCaseType for other actions.
 * @param <T>
 */
public abstract class AccessCertificationStageManagementRun<
        T extends Containerable,
        WD extends AccessCertificationCampaignWorkDefinition,
        AH extends AccessCertificationCampaignActivityHandler<WD, AH>>
        extends SearchBasedActivityRun<T, WD, AH, AbstractActivityWorkStateType> {

    private static final Trace LOGGER = TraceManager.getTrace(AccessCertificationStageManagementRun.class);

    private AccessCertificationCampaignType campaign;
    private ObjectQuery query;
    private AccessCertificationReviewerSpecificationType reviewerSpec;
    private AccessCertificationStageType stage;
    private int stageToBe;
    private int iteration;

    private CertificationHandler handler;
    private AccCertUpdateHelper updateHelper;
    private AccCertResponseComputationHelper computationHelper;
    private AccCertReviewersHelper reviewersHelper;

    public AccessCertificationStageManagementRun(@NotNull ActivityRunInstantiationContext<WD, AH> context, @NotNull String shortNameCapitalized) {
        super(context, shortNameCapitalized);
    }

    @Override
    public @NotNull ActivityReportingCharacteristics createReportingCharacteristics() {
        return super.createReportingCharacteristics()
                .actionsExecutedStatisticsSupported(true)
                .skipWritingOperationExecutionRecords(true);
    }

    @Override
    public boolean beforeRun(OperationResult result) throws CommonException, ActivityRunException {
        // HANDLERS
        computationHelper = getActivityHandler().getComputationHelper();
        updateHelper = getActivityHandler().getUpdateHelper();
        reviewersHelper = getActivityHandler().getReviewersHelper();

        String campaignOid = getWorkDefinition().getCertificationCampaignRef().getOid();
        campaign = getBeans().repositoryService.getObject(AccessCertificationCampaignType.class, campaignOid, null, result).asObjectable();
        handler = getActivityHandler().getCertificationManager().findCertificationHandler(campaign);

        iteration = norm(campaign.getIteration());
        stageToBe = or0(campaign.getStageNumber()) + 1;

        stage = createStage();
        reviewerSpec = reviewersHelper.findReviewersSpecification(campaign, stageToBe);

        query = prepareObjectQuery();

        return super.beforeRun(result);
    }

    @Override
    public void afterRun(OperationResult result) throws CommonException, ActivityRunException {

        ModificationsToExecute rv = new ModificationsToExecute();
        rv.add(createStageAddDelta(stage));
        rv.add(createDeltasToRecordStageOpen(campaign, stage));
        rv.add(updateHelper.getDeltasToCreateTriggersForTimedActions(campaign.getOid(), 0,
                XmlTypeConverter.toDate(stage.getStartTimestamp()), XmlTypeConverter.toDate(stage.getDeadline()),
                CertCampaignTypeUtil.findStageDefinition(campaign, stageToBe).getTimedActions()));

        updateHelper.modifyCampaignPreAuthorized(campaign.getOid(), rv, getRunningTask(), result);
        //#10373 if the campaign is not updated, it gets to the notifier with the old data which causes the problem
        campaign = getBeans().repositoryService.getObject(AccessCertificationCampaignType.class, campaign.getOid(), null, result).asObjectable();

        Task task = getRunningTask();
        AccCertEventHelper eventHelper = getActivityHandler().getEventHelper();
        if (stageToBe == 1) {
            eventHelper.onCampaignStart(campaign, task, result);
        }
        eventHelper.onCampaignStageStart(campaign, task, result);

        updateHelper.notifyReviewers(campaign, false, task, result);

        if (stageToBe == 1 && norm(campaign.getIteration()) == 1 && campaign.getDefinitionRef() != null) {
            List<ItemDelta<?,?>> deltas = PrismContext.get().deltaFor(AccessCertificationDefinitionType.class)
                    .item(F_LAST_CAMPAIGN_STARTED_TIMESTAMP).replace(getActivityHandler().getModelBeans().clock.currentTimeXMLGregorianCalendar())
                    .asItemDeltas();
            updateHelper.modifyObjectPreAuthorized(AccessCertificationDefinitionType.class, campaign.getDefinitionRef().getOid(), deltas, task, result);
        }

        super.afterRun(result);
    }

    protected abstract ObjectQuery prepareObjectQuery() throws SchemaException;
    protected abstract Class<T> getType();

    private ItemDelta<?, ?> createStageAddDelta(AccessCertificationStageType stage) throws SchemaException {
        return PrismContext.get().deltaFor(AccessCertificationCampaignType.class)
                .item(F_STAGE).add(stage)
                .asItemDelta();
    }

    // some bureaucracy... stage#, state, start time, triggers
    private List<ItemDelta<?,?>> createDeltasToRecordStageOpen(AccessCertificationCampaignType campaign,
            AccessCertificationStageType newStage) throws SchemaException {

        List<ItemDelta<?,?>> itemDeltaList = new ArrayList<>();

        itemDeltaList.add(updateHelper.createStageNumberDelta(or0(newStage.getNumber())));
        itemDeltaList.add(updateHelper.createStateDelta(IN_REVIEW_STAGE));

        boolean campaignJustStarted = or0(campaign.getStageNumber()) == 0;
        if (campaignJustStarted) {
            itemDeltaList.add(updateHelper.createStartTimeDelta(getActivityHandler().getModelBeans().clock.currentTimeXMLGregorianCalendar()));
        }

        XMLGregorianCalendar stageDeadline = newStage.getDeadline();
        if (stageDeadline != null) {
            // auto-closing and notifications triggers
            final AccessCertificationStageDefinitionType stageDef =
                    CertCampaignTypeUtil.findStageDefinition(campaign, or0(newStage.getNumber()));
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

    protected AccessCertificationStageType createStage() {
        AccessCertificationStageType stage = new AccessCertificationStageType();
        stage.setIteration(iteration);
        stage.setNumber(stageToBe);
        stage.setStartTimestamp(getActivityHandler().getModelBeans().clock.currentTimeXMLGregorianCalendar());

        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, or0(stage.getNumber()));
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

    protected List<AccessCertificationWorkItemType> createWorkItems(
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

    @Override
    public @Nullable SearchSpecification<T> createCustomSearchSpecification(OperationResult result) {
        //TODO use repo directly? or model?
        return new SearchSpecification<>(getType(), query, null, false);
    }

    @Override
    protected @NotNull ObjectReferenceType getDesiredTaskObjectRef() {
        return getWorkDefinition().getCertificationCampaignRef();
    }

    protected CertificationHandler getCertificationHandler() {
        return handler;
    }

    public AccessCertificationCampaignType getCampaign() {
        return campaign;
    }

    public AccessCertificationReviewerSpecificationType getReviewerSpec() {
        return reviewerSpec;
    }

    public AccessCertificationStageType getStage() {
        return stage;
    }

    public int getStageToBe() {
        return stageToBe;
    }

    public int getIteration() {
        return iteration;
    }

    public AccCertUpdateHelper getUpdateHelper() {
        return updateHelper;
    }

    public AccCertResponseComputationHelper getComputationHelper() {
        return computationHelper;
    }

    public AccCertReviewersHelper getReviewersHelper() {
        return reviewersHelper;
    }
}
