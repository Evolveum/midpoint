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
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import static com.evolveum.midpoint.prism.PrismContainerValue.asContainerable;
import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.EQUIVALENT;
import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.SUPERPATH;

/**
 * Manages metadata (mostly before delta execution), but also some other operational data, namely assignment effective status.
 * (I am not sure why do we that here. See {@link #setAssignmentEffectiveStatus(AssignmentType)}.)
 *
 * @author semancik
 */
@Component
public class OperationalDataManager implements DeltaExecutionPreprocessor {

    private static final Trace LOGGER = TraceManager.getTrace(OperationalDataManager.class);

    @Autowired private ActivationComputer activationComputer;
    @Autowired private PrismContext prismContext;

    /**
     * Stores request metadata in the model context. Because the operation can finish later
     * (if switched to background), we need to have these data recorded at the beginning.
     */
    <F extends ObjectType> void setRequestMetadataInContext(LensContext<F> context, XMLGregorianCalendar now, Task task) {
        context.setRequestMetadata(
                collectRequestMetadata(now, task));
    }

    private MetadataType collectRequestMetadata(XMLGregorianCalendar now, Task task) {
        MetadataType metaData = new MetadataType();
        metaData.setRequestTimestamp(now);
        if (task.getOwnerRef() != null) {
            metaData.setRequestorRef(ObjectTypeUtil.createObjectRefCopy(task.getOwnerRef()));
        }
        // It is not necessary to store requestor comment here as it is preserved in context.options field.
        return metaData;
    }

    /**
     * "Transplants" previously stored request metadata from model context into target metadata container.
     */
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

    /**
     * Sets object and assignment metadata on object ADD operation.
     */
    public <T extends ObjectType, F extends ObjectType> void applyMetadataAdd(
            LensContext<F> context, PrismObject<T> objectToAdd, XMLGregorianCalendar now, Task task) {

        MetadataType metadata = getOrCreateMetadata(objectToAdd);

        transplantRequestMetadata(context, metadata);
        applyCreateMetadata(context, metadata, now, task);
        applyCreateApprovalMetadata(context, metadata);

        if (objectToAdd.canRepresent(AssignmentHolderType.class)) {
            //noinspection unchecked
            applyAssignmentMetadataObject((LensContext<? extends AssignmentHolderType>) context, objectToAdd, now, task);
        }
    }

    private <T extends ObjectType> @NotNull MetadataType getOrCreateMetadata(PrismObject<T> objectToAdd) {
        T object = objectToAdd.asObjectable();
        MetadataType metadata = object.getMetadata();
        if (metadata != null) {
            return metadata;
        } else {
            MetadataType newMetadata = new MetadataType();
            object.setMetadata(newMetadata);
            return newMetadata;
        }
    }

    public <T extends ObjectType, F extends ObjectType, AH extends AssignmentHolderType> void applyMetadataModify(
            ObjectDelta<T> objectDelta,
            Class<T> objectTypeClass,
            LensElementContext<T> elementContext,
            XMLGregorianCalendar now,
            Task task,
            LensContext<F> context) throws SchemaException {
        assert objectDelta.isModify();

        MetadataType currentMetadata = getCurrentMetadata(elementContext);

        ItemDeltaCollectionsUtil.mergeAll(objectDelta.getModifications(),
                createModifyMetadataDeltas(context, currentMetadata, ObjectType.F_METADATA, objectTypeClass, now, task));

        ItemDeltaCollectionsUtil.mergeAll(objectDelta.getModifications(),
                createModifyApprovalMetadataDeltas(objectTypeClass, context));

        if (AssignmentHolderType.class.isAssignableFrom(objectTypeClass)) {
            //noinspection unchecked
            applyAssignmentMetadataDelta((LensContext<AH>) context, (ObjectDelta<AH>) objectDelta, now, task);
        }
    }

    private <T extends ObjectType> MetadataType getCurrentMetadata(LensElementContext<T> elementContext) {
        PrismObject<T> objectCurrent = elementContext.getObjectCurrent();
        return objectCurrent != null ? objectCurrent.asObjectable().getMetadata() : null;
    }

