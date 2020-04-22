/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.PropertyLimitations;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.schema.processor.MutableResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeStorageStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.jetbrains.annotations.NotNull;

/**
 * @author skublik
 *
 */
public class ResourceAttributeWrapperImpl<T> extends PrismPropertyWrapperImpl<T> implements ResourceAttributeWrapper<T>{

    private static final long serialVersionUID = 1L;

    public ResourceAttributeWrapperImpl(PrismContainerValueWrapper<?> parent, ResourceAttribute<T> item, ItemStatus status) {
        super(parent, item, status);
    }

    @Override
    public boolean isTolerant() {
        return getRefinedAttributeDefinition().isTolerant();
    }

    private RefinedAttributeDefinition getRefinedAttributeDefinition() {
        return (RefinedAttributeDefinition) getItemDefinition();
    }

    @Override
    public Boolean isSecondaryIdentifierOverride() {
        return getRefinedAttributeDefinition().isSecondaryIdentifierOverride();
    }

    @Override
    public boolean canAdd() {
        return canAdd(LayerType.PRESENTATION);
    }

    @Override
    public boolean canAdd(LayerType layer) {
        return getRefinedAttributeDefinition().canAdd(layer);
    }

    @Override
    public boolean canRead() {
        return canRead(LayerType.PRESENTATION);
    }


    @Override
    public boolean canRead(LayerType layer) {
        return getRefinedAttributeDefinition().canRead(layer);
    }

    @Override
    public boolean canModify() {
        return canModify(LayerType.PRESENTATION);
    }

    @Override
    public boolean canModify(LayerType layer) {
        return getRefinedAttributeDefinition().canModify(layer);
    }

    @Override
    public boolean isIgnored() {
        return isIgnored(LayerType.PRESENTATION);
    }

    @Override
    public boolean isIgnored(LayerType layer) {
        return getRefinedAttributeDefinition().isIgnored(layer);
    }

    @Override
    public ItemProcessing getProcessing() {
        return getProcessing(LayerType.PRESENTATION);
    }

    @Override
    public ItemProcessing getProcessing(LayerType layer) {
        return getRefinedAttributeDefinition().getProcessing(layer);
    }

    @Override
    public String getDescription() {
        return getRefinedAttributeDefinition().getDescription();
    }

    @Override
    public ResourceAttributeDefinition<T> getAttributeDefinition() {
        return getRefinedAttributeDefinition().getAttributeDefinition();
    }

    @Override
    public MappingType getOutboundMappingType() {
        return getRefinedAttributeDefinition().getOutboundMappingType();
    }

    @Override
    public boolean hasOutboundMapping() {
        return getRefinedAttributeDefinition().hasOutboundMapping();
    }

    @Override
    public List<MappingType> getInboundMappingTypes() {
        return getRefinedAttributeDefinition().getInboundMappingTypes();
    }

    @Override
    public int getMaxOccurs() {
        return getMaxOccurs(LayerType.PRESENTATION);
    }

    @Override
    public int getMaxOccurs(LayerType layer) {
        return getRefinedAttributeDefinition().getMaxOccurs(layer);
    }

    @Override
    public int getMinOccurs() {
        return getMinOccurs(LayerType.PRESENTATION);
    }

    @Override
    public int getMinOccurs(LayerType layer) {
        return getRefinedAttributeDefinition().getMinOccurs(layer);
    }

    @Override
    public boolean isOptional() {
        return isOptional(LayerType.PRESENTATION);
    }

    @Override
    public boolean isOptional(LayerType layer) {
        return getRefinedAttributeDefinition().isOptional(layer);
    }

    @Override
    public boolean isMandatory() {
        return isMandatory(LayerType.PRESENTATION);
    }

    @Override
    public boolean isMandatory(LayerType layer) {
        return getRefinedAttributeDefinition().isMandatory(layer);
    }

