/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Full prism definition of a {@link ShadowSimpleAttribute}: has a native part and a refined part from `schemaHandling`.
 *
 * TODO rename to ShadowAttributeDefinition (too many occurrences!)
 *
 * @see ShadowSimpleAttribute
 */
public interface ShadowSimpleAttributeDefinition<T>
        extends
        PrismPropertyDefinition<T>,
        ShadowAttributeDefinition<ShadowSimpleAttribute<T>, T> {

    @Override
    default @NotNull Class<T> getTypeClass() {
        return PrismPropertyDefinition.super.getTypeClass();
    }

    /**
     * Creates a new {@link ShadowSimpleAttribute} from given {@link PrismProperty}.
     * Used in the process of "definition application" in `applyDefinitions` and similar methods.
     *
     * Assumes that the original property is correctly constructed, i.e. it has no duplicate values.
     */
    default @NotNull ShadowSimpleAttribute<T> instantiateFrom(@NotNull PrismProperty<?> property) throws SchemaException {
        //noinspection unchecked
        ShadowSimpleAttribute<T> attribute = instantiateFromRealValues((Collection<T>) property.getRealValues());
        attribute.setIncomplete(property.isIncomplete());
        return attribute;
    }

    default @NotNull ShadowSimpleAttribute<T> instantiateFromValue(PrismPropertyValue<T> value) throws SchemaException {
        ShadowSimpleAttribute<T> attribute = instantiate();
        attribute.add(value);
        return attribute;
    }

    /**
     * Creates a new {@link ShadowSimpleAttribute} from given real values, converting them if necessary.
     *
     * Assumes that the values contain no duplicates and no nulls.
     */
    default @NotNull ShadowSimpleAttribute<T> instantiateFromRealValues(@NotNull Collection<T> realValues) throws SchemaException {
        ShadowSimpleAttribute<T> attribute = instantiate();
        attribute.addNormalizedValues(realValues, this); // FIXME SKIP NORMALIZATION!!!
        return attribute;
    }

    default @NotNull ShadowSimpleAttribute<T> instantiateFromRealValue(@NotNull T realValue) throws SchemaException {
        return instantiateFromRealValues(List.of(realValue));
    }

    default @NotNull PrismPropertyValue<T> convertPrismValue(@NotNull PrismPropertyValue<?> srcValue) {
        return PrismUtil.convertPropertyValue(srcValue, this);
    }

    @NotNull
    ShadowSimpleAttributeDefinition<T> clone();

    @Override
    ShadowSimpleAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation);

    @NotNull PrismPropertyDefinition.PrismPropertyDefinitionMutator<T> mutator();

    /**
     * Provides a debug dump respective to the given layer.
     *
     * TODO reconsider this method
     */
    String debugDump(int indent, LayerType layer);

//    /**
//     * Creates a copy of the definition, with a given customizer applied to the _raw_ part of the definition.
//     *
//     * Should be used only in special cases, near the resource schema construction time.
//     * (So various alternate implementations of this interface need not support this method.)
//     *
//     * May not preserve all information (like access override flags).
//     *
//     * TODO is this needed?
//     */
//    default ResourceAttributeDefinition<T> spawnModifyingRaw(
//            @NotNull Consumer<NativeShadowAttributeDefinitionImpl<T>> rawPartCustomizer) {
//        throw new UnsupportedOperationException();
//    }

    /**
     * Is this attribute designated as a secondary identifier via `schemaHandling`?
     *
     * @see ResourceAttributeDefinitionType#isSecondaryIdentifier()
     */
    Boolean isSecondaryIdentifierOverride();

    /**
     * Is this attribute configured to serve as a display name?
     *
     * @see ResourceItemDefinitionType#isDisplayNameAttribute()
     */
    boolean isDisplayNameAttribute();

    /** @see ItemRefinedDefinitionType#getCorrelator() */
    ItemCorrelatorDefinitionType getCorrelatorDefinition();

    /** Creates a normalization-aware version of this definition. */
    default <N> @NotNull NormalizationAwareResourceAttributeDefinition<N> toNormalizationAware() {
        return new NormalizationAwareResourceAttributeDefinition<>(this);
    }

    /** Returns the standard path where this attribute can be found in shadows. E.g. for searching. */
    default @NotNull ItemPath getStandardPath() {
        return ItemPath.create(ShadowType.F_ATTRIBUTES, getItemName());
    }

    /** Creates an empty delta for this attribute against its standard path. */
    default @NotNull PropertyDelta<T> createEmptyDelta() {
        return createEmptyDelta(getStandardPath());
    }

    @Override
    @NotNull
    ShadowSimpleAttributeDefinition<T> forLayer(@NotNull LayerType layer);
}
