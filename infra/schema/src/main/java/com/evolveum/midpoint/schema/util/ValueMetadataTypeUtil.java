/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.InfraItemName;

import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.util.MiscUtil.*;

public class ValueMetadataTypeUtil {

    private static final Function<ObjectReferenceType, ObjectReferenceType> OBJECT_REFERENCE_COPY = (o) -> {
        if (o == null) {
            return null;
        }
        return (ObjectReferenceType) o.asReferenceValue().clone().asReferencable();
    };


    public static @NotNull StorageMetadataType getOrCreateStorageMetadata(@NotNull PrismObject<? extends ObjectType> object) {
        return getOrCreateStorageMetadata(
                getOrCreateMetadata(object.asObjectable()));
    }

    public static @NotNull StorageMetadataType getOrCreateStorageMetadata(@NotNull ObjectType object) {
        return getOrCreateStorageMetadata(
                getOrCreateMetadata(object));
    }

    public static @NotNull StorageMetadataType getOrCreateStorageMetadata(
            @NotNull Containerable holder, @NotNull ProvenanceMetadataType provenance) {
        return getOrCreateStorageMetadata(
                getOrCreateMetadata(holder, provenance));
    }

    public static @NotNull StorageMetadataType getOrCreateStorageMetadata(@NotNull ValueMetadataType metadata) {
        var existing = metadata.getStorage();
        if (existing != null) {
            return existing;
        } else {
            var newValue = new StorageMetadataType();
            metadata.setStorage(newValue);
            return newValue;
        }
    }

    public static @NotNull ProcessMetadataType getOrCreateProcessMetadata(@NotNull ValueMetadataType metadata) {
        var existing = metadata.getProcess();
        if (existing != null) {
            return existing;
        } else {
            var newValue = new ProcessMetadataType();
            metadata.setProcess(newValue);
            return newValue;
        }
    }

    public static @NotNull ProvenanceMetadataType getOrCreateProvenanceMetadata(@NotNull ValueMetadataType metadata) {
        var existing = metadata.getProvenance();
        if (existing != null) {
            return existing;
        } else {
            var newValue = new ProvenanceMetadataType();
            metadata.setProvenance(newValue);
            return newValue;
        }
    }

    public static @NotNull ProvisioningMetadataType getOrCreateProvisioningMetadata(@NotNull ValueMetadataType metadata) {
        var existing = metadata.getProvisioning();
        if (existing != null) {
            return existing;
        } else {
            var newValue = new ProvisioningMetadataType();
            metadata.setProvisioning(newValue);
            return newValue;
        }
    }

    public static @NotNull MappingSpecificationType getOrCreateMappingSpecification(@NotNull ProvenanceMetadataType metadata) {
        var existing = metadata.getMappingSpecification();
        if (existing != null) {
            return existing;
        } else {
            var newValue = new MappingSpecificationType();
            metadata.setMappingSpecification(newValue);
            return newValue;
        }
    }

    public static @NotNull ValueMetadataType getOrCreateMetadata(@NotNull ObjectType object) {
        var metadata = object.asPrismContainerValue().getValueMetadata();
        if (metadata.hasNoValues()) {
            return Objects.requireNonNull(
                    metadata.createNewValue().getRealValue());
        } else if (metadata.size() == 1) {
            return metadata.getRealValue(ValueMetadataType.class);
        } else {
            throw new IllegalStateException("Multiple values for object-level value metadata in " + object);
        }
    }

    public static <T extends ObjectType> @NotNull ValueMetadataType getOrCreateMetadata(@NotNull PrismObject<T> object) {
        return getOrCreateMetadata(object.asObjectable());
    }

    /**
     * TEMPORARY/EXPERIMENTAL.
     */
    @Deprecated
    public static @NotNull ValueMetadataType getOrCreateMetadata(AssignmentType assignment) {
        var metadata = assignment.asPrismContainerValue().getValueMetadata();
        if (metadata.hasNoValues()) {
            return Objects.requireNonNull(metadata.createNewValue().getRealValue());
        } else if (metadata.size() == 1) {
            return metadata.getRealValue(ValueMetadataType.class);
        } else {
            // We assume that on assignment creation it cannot have multiple value metadata.
            // But this assumption can be wrong, e.g. if two mappings create the same assignment. TODO resolve
            throw new IllegalStateException(
                    "Multiple values for assignment-level value metadata in assignment-to-add" + assignment);
        }
    }