    @Override
    public boolean isMultiValue() {
        return isMultiValue(LayerType.PRESENTATION);
    }

    @Override
    public boolean isMultiValue(LayerType layer) {
        return getRefinedAttributeDefinition().isMultiValue(layer);
    }

    @Override
    public boolean isSingleValue() {
        return isSingleValue(LayerType.PRESENTATION);
    }

    @Override
    public boolean isSingleValue(LayerType layer) {
        return getRefinedAttributeDefinition().isSingleValue(layer);
    }

    @Override
    public boolean isExlusiveStrong() {
        return getRefinedAttributeDefinition().isExlusiveStrong();
    }

    @Override
    public PropertyLimitations getLimitations(LayerType layer) {
        return getRefinedAttributeDefinition().getLimitations(layer);
    }

    @Override
    public AttributeFetchStrategyType getFetchStrategy() {
        return getRefinedAttributeDefinition().getFetchStrategy();
    }

    @Override
    public AttributeStorageStrategyType getStorageStrategy() {
        return getRefinedAttributeDefinition().getStorageStrategy();
    }

    @Override
    public List<String> getTolerantValuePattern() {
        return getRefinedAttributeDefinition().getTolerantValuePattern();
    }

    @Override
    public List<String> getIntolerantValuePattern() {
        return getRefinedAttributeDefinition().getIntolerantValuePattern();
    }

    @Override
    public boolean isVolatilityTrigger() {
        return getRefinedAttributeDefinition().isVolatilityTrigger();
    }

    @NotNull
    @Override
    public RefinedAttributeDefinition<T> clone() {
        return getRefinedAttributeDefinition().clone();
    }

    @Override
    public RefinedAttributeDefinition<T> deepClone(
            Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath,
            Consumer<ItemDefinition> postCloneAction) {
        return getRefinedAttributeDefinition().deepClone(ctdMap, onThisPath, postCloneAction);
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, LayerType.PRESENTATION);
    }

    @Override
    public String debugDump(int indent, LayerType layer) {
        return getRefinedAttributeDefinition().debugDump(indent, layer);
    }

    @Override
    public Integer getModificationPriority() {
        return getRefinedAttributeDefinition().getModificationPriority();
    }

    @Override
    public Boolean getReadReplaceMode() {
        return getRefinedAttributeDefinition().getReadReplaceMode();
    }

    @Override
    public boolean isDisplayNameAttribute() {
        return getRefinedAttributeDefinition().isDisplayNameAttribute();
    }

    @NotNull
    @Override
    public ResourceAttribute<T> instantiate() {
        return getRefinedAttributeDefinition().instantiate();
    }

    @NotNull
    @Override
    public ResourceAttribute<T> instantiate(QName name) {
        return getRefinedAttributeDefinition().instantiate(name);
    }

    @Override
    public Boolean getReturnedByDefault() {
        return getRefinedAttributeDefinition().getReturnedByDefault();
    }

    @Override
    public boolean isReturnedByDefault() {
        return getRefinedAttributeDefinition().isReturnedByDefault();
    }

    @Override
    public boolean isPrimaryIdentifier(ResourceAttributeContainerDefinition objectDefinition) {
        return getRefinedAttributeDefinition().isPrimaryIdentifier(objectDefinition);
    }

    @Override
    public boolean isPrimaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
        return getRefinedAttributeDefinition().isPrimaryIdentifier(objectDefinition);
    }

    @Override
    public boolean isSecondaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
        return getRefinedAttributeDefinition().isSecondaryIdentifier(objectDefinition);
    }

    @Override
    public String getNativeAttributeName() {
        return getRefinedAttributeDefinition().getNativeAttributeName();
    }

    @Override
    public String getFrameworkAttributeName() {
        return getRefinedAttributeDefinition().getFrameworkAttributeName();
    }

    @Override
    public MutableResourceAttributeDefinition<T> toMutable() {
        return getRefinedAttributeDefinition().toMutable();
    }
}
