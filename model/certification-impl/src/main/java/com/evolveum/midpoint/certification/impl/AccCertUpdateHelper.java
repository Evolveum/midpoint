/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import static java.util.Collections.singleton;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.security.api.OtherPrivilegesLimitations;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTriggeringUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Common and lower-level update methods.
 */
@Component
public class AccCertUpdateHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AccCertUpdateHelper.class);

    @Autowired private AccCertEventHelper eventHelper;
    @Autowired private PrismContext prismContext;
    @Autowired private ModelService modelService;
    @Autowired private ModelInteractionService modelInteractionService;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
    @Autowired private AccCertGeneralHelper generalHelper;
    @Autowired protected AccCertQueryHelper queryHelper;

    //region ================================ Triggers ================================

    // see also MidpointUtil.createTriggersForTimedActions (in workflow-impl)
    @NotNull
    public List<ItemDelta<?, ?>> getDeltasToCreateTriggersForTimedActions(String campaignOid, int escalationLevel,
            Date workItemCreateTime,
            Date workItemDeadline, List<WorkItemTimedActionsType> timedActionsList) {
        LOGGER.trace("Creating triggers for timed actions for certification campaign {}, escalation level {}, create time {}, deadline {}, {} timed action(s)",
                campaignOid, escalationLevel, workItemCreateTime, workItemDeadline, timedActionsList.size());
        try {
            List<TriggerType> triggers = CaseTriggeringUtil.createTriggers(escalationLevel, workItemCreateTime, workItemDeadline,
                    timedActionsList, prismContext, LOGGER, null, AccCertTimedActionTriggerHandler.HANDLER_URI);
            LOGGER.trace("Created {} triggers for campaign {}:\n{}", triggers.size(), campaignOid, PrismUtil.serializeQuietlyLazily(prismContext, triggers));
            if (triggers.isEmpty()) {
                return Collections.emptyList();
            } else {
                return prismContext.deltaFor(AccessCertificationCampaignType.class)
                        .item(TaskType.F_TRIGGER).add(PrismContainerValue.toPcvList(triggers))
                        .asItemDeltas();
            }
        } catch (SchemaException | RuntimeException e) {
            throw new SystemException("Couldn't create deltas for creating trigger(s) for campaign " + campaignOid + ": " + e.getMessage(), e);
        }
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

    public PropertyDelta<Integer> createStageNumberDelta(int number) {
        return prismContext.deltaFactory().property().createReplaceDelta(generalHelper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_STAGE_NUMBER, number);
    }

    public PropertyDelta<AccessCertificationCampaignStateType> createStateDelta(AccessCertificationCampaignStateType state) {
        return prismContext.deltaFactory().property().createReplaceDelta(generalHelper.getCampaignObjectDefinition(), AccessCertificationCampaignType.F_STATE, state);
    }

    public ItemDelta<?, ?> createStartTimeDelta(XMLGregorianCalendar date) throws SchemaException {
        return prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(AccessCertificationCampaignType.F_START_TIMESTAMP).replace(date)
                .asItemDelta();
    }

    public ItemDelta<?, ?> createEndTimeDelta(XMLGregorianCalendar date) throws SchemaException {
        return prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(AccessCertificationCampaignType.F_END_TIMESTAMP).replace(date)
                .asItemDelta();
    }

    public ContainerDelta<?> createTriggerDeleteDelta() {
        return prismContext.deltaFactory().container()
                .createModificationReplace(ObjectType.F_TRIGGER, generalHelper.getCampaignObjectDefinition());
    }

    List<ItemDelta<?, ?>> createTriggerReplaceDelta(Collection<TriggerType> triggers) throws SchemaException {
        return prismContext.deltaFor(AccessCertificationCampaignType.class)
                .item(AccessCertificationCampaignType.F_TRIGGER).replaceRealValues(triggers)
                .asItemDeltas();
    }


    //endregion

    //region ================================ Model and repository operations ================================

    void addObjectPreAuthorized(ObjectType objectType, Task task, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        ObjectDelta<? extends ObjectType> objectDelta = DeltaFactory.Object.createAddDelta(objectType.asPrismObject());
        Collection<ObjectDeltaOperation<? extends ObjectType>> ops;
        try {
            ops = modelService.executeChanges(
                    singleton(objectDelta),
                    ModelExecuteOptions.create().raw().preAuthorized(), task, result);
        } catch (ExpressionEvaluationException|CommunicationException|ConfigurationException|PolicyViolationException|SecurityViolationException e) {
            throw new SystemException("Unexpected exception when adding object: " + e.getMessage(), e);
        }
        ObjectDeltaOperation<?> odo = ops.iterator().next();
        objectType.setOid(odo.getObjectDelta().getOid());

        /* ALTERNATIVELY, we can go directly into the repository. (No audit there.)
        String oid = repositoryService.addObject(objectType.asPrismObject(), null, result);
        objectType.setOid(oid);
         */
    }

    public void modifyCampaignPreAuthorized(String campaignOid, ModificationsToExecute modifications, Task task, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        for (List<ItemDelta<?, ?>> batch : modifications.batches) {
            if (!batch.isEmpty()) {
                LOGGER.trace("Applying {} changes to campaign {}", batch.size(), campaignOid);
                modifyObjectPreAuthorized(AccessCertificationCampaignType.class, campaignOid, batch, task, result);
            }
        }
    }

    public <T extends ObjectType> void modifyObjectPreAuthorized(Class<T> objectClass, String oid, Collection<ItemDelta<?, ?>> itemDeltas, Task task, OperationResult result) throws ObjectAlreadyExistsException, SchemaException, ObjectNotFoundException {
        ObjectDelta<T> objectDelta = prismContext.deltaFactory().object().createModifyDelta(oid, itemDeltas, objectClass
        );
        try {
            ModelExecuteOptions options = ModelExecuteOptions.create().raw().preAuthorized();
            modelService.executeChanges(Collections.singletonList(objectDelta), options, task, result);
        } catch (SecurityViolationException|ExpressionEvaluationException|CommunicationException|ConfigurationException|PolicyViolationException e) {
            throw new SystemException("Unexpected exception when modifying " + objectClass.getSimpleName() + " " + oid + ": " + e.getMessage(), e);
        }
    }

    // TODO implement more efficiently
    AccessCertificationCampaignType refreshCampaign(AccessCertificationCampaignType campaign,
            OperationResult result) throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(AccessCertificationCampaignType.class, campaign.getOid(), null, result).asObjectable();
    }
    //endregion

    public void notifyReviewers(AccessCertificationCampaignType campaign, boolean unansweredOnly, Task task, OperationResult result) throws SchemaException {
        final Map<String, List<AccessCertificationCaseType>> reviewersCases =
                this.queryHelper.getOpenedCasesMappedToReviewers(campaign.getOid(), unansweredOnly, result);

        for (Map.Entry<String, List<AccessCertificationCaseType>> entry : reviewersCases.entrySet()) {
            final String reviewerOid = entry.getKey();
            final List<AccessCertificationCaseType> reviewerCases = entry.getValue();
            final ObjectReferenceType actualReviewerRef = ObjectTypeUtil.createObjectRef(reviewerOid,
                    ObjectTypes.USER);
            for (ObjectReferenceType reviewerOrDeputyRef : getReviewerAndDeputies(actualReviewerRef, task, result)) {
                this.eventHelper.onReviewRequested(reviewerOrDeputyRef, actualReviewerRef, reviewerCases, campaign,
                        task, result);
            }
        }
    }

    @NotNull
    List<ObjectReferenceType> getReviewerAndDeputies(
            ObjectReferenceType actualReviewerRef, Task task, OperationResult result) throws SchemaException {
        List<ObjectReferenceType> reviewerOrDeputiesRef = new ArrayList<>();
        reviewerOrDeputiesRef.add(actualReviewerRef);
        reviewerOrDeputiesRef.addAll(
                modelInteractionService.getDeputyAssignees(
                        actualReviewerRef, OtherPrivilegesLimitations.Type.ACCESS_CERTIFICATION, task, result));
        return reviewerOrDeputiesRef;
    }


}
