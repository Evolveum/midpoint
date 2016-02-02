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
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
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
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
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
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType;
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
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
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

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.F_CASE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CURRENT_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_CLOSED_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_ID_USED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationDefinitionType.F_LAST_CAMPAIGN_STARTED_TIMESTAMP;

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

    void addObject(ObjectType objectType, Task task, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        ObjectDelta objectDelta = ObjectDelta.createAddDelta(objectType.asPrismObject());
        Collection<ObjectDeltaOperation<?>> ops = null;
        try {
            ops = modelService.executeChanges(
                    Arrays.<ObjectDelta<? extends ObjectType>>asList(objectDelta),
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
//        if (!_case.isEnabled()) {
//            throw new IllegalStateException("Cannot update case because it is not update-enabled in this stage");
//        }

        AccessCertificationDecisionType existingDecision = CertCampaignTypeUtil.findDecision(_case, currentStage, decision.getReviewerRef().getOid());

        if (existingDecision != null && existingDecision.equals(decision)) {
            // very unprobable, but ... to be sure let's check it
            // we skip the operation because add+delete the same value is unpredictable
        } else {
            ItemPath decisionPath = new ItemPath(
                    new NameItemPathSegment(F_CASE),
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
                                new NameItemPathSegment(F_CASE),
                                new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                                new NameItemPathSegment(F_CURRENT_RESPONSE)),
                        generalHelper.getCampaignObjectDefinition(), newResponse);
                deltaList.add(currentResponseDelta);
            }
            modifyObjectViaModel(AccessCertificationCampaignType.class, campaignOid, deltaList, task, result);
        }
    }

    void closeCampaign(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, SecurityViolationException {
        LOGGER.info("Closing campaign {}", ObjectTypeUtil.toShortString(campaign));
        int currentStageNumber = campaign.getStageNumber();
        int lastStageNumber = CertCampaignTypeUtil.getNumberOfStages(campaign);
        AccessCertificationCampaignStateType currentState = campaign.getState();
        // TODO issue a warning if we are not in a correct state
        PropertyDelta<Integer> stageNumberDelta = createStageNumberDelta(lastStageNumber + 1);
        PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(CLOSED);
        ContainerDelta triggerDelta = createTriggerDeleteDelta();
        modifyObjectViaModel(AccessCertificationCampaignType.class, campaign.getOid(),
                Arrays.asList(stateDelta, stageNumberDelta, triggerDelta), task, result);

        AccessCertificationCampaignType updatedCampaign = refreshCampaign(campaign, task, result);
        LOGGER.info("Updated campaign state: {}", updatedCampaign.getState());
        eventHelper.onCampaignEnd(updatedCampaign, task, result);

        if (campaign.getDefinitionRef() != null) {
            List<ItemDelta> deltas = DeltaBuilder.deltaFor(AccessCertificationDefinitionType.class, prismContext)
                    .item(F_LAST_CAMPAIGN_CLOSED_TIMESTAMP).replace(XmlTypeConverter.createXMLGregorianCalendar(new Date()))
                    .asItemDeltas();
            modifyObjectViaModel(AccessCertificationDefinitionType.class, campaign.getDefinitionRef().getOid(), deltas, task, result);
        }
    }

    <T extends ObjectType> void modifyObjectViaModel(Class<T> objectClass, String oid, Collection<ItemDelta> itemDeltas, Task task, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        ObjectDelta<T> objectDelta = ObjectDelta.createModifyDelta(oid, itemDeltas, objectClass, prismContext);
        try {
            ModelExecuteOptions options = ModelExecuteOptions.createRaw().setPreAuthorized();
            modelService.executeChanges((List) Arrays.asList(objectDelta), options, task, result);
        } catch (SecurityViolationException|ExpressionEvaluationException|CommunicationException|ConfigurationException|PolicyViolationException e) {
            throw new SystemException("Unexpected exception when modifying " + objectClass.getSimpleName() + " " + oid + ": " + e.getMessage(), e);
        }
    }

//    <T extends ObjectType> void modifyObject(Class<T> objectClass, String oid, Collection<ItemDelta> itemDeltas, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
//        repositoryService.modifyObject(objectClass, oid, itemDeltas, result);
//    }

    // TODO implement more efficiently
    public AccessCertificationCampaignType refreshCampaign(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException {
        return repositoryService.getObject(AccessCertificationCampaignType.class, campaign.getOid(), null, result).asObjectable();
    }

    List<ItemDelta> getDeltasForSetStageNumberAndState(int number, AccessCertificationCampaignStateType state) {
        final List<ItemDelta> rv = new ArrayList<>();
        rv.add(createStageNumberDelta(number));
        rv.add(createStateDelta(state));
        return rv;
    }

    private void setState(AccessCertificationCampaignType campaign, AccessCertificationCampaignStateType state, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, SecurityViolationException {
        PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(state);
        modifyObjectViaModel(AccessCertificationCampaignType.class, campaign.getOid(), Arrays.<ItemDelta>asList(stateDelta), task, result);
    }

    private PropertyDelta<Integer> createStageNumberDelta(int number) {
        return PropertyDelta.createReplaceDelta(generalHelper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_STAGE_NUMBER, number);
    }

    private PropertyDelta<AccessCertificationCampaignStateType> createStateDelta(AccessCertificationCampaignStateType state) {
        return PropertyDelta.createReplaceDelta(generalHelper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_STATE, state);
    }

    private PropertyDelta<XMLGregorianCalendar> createStartTimeDelta(XMLGregorianCalendar date) {
        return PropertyDelta.createReplaceDelta(generalHelper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_START, date);
    }

    public List<ItemDelta> getDeltasForStageOpen(AccessCertificationCampaignType campaign, AccessCertificationStageType stage, CertificationHandler handler, final Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        Validate.notNull(campaign, "certificationCampaign");
        Validate.notNull(campaign.getOid(), "certificationCampaign.oid");

        int stageNumber = campaign.getStageNumber();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("getDeltasForStageOpen starting; campaign = {}, stage number = {}",
                    ObjectTypeUtil.toShortString(campaign), stageNumber);
        }

        final List<ItemDelta> rv = new ArrayList<>();
        if (stageNumber == 0) {
            rv.addAll(getDeltasToCreateCases(campaign, stage, handler, task, result));
        } else {
            rv.addAll(getDeltasToUpdateCases(campaign, stage, task, result));
        }

        rv.addAll(getDeltasToRecordStageOpen(campaign, stage, task, result));

        LOGGER.trace("getDeltasForStageOpen finishing, returning {} deltas", rv.size());
        return rv;
    }

    protected AccessCertificationStageType createStage(AccessCertificationCampaignType campaign, int requestedStageNumber) {
        AccessCertificationStageType stage = new AccessCertificationStageType(prismContext);
        stage.setNumber(requestedStageNumber);
        stage.setStart(XmlTypeConverter.createXMLGregorianCalendar(new Date()));

        AccessCertificationStageDefinitionType stageDef = CertCampaignTypeUtil.findStageDefinition(campaign, stage.getNumber());
        XMLGregorianCalendar end = (XMLGregorianCalendar) stage.getStart().clone();
        if (stageDef.getDays() != null) {
            end.add(XmlTypeConverter.createDuration(true, 0, 0, stageDef.getDays(), 0, 0, 0));
        }
        end.setHour(23);
        end.setMinute(59);
        end.setSecond(59);
        end.setMillisecond(999);
        stage.setEnd(end);

        stage.setName(stageDef.getName());
        stage.setDescription(stageDef.getDescription());

        return stage;
    }

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

        newCampaign.setStart(null);
        newCampaign.setEnd(null);
        newCampaign.setState(CREATED);
        newCampaign.setStageNumber(0);

        return newCampaign;
    }

    private PolyStringType generateCampaignName(AccessCertificationDefinitionType definition, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        String prefix = definition.getName().getOrig();
        Integer lastCampaignIdUsed = definition.getLastCampaignIdUsed() != null ? definition.getLastCampaignIdUsed() : 0;
        for (int i = lastCampaignIdUsed+1;; i++) {
            String name = generateName(prefix, i);
            if (!campaignExists(name, task, result)) {
                recordLastCampaignIdUsed(definition.getOid(), i, task, result);
                return new PolyStringType(name);
            }
        }
    }

    private boolean campaignExists(String name, Task task, OperationResult result) throws SchemaException {
        ObjectQuery query = ObjectQueryUtil.createNameQuery(AccessCertificationCampaignType.class, prismContext, name);
        SearchResultList<PrismObject<AccessCertificationCampaignType>> existingCampaigns =
                repositoryService.searchObjects(AccessCertificationCampaignType.class, query, null, result);
        return !existingCampaigns.isEmpty();
    }

    private String generateName(String prefix, int i) {
        return prefix + " " + i;
    }

    private List<ItemDelta> getDeltasToCreateCases(
            final AccessCertificationCampaignType campaign, AccessCertificationStageType stage,
            final CertificationHandler handler, final Task task, final OperationResult result) throws SchemaException, ObjectNotFoundException {

        final List<ItemDelta> rv = new ArrayList<>();

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
        repositoryService.searchObjectsIterative(objectClass, query, (ResultHandler) resultHandler, null, false, result);

        AccessCertificationReviewerSpecificationType reviewerSpec =
                reviewersHelper.findReviewersSpecification(campaign, 1, task, result);

        ContainerDelta<AccessCertificationCaseType> caseDelta = ContainerDelta.createDelta(F_CASE,
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
            //caseCVal.setId((long) (i + 1));
            caseDelta.addValueToAdd(caseCVal);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Adding certification case:\n{}", caseCVal.debugDump());
            }
        }
        rv.add(caseDelta);

        ContainerDelta<AccessCertificationStageType> stageDelta = ContainerDelta.createDelta(AccessCertificationCampaignType.F_STAGE,
                AccessCertificationCampaignType.class, prismContext);
        stageDelta.addValueToAdd(stage.asPrismContainerValue());
        rv.add(stageDelta);

        LOGGER.trace("Prepared {}, deltas to create stage and {} cases for campaign {}", rv.size(), caseList.size(), campaignShortName);
        return rv;
    }

    // workaround for a query interpreter deficiency: we fill-in decisions when in stage 1
    // (in order to be able to find cases that were not responded to by a given reviewer)
    private List<AccessCertificationDecisionType> createEmptyDecisionsForCase(List<ObjectReferenceType> forReviewers, int forStage) {
        //long id = 1;
        List<AccessCertificationDecisionType> decisions = new ArrayList<>();
        for (ObjectReferenceType reviewer : forReviewers) {
            AccessCertificationDecisionType decision = new AccessCertificationDecisionType(prismContext);
            decision.setReviewerRef(reviewer);
            decision.setStageNumber(forStage);
            decision.setResponse(null);
            decision.setTimestamp(null);
//            if (forStage == 1) {
//                decision.setId(id++);
//            }
            decisions.add(decision);
        }
        return decisions;
    }

    private List<ItemDelta> getDeltasToUpdateCases(AccessCertificationCampaignType campaign, AccessCertificationStageType stage, Task task, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        LOGGER.trace("Updating reviewers and timestamps for cases in {}", ObjectTypeUtil.toShortString(campaign));
        final List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaign.getOid(), null, null, result);
        final List<ItemDelta> rv = new ArrayList<>(caseList.size());

        int stageToBe = campaign.getStageNumber() + 1;

        AccessCertificationReviewerSpecificationType reviewerSpec =
                reviewersHelper.findReviewersSpecification(campaign, stageToBe, task, result);
        for (int i = 0; i < caseList.size(); i++) {
            AccessCertificationCaseType _case = caseList.get(i);

            final boolean enabled = computationHelper.computeEnabled(campaign, _case);
            if (enabled) {
                reviewersHelper.setupReviewersForCase(_case, campaign, reviewerSpec, task, result);
            } else {
                _case.getReviewerRef().clear();
            }
            final PrismReference reviewersRef = _case.asPrismContainerValue().findOrCreateReference(AccessCertificationCaseType.F_REVIEWER_REF);
            final ReferenceDelta reviewerDelta = ReferenceDelta.createModificationReplace(
                    new ItemPath(
                            new NameItemPathSegment(F_CASE),
                            new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                            new NameItemPathSegment(AccessCertificationCaseType.F_REVIEWER_REF)),
                    generalHelper.getCampaignObjectDefinition(), CloneUtil.cloneCollectionMembers(reviewersRef.getValues()));
            rv.add(reviewerDelta);

            final List<AccessCertificationDecisionType> newDecisions = createEmptyDecisionsForCase(_case.getReviewerRef(), stageToBe);
            final PrismContainerDefinition decisionDef =
                    prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass(AccessCertificationCaseType.class)
                        .findContainerDefinition(AccessCertificationCaseType.F_DECISION);
            final ContainerDelta decisionDelta = ContainerDelta.createDelta(AccessCertificationCaseType.F_DECISION, decisionDef);
            for (AccessCertificationDecisionType newDecision : newDecisions) {
                decisionDelta.addValueToAdd(new PrismPropertyValue<>(newDecision));
            }

            final PropertyDelta reviewRequestedTimestampDelta = PropertyDelta.createModificationReplaceProperty(
                    new ItemPath(
                            new NameItemPathSegment(F_CASE),
                            new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                            new NameItemPathSegment(AccessCertificationCaseType.F_REVIEW_REQUESTED_TIMESTAMP)),
                    generalHelper.getCampaignObjectDefinition(),
                    enabled ? Arrays.asList(stage.getStart()) : new ArrayList(0));
            rv.add(reviewRequestedTimestampDelta);

            final ItemPath deadlinePath = new ItemPath(
                    new NameItemPathSegment(F_CASE),
                    new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                    new NameItemPathSegment(AccessCertificationCaseType.F_REVIEW_DEADLINE));
            final PropertyDelta deadlineDelta = PropertyDelta.createModificationReplaceProperty(deadlinePath,
                    generalHelper.getCampaignObjectDefinition(),
                    enabled ? Arrays.asList(stage.getEnd()) : new ArrayList(0));
            rv.add(deadlineDelta);

            if (enabled) {
                final PropertyDelta currentResponseDelta = PropertyDelta.createModificationReplaceProperty(
                        new ItemPath(
                                new NameItemPathSegment(F_CASE),
                                new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                                new NameItemPathSegment(F_CURRENT_RESPONSE)),
                        generalHelper.getCampaignObjectDefinition());
                rv.add(currentResponseDelta);

                final PropertyDelta currentResponseStageDelta = PropertyDelta.createModificationReplaceProperty(
                        new ItemPath(
                                new NameItemPathSegment(F_CASE),
                                new IdItemPathSegment(_case.asPrismContainerValue().getId()),
                                new NameItemPathSegment(AccessCertificationCaseType.F_CURRENT_STAGE_NUMBER)),
                        generalHelper.getCampaignObjectDefinition(),
                        stageToBe);
                rv.add(currentResponseStageDelta);
            }
        }

        ContainerDelta<AccessCertificationStageType> stageDelta = ContainerDelta.createDelta(AccessCertificationCampaignType.F_STAGE,
                AccessCertificationCampaignType.class, prismContext);
        stageDelta.addValueToAdd(stage.asPrismContainerValue());
        rv.add(stageDelta);

        LOGGER.debug("Created deltas to create a stage, update reviewers and timestamps in {} cases for campaign {}", caseList.size(), ObjectTypeUtil.toShortString(campaign));
        return rv;
    }

    // some bureaucracy... stage#, state, start time, triggers
    List<ItemDelta> getDeltasToRecordStageOpen(AccessCertificationCampaignType campaign, AccessCertificationStageType newStage, Task task,
                                               OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        final List<ItemDelta> itemDeltaList = new ArrayList<>();

        final PropertyDelta<Integer> stageNumberDelta = createStageNumberDelta(newStage.getNumber());
        itemDeltaList.add(stageNumberDelta);

        final PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(IN_REVIEW_STAGE);
        itemDeltaList.add(stateDelta);

        final boolean campaignJustCreated = newStage.getNumber() == 1;
        if (campaignJustCreated) {
            PropertyDelta<XMLGregorianCalendar> startDelta = createStartTimeDelta(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
            itemDeltaList.add(startDelta);
        }

        final XMLGregorianCalendar end = newStage.getEnd();
        if (end != null) {
            // auto-closing and notifications triggers
            final AccessCertificationStageDefinitionType stageDef =
                    CertCampaignTypeUtil.findStageDefinition(campaign, newStage.getNumber());
            List<TriggerType> triggers = new ArrayList<>();

            // pseudo-random ID so this trigger will not be deleted by trigger task handler (if this code itself is executed as part of previous trigger firing)
            // TODO implement this more seriously!
            long lastId = (long) (Math.random() * 1000000000);

            final TriggerType triggerClose = new TriggerType(prismContext);
            triggerClose.setHandlerUri(AccessCertificationCloseStageTriggerHandler.HANDLER_URI);
            triggerClose.setTimestamp(end);
            triggerClose.setId(lastId);
            triggers.add(triggerClose);

            for (Integer hoursBeforeDeadline : stageDef.getNotifyBeforeDeadline()) {
                final XMLGregorianCalendar beforeEnd = CloneUtil.clone(end);
                beforeEnd.add(XmlTypeConverter.createDuration(false, 0, 0, 0, hoursBeforeDeadline, 0, 0));
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

    List<ItemDelta> getDeltasForStageClose(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        List<ItemDelta> rv = createCurrentResponsesDeltas(campaign, task, result);
        PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(REVIEW_STAGE_DONE);
        rv.add(stateDelta);
        ContainerDelta triggerDelta = createTriggerDeleteDelta();
        rv.add(triggerDelta);
        return rv;
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
        Collection<String> reviewers = eventHelper.getCurrentReviewers(campaign, caseList);
        for (String reviewerOid : reviewers) {
            final List<AccessCertificationCaseType> cases = queryHelper.getCasesForReviewer(campaign, reviewerOid, task, result);
            final ObjectReferenceType reviewerRef = ObjectTypeUtil.createObjectRef(reviewerOid, ObjectTypes.USER);
            eventHelper.onReviewRequested(reviewerRef, cases, campaign, task, result);
        }

        if (newStage.getNumber() == 1 && campaign.getDefinitionRef() != null) {
            List<ItemDelta> deltas = DeltaBuilder.deltaFor(AccessCertificationDefinitionType.class, prismContext)
                    .item(F_LAST_CAMPAIGN_STARTED_TIMESTAMP).replace(XmlTypeConverter.createXMLGregorianCalendar(new Date()))
                    .asItemDeltas();
            modifyObjectViaModel(AccessCertificationDefinitionType.class, campaign.getDefinitionRef().getOid(), deltas, task, result);
        }
    }

    void afterStageClose(String campaignOid, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        final AccessCertificationCampaignType campaign = generalHelper.getCampaign(campaignOid, null, task, result);
        eventHelper.onCampaignStageEnd(campaign, task, result);
    }

    private ContainerDelta createTriggerDeleteDelta() {
        return ContainerDelta.createModificationReplace(ObjectType.F_TRIGGER, generalHelper.getCampaignObjectDefinition());
    }

    private List<ItemDelta> createCurrentResponsesDeltas(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException {
        final List<ItemDelta> rv = new ArrayList<>();

        LOGGER.trace("Updating current response for cases in {}", ObjectTypeUtil.toShortString(campaign));
        final List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaign.getOid(), null, null, result);

        for (int i = 0; i < caseList.size(); i++) {
            final AccessCertificationCaseType _case = caseList.get(i);
            if (_case.getCurrentStageNumber() != campaign.getStageNumber()) {
                continue;
            }
            final AccessCertificationResponseType newResponse = computationHelper.computeResponseForStage(_case, campaign);
            if (newResponse != _case.getCurrentResponse()) {
                if (newResponse == null) {
                    throw new IllegalStateException("Computed currentResponse is null for case id " + _case.asPrismContainerValue().getId());
                }
                final ItemDelta currentResponseDelta = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                        .item(F_CASE, _case.asPrismContainerValue().getId(), F_CURRENT_RESPONSE).replace(newResponse)
                        .asItemDelta();
                rv.add(currentResponseDelta);
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

        modifyObjectViaModel(AccessCertificationCampaignType.class, campaignOid, Arrays.<ItemDelta>asList(reviewRemediedDelta), task, parentResult);
    }

    public void recordLastCampaignIdUsed(String definitionOid, int lastIdUsed, Task task, OperationResult result) {
        try {
            List<ItemDelta> modifications = DeltaBuilder.deltaFor(AccessCertificationDefinitionType.class, prismContext)
                    .item(F_LAST_CAMPAIGN_ID_USED).replace(lastIdUsed)
                    .asItemDeltas();
            modifyObjectViaModel(AccessCertificationDefinitionType.class, definitionOid, modifications, task, result);
        } catch (SchemaException|ObjectNotFoundException|RuntimeException|ObjectAlreadyExistsException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update last campaign ID for definition {}", e, definitionOid);
        }
    }
}
