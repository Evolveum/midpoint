/*
 * Copyright (c) 2010-2015 Evolveum
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
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.WfContextUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_ASSIGNEE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_ESCALATION_LEVEL;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_EVENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_WORK_ITEM;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_CLOSED_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_ID_USED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_STARTED_TIMESTAMP;

/**
 * @author mederly
 */
@Component
public class AccCertUpdateHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertUpdateHelper.class);

    @Autowired private AccCertEventHelper eventHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelService modelService;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private AccCertGeneralHelper generalHelper;
    @Autowired protected AccCertQueryHelper queryHelper;
    @Autowired private AccCertCaseOperationsHelper caseHelper;
    @Autowired private Clock clock;
    @Autowired private AccCertExpressionHelper expressionHelper;

    //region ================================ Campaign create ================================

    AccessCertificationCampaignType createCampaignObject(AccessCertificationDefinitionType definition,
                                                         Task task, OperationResult result) throws SecurityViolationException, SchemaException, ObjectNotFoundException {
        AccessCertificationCampaignType newCampaign = new AccessCertificationCampaignType(prismContext);

        if (definition.getName() != null) {
            newCampaign.setName(generateCampaignName(definition, task, result));
        } else {
            throw new SchemaException("Couldn't create a campaign without name");
        }

        newCampaign.setDescription(definition.getDescription());
        newCampaign.setOwnerRef(securityEnforcer.getPrincipal().toObjectReference());
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

        return newCampaign;
    }

    private PolyStringType generateCampaignName(AccessCertificationDefinitionType definition, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
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

    public void recordLastCampaignIdUsed(String definitionOid, int lastIdUsed, Task task, OperationResult result) {
        try {
            List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(AccessCertificationDefinitionType.class, prismContext)
                    .item(F_LAST_CAMPAIGN_ID_USED).replace(lastIdUsed)
                    .asItemDeltas();
            modifyObjectViaModel(AccessCertificationDefinitionType.class, definitionOid, modifications, task, result);
        } catch (SchemaException|ObjectNotFoundException|RuntimeException|ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update last campaign ID for definition {}", e, definitionOid);
        }
    }

    //endregion

    //region ================================ Stage open ================================

    public List<ItemDelta<?,?>> getDeltasForStageOpen(AccessCertificationCampaignType campaign, AccessCertificationStageType stage, CertificationHandler handler, final Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        Validate.notNull(campaign, "certificationCampaign");
        Validate.notNull(campaign.getOid(), "certificationCampaign.oid");

        int stageNumber = campaign.getStageNumber();
        int newStageNumber = stageNumber + 1;

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("getDeltasForStageOpen starting; campaign = {}, stage number = {}",
                    ObjectTypeUtil.toShortString(campaign), stageNumber);
        }

        List<ItemDelta<?,?>> rv = new ArrayList<>();
        if (stageNumber == 0) {
            rv.addAll(caseHelper.getDeltasToCreateCases(campaign, stage, handler, task, result));
        } else {
            rv.addAll(caseHelper.getDeltasToAdvanceCases(campaign, stage, task, result));
        }

        rv.add(createStageAddDelta(stage));
        rv.addAll(createDeltasToRecordStageOpen(campaign, stage));
		rv.addAll(createTriggersForTimedActions(campaign.getOid(), 0,
				XmlTypeConverter.toDate(stage.getStart()), XmlTypeConverter.toDate(stage.getDeadline()),
				CertCampaignTypeUtil.findStageDefinition(campaign, newStageNumber).getTimedActions()));

		LOGGER.trace("getDeltasForStageOpen finishing, returning {} deltas:\n{}", rv.size(), DebugUtil.debugDumpLazily(rv));
        return rv;
    }

    // some bureaucracy... stage#, state, start time, triggers
    List<ItemDelta<?,?>> createDeltasToRecordStageOpen(AccessCertificationCampaignType campaign,
			AccessCertificationStageType newStage) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        final List<ItemDelta<?,?>> itemDeltaList = new ArrayList<>();

        itemDeltaList.add(createStageNumberDelta(newStage.getNumber()));

        final PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(IN_REVIEW_STAGE);
        itemDeltaList.add(stateDelta);

        final boolean campaignJustCreated = newStage.getNumber() == 1;
        if (campaignJustCreated) {
            PropertyDelta<XMLGregorianCalendar> startDelta = createStartTimeDelta(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
            itemDeltaList.add(startDelta);
        }

        final XMLGregorianCalendar stageDeadline = newStage.getDeadline();
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

    protected AccessCertificationStageType createStage(AccessCertificationCampaignType campaign, int requestedStageNumber) {
        AccessCertificationStageType stage = new AccessCertificationStageType(prismContext);
        stage.setNumber(requestedStageNumber);
        stage.setStart(XmlTypeConverter.createXMLGregorianCalendar(new Date()));

        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, stage.getNumber());
		XMLGregorianCalendar deadline = computeDeadline(stage.getStart(), stageDef.getDuration(), stageDef.getDeadlineRounding());
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

	void afterStageOpen(String campaignOid, AccessCertificationStageType newStage, Task task,
                        OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        // notifications
        final AccessCertificationCampaignType campaign = generalHelper.getCampaign(campaignOid, null, task, result);
        if (campaign.getStageNumber() == 1) {
            eventHelper.onCampaignStart(campaign, task, result);
        }
        eventHelper.onCampaignStageStart(campaign, task, result);

        final List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaign.getOid(), null, null, result);
        Collection<String> reviewers = eventHelper.getCurrentActiveReviewers(caseList);
        for (String reviewerOid : reviewers) {
            final List<AccessCertificationCaseType> cases = queryHelper.getCasesForReviewer(campaign, reviewerOid, task, result);
            final ObjectReferenceType reviewerRef = ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER);
            eventHelper.onReviewRequested(reviewerRef, cases, campaign, task, result);
        }

        if (newStage.getNumber() == 1 && campaign.getDefinitionRef() != null) {
            List<ItemDelta<?,?>> deltas = DeltaBuilder.deltaFor(AccessCertificationDefinitionType.class, prismContext)
                    .item(F_LAST_CAMPAIGN_STARTED_TIMESTAMP).replace(XmlTypeConverter.createXMLGregorianCalendar(new Date()))
                    .asItemDeltas();
            modifyObjectViaModel(AccessCertificationDefinitionType.class, campaign.getDefinitionRef().getOid(), deltas, task, result);
        }
    }

    //endregion
    //region ================================ Delegation/escalation ================================

    public void delegateWorkItems(String campaignOid, List<AccessCertificationWorkItemType> workItems,
            DelegateWorkItemActionType delegateAction, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException,
			SecurityViolationException {
        LOGGER.info("Going to delegate {} work item(s) in campaign {}", workItems.size(), campaignOid);

		MidPointPrincipal principal = securityEnforcer.getPrincipal();
		result.addContext("user", toShortString(principal.getUser()));
		ObjectReferenceType initiator = ObjectTypeUtil.createObjectRef(principal.getUser());

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
			if (workItem.getStageNumber() != campaign.getStageNumber()) {
				throw new IllegalStateException("Couldn't delegate a work item that is not in a current stage. Current stage: " + campaign.getStageNumber() + ", work item stage: " + workItem.getStageNumber());
			}
			List<ObjectReferenceType> delegates = computeDelegateTo(delegateAction, workItem, aCase, campaign, task, result);

			WorkItemEventCauseInformationType causeInformation = null;			// TODO

			LOGGER.trace("Delegating work item {} to {}: cause={}", workItem, delegates, causeInformation);
			List<ObjectReferenceType> assigneesBefore = CloneUtil.cloneCollectionMembers(workItem.getAssigneeRef());
			WorkItemDelegationMethodType method = getDelegationMethod(delegateAction);
			List<ObjectReferenceType> newAssignees = new ArrayList<>();
			List<ObjectReferenceType> delegatedTo = new ArrayList<>();
			WfContextUtil.computeAssignees(newAssignees, delegatedTo, delegates, method, workItem);
			WorkItemDelegationEventType event = WfContextUtil.createDelegationEvent(null, assigneesBefore, delegatedTo, method, causeInformation);
			event.setTimestamp(now);
			event.setInitiatorRef(initiator);
			event.setWorkItemId(workItem.getId());
			event.setEscalationLevel(workItem.getEscalationLevel());

			addDeltasForAssigneesAndEvent(deltas, workItem, aCase, newAssignees, event);

			// notification (after modifications)
		}
		modifyObjectViaModel(AccessCertificationCampaignType.class, campaignOid, deltas, task, result);

//		AccessCertificationCampaignType updatedCampaign = refreshCampaign(campaign, task, result);
//		LOGGER.info("Updated campaign state: {}", updatedCampaign.getState());
//		eventHelper.onCampaignEnd(updatedCampaign, task, result);

    }

	@NotNull
	private WorkItemDelegationMethodType getDelegationMethod(DelegateWorkItemActionType delegateAction) {
		WorkItemDelegationMethodType method = delegateAction.getDelegationMethod();
		if (method == null) {
			method = WorkItemDelegationMethodType.REPLACE_ASSIGNEES;
		}
		return method;
	}

	public void escalateCampaign(String campaignOid, EscalateWorkItemActionType escalateAction,
			WorkItemEventCauseInformationType causeInformation, Task task, OperationResult result)
			throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, ExpressionEvaluationException,
			SecurityViolationException {
		MidPointPrincipal principal = securityEnforcer.getPrincipal();
		result.addContext("user", toShortString(principal.getUser()));
		ObjectReferenceType initiator = ObjectTypeUtil.createObjectRef(principal.getUser());

		List<AccessCertificationWorkItemType> workItems = queryHelper.searchWorkItems(
				CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid, prismContext),
				null, false, null, task, result);

		if (workItems.isEmpty()) {
			LOGGER.debug("No work items, no escalation (campaign: {})", campaignOid);
			return;
		}

		LOGGER.info("Going to escalate the campaign {}: {} work item(s)", campaignOid, workItems.size());

		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        List<ItemDelta<?, ?>> deltas = new ArrayList<>();
        // Currently we expect all open certification work items for a given campaign to have the same escalation level.
		// Because of consistence with other parts of midPoint we store the escalation level within work item itself.
		// But we enforce it to be the same for all the open work items.
		// This behavior will most probably change in the future.
        Integer newGlobalEscalationLevelNumber = null;
		for (AccessCertificationWorkItemType workItem : workItems) {
			AccessCertificationCaseType aCase = CertCampaignTypeUtil.getCaseChecked(workItem);
			AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaignChecked(aCase);
			if (!java.util.Objects.equals(campaign.getOid(), campaignOid)) {
				throw new IllegalArgumentException("Work item to delegate does not belong to specified campaign (" + campaignOid + ") but to " + campaign);
			}
			if (workItem.getCloseTimestamp() != null) {
				throw new IllegalStateException("Couldn't delegate a work item that is already closed: " + workItem);
			}
			if (workItem.getStageNumber() != campaign.getStageNumber()) {
				throw new IllegalStateException("Couldn't delegate a work item that is not in a current stage. Current stage: " + campaign.getStageNumber() + ", work item stage: " + workItem.getStageNumber());
			}
			List<ObjectReferenceType> delegates = computeDelegateTo(escalateAction, workItem, aCase, campaign, task, result);

			int escalationLevel = WfContextUtil.getEscalationLevelNumber(workItem);
			int newEscalationLevelNumber = escalationLevel + 1;
			WorkItemEscalationLevelType newEscalationLevel = new WorkItemEscalationLevelType()
					.number(newEscalationLevelNumber)
					.name(escalateAction.getEscalationLevelName())
					.displayName(escalateAction.getEscalationLevelDisplayName());
			if (newGlobalEscalationLevelNumber == null) {
				newGlobalEscalationLevelNumber = newEscalationLevelNumber;
			} else if (newGlobalEscalationLevelNumber != newEscalationLevelNumber) {
				throw new IllegalStateException("Different escalation level numbers for certification cases: both " + newGlobalEscalationLevelNumber + " and " + newEscalationLevel);
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
			event.setWorkItemId(workItem.getId());
			event.setEscalationLevel(workItem.getEscalationLevel());

			addDeltasForAssigneesAndEvent(deltas, workItem, aCase, newAssignees, event);
			deltas.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
					.item(F_CASE, aCase.getId(), F_WORK_ITEM, workItem.getId(), F_ESCALATION_LEVEL).replace(newEscalationLevel)
					.asItemDelta());

			// notification (after modifications)
		}
		assert newGlobalEscalationLevelNumber != null;
		AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaignChecked(workItems.get(0));
		AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
		assert stage != null;
		AccessCertificationStageDefinitionType stageDefinition = CertCampaignTypeUtil.getCurrentStageDefinition(campaign);
		deltas.addAll(createTriggersForTimedActions(campaignOid, newGlobalEscalationLevelNumber,
				XmlTypeConverter.toDate(stage.getStart()), XmlTypeConverter.toDate(stage.getDeadline()), stageDefinition.getTimedActions()));

		modifyObjectViaModel(AccessCertificationCampaignType.class, campaignOid, deltas, task, result);

