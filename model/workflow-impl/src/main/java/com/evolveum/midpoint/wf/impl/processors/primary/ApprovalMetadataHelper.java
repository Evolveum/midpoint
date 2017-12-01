/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.EQUIVALENT;
import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.SUPERPATH;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

/**
 * @author mederly
 */
@Component
public class ApprovalMetadataHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ApprovalMetadataHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired private WorkflowManager workflowManager;

    public void addAssignmentApprovalMetadata(ObjectDelta<?> objectDelta, Task task, OperationResult result) throws SchemaException {
        if (objectDelta.isAdd()) {
            addAssignmentApprovalMetadataOnObjectAdd(objectDelta.getObjectToAdd(), task, result);
        } else if (objectDelta.isModify()) {
            addAssignmentApprovalMetadataOnObjectModify(objectDelta, task, result);
        }
    }

    private void addAssignmentApprovalMetadataOnObjectModify(ObjectDelta<?> objectDelta, Task task,
            OperationResult result) throws SchemaException {
        // see also OperationalDataManager.applyAssignmentMetadataDelta
        Collection<ObjectReferenceType> approvedBy = workflowManager.getApprovedBy(task, result);
        Collection<String> comments = workflowManager.getApproverComments(task, result);
        Set<Long> processedIds = new HashSet<>();
        List<ItemDelta<?,?>> assignmentMetadataDeltas = new ArrayList<>();
        for (ItemDelta<?,?> itemDelta: objectDelta.getModifications()) {
            ItemPath deltaPath = itemDelta.getPath();
            ItemPath.CompareResult comparison = deltaPath.compareComplex(SchemaConstants.PATH_ASSIGNMENT);
            if (comparison == EQUIVALENT) {
                // whole assignment is being added/replaced (or deleted but we are not interested in that)
                ContainerDelta<AssignmentType> assignmentDelta = (ContainerDelta<AssignmentType>)itemDelta;
                for (PrismContainerValue<AssignmentType> assignmentContainerValue : emptyIfNull(assignmentDelta.getValuesToAdd())) {
                    addAssignmentCreationApprovalMetadata(assignmentContainerValue.asContainerable(), approvedBy, comments);
                }
                for (PrismContainerValue<AssignmentType> assignmentContainerValue: emptyIfNull(assignmentDelta.getValuesToReplace())) {
                    addAssignmentCreationApprovalMetadata(assignmentContainerValue.asContainerable(), approvedBy, comments);
                }
            } else if (comparison == SUPERPATH) {
                ItemPathSegment secondSegment = deltaPath.rest().first();
                if (!(secondSegment instanceof IdItemPathSegment)) {
                    throw new IllegalStateException("Assignment modification contains no assignment ID. Offending path = " + deltaPath);
                }
                Long id = ((IdItemPathSegment) secondSegment).getId();
                if (id == null) {
                    throw new IllegalStateException("Assignment modification contains no assignment ID. Offending path = " + deltaPath);
                }
                if (processedIds.add(id)) {
                    assignmentMetadataDeltas.addAll(
                            createAssignmentModificationApprovalMetadata(objectDelta.getObjectTypeClass(), id, approvedBy, comments));
                }
            }
        }
        ItemDelta.mergeAll(objectDelta.getModifications(), assignmentMetadataDeltas);
    }

    private void addAssignmentApprovalMetadataOnObjectAdd(PrismObject<?> object, Task task,
            OperationResult result) throws SchemaException {
        Objectable objectable = object.asObjectable();
        if (!(objectable instanceof FocusType)) {
            return;
        }
        FocusType focus = (FocusType) objectable;

        Collection<ObjectReferenceType> approvedBy = workflowManager.getApprovedBy(task, result);
        Collection<String> comments = workflowManager.getApproverComments(task, result);

        for (AssignmentType assignment : focus.getAssignment()) {
            addAssignmentCreationApprovalMetadata(assignment, approvedBy, comments);
        }
    }

    private void addAssignmentCreationApprovalMetadata(AssignmentType assignment, Collection<ObjectReferenceType> approvedBy,
            Collection<String> comments) {
        MetadataType metadata = assignment.getMetadata();
        if (metadata == null) {
            assignment.setMetadata(metadata = new MetadataType(prismContext));
        }
        metadata.getCreateApproverRef().clear();
        metadata.getCreateApproverRef().addAll(CloneUtil.cloneCollectionMembers(approvedBy));
        metadata.getCreateApprovalComment().clear();
        metadata.getCreateApprovalComment().addAll(comments);
    }

    private Collection<ItemDelta<?, ?>> createAssignmentModificationApprovalMetadata(Class<? extends Objectable> objectTypeClass,
            long assignmentId, Collection<ObjectReferenceType> approvedBy, Collection<String> comments) throws SchemaException {
        return DeltaBuilder.deltaFor(objectTypeClass, prismContext)
                .item(FocusType.F_ASSIGNMENT, assignmentId, AssignmentType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF)
                        .replaceRealValues(CloneUtil.cloneCollectionMembers(approvedBy))
                .item(FocusType.F_ASSIGNMENT, assignmentId, AssignmentType.F_METADATA, MetadataType.F_MODIFY_APPROVAL_COMMENT)
                        .replaceRealValues(comments)
                .asItemDeltas();
    }
}