    public static @NotNull ValueMetadataType getOrCreateMetadata(
            @NotNull Containerable holder, @NotNull ProvenanceMetadataType provenance) {
        ValueMetadata valueMetadata = holder.asPrismContainerValue().getValueMetadata();
        return Objects.requireNonNullElseGet(
                findMatchingValue(valueMetadata, provenance),
                () -> ((ValueMetadataType) Objects.requireNonNull(valueMetadata.createNewValue().getRealValue()))
                        .provenance(provenance.clone()));
    }

    public static @Nullable ValueMetadataType findMatchingValue(ValueMetadata valueMetadata, ProvenanceMetadataType provenance) {
        return MiscUtil.extractSingleton(
                valueMetadata.getRealValues(ValueMetadataType.class).stream()
                        .filter(v -> v.getProvenance() != null && v.getProvenance().equals(provenance))
                        .toList(),
                () -> new IllegalStateException("Multiple values for metadata with provenance of " + provenance));
    }

    /**
     * TEMPORARY/EXPERIMENTAL.
     */
    @TestOnly
    public static @Nullable ValueMetadataType getMetadata(AssignmentType assignment) {
        return MiscUtil.extractSingleton(
                assignment.asPrismContainerValue().getValueMetadata().getRealValues(ValueMetadataType.class),
                () -> new IllegalStateException("Multiple values for assignment-level value metadata in " + assignment));
    }

    public static @Nullable ValueMetadataType getMetadata(Containerable holder, ProvenanceMetadataType provenance) {
        return findMatchingValue(
                holder.asPrismContainerValue().getValueMetadata(),
                provenance);
    }

    public static @NotNull ValueMetadataType getMetadataRequired(Containerable holder, ProvenanceMetadataType provenance) {
        return stateNonNull(getMetadata(holder, provenance), "No value metadata in %s for %s", holder, provenance);
    }

    public static @Nullable ValueMetadataType getMetadata(@NotNull ObjectType object) {
        return MiscUtil.extractSingleton(
                object.asPrismContainerValue().getValueMetadata().getRealValues(ValueMetadataType.class),
                () -> new IllegalStateException("Multiple values for object-level value metadata in " + object));
    }

    public static @Nullable ValueMetadataType getMetadata(@NotNull AbstractCredentialType credential) {
        return MiscUtil.extractSingleton(
                credential.asPrismContainerValue().getValueMetadata().getRealValues(ValueMetadataType.class),
                () -> new IllegalStateException("Multiple values for credential-level value metadata in " + credential));
    }

    public static @NotNull ValueMetadataType getMetadataRequired(ObjectType object) {
        return stateNonNull(getMetadata(object), "No value metadata in %s", object);
    }

    public static @Nullable StorageMetadataType getStorageMetadata(@NotNull ObjectType object) {
        var metadata = getMetadata(object);
        return metadata != null ? metadata.getStorage() : null;
    }


    public static @Nullable StorageMetadataType getStorageMetadata(
            @NotNull AssignmentType assignment, @NotNull ProvenanceMetadataType provenance) {
        var metadata = getMetadata(assignment, provenance);
        return metadata != null ? metadata.getStorage() : null;
    }

    public static @Nullable XMLGregorianCalendar getCreateTimestamp(@NotNull ObjectType object) {
        var metadata = getStorageMetadata(object);
        return metadata != null ? metadata.getCreateTimestamp() : null;
    }

    public static @Nullable XMLGregorianCalendar getCreateTimestamp(@NotNull AssignmentType assignment) {
        return getMetadataBeans(assignment).stream()
                .map(ValueMetadataType::getStorage)
                .filter(Objects::nonNull)
                .map(StorageMetadataType::getCreateTimestamp)
                .filter(Objects::nonNull)
                .min(XMLGregorianCalendar::compare)
                .orElse(null);
    }

    public static @Nullable XMLGregorianCalendar getModifyTimestamp(@NotNull ObjectType object) {
        var metadata = getStorageMetadata(object);
        return metadata != null ? metadata.getModifyTimestamp() : null;
    }

    public static @Nullable XMLGregorianCalendar getLastChangeTimestamp(@NotNull ObjectType object) {
        return getLastChangeTimestamp(
                getMetadata(object));
    }