//		AccessCertificationCampaignType updatedCampaign = refreshCampaign(campaign, task, result);
//		LOGGER.info("Updated campaign state: {}", updatedCampaign.getState());
//		eventHelper.onCampaignEnd(updatedCampaign, task, result);

    }

	private void addDeltasForAssigneesAndEvent(List<ItemDelta<?, ?>> deltas, AccessCertificationWorkItemType workItem,
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
			AccessCertificationCampaignType campaign, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException {

        List<ObjectReferenceType> rv = new ArrayList<>();
        rv.addAll(CloneUtil.cloneCollectionMembers(delegateAction.getApproverRef()));
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
	public List<ItemDelta<?, ?>> createTriggersForTimedActions(String campaignOid, int escalationLevel, Date workItemCreateTime,
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
			throw new SystemException("Couldn't create trigger(s) for campaign " + campaignOid + ": " + e.getMessage(), e);
		}
	}

	//endregion

    //region ================================ Campaign and stage close ================================

    void closeCampaign(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, SecurityViolationException {
        LOGGER.info("Closing campaign {}", ObjectTypeUtil.toShortString(campaign));
        int lastStageNumber = CertCampaignTypeUtil.getNumberOfStages(campaign);
        // TODO issue a warning if we are not in a correct state
        PropertyDelta<Integer> stageNumberDelta = createStageNumberDelta(lastStageNumber + 1);
        PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(CLOSED);
        ContainerDelta<TriggerType> triggerDelta = createTriggerDeleteDelta();
        PropertyDelta<XMLGregorianCalendar> endDelta = createEndTimeDelta(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        modifyObjectViaModel(AccessCertificationCampaignType.class, campaign.getOid(),
                Arrays.asList(stateDelta, stageNumberDelta, triggerDelta, endDelta), task, result);

        AccessCertificationCampaignType updatedCampaign = refreshCampaign(campaign, result);
        LOGGER.info("Updated campaign state: {}", updatedCampaign.getState());
        eventHelper.onCampaignEnd(updatedCampaign, task, result);

        if (campaign.getDefinitionRef() != null) {
            List<ItemDelta<?,?>> deltas = DeltaBuilder.deltaFor(AccessCertificationDefinitionType.class, prismContext)
                    .item(F_LAST_CAMPAIGN_CLOSED_TIMESTAMP).replace(XmlTypeConverter.createXMLGregorianCalendar(new Date()))
                    .asItemDeltas();
            modifyObjectViaModel(AccessCertificationDefinitionType.class, campaign.getDefinitionRef().getOid(), deltas, task, result);
        }
    }

    List<ItemDelta<?,?>> getDeltasForStageClose(AccessCertificationCampaignType campaign, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        List<ItemDelta<?,?>> rv = caseHelper.createOutcomeDeltas(campaign, result);

        rv.add(createStateDelta(REVIEW_STAGE_DONE));
        rv.add(createStageEndTimeDelta(campaign));
        rv.add(createTriggerDeleteDelta());

        return rv;
    }

    private ItemDelta createStageEndTimeDelta(AccessCertificationCampaignType campaign) throws SchemaException {
        AccessCertificationStageType stage = CertCampaignTypeUtil.findStage(campaign, campaign.getStageNumber());
        Long stageId = stage.asPrismContainerValue().getId();
        assert stageId != null;
        XMLGregorianCalendar currentTime = XmlTypeConverter.createXMLGregorianCalendar(new Date());
		return DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(AccessCertificationCampaignType.F_STAGE, stageId, AccessCertificationStageType.F_END).replace(currentTime)
				.asItemDelta();
    }

    void afterStageClose(String campaignOid, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        final AccessCertificationCampaignType campaign = generalHelper.getCampaign(campaignOid, null, task, result);
        eventHelper.onCampaignStageEnd(campaign, task, result);
    }

    //endregion

    //region ================================ Auxiliary methods for delta processing ================================

    List<ItemDelta<?,?>> createDeltasForStageNumberAndState(int number, AccessCertificationCampaignStateType state) {
        final List<ItemDelta<?,?>> rv = new ArrayList<>();
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

    private PropertyDelta<XMLGregorianCalendar> createStartTimeDelta(XMLGregorianCalendar date) {
        return PropertyDelta.createReplaceDelta(generalHelper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_START_TIMESTAMP, date);
    }

    private PropertyDelta<XMLGregorianCalendar> createEndTimeDelta(XMLGregorianCalendar date) {
        return PropertyDelta.createReplaceDelta(generalHelper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_END_TIMESTAMP, date);
    }

    private ContainerDelta createTriggerDeleteDelta() {
        return ContainerDelta.createModificationReplace(ObjectType.F_TRIGGER, generalHelper.getCampaignObjectDefinition());
    }

    private ItemDelta createStageAddDelta(AccessCertificationStageType stage) {
        ContainerDelta<AccessCertificationStageType> stageDelta = ContainerDelta.createDelta(AccessCertificationCampaignType.F_STAGE,
                AccessCertificationCampaignType.class, prismContext);
        stageDelta.addValueToAdd(stage.asPrismContainerValue());
        return stageDelta;
    }

    //endregion

    //region ================================ Model and repository operations ================================

    void addObject(ObjectType objectType, Task task, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        ObjectDelta<? extends ObjectType> objectDelta = ObjectDelta.createAddDelta(objectType.asPrismObject());
        Collection<ObjectDeltaOperation<? extends ObjectType>> ops;
        try {
            ops = modelService.executeChanges(
					Collections.singleton(objectDelta),
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

    <T extends ObjectType> void modifyObjectViaModel(Class<T> objectClass, String oid, Collection<ItemDelta<?,?>> itemDeltas, Task task, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
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
    public AccessCertificationCampaignType refreshCampaign(AccessCertificationCampaignType campaign,
			OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
        return repositoryService.getObject(AccessCertificationCampaignType.class, campaign.getOid(), null, result).asObjectable();
    }

    //endregion
}
