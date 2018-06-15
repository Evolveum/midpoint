/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_ASSIGNEE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CLOSE_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_ESCALATION_LEVEL;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_CLOSED_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_ID_USED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_STARTED_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType.F_END_TIMESTAMP;
import static java.util.Collections.singleton;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * @author mederly
 */
@Component
public class AccCertUpdateHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertUpdateHelper.class);

    @Autowired private AccCertEventHelper eventHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelService modelService;
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private AccCertGeneralHelper generalHelper;
    @Autowired protected AccCertQueryHelper queryHelper;
    @Autowired private AccCertCaseOperationsHelper caseHelper;
    @Autowired private Clock clock;
    @Autowired private AccCertExpressionHelper expressionHelper;

	private static final String CLASS_DOT = AccCertUpdateHelper.class.getName() + ".";
	private static final String OPERATION_DELETE_OBSOLETE_CAMPAIGN = CLASS_DOT + "deleteObsoleteCampaign";
	private static final String OPERATION_CLEANUP_CAMPAIGNS_BY_NUMBER = CLASS_DOT + "cleanupCampaignsByNumber";
	private static final String OPERATION_CLEANUP_CAMPAIGNS_BY_AGE = CLASS_DOT + "cleanupCampaignsByAge";

    //region ================================ Campaign create ================================

	AccessCertificationCampaignType createCampaign(PrismObject<AccessCertificationDefinitionType> definition,
			OperationResult result, Task task)
			throws SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ObjectNotFoundException {
		AccessCertificationCampaignType newCampaign = createCampaignObject(definition.asObjectable(), task, result);
		addObjectPreAuthorized(newCampaign, task, result);
		return newCampaign;
	}

	private AccessCertificationCampaignType createCampaignObject(AccessCertificationDefinitionType definition,
			Task task, OperationResult result)
			throws SchemaException, SecurityViolationException {
        AccessCertificationCampaignType newCampaign = new AccessCertificationCampaignType(prismContext);

        if (definition.getName() != null) {
            newCampaign.setName(generateCampaignName(definition, task, result));
        } else {
            throw new SchemaException("Couldn't create a campaign without name");
        }

        newCampaign.setDescription(definition.getDescription());
        newCampaign.setOwnerRef(securityContextManager.getPrincipal().toObjectReference());
        newCampaign.setTenantRef(definition.getTenantRef());
        newCampaign.setDefinitionRef(ObjectTypeUtil.createObjectRef(definition));

        if (definition.getHandlerUri() != null) {
            newCampaign.setHandlerUri(definition.getHandlerUri());
        } else {
            throw new SchemaException("Couldn't create a campaign without handlerUri");
        }

        newCampaign.setScopeDefinition(definition.getScopeDefinition());
        newCampaign.setRemediationDefinition(definition.getRemediationDefinition());

        newCampaign.getStageDefinition().addAll(CloneUtil.cloneCollectionMembers(definition.getStageDefinition()));
        CertCampaignTypeUtil.checkStageDefinitionConsistency(newCampaign.getStageDefinition());

        newCampaign.setReviewStrategy(definition.getReviewStrategy());

        newCampaign.setStartTimestamp(null);
        newCampaign.setEndTimestamp(null);
        newCampaign.setState(CREATED);
        newCampaign.setStageNumber(0);
        newCampaign.setIteration(1);

        return newCampaign;
    }

	<O extends ObjectType> AccessCertificationCampaignType createAdHocCampaignObject(
			AccessCertificationDefinitionType definition, PrismObject<O> focus, Task task,
			OperationResult result) throws SecurityViolationException, SchemaException {
    	definition.setName(PolyStringType.fromOrig(PolyString.getOrig(definition.getName()) + " " + PolyString.getOrig(focus.getName())));
    	definition.setLastCampaignIdUsed(null);
		AccessCertificationCampaignType campaign = createCampaignObject(definition, task, result);
		AccessCertificationObjectBasedScopeType scope;
		if ((campaign.getScopeDefinition() instanceof AccessCertificationObjectBasedScopeType)) {
			scope = (AccessCertificationObjectBasedScopeType) campaign.getScopeDefinition();
		} else {
			// TODO!
			scope = new AccessCertificationAssignmentReviewScopeType(prismContext);
			campaign.setScopeDefinition(scope);
		}
		Class<? extends ObjectType> focusClass = focus.asObjectable().getClass();
		scope.setObjectType(ObjectTypes.getObjectType(focusClass).getTypeQName());
		ObjectFilter objectFilter = QueryBuilder.queryFor(focusClass, prismContext).id(focus.getOid()).buildFilter();
		scope.setSearchFilter(QueryConvertor.createSearchFilterType(objectFilter, prismContext));
		return campaign;
	}

	private PolyStringType generateCampaignName(AccessCertificationDefinitionType definition, Task task, OperationResult result) throws SchemaException {
        String prefix = definition.getName().getOrig();
        Integer lastCampaignIdUsed = definition.getLastCampaignIdUsed() != null ? definition.getLastCampaignIdUsed() : 0;
        for (int i = lastCampaignIdUsed+1;; i++) {
            String name = generateName(prefix, i);
            if (!campaignExists(name, result)) {
                recordLastCampaignIdUsed(definition.getOid(), i, task, result);
                return new PolyStringType(name);
            }
        }
    }

    private boolean campaignExists(String name, OperationResult result) throws SchemaException {
        ObjectQuery query = ObjectQueryUtil.createNameQuery(AccessCertificationCampaignType.class, prismContext, name);
        SearchResultList<PrismObject<AccessCertificationCampaignType>> existingCampaigns =
                repositoryService.searchObjects(AccessCertificationCampaignType.class, query, null, result);
        return !existingCampaigns.isEmpty();
    }

    private String generateName(String prefix, int i) {
        return prefix + " " + i;
    }

    private void recordLastCampaignIdUsed(String definitionOid, int lastIdUsed, Task task, OperationResult result) {
        try {
            List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationDefinitionType.class, prismContext)
                    .item(F_LAST_CAMPAIGN_ID_USED).replace(lastIdUsed)
                    .asItemDeltas();
            modifyObjectPreAuthorized(AccessCertificationDefinitionType.class, definitionOid, modifications, task, result);
        } catch (SchemaException|ObjectNotFoundException|RuntimeException|ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update last campaign ID for definition {}", e, definitionOid);
        }
    }

    //endregion

    //region ================================ Stage open ================================

	void openNextStage(AccessCertificationCampaignType campaign, CertificationHandler handler, Task task,
			OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
		boolean skipEmptyStages = campaign.getIteration() > 1;        // TODO make configurable
		int requestedStageNumber = campaign.getStageNumber() + 1;
		for (;;) {
			Holder<Integer> workItemsCreatedHolder = new Holder<>(0);
			AccessCertificationStageType stage = createStage(campaign, requestedStageNumber);
			ModificationsToExecute modifications = getDeltasForStageOpen(campaign, stage, handler, workItemsCreatedHolder, task, result);
			if (!skipEmptyStages || workItemsCreatedHolder.getValue() != 0) {
				modifyCampaignPreAuthorized(campaign.getOid(), modifications, task, result);
				afterStageOpen(campaign.getOid(), stage, task, result);       // notifications, bookkeeping, ...
				return;
			}
			LOGGER.debug("No work items created, skipping to the next stage");
			requestedStageNumber++;
			if (requestedStageNumber > CertCampaignTypeUtil.getNumberOfStages(campaign)) {
				result.recordWarning("No more (non-empty) stages available");
				return;
			}
		}
	}

	private ModificationsToExecute getDeltasForStageOpen(AccessCertificationCampaignType campaign,
			AccessCertificationStageType stage, CertificationHandler handler,
			Holder<Integer> workItemsCreatedHolder, final Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        int stageNumber = campaign.getStageNumber();
        int newStageNumber = stageNumber + 1;

	    LOGGER.trace("getDeltasForStageOpen starting; campaign = {}, stage number = {}", ObjectTypeUtil.toShortStringLazy(campaign), stageNumber);

	    ModificationsToExecute rv = new ModificationsToExecute();
        if (stageNumber == 0 && campaign.getIteration() == 1) {
            caseHelper.getDeltasToCreateCases(campaign, stage, handler, rv, workItemsCreatedHolder, task, result);
        } else {
            caseHelper.getDeltasToAdvanceCases(campaign, stage, rv, workItemsCreatedHolder, task, result);
        }
		rv.createNewBatch();
        rv.add(createStageAddDelta(stage));
        rv.add(createDeltasToRecordStageOpen(campaign, stage));
		rv.add(getDeltasToCreateTriggersForTimedActions(campaign.getOid(), 0,
				XmlTypeConverter.toDate(stage.getStartTimestamp()), XmlTypeConverter.toDate(stage.getDeadline()),
				CertCampaignTypeUtil.findStageDefinition(campaign, newStageNumber).getTimedActions()));

		if (LOGGER.isTraceEnabled()) {
			List<ItemDelta<?, ?>> allDeltas = rv.getAllDeltas();
			LOGGER.trace("getDeltasForStageOpen finishing, returning {} deltas (in {} batches):\n{}",
					allDeltas.size(), rv.batches.size(), DebugUtil.debugDump(allDeltas));
		}
        return rv;
    }

    // some bureaucracy... stage#, state, start time, triggers
    private List<ItemDelta<?,?>> createDeltasToRecordStageOpen(AccessCertificationCampaignType campaign,
		    AccessCertificationStageType newStage) throws SchemaException {

        List<ItemDelta<?,?>> itemDeltaList = new ArrayList<>();

        itemDeltaList.add(createStageNumberDelta(newStage.getNumber()));
        itemDeltaList.add(createStateDelta(IN_REVIEW_STAGE));

        boolean campaignJustStarted = newStage.getNumber() == 1;
        if (campaignJustStarted) {
            itemDeltaList.add(createStartTimeDelta(clock.currentTimeXMLGregorianCalendar()));
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

            final TriggerType triggerClose = new TriggerType(prismContext);
            triggerClose.setHandlerUri(AccessCertificationCloseStageTriggerHandler.HANDLER_URI);
            triggerClose.setTimestamp(stageDeadline);
            triggerClose.setId(lastId);
            triggers.add(triggerClose);

            for (Duration beforeDeadline : stageDef.getNotifyBeforeDeadline()) {
                final XMLGregorianCalendar beforeEnd = CloneUtil.clone(stageDeadline);
                beforeEnd.add(beforeDeadline.negate());
                if (XmlTypeConverter.toMillis(beforeEnd) > System.currentTimeMillis()) {
                    final TriggerType triggerBeforeEnd = new TriggerType(prismContext);
                    triggerBeforeEnd.setHandlerUri(AccessCertificationCloseStageApproachingTriggerHandler.HANDLER_URI);
                    triggerBeforeEnd.setTimestamp(beforeEnd);
                    triggerBeforeEnd.setId(++lastId);
                    triggers.add(triggerBeforeEnd);
                }
            }

            ContainerDelta<TriggerType> triggerDelta = ContainerDelta.createModificationReplace(ObjectType.F_TRIGGER, AccessCertificationCampaignType.class, prismContext, triggers);
            itemDeltaList.add(triggerDelta);
        }
        return itemDeltaList;
    }

    private AccessCertificationStageType createStage(AccessCertificationCampaignType campaign, int requestedStageNumber) {
        AccessCertificationStageType stage = new AccessCertificationStageType(prismContext);
        stage.setIteration(campaign.getIteration());
        stage.setNumber(requestedStageNumber);
        stage.setStartTimestamp(clock.currentTimeXMLGregorianCalendar());

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

	private void afterStageOpen(String campaignOid, AccessCertificationStageType newStage, Task task,
			OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        // notifications
        final AccessCertificationCampaignType campaign = generalHelper.getCampaign(campaignOid, null, task, result);
        if (campaign.getStageNumber() == 1) {
            eventHelper.onCampaignStart(campaign, task, result);
        }
        eventHelper.onCampaignStageStart(campaign, task, result);

		notifyReviewers(campaign, false, task, result);

        if (newStage.getNumber() == 1 && campaign.getIteration() == 1 && campaign.getDefinitionRef() != null) {
            List<ItemDelta<?,?>> deltas = DeltaBuilder.deltaFor(AccessCertificationDefinitionType.class, prismContext)
                    .item(F_LAST_CAMPAIGN_STARTED_TIMESTAMP).replace(clock.currentTimeXMLGregorianCalendar())
                    .asItemDeltas();
            modifyObjectPreAuthorized(AccessCertificationDefinitionType.class, campaign.getDefinitionRef().getOid(), deltas, task, result);
        }
    }

	private void notifyReviewers(AccessCertificationCampaignType campaign, boolean unansweredOnly, Task task, OperationResult result) throws SchemaException {
		List<AccessCertificationCaseType> caseList = queryHelper.getAllCurrentIterationCases(campaign.getOid(), campaign.getIteration(), null, result);
		Collection<String> reviewers = CertCampaignTypeUtil.getActiveReviewers(caseList);
		for (String reviewerOid : reviewers) {
			List<AccessCertificationCaseType> cases = queryHelper.getOpenCasesForReviewer(campaign, reviewerOid, result);
			boolean notify = !unansweredOnly ||
					cases.stream()
							.flatMap(c -> c.getWorkItem().stream())
							.anyMatch(wi -> ObjectTypeUtil.containsOid(wi.getAssigneeRef(), reviewerOid) &&
											(wi.getOutput() == null || wi.getOutput().getOutcome() == null));
			if (notify) {
				ObjectReferenceType actualReviewerRef = ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER);
				for (ObjectReferenceType reviewerOrDeputyRef : getReviewerAndDeputies(actualReviewerRef, task, result)) {
					eventHelper.onReviewRequested(reviewerOrDeputyRef, actualReviewerRef, cases, campaign, task, result);
				}
			}
		}
	}

	@NotNull
	List<ObjectReferenceType> getReviewerAndDeputies(ObjectReferenceType actualReviewerRef, Task task,
			OperationResult result) throws SchemaException {
		List<ObjectReferenceType> reviewerOrDeputiesRef = new ArrayList<>();
		reviewerOrDeputiesRef.add(actualReviewerRef);
		reviewerOrDeputiesRef.addAll(modelInteractionService.getDeputyAssignees(actualReviewerRef, OtherPrivilegesLimitationType.F_CERTIFICATION_WORK_ITEMS, task, result));
		return reviewerOrDeputiesRef;
	}

	//endregion
    //region ================================ Delegation/escalation ================================

    void delegateWorkItems(String campaignOid, List<AccessCertificationWorkItemType> workItems,
		    DelegateWorkItemActionType delegateAction, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException,
		    SecurityViolationException {
        LOGGER.info("Going to delegate {} work item(s) in campaign {}", workItems.size(), campaignOid);

		MidPointPrincipal principal = securityContextManager.getPrincipal();
		result.addContext("user", toShortString(principal.getUser()));
		ObjectReferenceType initiator = ObjectTypeUtil.createObjectRef(principal.getUser());
		ObjectReferenceType attorney = ObjectTypeUtil.createObjectRef(principal.getAttorney());

        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        List<ItemDelta<?, ?>> deltas = new ArrayList<>();
		for (AccessCertificationWorkItemType workItem : workItems) {
			AccessCertificationCaseType aCase = CertCampaignTypeUtil.getCaseChecked(workItem);
			AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaignChecked(aCase);
			if (!java.util.Objects.equals(campaign.getOid(), campaignOid)) {
				throw new IllegalArgumentException("Work item to delegate does not belong to specified campaign (" + campaignOid + ") but to " + campaign);
			}
			// TODO reload the work item here (and replace exceptions with logged warnings)
			if (workItem.getCloseTimestamp() != null) {
				throw new IllegalStateException("Couldn't delegate a work item that is already closed: " + workItem);
			}
			// actually, stage/iteration should match, as the work item is not closed
			if (workItem.getIteration() != campaign.getIteration()) {
				throw new IllegalStateException("Couldn't delegate a work item that is not in a current iteration. Current iteration: " + campaign.getIteration() + ", work item iteration: " + workItem.getIteration());
			}
			if (workItem.getStageNumber() != campaign.getStageNumber()) {
				throw new IllegalStateException("Couldn't delegate a work item that is not in a current stage. Current stage: " + campaign.getStageNumber() + ", work item stage: " + workItem.getStageNumber());
			}
			List<ObjectReferenceType> delegates = computeDelegateTo(delegateAction, workItem, aCase, campaign, task, result);

			WorkItemEventCauseInformationType causeInformation = null;			// TODO

			//noinspection ConstantConditions
			LOGGER.trace("Delegating work item {} to {}: cause={}", workItem, delegates, causeInformation);
			List<ObjectReferenceType> assigneesBefore = CloneUtil.cloneCollectionMembers(workItem.getAssigneeRef());
			WorkItemDelegationMethodType method = getDelegationMethod(delegateAction);
			List<ObjectReferenceType> newAssignees = new ArrayList<>();
			List<ObjectReferenceType> delegatedTo = new ArrayList<>();
			WfContextUtil.computeAssignees(newAssignees, delegatedTo, delegates, method, workItem);
			//noinspection ConstantConditions
			WorkItemDelegationEventType event = WfContextUtil.createDelegationEvent(null, assigneesBefore, delegatedTo, method, causeInformation);
			event.setTimestamp(now);
			event.setInitiatorRef(initiator);
			event.setAttorneyRef(attorney);
			event.setWorkItemId(workItem.getId());
			event.setEscalationLevel(workItem.getEscalationLevel());
			event.setStageNumber(campaign.getStageNumber());
			event.setIteration(campaign.getIteration());
			addDeltasForNewAssigneesAndEvent(deltas, workItem, aCase, newAssignees, event);

			// notification (after modifications)
		}
		modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaignOid, deltas, task, result);

		// TODO notifications

//		AccessCertificationCampaignType updatedCampaign = refreshCampaign(campaign, task, result);
//		LOGGER.info("Updated campaign state: {}", updatedCampaign.getState());
//		eventHelper.onCampaignEnd(updatedCampaign, task, result);

    }

	@NotNull
	private WorkItemDelegationMethodType getDelegationMethod(DelegateWorkItemActionType delegateAction) {
		return defaultIfNull(delegateAction.getDelegationMethod(), WorkItemDelegationMethodType.REPLACE_ASSIGNEES);
	}

	public void escalateCampaign(String campaignOid, EscalateWorkItemActionType escalateAction,
			WorkItemEventCauseInformationType causeInformation, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException {
		MidPointPrincipal principal = securityContextManager.getPrincipal();
		result.addContext("user", toShortString(principal.getUser()));
		ObjectReferenceType initiator = ObjectTypeUtil.createObjectRef(principal.getUser());
		ObjectReferenceType attorney = ObjectTypeUtil.createObjectRef(principal.getAttorney());

		List<AccessCertificationWorkItemType> workItems = queryHelper.searchOpenWorkItems(
				CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid, prismContext),
				null, false, null, result);

		if (workItems.isEmpty()) {
			LOGGER.debug("No work items, no escalation (campaign: {})", campaignOid);
			return;
		}

		LOGGER.info("Going to escalate the campaign {}: {} work item(s)", campaignOid, workItems.size());

		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        ModificationsToExecute modifications = new ModificationsToExecute();
        // Currently we expect all open certification work items for a given campaign to have the same escalation level.
		// Because of consistence with other parts of midPoint we store the escalation level within work item itself.
		// But we enforce it to be the same for all the open work items.
		// This behavior will most probably change in the future.
		AccessCertificationCampaignType campaign = generalHelper.getCampaign(campaignOid, null, task, result);
		int newStageEscalationLevelNumber = CertCampaignTypeUtil.getCurrentStageEscalationLevelNumber(campaign) + 1;
		WorkItemEscalationLevelType newEscalationLevel = new WorkItemEscalationLevelType()
				.number(newStageEscalationLevelNumber)
				.name(escalateAction.getEscalationLevelName())
				.displayName(escalateAction.getEscalationLevelDisplayName());
		for (AccessCertificationWorkItemType workItem : workItems) {
			AccessCertificationCaseType aCase = CertCampaignTypeUtil.getCaseChecked(workItem);
			AccessCertificationCampaignType workItemCampaign = CertCampaignTypeUtil.getCampaignChecked(aCase);
			if (!java.util.Objects.equals(workItemCampaign.getOid(), campaignOid)) {
				throw new IllegalArgumentException("Work item to delegate does not belong to specified campaign (" + campaignOid + ") but to " + workItemCampaign);
			}
			if (workItem.getCloseTimestamp() != null) {
				throw new IllegalStateException("Couldn't delegate a work item that is already closed: " + workItem);
			}
			if (workItem.getStageNumber() != workItemCampaign.getStageNumber()) {
				throw new IllegalStateException("Couldn't delegate a work item that is not in a current stage. Current stage: " + workItemCampaign.getStageNumber() + ", work item stage: " + workItem.getStageNumber());
			}
			if (workItem.getIteration() != workItemCampaign.getIteration()) {
				throw new IllegalStateException("Couldn't delegate a work item that is not in a current iteration. Current stage: " + workItemCampaign.getIteration() + ", work item iteration: " + workItem.getIteration());
			}
			if (workItem.getOutput() != null && workItem.getOutput().getOutcome() != null) {
				// It is a bit questionable to skip this work item (as it is not signed off),
				// but it is also not quite OK to escalate it, as there's some output present.
				// The latter is less awkward, so let's do it that way.
				continue;
			}
			List<ObjectReferenceType> delegates = computeDelegateTo(escalateAction, workItem, aCase, workItemCampaign, task, result);

			int escalationLevel = WfContextUtil.getEscalationLevelNumber(workItem);
			if (escalationLevel + 1 != newStageEscalationLevelNumber) {
				throw new IllegalStateException("Different escalation level numbers for certification cases: work item level ("
						+ newEscalationLevel + ") is different from the stage level (" + newStageEscalationLevelNumber + ")");
			}
			LOGGER.debug("Escalating work item {} to level: {}; delegates={}: cause={}", workItem, newEscalationLevel, delegates, causeInformation);

			List<ObjectReferenceType> assigneesBefore = CloneUtil.cloneCollectionMembers(workItem.getAssigneeRef());
			WorkItemDelegationMethodType method = getDelegationMethod(escalateAction);
			List<ObjectReferenceType> newAssignees = new ArrayList<>();
			List<ObjectReferenceType> delegatedTo = new ArrayList<>();
			WfContextUtil.computeAssignees(newAssignees, delegatedTo, delegates, method, workItem);
			WorkItemDelegationEventType event = WfContextUtil.createDelegationEvent(newEscalationLevel, assigneesBefore, delegatedTo, method, causeInformation);
			event.setTimestamp(now);
			event.setInitiatorRef(initiator);
			event.setAttorneyRef(attorney);
			event.setWorkItemId(workItem.getId());
			event.setEscalationLevel(workItem.getEscalationLevel());
			event.setStageNumber(campaign.getStageNumber());
			event.setIteration(campaign.getIteration());
			List<ItemDelta<?, ?>> deltas = new ArrayList<>();
			addDeltasForNewAssigneesAndEvent(deltas, workItem, aCase, newAssignees, event);
			deltas.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
					.item(F_CASE, aCase.getId(), F_WORK_ITEM, workItem.getId(), F_ESCALATION_LEVEL).replace(newEscalationLevel)
					.asItemDelta());
			modifications.add(deltas);
			// notification (after modifications)
		}
		AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
		assert stage != null;
		Long stageId = stage.asPrismContainerValue().getId();
		assert stageId != null;
		modifications.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(F_STAGE, stageId, AccessCertificationStageType.F_ESCALATION_LEVEL).replace(newEscalationLevel)
				.asItemDelta());
		AccessCertificationStageDefinitionType stageDefinition = CertCampaignTypeUtil.getCurrentStageDefinition(campaign);
		modifications.add(getDeltasToCreateTriggersForTimedActions(campaignOid, newStageEscalationLevelNumber,
				XmlTypeConverter.toDate(stage.getStartTimestamp()), XmlTypeConverter.toDate(stage.getDeadline()), stageDefinition.getTimedActions()));

		modifyCampaignPreAuthorized(campaignOid, modifications, task, result);

		campaign = generalHelper.getCampaign(campaignOid, null, task, result);
		// TODO differentiate between "old" and "new" reviewers
		notifyReviewers(campaign, true, task, result);

		//		AccessCertificationCampaignType updatedCampaign = refreshCampaign(campaign, task, result);