    /** Returns modification time or creation time. */
    public static @Nullable XMLGregorianCalendar getLastChangeTimestamp(ValueMetadataType metadata) {
        if (metadata == null) {
            return null;
        }
        var storageMetadata = metadata.getStorage();
        if (storageMetadata == null) {
            return null;
        }
        XMLGregorianCalendar modifyTimestamp = storageMetadata.getModifyTimestamp();
        if (modifyTimestamp != null) {
            return modifyTimestamp;
        } else {
            return storageMetadata.getCreateTimestamp();
        }
    }

    public static @Nullable ProcessMetadataType getProcessMetadata(@NotNull ObjectType object) {
        var metadata = getMetadata(object);
        return metadata != null ? metadata.getProcess() : null;
    }

    public static @NotNull List<ObjectReferenceType> getCreateApproverRefs(@NotNull ObjectType object) {
        var metadata = getProcessMetadata(object);
        return metadata != null ? metadata.getCreateApproverRef() : List.of();
    }

    public static @NotNull Collection<ObjectReferenceType> getCreateApproverRefs(@NotNull AssignmentType assignment) {
        return getMetadataBeans(assignment).stream()
                .map(ValueMetadataType::getProcess)
                .filter(Objects::nonNull)
                .flatMap(p -> p.getCreateApproverRef().stream())
                .collect(Collectors.toSet());
    }

    public static @NotNull Collection<ValueMetadataType> getMetadataBeans(Containerable containerable) {
        return containerable.asPrismContainerValue().getValueMetadata().getRealValues(ValueMetadataType.class);
    }

    public static @NotNull Collection<ValueMetadataType> getMetadataBeans(Referencable referencable) {
        return referencable.asReferenceValue().getValueMetadata().getRealValues(ValueMetadataType.class);
    }

    public static long getSingleValueMetadataId(@NotNull ObjectType object) {
        return stateNonNull(
                getMetadataRequired(object).getId(),
                "No ID for value metadata in %s", object);
    }

    public static long getValueMetadataId(@NotNull Containerable holder, @NotNull ProvenanceMetadataType provenance) {
        return stateNonNull(
                getMetadataRequired(holder, provenance).getId(),
                "No ID for value metadata in %s for %s", holder, provenance);
    }

    public static void addCreationMetadata(@NotNull ObjectType object, XMLGregorianCalendar now) {
        var storageMetadata = getOrCreateStorageMetadata(object);
        if (storageMetadata.getCreateTimestamp() == null) {
            storageMetadata.setCreateTimestamp(now);
        }
    }

    public static boolean hasModifyTimestampDelta(List<ItemDelta<?, ?>> itemDeltas) {
        return itemDeltas.stream().anyMatch(d -> isModifyTimestampDelta(d));
    }

    private static boolean isModifyTimestampDelta(ItemDelta<?, ?> delta) {
        var p = delta.getPath();
        if (p.equivalent(InfraItemName.METADATA)) {
            //noinspection unchecked
            return ((ContainerDelta<ValueMetadataType>) delta).addsAnyValueMatching(
                    pcv -> {
                        var storage = pcv.asContainerable().getStorage();
                        return storage != null && storage.getModifyTimestamp() != null;
                    });
        }
        if (p.startsWith(InfraItemName.METADATA)) {
            var remainder = p.rest();
            stateCheck(remainder.startsWithId(), "Illegal metadata delta path %s", p);
            remainder = remainder.rest();
            if (!remainder.startsWith(ValueMetadataType.F_STORAGE)) {
                return false;
            } else if (remainder.size() == 1) {
                //noinspection unchecked
                return ((ContainerDelta<StorageMetadataType>) delta).addsAnyValueMatching(
                        pcv -> pcv.asContainerable().getModifyTimestamp() != null);

            } else {
                return remainder.rest().equivalent(StorageMetadataType.F_MODIFY_TIMESTAMP);
            }
        }
        return false;
    }

    public static @NotNull ItemDelta<?, ?> createModifyTimestampDelta(@NotNull ObjectType object, XMLGregorianCalendar now)
            throws SchemaException {
        var metadata = getMetadata(object);
        if (metadata == null) {
            return PrismContext.get().deltaFor(object.getClass())
                    .item(InfraItemName.METADATA)
                    .add(new ValueMetadataType()
                            .storage(new StorageMetadataType()
                                    .modifyTimestamp(now)))
                    .asItemDelta();
        } else {
            return PrismContext.get().deltaFor(object.getClass())
                    .item(InfraItemName.METADATA,
                            Objects.requireNonNull(metadata.getId(), () -> "No ID for value metadata in " + object),
                            ValueMetadataType.F_STORAGE,
                            StorageMetadataType.F_MODIFY_TIMESTAMP)
                    .replace(now)
                    .asItemDelta();
        }
    }