    private <AH extends AssignmentHolderType, T extends ObjectType> void applyAssignmentMetadataObject(
            LensContext<AH> context, PrismObject<T> objectToAdd, XMLGregorianCalendar now, Task task) {

        PrismContainer<AssignmentType> assignmentContainer = objectToAdd.findContainer(AssignmentHolderType.F_ASSIGNMENT);
        if (assignmentContainer != null) {
            for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentContainer.getValues()) {
                applyAssignmentValueMetadataAdd(context, assignmentContainerValue, "ADD", now, task);
            }
        }
    }

    private <AH extends AssignmentHolderType> void applyAssignmentMetadataDelta(
            LensContext<AH> context, ObjectDelta<AH> objectDelta,
            XMLGregorianCalendar now, Task task) throws SchemaException {

        assert objectDelta.isModify();

        // see also ApprovalMetadataHelper.addAssignmentApprovalMetadataOnObjectModify
        Set<Long> processedIds = new HashSet<>();
        List<ItemDelta<?,?>> assignmentMetadataDeltas = new ArrayList<>();
        for (ItemDelta<?,?> itemDelta: objectDelta.getModifications()) {
            ItemPath deltaPath = itemDelta.getPath();
            CompareResult comparison = deltaPath.compareComplex(SchemaConstants.PATH_ASSIGNMENT);
            if (comparison == EQUIVALENT) {
                // whole assignment is being added/replaced (or deleted but we are not interested in that)
                //noinspection unchecked
                ContainerDelta<AssignmentType> assignmentDelta = (ContainerDelta<AssignmentType>)itemDelta;
                if (assignmentDelta.getValuesToAdd() != null) {
                    for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentDelta.getValuesToAdd()) {
                        applyAssignmentValueMetadataAdd(context, assignmentContainerValue, "MOD/add", now, task);
                    }
                }
                if (assignmentDelta.getValuesToReplace() != null) {
                    for (PrismContainerValue<AssignmentType> assignmentContainerValue: assignmentDelta.getValuesToReplace()) {
                        applyAssignmentValueMetadataAdd(context, assignmentContainerValue, "MOD/replace", now, task);
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
                    assignmentMetadataDeltas.addAll(
                            createModifyMetadataDeltas(context,
                                    getCurrentAssignmentMetadata(context, id),
                                    ItemPath.create(FocusType.F_ASSIGNMENT, id, AssignmentType.F_METADATA),
                                    context.getFocusClass(), now, task));
                }
            }
        }
        ItemDeltaCollectionsUtil.mergeAll(objectDelta.getModifications(), assignmentMetadataDeltas);
    }

    private <AH extends AssignmentHolderType> MetadataType getCurrentAssignmentMetadata(LensContext<AH> context, Long id) {
        LensFocusContext<AH> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return null;
        }
        PrismObject<AH> focusCurrent = focusContext.getObjectCurrent();
        if (focusCurrent == null) {
            return null;
        }
        PrismContainer<MetadataType> metadataContainer = focusCurrent.findContainer(
                ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, id, AssignmentType.F_METADATA));
        return metadataContainer != null && metadataContainer.hasAnyValue() ?
                asContainerable(metadataContainer.getValue()) : null;
    }

    private <AH extends AssignmentHolderType> void applyAssignmentValueMetadataAdd(
            LensContext<AH> context, PrismContainerValue<AssignmentType> assignmentContainerValue, String desc,
            XMLGregorianCalendar now, Task task) {

        AssignmentType assignment = assignmentContainerValue.asContainerable();
        MetadataType metadata = getOrCreateMetadata(assignment);

        transplantRequestMetadata(context, metadata);
        ActivationType activation = setAssignmentEffectiveStatus(assignment);
        applyCreateMetadata(context, metadata, now, task);

        LOGGER.trace("Adding operational data {} to assignment cval ({}):\nMETADATA:\n{}\nACTIVATION:\n{}",
                metadata, desc, assignmentContainerValue.debugDumpLazily(1),
                activation.asPrismContainerValue().debugDumpLazily(1));
    }

    /**
     * TODO why we do this here and not in ActivationProcessor or something like that?
     */
    @NotNull
    private ActivationType setAssignmentEffectiveStatus(AssignmentType assignment) {
        // This applies the effective status only to assignments that are completely new (whole container is added/replaced)
        // The effectiveStatus of existing assignments is processed in FocusProcessor.processAssignmentActivation()
        // We cannot process that here. Because this code is not even triggered when there is no delta. So recompute will not work.
        ActivationType activation = assignment.getActivation();
        ActivationStatusType effectiveStatus =
                activationComputer.getEffectiveStatus(assignment.getLifecycleState(), activation, null);
        if (activation == null) {
            activation = new ActivationType();
            assignment.setActivation(activation);
        }
        activation.setEffectiveStatus(effectiveStatus);
        return activation;
    }

    private @NotNull MetadataType getOrCreateMetadata(AssignmentType assignment) {
        MetadataType metadata = assignment.getMetadata();
        if (metadata != null) {
            return metadata;
        } else {
            MetadataType newMetadata = new MetadataType();
            assignment.setMetadata(newMetadata);
            return newMetadata;
        }
    }

    public <F extends ObjectType> MetadataType createCreateMetadata(LensContext<F> context, XMLGregorianCalendar now, Task task) {
        MetadataType metaData = new MetadataType();
        applyCreateMetadata(context, metaData, now, task);
        return metaData;
    }

    private <F extends ObjectType> void applyCreateMetadata(
            LensContext<F> context, MetadataType metaData, XMLGregorianCalendar now, Task task) {
        String channel = LensUtil.getChannel(context, task);
        metaData.setCreateChannel(channel);
        metaData.setCreateTimestamp(now);
        if (task.getOwnerRef() != null) {
            metaData.setCreatorRef(ObjectTypeUtil.createObjectRefCopy(task.getOwnerRef()));
        }
        metaData.setCreateTaskRef(createRootTaskRef(task));
    }

    private <F extends ObjectType> void applyCreateApprovalMetadata(LensContext<F> context, MetadataType metadata) {
        metadata.getCreateApproverRef().addAll(CloneUtil.cloneCollectionMembers(context.getOperationApprovedBy()));
        metadata.getCreateApprovalComment().addAll(context.getOperationApproverComments());
    }

    private List<ItemDelta<?, ?>> createModifyApprovalMetadataDeltas(
            Class<? extends ObjectType> objectTypeClass, LensContext<? extends ObjectType> context) throws SchemaException {
        List<PrismReferenceValue> approverReferenceValues = new ArrayList<>();
        for (ObjectReferenceType approverRef : context.getOperationApprovedBy()) {
            approverReferenceValues.add(approverRef.asReferenceValue().clone());
        }
        return prismContext.deltaFor(objectTypeClass)
                .item(ObjectType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF).replace(approverReferenceValues)
                .item(ObjectType.F_METADATA, MetadataType.F_MODIFY_APPROVAL_COMMENT).replaceRealValues(context.getOperationApproverComments())
                .asItemDeltas();
    }

    /**
     * Creates deltas for modification-related metadata (except for modification approval metadata).
     * But also migrates creation channel, if needed.
     *
     * @param existingMetadata Existing metadata bean, if known. It is used to execute the migration.
     */
    public <F extends ObjectType, T extends ObjectType> Collection<ItemDelta<?,?>> createModifyMetadataDeltas(
            LensContext<F> context, MetadataType existingMetadata,
            ItemPath metadataPath, Class<T> objectType, XMLGregorianCalendar now, Task task) throws SchemaException {

        List<ItemDelta<?, ?>> deltas = new ArrayList<>(prismContext.deltaFor(objectType)
                .item(metadataPath.append(MetadataType.F_MODIFY_CHANNEL)).replace(LensUtil.getChannel(context, task))
                .item(metadataPath.append(MetadataType.F_MODIFY_TIMESTAMP)).replace(now)
                .item(metadataPath.append(MetadataType.F_MODIFIER_REF))
                    .replace(ObjectTypeUtil.createObjectRefCopy(task.getOwnerRef()))
                .item(metadataPath.append(MetadataType.F_MODIFY_TASK_REF))
                    .replace(createRootTaskRef(task))
                .asItemDeltas());
        if (existingMetadata != null) {
            createMigrationDelta(existingMetadata, metadataPath, objectType, deltas);
        }
        return deltas;
    }

    private <T extends ObjectType> void createMigrationDelta(MetadataType existingMetadata, ItemPath metadataPath,
            Class<T> objectType, List<ItemDelta<?, ?>> deltas) throws SchemaException {
        Channel.Migration migration = Channel.findMigration(existingMetadata.getCreateChannel());
        if (migration != null && migration.isNeeded()) {
            deltas.add(
                    prismContext.deltaFor(objectType)
                            .item(metadataPath.append(MetadataType.F_CREATE_CHANNEL)).replace(migration.getNewUri())
                            .asItemDelta());
        }
    }

    /**
     * Returns a reference suitable for use as create/modifyTaskRef: a reference to the root of the task tree.
     *
     * (We assume that if the task is a part of a task tree, it is always a {@link RunningTask}.)
     */
    private static @Nullable ObjectReferenceType createRootTaskRef(@NotNull Task task) {
        if (task instanceof RunningTask) {
            return ((RunningTask) task).getRootTask().getSelfReference();
        } else if (task.isPersistent()) {
            // Actually this should not occur in real life. If a task is persistent, it should be a RunningTask.
            return task.getSelfReference();
        } else {
            return null;
        }
    }
}
