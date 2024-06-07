/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.EQUIVALENT;
import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.SUPERPATH;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.cases.api.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensElementContext;
import com.evolveum.midpoint.model.impl.lens.OperationalDataManager;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.util.MiscHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ApprovalMetadataHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ApprovalMetadataHelper.class);

    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private OperationalDataManager operationalDataManager;
    @Autowired private MiscHelper miscHelper;
    @Autowired private CaseManager caseManager;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    public void addAssignmentApprovalMetadata(
            ObjectDelta<?> objectDelta, CaseType aCase, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        if (objectDelta.isAdd()) {
            addAssignmentApprovalMetadataOnObjectAdd(objectDelta.getObjectToAdd(), aCase, task, result);
        } else if (objectDelta.isModify()) {
            addAssignmentApprovalMetadataOnObjectModify(objectDelta, aCase, task, result);
        }
    }

    /**
     * See also {@link OperationalDataManager#applyMetadataOnObjectModifyOp(
     * ObjectDelta, LensElementContext, XMLGregorianCalendar, Task, LensContext)}
     */
    private void addAssignmentApprovalMetadataOnObjectModify(
            ObjectDelta<?> objectDelta, CaseType aCase, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        Collection<ObjectReferenceType> approvedBy = getApprovedBy(aCase);
        Collection<String> comments = getApproverComments(aCase, task, result);
        Set<Long> processedIds = new HashSet<>();
        Holder<AssignmentHolderType> focusObjectHolder = new Holder<>();
        List<ItemDelta<?, ?>> assignmentMetadataDeltas = new ArrayList<>();
        for (ItemDelta<?, ?> itemDelta : objectDelta.getModifications()) {
            ItemPath deltaPath = itemDelta.getPath();
            ItemPath.CompareResult comparison = deltaPath.compareComplex(FocusType.F_ASSIGNMENT);
            if (comparison == EQUIVALENT) {
                // whole assignment is being added/replaced (or deleted but we are not interested in that)
                //noinspection unchecked
                ContainerDelta<AssignmentType> assignmentDelta = (ContainerDelta<AssignmentType>) itemDelta;
                for (PrismContainerValue<AssignmentType> assignmentContainerValue : emptyIfNull(assignmentDelta.getValuesToAdd())) {
                    operationalDataManager.addAssignmentCreationApprovalMetadata(
                            assignmentContainerValue.asContainerable(), approvedBy, comments);
                }
                for (PrismContainerValue<AssignmentType> assignmentContainerValue: emptyIfNull(assignmentDelta.getValuesToReplace())) {
                    operationalDataManager.addAssignmentCreationApprovalMetadata(
                            assignmentContainerValue.asContainerable(), approvedBy, comments);
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
                    var focus = fetchFocus(objectDelta, focusObjectHolder, result); // needed because of metadata PCV IDs
                    assignmentMetadataDeltas.addAll(
                            operationalDataManager.createAssignmentModificationApprovalMetadata(focus, id, approvedBy, comments));
                }
            }
        }
        ItemDeltaCollectionsUtil.mergeAll(objectDelta.getModifications(), assignmentMetadataDeltas);
    }

    private <T extends AssignmentHolderType> T fetchFocus(
            ObjectDelta<?> objectDelta, Holder<AssignmentHolderType> focusObjectHolder, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        if (focusObjectHolder.getValue() != null) {
            //noinspection unchecked
            return (T) focusObjectHolder.getValue();
        }
        //noinspection unchecked
        var object = repositoryService
                .getObject((Class<T>) objectDelta.getObjectTypeClass(), objectDelta.getOid(), null, result)
                .asObjectable();
        focusObjectHolder.setValue(object);
        return object;
    }

    private void addAssignmentApprovalMetadataOnObjectAdd(PrismObject<?> object, CaseType aCase,
            Task task, OperationResult result) throws SchemaException {
        Objectable objectable = object.asObjectable();
        if (!(objectable instanceof FocusType focus)) {
            return;
        }

        Collection<ObjectReferenceType> approvedBy = getApprovedBy(aCase);
        Collection<String> comments = getApproverComments(aCase, task, result);

        for (AssignmentType assignment : focus.getAssignment()) {
            operationalDataManager.addAssignmentCreationApprovalMetadata(assignment, approvedBy, comments);
        }
    }

    private Collection<String> getApproverComments(CaseType aCase, Task task, OperationResult result) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        PerformerCommentsFormattingType formatting = systemConfiguration != null &&
                systemConfiguration.asObjectable().getWorkflowConfiguration() != null ?
                systemConfiguration.asObjectable().getWorkflowConfiguration().getApproverCommentsFormatting() : null;
        PerformerCommentsFormatter formatter = caseManager.createPerformerCommentsFormatter(formatting);

        List<String> rv = new ArrayList<>();
        for (CaseWorkItemType workItem : aCase.getWorkItem()) {
            if (ApprovalUtils.isApproved(workItem.getOutput()) && StringUtils.isNotBlank(workItem.getOutput().getComment())) {
                CollectionUtils.addIgnoreNull(rv, formatter.formatComment(workItem, task, result));
            }
        }
        LOGGER.trace("approver comments = {}", rv);
        return rv;
    }

    private Collection<ObjectReferenceType> getApprovedBy(CaseType aCase) {
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
                rv.addAll(getApprovedBy(subcase));
            }
            return ObjectTypeUtil.keepDistinctReferences(rv);
        } else {
            return getApprovedBy(aCase);
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
