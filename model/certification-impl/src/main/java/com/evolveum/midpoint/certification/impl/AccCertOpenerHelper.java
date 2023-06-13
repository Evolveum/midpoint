/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.certification.api.OutcomeUtils;
import com.evolveum.midpoint.certification.impl.handlers.CertificationHandler;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryConverter;
import com.evolveum.midpoint.prism.query.TypedObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;
import java.util.Objects;

import static com.evolveum.midpoint.certification.api.OutcomeUtils.*;
import static com.evolveum.midpoint.schema.util.CertCampaignTypeUtil.norm;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortString;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.toShortStringLazy;
import static com.evolveum.midpoint.util.MiscUtil.or0;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CREATED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CURRENT_STAGE_CREATE_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_ID_USED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_STARTED_TIMESTAMP;

/**
 * Supports campaign and stage opening operations.
 */
@Component
public class AccCertOpenerHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AccCertOpenerHelper.class);

    @Autowired private AccCertReviewersHelper reviewersHelper;
    @Autowired private AccCertEventHelper eventHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private SecurityContextManager securityContextManager;
    @Autowired private AccCertGeneralHelper generalHelper;
    @Autowired protected AccCertQueryHelper queryHelper;
    @Autowired private Clock clock;
    @Autowired private AccCertResponseComputationHelper computationHelper;
    @Autowired private AccCertUpdateHelper updateHelper;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    //region ================================ Campaign create ================================

    AccessCertificationCampaignType createCampaign(PrismObject<AccessCertificationDefinitionType> definition,
            OperationResult result, Task task)
            throws SchemaException, SecurityViolationException, ObjectAlreadyExistsException, ObjectNotFoundException {
        AccessCertificationCampaignType newCampaign = createCampaignObject(definition.asObjectable(), task, result);
        updateHelper.addObjectPreAuthorized(newCampaign, task, result);
        return newCampaign;
    }

    private AccessCertificationCampaignType createCampaignObject(AccessCertificationDefinitionType definition,
            Task task, OperationResult result)
            throws SchemaException, SecurityViolationException {
        AccessCertificationCampaignType newCampaign = new AccessCertificationCampaignType();

        if (definition.getName() != null) {
            newCampaign.setName(generateCampaignName(definition, task, result));
        } else {
            throw new SchemaException("Couldn't create a campaign without name");
        }

        newCampaign.setDescription(definition.getDescription());
        newCampaign.setOwnerRef(securityContextManager.getPrincipal().toObjectReference());
        newCampaign.setTenantRef(definition.getTenantRef());
        newCampaign.setDefinitionRef(ObjectTypeUtil.createObjectRef(definition, prismContext));

        if (definition.getHandlerUri() != null) {
            newCampaign.setHandlerUri(definition.getHandlerUri());
        } else {
            throw new SchemaException("Couldn't create a campaign without handlerUri");
        }

        newCampaign.setScopeDefinition(definition.getScopeDefinition());
        newCampaign.setRemediationDefinition(definition.getRemediationDefinition());
        newCampaign.setReiterationDefinition(definition.getReiterationDefinition());

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
        ObjectFilter objectFilter;
        Class<? extends ObjectType> objectClass;
        Class<? extends ObjectType> focusClass = focus.asObjectable().getClass();
        if ((campaign.getScopeDefinition() instanceof AccessCertificationObjectBasedScopeType)) {
            scope = (AccessCertificationObjectBasedScopeType) campaign.getScopeDefinition();
            ObjectFilter focusFilter = prismContext.queryFor(focusClass).id(focus.getOid()).buildFilter();
            Class<? extends ObjectType> originalObjectClass = ObjectTypes.getObjectTypeClass(scope.getObjectType());
            if (scope.getSearchFilter() != null) {
                ObjectFilter parsedFilter = getQueryConverter().parseFilter(scope.getSearchFilter(), originalObjectClass);
                objectFilter = prismContext.queryFactory().createAnd(parsedFilter, focusFilter);
            } else {
                objectFilter = prismContext.queryFor(focusClass).id(focus.getOid()).buildFilter();
            }
            objectClass = originalObjectClass;
        } else {
            // TODO!
            scope = new AccessCertificationAssignmentReviewScopeType();
            campaign.setScopeDefinition(scope);
            objectFilter = prismContext.queryFor(focusClass).id(focus.getOid()).buildFilter();
            objectClass = focusClass;
        }

        scope.setObjectType(ObjectTypes.getObjectType(objectClass).getTypeQName());
        scope.setSearchFilter(getQueryConverter().createSearchFilterType(objectFilter));
        return campaign;
    }

    private PolyStringType generateCampaignName(AccessCertificationDefinitionType definition, Task task, OperationResult result) throws SchemaException {
        String prefix = definition.getName().getOrig();
        int lastCampaignIdUsed = or0(definition.getLastCampaignIdUsed());
        for (int i = lastCampaignIdUsed + 1; ; i++) {
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
            List<ItemDelta<?,?>> modifications = prismContext.deltaFor(AccessCertificationDefinitionType.class)
                    .item(F_LAST_CAMPAIGN_ID_USED).replace(lastIdUsed)
                    .asItemDeltas();
            updateHelper.modifyObjectPreAuthorized(AccessCertificationDefinitionType.class, definitionOid, modifications, task, result);
        } catch (SchemaException|ObjectNotFoundException|RuntimeException|ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update last campaign ID for definition {}", e, definitionOid);
        }
    }

    //endregion

    //region ================================ Stage open ================================

    private static class OpeningContext {
        int casesEnteringStage;
        int workItemsCreated;
    }

    void openNextStage(AccessCertificationCampaignType campaign, CertificationHandler handler, Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        boolean skipEmptyStages = norm(campaign.getIteration()) > 1;        // TODO make configurable
        int requestedStageNumber = campaign.getStageNumber() + 1;
        for (;;) {
            OpeningContext openingContext = new OpeningContext();
            AccessCertificationStageType stage = createStage(campaign, requestedStageNumber);
            ModificationsToExecute modifications = getDeltasForStageOpen(campaign, stage, handler, openingContext, task, result);
            if (!skipEmptyStages || openingContext.casesEnteringStage > 0) {
                updateHelper.modifyCampaignPreAuthorized(campaign.getOid(), modifications, task, result);
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
            OpeningContext openingContext, final Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        int stageNumber = campaign.getStageNumber();
        int newStageNumber = stage.getNumber();

        LOGGER.trace("getDeltasForStageOpen starting; campaign = {}, stage number = {}, new stage number = {}, iteration = {}",
                ObjectTypeUtil.toShortStringLazy(campaign), stageNumber, newStageNumber, norm(campaign.getIteration()));

        ModificationsToExecute rv = new ModificationsToExecute();
        if (stageNumber == 0 && norm(campaign.getIteration()) == 1) {
            getDeltasToCreateCases(campaign, stage, handler, rv, openingContext, task, result);
        } else {
            getDeltasToUpdateCases(campaign, stage, rv, openingContext, task, result);
        }
        rv.createNewBatch();
        rv.add(createStageAddDelta(stage));
        rv.add(createDeltasToRecordStageOpen(campaign, stage));
        rv.add(updateHelper.getDeltasToCreateTriggersForTimedActions(campaign.getOid(), 0,
                XmlTypeConverter.toDate(stage.getStartTimestamp()), XmlTypeConverter.toDate(stage.getDeadline()),
                CertCampaignTypeUtil.findStageDefinition(campaign, newStageNumber).getTimedActions()));

        if (LOGGER.isTraceEnabled()) {
            List<ItemDelta<?, ?>> allDeltas = rv.getAllDeltas();
            LOGGER.trace("getDeltasForStageOpen finishing, returning {} deltas (in {} batches):\n{}",
                    allDeltas.size(), rv.batches.size(), DebugUtil.debugDump(allDeltas));
        }
        return rv;
    }

    /**
     *  Creates certification cases (in the form of delta list) on first stage opening.
     */
    private <F extends FocusType> void getDeltasToCreateCases(AccessCertificationCampaignType campaign,
            AccessCertificationStageType stage,
            CertificationHandler handler, ModificationsToExecute modifications, OpeningContext openingContext, Task task,
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

        assert norm(campaign.getIteration()) == 1;

        for (AccessCertificationCaseType aCase : caseList) {
            ContainerDelta<AccessCertificationCaseType> caseDelta = prismContext.deltaFactory().container().createDelta(F_CASE,
                    AccessCertificationCampaignType.class);
            aCase.setIteration(1);
            aCase.setStageNumber(1);
            aCase.setCurrentStageCreateTimestamp(stage.getStartTimestamp());
            aCase.setCurrentStageDeadline(stage.getDeadline());

            List<ObjectReferenceType> reviewers = reviewersHelper.getReviewersForCase(aCase, campaign, reviewerSpec, task, result);
            aCase.getWorkItem().addAll(createWorkItems(reviewers, 1, 1, aCase));

            openingContext.workItemsCreated += aCase.getWorkItem().size();
            openingContext.casesEnteringStage++;

            AccessCertificationResponseType currentStageOutcome = computationHelper.computeOutcomeForStage(aCase, campaign, 1);
            aCase.setCurrentStageOutcome(toUri(currentStageOutcome));
            aCase.setOutcome(toUri(computationHelper.computeOverallOutcome(aCase, campaign, 1, currentStageOutcome)));

            @SuppressWarnings({ "raw", "unchecked" })
            PrismContainerValue<AccessCertificationCaseType> caseCVal = aCase.asPrismContainerValue();
            caseDelta.addValueToAdd(caseCVal);
            LOGGER.trace("Adding certification case:\n{}", caseCVal.debugDumpLazily());
            modifications.add(caseDelta);
        }

        LOGGER.trace("Created {} deltas (in {} batches) to create {} cases ({} work items) for campaign {}",
                modifications.getTotalDeltasCount(), modifications.batches.size(), caseList.size(),
                openingContext.workItemsCreated, campaignShortName);
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
        ObjectQuery query = prismContext.queryFactory().createQuery();
        if (searchFilter != null) {
            query.setFilter(getQueryConverter().parseFilter(searchFilter, objectClass));
        }
        return new TypedObjectQuery<>(objectClass, query);
    }

    private void assertNoExistingCases(AccessCertificationCampaignType campaign, OperationResult result)
            throws SchemaException {
        List<AccessCertificationCaseType> existingCases = queryHelper.searchCases(campaign.getOid(), null, result);
        if (!existingCases.isEmpty()) {
            throw new IllegalStateException("Unexpected " + existingCases.size() + " certification case(s) in campaign object "
                    + toShortString(campaign) + ". At this time there should be none.");
        }
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

    /**
     * Deltas to advance cases to next stage when opening it.
     */
    private void getDeltasToUpdateCases(AccessCertificationCampaignType campaign, AccessCertificationStageType stage,
            ModificationsToExecute modifications, OpeningContext openingContext,
            Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {

        int stageToBe = stage.getNumber();
        int iteration = norm(campaign.getIteration());
        LOGGER.trace("Updating cases in {}; current stage = {}, stageToBe = {}, iteration = {}", toShortStringLazy(campaign),
                campaign.getStageNumber(), stageToBe, iteration);
        List<AccessCertificationCaseType> caseList = queryHelper.getAllCurrentIterationCases(campaign.getOid(), iteration, result);

        AccessCertificationReviewerSpecificationType reviewerSpec =
                reviewersHelper.findReviewersSpecification(campaign, stageToBe);

        LOGGER.trace("cases: {}, reviewerSpec: {}", caseList.size(), reviewerSpec);
        for (AccessCertificationCaseType aCase : caseList) {
            LOGGER.trace("----------------------------------------------------------------------------------------");
            LOGGER.trace("Considering case: {}", aCase);
            Long caseId = aCase.asPrismContainerValue().getId();
            assert caseId != null;
            if (aCase.getReviewFinishedTimestamp() != null) {
                LOGGER.trace("Case {} review process has already finished", caseId);
                continue;
            }
            AccessCertificationResponseType stageOutcome = computationHelper.getStageOutcome(aCase, stageToBe);
            if (OutcomeUtils.normalizeToNull(stageOutcome) != null) {
                LOGGER.trace("Case {} already has an outcome for stage {} - it will not be reviewed in this stage in iteration {}",
                        caseId, stageToBe, iteration);
                continue;
            }

            List<ObjectReferenceType> reviewers = reviewersHelper.getReviewersForCase(aCase, campaign, reviewerSpec, task, result);
            List<AccessCertificationWorkItemType> workItems = createWorkItems(reviewers, stageToBe, iteration, aCase);
            openingContext.workItemsCreated += workItems.size();
            openingContext.casesEnteringStage++;
            aCase.getWorkItem().addAll(CloneUtil.cloneCollectionMembers(workItems));
            AccessCertificationResponseType currentStageOutcome = computationHelper.computeOutcomeForStage(aCase, campaign, stageToBe);
            AccessCertificationResponseType overallOutcome = computationHelper.computeOverallOutcome(aCase, campaign, stageToBe, currentStageOutcome);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Computed: reviewers: {}, workItems: {}, currentStageOutcome: {}, overallOutcome: {}",
                        PrettyPrinter.prettyPrint(reviewers), workItems.size(), currentStageOutcome, overallOutcome);
            }
            modifications.add(prismContext.deltaFor(AccessCertificationCampaignType.class)
                    .item(F_CASE, caseId, F_WORK_ITEM).add(PrismContainerValue.toPcvList(workItems))
                    .item(F_CASE, caseId, F_CURRENT_STAGE_CREATE_TIMESTAMP).replace(stage.getStartTimestamp())
                    .item(F_CASE, caseId, F_CURRENT_STAGE_DEADLINE).replace(stage.getDeadline())
                    .item(F_CASE, caseId, F_CURRENT_STAGE_OUTCOME).replace(toUri(currentStageOutcome))
                    .item(F_CASE, caseId, F_OUTCOME).replace(toUri(overallOutcome))
                    .item(F_CASE, caseId, F_STAGE_NUMBER).replace(stageToBe)
                    .item(F_CASE, caseId, F_ITERATION).replace(iteration)
                    .asItemDeltas());
        }

        LOGGER.debug("Created {} deltas (in {} batches) to advance {} out of {} cases for campaign {}; work items created: {}",
                modifications.getTotalDeltasCount(), modifications.batches.size(), openingContext.casesEnteringStage,
                caseList.size(), toShortString(campaign), openingContext.workItemsCreated);
    }

    // some bureaucracy... stage#, state, start time, triggers
    private List<ItemDelta<?,?>> createDeltasToRecordStageOpen(AccessCertificationCampaignType campaign,
            AccessCertificationStageType newStage) throws SchemaException {

        List<ItemDelta<?,?>> itemDeltaList = new ArrayList<>();

        itemDeltaList.add(updateHelper.createStageNumberDelta(newStage.getNumber()));
        itemDeltaList.add(updateHelper.createStateDelta(IN_REVIEW_STAGE));

        boolean campaignJustStarted = campaign.getStageNumber() == 0;
        if (campaignJustStarted) {
            itemDeltaList.add(updateHelper.createStartTimeDelta(clock.currentTimeXMLGregorianCalendar()));
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

            ContainerDelta<TriggerType> triggerDelta = prismContext.deltaFactory().container()
                    .createModificationReplace(ObjectType.F_TRIGGER, AccessCertificationCampaignType.class, triggers);
            itemDeltaList.add(triggerDelta);
        }
        return itemDeltaList;
    }

    private AccessCertificationStageType createStage(AccessCertificationCampaignType campaign, int requestedStageNumber) {
        AccessCertificationStageType stage = new AccessCertificationStageType();
        stage.setIteration(norm(campaign.getIteration()));
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

        updateHelper.notifyReviewers(campaign, false, task, result);

        if (newStage.getNumber() == 1 && norm(campaign.getIteration()) == 1 && campaign.getDefinitionRef() != null) {
            List<ItemDelta<?,?>> deltas = prismContext.deltaFor(AccessCertificationDefinitionType.class)
                    .item(F_LAST_CAMPAIGN_STARTED_TIMESTAMP).replace(clock.currentTimeXMLGregorianCalendar())
                    .asItemDeltas();
            updateHelper.modifyObjectPreAuthorized(AccessCertificationDefinitionType.class, campaign.getDefinitionRef().getOid(), deltas, task, result);
        }
    }

    //endregion

    //region ================================ Campaign reiteration ================================

    void reiterateCampaign(AccessCertificationCampaignType campaign, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        LOGGER.info("Reiterating campaign {}", ObjectTypeUtil.toShortString(campaign));
        if (campaign.getState() != CLOSED) {
            throw new IllegalStateException("Campaign is not in CLOSED state");
        }
        if (campaign.getReiterationDefinition() != null && campaign.getReiterationDefinition().getLimit() != null
                && norm(campaign.getIteration()) >= campaign.getReiterationDefinition().getLimit()) {
            throw new IllegalStateException("Campaign cannot be reiterated: maximum number of iterations ("
                + campaign.getReiterationDefinition().getLimit() + ") was reached.");
        }
        ModificationsToExecute modifications = new ModificationsToExecute();
        modifications.add(updateHelper.createStageNumberDelta(0));
        modifications.add(updateHelper.createStateDelta(CREATED));
        modifications.add(updateHelper.createTriggerDeleteDelta());
        modifications.add(updateHelper.createStartTimeDelta(null));
        modifications.add(updateHelper.createEndTimeDelta(null));
        int newIteration = norm(campaign.getIteration()) + 1;
        modifications.add(prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(AccessCertificationCampaignType.F_ITERATION).replace(newIteration)
                .asItemDelta());

        createCasesReiterationDeltas(campaign, newIteration, modifications, result);

        updateHelper.modifyCampaignPreAuthorized(campaign.getOid(), modifications, task, result);
    }

    private void createCasesReiterationDeltas(AccessCertificationCampaignType campaign, int newIteration,
            ModificationsToExecute modifications, OperationResult result) throws SchemaException {
        ObjectQuery unresolvedCasesQuery = prismContext.queryFor(AccessCertificationCaseType.class)
                .item(AccessCertificationCaseType.F_OUTCOME).eq(SchemaConstants.MODEL_CERTIFICATION_OUTCOME_NO_RESPONSE)
                .build();
        List<AccessCertificationCaseType> unresolvedCases =
                queryHelper.searchCases(campaign.getOid(), unresolvedCasesQuery, result);
        for (AccessCertificationCaseType aCase : unresolvedCases) {
            modifications.add(
                    prismContext.deltaFor(AccessCertificationCampaignType.class)
                            .item(F_CASE, aCase.getId(), F_ITERATION).replace(newIteration)
                            .item(F_CASE, aCase.getId(), F_STAGE_NUMBER).replace(0)
                            .item(F_CASE, aCase.getId(), F_CURRENT_STAGE_OUTCOME).replace()
                            .item(F_CASE, aCase.getId(), F_CURRENT_STAGE_DEADLINE).replace()
                            .item(F_CASE, aCase.getId(), F_CURRENT_STAGE_CREATE_TIMESTAMP).replace()
                            .item(F_CASE, aCase.getId(), F_REVIEW_FINISHED_TIMESTAMP).replace()
                            .asItemDeltas());
        }
    }

    //endregion

    //region ================================ Misc / helper methods ================================
    private ItemDelta<?, ?> createStageAddDelta(AccessCertificationStageType stage) throws SchemaException {
        return prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(F_STAGE).add(stage)
                .asItemDelta();
    }

    private QueryConverter getQueryConverter() {
        return prismContext.getQueryConverter();
    }
    //endregion
}
