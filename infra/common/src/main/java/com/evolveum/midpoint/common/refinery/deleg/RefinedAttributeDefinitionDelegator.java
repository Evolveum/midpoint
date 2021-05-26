package com.evolveum.midpoint.common.refinery.deleg;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.deleg.AttributeDefinitionDelegator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeStorageStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

public interface RefinedAttributeDefinitionDelegator<T> extends AttributeDefinitionDelegator<T>, RefinedAttributeDefinition<T> {

    @Override
    RefinedAttributeDefinition<T> delegate();

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
        return delegate().isIgnored(layer);
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
    default ResourceAttributeDefinition<T> getAttributeDefinition() {
        return delegate().getAttributeDefinition();
    }

    @Override
    default MappingType getOutboundMappingType() {
        return delegate().getOutboundMappingType();
    }

    @Override
    default boolean hasOutboundMapping() {
        return delegate().hasOutboundMapping();
    }

    @Override
    default List<MappingType> getInboundMappingTypes() {
        return delegate().getInboundMappingTypes();
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
        return delegate().isOptional(layer);
    }

    @Override
    default boolean isMandatory(LayerType layer) {
        return delegate().isMandatory(layer);
    }

    @Override
    default boolean isMultiValue(LayerType layer) {
        return delegate().isMultiValue(layer);
    }

    @Override
    default boolean isSingleValue(LayerType layer) {
        return delegate().isSingleValue(layer);
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
    default AttributeStorageStrategyType getStorageStrategy() {
        return delegate().getStorageStrategy();
    }

    @Override
    default List<String> getTolerantValuePattern() {
        return delegate().getTolerantValuePattern();
    }

    @Override
    default List<String> getIntolerantValuePattern() {
        return delegate().getIntolerantValuePattern();
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
    RefinedAttributeDefinition<T> deepClone(Map<QName, ComplexTypeDefinition> ctdMap,
            Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction);
}
