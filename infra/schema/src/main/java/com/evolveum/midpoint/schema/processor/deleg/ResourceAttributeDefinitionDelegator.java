/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor.deleg;

import java.util.List;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.deleg.PropertyDefinitionDelegator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.DeepCloneOperation;
import com.evolveum.midpoint.prism.ItemProcessing;

import javax.xml.namespace.QName;

public interface ResourceAttributeDefinitionDelegator<T>
        extends PropertyDefinitionDelegator<T>, ShadowSimpleAttributeDefinition<T> {

    @Override
    ShadowSimpleAttributeDefinition<T> delegate();

    @Override
    default Boolean getReturnedByDefault() {
        return delegate().getReturnedByDefault();
    }

    @Override
    default String getNativeAttributeName() {
        return delegate().getNativeAttributeName();
    }

    @Override
    default String getFrameworkAttributeName() {
        return delegate().getFrameworkAttributeName();
    }

    @Override
    default @NotNull ShadowSimpleAttribute<T> instantiate() {
        return delegate().instantiate();
    }

    @Override
    default @NotNull ShadowSimpleAttribute<T> instantiate(QName name) {
        return delegate().instantiate(name);
    }

    @Override
    default String debugDump(int indent, LayerType layer) {
        return delegate().debugDump(indent, layer);
    }

    @Override
    default PropertyLimitations getLimitations(LayerType layer) {
        return delegate().getLimitations(layer);
    }

    @Override
    default @NotNull AttributeFetchStrategyType getFetchStrategy() {
        return delegate().getFetchStrategy();
    }

    @Override
    default @NotNull AttributeStorageStrategyType getStorageStrategy() {
        return delegate().getStorageStrategy();
    }

    @Override
    default Boolean isCached() {
        return delegate().isCached();
    }

    @Override
    default boolean isVolatilityTrigger() {
        return delegate().isVolatilityTrigger();
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
    default boolean isIgnored(LayerType layer) {
        return ShadowSimpleAttributeDefinition.super.isIgnored(layer);
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
    default @Nullable MappingType getOutboundMappingBean() {
        return delegate().getOutboundMappingBean();
    }

    @Override
    default boolean hasOutboundMapping() {
        return delegate().hasOutboundMapping();
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
    default boolean isOptional(LayerType layer) {
        return ShadowSimpleAttributeDefinition.super.isOptional(layer);
    }

    @Override
    default boolean isMandatory(LayerType layer) {
        return ShadowSimpleAttributeDefinition.super.isMandatory(layer);
    }

    @Override
    default boolean isMultiValue(LayerType layer) {
        return ShadowSimpleAttributeDefinition.super.isMultiValue(layer);
    }

    @Override
    default boolean isSingleValue(LayerType layer) {
        return ShadowSimpleAttributeDefinition.super.isSingleValue(layer);
    }

    @Override
    default boolean isExclusiveStrong() {
        return delegate().isExclusiveStrong();
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
    default boolean isDisplayNameAttribute() {
        return delegate().isDisplayNameAttribute();
    }

    @Override
    default ItemCorrelatorDefinitionType getCorrelatorDefinition() {
        return delegate().getCorrelatorDefinition();
    }

    @Override
    default @Nullable ItemChangeApplicationModeType getChangeApplicationMode() {
        return delegate().getChangeApplicationMode();
    }

    @Override
    @Nullable
    default String getLifecycleState() {
        return delegate().getLifecycleState();
    }

    @Override
    default ShadowSimpleAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation) {
        return null;
    }

    @Override
    default void setOverrideCanRead(Boolean value) {
        delegate().setOverrideCanRead(value);
    }

    @Override
    default void setOverrideCanAdd(Boolean value) {
        delegate().setOverrideCanAdd(value);
    }

    @Override
    default void setOverrideCanModify(Boolean value) {
        delegate().setOverrideCanModify(value);
    }

    @Override
    @NotNull
    default ShadowSimpleAttributeDefinition<T> forLayer(@NotNull LayerType layer) {
        return delegate().forLayer(layer);
    }

    @Override
    @NotNull
    default LayerType getCurrentLayer() {
        return delegate().getCurrentLayer();
    }

    @Override
    @NotNull default Class<T> getTypeClass() {
        return delegate().getTypeClass();
    }

    @Override
    default boolean hasRefinements() {
        return delegate().hasRefinements();
    }

    default boolean isIndexOnly() {
        return delegate().isIndexOnly();
    }

    @Override
    default String getHumanReadableDescription() {
        return delegate().getHumanReadableDescription();
    }

    @Override
    default boolean isSimulated() {
        return delegate().isSimulated();
    }

    @Override
    default <T extends ItemDefinition<?>> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        return delegate().findItemDefinition(path, clazz);
    }

    @Override
    default String getNativeDescription() {
        return delegate().getNativeDescription();
    }
}
