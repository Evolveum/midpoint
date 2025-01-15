/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus.inbounds;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * The source data for inbound mappings related to a single shadow or association value.
 *
 * It covers:
 *
 * - the object itself: {@link ShadowType} or {@link ShadowAssociationValue} (before and after the change - if applicable)
 * - delta (sync delta or the delta computed in the previous wave)
 *
 * Note that deltas are currently supported only for shadows, not for shadow association values.
 *
 * @see ShadowLikeValue
 */
@Experimental
public interface InboundSourceData extends DebugDumpable, Serializable {

    /** Returns old or current shadow, for the purpose of setting expression variables. */
    default PrismObject<ShadowType> getShadowVariableValue() {
        return null;
    }

    /** Returns the association value, for the purpose of setting expression variables. */
    default ShadowAssociationValueType getAssociationVariableValue() {
        return null;
    }

    /** Returns the specific item, for the purpose of providing the mapping source (IDI). */
    <V extends PrismValue, D extends ItemDefinition<?>> Item<V,D> getItemOld(@NotNull ItemPath itemPath);

    /** Returns true if there is no shadow (after change). */
    default boolean isNoShadow() {
        return false; // Association values are always considered to "be there".
    }

    default @NotNull InboundSourceData updateShadowAfterReload(@NotNull PrismObject<ShadowType> currentShadow) {
        throw new UnsupportedOperationException();
    }

    default boolean hasSyncOrEffectiveDelta() {
        return false; // supported only for shadows
    }

    default boolean hasEffectiveDelta() {
        return false; // supported only for shadows
    }

    /**
     * Delta that moves the item from the state at the beginning of the clockwork execution to the current state.
     * NEVER the sync delta. Always the delta computed by the projector in the previous wave.
     */
    default @Nullable <V extends PrismValue, D extends ItemDefinition<?>> ItemDelta<V, D> getEffectiveItemDelta(ItemPath path) {
        return null;
    }

    static InboundSourceData forShadowWithoutDelta(@Nullable PrismObject<ShadowType> shadow) {
        return new Shadow(
                asObjectable(shadow),
                asObjectable(shadow),
                null, true, false);
    }

    static InboundSourceData forShadow(
            @Nullable PrismObject<ShadowType> oldShadow,
            @Nullable PrismObject<ShadowType> currentShadow,
            @Nullable ObjectDelta<ShadowType> delta,
            boolean isDeltaEffective) {
        return new Shadow(
                asObjectable(oldShadow),
                asObjectable(currentShadow),
                delta, isDeltaEffective, false);
    }

    static InboundSourceData forAssociationValue(@NotNull ShadowAssociationValue associationValue) {
        return new AssociationValue(associationValue);
    }

    /** The delta (if present) must be the sync delta. */
    static @NotNull InboundSourceData forShadowLikeValue(
            @NotNull ShadowLikeValue shadowLikeValue,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta) {
        if (shadowLikeValue instanceof AbstractShadow shadow) {
            return forShadow(
                    shadow.getPrismObject(), // old = current here, as the delta (if present at all) is the sync delta
                    shadow.getPrismObject(),
                    resourceObjectDelta,
                    false);
        } else if (shadowLikeValue instanceof ShadowAssociationValue associationValue) {
            return forAssociationValue(associationValue);
        } else {
            throw new IllegalStateException("Unsupported shadow-like value: " + shadowLikeValue);
        }
    }

    class Shadow implements InboundSourceData {

        @Nullable private final ShadowType oldShadow;
        @Nullable private final ShadowType currentShadow;
        @Nullable private final ObjectDelta<ShadowType> shadowDelta;

        /** True if the delta is NOT a sync delta, but a computed one, bringing {@link #oldShadow} into {@link #currentShadow}. */
        private final boolean isDeltaEffective;

        /**
         * True if the {@link #currentShadow} was obtained by re-loading the shadow from the resource.
         * In that case, we use {@link #currentShadow} for some of the items.
         */
        private final boolean shadowReloaded;

