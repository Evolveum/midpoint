/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utility class for provenance- and validation-related metadata.
 * <p>
 * Supports:
 * - AI-provided data provenance
 * - System-provided data provenance
 * - Validation error metadata
 */
public class SmartMetadataUtil {

    /**
     * Static instance representing AI provenance origin.
     * Currently, we use the origin of {@link SystemObjectsType#ORIGIN_ARTIFICIAL_INTELLIGENCE}.
     * (with all other items null) to indicate that the value was provided by AI.
     */
    private static final ProvenanceMetadataType AI_PROVENANCE_METADATA = createAiProvenanceMetadata();

    /**
     * Static instance representing System provenance origin.
     * Currently, we use the origin of {@link SystemObjectsType#ORIGIN_SYSTEM_INTELLIGENCE}.
     * (with all other items null) to indicate that the value was provided by the system
     */
    private static final ProvenanceMetadataType SYSTEM_PROVENANCE_METADATA = createSystemProvenanceMetadata();

    /** Shared provenance selector. */
    public enum ProvenanceKind {
        AI(AI_PROVENANCE_METADATA),
        SYSTEM(SYSTEM_PROVENANCE_METADATA);

        final ProvenanceMetadataType metadata;

        ProvenanceKind(ProvenanceMetadataType metadata) {
            this.metadata = metadata;
        }
    }

    public static boolean isMarkedProvenanceProvided(@NotNull ProvenanceKind kind, PrismValue value) {
        return switch (kind) {
            case AI -> SmartMetadataUtil.isMarkedAsAiProvided(value);
            case SYSTEM -> SmartMetadataUtil.isMarkedAsSystemProvided(value);
        };
    }

    private static @NotNull ProvenanceMetadataType createAiProvenanceMetadata() {
        var metadata = new ProvenanceMetadataType()
                .acquisition(new ProvenanceAcquisitionType()
                        .originRef(SystemObjectsType.ORIGIN_ARTIFICIAL_INTELLIGENCE.value(), ServiceType.COMPLEX_TYPE));
        metadata.freeze();
        return metadata;
    }

    private static @NotNull ProvenanceMetadataType createSystemProvenanceMetadata() {
        var metadata = new ProvenanceMetadataType()
                .acquisition(new ProvenanceAcquisitionType()
                        .originRef(SystemObjectsType.ORIGIN_SYSTEM_INTELLIGENCE.value(), ServiceType.COMPLEX_TYPE));
        metadata.freeze();
        return metadata;
    }

    private static void markProvenance(@Nullable PrismValue value, @NotNull ProvenanceKind kind) {
        forEachLeafValue(value,
                pv -> ValueMetadataTypeUtil.getOrCreateMetadata(pv, kind.metadata));
    }

    public static void markContainerProvenance(@Nullable PrismValue value, @NotNull ProvenanceKind kind) {
        if(value != null){
            ValueMetadataTypeUtil.getOrCreateMetadata(value, kind.metadata);
        }
    }

    private static void unmarkProvenance(@Nullable PrismValue value, @NotNull ProvenanceKind kind) {
        forEachLeafValue(value,
                pv -> removeProvenanceMetadata(pv, kind));
    }

    private static boolean isMarkedWithProvenance(
            @Nullable PrismValue value, @NotNull ProvenanceKind kind) {

        return value != null
                && value.hasValueMetadata()
                && (ValueMetadataTypeUtil.getMetadata(value, kind.metadata) != null
                || hasProvenance(value, kind));
    }

    private static boolean hasProvenance(@NotNull PrismValue value, @NotNull ProvenanceKind kind) {
        return anyMetadataEntry(value, vm ->
                vm.getProvenance() != null &&
                        containsMatchingOriginRefMetadata(vm.getProvenance(), kind.metadata));
    }

    /**
     * Removes AI provenance metadata entries from the given PrismValue.
     * <p>
     * Operates directly on the {@link ValueMetadata} container of the value.
     * Returns {@code true} if at least one provenance entry was removed.
     * </p>
     */
    private static boolean removeProvenanceMetadata(@NotNull PrismValue prismValue, @NotNull ProvenanceKind kind) {

        ValueMetadata vmc = prismValue.getValueMetadata();
        if (vmc.hasNoValues()) {
            return false;
        }

        Iterator<PrismContainerValue<Containerable>> it = vmc.getValues().iterator();
        boolean removed = false;

        while (it.hasNext()) {
            PrismContainerValue<?> pcv = it.next();
            ValueMetadataType vmType = pcv.getRealValue();
            if (vmType == null || vmType.getProvenance() == null) {
                continue;
            }
            if (containsMatchingOriginRefMetadata(vmType.getProvenance(), kind.metadata)) {
                it.remove();
                removed = true;
            }
        }

        if (removed && vmc.hasNoValues()) {
            vmc.clear();
        }
        return removed;
    }

