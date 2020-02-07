/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.*;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.EQUIVALENT;
import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.SUPERPATH;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

/**
 * @author semancik
 *
 */
@Component
public class OperationalDataManager {

    private static final Trace LOGGER = TraceManager.getTrace(OperationalDataManager.class);

    @Autowired(required = false)
    private ActivationComputer activationComputer;

    @Autowired
    private PrismContext prismContext;

    public <F extends ObjectType> void setRequestMetadataInContext(LensContext<F> context, XMLGregorianCalendar now, Task task)
            throws SchemaException {
        MetadataType requestMetadata = collectRequestMetadata(context, now, task);
        context.setRequestMetadata(requestMetadata);
    }

    private <F extends ObjectType> MetadataType collectRequestMetadata(LensContext<F> context, XMLGregorianCalendar now, Task task) {
        MetadataType metaData = new MetadataType();
        metaData.setCreateChannel(LensUtil.getChannel(context, task));      // TODO is this really used?
        metaData.setRequestTimestamp(now);
        if (task.getOwner() != null) {
            metaData.setRequestorRef(createObjectRef(task.getOwner(), prismContext));
        }
        // It is not necessary to store requestor comment here as it is preserved in context.options field.
        return metaData;
    }

    private <F extends ObjectType> void transplantRequestMetadata(LensContext<F> context, MetadataType metaData) {
        MetadataType requestMetadata = context.getRequestMetadata();
        if (requestMetadata != null) {
            metaData.setRequestTimestamp(requestMetadata.getRequestTimestamp());
            metaData.setRequestorRef(requestMetadata.getRequestorRef());
        }
        OperationBusinessContextType businessContext = context.getRequestBusinessContext();
        if (businessContext != null) {
            metaData.setRequestorComment(businessContext.getComment());
        }
    }

    public <T extends ObjectType, F extends ObjectType> void applyMetadataAdd(LensContext<F> context,
            PrismObject<T> objectToAdd, XMLGregorianCalendar now, Task task, OperationResult result)
                    throws SchemaException {

        T objectType = objectToAdd.asObjectable();
        MetadataType metadataType = objectType.getMetadata();
        if (metadataType == null) {
            metadataType = new MetadataType(prismContext);
            objectType.setMetadata(metadataType);
        }

        transplantRequestMetadata(context, metadataType);

        applyCreateMetadata(context, metadataType, now, task);

        metadataType.getCreateApproverRef().addAll(CloneUtil.cloneCollectionMembers(context.getOperationApprovedBy()));
        metadataType.getCreateApprovalComment().addAll(context.getOperationApproverComments());

        if (objectToAdd.canRepresent(FocusType.class)) {
            applyAssignmentMetadataObject((LensContext<? extends FocusType>) context, objectToAdd, now, task, result);
        }
    }

    public <T extends ObjectType, F extends ObjectType> void applyMetadataModify(ObjectDelta<T> objectDelta,
            LensElementContext<T> objectContext, Class objectTypeClass,
            XMLGregorianCalendar now, Task task, LensContext<F> context,
            OperationResult result) throws SchemaException {

        ItemDeltaCollectionsUtil.mergeAll(objectDelta.getModifications(),
                createModifyMetadataDeltas(context, ObjectType.F_METADATA, objectTypeClass, now, task));

        List<PrismReferenceValue> approverReferenceValues = new ArrayList<>();
        for (ObjectReferenceType approverRef : context.getOperationApprovedBy()) {
            approverReferenceValues.add(approverRef.asReferenceValue().clone());
        }
        ItemDeltaCollectionsUtil.mergeAll(objectDelta.getModifications(),
                prismContext.deltaFor(objectTypeClass)
                        .item(ObjectType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF).replace(approverReferenceValues)
                        .item(ObjectType.F_METADATA, MetadataType.F_MODIFY_APPROVAL_COMMENT).replaceRealValues(context.getOperationApproverComments())
                        .asItemDeltas());
        if (FocusType.class.isAssignableFrom(objectTypeClass)) {
            applyAssignmentMetadataDelta((LensContext) context,
                    (ObjectDelta)objectDelta, now, task, result);
        }
    }

    private <F extends FocusType, T extends ObjectType> void applyAssignmentMetadataObject(LensContext<F> context,
            PrismObject<T> objectToAdd,
            XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {

        PrismContainer<AssignmentType> assignmentContainer = objectToAdd.findContainer(FocusType.F_ASSIGNMENT);
        if (assignmentContainer != null) {
            for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentContainer.getValues()) {
                applyAssignmentValueMetadataAdd(context, assignmentContainerValue, "ADD", now, task, result);
            }
        }
    }

