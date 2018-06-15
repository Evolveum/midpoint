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
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypedObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
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
import java.util.Objects;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.*;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortStringLazy;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;
import static java.util.Collections.emptyList;

/**
 * Logic for certification operations like decision recording, case creation or advancement.
 * TODO specify relation to {@link AccCertUpdateHelper}
 *
 * @author mederly
 */
@Component
public class AccCertCaseOperationsHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertCaseOperationsHelper.class);

    @Autowired private AccCertReviewersHelper reviewersHelper;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
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

		ObjectReferenceType responderRef = ObjectTypeUtil.createObjectRef(securityContextManager.getPrincipal().getUser());
		XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
		ItemPath workItemPath = new ItemPath(F_CASE, caseId, F_WORK_ITEM, workItemId);
		Collection<ItemDelta<?,?>> deltaList = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
				.item(workItemPath.subPath(AccessCertificationWorkItemType.F_OUTPUT))
						.replace(new AbstractWorkItemOutputType()
								.outcome(toUri(normalizeToNull(response)))
								.comment(comment))
				.item(workItemPath.subPath(AccessCertificationWorkItemType.F_OUTPUT_CHANGE_TIMESTAMP)).replace(now)
				.item(workItemPath.subPath(AccessCertificationWorkItemType.F_PERFORMER_REF)).replace(responderRef)
				.asItemDeltas();
		ItemDelta.applyTo(deltaList, campaign.asPrismContainerValue()); // to have data for outcome computation

		String newCurrentOutcome = toUri(computationHelper.computeOutcomeForStage(_case, campaign, campaign.getStageNumber()));
		if (!ObjectUtils.equals(newCurrentOutcome, _case.getCurrentStageOutcome())) {
			deltaList.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
					.item(F_CASE, caseId, F_CURRENT_STAGE_OUTCOME).replace(newCurrentOutcome)
					.asItemDelta());
		}

		String newOverallOutcome = toUri(normalizeToNonNull(
						computationHelper.computeOverallOutcome(_case, campaign, newCurrentOutcome)));
		if (!ObjectUtils.equals(newOverallOutcome, _case.getOutcome())) {
			deltaList.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
					.item(F_CASE, caseId, F_OUTCOME).replace(newOverallOutcome)
					.asItemDelta());
		}

		updateHelper.modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaignOid, deltaList, task, result);
	}

	/**
	 *  Creates certification cases (in the form of delta list) on first stage opening.
	 */
    <F extends FocusType> void getDeltasToCreateCases(AccessCertificationCampaignType campaign, AccessCertificationStageType stage,
		    CertificationHandler handler, ModificationsToExecute modifications, Holder<Integer> workItemsCreatedHolder, Task task,
		    OperationResult result) throws SchemaException, ObjectNotFoundException {
        String campaignShortName = toShortString(campaign);

        AccessCertificationScopeType scope = campaign.getScopeDefinition();
        LOGGER.trace("Creating cases for scope {} in campaign {}", scope, campaignShortName);
        if (scope != null && !(scope instanceof AccessCertificationObjectBasedScopeType)) {
            throw new IllegalStateException("Unsupported access certification scope type: " + scope.getClass() + " for campaign " + campaignShortName);
        }
        AccessCertificationObjectBasedScopeType objectBasedScope = (AccessCertificationObjectBasedScopeType) scope;

	    assertNoExistingCases(campaign, result);

	    TypedObjectQuery<F> typedQuery = prepareObjectQuery(objectBasedScope, handler, campaignShortName);

	    List<AccessCertificationCaseType> caseList = new ArrayList<>();

        // create certification cases by executing the query and caseExpression on its results
        // here the subclasses of this class come into play
        repositoryService.searchObjectsIterative(typedQuery.getObjectClass(), typedQuery.getObjectQuery(),
		        (object, parentResult) -> {
			        try {
				        caseList.addAll(handler.createCasesForObject(object, campaign, task, parentResult));
			        } catch (CommonException | RuntimeException e) {
				        // TODO process the exception more intelligently
				        throw new SystemException("Cannot create certification case for object " + toShortString(object.asObjectable()) + ": " + e.getMessage(), e);
			        }
			        return true;
		        }, null, true, result);

        AccessCertificationReviewerSpecificationType reviewerSpec =
                reviewersHelper.findReviewersSpecification(campaign, 1);

        assert campaign.getIteration() == 1;

        for (AccessCertificationCaseType _case : caseList) {
	        ContainerDelta<AccessCertificationCaseType> caseDelta = ContainerDelta.createDelta(F_CASE,
			        AccessCertificationCampaignType.class, prismContext);
	        _case.setIteration(1);
            _case.setStageNumber(1);
            _case.setCurrentStageCreateTimestamp(stage.getStartTimestamp());
            _case.setCurrentStageDeadline(stage.getDeadline());

			List<ObjectReferenceType> reviewers = reviewersHelper.getReviewersForCase(_case, campaign, reviewerSpec, task, result);
			_case.getWorkItem().addAll(createWorkItems(reviewers, 1, 1, _case));

			workItemsCreatedHolder.setValue(workItemsCreatedHolder.getValue() + _case.getWorkItem().size());

            String currentStageOutcome = toUri(computationHelper.computeOutcomeForStage(_case, campaign, 1));
            _case.setCurrentStageOutcome(currentStageOutcome);
            _case.setOutcome(toUri(computationHelper.computeOverallOutcome(_case, campaign, currentStageOutcome)));

            @SuppressWarnings({ "raw", "unchecked" })
            PrismContainerValue<AccessCertificationCaseType> caseCVal = _case.asPrismContainerValue();
            caseDelta.addValueToAdd(caseCVal);
			LOGGER.trace("Adding certification case:\n{}", caseCVal.debugDumpLazily());
			modifications.add(caseDelta);
        }

        LOGGER.trace("Created {} deltas (in {} batches) to create {} cases ({} work items) for campaign {}",
		        modifications.getTotalDeltasCount(), modifications.batches.size(), caseList.size(),
		        workItemsCreatedHolder.getValue(), campaignShortName);
    }

	// create a query to find target objects from which certification cases will be created
	@NotNull
	private <F extends FocusType> TypedObjectQuery<F> prepareObjectQuery(AccessCertificationObjectBasedScopeType objectBasedScope,
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
		Class<F> objectClass = (Class<F>) prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(objectType);
		if (objectClass == null) {
		    throw new IllegalStateException("Object class not found for object type " + objectType + " in campaign " + campaignShortName);
		}

		// TODO derive search filter from certification handler (e.g. select only objects having assignments with the proper policySituation)
		// It is only an optimization but potentially a very strong one. Workaround: enter query filter manually into scope definition.
		final SearchFilterType searchFilter = objectBasedScope != null ? objectBasedScope.getSearchFilter() : null;
		ObjectQuery query = new ObjectQuery();
		if (searchFilter != null) {
		    query.setFilter(QueryConvertor.parseFilter(searchFilter, objectClass, prismContext));
		}
		return new TypedObjectQuery<>(objectClass, query);
	}

	private void assertNoExistingCases(AccessCertificationCampaignType campaign, OperationResult result)
			throws SchemaException {
		List<AccessCertificationCaseType> existingCases = queryHelper.searchCases(campaign.getOid(), null, null, result);
		if (!existingCases.isEmpty()) {
		    throw new IllegalStateException("Unexpected " + existingCases.size() + " certification case(s) in campaign object "
				    + toShortString(campaign) + ". At this time there should be none.");
		}
	}

	private List<AccessCertificationWorkItemType> createWorkItems(List<ObjectReferenceType> forReviewers, int forStage,
		    int forIteration, AccessCertificationCaseType _case) {
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
				        LOGGER.trace("Skipping creation of a work item for {}, because the relevant outcome already exists in {}", reviewer, existing);
				        break;
			        }
		        }
	        }
	        if (!skipCreation) {
		        AccessCertificationWorkItemType workItem = new AccessCertificationWorkItemType(prismContext)
				        .stageNumber(forStage)
				        .iteration(forIteration)
				        .assigneeRef(reviewer.clone())
				        .originalAssigneeRef(reviewer.clone());
		        workItems.add(workItem);
	        }
        }
        return workItems;
    }

	/**
	 * Deltas to advance cases to next stage when opening it.
	 */
    void getDeltasToAdvanceCases(AccessCertificationCampaignType campaign, AccessCertificationStageType stage,
		    ModificationsToExecute modifications, Holder<Integer> workItemsCreatedHolder,
		    Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {

        LOGGER.trace("Advancing reviewers and timestamps for cases in {}", toShortString(campaign));
	    int stageToBe = campaign.getStageNumber() + 1;
	    int iteration = campaign.getIteration();
        List<AccessCertificationCaseType> caseList = queryHelper.getAllCurrentIterationCases(campaign.getOid(), iteration, null, result);

        List<AccessCertificationResponseType> outcomesToStopOn = stageToBe > 1 ?
		        computationHelper.getOutcomesToStopOn(campaign) : emptyList();

        AccessCertificationReviewerSpecificationType reviewerSpec =
                reviewersHelper.findReviewersSpecification(campaign, stageToBe);

        for (AccessCertificationCaseType _case : caseList) {
            if (!computationHelper.advancesToNextStage(_case, campaign, outcomesToStopOn)) {
                continue;
            }
            Long caseId = _case.asPrismContainerValue().getId();
            assert caseId != null;
			List<ObjectReferenceType> reviewers = reviewersHelper.getReviewersForCase(_case, campaign, reviewerSpec, task, result);
			List<AccessCertificationWorkItemType> workItems = createWorkItems(reviewers, stageToBe, iteration, _case);
			workItemsCreatedHolder.setValue(workItemsCreatedHolder.getValue() + workItems.size());
			_case.getWorkItem().addAll(CloneUtil.cloneCollectionMembers(workItems));
			AccessCertificationResponseType currentOutcome = computationHelper.computeOutcomeForStage(_case, campaign, stageToBe);
			AccessCertificationResponseType overallOutcome = computationHelper.computeOverallOutcome(_case, campaign, currentOutcome);
			modifications.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
					.item(F_CASE, caseId, F_WORK_ITEM).add(PrismContainerValue.toPcvList(workItems))
					.item(F_CASE, caseId, F_CURRENT_STAGE_CREATE_TIMESTAMP).replace(stage.getStartTimestamp())
					.item(F_CASE, caseId, F_CURRENT_STAGE_DEADLINE).replace(stage.getDeadline())
					.item(F_CASE, caseId, F_CURRENT_STAGE_OUTCOME).replace(toUri(currentOutcome))
					.item(F_CASE, caseId, F_OUTCOME).replace(toUri(overallOutcome))
					.item(F_CASE, caseId, F_STAGE_NUMBER).replace(stageToBe)
					.item(F_CASE, caseId, F_ITERATION).replace(iteration)
					.asItemDeltas());
        }

        LOGGER.debug("Created {} deltas (in {} batches) to advance {} cases for campaign {}; work items created: {}",
		        modifications.getTotalDeltasCount(), modifications.batches.size(), caseList.size(), toShortString(campaign),
		        workItemsCreatedHolder.getValue());
    }

    // computes cases outcomes (stage-level and overall) at stage close and creates appropriate deltas
    void createOutcomeAndEventDeltasForCasesOnStageClose(AccessCertificationCampaignType campaign, ModificationsToExecute modifications,
		    OperationResult result) throws SchemaException {
	    LOGGER.trace("Updating current outcome for cases in {}", toShortStringLazy(campaign));
        List<AccessCertificationCaseType> caseList = queryHelper.getAllCurrentIterationCases(campaign.getOid(), campaign.getIteration(), null, result);
		for (AccessCertificationCaseType _case : caseList) {
			if (!AccCertUtil.isCaseRelevantForStage(_case, campaign)) {
				continue;
			}
			List<ItemDelta<?,?>> deltas = new ArrayList<>();
			String newStageOutcome = toUri(computationHelper.computeOutcomeForStage(_case, campaign, campaign.getStageNumber()));
			long caseId = _case.asPrismContainerValue().getId();
			if (!Objects.equals(newStageOutcome, _case.getCurrentStageOutcome())) {
				deltas.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
						.item(F_CASE, caseId, F_CURRENT_STAGE_OUTCOME).replace(newStageOutcome)
						.asItemDelta());
			}
			deltas.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
					.item(F_CASE, caseId, F_EVENT).add(new StageCompletionEventType()
							.timestamp(clock.currentTimeXMLGregorianCalendar())
							.stageNumber(campaign.getStageNumber())
							.iteration(campaign.getIteration())
							.outcome(newStageOutcome))
					.asItemDelta());
			String newOverallOutcome = toUri(computationHelper.computeOverallOutcome(_case, campaign, newStageOutcome));
			if (!Objects.equals(newOverallOutcome, _case.getOutcome())) {
				deltas.add(DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
						.item(F_CASE, caseId, F_OUTCOME).replace(newOverallOutcome)
						.asItemDelta());
			}
			modifications.add(deltas);
		}
    }

    // TODO temporary implementation - should be done somehow in batches in order to improve performance
	void markCaseAsRemedied(@NotNull String campaignOid, long caseId, Task task, OperationResult parentResult)
			throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        PropertyDelta<XMLGregorianCalendar> remediedDelta = PropertyDelta.createModificationReplaceProperty(
                new ItemPath(
                        new NameItemPathSegment(F_CASE),
                        new IdItemPathSegment(caseId),
                        new NameItemPathSegment(AccessCertificationCaseType.F_REMEDIED_TIMESTAMP)),
                generalHelper.getCampaignObjectDefinition(), XmlTypeConverter.createXMLGregorianCalendar(new Date()));

        updateHelper.modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaignOid,
				Collections.singletonList(remediedDelta), task, parentResult);
    }
}
