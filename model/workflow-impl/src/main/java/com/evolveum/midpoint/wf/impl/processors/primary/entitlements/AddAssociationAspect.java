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

package com.evolveum.midpoint.wf.impl.processors.primary.entitlements;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.schema.ObjectTreeDeltas;
import com.evolveum.midpoint.wf.impl.processors.primary.ModelInvocationContext;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildWfTaskCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DateFormat;
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

	//region ------------------------------------------------------------ Things that execute on request arrival

    @NotNull
	@Override
    public List<PcpChildWfTaskCreationInstruction> prepareTasks(@NotNull ObjectTreeDeltas objectTreeDeltas,
			ModelInvocationContext ctx, @NotNull OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (!isFocusRelevant(ctx.modelContext)) {
            return Collections.emptyList();
        }
        List<ApprovalRequest<AssociationAdditionType>> approvalRequestList =
                getApprovalRequests(ctx.modelContext, baseConfigurationHelper.getPcpConfiguration(ctx.wfConfiguration),
                        objectTreeDeltas, ctx.taskFromModel, result);
        if (approvalRequestList == null || approvalRequestList.isEmpty()) {
            return Collections.emptyList();
        }
        return prepareJobCreateInstructions(ctx.modelContext, ctx.taskFromModel, result, approvalRequestList);
    }

    protected boolean isFocusRelevant(ModelContext modelContext) {
        //return modelContext.getFocusClass() != null && UserType.class.isAssignableFrom(modelContext.getFocusClass());
        return true;
    }

    private List<ApprovalRequest<AssociationAdditionType>> getApprovalRequests(ModelContext<?> modelContext, PrimaryChangeProcessorConfigurationType wfConfigurationType,
                                                                               ObjectTreeDeltas changes, Task taskFromModel, OperationResult result) {

        List<ApprovalRequest<AssociationAdditionType>> requests = new ArrayList<>();

        PcpAspectConfigurationType config = primaryChangeAspectHelper.getPcpAspectConfigurationType(wfConfigurationType, this);
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

    private List<ApprovalRequest<AssociationAdditionType>>
    getApprovalRequestsFromShadowAdd(PcpAspectConfigurationType config, ObjectDelta<ShadowType> change,
                                     ResourceShadowDiscriminator rsd, ModelContext<?> modelContext, Task taskFromModel, OperationResult result) {
        LOGGER.trace("Relevant associations in shadow add delta:");

        List<ApprovalRequest<AssociationAdditionType>> approvalRequestList = new ArrayList<>();
        ShadowType shadowType = change.getObjectToAdd().asObjectable();
        Iterator<ShadowAssociationType> associationIterator = shadowType.getAssociation().iterator();
        while (associationIterator.hasNext()) {
            ShadowAssociationType a = associationIterator.next();
            AssociationAdditionType itemToApprove = createItemToApprove(a, rsd);
            if (isAssociationRelevant(config, itemToApprove, rsd, modelContext, taskFromModel, result)) {
                approvalRequestList.add(createApprovalRequest(config, itemToApprove, modelContext, taskFromModel, result));
                associationIterator.remove();
                miscDataUtil.generateProjectionOidIfNeeded(modelContext, shadowType, rsd);
            }
        }
        return approvalRequestList;
    }

    private AssociationAdditionType createItemToApprove(ShadowAssociationType a, ResourceShadowDiscriminator rsd) {
        ShadowAssociationType aCopy = cloneAndCanonicalizeAssociation(a);
        AssociationAdditionType aat = new AssociationAdditionType(prismContext);
        aat.setAssociation(aCopy);
        aat.setResourceShadowDiscriminator(rsd.toResourceShadowDiscriminatorType());
        return aat;
    }

    private List<ApprovalRequest<AssociationAdditionType>>
    getApprovalRequestsFromShadowModify(PcpAspectConfigurationType config, PrismObject<ShadowType> shadowOld,
                                        ObjectDelta<ShadowType> change, ResourceShadowDiscriminator rsd,
                                        ModelContext<?> modelContext, Task taskFromModel, OperationResult result) {
        LOGGER.trace("Relevant associations in shadow modify delta:");

        List<ApprovalRequest<AssociationAdditionType>> approvalRequestList = new ArrayList<>();
        Iterator<? extends ItemDelta> deltaIterator = change.getModifications().iterator();

        final ItemPath ASSOCIATION_PATH = new ItemPath(ShadowType.F_ASSOCIATION);

        while (deltaIterator.hasNext()) {
            ItemDelta delta = deltaIterator.next();
            if (!ASSOCIATION_PATH.equivalent(delta.getPath())) {
                continue;
            }

            if (delta.getValuesToAdd() != null && !delta.getValuesToAdd().isEmpty()) {
                Iterator<PrismContainerValue<ShadowAssociationType>> valueIterator = delta.getValuesToAdd().iterator();
                while (valueIterator.hasNext()) {
                    PrismContainerValue<ShadowAssociationType> association = valueIterator.next();
                    ApprovalRequest<AssociationAdditionType> req =
                            processAssociationToAdd(config, association, rsd, modelContext, taskFromModel, result);
                    if (req != null) {
                        approvalRequestList.add(req);
                        valueIterator.remove();
                    }
                }
            }
            if (delta.getValuesToReplace() != null && !delta.getValuesToReplace().isEmpty()) {
                Iterator<PrismContainerValue<ShadowAssociationType>> valueIterator = delta.getValuesToReplace().iterator();
                while (valueIterator.hasNext()) {
                    PrismContainerValue<ShadowAssociationType> association = valueIterator.next();
                    if (existsEquivalentValue(shadowOld, association)) {
                        continue;
                    }
                    ApprovalRequest<AssociationAdditionType> req =
                            processAssociationToAdd(config, association, rsd, modelContext, taskFromModel, result);
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
            if (existing.asPrismContainerValue().equalsRealValue(association)) {        // TODO better check
                return true;
            }
        }
        return false;
    }

    private ApprovalRequest<AssociationAdditionType>
    processAssociationToAdd(PcpAspectConfigurationType config, PrismContainerValue<ShadowAssociationType> associationCval,
                            ResourceShadowDiscriminator rsd, ModelContext<?> modelContext, Task taskFromModel, OperationResult result) {
        ShadowAssociationType association = associationCval.asContainerable();
        AssociationAdditionType itemToApprove = createItemToApprove(association, rsd);
        if (isAssociationRelevant(config, itemToApprove, rsd, modelContext, taskFromModel, result)) {
            return createApprovalRequest(config, itemToApprove, modelContext, taskFromModel, result);
        } else {
            return null;
        }
    }

    private List<PcpChildWfTaskCreationInstruction>
    prepareJobCreateInstructions(ModelContext<?> modelContext, Task taskFromModel,
                                 OperationResult result, List<ApprovalRequest<AssociationAdditionType>> approvalRequestList)
            throws SchemaException, ObjectNotFoundException {

        List<PcpChildWfTaskCreationInstruction> instructions = new ArrayList<>();
        String assigneeName = MiscDataUtil.getFocusObjectName(modelContext);
        String assigneeOid = MiscDataUtil.getFocusObjectOid(modelContext);
        PrismObject<UserType> requester = baseModelInvocationProcessingHelper.getRequester(taskFromModel, result);

        for (ApprovalRequest<AssociationAdditionType> approvalRequest : approvalRequestList) {

            LOGGER.trace("Approval request = {}", approvalRequest);
            AssociationAdditionType associationAddition = approvalRequest.getItemToApprove();
            ShadowAssociationType association = associationAddition.getAssociation();
            ShadowType target = getAssociationApprovalTarget(association, result);
            Validate.notNull(target, "No target in association to be approved");

            String targetName = target.getName() != null ? target.getName().getOrig() : "(unnamed)";

            String approvalTaskName = "Approve adding " + targetName + " to " + assigneeName;

            // create a JobCreateInstruction for a given change processor (primaryChangeProcessor in this case)
            PcpChildWfTaskCreationInstruction instruction =
                    PcpChildWfTaskCreationInstruction.createItemApprovalInstruction(
                    		getChangeProcessor(), approvalTaskName,
							approvalRequest.getApprovalSchemaType(), null);

            // set some common task/process attributes
            instruction.prepareCommonAttributes(this, modelContext, requester);

            // prepare and set the delta that has to be approved
            ObjectTreeDeltas objectTreeDeltas = associationAdditionToDelta(modelContext, associationAddition, assigneeOid);
            instruction.setDeltasToProcesses(objectTreeDeltas);

            instruction.setObjectRef(modelContext, result);     // TODO - or should we take shadow as an object?
            instruction.setTargetRef(ObjectTypeUtil.createObjectRef(target), result);

            // set the names of midPoint task and activiti process instance
            String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and execution " : "";
            instruction.setTaskName("Approval " + andExecuting + "of adding " + targetName + " to " + assigneeName);
            instruction.setProcessInstanceName("Adding " + targetName + " to " + assigneeName);

            // setup general item approval process
            itemApprovalProcessInterface.prepareStartInstruction(instruction);

            instructions.add(instruction);
        }
        return instructions;
    }

    // creates an ObjectTreeDeltas that will be executed after successful approval of the given assignment
    private ObjectTreeDeltas associationAdditionToDelta(ModelContext<?> modelContext, AssociationAdditionType addition, String objectOid)
            throws SchemaException {
        ObjectTreeDeltas changes = new ObjectTreeDeltas(prismContext);
        ResourceShadowDiscriminator shadowDiscriminator =
                ResourceShadowDiscriminator.fromResourceShadowDiscriminatorType(addition.getResourceShadowDiscriminator());
        String projectionOid = modelContext.findProjectionContext(shadowDiscriminator).getOid();
        ObjectDelta<ShadowType> objectDelta = (ObjectDelta<ShadowType>) DeltaBuilder.deltaFor(ShadowType.class, prismContext)
                .item(ShadowType.F_ASSOCIATION).add(addition.getAssociation().clone())
                .asObjectDelta(projectionOid);

        changes.addProjectionChange(shadowDiscriminator, objectDelta);
        return changes;
    }

    //endregion

    //region ------------------------------------------------------------ Things that execute when item is being approved

    private static String formatTime(XMLGregorianCalendar time) {
        DateFormat formatter = DateFormat.getDateInstance();
        return formatter.format(time.toGregorianCalendar().getTime());
    }

    //endregion

    private boolean isAssociationRelevant(PcpAspectConfigurationType config, AssociationAdditionType itemToApprove,
            ResourceShadowDiscriminator rsd, ModelContext<?> modelContext, Task task, OperationResult result) {
        LOGGER.trace(" - considering: {}", itemToApprove);
        ExpressionVariables variables = new ExpressionVariables();
        variables.addVariableDefinition(SchemaConstants.C_ASSOCIATION, itemToApprove.getAssociation());
        variables.addVariableDefinition(SchemaConstants.C_SHADOW_DISCRIMINATOR, rsd);
        boolean applicable = primaryChangeAspectHelper.evaluateApplicabilityCondition(
                config, modelContext, itemToApprove, variables, this, task, result);
        LOGGER.trace("   - result: applicable = {}", applicable);
        return applicable;
    }

    private ShadowAssociationType cloneAndCanonicalizeAssociation(ShadowAssociationType a) {
        return a.clone();       // TODO - should we canonicalize?
    }

    // creates an approval requests (e.g. by providing approval schema) for a given assignment and a target
    private ApprovalRequest<AssociationAdditionType>
    createApprovalRequest(PcpAspectConfigurationType config, AssociationAdditionType itemToApprove, ModelContext<?> modelContext,
			Task taskFromModel, OperationResult result) {
        ApprovalRequest<AssociationAdditionType> request = new ApprovalRequestImpl<>(itemToApprove, config, prismContext);
        approvalSchemaHelper.prepareSchema(request.getApprovalSchemaType(),
                createRelationResolver((PrismObject<?>) null, result),		// TODO rel resolver
				createReferenceResolver(modelContext, taskFromModel, result));
        return request;
    }

    // retrieves the relevant target for a given assignment - a role, an org, or a resource
    private ShadowType getAssociationApprovalTarget(ShadowAssociationType association, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return primaryChangeAspectHelper.resolveTargetUnchecked(association, result);
    }

}