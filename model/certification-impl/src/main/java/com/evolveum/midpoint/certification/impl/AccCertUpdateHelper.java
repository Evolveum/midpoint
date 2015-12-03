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
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationObjectBasedScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationReviewerSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationStageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import net.sf.jasperreports.engine.util.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REVIEW_STAGE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.REVIEW_STAGE_DONE;

/**
 * @author mederly
 */
@Component
public class AccCertUpdateHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(AccCertUpdateHelper.class);

    @Autowired
    private AccCertReviewersHelper reviewersHelper;

    @Autowired
    protected AccCertEventHelper eventHelper;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private ModelService modelService;

    @Autowired
    protected SecurityEnforcer securityEnforcer;

    @Autowired
    protected AccCertGeneralHelper helper;

    @Autowired
    protected AccCertResponseComputationHelper computationHelper;

    @Autowired
    protected AccCertQueryHelper queryHelper;

    // TODO temporary hack because of some problems in model service...
    @Autowired
    @Qualifier("cacheRepositoryService")
    protected RepositoryService repositoryService;

    void addObject(ObjectType objectType, Task task, OperationResult result) throws CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException, PolicyViolationException, SchemaException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        ObjectDelta objectDelta = ObjectDelta.createAddDelta(objectType.asPrismObject());
        Collection<ObjectDeltaOperation<?>> ops = modelService.executeChanges((Collection) Arrays.asList(objectDelta), null, task, result);
        ObjectDeltaOperation odo = ops.iterator().next();
        objectType.setOid(odo.getObjectDelta().getOid());
    }

    void recordDecision(AccessCertificationCampaignType campaign, long caseId, AccessCertificationDecisionType decision, OperationResult result) throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
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
        if (decision.getReviewerRef() == null) {
            UserType currentUser = securityEnforcer.getPrincipal().getUser();
            decision.setReviewerRef(ObjectTypeUtil.createObjectRef(currentUser));
        }

        AccessCertificationCaseType _case = queryHelper.getCase(campaignOid, caseId, result);
        if (_case == null) {
            throw new ObjectNotFoundException("Case " + caseId + " was not found in campaign " + ObjectTypeUtil.toShortString(campaign));
        }