    /**
     * Synchronizes AI provenance metadata between the old and new value.
     * <ul>
     *     <li>If the old value was marked as AI-provided and the new value differs,
     *         the AI provenance is removed from the new value.</li>
     *     <li>If the old and new values are equal, the AI provenance mark is re-applied
     *         to the new value.</li>
     * </ul>
     */
    private static void syncProvenance(
            @NotNull PrismValue newValue,
            @NotNull PrismValue oldValue,
            @NotNull ProvenanceKind kind) {

        if (isMarkedWithProvenance(oldValue, kind)) {
            if (!Objects.equals(oldValue.getRealValue(), newValue.getRealValue())) {
                unmarkProvenance(newValue, kind);
            } else {
                markProvenance(newValue, kind);
            }
        }
    }

    /** Convenience variant of {@link #markAsAiProvided(PrismValue)}. Marks the leaf nodes in the whole value. */
    public static <C extends Containerable> @NotNull C markAsAiProvided(@NotNull C containerable) {
        markAsAiProvided(containerable.asPrismContainerValue());
        return containerable;
    }

    /** Convenience variant of {@link #markAsAiProvided(PrismValue)}. Marks the leaf nodes in items in the value (if present). */
    public static <C extends Containerable> C markAsAiProvided(@NotNull C containerable, ItemPath... paths) {
        markForPaths(containerable, ProvenanceKind.AI, paths);
        return containerable;
    }

    /**
     * Marks all leaf values (i.e. property and reference values) as provided by AI. Uses value metadata to indicate that.
     * Does nothing if the value is {@code null}.
     * <p>
     * The values themselves must be mutable!
     * <p>
     * TODO The question is whether we should also mark the container values as AI-provided.
     */
    public static void markAsAiProvided(@Nullable PrismValue value) {
        markProvenance(value, ProvenanceKind.AI);
    }

    public static boolean isMarkedAsAiProvided(@Nullable PrismValue value) {
        return isMarkedWithProvenance(value, ProvenanceKind.AI);
    }

    public static void unmarkAsAiProvided(@Nullable PrismValue value) {
        unmarkProvenance(value, ProvenanceKind.AI);
    }

    // TODO remove after MVP
    public static void markContainerValueAsAiProvided(@Nullable PrismContainerValue<?> value) {
        if (value != null) {
            ValueMetadataTypeUtil.getOrCreateMetadata(value, AI_PROVENANCE_METADATA);
        }
    }

    public static void syncAiProvenanceWithChangeIfApplied(
            @NotNull PrismValue newValue, @NotNull PrismValue oldValue) {
        syncProvenance(newValue, oldValue, ProvenanceKind.AI);
    }

    public static <C extends Containerable> @NotNull C markAsSystemProvided(@NotNull C containerable) {
        markAsSystemProvided(containerable.asPrismContainerValue());
        return containerable;
    }

    public static <C extends Containerable> C markAsSystemProvided(@NotNull C containerable, ItemPath... paths) {
        markForPaths(containerable, ProvenanceKind.SYSTEM, paths);
        return containerable;
    }

    public static void markAsSystemProvided(@Nullable PrismValue value) {
        markProvenance(value, ProvenanceKind.SYSTEM);
    }

    public static boolean isMarkedAsSystemProvided(@Nullable PrismValue value) {
        return isMarkedWithProvenance(value, ProvenanceKind.SYSTEM);
    }

    public static void unmarkAsSystemProvided(@Nullable PrismValue value) {
        unmarkProvenance(value, ProvenanceKind.SYSTEM);
    }

    public static void syncSystemProvenanceWithChangeIfApplied(
            @NotNull PrismValue newValue, @NotNull PrismValue oldValue) {
        syncProvenance(newValue, oldValue, ProvenanceKind.SYSTEM);
    }