    private <F extends FocusType> void applyAssignmentMetadataDelta(LensContext<F> context, ObjectDelta<F> objectDelta,
            XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {

        if (objectDelta == null || objectDelta.isDelete()) {
            return;
        }

        if (objectDelta.isAdd()) {
            applyAssignmentMetadataObject(context, objectDelta.getObjectToAdd(), now, task, result);
        } else {
            // see also ApprovalMetadataHelper.addAssignmentApprovalMetadataOnObjectModify
            Set<Long> processedIds = new HashSet<>();
            List<ItemDelta<?,?>> assignmentMetadataDeltas = new ArrayList<>();
            for (ItemDelta<?,?> itemDelta: objectDelta.getModifications()) {
                ItemPath deltaPath = itemDelta.getPath();
                CompareResult comparison = deltaPath.compareComplex(SchemaConstants.PATH_ASSIGNMENT);
                if (comparison == EQUIVALENT) {
                    // whole assignment is being added/replaced (or deleted but we are not interested in that)
                    ContainerDelta<AssignmentType> assignmentDelta = (ContainerDelta<AssignmentType>)itemDelta;
                    if (assignmentDelta.getValuesToAdd() != null) {
                        for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentDelta.getValuesToAdd()) {
                            applyAssignmentValueMetadataAdd(context, assignmentContainerValue, "MOD/add", now, task, result);
                        }
                    }
                    if (assignmentDelta.getValuesToReplace() != null) {
                        for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentDelta.getValuesToReplace()) {
                            applyAssignmentValueMetadataAdd(context, assignmentContainerValue, "MOD/replace", now, task, result);
                        }
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
                        assignmentMetadataDeltas.addAll(createModifyMetadataDeltas(context,
                                ItemPath.create(FocusType.F_ASSIGNMENT, id, AssignmentType.F_METADATA), context.getFocusClass(), now, task));
                    }
                }
            }
            ItemDeltaCollectionsUtil.mergeAll(objectDelta.getModifications(), assignmentMetadataDeltas);
        }
    }

    private <F extends FocusType> void applyAssignmentValueMetadataAdd(LensContext<F> context,
            PrismContainerValue<AssignmentType> assignmentContainerValue, String desc,
            XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {

        AssignmentType assignmentType = assignmentContainerValue.asContainerable();
        MetadataType metadataType = assignmentType.getMetadata();
        if (metadataType == null) {
            metadataType = new MetadataType();
            assignmentType.setMetadata(metadataType);
        }

        transplantRequestMetadata(context, metadataType);

        // This applies the effective status only to assignments that are completely new (whole container is added/replaced)
        // The effectiveStatus of existing assignments is processed in FocusProcessor.processAssignmentActivation()
        // We cannot process that here. Because this code is not even triggered when there is no delta. So recompute will not work.
        ActivationType activationType = assignmentType.getActivation();
        ActivationStatusType effectiveStatus = activationComputer.getEffectiveStatus(assignmentType.getLifecycleState(), activationType, null);
        if (activationType == null) {
            activationType = new ActivationType();
            assignmentType.setActivation(activationType);
        }
        activationType.setEffectiveStatus(effectiveStatus);

        applyCreateMetadata(context, metadataType, now, task);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Adding operational data {} to assignment cval ({}):\nMETADATA:\n{}\nACTIVATION:\n{}",
                 metadataType, desc, assignmentContainerValue.debugDump(1), activationType.asPrismContainerValue().debugDump(1));
        }
    }

    public <F extends ObjectType> MetadataType createCreateMetadata(LensContext<F> context, XMLGregorianCalendar now, Task task) {
        MetadataType metaData = new MetadataType(prismContext);
        applyCreateMetadata(context, metaData, now, task);
        return metaData;
    }

    private <F extends ObjectType> void applyCreateMetadata(LensContext<F> context, MetadataType metaData, XMLGregorianCalendar now, Task task) {
        String channel = LensUtil.getChannel(context, task);
        metaData.setCreateChannel(channel);
        metaData.setCreateTimestamp(now);
        if (task.getOwner() != null) {
            metaData.setCreatorRef(createObjectRef(task.getOwner(), prismContext));
        }
        metaData.setCreateTaskRef(task.getOid() != null ? task.getSelfReference() : null);
    }

    public <F extends ObjectType, T extends ObjectType> Collection<ItemDelta<?,?>> createModifyMetadataDeltas(LensContext<F> context,
            ItemPath metadataPath, Class<T> objectType, XMLGregorianCalendar now, Task task) throws SchemaException {
        return prismContext.deltaFor(objectType)
                .item(metadataPath.append(MetadataType.F_MODIFY_CHANNEL)).replace(LensUtil.getChannel(context, task))
                .item(metadataPath.append(MetadataType.F_MODIFY_TIMESTAMP)).replace(now)
                .item(metadataPath.append(MetadataType.F_MODIFIER_REF)).replace(createObjectRef(task.getOwner(), prismContext))
                .item(metadataPath.append(MetadataType.F_MODIFY_TASK_REF)).replaceRealValues(
                        task.getOid() != null ? singleton(task.getSelfReference()) : emptySet())
                .asItemDeltas();
    }

}