//        if (!_case.isEnabled()) {
//            throw new IllegalStateException("Cannot update case because it is not update-enabled in this stage");
//        }

        AccessCertificationDecisionType existingDecision = CertCampaignTypeUtil.findDecision(_case, currentStage, decision.getReviewerRef().getOid());

        if (existingDecision != null && existingDecision.equals(decision)) {
            // very unprobable, but ... to be sure let's check it
            // we skip the operation because add+delete the same value is unpredictable
        } else {
            ItemPath decisionPath = new ItemPath(
                    new NameItemPathSegment(AccessCertificationCampaignType.F_CASE),
                    new IdItemPathSegment(caseId),
                    new NameItemPathSegment(AccessCertificationCaseType.F_DECISION));
            Collection<ItemDelta> deltaList = new ArrayList<>();

            // let's remove existing decision and add the new one
            if (existingDecision != null) {
                ContainerDelta<AccessCertificationDecisionType> decisionDeleteDelta =
                        ContainerDelta.createModificationDelete(decisionPath, AccessCertificationCampaignType.class, prismContext, existingDecision.clone());
                deltaList.add(decisionDeleteDelta);
            }

            ContainerDelta<AccessCertificationDecisionType> decisionAddDelta =
                    ContainerDelta.createModificationAdd(decisionPath, AccessCertificationCampaignType.class, prismContext, decision);
            deltaList.add(decisionAddDelta);

            AccessCertificationResponseType newResponse = computationHelper.computeResponseForStage(_case, decision, campaign);
            if (!ObjectUtils.equals(newResponse, _case.getCurrentResponse())) {
                PropertyDelta currentResponseDelta = PropertyDelta.createModificationReplaceProperty(
                        new ItemPath(
                                new NameItemPathSegment(AccessCertificationCampaignType.F_CASE),
                                new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                                new NameItemPathSegment(AccessCertificationCaseType.F_CURRENT_RESPONSE)),
                        helper.getCampaignObjectDefinition(), newResponse);
                deltaList.add(currentResponseDelta);
            }
            // TODO: call model service instead of repository
            repositoryService.modifyObject(AccessCertificationCampaignType.class, campaignOid, deltaList, result);
        }
    }

    void closeCampaign(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        int currentStageNumber = campaign.getStageNumber();
        int lastStageNumber = CertCampaignTypeUtil.getNumberOfStages(campaign);
        AccessCertificationCampaignStateType currentState = campaign.getState();
        // TODO issue a warning if we are not in a correct state
        PropertyDelta<Integer> stageNumberDelta = createStageNumberDelta(lastStageNumber + 1);
        PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(CLOSED);
        ContainerDelta triggerDelta = createTriggerDeleteDelta();
        repositoryService.modifyObject(AccessCertificationCampaignType.class, campaign.getOid(),
                Arrays.asList(stateDelta, stageNumberDelta, triggerDelta), result);

        AccessCertificationCampaignType updatedCampaign = refreshCampaign(campaign, task, result);
//        if (currentState == IN_REMEDIATION) {
//            eventHelper.onCampaignStageEnd(updatedCampaign, task, result);
//        }
        eventHelper.onCampaignEnd(updatedCampaign, task, result);
    }

    // TODO implement more efficiently
    public AccessCertificationCampaignType refreshCampaign(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(AccessCertificationCampaignType.class, campaign.getOid(), null, result).asObjectable();
    }

    void setStageNumberAndState(AccessCertificationCampaignType campaign, int number, AccessCertificationCampaignStateType state,
                                        Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        PropertyDelta<Integer> stageNumberDelta = createStageNumberDelta(number);
        PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(state);
        // todo switch to model service
        repositoryService.modifyObject(AccessCertificationCampaignType.class, campaign.getOid(), Arrays.asList(stateDelta, stageNumberDelta), result);
    }

    private void setState(AccessCertificationCampaignType campaign, AccessCertificationCampaignStateType state, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(state);
        // todo switch to model service
        repositoryService.modifyObject(AccessCertificationCampaignType.class, campaign.getOid(), Arrays.asList(stateDelta), result);
    }

    private PropertyDelta<Integer> createStageNumberDelta(int number) {
        return PropertyDelta.createReplaceDelta(helper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_STAGE_NUMBER, number);
    }

    private PropertyDelta<AccessCertificationCampaignStateType> createStateDelta(AccessCertificationCampaignStateType state) {
        return PropertyDelta.createReplaceDelta(helper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_STATE, state);
    }

    private PropertyDelta<XMLGregorianCalendar> createStartTimeDelta(XMLGregorianCalendar date) {
        return PropertyDelta.createReplaceDelta(helper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_START, date);
    }

    public void moveToNextStage(AccessCertificationCampaignType campaign, AccessCertificationStageType stage, CertificationHandler handler, final Task task, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException {
        Validate.notNull(campaign, "certificationCampaign");
        Validate.notNull(campaign.getOid(), "certificationCampaign.oid");

        int stageNumber = campaign.getStageNumber();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("moveToNextStage starting; campaign = {}, stage number = {}",
                    ObjectTypeUtil.toShortString(campaign), stageNumber);
        }

        if (stageNumber == 0) {
            createCases(campaign, stage, handler, task, result);
        } else {
            updateCases(campaign, stage, task, result);
        }

        LOGGER.trace("moveToNextStage finishing");
    }

    protected AccessCertificationStageType createStage(AccessCertificationCampaignType campaign, int requestedStageNumber) {
        AccessCertificationStageType stage = new AccessCertificationStageType(prismContext);
        stage.setNumber(requestedStageNumber);
        stage.setStart(XmlTypeConverter.createXMLGregorianCalendar(new Date()));

        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, stage.getNumber());
        XMLGregorianCalendar end = (XMLGregorianCalendar) stage.getStart().clone();
        end.add(XmlTypeConverter.createDuration(true, 0, 0, stageDef.getDays(), 0, 0, 0));
        end.setHour(23);
        end.setMinute(59);
        end.setSecond(59);
        end.setMillisecond(999);
        stage.setEnd(end);

        stage.setName(stageDef.getName());
        stage.setDescription(stageDef.getDescription());

        return stage;
    }

    private void createCases(final AccessCertificationCampaignType campaign, AccessCertificationStageType stage,
                             final CertificationHandler handler, final Task task, final OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException, PolicyViolationException, ObjectAlreadyExistsException {
        String campaignShortName = ObjectTypeUtil.toShortString(campaign);

        AccessCertificationScopeType scope = campaign.getScopeDefinition();
        LOGGER.trace("Creating cases for scope {} in campaign {}", scope, campaignShortName);

        if (scope != null && !(scope instanceof AccessCertificationObjectBasedScopeType)) {
            throw new IllegalStateException("Unsupported access certification scope type: " + scope.getClass() + " for campaign " + campaignShortName);
        }
        AccessCertificationObjectBasedScopeType objectBasedScope = (AccessCertificationObjectBasedScopeType) scope;

        List<AccessCertificationCaseType> existingCases = queryHelper.searchCases(campaign.getOid(), null, null, task, result);
        if (!existingCases.isEmpty()) {
            throw new IllegalStateException("Unexpected " + existingCases.size() + " certification case(s) in campaign object " + campaignShortName + ". At this time there should be none.");
        }

        // create a query to find target objects from which certification cases will be created
        ObjectQuery query = new ObjectQuery();
        QName objectType = objectBasedScope != null ? objectBasedScope.getObjectType() : null;
        if (objectType == null) {
            objectType = handler.getDefaultObjectType();
        }
        if (objectType == null) {
            throw new IllegalStateException("Unspecified object type (and no default one provided) for campaign " + campaignShortName);
        }
        PrismObjectDefinition<? extends ObjectType> objectDef = prismContext.getSchemaRegistry().findObjectDefinitionByType(objectType);
        if (objectDef == null) {
            throw new IllegalStateException("Object definition not found for object type " + objectType + " in campaign " + campaignShortName);
        }
        Class<? extends ObjectType> objectClass = objectDef.getCompileTimeClass();
        if (objectClass == null) {
            throw new IllegalStateException("Object class not found for object type " + objectType + " in campaign " + campaignShortName);
        }

        SearchFilterType searchFilter = objectBasedScope != null ? objectBasedScope.getSearchFilter() : null;
        if (searchFilter != null) {
            ObjectFilter filter = QueryConvertor.parseFilter(searchFilter, objectClass, prismContext);
            query.setFilter(filter);
        }

        final List<AccessCertificationCaseType> caseList = new ArrayList<>();

        // create certification cases by executing the query and caseExpression on its results
        // here the subclasses of this class come into play
        ResultHandler<ObjectType> resultHandler = new ResultHandler<ObjectType>() {
            @Override
            public boolean handle(PrismObject<ObjectType> object, OperationResult parentResult) {
                try {
                    caseList.addAll(handler.createCasesForObject(object, campaign, task, parentResult));
                } catch (ExpressionEvaluationException|ObjectNotFoundException|SchemaException e) {
                    // TODO process the exception more intelligently
                    throw new SystemException("Cannot create certification case for object " + ObjectTypeUtil.toShortString(object.asObjectable()) + ": " + e.getMessage(), e);
                }
                return true;
            }
        };
        modelService.searchObjectsIterative(objectClass, query, (ResultHandler) resultHandler, null, task, result);

        AccessCertificationReviewerSpecificationType reviewerSpec =
                reviewersHelper.findReviewersSpecification(campaign, 1, task, result);

        // put the cases into repository
        ContainerDelta<AccessCertificationCaseType> caseDelta = ContainerDelta.createDelta(AccessCertificationCampaignType.F_CASE,
                AccessCertificationCampaignType.class, prismContext);
        for (int i = 0; i < caseList.size(); i++) {
            AccessCertificationCaseType _case = caseList.get(i);
            _case.setReviewRequestedTimestamp(stage.getStart());
            _case.setReviewDeadline(stage.getEnd());
            _case.setCurrentResponse(null);
            _case.setCurrentStageNumber(1);

            reviewersHelper.setupReviewersForCase(_case, campaign, reviewerSpec, task, result);

            List<AccessCertificationDecisionType> decisions = createEmptyDecisionsForCase(_case.getReviewerRef(), 1);
            _case.getDecision().addAll(decisions);

            PrismContainerValue<AccessCertificationCaseType> caseCVal = _case.asPrismContainerValue();
            caseCVal.setId((long) (i + 1));
            caseDelta.addValueToAdd(caseCVal);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Adding certification case:\n{}", caseCVal.debugDump());
            }
        }

        // there are some problems with container IDs when using model - as a temporary hack we go directly into repo
        // todo fix it and switch to model service
//        ObjectDelta<AccessCertificationCampaignType> campaignDelta = ObjectDelta.createModifyDelta(campaign.getOid(),
//                caseDelta, AccessCertificationCampaignType.class, prismContext);
//        modelService.executeChanges((Collection) Arrays.asList(campaignDelta), null, task, result);

        ContainerDelta<AccessCertificationStageType> stageDelta = ContainerDelta.createDelta(AccessCertificationCampaignType.F_STAGE,
                AccessCertificationCampaignType.class, prismContext);
        stageDelta.addValueToAdd(stage.asPrismContainerValue());

        repositoryService.modifyObject(AccessCertificationCampaignType.class, campaign.getOid(),
                Arrays.asList(caseDelta, stageDelta), result);
        LOGGER.trace("Created stage and {} cases for campaign {}", caseList.size(), campaignShortName);
    }

    // BRUTAL HACK: we fill-in decisions when in stage 1 (to be fixed in repository implementation)
    private List<AccessCertificationDecisionType> createEmptyDecisionsForCase(List<ObjectReferenceType> forReviewers, int forStage) {
        long id = 1;
        List<AccessCertificationDecisionType> decisions = new ArrayList<>();
        for (ObjectReferenceType reviewer : forReviewers) {
            AccessCertificationDecisionType decision = new AccessCertificationDecisionType(prismContext);
            decision.setReviewerRef(reviewer);
            decision.setStageNumber(forStage);
            decision.setResponse(null);
            decision.setTimestamp(null);
            if (forStage == 1) {
                decision.setId(id++);
            }
            decisions.add(decision);
        }
        return decisions;
    }

    private void updateCases(AccessCertificationCampaignType campaign, AccessCertificationStageType stage, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, SecurityViolationException, ConfigurationException, CommunicationException {
        LOGGER.trace("Updating reviewers and timestamps for cases in {}", ObjectTypeUtil.toShortString(campaign));
        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaign.getOid(), null, null, task, result);

        int stageToBe = campaign.getStageNumber() + 1;

        AccessCertificationReviewerSpecificationType reviewerSpec =
                reviewersHelper.findReviewersSpecification(campaign, stageToBe, task, result);

        List<ItemDelta> campaignDeltaList = new ArrayList<>(caseList.size());
        for (int i = 0; i < caseList.size(); i++) {
            AccessCertificationCaseType _case = caseList.get(i);

            boolean enabled = computationHelper.computeEnabled(campaign, _case);
//            PropertyDelta enabledDelta = PropertyDelta.createModificationReplaceProperty(
//                    new ItemPath(
//                            new NameItemPathSegment(AccessCertificationCampaignType.F_CASE),
//                            new IdItemPathSegment(_case.asPrismContainerValue().getId()),
//                            new NameItemPathSegment(AccessCertificationCaseType.F_ENABLED)),
//                    helper.getCampaignObjectDefinition(), enabled);
//            campaignDeltaList.add(enabledDelta);

            if (enabled) {
                reviewersHelper.setupReviewersForCase(_case, campaign, reviewerSpec, task, result);
            } else {
                _case.getReviewerRef().clear();
            }
            PrismReference reviewersRef = _case.asPrismContainerValue().findOrCreateReference(AccessCertificationCaseType.F_REVIEWER_REF);
            ReferenceDelta reviewerDelta = ReferenceDelta.createModificationReplace(
                    new ItemPath(
                            new NameItemPathSegment(AccessCertificationCampaignType.F_CASE),
                            new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                            new NameItemPathSegment(AccessCertificationCaseType.F_REVIEWER_REF)),
                    helper.getCampaignObjectDefinition(), CloneUtil.cloneCollectionMembers(reviewersRef.getValues()));
            campaignDeltaList.add(reviewerDelta);

            List<AccessCertificationDecisionType> newDecisions = createEmptyDecisionsForCase(_case.getReviewerRef(), stageToBe);
            PrismContainerDefinition decisionDef =
                    prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(AccessCertificationCaseType.class)
                        .findContainerDefinition(AccessCertificationCaseType.F_DECISION);
            ContainerDelta decisionDelta = ContainerDelta.createDelta(AccessCertificationCaseType.F_DECISION, decisionDef);
            decisionDelta.addValuesToAdd(newDecisions);

            PropertyDelta reviewRequestedTimestampDelta = PropertyDelta.createModificationReplaceProperty(
                    new ItemPath(
                            new NameItemPathSegment(AccessCertificationCampaignType.F_CASE),
                            new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                            new NameItemPathSegment(AccessCertificationCaseType.F_REVIEW_REQUESTED_TIMESTAMP)),
                    helper.getCampaignObjectDefinition(),
                    enabled ? Arrays.asList(stage.getStart()) : new ArrayList(0));
            campaignDeltaList.add(reviewRequestedTimestampDelta);

            ItemPath deadlinePath = new ItemPath(
                    new NameItemPathSegment(AccessCertificationCampaignType.F_CASE),
                    new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                    new NameItemPathSegment(AccessCertificationCaseType.F_REVIEW_DEADLINE));
            PropertyDelta deadlineDelta = PropertyDelta.createModificationReplaceProperty(deadlinePath,
                    helper.getCampaignObjectDefinition(),
                    enabled ? Arrays.asList(stage.getEnd()) : new ArrayList(0));
            campaignDeltaList.add(deadlineDelta);

            if (enabled) {
                PropertyDelta currentResponseDelta = PropertyDelta.createModificationReplaceProperty(
                        new ItemPath(
                                new NameItemPathSegment(AccessCertificationCampaignType.F_CASE),
                                new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                                new NameItemPathSegment(AccessCertificationCaseType.F_CURRENT_RESPONSE)),
                        helper.getCampaignObjectDefinition());
                campaignDeltaList.add(currentResponseDelta);

                PropertyDelta currentResponseStageDelta = PropertyDelta.createModificationReplaceProperty(
                        new ItemPath(
                                new NameItemPathSegment(AccessCertificationCampaignType.F_CASE),
                                new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                                new NameItemPathSegment(AccessCertificationCaseType.F_CURRENT_STAGE_NUMBER)),
                        helper.getCampaignObjectDefinition(),
                        stageToBe);
                campaignDeltaList.add(currentResponseStageDelta);
            }
        }

        ContainerDelta<AccessCertificationStageType> stageDelta = ContainerDelta.createDelta(AccessCertificationCampaignType.F_STAGE,
                AccessCertificationCampaignType.class, prismContext);
        stageDelta.addValueToAdd(stage.asPrismContainerValue());
        campaignDeltaList.add(stageDelta);

        repositoryService.modifyObject(AccessCertificationCampaignType.class, campaign.getOid(), campaignDeltaList, result);
        LOGGER.debug("Created a stage, updated reviewers and timestamps in {} cases for campaign {}", caseList.size(), ObjectTypeUtil.toShortString(campaign));
    }


    void recordMoveToNextStage(AccessCertificationCampaignType campaign, AccessCertificationStageType newStage, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, SecurityViolationException, ConfigurationException, CommunicationException {
        // some bureaucracy... stage#, state, start time, trigger
        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaign.getOid(), null, null, task, result);
        List<ItemDelta> itemDeltaList = new ArrayList<>();
        PropertyDelta<Integer> stageNumberDelta = createStageNumberDelta(newStage.getNumber());
        itemDeltaList.add(stageNumberDelta);
        PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(IN_REVIEW_STAGE);
        itemDeltaList.add(stateDelta);
        if (newStage.getNumber() == 1) {
            PropertyDelta<XMLGregorianCalendar> startDelta = createStartTimeDelta(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
            itemDeltaList.add(startDelta);
        }
        if (newStage.getEnd() != null) {
            XMLGregorianCalendar end = newStage.getEnd();
            AccessCertificationStageDefinitionType stageDef =
                    CertCampaignTypeUtil.findStageDefinition(campaign, newStage.getNumber());
            List<TriggerType> triggers = new ArrayList<>();

            // pseudo-random ID so this trigger will not be deleted by trigger task handler (if this code itself is executed as part of previous trigger firing)
            // TODO implement this more seriously!
            long lastId = (long) (Math.random() * 1000000000);

            TriggerType triggerClose = new TriggerType(prismContext);
            triggerClose.setHandlerUri(AccessCertificationCloseStageTriggerHandler.HANDLER_URI);
            triggerClose.setTimestamp(end);
            triggerClose.setId(lastId);
            triggers.add(triggerClose);

            for (Integer hoursBeforeDeadline : stageDef.getNotifyBeforeDeadline()) {
                XMLGregorianCalendar beforeEnd = null;
                beforeEnd = CloneUtil.clone(end);
                beforeEnd.add(XmlTypeConverter.createDuration(false, 0, 0, 0, hoursBeforeDeadline, 0, 0));
                if (XmlTypeConverter.toMillis(beforeEnd) > System.currentTimeMillis()) {
                    TriggerType triggerBeforeEnd = new TriggerType(prismContext);
                    triggerBeforeEnd.setHandlerUri(AccessCertificationCloseStageApproachingTriggerHandler.HANDLER_URI);
                    triggerBeforeEnd.setTimestamp(beforeEnd);
                    triggerBeforeEnd.setId(++lastId);
                    triggers.add(triggerBeforeEnd);
                }
            }

            ContainerDelta<TriggerType> triggerDelta = ContainerDelta.createModificationReplace(ObjectType.F_TRIGGER, AccessCertificationCampaignType.class, prismContext, triggers);
            itemDeltaList.add(triggerDelta);
        }
        repositoryService.modifyObject(AccessCertificationCampaignType.class, campaign.getOid(), itemDeltaList, result);

        // notifications
        campaign = refreshCampaign(campaign, task, result);
        if (campaign.getStageNumber() == 1) {
            eventHelper.onCampaignStart(campaign, task, result);
        }
        eventHelper.onCampaignStageStart(campaign, task, result);
        Collection<String> reviewers = eventHelper.getCurrentReviewers(campaign, caseList);
        for (String reviewerOid : reviewers) {
            List<AccessCertificationCaseType> cases = queryHelper.getCasesForReviewer(campaign, reviewerOid, task, result);
            ObjectReferenceType reviewerRef = ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER);
            eventHelper.onReviewRequested(reviewerRef, cases, campaign, task, result);
        }
    }

    void recordCloseCurrentState(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, SecurityViolationException, ConfigurationException, CommunicationException {
        List<ItemDelta> campaignDeltaList = createCurrentResponsesDeltas(campaign, task, result);
        PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(REVIEW_STAGE_DONE);
        campaignDeltaList.add(stateDelta);
        ContainerDelta triggerDelta = createTriggerDeleteDelta();
        campaignDeltaList.add(triggerDelta);
        repositoryService.modifyObject(AccessCertificationCampaignType.class, campaign.getOid(), campaignDeltaList, result);

        campaign = refreshCampaign(campaign, task, result);
        eventHelper.onCampaignStageEnd(campaign, task, result);
    }

    private ContainerDelta createTriggerDeleteDelta() {
        return ContainerDelta.createModificationReplace(ObjectType.F_TRIGGER, helper.getCampaignObjectDefinition());
    }

    private List<ItemDelta> createCurrentResponsesDeltas(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException, SecurityViolationException {
        List<ItemDelta> campaignDeltaList = new ArrayList<>();

        LOGGER.trace("Updating current response for cases in {}", ObjectTypeUtil.toShortString(campaign));
        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaign.getOid(), null, null, task, result);

        for (int i = 0; i < caseList.size(); i++) {
            AccessCertificationCaseType _case = caseList.get(i);
            if (_case.getCurrentStageNumber() != campaign.getStageNumber()) {
                continue;
            }
            AccessCertificationResponseType newResponse = computationHelper.computeResponseForStage(_case, campaign);
            if (newResponse != _case.getCurrentResponse()) {
                if (newResponse == null) {
                    throw new IllegalStateException("Computed currentResponse is null for case id " + _case.asPrismContainerValue().getId());
                }
                PropertyDelta currentResponseDelta = PropertyDelta.createModificationReplaceProperty(
                        new ItemPath(
                                new NameItemPathSegment(AccessCertificationCampaignType.F_CASE),
                                new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                                new NameItemPathSegment(AccessCertificationCaseType.F_CURRENT_RESPONSE)),
                        helper.getCampaignObjectDefinition(), newResponse);
                campaignDeltaList.add(currentResponseDelta);
            }
        }
        return campaignDeltaList;
    }

    // TODO temporary implementation - should be done somehow in batches in order to improve performance
    public void markCaseAsRemedied(String campaignOid, long caseId, Task task, OperationResult parentResult) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException {
        Validate.notNull(campaignOid, "campaignOid");
        Validate.notNull(task, "task");
        Validate.notNull(parentResult, "parentResult");

        PropertyDelta reviewRemediedDelta = PropertyDelta.createModificationReplaceProperty(
                new ItemPath(
                        new NameItemPathSegment(AccessCertificationCampaignType.F_CASE),
                        new IdItemPathSegment(caseId),
                        new NameItemPathSegment(AccessCertificationCaseType.F_REMEDIED_TIMESTAMP)),
                helper.getCampaignObjectDefinition(), XmlTypeConverter.createXMLGregorianCalendar(new Date()));

        repositoryService.modifyObject(AccessCertificationCampaignType.class, campaignOid, Arrays.asList(reviewRemediedDelta), parentResult);
    }

}