    private static <C extends Containerable> void markForPaths(
            @NotNull C containerable,
            @NotNull ProvenanceKind kind,
            ItemPath @NotNull ... paths) {

        for (ItemPath p : paths) {
            Item<?, ?> item = containerable.asPrismContainerValue().findItem(p);
            if (item != null) {
                item.getValues().forEach(v -> markProvenance(v, kind));
            }
        }
    }

    /** Marks leaf values as invalid with an optional validationError. */
    public static void markAsInvalid(@Nullable PrismValue value, @Nullable String validationError) {
        forEachLeafValue(value, v -> setValidationOnValue(v, validationError));
    }

    public static <C extends Containerable> C markAsInvalid(
            @NotNull C containerable, @Nullable String error, ItemPath @NotNull ... paths) {

        for (ItemPath path : paths) {
            Item<?, ?> item = containerable.asPrismContainerValue().findItem(path);
            if (item != null) {
                item.getValues().forEach(v -> markAsInvalid(v, error));
            }
        }
        return containerable;
    }

    /** Returns {@code true} if the value has validation metadata at the root level. */
    public static boolean isMarkedAsInvalid(@Nullable PrismValue value) {
        return value != null && value.hasValueMetadata() && hasErrorValidation(value);
    }

    /** Returns a user-friendly message if the filter is marked as invalid. */
    public static @Nullable String getFilterInvalidMessage(@Nullable PrismValue value) {
        if (value == null || !value.hasValueMetadata()) {
            return null;
        }

        ValueMetadata metadata = value.getValueMetadata();
        if (metadata.hasNoValues()) {
            return null;
        }

        for (PrismContainerValue<Containerable> pcv : metadata.getValues()) {
            ValueMetadataType vmType = pcv.getRealValue();
            if (vmType != null && vmType.getValidation() != null) {
                String msg = vmType.getValidation().getValidationError();
                if (msg != null && !msg.isBlank()) {
                    return msg;
                }
            }
        }

        return isMarkedAsInvalid(value)
                ? "This filter is invalid. Update or remove it to continue."
                : null;
    }

    private static void setValidationOnValue(@NotNull PrismValue value, @Nullable String msg) {
        ValueMetadata metadata = value.getValueMetadata();
        ValueMetadataType target = ValueMetadataTypeUtil.getOrCreateMetadataWithValidation(metadata);

        if (msg != null) {
            ValueMetadataTypeUtil.getOrCreateValidationMetadata(target).setValidationError(msg);
        } else {
            ValueMetadataTypeUtil.getOrCreateValidationMetadata(target);
        }
    }

    private static boolean hasErrorValidation(@NotNull PrismValue value) {
        return anyMetadataEntry(value, vm ->
                vm.getValidation() != null && vm.getValidation().getValidationError() != null);
    }

    /** Shared visitor to act on leaf values (property/reference). Skips metadata and recurses into containers. */
    private static void forEachLeafValue(
            @Nullable PrismValue value,
            @NotNull Consumer<PrismValue> leafConsumer) {

        if (value == null) {
            return;
        }

        value.acceptVisitor(visitable -> {
            if (visitable instanceof ValueMetadata) {
                return false;
            }
            if (visitable instanceof PrismContainerValue<?>) {
                return true;
            }
            if (visitable instanceof PrismValue pv) {
                leafConsumer.accept(pv);
                return false;
            }
            return visitable instanceof Item;
        });
    }

    /** Checks if any metadata entry on the root value satisfies the provided predicate. */
    private static boolean anyMetadataEntry(
            @NotNull PrismValue value,
            @NotNull Predicate<ValueMetadataType> predicate) {

        if (!value.hasValueMetadata()) {
            return false;
        }

        ValueMetadata vm = value.getValueMetadata();
        if (vm.hasNoValues()) {
            return false;
        }

        for (PrismContainerValue<Containerable> pcv : vm.getValues()) {
            ValueMetadataType type = pcv.getRealValue();
            if (type != null && predicate.test(type)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Compares two provenance metadata instances by their originRef OIDs.
     */
    public static boolean containsMatchingOriginRefMetadata(
            @NotNull ProvenanceMetadataType candidate,
            @NotNull ProvenanceMetadataType expected) {

        Set<String> cand = candidate.getAcquisition().stream()
                .map(a -> a.getOriginRef() != null ? a.getOriginRef().getOid() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<String> exp = expected.getAcquisition().stream()
                .map(a -> a.getOriginRef() != null ? a.getOriginRef().getOid() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        return cand.equals(exp);
    }
}
