/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.marshaller.QueryConvertor;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseStageOutcomeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationObjectBasedScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationReviewerSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_COMPLETED_STAGE_OUTCOME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CURRENT_STAGE_OUTCOME;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CURRENT_STAGE_NUMBER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_DECISION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_OVERALL_OUTCOME;

/**
 * @author mederly
 */
@Component
public class AccCertCaseOperationsHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertCaseOperationsHelper.class);

    @Autowired
    private AccCertReviewersHelper reviewersHelper;

    @Autowired
    protected AccCertEventHelper eventHelper;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    protected SecurityEnforcer securityEnforcer;

    @Autowired
    protected AccCertGeneralHelper generalHelper;

    @Autowired
    protected AccCertResponseComputationHelper computationHelper;

    @Autowired
    protected AccCertQueryHelper queryHelper;

    @Autowired
    protected AccCertUpdateHelper updateHelper;

    void recordDecision(AccessCertificationCampaignType campaign, long caseId, AccessCertificationDecisionType decision, Task task, OperationResult result) throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        String campaignOid = campaign.getOid();

        if (!AccessCertificationCampaignStateType.IN_REVIEW_STAGE.equals(campaign.getState())) {
            throw new IllegalStateException("Campaign is not in review stage; its state is " + campaign.getState());
        }

        decision = decision.clone();        // to not modify the original decision

        // filling-in missing pieces (if any)
        int currentStage = campaign.getStageNumber();
        if (decision.getStageNumber() == 0) {
            decision.setStageNumber(currentStage);
        } else {
            if (decision.getStageNumber() != currentStage) {
                throw new IllegalStateException("Cannot add decision with stage number (" + decision.getStageNumber() + ") other than current (" + currentStage + ")");
            }
        }
        if (decision.getTimestamp() == null) {
            decision.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(System.currentTimeMillis()));
        }
        if (decision.getReviewerRef() != null) {
            throw new IllegalArgumentException("ReviewerRef must not be filled-in in decision to be recorded.");
        }
        UserType currentUser = securityEnforcer.getPrincipal().getUser();
        decision.setReviewerRef(ObjectTypeUtil.createObjectRef(currentUser));

        AccessCertificationCaseType _case = queryHelper.getCase(campaignOid, caseId, task, result);
        if (_case == null) {
            throw new ObjectNotFoundException("Case " + caseId + " was not found in campaign " + ObjectTypeUtil.toShortString(campaign));
        }
        AccessCertificationDecisionType existingDecision = CertCampaignTypeUtil.findDecision(_case, currentStage, decision.getReviewerRef().getOid());

        if (existingDecision != null && existingDecision.equals(decision)) {
            // very unprobable, but ... to be sure let's check it
            // we skip the operation because add+delete the same value is unpredictable
        } else {
            ItemPath decisionPath = new ItemPath(
                    new NameItemPathSegment(F_CASE),
                    new IdItemPathSegment(caseId),
                    new NameItemPathSegment(AccessCertificationCaseType.F_DECISION));
            Collection<ItemDelta<?,?>> deltaList = new ArrayList<>();

            // let's remove existing decision and add the new one
            if (existingDecision != null) {
                ContainerDelta<AccessCertificationDecisionType> decisionDeleteDelta =
                        ContainerDelta.createModificationDelete(decisionPath, AccessCertificationCampaignType.class, prismContext, existingDecision.clone());
                deltaList.add(decisionDeleteDelta);
            }

            ContainerDelta<AccessCertificationDecisionType> decisionAddDelta =
                    ContainerDelta.createModificationAdd(decisionPath, AccessCertificationCampaignType.class, prismContext, decision);
            deltaList.add(decisionAddDelta);

            AccessCertificationResponseType newCurrentOutcome = computationHelper.computeOutcomeForStage(_case, decision, campaign);
            if (!ObjectUtils.equals(newCurrentOutcome, _case.getCurrentStageOutcome())) {
                PropertyDelta currentOutcomeDelta = PropertyDelta.createModificationReplaceProperty(
                        new ItemPath(
                                new NameItemPathSegment(F_CASE),
                                new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                                new NameItemPathSegment(F_CURRENT_STAGE_OUTCOME)),
                        generalHelper.getCampaignObjectDefinition(), newCurrentOutcome);
                deltaList.add(currentOutcomeDelta);
            }

            AccessCertificationResponseType newOverallOutcome = computationHelper.computeOverallOutcome(_case, campaign, newCurrentOutcome);
            if (!ObjectUtils.equals(newOverallOutcome, _case.getOverallOutcome())) {
                PropertyDelta overallOutcomeDelta = PropertyDelta.createModificationReplaceProperty(
                        new ItemPath(
                                new NameItemPathSegment(F_CASE),
                                new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                                new NameItemPathSegment(F_OVERALL_OUTCOME)),
                        generalHelper.getCampaignObjectDefinition(), newOverallOutcome);
                deltaList.add(overallOutcomeDelta);
            }

            updateHelper.modifyObjectViaModel(AccessCertificationCampaignType.class, campaignOid, deltaList, task, result);
        }
    }

    List<ItemDelta<?,?>> getDeltasToCreateCases(
            final AccessCertificationCampaignType campaign, AccessCertificationStageType stage,
            final CertificationHandler handler, final Task task, final OperationResult result) throws SchemaException, ObjectNotFoundException {

        final List<ItemDelta<?,?>> rv = new ArrayList<>();

        final String campaignShortName = ObjectTypeUtil.toShortString(campaign);

        final AccessCertificationScopeType scope = campaign.getScopeDefinition();
        LOGGER.trace("Creating cases for scope {} in campaign {}", scope, campaignShortName);
        if (scope != null && !(scope instanceof AccessCertificationObjectBasedScopeType)) {
            throw new IllegalStateException("Unsupported access certification scope type: " + scope.getClass() + " for campaign " + campaignShortName);
        }
        final AccessCertificationObjectBasedScopeType objectBasedScope = (AccessCertificationObjectBasedScopeType) scope;

        final List<AccessCertificationCaseType> existingCases = queryHelper.searchCases(campaign.getOid(), null, null, result);
        if (!existingCases.isEmpty()) {
            throw new IllegalStateException("Unexpected " + existingCases.size() + " certification case(s) in campaign object " + campaignShortName + ". At this time there should be none.");
        }

        // create a query to find target objects from which certification cases will be created
        final ObjectQuery query = new ObjectQuery();
        final QName scopeDeclaredObjectType;
        if (objectBasedScope != null) {
            scopeDeclaredObjectType = objectBasedScope.getObjectType();
        } else {
            scopeDeclaredObjectType = null;
        }
        final QName objectType;
        if (scopeDeclaredObjectType != null) {
            objectType = scopeDeclaredObjectType;
        } else {
            objectType = handler.getDefaultObjectType();
        }
        if (objectType == null) {
            throw new IllegalStateException("Unspecified object type (and no default one provided) for campaign " + campaignShortName);
        }
        final Class objectClass = prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(objectType);
        if (objectClass == null) {
            throw new IllegalStateException("Object class not found for object type " + objectType + " in campaign " + campaignShortName);
        }

        final SearchFilterType searchFilter = objectBasedScope != null ? objectBasedScope.getSearchFilter() : null;
        if (searchFilter != null) {
            ObjectFilter filter = QueryConvertor.parseFilter(searchFilter, objectClass, prismContext);
            query.setFilter(filter);
        }

        final List<AccessCertificationCaseType> caseList = new ArrayList<>();

        // create certification cases by executing the query and caseExpression on its results
        // here the subclasses of this class come into play
        ResultHandler<ObjectType> resultHandler = (object, parentResult) -> {
			try {
				caseList.addAll(handler.createCasesForObject(object, campaign, task, parentResult));
			} catch (ExpressionEvaluationException|ObjectNotFoundException|SchemaException e) {
				// TODO process the exception more intelligently
				throw new SystemException("Cannot create certification case for object " + ObjectTypeUtil.toShortString(object.asObjectable()) + ": " + e.getMessage(), e);
			}
			return true;
		};
        repositoryService.searchObjectsIterative(objectClass, query, resultHandler, null, false, result);

        AccessCertificationReviewerSpecificationType reviewerSpec =
                reviewersHelper.findReviewersSpecification(campaign, 1, task, result);

        ContainerDelta<AccessCertificationCaseType> caseDelta = ContainerDelta.createDelta(F_CASE,
                AccessCertificationCampaignType.class, prismContext);
        for (int i = 0; i < caseList.size(); i++) {
            final AccessCertificationCaseType _case = caseList.get(i);

            _case.setCurrentStageNumber(1);
            reviewersHelper.setupReviewersForCase(_case, campaign, reviewerSpec, task, result);
            _case.setCurrentReviewRequestedTimestamp(stage.getStart());
            _case.setCurrentReviewDeadline(stage.getDeadline());

            List<AccessCertificationDecisionType> decisions = createEmptyDecisionsForCase(_case.getCurrentReviewerRef(), 1);
            _case.getDecision().addAll(decisions);

            final AccessCertificationResponseType currentStageOutcome = computationHelper.computeInitialResponseForStage(_case, campaign, 1);
            _case.setCurrentStageOutcome(currentStageOutcome);
            _case.setOverallOutcome(computationHelper.computeOverallOutcome(_case, campaign, currentStageOutcome));

            PrismContainerValue<AccessCertificationCaseType> caseCVal = _case.asPrismContainerValue();
            caseDelta.addValueToAdd(caseCVal);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Adding certification case:\n{}", caseCVal.debugDump());
            }
        }
        rv.add(caseDelta);

        LOGGER.trace("Created {} deltas to create {} cases for campaign {}", rv.size(), caseList.size(), campaignShortName);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Deltas: {}", DebugUtil.debugDump(rv));
        }
        return rv;
    }

    // workaround for a query interpreter deficiency: we fill-in decisions when in stage 1
    // (in order to be able to find cases that were not responded to by a given reviewer)
    private List<AccessCertificationDecisionType> createEmptyDecisionsForCase(List<ObjectReferenceType> forReviewers, int forStage) {
        List<AccessCertificationDecisionType> decisions = new ArrayList<>();
        for (ObjectReferenceType reviewer : forReviewers) {
            AccessCertificationDecisionType decision = new AccessCertificationDecisionType(prismContext);
            decision.setReviewerRef(reviewer);
            decision.setStageNumber(forStage);
            decision.setResponse(null);
            decision.setTimestamp(null);
            decisions.add(decision);
        }
        return decisions;
    }

    List<ItemDelta<?,?>> getDeltasToAdvanceCases(AccessCertificationCampaignType campaign, AccessCertificationStageType stage, Task task, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        LOGGER.trace("Advancing reviewers and timestamps for cases in {}", ObjectTypeUtil.toShortString(campaign));
        final List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaign.getOid(), null, null, result);
        final List<ItemDelta<?,?>> rv = new ArrayList<>(caseList.size());

        final int stageToBe = campaign.getStageNumber() + 1;

        final List<AccessCertificationResponseType> outcomesToStopOn = computationHelper.getOutcomesToStopOn(campaign);

        final AccessCertificationReviewerSpecificationType reviewerSpec =
                reviewersHelper.findReviewersSpecification(campaign, stageToBe, task, result);

        for (int i = 0; i < caseList.size(); i++) {
            final AccessCertificationCaseType _case = caseList.get(i);

            if (!computationHelper.computeEnabled(campaign, _case, outcomesToStopOn)) {
                continue;
            }

            final Long caseId = _case.asPrismContainerValue().getId();
            assert caseId != null;

            reviewersHelper.setupReviewersForCase(_case, campaign, reviewerSpec, task, result);
            final PrismReference reviewersRef = _case.asPrismContainerValue().findOrCreateReference(AccessCertificationCaseType.F_CURRENT_REVIEWER_REF);
            final ReferenceDelta reviewersDelta = ReferenceDelta.createModificationReplace(
                    new ItemPath(
                            new NameItemPathSegment(F_CASE),
                            new IdItemPathSegment(caseId),
                            new NameItemPathSegment(AccessCertificationCaseType.F_CURRENT_REVIEWER_REF)),
                    generalHelper.getCampaignObjectDefinition(), CloneUtil.cloneCollectionMembers(reviewersRef.getValues()));
            rv.add(reviewersDelta);

            final PropertyDelta reviewRequestedTimestampDelta = PropertyDelta.createModificationReplaceProperty(
                    new ItemPath(
                            new NameItemPathSegment(F_CASE),
                            new IdItemPathSegment(caseId),
                            new NameItemPathSegment(AccessCertificationCaseType.F_CURRENT_REVIEW_REQUESTED_TIMESTAMP)),
                    generalHelper.getCampaignObjectDefinition(),
                    Arrays.asList(stage.getStart()));
            rv.add(reviewRequestedTimestampDelta);

            final ItemPath deadlinePath = new ItemPath(
                    new NameItemPathSegment(F_CASE),
                    new IdItemPathSegment(caseId),
                    new NameItemPathSegment(AccessCertificationCaseType.F_CURRENT_REVIEW_DEADLINE));
            final PropertyDelta deadlineDelta = PropertyDelta.createModificationReplaceProperty(deadlinePath,
                    generalHelper.getCampaignObjectDefinition(),
                    Arrays.asList(stage.getDeadline()));
            rv.add(deadlineDelta);

            final AccessCertificationResponseType currentOutcome = computationHelper.computeInitialResponseForStage(_case, campaign, stageToBe);
            final PropertyDelta currentOutcomeDelta = PropertyDelta.createModificationReplaceProperty(
                    new ItemPath(
                            new NameItemPathSegment(F_CASE),
                            new IdItemPathSegment(caseId),
                            new NameItemPathSegment(F_CURRENT_STAGE_OUTCOME)),
                    generalHelper.getCampaignObjectDefinition(),
                    currentOutcome);
            rv.add(currentOutcomeDelta);

            final AccessCertificationResponseType overallOutcome = computationHelper.computeOverallOutcome(_case, campaign, currentOutcome);
            final PropertyDelta overallOutcomeDelta = PropertyDelta.createModificationReplaceProperty(
                    new ItemPath(
                            new NameItemPathSegment(F_CASE),
                            new IdItemPathSegment(caseId),
                            new NameItemPathSegment(F_OVERALL_OUTCOME)),
                    generalHelper.getCampaignObjectDefinition(),
                    overallOutcome);
            rv.add(overallOutcomeDelta);

            final PropertyDelta currentStageNumberDelta = PropertyDelta.createModificationReplaceProperty(
                    new ItemPath(
                            new NameItemPathSegment(F_CASE),
                            new IdItemPathSegment(caseId),
                            new NameItemPathSegment(F_CURRENT_STAGE_NUMBER)),
                    generalHelper.getCampaignObjectDefinition(),
                    stageToBe);
            rv.add(currentStageNumberDelta);

            final List<AccessCertificationDecisionType> emptyDecisions = createEmptyDecisionsForCase(_case.getCurrentReviewerRef(), stageToBe);
            final ItemDelta emptyDecisionsDelta = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                    .item(F_CASE, caseId, F_DECISION).add(emptyDecisions.toArray())
                    .asItemDelta();
            rv.add(emptyDecisionsDelta);
        }

        LOGGER.debug("Created {} deltas to advance {} cases for campaign {}", rv.size(), caseList.size(), ObjectTypeUtil.toShortString(campaign));
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Deltas: {}", DebugUtil.debugDump(rv));
        }
        return rv;
    }

    // computes outcomes at stage close (stage-level and overall) and creates appropriate deltas
    List<ItemDelta<?,?>> createOutcomeDeltas(AccessCertificationCampaignType campaign, OperationResult result) throws ObjectNotFoundException, SchemaException {
        final List<ItemDelta<?,?>> rv = new ArrayList<>();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Updating current outcome for cases in {}", ObjectTypeUtil.toShortString(campaign));
        }
        final List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaign.getOid(), null, null, result);

        for (int i = 0; i < caseList.size(); i++) {
            final AccessCertificationCaseType _case = caseList.get(i);
            if (_case.getCurrentStageNumber() != campaign.getStageNumber()) {
                continue;
            }
            final AccessCertificationResponseType newStageOutcome = computationHelper.computeOutcomeForStage(_case, campaign);
            if (newStageOutcome != _case.getCurrentStageOutcome()) {
                if (newStageOutcome == null) {
                    throw new IllegalStateException("Computed currentStateOutcome is null for case id " + _case.asPrismContainerValue().getId());
                }
                final ItemDelta currentStageOutcomeDelta = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                        .item(F_CASE, _case.asPrismContainerValue().getId(), F_CURRENT_STAGE_OUTCOME).replace(newStageOutcome)
                        .asItemDelta();
                rv.add(currentStageOutcomeDelta);
            }

            AccessCertificationCaseStageOutcomeType stageOutcomeRecord = new AccessCertificationCaseStageOutcomeType(prismContext);
            stageOutcomeRecord.setStageNumber(campaign.getStageNumber());
            stageOutcomeRecord.setOutcome(newStageOutcome);
            final ItemDelta stageOutcomeDelta = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                    .item(F_CASE, _case.asPrismContainerValue().getId(), F_COMPLETED_STAGE_OUTCOME).add(stageOutcomeRecord)
                    .asItemDelta();
            rv.add(stageOutcomeDelta);

            final AccessCertificationResponseType newOverallOutcome = computationHelper.computeOverallOutcome(_case, campaign, newStageOutcome);
            if (newOverallOutcome != _case.getOverallOutcome()) {
                if (newOverallOutcome == null) {
                    throw new IllegalStateException("Computed overallOutcome is null for case id " + _case.asPrismContainerValue().getId());
                }
                final ItemDelta overallOutcomeDelta = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                        .item(F_CASE, _case.asPrismContainerValue().getId(), F_OVERALL_OUTCOME).replace(newOverallOutcome)
                        .asItemDelta();
                rv.add(overallOutcomeDelta);
            }
        }
        return rv;
    }

    // TODO temporary implementation - should be done somehow in batches in order to improve performance
    public void markCaseAsRemedied(String campaignOid, long caseId, Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, SecurityViolationException {
        Validate.notNull(campaignOid, "campaignOid");
        Validate.notNull(task, "task");
        Validate.notNull(parentResult, "parentResult");

        PropertyDelta reviewRemediedDelta = PropertyDelta.createModificationReplaceProperty(
                new ItemPath(
                        new NameItemPathSegment(F_CASE),
                        new IdItemPathSegment(caseId),
                        new NameItemPathSegment(AccessCertificationCaseType.F_REMEDIED_TIMESTAMP)),
                generalHelper.getCampaignObjectDefinition(), XmlTypeConverter.createXMLGregorianCalendar(new Date()));

        updateHelper.modifyObjectViaModel(AccessCertificationCampaignType.class, campaignOid, Arrays.<ItemDelta<?,?>>asList(reviewRemediedDelta), task, parentResult);
    }


}