    /** Returns the (relative) path to the given metadata bean. */
    public static @NotNull ItemPath getPathOf(@NotNull ValueMetadataType metadata) {
        Long id = metadata.getId();
        if (id != null) {
            return ItemPath.create(InfraItemName.METADATA, id);
        } else {
            throw new IllegalStateException("Metadata value has no ID: " + metadata);
        }
    }

    public static boolean hasValueMetadata(ObjectType object) {
        return object.asPrismContainerValue().hasValueMetadata();
    }

    public static boolean needsMetadataValuePcvIdUpdate(ObjectType object) {
        return hasValueMetadata(object)
                && getMetadataRequired(object).getId() == null;
    }

    public static void updatePcvId(ObjectType object, Collection<? extends ItemDelta<?, ?>> modifications) {
        getMetadataRequired(object)
                .setId(getSingleValueMetadataId(modifications));
    }

    private static long getSingleValueMetadataId(@NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        for (ItemDelta<?, ?> modification : emptyIfNull(modifications)) {
            if (InfraItemName.METADATA.equivalent(modification.getPath())) {
                PrismValue newValue =
                        MiscUtil.extractSingleton(
                                modification.getNewValues(),
                                () -> new IllegalStateException("Multiple new values for object metadata " + modification));
                if (newValue != null) {
                    return MiscUtil.stateNonNull(
                            ((PrismContainerValue<?>) newValue).getId(),
                            "No ID in new value for object metadata " + modification);
                }
            }
        }
        throw new IllegalStateException("Expected metadata ID in modifications, but none was found: " + modifications);
    }

    public static ItemPath getStorageMetadataPath(ObjectType object, QName propName) {
        return ItemPath.create(
                InfraItemName.METADATA,
                getSingleValueMetadataId(object),
                ValueMetadataType.F_STORAGE,
                propName);
    }

    public static Collection<String> getCreateApprovalComments(AssignmentType assignment) {
        return getMetadataBeans(assignment).stream()
                .map(ValueMetadataType::getProcess)
                .filter(Objects::nonNull)
                .flatMap(p -> p.getCreateApprovalComment().stream())
                .collect(Collectors.toSet());
    }

    public static Collection<ObjectReferenceType> getModifyApproverRefs(AssignmentType assignment) {
        return getMetadataBeans(assignment).stream()
                .map(ValueMetadataType::getProcess)
                .filter(Objects::nonNull)
                .flatMap(p -> p.getModifyApproverRef().stream())
                .collect(Collectors.toSet());
    }

    public static Collection<String> getModifyApprovalComments(AssignmentType assignment) {
        return getMetadataBeans(assignment).stream()
                .map(ValueMetadataType::getProcess)
                .filter(Objects::nonNull)
                .flatMap(p -> p.getModifyApprovalComment().stream())
                .collect(Collectors.toSet());
    }

    public static @Nullable XMLGregorianCalendar getRequestTimestamp(AssignmentType assignment) {
        return getMetadataBeans(assignment).stream()
                .map(ValueMetadataType::getProcess)
                .filter(Objects::nonNull)
                .map(ProcessMetadataType::getRequestTimestamp)
                .filter(Objects::nonNull)
                .min(XMLGregorianCalendar::compare)
                .orElse(null);
    }

