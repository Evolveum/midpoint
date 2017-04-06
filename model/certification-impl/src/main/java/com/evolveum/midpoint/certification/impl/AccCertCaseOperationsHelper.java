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

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
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
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.apache.commons.lang.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;

/**
 * @author mederly
 */
@Component
public class AccCertCaseOperationsHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertCaseOperationsHelper.class);

    @Autowired private AccCertReviewersHelper reviewersHelper;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private AccCertGeneralHelper generalHelper;
	@Autowired private AccCertResponseComputationHelper computationHelper;
	@Autowired private AccCertQueryHelper queryHelper;
	@Autowired private AccCertUpdateHelper updateHelper;
    @Autowired private Clock clock;

    void recordDecision(String campaignOid, long caseId, long workItemId,
			AccessCertificationResponseType response, String comment, Task task, OperationResult result)
			throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		AccessCertificationCaseType _case = queryHelper.getCase(campaignOid, caseId, task, result);
		if (_case == null) {
			throw new ObjectNotFoundException("Case " + caseId + " was not found in campaign " + campaignOid);
		}
		AccessCertificationCampaignType campaign = CertCampaignTypeUtil.getCampaign(_case);
		if (campaign == null) {
			throw new IllegalStateException("No owning campaign present in case " + _case);
		}
		AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(_case, workItemId);
		if (workItem == null) {
			throw new ObjectNotFoundException("Work item " + workItemId + " was not found in campaign " + toShortString(campaign) + ", case " + caseId);
		}

		ObjectReferenceType responderRef = ObjectTypeUtil.createObjectRef(securityEnforcer.getPrincipal().getUser());
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		ItemPath workItemPath = new ItemPath(F_CASE, caseId, F_WORK_ITEM, workItemId);
		Collection<ItemDelta<?,?>> deltaList = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(workItemPath.subPath(AccessCertificationWorkItemType.F_OUTPUT)).replace(new AbstractWorkItemOutputType()
						.outcome(OutcomeUtils.toUri(response))
						.comment(comment))
				.item(workItemPath.subPath(AccessCertificationWorkItemType.F_OUTPUT_CHANGE_TIMESTAMP)).replace(now)
				.item(workItemPath.subPath(AccessCertificationWorkItemType.F_PERFORMER_REF)).replace(responderRef)
				.asItemDeltas();

		ItemDelta.applyTo(deltaList, campaign.asPrismContainerValue());

		AccessCertificationResponseType newCurrentOutcome = computationHelper.computeOutcomeForStage(_case, campaign, campaign.getStageNumber());
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

    <F extends FocusType> List<ItemDelta<?,?>> getDeltasToCreateCases(
            final AccessCertificationCampaignType campaign, AccessCertificationStageType stage,
            final CertificationHandler handler, final Task task, final OperationResult result) throws SchemaException, ObjectNotFoundException {

        final List<ItemDelta<?,?>> rv = new ArrayList<>();

        final String campaignShortName = toShortString(campaign);

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
        @SuppressWarnings({ "unchecked", "raw" })
        final Class<F> objectClass = (Class<F>) prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(objectType);
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
        ResultHandler<F> resultHandler = (object, parentResult) -> {
			try {
				caseList.addAll(handler.createCasesForObject(object, campaign, task, parentResult));
			} catch (ExpressionEvaluationException|ObjectNotFoundException|SchemaException e) {
				// TODO process the exception more intelligently
				throw new SystemException("Cannot create certification case for object " + toShortString(object.asObjectable()) + ": " + e.getMessage(), e);
			}
			return true;
		};
        repositoryService.searchObjectsIterative(objectClass, query, resultHandler, null, false, result);

        AccessCertificationReviewerSpecificationType reviewerSpec =
                reviewersHelper.findReviewersSpecification(campaign, 1, task, result);

        ContainerDelta<AccessCertificationCaseType> caseDelta = ContainerDelta.createDelta(F_CASE,
                AccessCertificationCampaignType.class, prismContext);
        for (AccessCertificationCaseType _case : caseList) {
            _case.setCurrentStageNumber(1);
            _case.setCurrentReviewRequestedTimestamp(stage.getStart());
            _case.setCurrentReviewDeadline(stage.getDeadline());

			List<ObjectReferenceType> reviewers = reviewersHelper.getReviewersForCase(_case, campaign, reviewerSpec, task, result);
			_case.getWorkItem().addAll(createWorkItems(reviewers, 1));

            AccessCertificationResponseType currentStageOutcome = computationHelper.computeOutcomeForStage(_case, campaign, 1);
            _case.setCurrentStageOutcome(currentStageOutcome);
            _case.setOverallOutcome(computationHelper.computeOverallOutcome(_case, campaign, currentStageOutcome));

            @SuppressWarnings({ "raw", "unchecked" })
            PrismContainerValue<AccessCertificationCaseType> caseCVal = _case.asPrismContainerValue();
            caseDelta.addValueToAdd(caseCVal);
			LOGGER.trace("Adding certification case:\n{}", caseCVal.debugDumpLazily());
        }
        rv.add(caseDelta);

        LOGGER.trace("Created {} deltas to create {} cases for campaign {}", rv.size(), caseList.size(), campaignShortName);
        return rv;
    }

    private List<AccessCertificationWorkItemType> createWorkItems(List<ObjectReferenceType> forReviewers, int forStage) {
        List<AccessCertificationWorkItemType> workItems = new ArrayList<>();
        for (ObjectReferenceType reviewer : forReviewers) {
			AccessCertificationWorkItemType workItem = new AccessCertificationWorkItemType(prismContext)
					.stageNumber(forStage)
					.assigneeRef(reviewer.clone())
					.originalAssigneeRef(reviewer.clone());
            workItems.add(workItem);
        }
        return workItems;
    }

    List<ItemDelta<?,?>> getDeltasToAdvanceCases(AccessCertificationCampaignType campaign, AccessCertificationStageType stage, Task task, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        LOGGER.trace("Advancing reviewers and timestamps for cases in {}", toShortString(campaign));
        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaign.getOid(), null, null, result);
        List<ItemDelta<?,?>> rv = new ArrayList<>(caseList.size());

        int stageToBe = campaign.getStageNumber() + 1;

        List<AccessCertificationResponseType> outcomesToStopOn = computationHelper.getOutcomesToStopOn(campaign);

        AccessCertificationReviewerSpecificationType reviewerSpec =
                reviewersHelper.findReviewersSpecification(campaign, stageToBe, task, result);

        for (AccessCertificationCaseType _case : caseList) {
            if (!computationHelper.computeEnabled(campaign, _case, outcomesToStopOn)) {
                continue;
            }
            Long caseId = _case.asPrismContainerValue().getId();
            assert caseId != null;
			List<ObjectReferenceType> reviewers = reviewersHelper.getReviewersForCase(_case, campaign, reviewerSpec, task, result);
			List<AccessCertificationWorkItemType> workItems = createWorkItems(reviewers, stageToBe);
			_case.getWorkItem().addAll(CloneUtil.cloneCollectionMembers(workItems));
			AccessCertificationResponseType currentOutcome = computationHelper.computeOutcomeForStage(_case, campaign, stageToBe);
			AccessCertificationResponseType overallOutcome = computationHelper.computeOverallOutcome(_case, campaign, currentOutcome);
			rv.addAll(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
					.item(F_CASE, caseId, F_WORK_ITEM).add(PrismContainerValue.toPcvList(workItems))
					.item(F_CASE, caseId, F_CURRENT_REVIEW_REQUESTED_TIMESTAMP).replace(stage.getStart())
					.item(F_CASE, caseId, F_CURRENT_REVIEW_DEADLINE).replace(stage.getDeadline())
					.item(F_CASE, caseId, F_CURRENT_STAGE_OUTCOME).replace(currentOutcome)
					.item(F_CASE, caseId, F_OVERALL_OUTCOME).replace(overallOutcome)
					.item(F_CASE, caseId, F_CURRENT_STAGE_NUMBER).replace(stageToBe)
					.asItemDeltas());
        }

        LOGGER.debug("Created {} deltas to advance {} cases for campaign {}", rv.size(), caseList.size(), toShortString(campaign));
        return rv;
    }

    // computes outcomes at stage close (stage-level and overall) and creates appropriate deltas
    List<ItemDelta<?,?>> createOutcomeDeltas(AccessCertificationCampaignType campaign, OperationResult result) throws ObjectNotFoundException, SchemaException {
        List<ItemDelta<?,?>> rv = new ArrayList<>();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Updating current outcome for cases in {}", toShortString(campaign));
        }
        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaign.getOid(), null, null, result);

		for (AccessCertificationCaseType _case : caseList) {
			if (_case.getCurrentStageNumber() != campaign.getStageNumber()) {
				continue;
			}
			AccessCertificationResponseType newStageOutcome = computationHelper.computeOutcomeForStage(_case, campaign, campaign.getStageNumber());
			if (newStageOutcome != _case.getCurrentStageOutcome()) {
				if (newStageOutcome == null) {
					throw new IllegalStateException(
							"Computed currentStateOutcome is null for case id " + _case.asPrismContainerValue().getId());
				}
				ItemDelta currentStageOutcomeDelta = DeltaBuilder
						.deltaFor(AccessCertificationCampaignType.class, prismContext)
						.item(F_CASE, _case.asPrismContainerValue().getId(), F_CURRENT_STAGE_OUTCOME).replace(newStageOutcome)
						.asItemDelta();
				rv.add(currentStageOutcomeDelta);
			}

			AccessCertificationCaseStageOutcomeType stageOutcomeRecord = new AccessCertificationCaseStageOutcomeType(prismContext)
					.stageNumber(campaign.getStageNumber())
					.outcome(newStageOutcome);
			rv.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
					.item(F_CASE, _case.asPrismContainerValue().getId(), F_COMPLETED_STAGE_OUTCOME).add(stageOutcomeRecord)
					.asItemDelta());

			AccessCertificationResponseType newOverallOutcome = computationHelper.computeOverallOutcome(_case, campaign, newStageOutcome);
			if (newOverallOutcome != _case.getOverallOutcome()) {
				if (newOverallOutcome == null) {
					throw new IllegalStateException(
							"Computed overallOutcome is null for case id " + _case.asPrismContainerValue().getId());
				}
				rv.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
						.item(F_CASE, _case.asPrismContainerValue().getId(), F_OVERALL_OUTCOME).replace(newOverallOutcome)
						.asItemDelta());
			}
		}
        return rv;
    }

    // TODO temporary implementation - should be done somehow in batches in order to improve performance
	void markCaseAsRemedied(@NotNull String campaignOid, long caseId, Task task, OperationResult parentResult)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, SecurityViolationException {
        PropertyDelta<XMLGregorianCalendar> reviewRemediedDelta = PropertyDelta.createModificationReplaceProperty(
                new ItemPath(
                        new NameItemPathSegment(F_CASE),
                        new IdItemPathSegment(caseId),
                        new NameItemPathSegment(AccessCertificationCaseType.F_REMEDIED_TIMESTAMP)),
                generalHelper.getCampaignObjectDefinition(), XmlTypeConverter.createXMLGregorianCalendar(new Date()));

        updateHelper.modifyObjectViaModel(AccessCertificationCampaignType.class, campaignOid,
				Collections.singletonList(reviewRemediedDelta), task, parentResult);
    }


}
