/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.toUri;
import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortStringLazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CLOSE_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.REVIEW_STAGE_DONE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_REVIEW_FINISHED_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_CLOSED_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType.F_END_TIMESTAMP;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

/**
 * Supports campaign/stage closing operations and related actions.
 */
@Component
public class AccCertCloserHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AccCertCloserHelper.class);

    @Autowired private AccCertEventHelper eventHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelService modelService;
    @Autowired private AccCertGeneralHelper generalHelper;
    @Autowired protected AccCertQueryHelper queryHelper;
    @Autowired private Clock clock;
    @Autowired private AccCertResponseComputationHelper computationHelper;
    @Autowired private AccCertUpdateHelper updateHelper;

    private static final String CLASS_DOT = AccCertCloserHelper.class.getName() + ".";
    private static final String OPERATION_DELETE_OBSOLETE_CAMPAIGN = CLASS_DOT + "deleteObsoleteCampaign";
    private static final String OPERATION_CLEANUP_CAMPAIGNS_BY_NUMBER = CLASS_DOT + "cleanupCampaignsByNumber";
    private static final String OPERATION_CLEANUP_CAMPAIGNS_BY_AGE = CLASS_DOT + "cleanupCampaignsByAge";

    void closeCampaign(AccessCertificationCampaignType campaign, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        LOGGER.info("Closing campaign {}", ObjectTypeUtil.toShortString(campaign));
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        int lastStageNumber = CertCampaignTypeUtil.getNumberOfStages(campaign);
        // TODO issue a warning if we are not in a correct state
        ModificationsToExecute modifications = new ModificationsToExecute();
        modifications.add(updateHelper.createStageNumberDelta(lastStageNumber + 1));
        modifications.add(updateHelper.createStateDelta(CLOSED));
        modifications.add(updateHelper.createTriggerReplaceDelta(createTriggersForCampaignClose(campaign, result)));
        modifications.add(updateHelper.createEndTimeDelta(now));
        createWorkItemsCloseDeltas(campaign, modifications, now, result);

        updateHelper.modifyCampaignPreAuthorized(campaign.getOid(), modifications, task, result);

        AccessCertificationCampaignType updatedCampaign = updateHelper.refreshCampaign(campaign, result);
        eventHelper.onCampaignEnd(updatedCampaign, task, result);

        if (campaign.getDefinitionRef() != null) {
            List<ItemDelta<?,?>> definitionDeltas = prismContext.deltaFor(AccessCertificationDefinitionType.class)
                    .item(F_LAST_CAMPAIGN_CLOSED_TIMESTAMP).replace(now)
                    .asItemDeltas();
            updateHelper.modifyObjectPreAuthorized(AccessCertificationDefinitionType.class, campaign.getDefinitionRef().getOid(), definitionDeltas, task, result);
        }
    }

    @NotNull
    private Collection<TriggerType> createTriggersForCampaignClose(AccessCertificationCampaignType campaign, OperationResult result) {
        if (campaign.getReiterationDefinition() == null || campaign.getReiterationDefinition().getStartsAfter() == null) {
            return emptySet();
        }
        if (limitReached(campaign, campaign.getReiterationDefinition().getLimitWhenAutomatic())
            || limitReached(campaign, campaign.getReiterationDefinition().getLimit())) {
            return emptySet();
        }
        if (queryHelper.hasNoResponseCases(campaign.getOid(), result)) {
            TriggerType trigger = new TriggerType();
            XMLGregorianCalendar triggerTime = clock.currentTimeXMLGregorianCalendar();
            triggerTime.add(campaign.getReiterationDefinition().getStartsAfter());
            trigger.setTimestamp(triggerTime);
            trigger.setHandlerUri(AccessCertificationCampaignReiterationTriggerHandler.HANDLER_URI);
            return singleton(trigger);
        } else {
            LOGGER.debug("Campaign {} has no no-response cases, skipping creation of reiteration trigger", toShortStringLazy(campaign));
            return emptySet();
        }
    }

    private boolean limitReached(AccessCertificationCampaignType campaign, Integer limit) {
        return limit != null && norm(campaign.getIteration()) >= limit;
    }

    private void createWorkItemsCloseDeltas(AccessCertificationCampaignType campaign, ModificationsToExecute modifications,
            XMLGregorianCalendar now, OperationResult result) throws SchemaException {
        ObjectQuery query = CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaign.getOid(), prismContext);
        List<AccessCertificationWorkItemType> openWorkItems = queryHelper.searchOpenWorkItems(query, false, result);
        LOGGER.debug("There are {} open work items for {}", openWorkItems.size(), ObjectTypeUtil.toShortString(campaign));
        for (AccessCertificationWorkItemType workItem : openWorkItems) {
            AccessCertificationCaseType aCase = CertCampaignTypeUtil.getCaseChecked(workItem);
            modifications.add(
                    prismContext.deltaFor(AccessCertificationCampaignType.class)
                            .item(F_CASE, aCase.getId(), F_WORK_ITEM, workItem.getId(), F_CLOSE_TIMESTAMP)
                            .replace(now)
                            .asItemDelta());
        }
    }

    void closeStage(AccessCertificationCampaignType campaign, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        ModificationsToExecute modifications = getDeltasForStageClose(campaign, result);
        updateHelper.modifyCampaignPreAuthorized(campaign.getOid(), modifications, task, result);
        afterStageClose(campaign.getOid(), task, result);
    }

    private ModificationsToExecute getDeltasForStageClose(AccessCertificationCampaignType campaign, OperationResult result)
            throws SchemaException {
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        ModificationsToExecute rv = new ModificationsToExecute();
        List<AccessCertificationResponseType> outcomesToStopOn = computationHelper.getOutcomesToStopOn(campaign);
        createCaseDeltasOnStageClose(campaign, rv, now, outcomesToStopOn, result);
        rv.createNewBatch();
        createWorkItemsCloseDeltas(campaign, rv, now, result);
        rv.createNewBatch();
        rv.add(updateHelper.createStateDelta(REVIEW_STAGE_DONE));
        rv.add(createStageEndTimeDelta(campaign, now));
        rv.add(updateHelper.createTriggerDeleteDelta());
        return rv;
    }

    // computes cases outcomes (stage-level and overall) at stage close and creates appropriate deltas
    private void createCaseDeltasOnStageClose(AccessCertificationCampaignType campaign,
            ModificationsToExecute modifications, XMLGregorianCalendar now,
            List<AccessCertificationResponseType> outcomesToStopOn,
            OperationResult result) throws SchemaException {
        LOGGER.debug("Updating current outcome for cases in {}", toShortStringLazy(campaign));
        List<AccessCertificationCaseType> caseList = queryHelper.getAllCurrentIterationCases(campaign.getOid(), norm(campaign.getIteration()), result);
        for (AccessCertificationCaseType aCase : caseList) {
            long caseId = aCase.getId();
            if (aCase.getReviewFinishedTimestamp() != null) {
                LOGGER.trace("Review process of case {} has already finished, skipping to the next one", caseId);
                continue;
            }
            LOGGER.trace("Updating current outcome for case {}", caseId);
            AccessCertificationResponseType newStageOutcome = computationHelper.computeOutcomeForStage(aCase, campaign, campaign.getStageNumber());
            String newStageOutcomeUri = toUri(newStageOutcome);
            String newOverallOutcomeUri = toUri(computationHelper.computeOverallOutcome(aCase, campaign, campaign.getStageNumber(), newStageOutcome));
            List<ItemDelta<?, ?>> deltas = new ArrayList<>(
                    prismContext.deltaFor(AccessCertificationCampaignType.class)
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
                deltas.add(prismContext.deltaFor(AccessCertificationCampaignType.class)
                        .item(F_CASE, caseId, F_REVIEW_FINISHED_TIMESTAMP).replace(now)
                        .asItemDelta());
                LOGGER.debug("Marking case {} as review-finished because stage outcome = {}", caseId, newStageOutcome);
            }
            modifications.add(deltas);
        }
    }

    private ItemDelta<?, ?> createStageEndTimeDelta(AccessCertificationCampaignType campaign, XMLGregorianCalendar now)
            throws SchemaException {
        AccessCertificationStageType stage = CertCampaignTypeUtil.findStage(campaign, campaign.getStageNumber());
        Long stageId = stage.asPrismContainerValue().getId();
        assert stageId != null;
        return prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_STAGE, stageId, F_END_TIMESTAMP).replace(now)
                .asItemDelta();
    }

    private void afterStageClose(String campaignOid, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        AccessCertificationCampaignType campaign = generalHelper.getCampaign(campaignOid, null, task, result);
        eventHelper.onCampaignStageEnd(campaign, task, result);
    }

    void cleanupCampaigns(@NotNull CleanupPolicyType policy, Task task, OperationResult result) {
        if (policy.getMaxAge() != null) {
            cleanupCampaignsByDate(policy.getMaxAge(), task, result);
        }
        if (policy.getMaxRecords() != null) {
            cleanupCampaignsByNumber(policy.getMaxRecords(), task, result);
        }
    }

    private static final int DELETE_BLOCK_SIZE = 100;

    private void cleanupCampaignsByNumber(int maxRecords, Task task, OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OPERATION_CLEANUP_CAMPAIGNS_BY_NUMBER);
        LOGGER.debug("Starting cleanup for closed certification campaigns, keeping {} ones.", maxRecords);
        int deleted = 0;
        Set<String> poisonedCampaigns = new HashSet<>();
        try {
            for (;;) {
                ObjectQuery query = prismContext.queryFor(AccessCertificationCampaignType.class)
                        .item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CLOSED)
                        .and().not().id(poisonedCampaigns.toArray(new String[0]))   // hoping there are not many of these
                        .desc(AccessCertificationCampaignType.F_END_TIMESTAMP)
                        .offset(maxRecords)
                        .maxSize(DELETE_BLOCK_SIZE)
                        .build();
                int delta = searchAndDeleteCampaigns(query, poisonedCampaigns, result, task);
                if (delta == 0) {
                    if (deleted > 0) {
                        LOGGER.info("Cleaned up {} closed certification campaigns.", deleted);
                    }
                    return;
                }
                deleted += delta;
            }
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void cleanupCampaignsByDate(Duration maxAge, Task task, OperationResult parentResult) {
        if (maxAge.getSign() > 0) {
            maxAge = maxAge.negate();
        }
        Date deleteCampaignsFinishedUpTo = new Date();
        maxAge.addTo(deleteCampaignsFinishedUpTo);

        LOGGER.debug("Starting cleanup for closed certification campaigns deleting up to {} (max age '{}').", deleteCampaignsFinishedUpTo, maxAge);

        OperationResult result = parentResult.createSubresult(OPERATION_CLEANUP_CAMPAIGNS_BY_AGE);
        XMLGregorianCalendar timeXml = createXMLGregorianCalendar(deleteCampaignsFinishedUpTo);
        int deleted = 0;
        Set<String> poisonedCampaigns = new HashSet<>();
        try {
            for (;;) {
                ObjectQuery query = prismContext.queryFor(AccessCertificationCampaignType.class)
                        .item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CLOSED)
                        .and().item(AccessCertificationCampaignType.F_END_TIMESTAMP).lt(timeXml)
                        .and().not().id(poisonedCampaigns.toArray(new String[0]))   // hoping there are not many of these
                        .maxSize(DELETE_BLOCK_SIZE)
                        .build();
                int delta = searchAndDeleteCampaigns(query, poisonedCampaigns, result, task);
                if (delta == 0) {
                    if (deleted > 0) {
                        LOGGER.info("Cleaned up {} closed certification campaigns.", deleted);
                    }
                    return;
                }
                deleted += delta;
            }
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private int searchAndDeleteCampaigns(ObjectQuery query, Set<String> poisonedCampaigns,
            OperationResult result, Task task) {
        SearchResultList<PrismObject<AccessCertificationCampaignType>> campaigns;
        try {
            campaigns = modelService.searchObjects(AccessCertificationCampaignType.class, query, null, task, result);
        } catch (CommonException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get a list of campaigns to be cleaned up", e);
            result.recordFatalError(e.getMessage(), e);
            return 0;
        }
        LOGGER.debug("Campaigns to be deleted: {}", campaigns.size());
        int deleted = 0;
        for (PrismObject<AccessCertificationCampaignType> campaign : campaigns) {
            if (ObjectTypeUtil.isIndestructible(campaign)) {
                LOGGER.trace("Campaign {} is indestructible, will not be deleted", campaign);
                continue;
            }
            OperationResult subresult = result.createMinorSubresult(OPERATION_DELETE_OBSOLETE_CAMPAIGN);
            try {
                LOGGER.debug("Deleting campaign {}", campaign);
                ObjectDelta<AccessCertificationCampaignType> deleteDelta = prismContext.deltaFactory().object().create(
                        AccessCertificationCampaignType.class, ChangeType.DELETE);
                deleteDelta.setOid(campaign.getOid());
                modelService.executeChanges(singleton(deleteDelta), null, task, subresult);
                deleted++;
            } catch (CommonException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete obsolete campaign {}", e, campaign);
                poisonedCampaigns.add(campaign.getOid());
            } finally {
                subresult.computeStatusIfUnknown();
            }
        }
        LOGGER.debug("Campaigns really deleted: {}", deleted);
        return deleted;
    }
}
