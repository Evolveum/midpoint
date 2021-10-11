/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.entitlements;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.OidUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processors.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpStartInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Aspect for adding associations.
 *
 * In current version it treats associations that are DIRECTLY added, i.e. not as a part of an assignment.
 *
 * @author mederly
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
    public List<PcpStartInstruction> getStartInstructions(@NotNull ObjectTreeDeltas objectTreeDeltas,
            @NotNull ModelInvocationContext ctx, @NotNull OperationResult parentResult) throws SchemaException, ObjectNotFoundException {

        OperationResult result = parentResult.subresult(OP_GET_START_INSTRUCTIONS)
                .setMinor()
                .build();
        try {
            List<Request> approvalRequestList =
                    getApprovalRequests(ctx.modelContext, configurationHelper.getPcpConfiguration(ctx.wfConfiguration),
                            objectTreeDeltas, ctx.task, result);
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

    private List<Request> getApprovalRequests(ModelContext<?> modelContext, PrimaryChangeProcessorConfigurationType wfConfigurationType,
            ObjectTreeDeltas changes, Task taskFromModel, OperationResult result) {

        List<Request> requests = new ArrayList<>();

        PcpAspectConfigurationType config = primaryChangeAspectHelper.getPcpAspectConfigurationType(wfConfigurationType, this);
        //noinspection unchecked
        Set<Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>>> entries = changes.getProjectionChangeMapEntries();
        for (Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> entry : entries) {
            ObjectDelta<ShadowType> delta = entry.getValue();
            if (delta.isAdd()) {
                requests.addAll(getApprovalRequestsFromShadowAdd(config, entry.getValue(), entry.getKey(), modelContext, taskFromModel, result));
            } else if (delta.isModify()) {
                ModelProjectionContext projectionContext = modelContext.findProjectionContext(entry.getKey());
                requests.addAll(getApprovalRequestsFromShadowModify(
                        config, projectionContext.getObjectOld(), entry.getValue(), entry.getKey(), modelContext, taskFromModel, result));
            } else {
                // no-op
            }
        }
        return requests;
    }

    private List<Request> getApprovalRequestsFromShadowAdd(PcpAspectConfigurationType config, ObjectDelta<ShadowType> change,
                                     ResourceShadowDiscriminator rsd, ModelContext<?> modelContext, Task taskFromModel, OperationResult result) {
        LOGGER.trace("Relevant associations in shadow add delta:");

        List<Request> approvalRequestList = new ArrayList<>();
        ShadowType shadowType = change.getObjectToAdd().asObjectable();
        Iterator<ShadowAssociationType> associationIterator = shadowType.getAssociation().iterator();
        while (associationIterator.hasNext()) {
            ShadowAssociationType a = associationIterator.next();
            AssociationAdditionType itemToApprove = createItemToApprove(a, rsd);
            if (isAssociationRelevant(config, itemToApprove, rsd, modelContext, taskFromModel, result)) {
                approvalRequestList.add(createApprovalRequest(config, itemToApprove, modelContext, taskFromModel, result));
                associationIterator.remove();
                generateProjectionOidIfNeeded(modelContext, shadowType, rsd);
            }
        }
        return approvalRequestList;
    }

    private void generateProjectionOidIfNeeded(ModelContext<?> modelContext, ShadowType shadow, ResourceShadowDiscriminator rsd) {
        if (shadow.getOid() != null) {
            return;
        }
        String newOid = OidUtil.generateOid();
        LOGGER.trace("This is ADD operation with no shadow OID for {} provided. Generated new OID to be used: {}", rsd, newOid);
        shadow.setOid(newOid);
        LensProjectionContext projCtx = ((LensProjectionContext) modelContext.findProjectionContext(rsd));
        if (projCtx == null) {
            throw new IllegalStateException("No projection context for " + rsd + " could be found");
        } else if (projCtx.getOid() != null) {
            throw new IllegalStateException("No projection context for " + rsd + " has already an OID: " + projCtx.getOid());
        }
        projCtx.setOid(newOid);
    }

    private AssociationAdditionType createItemToApprove(ShadowAssociationType a, ResourceShadowDiscriminator rsd) {
        ShadowAssociationType aCopy = cloneAndCanonicalizeAssociation(a);
        AssociationAdditionType aat = new AssociationAdditionType(prismContext);
        aat.setAssociation(aCopy);
        aat.setResourceShadowDiscriminator(rsd.toResourceShadowDiscriminatorType());
        return aat;
    }

    private List<Request> getApprovalRequestsFromShadowModify(PcpAspectConfigurationType config, PrismObject<ShadowType> shadowOld,
                                        ObjectDelta<ShadowType> change, ResourceShadowDiscriminator rsd,
                                        ModelContext<?> modelContext, Task taskFromModel, OperationResult result) {
        LOGGER.trace("Relevant associations in shadow modify delta:");

        List<Request> approvalRequestList = new ArrayList<>();
        Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();

        while (deltaIterator.hasNext()) {
            ItemDelta delta = deltaIterator.next();
            if (!ShadowType.F_ASSOCIATION.equivalent(delta.getPath())) {
                continue;
            }

            if (delta.getValuesToAdd() != null && !delta.getValuesToAdd().isEmpty()) {
                //noinspection unchecked
                Iterator<PrismContainerValue<ShadowAssociationType>> valueIterator = delta.getValuesToAdd().iterator();
                while (valueIterator.hasNext()) {
                    PrismContainerValue<ShadowAssociationType> association = valueIterator.next();
                    Request req = processAssociationToAdd(config, association, rsd, modelContext, taskFromModel, result);
                    if (req != null) {
                        approvalRequestList.add(req);
                        valueIterator.remove();
                    }
                }
            }
            if (delta.getValuesToReplace() != null && !delta.getValuesToReplace().isEmpty()) {
                //noinspection unchecked
                Iterator<PrismContainerValue<ShadowAssociationType>> valueIterator = delta.getValuesToReplace().iterator();
                while (valueIterator.hasNext()) {
                    PrismContainerValue<ShadowAssociationType> association = valueIterator.next();
                    if (existsEquivalentValue(shadowOld, association)) {
                        continue;
                    }
                    Request req = processAssociationToAdd(config, association, rsd, modelContext, taskFromModel, result);
                    if (req != null) {
                        approvalRequestList.add(req);
                        valueIterator.remove();
                    }
                }
            }
            // let's sanitize the delta
            if (delta.getValuesToAdd() != null && delta.getValuesToAdd().isEmpty()) {         // empty set of values to add is an illegal state
                delta.resetValuesToAdd();
            }
            if (delta.getValuesToAdd() == null && delta.getValuesToReplace() == null && delta.getValuesToDelete() == null) {
                deltaIterator.remove();
            }
        }
        return approvalRequestList;
    }

    private boolean existsEquivalentValue(PrismObject<ShadowType> shadowOld, PrismContainerValue<ShadowAssociationType> association) {
        ShadowType shadowType = shadowOld.asObjectable();
        for (ShadowAssociationType existing : shadowType.getAssociation()) {
            if (existing.asPrismContainerValue().equals(association, EquivalenceStrategy.REAL_VALUE)) {        // TODO better check
                return true;
            }
        }
        return false;
    }

    private Request processAssociationToAdd(PcpAspectConfigurationType config, PrismContainerValue<ShadowAssociationType> associationCval,
                            ResourceShadowDiscriminator rsd, ModelContext<?> modelContext, Task taskFromModel, OperationResult result) {
        ShadowAssociationType association = associationCval.asContainerable();
        AssociationAdditionType itemToApprove = createItemToApprove(association, rsd);
        if (isAssociationRelevant(config, itemToApprove, rsd, modelContext, taskFromModel, result)) {
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
            ShadowAssociationType association = associationAddition.getAssociation();
            ShadowType target = getAssociationApprovalTarget(association, result);
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
            ObjectTreeDeltas objectTreeDeltas = associationAdditionToDelta(ctx.modelContext, associationAddition);
            instruction.setDeltasToApprove(objectTreeDeltas);

            instruction.setObjectRef(ctx);     // TODO - or should we take shadow as an object?
            instruction.setTargetRef(ObjectTypeUtil.createObjectRef(target, prismContext), result);

            // set the names of midPoint task and process instance
            String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and execution " : "";
            instruction.setName("Approval " + andExecuting + "of adding " + targetName + " to " + assigneeName);
            //instruction.setProcessInstanceName("Adding " + targetName + " to " + assigneeName);

            instructions.add(instruction);
        }
        return instructions;
    }

    // creates an ObjectTreeDeltas that will be executed after successful approval of the given assignment
    private ObjectTreeDeltas associationAdditionToDelta(ModelContext<?> modelContext, AssociationAdditionType addition)
            throws SchemaException {
        ObjectTreeDeltas changes = new ObjectTreeDeltas(prismContext);
        // TODO reconsider providing default intent here
        ResourceShadowDiscriminator shadowDiscriminator =
                ResourceShadowDiscriminator.fromResourceShadowDiscriminatorType(addition.getResourceShadowDiscriminator(), true);
        String projectionOid = modelContext.findProjectionContext(shadowDiscriminator).getOid();
        ObjectDelta<ShadowType> objectDelta = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_ASSOCIATION).add(addition.getAssociation().clone())
                .asObjectDelta(projectionOid);

        changes.addProjectionChange(shadowDiscriminator, objectDelta);
        return changes;
    }

    //endregion

    private boolean isAssociationRelevant(PcpAspectConfigurationType config, AssociationAdditionType itemToApprove,
            ResourceShadowDiscriminator rsd, ModelContext<?> modelContext, Task task, OperationResult result) {
        LOGGER.trace(" - considering: {}", itemToApprove);
        ExpressionVariables variables = new ExpressionVariables();
        variables.put(ExpressionConstants.VAR_ASSOCIATION, itemToApprove.getAssociation(), ShadowAssociationType.class);
        variables.put(ExpressionConstants.VAR_SHADOW_DISCRIMINATOR, rsd, ResourceShadowDiscriminator.class);
        boolean applicable = primaryChangeAspectHelper.evaluateApplicabilityCondition(
                config, modelContext, itemToApprove, variables, this, task, result);
        LOGGER.trace("   - result: applicable = {}", applicable);
        return applicable;
    }

    private ShadowAssociationType cloneAndCanonicalizeAssociation(ShadowAssociationType a) {
        return a.clone();       // TODO - should we canonicalize?
    }

    // creates an approval requests (e.g. by providing approval schema) for a given assignment and a target
    private Request createApprovalRequest(PcpAspectConfigurationType config, AssociationAdditionType itemToApprove, ModelContext<?> modelContext,
            Task taskFromModel, OperationResult result) {
        Request request = new Request();
        request.addition = itemToApprove;
        request.schema = getSchemaFromConfig(config, prismContext);
        approvalSchemaHelper.prepareSchema(request.schema,
                createRelationResolver((PrismObject<?>) null, result),        // TODO rel resolver
                createReferenceResolver(modelContext, taskFromModel, result));
        return request;
    }

    // retrieves the relevant target for a given assignment - a role, an org, or a resource
    private ShadowType getAssociationApprovalTarget(ShadowAssociationType association, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (association == null) {
            return null;
        }
        ObjectReferenceType shadowRef = association.getShadowRef();
        if (shadowRef == null || shadowRef.getOid() == null) {
            throw new IllegalStateException("None or null-OID shadowRef in " + association);
        }
        PrismObject<ShadowType> shadow = shadowRef.asReferenceValue().getObject();
        if (shadow == null) {
            Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
                    GetOperationOptions.createNoFetch());
            shadow = repositoryService.getObject(ShadowType.class, shadowRef.getOid(), options, result);
            shadowRef.asReferenceValue().setObject(shadow);
        }
        return shadow.asObjectable();
    }


    @NotNull
    private ApprovalSchemaType getSchemaFromConfig(PcpAspectConfigurationType config, @NotNull PrismContext prismContext) {
        if (config == null) {
            return new ApprovalSchemaType();
        } else {
            return getSchema(config.getApprovalSchema(), config.getApproverRef(), config.getApproverExpression(), prismContext);
        }
    }

    @NotNull
    private ApprovalSchemaType getSchema(ApprovalSchemaType schema, List<ObjectReferenceType> approverRef,
            List<ExpressionType> approverExpression, @NotNull PrismContext prismContext) {
        if (schema != null) {
            return schema;
        } else {
            schema = new ApprovalSchemaType(prismContext);
            ApprovalStageDefinitionType stageDef = new ApprovalStageDefinitionType(prismContext);
            stageDef.getApproverRef().addAll(CloneUtil.cloneCollectionMembers(approverRef));
            stageDef.getApproverExpression().addAll(approverExpression);
            schema.getStage().add(stageDef);
            return schema;
        }
    }

}
