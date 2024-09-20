/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.entitlements;

import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ModificationType;
import com.evolveum.midpoint.schema.util.*;

import com.evolveum.midpoint.schema.util.ShadowAssociationsCollection.IterableAssociationValue;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ObjectTreeDeltas;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpStartInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;

/**
 * Aspect for adding associations.
 *
 * In current version it treats associations that are DIRECTLY added, i.e. not as a part of an assignment.
 */
@Component
public class AddAssociationAspect extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(AddAssociationAspect.class);

    private static final String OP_GET_START_INSTRUCTIONS = AddAssociationAspect.class.getName() + ".getStartInstructions";

    //region ------------------------------------------------------------ Things that execute on request arrival

    private static class Request {
        ApprovalSchemaType schema;
        AssociationAdditionType addition;
    }

    @NotNull
    @Override
    public <T extends ObjectType> List<PcpStartInstruction> getStartInstructions(
            @NotNull ObjectTreeDeltas<T> objectTreeDeltas,
            @NotNull ModelInvocationContext<T> ctx,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException {

        OperationResult result = parentResult.subresult(OP_GET_START_INSTRUCTIONS)
                .setMinor()
                .build();
        try {
            List<Request> approvalRequestList =
                    getApprovalRequests(
                            ctx.modelContext,
                            configurationHelper.getPcpConfiguration(ctx.wfConfiguration),
                            objectTreeDeltas,
                            ctx.task,
                            result);
            if (!approvalRequestList.isEmpty()) {
                return prepareJobCreateInstructions(ctx, result, approvalRequestList);
            } else {
                return Collections.emptyList();
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private List<Request> getApprovalRequests(
            LensContext<?> modelContext, PrimaryChangeProcessorConfigurationType wfConfiguration,
            ObjectTreeDeltas<?> changes, Task taskFromModel, OperationResult result) throws SchemaException {

        List<Request> requests = new ArrayList<>();

        PcpAspectConfigurationType config = primaryChangeAspectHelper.getPcpAspectConfigurationType(wfConfiguration, this);
        Set<Map.Entry<ProjectionContextKey, ObjectDelta<ShadowType>>> entries = changes.getProjectionChangeMapEntries();
        for (Map.Entry<ProjectionContextKey, ObjectDelta<ShadowType>> entry : entries) {
            ObjectDelta<ShadowType> delta = entry.getValue();
            if (delta.isAdd()) {
                requests.addAll(
                        getApprovalRequestsFromShadowAdd(
                                config, entry.getValue(), entry.getKey(), modelContext, taskFromModel, result));
            } else if (delta.isModify()) {
                ModelProjectionContext projectionContext = modelContext.findProjectionContextByKeyExact(entry.getKey());
                requests.addAll(
                        getApprovalRequestsFromShadowModify(
                                config, projectionContext.getObjectOld(), entry.getValue(), entry.getKey(), modelContext,
                                taskFromModel, result));
            } else {
                // no-op
            }
        }
        return requests;
    }

    private List<Request> getApprovalRequestsFromShadowAdd(
            PcpAspectConfigurationType config,
            ObjectDelta<ShadowType> change,
            ProjectionContextKey projectionContextKey,
            LensContext<?> modelContext,
            Task taskFromModel,
            OperationResult result) {
        LOGGER.trace("Relevant associations in shadow add delta:");

        List<Request> approvalRequestList = new ArrayList<>();
        ShadowType shadow = change.getObjectToAdd().asObjectable();
        ShadowAssociationsType associationsBean = shadow.getAssociations();
        if (associationsBean != null) {
            //noinspection unchecked
            var associationsIterator =
                    ((PrismContainerValue<ShadowAssociationsType>) associationsBean.asPrismContainerValue()).getItems().iterator();
            while (associationsIterator.hasNext()) {
                //noinspection unchecked
                var association = (PrismContainer<ShadowAssociationValueType>) associationsIterator.next();
                for (var associationPcv : List.copyOf(association.getValues())) {
                    AssociationAdditionType itemToApprove =
                            createItemToApprove(association.getElementName(), associationPcv.getValue(), projectionContextKey);
                    if (isAssociationRelevant(config, itemToApprove, projectionContextKey, modelContext, taskFromModel, result)) {
                        approvalRequestList.add(createApprovalRequest(config, itemToApprove, modelContext, taskFromModel, result));
                        generateProjectionOidIfNeeded(modelContext, shadow, projectionContextKey);
                        association.remove(associationPcv);
                    }
                }
                if (association.hasNoValues()) {
                    associationsIterator.remove();
                }
            }
        }
        return approvalRequestList;
    }

    private void generateProjectionOidIfNeeded(ModelContext<?> modelContext, ShadowType shadow, ProjectionContextKey key) {
        if (shadow.getOid() != null) {
            return;
        }
        String newOid = OidUtil.generateOid();
        LOGGER.trace("This is ADD operation with no shadow OID for {} provided. Generated new OID to be used: {}", key, newOid);
        shadow.setOid(newOid);
        LensProjectionContext projCtx = ((LensProjectionContext) modelContext.findProjectionContextByKeyExact(key));
        if (projCtx == null) {
            throw new IllegalStateException("No projection context for " + key + " could be found");
        } else if (projCtx.getOid() != null) {
            throw new IllegalStateException("No projection context for " + key + " has already an OID: " + projCtx.getOid());
        }
        projCtx.setOid(newOid);
    }

    private @NotNull AssociationAdditionType createItemToApprove(
            QName assocName, ShadowAssociationValueType value, ProjectionContextKey rsd) {
        AssociationAdditionType aat = new AssociationAdditionType();
        aat.setName(assocName);
        aat.setValue(value.clone());
        aat.setResourceShadowDiscriminator(rsd.toResourceShadowDiscriminatorType());
        return aat;
    }

    private List<Request> getApprovalRequestsFromShadowModify(
            PcpAspectConfigurationType config,
            PrismObject<ShadowType> shadowOld,
            ObjectDelta<ShadowType> change,
            ProjectionContextKey key,
            LensContext<?> modelContext,
            Task taskFromModel,
            OperationResult result) throws SchemaException {
        LOGGER.trace("Relevant associations in shadow modify delta:");

        List<Request> approvalRequestList = new ArrayList<>();
        Iterator<? extends ItemDelta<?, ?>> deltaIterator = change.getModifications().iterator();

        // FIXME what about adding/deleting/replacing the whole container?

        while (deltaIterator.hasNext()) {
            ItemDelta<?, ?> delta = deltaIterator.next();
            var valueIterator = ShadowAssociationsCollection.ofDelta(delta).iterator();
            while (valueIterator.hasNext()) {
                var iterableAssociationValue = valueIterator.next();
                var modType = iterableAssociationValue.modificationType();
                if (modType == ModificationType.DELETE) {
                    continue;
                }
                if (modType == ModificationType.REPLACE) {
                    if (existsEquivalentValue(shadowOld, iterableAssociationValue)) {
                        continue; // not really being added
                    }
                }
                Request req = processAssociationToAdd(
                        config, iterableAssociationValue, key, modelContext, taskFromModel, result);
                if (req != null) {
                    approvalRequestList.add(req);
                    valueIterator.remove();
                }
            }
            if (delta.isEmpty()) {
                deltaIterator.remove();
            }
        }
        return approvalRequestList;
    }

    private boolean existsEquivalentValue(PrismObject<ShadowType> shadowOld, IterableAssociationValue iterableAssociationValue) {
        for (var existing : ShadowUtil.getAssociationValuesRaw(shadowOld, iterableAssociationValue.name())) {
            if (existing.equals(iterableAssociationValue.value())) { // TODO better check
                return true;
            }
        }
        return false;
    }

    private Request processAssociationToAdd(
            PcpAspectConfigurationType config,
            IterableAssociationValue iterableAssociationValue,
            ProjectionContextKey key,
            LensContext<?> modelContext,
            Task taskFromModel,
            OperationResult result) {
        var associationValue = iterableAssociationValue.value();
        AssociationAdditionType itemToApprove = createItemToApprove(iterableAssociationValue.name(), associationValue, key);
        if (isAssociationRelevant(config, itemToApprove, key, modelContext, taskFromModel, result)) {
            return createApprovalRequest(config, itemToApprove, modelContext, taskFromModel, result);
        } else {
            return null;
        }
    }

    private List<PcpStartInstruction> prepareJobCreateInstructions(ModelInvocationContext<?> ctx, OperationResult result,
            List<Request> approvalRequestList)
            throws SchemaException, ObjectNotFoundException {

        List<PcpStartInstruction> instructions = new ArrayList<>();
        String assigneeName = ctx.getFocusObjectName();
        PrismObject<? extends FocusType> requester = ctx.getRequestor(result);

        for (Request approvalRequest : approvalRequestList) {

            LOGGER.trace("Approval request = {}", approvalRequest);
            AssociationAdditionType associationAddition = approvalRequest.addition;
            ShadowType target = getAssociationApprovalTarget(associationAddition.getValue(), result);
            Validate.notNull(target, "No target in association to be approved");

            String targetName = target.getName() != null ? target.getName().getOrig() : "(unnamed)";

            // create a JobCreateInstruction for a given change processor (primaryChangeProcessor in this case)
            PcpStartInstruction instruction =
                    PcpStartInstruction.createItemApprovalInstruction(
                            getChangeProcessor(),
                            approvalRequest.schema, null);

            // set some common task/process attributes
            instruction.prepareCommonAttributes(this, ctx.modelContext, requester);

            // prepare and set the delta that has to be approved
            ObjectTreeDeltas<?> objectTreeDeltas = associationAdditionToDelta(ctx.modelContext, associationAddition);
            instruction.setDeltasToApprove(objectTreeDeltas);

            instruction.setObjectRef(ctx);     // TODO - or should we take shadow as an object?
            instruction.setTargetRef(ObjectTypeUtil.createObjectRef(target), result);

            // set the names of midPoint task and process instance
            String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and execution " : "";
            instruction.setName("Approval " + andExecuting + "of adding " + targetName + " to " + assigneeName);
            //instruction.setProcessInstanceName("Adding " + targetName + " to " + assigneeName);

            instructions.add(instruction);
        }
        return instructions;
    }

    // creates an ObjectTreeDeltas that will be executed after successful approval of the given assignment
    private ObjectTreeDeltas<?> associationAdditionToDelta(ModelContext<?> modelContext, AssociationAdditionType addition)
            throws SchemaException {
        ObjectTreeDeltas<?> changes = new ObjectTreeDeltas<>();
        ProjectionContextKey projectionContextKey =
                ProjectionContextKey.fromBean(addition.getResourceShadowDiscriminator());
        String projectionOid = modelContext.findProjectionContextByKeyExact(projectionContextKey).getOid();
        ObjectDelta<ShadowType> objectDelta = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_ASSOCIATIONS, addition.getName()).add(addition.getValue().clone())
                .asObjectDelta(projectionOid);

        changes.addProjectionChange(projectionContextKey, objectDelta);
        return changes;
    }

    //endregion

    private boolean isAssociationRelevant(
            PcpAspectConfigurationType config,
            AssociationAdditionType itemToApprove,
            ProjectionContextKey projectionContextKey,
            ModelContext<?> modelContext,
            Task task,
            OperationResult result) {
        LOGGER.trace(" - considering: {}", itemToApprove);
        VariablesMap variables = new VariablesMap();
        var association = itemToApprove.getValue().clone();
        variables.put(ExpressionConstants.VAR_ASSOCIATION, association, ShadowAssociationValueType.class);
        variables.put(ExpressionConstants.VAR_SHADOW_DISCRIMINATOR, projectionContextKey, ProjectionContextKey.class);
        boolean applicable = primaryChangeAspectHelper.evaluateApplicabilityCondition(
                config, modelContext, itemToApprove, variables, this, task, result);
        LOGGER.trace("   - result: applicable = {}", applicable);
        return applicable;
    }

    // creates an approval requests (e.g. by providing approval schema) for a given assignment and a target
    private Request createApprovalRequest(
            PcpAspectConfigurationType config, AssociationAdditionType itemToApprove, LensContext<?> modelContext,
            Task taskFromModel, OperationResult result) {
        Request request = new Request();
        request.addition = itemToApprove;
        request.schema = getSchemaFromConfig(config);
        approvalSchemaHelper.prepareSchema(request.schema,
                createRelationResolver(null, result),
                createReferenceResolver(modelContext, taskFromModel, result));
        return request;
    }

    // retrieves the relevant target for a given assignment - a role, an org, or a resource
    private ShadowType getAssociationApprovalTarget(ShadowAssociationValueType association, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        if (association == null) {
            return null;
        }
        var shadowRef = ShadowAssociationsUtil.getSingleObjectRefRelaxed(association);
        if (shadowRef == null || shadowRef.getOid() == null) {
            throw new IllegalStateException("None or null-OID shadowRef in " + association);
        }
        PrismObject<ShadowType> shadow = shadowRef.getObject();
        if (shadow == null) {
            // FIXME here should be provisioning call, right?
            shadow = repositoryService.getObject(ShadowType.class, shadowRef.getOid(), createNoFetchCollection(), result);
            shadowRef.asReferenceValue().setObject(shadow);
        }
        return shadow.asObjectable();
    }


    @NotNull
    private ApprovalSchemaType getSchemaFromConfig(PcpAspectConfigurationType config) {
        if (config == null) {
            return new ApprovalSchemaType();
        } else {
            return getSchema(config.getApprovalSchema(), config.getApproverRef(), config.getApproverExpression());
        }
    }

    @NotNull
    private ApprovalSchemaType getSchema(
            ApprovalSchemaType schema,
            List<ObjectReferenceType> approverRef,
            List<ExpressionType> approverExpression) {
        if (schema != null) {
            return schema;
        }
        var newSchema = new ApprovalSchemaType();
        ApprovalStageDefinitionType stageDef = new ApprovalStageDefinitionType();
        stageDef.getApproverRef().addAll(CloneUtil.cloneCollectionMembers(approverRef));
        stageDef.getApproverExpression().addAll(approverExpression);
        newSchema.getStage().add(stageDef);
        return newSchema;
    }
}
