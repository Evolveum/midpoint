/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor.deleg;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.DeepCloneOperation;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.schema.processor.PropertyLimitations;
import com.evolveum.midpoint.schema.processor.RawResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

public interface RefinedAttributeDefinitionDelegator<T> extends AttributeDefinitionDelegator<T>, ResourceAttributeDefinition<T> {

    @Override
    ResourceAttributeDefinition<T> delegate();

    @Override
    default boolean isTolerant() {
        return delegate().isTolerant();
    }

    @Override
    default Boolean isSecondaryIdentifierOverride() {
        return delegate().isSecondaryIdentifierOverride();
    }

    @Override
    default boolean canAdd(LayerType layer) {
        return delegate().canAdd(layer);
    }

    @Override
    default boolean canRead(LayerType layer) {
        return delegate().canRead(layer);
    }

    @Override
    default boolean canModify(LayerType layer) {
        return delegate().canModify(layer);
    }

    @Override
    default ItemProcessing getProcessing(LayerType layer) {
        return delegate().getProcessing(layer);
    }

    @Override
    default String getDescription() {
        return delegate().getDescription();
    }

    @Override
    default RawResourceAttributeDefinition<T> getRawAttributeDefinition() {
        return delegate().getRawAttributeDefinition();
    }

    @Override
    default @Nullable MappingType getOutboundMappingBean() {
        return delegate().getOutboundMappingBean();
    }

    @Override
    default @NotNull List<InboundMappingType> getInboundMappingBeans() {
        return delegate().getInboundMappingBeans();
    }

    @Override
    default int getMaxOccurs(LayerType layer) {
        return delegate().getMaxOccurs(layer);
    }

    @Override
    default int getMinOccurs(LayerType layer) {
        return delegate().getMinOccurs(layer);
    }


    @Override
    default boolean isExclusiveStrong() {
        return delegate().isExclusiveStrong();
    }

    @Override
    default PropertyLimitations getLimitations(LayerType layer) {
        return delegate().getLimitations(layer);
    }

    @Override
    default AttributeFetchStrategyType getFetchStrategy() {
        return delegate().getFetchStrategy();
    }

    @Override
    default @NotNull AttributeStorageStrategyType getStorageStrategy() {
        return delegate().getStorageStrategy();
    }

    @Override
    default @NotNull List<String> getTolerantValuePatterns() {
        return delegate().getTolerantValuePatterns();
    }

    @Override
    default @NotNull List<String> getIntolerantValuePatterns() {
        return delegate().getIntolerantValuePatterns();
    }

    @Override
    default boolean isVolatilityTrigger() {
        return delegate().isVolatilityTrigger();
    }

    @Override
    default String debugDump(int indent, LayerType layer) {
        return delegate().debugDump(indent, layer);
    }

    @Override
    default Integer getModificationPriority() {
        return delegate().getModificationPriority();
    }

    @Override
    default Boolean getReadReplaceMode() {
        return delegate().getReadReplaceMode();
    }

    @Override
    default boolean isDisplayNameAttribute() {
        return delegate().isDisplayNameAttribute();
    }

    @Override
    default @Nullable ItemCorrelatorDefinitionType getCorrelatorDefinition() {
        return delegate().getCorrelatorDefinition();
    }

    @Override
    ResourceAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation);
}
