/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Full prism definition of a {@link ShadowSimpleAttribute}: has a native part and a refined part from `schemaHandling`.
 *
 * @see ShadowSimpleAttribute
 */
public interface ShadowSimpleAttributeDefinition<T>
        extends
        PrismPropertyDefinition<T>,
        ShadowAttributeDefinition<
                PrismPropertyValue<T>,
                ShadowSimpleAttributeDefinition<T>,
                T,
                ShadowSimpleAttribute<T>
                > {

    @Override
    default @NotNull Class<T> getTypeClass() {
        return PrismPropertyDefinition.super.getTypeClass();
    }

    default @NotNull PrismPropertyValue<T> convertPrismValue(@NotNull PrismPropertyValue<?> srcValue) {
        return ShadowAttributeValueConvertor.convertPropertyValue(srcValue, this);
    }

    @Override
    <T extends ItemDefinition<?>> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz);

    @NotNull
    ShadowSimpleAttributeDefinition<T> clone();

    @Override
    ShadowSimpleAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation);

    @NotNull PrismPropertyDefinition.PrismPropertyDefinitionMutator<T> mutator();

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

    /** Creates an empty delta for this attribute against its standard path. */
    default @NotNull PropertyDelta<T> createEmptyDelta() {
        return createEmptyDelta(getStandardPath());
    }

    @Override
    @NotNull
    ShadowSimpleAttributeDefinition<T> forLayer(@NotNull LayerType layer);
}
