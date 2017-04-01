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
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.*;
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

    @Autowired
    protected AccCertCaseOperationsHelper caseHelper;

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

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("getDeltasForStageOpen starting; campaign = {}, stage number = {}",
                    ObjectTypeUtil.toShortString(campaign), stageNumber);
        }

        final List<ItemDelta<?,?>> rv = new ArrayList<>();
        if (stageNumber == 0) {
            rv.addAll(caseHelper.getDeltasToCreateCases(campaign, stage, handler, task, result));
        } else {
            rv.addAll(caseHelper.getDeltasToAdvanceCases(campaign, stage, task, result));
        }

        rv.add(createStageAddDelta(stage));
        rv.addAll(createDeltasToRecordStageOpen(campaign, stage, task, result));

		LOGGER.trace("getDeltasForStageOpen finishing, returning {} deltas:\n{}", rv.size(), DebugUtil.debugDumpLazily(rv));
        return rv;
    }

    // some bureaucracy... stage#, state, start time, triggers
    List<ItemDelta<?,?>> createDeltasToRecordStageOpen(AccessCertificationCampaignType campaign, AccessCertificationStageType newStage, Task task,
                                                  OperationResult result) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

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
        XMLGregorianCalendar deadline = (XMLGregorianCalendar) stage.getStart().clone();
        if (stageDef.getDuration() != null) {
            deadline.add(stageDef.getDuration());
        }
		DeadlineRoundingType rounding = stageDef.getDeadlineRounding() != null ?
				stageDef.getDeadlineRounding() : DeadlineRoundingType.DAY;
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
        stage.setDeadline(deadline);

        stage.setName(stageDef.getName());
        stage.setDescription(stageDef.getDescription());

        return stage;
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

    //region ================================ Campaign and stage close ================================

    void closeCampaign(AccessCertificationCampaignType campaign, Task task, OperationResult result) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, SecurityViolationException {
        LOGGER.info("Closing campaign {}", ObjectTypeUtil.toShortString(campaign));
        int lastStageNumber = CertCampaignTypeUtil.getNumberOfStages(campaign);
        // TODO issue a warning if we are not in a correct state
        PropertyDelta<Integer> stageNumberDelta = createStageNumberDelta(lastStageNumber + 1);
        PropertyDelta<AccessCertificationCampaignStateType> stateDelta = createStateDelta(CLOSED);
        ContainerDelta triggerDelta = createTriggerDeleteDelta();
        PropertyDelta<XMLGregorianCalendar> endDelta = createEndTimeDelta(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        modifyObjectViaModel(AccessCertificationCampaignType.class, campaign.getOid(),
                Arrays.<ItemDelta<?,?>>asList(stateDelta, stageNumberDelta, triggerDelta, endDelta), task, result);

        AccessCertificationCampaignType updatedCampaign = refreshCampaign(campaign, task, result);
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
        ItemDelta delta = DeltaBuilder.deltaFor(AccessCertificationCampaignType.class, prismContext)
                .item(AccessCertificationCampaignType.F_STAGE, stageId, AccessCertificationStageType.F_END).replace(currentTime)
                .asItemDelta();
        return delta;
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
        return PropertyDelta.createReplaceDelta(generalHelper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_START, date);
    }

    private PropertyDelta<XMLGregorianCalendar> createEndTimeDelta(XMLGregorianCalendar date) {
        return PropertyDelta.createReplaceDelta(generalHelper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_END, date);
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
        ObjectDelta objectDelta = ObjectDelta.createAddDelta(objectType.asPrismObject());
        Collection<ObjectDeltaOperation<?>> ops;
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

    <T extends ObjectType> void modifyObjectViaModel(Class<T> objectClass, String oid, Collection<ItemDelta<?,?>> itemDeltas, Task task, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
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

    //endregion
}
