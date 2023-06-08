/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValueCollectionsUtil;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.*;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.cases.CaseRelatedUtils;
import com.evolveum.midpoint.schema.util.cases.WorkItemTypeUtil;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.*;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.*;
import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_ASSIGNEE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_ESCALATION_LEVEL;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Logic for certification operations like decision recording, case creation or advancement.
 */
@Component
public class AccCertCaseOperationsHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AccCertCaseOperationsHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired private AccCertExpressionHelper expressionHelper;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private AccCertGeneralHelper generalHelper;
    @Autowired private AccCertResponseComputationHelper computationHelper;
    @Autowired private AccCertQueryHelper queryHelper;
    @Autowired private AccCertUpdateHelper updateHelper;
    @Autowired private Clock clock;

    /**
     * Records a decision. Updates necessary items like the outcomes.
     */
    void recordDecision(String campaignOid, long caseId, long workItemId, AccessCertificationResponseType response,
            String comment, Task task, OperationResult result) throws SecurityViolationException, ObjectNotFoundException,
            SchemaException, ObjectAlreadyExistsException {
        AccessCertificationCaseType acase = queryHelper.getCase(campaignOid, caseId, task, result);
        if (acase == null) {
            throw new ObjectNotFoundException(
                    "Case " + caseId + " was not found in campaign " + campaignOid,
                    AccessCertificationCaseType.class,
                    campaignOid + ":" + caseId);
        }
        AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaign(acase);
        if (campaign == null) {
            throw new IllegalStateException("No owning campaign present in case " + acase);
        }
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(acase, workItemId);
        if (workItem == null) {
            throw new ObjectNotFoundException(
                    "Work item " + workItemId + " was not found in campaign " + toShortString(campaign) + ", case " + caseId,
                    AccessCertificationWorkItemType.class,
                    campaignOid + ":" + workItemId);
        }

        ObjectReferenceType responderRef = ObjectTypeUtil.createObjectRef(securityContextManager.getPrincipal().getFocus(), prismContext);
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        ItemPath workItemPath = ItemPath.create(F_CASE, caseId, F_WORK_ITEM, workItemId);
        Collection<ItemDelta<?,?>> deltaList = prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(workItemPath.append(AccessCertificationWorkItemType.F_OUTPUT))
                        .replace(new AbstractWorkItemOutputType()
                                .outcome(toUri(normalizeToNull(response)))
                                .comment(comment))
                .item(workItemPath.append(AccessCertificationWorkItemType.F_OUTPUT_CHANGE_TIMESTAMP)).replace(now)
                .item(workItemPath.append(AccessCertificationWorkItemType.F_PERFORMER_REF)).replace(responderRef)
                .asItemDeltas();
        ItemDeltaCollectionsUtil.applyTo(deltaList, campaign.asPrismContainerValue()); // to have data for outcome computation

        AccessCertificationResponseType newCurrentOutcome = computationHelper.computeOutcomeForStage(acase, campaign, campaign.getStageNumber());
        AccessCertificationResponseType newOverallOutcome = computationHelper.computeOverallOutcome(acase, campaign, campaign.getStageNumber(), newCurrentOutcome);
        deltaList.addAll(prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE, caseId, F_CURRENT_STAGE_OUTCOME).replace(toUri(newCurrentOutcome))
                .item(F_CASE, caseId, F_OUTCOME).replace(toUri(newOverallOutcome))
                .asItemDeltas());

        updateHelper.modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaignOid, deltaList, task, result);
    }

    // TODO temporary implementation - should be done somehow in batches in order to improve performance
    void markCaseAsRemedied(@NotNull String campaignOid, long caseId, Task task, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        PropertyDelta<XMLGregorianCalendar> remediedDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                ItemPath.create(F_CASE, caseId, AccessCertificationCaseType.F_REMEDIED_TIMESTAMP),
                generalHelper.getCampaignObjectDefinition(), XmlTypeConverter.createXMLGregorianCalendar(new Date()));

        updateHelper.modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaignOid,
                Collections.singletonList(remediedDelta), task, parentResult);
    }

    //region ================================ Delegation/escalation ================================

    void delegateWorkItems(String campaignOid, List<AccessCertificationWorkItemType> workItems,
            DelegateWorkItemActionType delegateAction, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException,
            SecurityViolationException {
        LOGGER.debug("Going to delegate {} work item(s) in campaign {}", workItems.size(), campaignOid);

        MidPointPrincipal principal = securityContextManager.getPrincipal();
        result.addContext("user", toShortString(principal.getFocus()));
        ObjectReferenceType initiator = ObjectTypeUtil.createObjectRef(principal.getFocus(), prismContext);
        ObjectReferenceType attorney = ObjectTypeUtil.createObjectRef(principal.getAttorney(), prismContext);

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
            if (norm(workItem.getIteration()) != norm(campaign.getIteration())) {
                throw new IllegalStateException("Couldn't delegate a work item that is not in a current iteration. Current iteration: " + norm(campaign.getIteration()) + ", work item iteration: " + norm(workItem.getIteration()));
            }
            if (workItem.getStageNumber() != campaign.getStageNumber()) {
                throw new IllegalStateException("Couldn't delegate a work item that is not in a current stage. Current stage: " + campaign.getStageNumber() + ", work item stage: " + workItem.getStageNumber());
            }
            List<ObjectReferenceType> delegates = computeDelegateTo(delegateAction, workItem, aCase, campaign, task, result);

            WorkItemEventCauseInformationType causeInformation = null;            // TODO

            //noinspection ConstantConditions
            LOGGER.trace("Delegating work item {} to {}: cause={}", workItem, delegates, causeInformation);
            List<ObjectReferenceType> assigneesBefore = CloneUtil.cloneCollectionMembers(workItem.getAssigneeRef());
            WorkItemDelegationMethodType method = getDelegationMethod(delegateAction);
            List<ObjectReferenceType> newAssignees = new ArrayList<>();
            List<ObjectReferenceType> delegatedTo = new ArrayList<>();
            CaseRelatedUtils.computeAssignees(newAssignees, delegatedTo, delegates, method, workItem.getAssigneeRef());
            //noinspection ConstantConditions
            WorkItemDelegationEventType event = ApprovalContextUtil
                    .createDelegationEvent(null, assigneesBefore, delegatedTo, method, causeInformation, prismContext);
            event.setTimestamp(now);
            event.setInitiatorRef(initiator);
            event.setAttorneyRef(attorney);
            event.setWorkItemId(workItem.getId());
            event.setEscalationLevel(workItem.getEscalationLevel());
            event.setStageNumber(campaign.getStageNumber());
            event.setIteration(norm(campaign.getIteration()));
            addDeltasForNewAssigneesAndEvent(deltas, workItem, aCase, newAssignees, event);

            // notification (after modifications)
        }
        updateHelper.modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaignOid, deltas, task, result);

        // TODO notifications

        //        AccessCertificationCampaignType updatedCampaign = refreshCampaign(campaign, task, result);
        //        LOGGER.info("Updated campaign state: {}", updatedCampaign.getState());
        //        eventHelper.onCampaignEnd(updatedCampaign, task, result);

    }

    @NotNull
    private WorkItemDelegationMethodType getDelegationMethod(DelegateWorkItemActionType delegateAction) {
        return defaultIfNull(delegateAction.getDelegationMethod(), WorkItemDelegationMethodType.REPLACE_ASSIGNEES);
    }

    public void escalateCampaign(String campaignOid, EscalateWorkItemActionType escalateAction,
            WorkItemEventCauseInformationType causeInformation, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException {
        MidPointPrincipal principal = securityContextManager.getPrincipal();
        result.addContext("user", toShortString(principal.getFocus()));
        ObjectReferenceType initiator = ObjectTypeUtil.createObjectRef(principal.getFocus(), prismContext);
        ObjectReferenceType attorney = ObjectTypeUtil.createObjectRef(principal.getAttorney(), prismContext);

        List<AccessCertificationWorkItemType> workItems = queryHelper.searchOpenWorkItems(
                CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid, prismContext),
                false,
                result);

        if (workItems.isEmpty()) {
            LOGGER.debug("No work items, no escalation (campaign: {})", campaignOid);
            return;
        }

        LOGGER.debug("Going to escalate the campaign {}: {} work item(s)", campaignOid, workItems.size());

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
            if (norm(workItem.getIteration()) != norm(workItemCampaign.getIteration())) {
                throw new IllegalStateException("Couldn't delegate a work item that is not in a current iteration. Current stage: " + norm(workItemCampaign.getIteration()) + ", work item iteration: " + norm(workItem.getIteration()));
            }
            if (workItem.getOutput() != null && workItem.getOutput().getOutcome() != null) {
                // It is a bit questionable to skip this work item (as it is not signed off),
                // but it is also not quite OK to escalate it, as there's some output present.
                // The latter is less awkward, so let's do it that way.
                continue;
            }
            List<ObjectReferenceType> delegates = computeDelegateTo(escalateAction, workItem, aCase, workItemCampaign, task, result);

            int escalationLevel = WorkItemTypeUtil.getEscalationLevelNumber(workItem);
            if (escalationLevel + 1 != newStageEscalationLevelNumber) {
                throw new IllegalStateException("Different escalation level numbers for certification cases: work item level ("
                        + newEscalationLevel + ") is different from the stage level (" + newStageEscalationLevelNumber + ")");
            }
            LOGGER.debug("Escalating work item {} to level: {}; delegates={}: cause={}", workItem, newEscalationLevel, delegates, causeInformation);

            List<ObjectReferenceType> assigneesBefore = CloneUtil.cloneCollectionMembers(workItem.getAssigneeRef());
            WorkItemDelegationMethodType method = getDelegationMethod(escalateAction);
            List<ObjectReferenceType> newAssignees = new ArrayList<>();
            List<ObjectReferenceType> delegatedTo = new ArrayList<>();
            CaseRelatedUtils.computeAssignees(newAssignees, delegatedTo, delegates, method, workItem.getAssigneeRef());
            WorkItemDelegationEventType event = ApprovalContextUtil
                    .createDelegationEvent(newEscalationLevel, assigneesBefore, delegatedTo, method, causeInformation, prismContext);
            event.setTimestamp(now);
            event.setInitiatorRef(initiator);
            event.setAttorneyRef(attorney);
            event.setWorkItemId(workItem.getId());
            event.setEscalationLevel(workItem.getEscalationLevel());
            event.setStageNumber(campaign.getStageNumber());
            event.setIteration(norm(campaign.getIteration()));
            List<ItemDelta<?, ?>> deltas = new ArrayList<>();
            addDeltasForNewAssigneesAndEvent(deltas, workItem, aCase, newAssignees, event);
            deltas.add(prismContext.deltaFor(AccessCertificationCampaignType.class)
                    .item(F_CASE, aCase.getId(), F_WORK_ITEM, workItem.getId(), F_ESCALATION_LEVEL).replace(newEscalationLevel)
                    .asItemDelta());
            modifications.add(deltas);
            // notification (after modifications)
        }
        AccessCertificationStageType stage = CertCampaignTypeUtil.getCurrentStage(campaign);
        assert stage != null;
        Long stageId = stage.asPrismContainerValue().getId();
        assert stageId != null;
        modifications.add(prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_STAGE, stageId, AccessCertificationStageType.F_ESCALATION_LEVEL).replace(newEscalationLevel)
                .asItemDelta());
        AccessCertificationStageDefinitionType stageDefinition = CertCampaignTypeUtil.getCurrentStageDefinition(campaign);
        modifications.add(updateHelper.getDeltasToCreateTriggersForTimedActions(campaignOid, newStageEscalationLevelNumber,
                XmlTypeConverter.toDate(stage.getStartTimestamp()), XmlTypeConverter.toDate(stage.getDeadline()), stageDefinition.getTimedActions()));

        updateHelper.modifyCampaignPreAuthorized(campaignOid, modifications, task, result);

        campaign = generalHelper.getCampaign(campaignOid, null, task, result);
        // TODO differentiate between "old" and "new" reviewers
        updateHelper.notifyReviewers(campaign, true, task, result);

        //        AccessCertificationCampaignType updatedCampaign = refreshCampaign(campaign, task, result);
        //        LOGGER.info("Updated campaign state: {}", updatedCampaign.getState());
        //        eventHelper.onCampaignEnd(updatedCampaign, task, result);

    }

    private void addDeltasForNewAssigneesAndEvent(List<ItemDelta<?, ?>> deltas, AccessCertificationWorkItemType workItem,
            AccessCertificationCaseType aCase, List<ObjectReferenceType> newAssignees, WorkItemDelegationEventType event)
            throws SchemaException {
        deltas.add(prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE, aCase.getId(), F_WORK_ITEM, workItem.getId(), F_ASSIGNEE_REF)
                .replace(PrismValueCollectionsUtil.asReferenceValues(newAssignees))
                .asItemDelta());
        deltas.add(prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_CASE, aCase.getId(), F_EVENT).add(event)
                .asItemDelta());
    }

    private List<ObjectReferenceType> computeDelegateTo(DelegateWorkItemActionType delegateAction,
            AccessCertificationWorkItemType workItem, AccessCertificationCaseType aCase,
            AccessCertificationCampaignType campaign, Task task, OperationResult result) {

        List<ObjectReferenceType> rv = CloneUtil.cloneCollectionMembers(delegateAction.getApproverRef());
        if (!delegateAction.getApproverExpression().isEmpty()) {
            VariablesMap variables = new VariablesMap();
            variables.put(ExpressionConstants.VAR_WORK_ITEM, workItem, AccessCertificationWorkItemType.class);
            variables.put(ExpressionConstants.VAR_CERTIFICATION_CASE, aCase, aCase.asPrismContainerValue().getDefinition());
            variables.putObject(ExpressionConstants.VAR_CAMPAIGN, campaign, AccessCertificationCampaignType.class);
            for (ExpressionType expression : delegateAction.getApproverExpression()) {
                rv.addAll(expressionHelper.evaluateRefExpressionChecked(expression, variables, "computing delegates", task, result));
            }
        }
        return rv;
    }

    //endregion

}
