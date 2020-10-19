/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.wf.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
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

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private PrismContext prismContext;
    @Autowired private MiscHelper miscHelper;
    @Autowired private WorkflowManager workflowManager;

    public void addAssignmentApprovalMetadata(ObjectDelta<?> objectDelta, CaseType aCase,
            Task task, OperationResult result) throws SchemaException {
        if (objectDelta.isAdd()) {
            addAssignmentApprovalMetadataOnObjectAdd(objectDelta.getObjectToAdd(), aCase, task, result);
        } else if (objectDelta.isModify()) {
            addAssignmentApprovalMetadataOnObjectModify(objectDelta, aCase, task, result);
        }
    }

    /**
     * See also {@link com.evolveum.midpoint.model.impl.lens.OperationalDataManager#applyAssignmentMetadataDelta(LensContext, ObjectDelta, XMLGregorianCalendar, Task)}.
     */
    @SuppressWarnings("JavadocReference")
    private void addAssignmentApprovalMetadataOnObjectModify(ObjectDelta<?> objectDelta, CaseType aCase,
            Task task, OperationResult result) throws SchemaException {
        Collection<ObjectReferenceType> approvedBy = getApprovedBy(aCase, result);
        Collection<String> comments = getApproverComments(aCase, task, result);
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
                Object secondSegment = deltaPath.rest().first();
                if (!ItemPath.isId(secondSegment)) {
                    throw new IllegalStateException("Assignment modification contains no assignment ID. Offending path = " + deltaPath);
                }
                Long id = ItemPath.toId(secondSegment);
                if (id == null) {
                    throw new IllegalStateException("Assignment modification contains no assignment ID. Offending path = " + deltaPath);
                }
                if (processedIds.add(id)) {
                    assignmentMetadataDeltas.addAll(
                            createAssignmentModificationApprovalMetadata(objectDelta.getObjectTypeClass(), id, approvedBy, comments));
                }
            }
        }
        ItemDeltaCollectionsUtil.mergeAll(objectDelta.getModifications(), assignmentMetadataDeltas);
    }

    private void addAssignmentApprovalMetadataOnObjectAdd(PrismObject<?> object, CaseType aCase,
            Task task, OperationResult result) throws SchemaException {
        Objectable objectable = object.asObjectable();
        if (!(objectable instanceof FocusType)) {
            return;
        }
        FocusType focus = (FocusType) objectable;

        Collection<ObjectReferenceType> approvedBy = getApprovedBy(aCase, result);
        Collection<String> comments = getApproverComments(aCase, task, result);

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
        return prismContext.deltaFor(objectTypeClass)
                .item(FocusType.F_ASSIGNMENT, assignmentId, AssignmentType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF)
                        .replaceRealValues(CloneUtil.cloneCollectionMembers(approvedBy))
                .item(FocusType.F_ASSIGNMENT, assignmentId, AssignmentType.F_METADATA, MetadataType.F_MODIFY_APPROVAL_COMMENT)
                        .replaceRealValues(comments)
                .asItemDeltas();
    }

    public Collection<String> getApproverComments(CaseType aCase, Task task, OperationResult result) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        PerformerCommentsFormattingType formatting = systemConfiguration != null &&
                systemConfiguration.asObjectable().getWorkflowConfiguration() != null ?
                systemConfiguration.asObjectable().getWorkflowConfiguration().getApproverCommentsFormatting() : null;
        PerformerCommentsFormatter formatter = workflowManager.createPerformerCommentsFormatter(formatting);

        List<String> rv = new ArrayList<>();
        for (CaseWorkItemType workItem : aCase.getWorkItem()) {
            if (ApprovalUtils.isApproved(workItem.getOutput()) && StringUtils.isNotBlank(workItem.getOutput().getComment())) {
                CollectionUtils.addIgnoreNull(rv, formatter.formatComment(workItem, task, result));
            }
        }
        LOGGER.trace("approver comments = {}", rv);
        return rv;
    }

    public Collection<ObjectReferenceType> getApprovedBy(CaseType aCase, OperationResult result) {
        List<ObjectReferenceType> rv = new ArrayList<>();
        for (CaseWorkItemType workItem : aCase.getWorkItem()) {
            if (ApprovalUtils.isApproved(workItem.getOutput()) && workItem.getPerformerRef() != null) {
                rv.add(workItem.getPerformerRef().clone());
            }
        }
        LOGGER.trace("approvedBy = {}", rv);
        return rv;
    }

    public Collection<? extends ObjectReferenceType> getAllApprovers(CaseType aCase, OperationResult result)
            throws SchemaException {
        if (aCase.getParentRef() == null) {
            List<ObjectReferenceType> rv = new ArrayList<>();
            for (CaseType subcase : miscHelper.getSubcases(aCase, result)) {
                rv.addAll(getApprovedBy(subcase, result));
            }
            return ObjectTypeUtil.keepDistinctReferences(rv);
        } else {
            return getApprovedBy(aCase, result);
        }
    }

    public Collection<String> getAllApproverComments(CaseType aCase, Task task, OperationResult result)
            throws SchemaException {
        if (aCase.getParentRef() == null) {
            Set<String> rv = new HashSet<>();
            for (CaseType subcase : miscHelper.getSubcases(aCase, result)) {
                rv.addAll(getApproverComments(subcase, task, result));
            }
            return rv;
        } else {
            return getApproverComments(aCase, task, result);
        }
    }


}