    public static Collection<ObjectReferenceType> getRequestorRefs(AssignmentType assignment) {
        return getMetadataBeans(assignment).stream()
                .map(ValueMetadataType::getProcess)
                .filter(Objects::nonNull)
                .map(ProcessMetadataType::getRequestorRef)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static Collection<String> getRequestorComments(AssignmentType assignment) {
        return getMetadataBeans(assignment).stream()
                .map(ValueMetadataType::getProcess)
                .filter(Objects::nonNull)
                .map(ProcessMetadataType::getRequestorComment)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static Collection<String> getCertificationOutcomes(AssignmentType assignment) {
        return getMetadataBeans(assignment).stream()
                .map(ValueMetadataType::getProcess)
                .filter(Objects::nonNull)
                .map(ProcessMetadataType::getCertificationOutcome)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static @NotNull Collection<ObjectReferenceType> getCertifierRefs(AssignmentType assignment) {
        return getMetadataBeans(assignment).stream()
                .map(ValueMetadataType::getProcess)
                .filter(Objects::nonNull)
                .map(ProcessMetadataType::getCertifierRef)
                .flatMap(strings -> strings.stream())
                .collect(Collectors.toSet());
    }

    public static @NotNull Collection<String> getCertifierComments(AssignmentType assignment) {
        return getMetadataBeans(assignment).stream()
                .map(ValueMetadataType::getProcess)
                .filter(Objects::nonNull)
                .map(ProcessMetadataType::getCertifierComment)
                .flatMap(strings -> strings.stream())
                .collect(Collectors.toSet());
    }

    public static ValueMetadataType fromLegacy(@NotNull MetadataType legacy) {
        var storage = storageMetadataFrom(legacy);
        var process = processMetadataFrom(legacy);
        var provenance = provenanceMetadataFrom(legacy);
        var provisioning = provisioningMetadataFrom(legacy);
        var transformation = transformationMetadataFrom(legacy);
        return  new ValueMetadataType()
                .storage(nullIfEmpty(storage))
                .process(nullIfEmpty(process))
                .provenance(nullIfEmpty(provenance))
                .provisioning(nullIfEmpty(provisioning))
                .transformation(nullIfEmpty(transformation));
    }


    private static StorageMetadataType storageMetadataFrom(MetadataType legacy) {
        return  new StorageMetadataType()
                // Create metadata
                .createChannel(legacy.getCreateChannel())
                .createTimestamp(legacy.getCreateTimestamp())
                .creatorRef(copyValue(legacy.getCreatorRef()))
                .createTaskRef(copyValue(legacy.getCreateTaskRef()))
                // Modify metadata
                .modifyChannel(legacy.getModifyChannel())
                .modifyTimestamp(legacy.getModifyTimestamp())
                .modifierRef(legacy.getModifierRef())
                .modifyTaskRef(legacy.getModifyTaskRef());
    }

    private static ObjectReferenceType copyValue(ObjectReferenceType ort) {
        return ort != null ? ort.clone() : null;
    }

    private static ProcessMetadataType processMetadataFrom(MetadataType legacy) {
        var process = new ProcessMetadataType()
                .requestTimestamp(legacy.getRequestTimestamp())
                .requestorRef(legacy.getRequestorRef())
                .requestorComment(legacy.getRequestorComment())
                //createApproverRef
                //createApprovalComment
                .createApprovalTimestamp(legacy.getCreateApprovalTimestamp())
                //modifyApproverRef
                //modifyApprovalComment

                .modifyApprovalTimestamp(legacy.getModifyTimestamp())
                //certifierRef
                //certifierComment
                .certificationFinishedTimestamp(legacy.getCertificationFinishedTimestamp())
                .certificationOutcome(legacy.getCertificationOutcome())
                ;
        copyValuesTo(process::createApproverRef,legacy.getCreateApproverRef(), OBJECT_REFERENCE_COPY);
        copyValuesTo(process::createApprovalComment,legacy.getCreateApprovalComment(), Function.identity());
        copyValuesTo(process::modifyApproverRef, legacy.getModifyApproverRef(), OBJECT_REFERENCE_COPY);
        copyValuesTo(process::modifyApprovalComment, legacy.getModifyApprovalComment(), Function.identity());
        copyValuesTo(process::certifierRef, legacy.getCertifierRef(), OBJECT_REFERENCE_COPY);
        copyValuesTo(process::certifierComment, legacy.getCertifierComment(), Function.identity());

        return process;
    }

    private static ProvenanceMetadataType provenanceMetadataFrom(MetadataType legacy) {
        // Legacy Metadata can not be mapped to provenance
        return new ProvenanceMetadataType();
    }


    private static ProvisioningMetadataType provisioningMetadataFrom(MetadataType legacy) {
        return new ProvisioningMetadataType()
                .lastProvisioningTimestamp(legacy.getLastProvisioningTimestamp());
    }

    private static TransformationMetadataType transformationMetadataFrom(MetadataType legacy) {
        // Legacy Metadata can not be mapped to provenance
        return new TransformationMetadataType();

    }


    private static <T extends Containerable> T nullIfEmpty(T container) {
        if (container == null || container.asPrismContainerValue().isEmpty()) {
            return null;
        }
        return container;
    }

    private static <I,O> void copyValuesTo(Function<I,O> addFunc, Collection<I> values, Function<I,I> copyFunc) {
        for (var v : values) {
            addFunc.apply(copyFunc.apply(v));
        }
    }
}
