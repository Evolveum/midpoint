package com.evolveum.midpoint.schema.processor.deleg;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.DeepCloneOperation;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.schema.processor.PropertyLimitations;
import com.evolveum.midpoint.schema.processor.RawResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.deleg.PropertyDefinitionDelegator;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface AttributeDefinitionDelegator<T> extends PropertyDefinitionDelegator<T>, ResourceAttributeDefinition<T> {

    @Override
    ResourceAttributeDefinition<T> delegate();

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
    default @NotNull ResourceAttribute<T> instantiate() {
        return delegate().instantiate();
    }

    @Override
    default @NotNull ResourceAttribute<T> instantiate(QName name) {
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
    default AttributeFetchStrategyType getFetchStrategy() {
        return delegate().getFetchStrategy();
    }

    @Override
    default @NotNull AttributeStorageStrategyType getStorageStrategy() {
        return delegate().getStorageStrategy();
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
        return ResourceAttributeDefinition.super.isIgnored(layer);
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
    default boolean hasOutboundMapping() {
        return ResourceAttributeDefinition.super.hasOutboundMapping();
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
        return ResourceAttributeDefinition.super.isOptional(layer);
    }

    @Override
    default boolean isMandatory(LayerType layer) {
        return ResourceAttributeDefinition.super.isMandatory(layer);
    }

    @Override
    default boolean isMultiValue(LayerType layer) {
        return ResourceAttributeDefinition.super.isMultiValue(layer);
    }

    @Override
    default boolean isSingleValue(LayerType layer) {
        return ResourceAttributeDefinition.super.isSingleValue(layer);
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
    default @Nullable ItemCorrelatorDefinitionType getCorrelatorDefinition() {
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
    default ResourceAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation) {
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
    default ResourceAttributeDefinition<T> forLayer(@NotNull LayerType layer) {
        return delegate().forLayer(layer);
    }

    @Override
    @NotNull
    default LayerType getCurrentLayer() {
        return delegate().getCurrentLayer();
    }
}
