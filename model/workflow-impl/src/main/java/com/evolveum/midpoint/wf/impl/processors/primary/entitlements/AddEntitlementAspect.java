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
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.addrole.AddRoleVariableNames;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequest;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ApprovalRequestImpl;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ItemApprovalProcessInterface;
import com.evolveum.midpoint.wf.impl.processes.itemApproval.ProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processors.primary.ObjectTreeDeltas;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpChildJobCreationInstruction;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.BasePrimaryChangeAspect;
import com.evolveum.midpoint.wf.impl.processors.primary.aspect.PrimaryChangeAspectHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.assignments.AssignmentHelper;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.AssociationCreationApprovalFormType;
import com.evolveum.midpoint.xml.ns.model.workflow.common_forms_3.QuestionFormType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Aspect for adding entitlements.
 *
 * In current version it treats entitlements that are DIRECTLY added, i.e. not as a part of an assignment.
 *
 * @author mederly
 */
@Component
public class AddEntitlementAspect extends BasePrimaryChangeAspect {

    private static final Trace LOGGER = TraceManager.getTrace(AddEntitlementAspect.class);

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ItemApprovalProcessInterface itemApprovalProcessInterface;

    @Autowired
    protected AssignmentHelper assignmentHelper;

    @Autowired
    protected PrimaryChangeAspectHelper primaryChangeAspectHelper;

    //region ------------------------------------------------------------ Things that execute on request arrival

    @Override
    public List<PcpChildJobCreationInstruction> prepareJobCreationInstructions(ModelContext<?> modelContext, WfConfigurationType wfConfigurationType, ObjectTreeDeltas objectTreeDeltas, Task taskFromModel, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (!isFocusRelevant(modelContext)) {
            return null;
        }
        List<ApprovalRequest<AssociationAdditionType>> approvalRequestList = getApprovalRequests(modelContext, wfConfigurationType, objectTreeDeltas, result);
        if (approvalRequestList == null || approvalRequestList.isEmpty()) {
            return null;
        }
        return prepareJobCreateInstructions(modelContext, taskFromModel, result, approvalRequestList);
    }

    protected boolean isFocusRelevant(ModelContext modelContext) {
        //return modelContext.getFocusClass() != null && UserType.class.isAssignableFrom(modelContext.getFocusClass());
        return true;
    }

    private List<ApprovalRequest<AssociationAdditionType>> getApprovalRequests(ModelContext<?> modelContext, WfConfigurationType wfConfigurationType,
                                                                             ObjectTreeDeltas changes, OperationResult result) {

        List<ApprovalRequest<AssociationAdditionType>> requests = new ArrayList<>();

        PcpAspectConfigurationType config = primaryChangeAspectHelper.getPcpAspectConfigurationType(wfConfigurationType, this);
        Set<Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>>> entries = changes.getProjectionChangeMapEntries();
        for (Map.Entry<ResourceShadowDiscriminator, ObjectDelta<ShadowType>> entry : entries) {
            ObjectDelta<ShadowType> delta = entry.getValue();
            if (delta.isAdd()) {
                requests.addAll(getApprovalRequestsFromShadowAdd(config, entry.getValue(), entry.getKey(), result));
            } else if (delta.isModify()) {
                ModelProjectionContext projectionContext = modelContext.findProjectionContext(entry.getKey());
                requests.addAll(getApprovalRequestsFromShadowModify(config, projectionContext.getObjectOld(), entry.getValue(), entry.getKey(), result));
            } else {
                // no-op
            }
        }
        return requests;
    }

    private List<ApprovalRequest<AssociationAdditionType>>
    getApprovalRequestsFromShadowAdd(PcpAspectConfigurationType config, ObjectDelta<ShadowType> change,
                                     ResourceShadowDiscriminator rsd, OperationResult result) {
        LOGGER.trace("Relevant associations in shadow add delta:");

        List<ApprovalRequest<AssociationAdditionType>> approvalRequestList = new ArrayList<>();
        ShadowType shadowType = change.getObjectToAdd().asObjectable();
        Iterator<ShadowAssociationType> associationIterator = shadowType.getAssociation().iterator();
        while (associationIterator.hasNext()) {
            ShadowAssociationType a = associationIterator.next();
            if (isAssociationRelevant(a)) {
                LOGGER.trace(" - {}", a);
                ShadowAssociationType aCopy = cloneAndCanonicalizeAssociation(a);
                approvalRequestList.add(createApprovalRequest(config, aCopy, rsd));
                associationIterator.remove();
            }
        }
        return approvalRequestList;
    }