//		LOGGER.info("Updated campaign state: {}", updatedCampaign.getState());
//		eventHelper.onCampaignEnd(updatedCampaign, task, result);

    }

	private void addDeltasForNewAssigneesAndEvent(List<ItemDelta<?, ?>> deltas, AccessCertificationWorkItemType workItem,
			AccessCertificationCaseType aCase, List<ObjectReferenceType> newAssignees, WorkItemDelegationEventType event)
			throws SchemaException {
		deltas.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(F_CASE, aCase.getId(), F_WORK_ITEM, workItem.getId(), F_ASSIGNEE_REF)
					.replace(PrismReferenceValue.asReferenceValues(newAssignees))
				.asItemDelta());
		deltas.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(F_CASE, aCase.getId(), F_EVENT).add(event)
				.asItemDelta());
	}

	private List<ObjectReferenceType> computeDelegateTo(DelegateWorkItemActionType delegateAction,
			AccessCertificationWorkItemType workItem, AccessCertificationCaseType aCase,
			AccessCertificationCampaignType campaign, Task task, OperationResult result) {

		List<ObjectReferenceType> rv = CloneUtil.cloneCollectionMembers(delegateAction.getApproverRef());
        if (!delegateAction.getApproverExpression().isEmpty()) {
            ExpressionVariables variables = new ExpressionVariables();
            variables.addVariableDefinition(ExpressionConstants.VAR_WORK_ITEM, workItem);
			variables.addVariableDefinition(ExpressionConstants.VAR_CERTIFICATION_CASE, aCase);
			variables.addVariableDefinition(ExpressionConstants.VAR_CAMPAIGN, campaign);
			for (ExpressionType expression : delegateAction.getApproverExpression()) {
				rv.addAll(expressionHelper.evaluateRefExpressionChecked(expression, variables, "computing delegates", task, result));
			}
        }
        return rv;
    }

	//endregion
	//region ================================ Triggers ================================

	// see also MidpointUtil.createTriggersForTimedActions (in workflow-impl)
	@NotNull
	private List<ItemDelta<?, ?>> getDeltasToCreateTriggersForTimedActions(String campaignOid, int escalationLevel, Date workItemCreateTime,
			Date workItemDeadline, List<WorkItemTimedActionsType> timedActionsList) {
		LOGGER.trace("Creating triggers for timed actions for certification campaign {}, escalation level {}, create time {}, deadline {}, {} timed action(s)",
				campaignOid, escalationLevel, workItemCreateTime, workItemDeadline, timedActionsList.size());
		try {
			List<TriggerType> triggers = WfContextUtil.createTriggers(escalationLevel, workItemCreateTime, workItemDeadline,
					timedActionsList, prismContext, LOGGER, null, AccCertTimedActionTriggerHandler.HANDLER_URI);
			LOGGER.trace("Created {} triggers for campaign {}:\n{}", triggers.size(), campaignOid, PrismUtil.serializeQuietlyLazily(prismContext, triggers));
			if (triggers.isEmpty()) {
				return Collections.emptyList();
			} else {
				return DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
						.item(TaskType.F_TRIGGER).add(PrismContainerValue.toPcvList(triggers))
						.asItemDeltas();
			}
		} catch (SchemaException | RuntimeException e) {
			throw new SystemException("Couldn't create deltas for creating trigger(s) for campaign " + campaignOid + ": " + e.getMessage(), e);
		}
	}

	//endregion

	//region ================================ Campaign iteration ================================

    void reiterateCampaign(AccessCertificationCampaignType campaign, Task task, OperationResult result)
		    throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
	    LOGGER.info("Reiterating campaign {}", ObjectTypeUtil.toShortString(campaign));
	    if (campaign.getState() != CLOSED) {
		    throw new IllegalStateException("Campaign is not in CLOSED state");
	    }
		ModificationsToExecute modifications = new ModificationsToExecute();
        modifications.add(createStageNumberDelta(0));
        modifications.add(createStateDelta(CREATED));
        modifications.add(createTriggerDeleteDelta());
		modifications.add(createStartTimeDelta(null));
		modifications.add(createEndTimeDelta(null));
		int newIteration = campaign.getIteration() + 1;
		modifications.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(AccessCertificationCampaignType.F_ITERATION).replace(newIteration)
				.asItemDelta());

	    createCasesReiterationDeltas(campaign, newIteration, modifications, result);

        modifyCampaignPreAuthorized(campaign.getOid(), modifications, task, result);
    }

	private void createCasesReiterationDeltas(AccessCertificationCampaignType campaign, int newIteration,
			ModificationsToExecute modifications, OperationResult result) throws SchemaException {
		ObjectQuery unresolvedCasesQuery = QueryBuilder.queryFor(AccessCertificationCaseType.class, prismContext)
				.item(AccessCertificationCaseType.F_OUTCOME).eq(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE)
				.build();
		List<AccessCertificationCaseType> unresolvedCases = queryHelper
				.searchCases(campaign.getOid(), unresolvedCasesQuery, null, result);
		for (AccessCertificationCaseType aCase : unresolvedCases) {
			modifications.add(
					DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
							.item(F_CASE, aCase.getId(), F_ITERATION).replace(newIteration)
							.item(F_CASE, aCase.getId(), F_STAGE_NUMBER).replace(0)
							.item(F_CASE, aCase.getId(), F_CURRENT_STAGE_OUTCOME).replace()
							.item(F_CASE, aCase.getId(), F_CURRENT_STAGE_DEADLINE).replace()
							.item(F_CASE, aCase.getId(), F_CURRENT_STAGE_CREATE_TIMESTAMP).replace()
							.asItemDeltas());
		}
	}

	//endregion

	//region ================================ Campaign and stage close ================================

	void closeCampaign(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
		LOGGER.info("Closing campaign {}", ObjectTypeUtil.toShortString(campaign));
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		int lastStageNumber = CertCampaignTypeUtil.getNumberOfStages(campaign);
		// TODO issue a warning if we are not in a correct state
		ModificationsToExecute modifications = new ModificationsToExecute();
		modifications.add(createStageNumberDelta(lastStageNumber + 1));
		modifications.add(createStateDelta(CLOSED));
		modifications.add(createTriggerDeleteDelta());
		modifications.add(createEndTimeDelta(now));
		createWorkItemsCloseDeltas(campaign, modifications, now, result);

		modifyCampaignPreAuthorized(campaign.getOid(), modifications, task, result);

		AccessCertificationCampaignType updatedCampaign = refreshCampaign(campaign, result);
		eventHelper.onCampaignEnd(updatedCampaign, task, result);

		if (campaign.getDefinitionRef() != null) {
			List<ItemDelta<?,?>> definitionDeltas = DeltaBuilder.deltaFor(AccessCertificationDefinitionType.class, prismContext)
					.item(F_LAST_CAMPAIGN_CLOSED_TIMESTAMP).replace(now)
					.asItemDeltas();
			modifyObjectPreAuthorized(AccessCertificationDefinitionType.class, campaign.getDefinitionRef().getOid(), definitionDeltas, task, result);
		}
	}

	private void createWorkItemsCloseDeltas(AccessCertificationCampaignType campaign, ModificationsToExecute modifications,
			XMLGregorianCalendar now, OperationResult result) throws SchemaException {
		ObjectQuery query = CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaign.getOid(), prismContext);
		List<AccessCertificationWorkItemType> openWorkItems = queryHelper.searchOpenWorkItems(query, null, false, null, result);
		LOGGER.debug("There are {} open work items for {}", openWorkItems.size(), ObjectTypeUtil.toShortString(campaign));
		for (AccessCertificationWorkItemType workItem : openWorkItems) {
			AccessCertificationCaseType aCase = CertCampaignTypeUtil.getCaseChecked(workItem);
			modifications.add(
					DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
							.item(F_CASE, aCase.getId(), F_WORK_ITEM, workItem.getId(), F_CLOSE_TIMESTAMP)
							.replace(now)
							.asItemDelta());
		}
	}

	ModificationsToExecute getDeltasForStageClose(AccessCertificationCampaignType campaign, OperationResult result) throws
			SchemaException {
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		ModificationsToExecute rv = new ModificationsToExecute();
		caseHelper.createOutcomeAndEventDeltasForCasesOnStageClose(campaign, rv, result);
		rv.createNewBatch();
		createWorkItemsCloseDeltas(campaign, rv, now, result);
		rv.createNewBatch();
		rv.add(createStateDelta(REVIEW_STAGE_DONE));
		rv.add(createStageEndTimeDelta(campaign, now));
		rv.add(createTriggerDeleteDelta());
		return rv;
	}

	private ItemDelta createStageEndTimeDelta(AccessCertificationCampaignType campaign, XMLGregorianCalendar now) throws SchemaException {
		AccessCertificationStageType stage = CertCampaignTypeUtil.findStage(campaign, campaign.getStageNumber());
		Long stageId = stage.asPrismContainerValue().getId();
		assert stageId != null;
		return DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(F_STAGE, stageId, F_END_TIMESTAMP).replace(now)
				.asItemDelta();
	}

	void afterStageClose(String campaignOid, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
		final AccessCertificationCampaignType campaign = generalHelper.getCampaign(campaignOid, null, task, result);
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
		LOGGER.info("Starting cleanup for closed certification campaigns, keeping {} ones.", maxRecords);
		int deleted = 0;
		Set<String> poisonedCampaigns = new HashSet<>();
		try {
			for (;;) {
				ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCampaignType.class, prismContext)
						.item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CLOSED)
						.and().not().id(poisonedCampaigns.toArray(new String[0]))   // hoping there are not many of these
						.desc(AccessCertificationCampaignType.F_END_TIMESTAMP)
						.offset(maxRecords)
						.maxSize(DELETE_BLOCK_SIZE)
						.build();
				int delta = searchAndDeleteCampaigns(query, poisonedCampaigns, result, task);
				if (delta == 0) {
					LOGGER.info("Deleted {} campaigns.", deleted);
					return;
				}
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

		LOGGER.info("Starting cleanup for closed certification campaigns deleting up to {} (max age '{}').", deleteCampaignsFinishedUpTo, maxAge);

		OperationResult result = parentResult.createSubresult(OPERATION_CLEANUP_CAMPAIGNS_BY_AGE);
		XMLGregorianCalendar timeXml = createXMLGregorianCalendar(deleteCampaignsFinishedUpTo);
		int deleted = 0;
		Set<String> poisonedCampaigns = new HashSet<>();
		try {
			for (;;) {
				ObjectQuery query = QueryBuilder.queryFor(AccessCertificationCampaignType.class, prismContext)
						.item(AccessCertificationCampaignType.F_STATE).eq(AccessCertificationCampaignStateType.CLOSED)
						.and().item(AccessCertificationCampaignType.F_END_TIMESTAMP).lt(timeXml)
						.and().not().id(poisonedCampaigns.toArray(new String[0]))   // hoping there are not many of these
						.maxSize(DELETE_BLOCK_SIZE)
						.build();
				int delta = searchAndDeleteCampaigns(query, poisonedCampaigns, result, task);
				if (delta == 0) {
					LOGGER.info("Deleted {} campaigns.", deleted);
					return;
				}
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
			OperationResult subresult = result.createMinorSubresult(OPERATION_DELETE_OBSOLETE_CAMPAIGN);
			try {
				LOGGER.debug("Deleting campaign {}", campaign);
				ObjectDelta<AccessCertificationCampaignType> deleteDelta = new ObjectDelta<>(
						AccessCertificationCampaignType.class, ChangeType.DELETE, prismContext);
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

	//endregion

	//region ================================ Auxiliary methods for delta processing ================================

    @SuppressWarnings("SameParameterValue")
    List<ItemDelta<?,?>> createDeltasForStageNumberAndState(int number, AccessCertificationCampaignStateType state) {
        List<ItemDelta<?,?>> rv = new ArrayList<>();
        rv.add(createStageNumberDelta(number));
        rv.add(createStateDelta(state));
        return rv;
    }

    private PropertyDelta<Integer> createStageNumberDelta(int number) {
        return PropertyDelta.createReplaceDelta(generalHelper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_STAGE_NUMBER, number);
    }

    private PropertyDelta<AccessCertificationCampaignStateType> createStateDelta(AccessCertificationCampaignStateType state) {
        return PropertyDelta.createReplaceDelta(generalHelper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_STATE, state);
    }

    private ItemDelta<?, ?> createStartTimeDelta(XMLGregorianCalendar date) throws SchemaException {
		return DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(AccessCertificationCampaignType.F_START_TIMESTAMP).replace(date)
				.asItemDelta();
    }

	private ItemDelta<?, ?> createEndTimeDelta(XMLGregorianCalendar date) throws SchemaException {
		return DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(AccessCertificationCampaignType.F_END_TIMESTAMP).replace(date)
				.asItemDelta();
	}

    private ContainerDelta createTriggerDeleteDelta() {
        return ContainerDelta.createModificationReplace(ObjectType.F_TRIGGER, generalHelper.getCampaignObjectDefinition());
    }

    private ItemDelta createStageAddDelta(AccessCertificationStageType stage) throws SchemaException {
		return DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(F_STAGE).add(stage)
				.asItemDelta();
    }

    //endregion

    //region ================================ Model and repository operations ================================

    void addObjectPreAuthorized(ObjectType objectType, Task task, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        ObjectDelta<? extends ObjectType> objectDelta = ObjectDelta.createAddDelta(objectType.asPrismObject());
        Collection<ObjectDeltaOperation<? extends ObjectType>> ops;
        try {
            ops = modelService.executeChanges(
					singleton(objectDelta),
                    ModelExecuteOptions.createRaw().setPreAuthorized(), task, result);
        } catch (ExpressionEvaluationException|CommunicationException|ConfigurationException|PolicyViolationException|SecurityViolationException e) {
            throw new SystemException("Unexpected exception when adding object: " + e.getMessage(), e);
        }
        ObjectDeltaOperation odo = ops.iterator().next();
        objectType.setOid(odo.getObjectDelta().getOid());

        /* ALTERNATIVELY, we can go directly into the repository. (No audit there.)
        String oid = repositoryService.addObject(objectType.asPrismObject(), null, result);
        objectType.setOid(oid);
         */
    }

	void modifyCampaignPreAuthorized(String campaignOid, ModificationsToExecute modifications, Task task, OperationResult result)
			throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
		for (List<ItemDelta<?, ?>> batch : modifications.batches) {
			if (!batch.isEmpty()) {
				LOGGER.trace("Applying {} changes to campaign {}", batch.size(), campaignOid);
				modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaignOid, batch, task, result);
			}
		}
	}

	<T extends ObjectType> void modifyObjectPreAuthorized(Class<T> objectClass, String oid, Collection<ItemDelta<?,?>> itemDeltas, Task task, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        ObjectDelta<T> objectDelta = ObjectDelta.createModifyDelta(oid, itemDeltas, objectClass, prismContext);
        try {
            ModelExecuteOptions options = ModelExecuteOptions.createRaw().setPreAuthorized();
            modelService.executeChanges(Collections.singletonList(objectDelta), options, task, result);
        } catch (SecurityViolationException|ExpressionEvaluationException|CommunicationException|ConfigurationException|PolicyViolationException e) {
            throw new SystemException("Unexpected exception when modifying " + objectClass.getSimpleName() + " " + oid + ": " + e.getMessage(), e);
        }
    }

//    <T extends ObjectType> void modifyObject(Class<T> objectClass, String oid, Collection<ItemDelta> itemDeltas, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
//        repositoryService.modifyObject(objectClass, oid, itemDeltas, result);
//    }

    // TODO implement more efficiently
    AccessCertificationCampaignType refreshCampaign(AccessCertificationCampaignType campaign,
		    OperationResult result) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(AccessCertificationCampaignType.class, campaign.getOid(), null, result).asObjectable();
    }
	//endregion
}