        public Shadow(
                @Nullable ShadowType oldShadow,
                @Nullable ShadowType currentShadow,
                @Nullable ObjectDelta<ShadowType> shadowDelta,
                boolean isDeltaEffective,
                boolean shadowReloaded) {
            this.oldShadow = oldShadow;
            this.currentShadow = currentShadow;
            this.shadowDelta = shadowDelta;
            this.isDeltaEffective = isDeltaEffective;
            this.shadowReloaded = shadowReloaded;
        }

        @Override
        public boolean hasSyncOrEffectiveDelta() {
            return shadowDelta != null;
        }

        @Override
        public boolean hasEffectiveDelta() {
            return shadowDelta != null && isDeltaEffective;
        }

        @Override
        public @Nullable <V extends PrismValue, D extends ItemDefinition<?>> ItemDelta<V, D> getEffectiveItemDelta(ItemPath path) {
            return shadowDelta != null && isDeltaEffective ? shadowDelta.findItemDelta(path) : null;
        }

        @Override
        public boolean isNoShadow() {
            return currentShadow == null;
        }

        @Override
        public <V extends PrismValue, D extends ItemDefinition<?>> Item<V, D> getItemOld(@NotNull ItemPath itemPath) {
            if (shadowReloaded) {
                if (getEffectiveItemDelta(itemPath) != null) {
                    // If we have the delta, we need to apply it to the old state.
                    return getItem(oldShadow, itemPath);
                } else {
                    // But if there's no delta, let's take the reloaded state.
                    return getItem(currentShadow, itemPath);
                }
            } else {
                // Shadow was not reloaded, so it's OK to take old state even if there's no delta (old = current in that case).
                return getItem(oldShadow, itemPath);
            }
        }

        private static <V extends PrismValue, D extends ItemDefinition<?>> Item<V, D> getItem(ShadowType shadow, ItemPath path) {
            return shadow != null ? shadow.asPrismObject().findItem(path) : null;
        }

        @Override
        public @NotNull InboundSourceData updateShadowAfterReload(@NotNull PrismObject<ShadowType> currentShadow) {
            return new Shadow(oldShadow, currentShadow.asObjectable(), shadowDelta, isDeltaEffective, true);
        }

        @Override
        public PrismObject<ShadowType> getShadowVariableValue() {
            return asPrismObject(currentShadow != null ? currentShadow : oldShadow);
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
            if (oldShadow != currentShadow) {
                DebugUtil.debugDumpWithLabelLn(sb, "oldShadow", oldShadow, indent + 1);
                DebugUtil.debugDumpWithLabelLn(sb, "currentShadow", currentShadow, indent + 1);
            } else {
                DebugUtil.debugDumpWithLabelLn(
                        sb, "oldShadow=currentShadow", oldShadow, indent + 1);
            }
            DebugUtil.debugDumpWithLabelLn(sb, "shadowDelta", shadowDelta, indent + 1);
            DebugUtil.debugDumpWithLabelLn(sb, "isDeltaEffective", isDeltaEffective, indent + 1);
            DebugUtil.debugDumpWithLabel(sb, "shadowReloaded", shadowReloaded, indent + 1);
            return sb.toString();
        }
    }

    class AssociationValue implements InboundSourceData {

        @NotNull private final ShadowAssociationValue associationValue;

        AssociationValue(@NotNull ShadowAssociationValue associationValue) {
            this.associationValue = associationValue;
        }

        public @NotNull ShadowAssociationValue getAssociationValue() {
            return associationValue;
        }

        @Override
        public PrismObject<ShadowType> getShadowVariableValue() {
            return associationValue.isComplex() ?
                    associationValue.getAssociationDataObject().getPrismObject() : null;
        }

        @Override
        public ShadowAssociationValueType getAssociationVariableValue() {
            return associationValue.asContainerable();
        }

        @Override
        public <V extends PrismValue, D extends ItemDefinition<?>> Item<V, D> getItemOld(@NotNull ItemPath itemPath) {
            return associationValue.findItem(itemPath);
        }

        @Override
        public String debugDump(int indent) {
            return associationValue.debugDump(indent);
        }
    }
}