    private List<ApprovalRequest<AssociationAdditionType>>
    getApprovalRequestsFromShadowModify(PcpAspectConfigurationType config, PrismObject<ShadowType> shadowOld,
                                        ObjectDelta<ShadowType> change, ResourceShadowDiscriminator rsd,
                                        OperationResult result) {
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
                    ApprovalRequest<AssociationAdditionType> req = processAssociationToAdd(config, association, rsd, result);
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
                    ApprovalRequest<AssociationAdditionType> req = processAssociationToAdd(config, association, rsd, result);
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
                            ResourceShadowDiscriminator rsd, OperationResult result) {
        ShadowAssociationType association = associationCval.asContainerable();
        if (isAssociationRelevant(association)) {
            LOGGER.trace(" - {}", association);
            ShadowAssociationType aCopy = cloneAndCanonicalizeAssociation(association);
            return createApprovalRequest(config, aCopy, rsd);
        }
        return null;
    }

    private List<PcpChildJobCreationInstruction>
    prepareJobCreateInstructions(ModelContext<?> modelContext, Task taskFromModel,
                                 OperationResult result, List<ApprovalRequest<AssociationAdditionType>> approvalRequestList)
            throws SchemaException, ObjectNotFoundException {

        List<PcpChildJobCreationInstruction> instructions = new ArrayList<>();
        String assigneeName = MiscDataUtil.getFocusObjectName(modelContext);
        String assigneeOid = primaryChangeAspectHelper.getObjectOid(modelContext);
        PrismObject<UserType> requester = primaryChangeAspectHelper.getRequester(taskFromModel, result);

        for (ApprovalRequest<AssociationAdditionType> approvalRequest : approvalRequestList) {

            assert approvalRequest.getPrismContext() != null;

            LOGGER.trace("Approval request = {}", approvalRequest);
            AssociationAdditionType associationAddition = approvalRequest.getItemToApprove();
            ShadowAssociationType association = associationAddition.getAssociation();
            ShadowType target = getAssociationApprovalTarget(association, result);
            Validate.notNull(target, "No target in association to be approved");

            String targetName = target.getName() != null ? target.getName().getOrig() : "(unnamed)";

            // create a JobCreateInstruction for a given change processor (primaryChangeProcessor in this case)
            PcpChildJobCreationInstruction instruction =
                    PcpChildJobCreationInstruction.createInstruction(getChangeProcessor());

            // set some common task/process attributes
            instruction.prepareCommonAttributes(this, modelContext, assigneeOid, requester);

            // prepare and set the delta that has to be approved
            ObjectTreeDeltas objectTreeDeltas = entitlementAdditionToDelta(modelContext, associationAddition, assigneeOid);
            instruction.setObjectTreeDeltasProcessAndTaskVariables(objectTreeDeltas);

            // set the names of midPoint task and activiti process instance
            String andExecuting = instruction.isExecuteApprovedChangeImmediately() ? "and executing " : "";
            instruction.setTaskName("Workflow for approving " + andExecuting + "adding " + targetName + " to " + assigneeName);
            instruction.setProcessInstanceName("Adding " + targetName + " to " + assigneeName);

            // setup general item approval process
            String approvalTaskName = "Approve adding " + targetName + " to " + assigneeName;
            itemApprovalProcessInterface.prepareStartInstruction(instruction, approvalRequest, approvalTaskName);

            // set some aspect-specific variables
            instruction.addProcessVariable(AddRoleVariableNames.FOCUS_NAME, assigneeName);

            instructions.add(instruction);
        }
        return instructions;
    }

