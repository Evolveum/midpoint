/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import static com.evolveum.midpoint.prism.delta.ObjectDelta.isAdd;
import static com.evolveum.midpoint.prism.delta.ObjectDelta.isModify;
import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.EQUIVALENT;
import static com.evolveum.midpoint.prism.path.ItemPath.CompareResult.SUPERPATH;
import static com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil.*;
import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.ActivationComputer;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.InfraItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPath.CompareResult;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    @Autowired private Clock clock;

    //region Object ADD operation
    /**
     * Sets object and assignment metadata on object ADD operation.
     * We assume that assignments already have the respective metadata values, see
     * {@link #applyMetadataOnAssignmentAddOp(LensContext, AssignmentType, String, XMLGregorianCalendar, Task)}.
     */
    public <T extends ObjectType> void applyMetadataOnObjectAddOp(
            LensContext<?> context, PrismObject<T> objectToAdd, XMLGregorianCalendar now, Task task)
            throws SchemaException, ConfigurationException {

        var objectBean = objectToAdd.asObjectable();
        ValueMetadataType metadata = ValueMetadataTypeUtil.getOrCreateMetadata(objectBean);

        setRequestMetadataOnAddOp(context, metadata);
        setStorageMetadataOnAddOp(context, metadata, now, task);
        setProcessMetadataOnAddOp(context, metadata);
        setProvisioningMetadataOnAddOp(context, metadata, now, objectBean instanceof ShadowType);

        if (objectBean instanceof AssignmentHolderType assignmentHolder) {
            for (var assignment : assignmentHolder.getAssignment()) {
                applyMetadataOnAssignmentAddOp(context, assignment, "ADD", now, task);
            }
        }
    }

    private void setProcessMetadataOnAddOp(LensContext<?> context, ValueMetadataType metadata) {
        var process = getOrCreateProcessMetadata(metadata);
        process.getCreateApproverRef().addAll(
                CloneUtil.cloneCollectionMembers(context.getOperationApprovedBy()));
        process.getCreateApprovalComment().addAll(
                context.getOperationApproverComments());
    }

    private void setProvisioningMetadataOnAddOp(
            LensContext<?> context, ValueMetadataType metadata, XMLGregorianCalendar now, boolean isProjection)
            throws SchemaException, ConfigurationException {
        if (!isProjection && context.hasProjectionChange()) {
            getOrCreateProvisioningMetadata(metadata).setLastProvisioningTimestamp(now);
        }
    }
    //endregion

    //region Object MODIFY operation
    /**
     * Creates deltas for object and assignment metadata on object MODIFY operation.
     * We assume that assignments already have the respective metadata values, see
     * {@link #applyMetadataOnAssignmentAddOp(LensContext, AssignmentType, String, XMLGregorianCalendar, Task)}.
     */
    public <T extends ObjectType> void applyMetadataOnObjectModifyOp(
            @NotNull ObjectDelta<T> objectDelta,
            @NotNull LensElementContext<T> elementContext,
            @NotNull XMLGregorianCalendar now,
            @NotNull Task task,
            @NotNull LensContext<?> context) throws SchemaException, ConfigurationException {
        assert objectDelta.isModify();

        var objectCurrent = elementContext.getObjectCurrentRequired().asObjectable();
        var isProjection = elementContext instanceof LensProjectionContext;
        ItemDeltaCollectionsUtil.mergeAll(
                objectDelta.getModifications(),
                createObjectModificationRelatedMetadataDeltas(context, isProjection, objectCurrent, now, task));

        if (objectCurrent instanceof AssignmentHolderType assignmentHolder) {
            applyAssignmentMetadataOnObjectModifyOp(context, assignmentHolder, objectDelta, now, task);
        }
    }

    /**
     * Creates deltas for object-modification-related metadata.
     * The exact form of these deltas (add/replace/...) depends on the existence of relevant items/values in the current object.
     */
    private Collection<ItemDelta<?, ?>> createObjectModificationRelatedMetadataDeltas(
            LensContext<?> context, boolean isProjection, @NotNull ObjectType currentObject, XMLGregorianCalendar now, Task task)
            throws SchemaException, ConfigurationException {
        var storageMetadata = ObjectModificationStorageMetadata.create(context, now, task);
        var processMetadata = ObjectModificationProcessMetadata.create(context);
        var provisioningMetadata = ObjectModificationProvisioningMetadata.create(context, now, isProjection);
        var metadata = ValueMetadataTypeUtil.getMetadata(currentObject);
        if (metadata == null) {
            return prismContext.deltaFor(currentObject.getClass())
                    .item(InfraItemName.METADATA)
                    .add(new ValueMetadataType()
                            .storage(storageMetadata.toBean())
                            .process(processMetadata.toBean())
                            .provisioning(provisioningMetadata.toBean()))
                    .asItemDeltas();
        } else {
            ItemPath metadataValuePath = ValueMetadataTypeUtil.getPathOf(metadata);
            var rv = new ArrayList<ItemDelta<?, ?>>();
            rv.addAll(
                    storageMetadata.toItemDeltas(
                            metadataValuePath.append(ValueMetadataType.F_STORAGE),
                            currentObject.getClass()));
            rv.addAll(
                    processMetadata.toItemDeltas(
                            metadataValuePath.append(ValueMetadataType.F_PROCESS),
                            currentObject.getClass()));
            rv.addAll(
                    provisioningMetadata.toItemDeltas(
                            metadataValuePath.append(ValueMetadataType.F_PROVISIONING),
                            currentObject.getClass()));
            return rv;
        }
    }

    /**
     * Creates deltas for object-modification-related STORAGE metadata.
     */
    public Collection<ItemDelta<?, ?>> createObjectModificationRelatedStorageMetadataDeltas(
            LensContext<?> context, @NotNull ObjectType currentObject, XMLGregorianCalendar now, Task task)
            throws SchemaException {
        var storageMetadata = ObjectModificationStorageMetadata.create(context, now, task);
        var metadata = ValueMetadataTypeUtil.getMetadata(currentObject);
        if (metadata == null) {
            return prismContext.deltaFor(currentObject.getClass())
                    .item(InfraItemName.METADATA)
                    .add(new ValueMetadataType()
                            .storage(storageMetadata.toBean()))
                    .asItemDeltas();
        } else {
            return storageMetadata.toItemDeltas(
                    ValueMetadataTypeUtil.getPathOf(metadata).append(ValueMetadataType.F_STORAGE),
                    currentObject.getClass());
        }
    }
    //endregion

    //region Objects - other operations
    public Collection<? extends ItemDelta<?, ?>> createObjectCertificationMetadataDeltas(
            @NotNull ObjectType object,
            CertificationProcessMetadata certificationProcessMetadata)
            throws SchemaException {
        var metadata = ValueMetadataTypeUtil.getMetadata(object);
        if (metadata == null) {
            return prismContext.deltaFor(object.getClass())
                    .item(InfraItemName.METADATA)
                    .add(new ValueMetadataType().process(certificationProcessMetadata.toBean()))
                    .asItemDeltas();
        } else {
            return certificationProcessMetadata.toItemDeltas(
                    ValueMetadataTypeUtil.getPathOf(metadata).append(ValueMetadataType.F_PROCESS),
                    object.getClass());
        }
    }

    //endregion

    //region Assignments
    /**
     * Adds provenance metadata to assignments being explicitly added (via primary delta). This is necessary so that they
     * can be distinguished from assignments provided by mappings. Typically used on operation start.
     */
    public void addExternalAssignmentProvenance(LensContext<?> context, Task task)
            throws SchemaException {
        var focusContext = context.getFocusContext();
        if (focusContext == null) {
            return;
        }
        var primaryDelta = focusContext.getPrimaryDelta();
        if (isAdd(primaryDelta)) {
            // These conditions are here to avoid unnecessary cloning of the primary delta in modifyPrimaryDelta method.
            if (primaryDelta.getObjectableToAdd() instanceof AssignmentHolderType assignmentHolder
                    && !assignmentHolder.getAssignment().isEmpty()) {
                focusContext.modifyPrimaryDelta(delta -> addExternalAssignmentProvenanceForAddDelta(context, delta, task));
            }
        } else if (isModify(primaryDelta)) {
            // These conditions are here to avoid unnecessary cloning of the primary delta in modifyPrimaryDelta method.
            if (ItemDeltaCollectionsUtil.findItemDelta(
                    primaryDelta.getModifications(), AssignmentHolderType.F_ASSIGNMENT, ContainerDelta.class, true) != null) {
                focusContext.modifyPrimaryDelta(delta -> addExternalAssignmentProvenanceForModifyDelta(context, delta, task));
            }
        }
    }

    private void addExternalAssignmentProvenanceForAddDelta(LensContext<?> context, ObjectDelta<?> delta, Task task)
            throws SchemaException {
        for (var assignment : ((AssignmentHolderType) delta.getObjectableToAdd()).getAssignment()) {
            addExternalAssignmentProvenance(context, assignment.asPrismContainerValue(), task);
        }
    }

    private void addExternalAssignmentProvenanceForModifyDelta(LensContext<?> context, ObjectDelta<?> delta, Task task)
            throws SchemaException {
        for (var itemDelta : delta.getModifications()) {
            if (AssignmentHolderType.F_ASSIGNMENT.equivalent(itemDelta.getPath())) {
                var assignmentDelta = (ContainerDelta<?>) itemDelta;
                for (var assignmentContainerValue : emptyIfNull(assignmentDelta.getValuesToAdd())) {
                    addExternalAssignmentProvenance(context, assignmentContainerValue, task);
                }
                for (var assignmentContainerValue: emptyIfNull(assignmentDelta.getValuesToReplace())) {
                    addExternalAssignmentProvenance(context, assignmentContainerValue, task);
                }
            }
        }
    }

    private void addExternalAssignmentProvenance(LensContext<?> context, PrismContainerValue<?> value, Task task)
            throws SchemaException {
        if (value.hasValueMetadata()) {
            return; // Supplied by the caller. We hope they know what they are doing.
        }
        ValueMetadataType metadata = new ValueMetadataType()
                .provenance(new ProvenanceMetadataType()
                        .acquisition(new ProvenanceAcquisitionType()
                                //.originRef(SystemObjectsType.ORIGIN_USER_ENTRY.value(), ServiceType.COMPLEX_TYPE)
                                .actorRef(ObjectTypeUtil.createObjectRefCopy(task.getOwnerRef()))
                                .channel(LensUtil.getChannel(context, task))
                                .timestamp(clock.currentTimeXMLGregorianCalendar()))
                );
        value.getValueMetadata().addMetadataValue(metadata.asPrismContainerValue());
    }

    /**
     * Applies metadata to an assignment that is being added (either as part of object ADD operation, or by object MODIFY
     * operation). We assume that there are already existing metadata, generated either by mapping(s), by model API caller,
     * or at the beginning of the clockwork operation. We only set the relevant metadata there.
     */
    private void applyMetadataOnAssignmentAddOp(
            LensContext<?> context, AssignmentType assignment, String desc,
            XMLGregorianCalendar now, Task task) {

        var allMetadata = assignment.asPrismContainerValue().getValueMetadata();
        if (allMetadata.hasNoValues()) {
            LOGGER.warn("No metadata in assignment ({})", desc);
            return;
        }

        ActivationType activation = setAssignmentEffectiveStatus(assignment);

        // TODO is it OK that we add the request/create metadata to all the values?
        for (var metadata : allMetadata.getRealValues(ValueMetadataType.class)) {
            setRequestMetadataOnAddOp(context, metadata);
            setStorageMetadataOnAddOp(context, metadata, now, task);
            // note that assignment approval metadata are handled by ApprovalMetadataHelper in workflow-impl (WHY?)
        }

        LOGGER.trace("Added operational data to assignment ({}):\nCurrent metadata:\n{}\nCurrent activation:\n{}",
                desc, allMetadata.debugDumpLazily(1), activation.debugDumpLazily(1));
    }

    private void applyAssignmentMetadataOnObjectModifyOp(
            LensContext<?> context, @NotNull AssignmentHolderType objectCurrent, ObjectDelta<?> objectDelta,
            XMLGregorianCalendar now, Task task) throws SchemaException {

        assert objectDelta.isModify();

        // see also ApprovalMetadataHelper.addAssignmentApprovalMetadataOnObjectModify
        Set<Long> processedAssignmentIds = new HashSet<>(); // an assignment can be mentioned by multiple item deltas
        for (var itemDelta : List.copyOf(objectDelta.getModifications())) { // copying because of concurrent modifications
            ItemPath deltaPath = itemDelta.getPath();
            CompareResult comparison = deltaPath.compareComplex(FocusType.F_ASSIGNMENT);
            if (comparison == EQUIVALENT) {
                // whole assignment is being added/replaced (or deleted but we are not interested in that)
                //noinspection unchecked
                ContainerDelta<AssignmentType> assignmentDelta = (ContainerDelta<AssignmentType>)itemDelta;
                for (var assignmentContainerValue : emptyIfNull(assignmentDelta.getValuesToAdd())) {
                    applyMetadataOnAssignmentAddOp(
                            context, assignmentContainerValue.asContainerable(), "MOD/add", now, task);
                }
                for (var assignmentContainerValue: emptyIfNull(assignmentDelta.getValuesToReplace())) {
                    applyMetadataOnAssignmentAddOp(
                            context, assignmentContainerValue.asContainerable(), "MOD/replace", now, task);
                }
            } else if (comparison == SUPERPATH) {
                Object secondSegment = deltaPath.rest().first();
                Long id = ItemPath.isId(secondSegment) ? ItemPath.toId(secondSegment) : null;
                if (id == null) {
                    throw new IllegalStateException(
                            "Assignment modification contains no assignment ID. Offending path = " + deltaPath);
                }
                if (processedAssignmentIds.add(id)) {
                    var assignment = ObjectTypeUtil.getAssignmentRequired(objectCurrent, id);
                    ItemDeltaCollectionsUtil.mergeAll(
                            objectDelta.getModifications(),
                            createAssignmentModificationRelatedMetadataDeltas(context, assignment, now, task));
                } else {
                    // this assignment was already processed
                }
            }
        }
    }

    private Collection<ItemDelta<?, ?>> createAssignmentModificationRelatedMetadataDeltas(
            LensContext<?> context, AssignmentType assignment, XMLGregorianCalendar now, Task task) throws SchemaException {
        var metadataPath = ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, assignment.getId(), InfraItemName.METADATA);
        var storageMetadata = ObjectModificationStorageMetadata.create(context, now, task);
        var allMetadata = assignment.asPrismContainerValue().getValueMetadata();
        if (allMetadata.hasNoValues()) {
            LOGGER.debug("No metadata in assignment being modified (id {}), we'll provide some", assignment.getId());
            return prismContext.deltaFor(context.getFocusClass())
                    .item(metadataPath)
                    .add(new ValueMetadataType()
                            .storage(storageMetadata.toBean()))
                    .asItemDeltas();
        }

        var deltas = new ArrayList<ItemDelta<?, ?>>();
        // TODO is it OK that we add the request/create metadata to all the values?
        for (var metadata : allMetadata.getRealValues(ValueMetadataType.class)) {
            deltas.addAll(
                    storageMetadata.toItemDeltas(
                            ItemPath.create(metadataPath, metadata.getId(), ValueMetadataType.F_STORAGE),
                            context.getFocusClass()));
            // note that assignment approval metadata are handled by ApprovalMetadataHelper in workflow-impl (WHY?)
        }
        return deltas;
    }

    public void addAssignmentCreationApprovalMetadata(
            AssignmentType assignment, Collection<ObjectReferenceType> approvedBy, Collection<String> comments)
            throws SchemaException {
        var metadata = assignment.asPrismContainerValue().getValueMetadata();
        if (metadata.hasNoValues()) {
            metadata.addMetadataValue(
                    minimalAssignmentMetadata().asPrismContainerValue());
        }
        for (var metadataBean : metadata.getRealValues(ValueMetadataType.class)) {
            var processMetadata = getOrCreateProcessMetadata(metadataBean);
            processMetadata.getCreateApproverRef().clear();
            processMetadata.getCreateApproverRef().addAll(CloneUtil.cloneCollectionMembers(approvedBy));
            processMetadata.getCreateApprovalComment().clear();
            processMetadata.getCreateApprovalComment().addAll(comments);
        }
    }

    /** This is the minimal information. Later, we can add more (actor, channel, timestamp). */
    private static ValueMetadataType minimalAssignmentMetadata() {
        return new ValueMetadataType();
//                .provenance(new ProvenanceMetadataType()
//                        .acquisition(new ProvenanceAcquisitionType()
//                                .originRef(SystemObjectsType.ORIGIN_USER_ENTRY.value(), ServiceType.COMPLEX_TYPE)));
    }

    public Collection<ItemDelta<?, ?>> createAssignmentModificationApprovalMetadata(
            AssignmentHolderType focus, long assignmentId, Collection<ObjectReferenceType> approvedBy, Collection<String> comments)
            throws SchemaException {
        var assignmentPcv = (PrismContainerValue<?>) MiscUtil.stateNonNull(
                focus.asPrismObject().find(ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, assignmentId)),
                "No assignment with ID %d in %s", assignmentId, focus);
        var metadata = assignmentPcv.getValueMetadata();
        if (metadata.hasNoValues()) {
            var processMetadata = new ProcessMetadataType();
            processMetadata.getModifyApproverRef().addAll(CloneUtil.cloneCollectionMembers(approvedBy));
            processMetadata.getModifyApprovalComment().addAll(comments);
            return PrismContext.get().deltaFor(focus.getClass())
                    .item(FocusType.F_ASSIGNMENT, assignmentId, InfraItemName.METADATA)
                    .add(minimalAssignmentMetadata()
                            .process(processMetadata))
                    .asItemDeltas();
        }
        List<ItemDelta<?, ?>> rv = new ArrayList<>();
        for (var metadataBean : metadata.getRealValues(ValueMetadataType.class)) {
            long mdId = MiscUtil.stateNonNull(metadataBean.getId(),
                    "No metadata PCV ID in %s in assignment #%d in %s", metadataBean, assignmentId, focus);
            rv.addAll(
                    PrismContext.get().deltaFor(focus.getClass())
                            .item(FocusType.F_ASSIGNMENT, assignmentId, InfraItemName.METADATA, mdId,
                                    ValueMetadataType.F_PROCESS, ProcessMetadataType.F_MODIFY_APPROVER_REF)
                            .replaceRealValues(CloneUtil.cloneCollectionMembers(approvedBy))
                            .item(FocusType.F_ASSIGNMENT, assignmentId, InfraItemName.METADATA, mdId,
                                    ValueMetadataType.F_PROCESS, ProcessMetadataType.F_MODIFY_APPROVAL_COMMENT)
                            .replaceRealValues(comments)
                            .asItemDeltas());
        }
        return rv;
    }

    public Collection<? extends ItemDelta<?, ?>> createAssignmentCertificationMetadataDeltas(
            Class<? extends ObjectType> objectClass,
            ItemPath assignmentPcvPath,
            PrismContainerValue<?> assignmentPcv,
            CertificationProcessMetadata certificationProcessMetadata)
            throws SchemaException {
        var metadata = assignmentPcv.getValueMetadata();
        if (metadata.hasNoValues()) {
            return PrismContext.get().deltaFor(objectClass)
                    .item(assignmentPcvPath, InfraItemName.METADATA)
                    .add(minimalAssignmentMetadata()
                            .process(certificationProcessMetadata.toBean()))
                    .asItemDeltas();
        }
        List<ItemDelta<?, ?>> rv = new ArrayList<>();
        for (var metadataBean : metadata.getRealValues(ValueMetadataType.class)) {
            long mdId = MiscUtil.stateNonNull(metadataBean.getId(),
                    "No metadata PCV ID in %s in %s", metadataBean, assignmentPcvPath);
            rv.addAll(
                    certificationProcessMetadata.toItemDeltas(
                            assignmentPcvPath.append(InfraItemName.METADATA, mdId, ValueMetadataType.F_PROCESS),
                            objectClass));
        }
        return rv;
    }
    //endregion

    //region Support

    private <F extends ObjectType> void setStorageMetadataOnAddOp(
            LensContext<F> context, ValueMetadataType metadata, XMLGregorianCalendar now, Task task) {
        var storage = getOrCreateStorageMetadata(metadata);
        storage.setCreateChannel(LensUtil.getChannel(context, task));
        storage.setCreateTimestamp(now);
        if (task.getOwnerRef() != null) {
            storage.setCreatorRef(ObjectTypeUtil.createObjectRefCopy(task.getOwnerRef()));
        }
        storage.setCreateTaskRef(createRootTaskRef(task));
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

    /** "Transplants" previously stored request metadata from model context into target metadata container. */
    private <F extends ObjectType> void setRequestMetadataOnAddOp(LensContext<F> context, ValueMetadataType targetMetadata) {
        var requestMetadata = context.getRequestMetadata();
        if (requestMetadata != null) {
            var process = getOrCreateProcessMetadata(targetMetadata);
            process.requestTimestamp(requestMetadata.requestTimestamp());
            process.setRequestorRef(CloneUtil.clone(requestMetadata.requestorRef()));
        }
        OperationBusinessContextType businessContext = context.getRequestBusinessContext();
        if (businessContext != null) {
            var process = getOrCreateProcessMetadata(targetMetadata);
            process.setRequestorComment(businessContext.getComment());
        }
    }
    //endregion

    //region Unsorted
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
    //endregion

    //region Credentials
    /** Currently used for the credentials. */
    public <F extends ObjectType> ValueMetadataType createCreateMetadata(
            LensContext<F> context, XMLGregorianCalendar now, Task task) {
        ValueMetadataType metadata = new ValueMetadataType();
        // TODO apply provenance metadata
        setStorageMetadataOnAddOp(context, metadata, now, task);
        return metadata;
    }

    public <T extends ObjectType> Collection<ItemDelta<?,?>> createCredentialsModificationRelatedStorageMetadataDeltas(
            @NotNull LensContext<?> context,
            @NotNull ItemPath credentialContainerPath,
            @Nullable AbstractCredentialType currentObject,
            @NotNull Class<T> objectType,
            @NotNull XMLGregorianCalendar now,
            @NotNull Task task)
            throws SchemaException {
        var storageMetadata = ObjectModificationStorageMetadata.create(context, now, task);
        var metadata = currentObject != null ? ValueMetadataTypeUtil.getMetadata(currentObject) : null;
        if (metadata == null) {
            return prismContext.deltaFor(objectType)
                    .item(credentialContainerPath.append(InfraItemName.METADATA))
                    .add(new ValueMetadataType()
                            .storage(storageMetadata.toBean()))
                    .asItemDeltas();
        } else {
            return storageMetadata.toItemDeltas(
                    credentialContainerPath
                            .append(ValueMetadataTypeUtil.getPathOf(metadata))
                            .append(ValueMetadataType.F_STORAGE),
                    objectType);
        }
    }
    //endregion

    /** All values are parent-less here, to be directly insertable into beans and deltas. */
    private record ObjectModificationStorageMetadata(
            String modifyChannel,
            XMLGregorianCalendar modifyTimestamp,
            ObjectReferenceType modifierRef,
            ObjectReferenceType modifyTaskRef) {

        static ObjectModificationStorageMetadata create(LensContext<?> context, XMLGregorianCalendar now, Task task) {
            return new ObjectModificationStorageMetadata(
                    LensUtil.getChannel(context, task),
                    now,
                    ObjectTypeUtil.createObjectRefCopy(task.getOwnerRef()),
                    createRootTaskRef(task));
        }

        private StorageMetadataType toBean() {
            return new StorageMetadataType()
                    .modifyChannel(modifyChannel)
                    .modifyTimestamp(modifyTimestamp)
                    .modifierRef(modifierRef)
                    .modifyTaskRef(modifyTaskRef);
        }

        private Collection<ItemDelta<?, ?>> toItemDeltas(
                ItemPath storageMetadataPath, Class<? extends ObjectType> objectTypeClass) throws SchemaException {
            return PrismContext.get().deltaFor(objectTypeClass)
                    .item(storageMetadataPath.append(StorageMetadataType.F_MODIFY_CHANNEL)).replace(modifyChannel)
                    .item(storageMetadataPath.append(StorageMetadataType.F_MODIFY_TIMESTAMP)).replace(modifyTimestamp)
                    .item(storageMetadataPath.append(StorageMetadataType.F_MODIFIER_REF)).replace(modifierRef)
                    .item(storageMetadataPath.append(StorageMetadataType.F_MODIFY_TASK_REF)).replace(modifyTaskRef)
                    .asItemDeltas();
        }
    }

    /** All values are parent-less here, to be directly insertable into beans and deltas. */
    private record ObjectModificationProcessMetadata(
            Collection<ObjectReferenceType> modifyApproverRefCollection,
            Collection<String> approvalComments) {

        static ObjectModificationProcessMetadata create(LensContext<?> context) {
            return new ObjectModificationProcessMetadata(
                    CloneUtil.cloneCollectionMembers(context.getOperationApprovedBy()),
                    context.getOperationApproverComments());
        }

        @Nullable ProcessMetadataType toBean() {
            var process = new ProcessMetadataType();
            process.getModifyApproverRef().addAll(modifyApproverRefCollection);
            process.getModifyApprovalComment().addAll(approvalComments);
            return !process.asPrismContainerValue().hasNoItems() ? process : null;
        }

        private Collection<ItemDelta<?, ?>> toItemDeltas(
                ItemPath processMetadataPath, Class<? extends ObjectType> objectTypeClass)
                throws SchemaException {
            return PrismContext.get().deltaFor(objectTypeClass)
                    .item(processMetadataPath.append(ProcessMetadataType.F_MODIFY_APPROVER_REF))
                    .replaceRealValues(modifyApproverRefCollection)
                    .item(processMetadataPath.append(MetadataType.F_MODIFY_APPROVAL_COMMENT))
                    .replaceRealValues(approvalComments)
                    .asItemDeltas();
        }
    }

    /** All values are parent-less here, to be directly insertable into beans and deltas. */
    public record CertificationProcessMetadata(
            XMLGregorianCalendar certificationFinishedTimestamp,
            String outcome,
            @NotNull Collection<ObjectReferenceType> certifierRefs,
            @NotNull Collection<String> comments) {


        ProcessMetadataType toBean() {
            var processMetadata = new ProcessMetadataType()
                    .certificationFinishedTimestamp(certificationFinishedTimestamp)
                    .certificationOutcome(outcome);
            processMetadata.getCertifierRef().addAll(CloneUtil.cloneCollectionMembers(certifierRefs));
            processMetadata.getCertifierComment().addAll(comments);
            return processMetadata;
        }

        private Collection<ItemDelta<?, ?>> toItemDeltas(
                ItemPath processMetadataPath, Class<? extends ObjectType> objectTypeClass)
                throws SchemaException {
            return PrismContext.get().deltaFor(objectTypeClass)
                    .item(processMetadataPath.append(ProcessMetadataType.F_CERTIFICATION_FINISHED_TIMESTAMP))
                    .replace(certificationFinishedTimestamp)
                    .item(processMetadataPath.append(ProcessMetadataType.F_CERTIFICATION_OUTCOME))
                    .replace(outcome)
                    .item(processMetadataPath.append(ProcessMetadataType.F_CERTIFIER_REF))
                    .replaceRealValues(certifierRefs)
                    .item(processMetadataPath.append(ProcessMetadataType.F_CERTIFIER_COMMENT))
                    .replaceRealValues(comments)
                    .asItemDeltas();
        }
    }

    private record ObjectModificationProvisioningMetadata(
            @Nullable XMLGregorianCalendar lastProvisioningTimestamp) {

        static ObjectModificationProvisioningMetadata create(
                LensContext<?> context, XMLGregorianCalendar now, boolean isProjection)
                throws SchemaException, ConfigurationException {
            return new ObjectModificationProvisioningMetadata(
                    !isProjection && context.hasProjectionChange() ? now : null);
        }

        @Nullable ProvisioningMetadataType toBean() {
            if (lastProvisioningTimestamp != null) {
                return new ProvisioningMetadataType().lastProvisioningTimestamp(lastProvisioningTimestamp);
            } else {
                return null;
            }
        }

        private Collection<ItemDelta<?, ?>> toItemDeltas(
                ItemPath provisioningMetadataPath, Class<? extends ObjectType> objectTypeClass)
                throws SchemaException {
            // This is different from the other kinds of data: no information means no change
            if (lastProvisioningTimestamp != null) {
                return PrismContext.get().deltaFor(objectTypeClass)
                        .item(provisioningMetadataPath.append(ProvisioningMetadataType.F_LAST_PROVISIONING_TIMESTAMP))
                        .replace(lastProvisioningTimestamp)
                        .asItemDeltas();
            } else {
                return List.of();
            }
        }
    }
}