    // creates an ObjectTreeDeltas that will be executed after successful approval of the given assignment
    private ObjectTreeDeltas entitlementAdditionToDelta(ModelContext<?> modelContext, AssociationAdditionType addition, String objectOid)
            throws SchemaException {
        ObjectTreeDeltas changes = new ObjectTreeDeltas(prismContext);
        ResourceShadowDiscriminator shadowDiscriminator =
                ResourceShadowDiscriminator.fromResourceShadowDiscriminatorType(addition.getResourceShadowDiscriminator());
        String projectionOid = modelContext.findProjectionContext(shadowDiscriminator).getOid();
        ObjectDelta<ShadowType> objectDelta = DeltaBuilder.deltaFor(ShadowType.class, prismContext)
                .item(ShadowType.F_ASSOCIATION).add(addition.getAssociation().clone())
                .asObjectDelta(projectionOid);

        changes.addProjectionChange(shadowDiscriminator, objectDelta);
        return changes;
    }

    //endregion

    //region ------------------------------------------------------------ Things that execute when item is being approved

    @Override
    public PrismObject<? extends QuestionFormType> prepareQuestionForm(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("prepareQuestionForm starting: execution id {}, pid {}, variables = {}", task.getExecutionId(), task.getProcessInstanceId(), variables);
        }

        // todo check type compatibility
        ApprovalRequest request = (ApprovalRequest) variables.get(ProcessVariableNames.APPROVAL_REQUEST);
        request.setPrismContext(prismContext);
        Validate.notNull(request, "Approval request is not present among process variables");

        AssociationAdditionType aat = (AssociationAdditionType) request.getItemToApprove();
        Validate.notNull(aat, "Approval request does not contain the association addition information");

        ShadowType target = getAssociationApprovalTarget(aat.getAssociation(), result);     // may throw an (unchecked) exception

        PrismObjectDefinition<AssociationCreationApprovalFormType> formDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByType(AssociationCreationApprovalFormType.COMPLEX_TYPE);
        PrismObject<AssociationCreationApprovalFormType> formPrism = formDefinition.instantiate();
        AssociationCreationApprovalFormType form = formPrism.asObjectable();

        String focusName = (String) variables.get(AddRoleVariableNames.FOCUS_NAME);
        form.setFocusName(focusName);                                   // TODO distinguish somehow between users/roles/orgs
        form.setAssociatedObjectName(getTargetDisplayName(target));

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Resulting prism object instance = {}", formPrism.debugDump());
        }
        return formPrism;
    }

    private static String formatTime(XMLGregorianCalendar time) {
        DateFormat formatter = DateFormat.getDateInstance();
        return formatter.format(time.toGregorianCalendar().getTime());
    }

    @Override
    public PrismObject<? extends ObjectType> prepareRelatedObject(org.activiti.engine.task.Task task, Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return null;
    }
    //endregion

    //region ------------------------------------------------------------ Things to override in concrete aspect classes

    protected boolean isAssociationRelevant(ShadowAssociationType association) {
        return true;
        // TODO implement some expression evaluation
    }

    protected ShadowAssociationType cloneAndCanonicalizeAssociation(ShadowAssociationType a) {
        return a.clone();       // TODO
    }

    // creates an approval requests (e.g. by providing approval schema) for a given assignment and a target
    protected ApprovalRequest<AssociationAdditionType>
    createApprovalRequest(PcpAspectConfigurationType config, ShadowAssociationType association,
                          ResourceShadowDiscriminator resourceShadowDiscriminator) {
        AssociationAdditionType aat = new AssociationAdditionType(prismContext);
        aat.setAssociation(association);
        aat.setResourceShadowDiscriminator(resourceShadowDiscriminator.toResourceShadowDiscriminatorType());
        return new ApprovalRequestImpl<>(aat, config, prismContext);
    }

    // retrieves the relevant target for a given assignment - a role, an org, or a resource
    protected ShadowType getAssociationApprovalTarget(ShadowAssociationType association, OperationResult result) throws SchemaException, ObjectNotFoundException {
        return primaryChangeAspectHelper.resolveTargetUnchecked(association, result);
    }

    // creates name to be displayed in the question form (may be overriden by child objects)
    protected String getTargetDisplayName(ShadowType target) {
        if (target.getName() != null) {
            return target.getName().getOrig();
        } else {
            return target.getOid();
        }
    }
    //endregion
}