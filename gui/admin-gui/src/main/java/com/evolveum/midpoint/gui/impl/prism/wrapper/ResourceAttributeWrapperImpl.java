/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.DeepCloneOperation;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ResourceAttributeWrapper;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author skublik
 *
 */
public class ResourceAttributeWrapperImpl<T> extends PrismPropertyWrapperImpl<T> implements ResourceAttributeWrapper<T> {

    private static final long serialVersionUID = 1L;

    public ResourceAttributeWrapperImpl(PrismContainerValueWrapper<?> parent, ResourceAttribute<T> item, ItemStatus status) {
        super(parent, item, status);
    }

    @Override
    public boolean isTolerant() {
        return getRefinedAttributeDefinition().isTolerant();
    }

    private ResourceAttributeDefinition<T> getRefinedAttributeDefinition() {
        return (ResourceAttributeDefinition<T>) getItemDefinition();
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
    public RawResourceAttributeDefinition<T> getRawAttributeDefinition() {
        return getRefinedAttributeDefinition().getRawAttributeDefinition();
    }

    @Override
    public @Nullable MappingType getOutboundMappingBean() {
        return getRefinedAttributeDefinition().getOutboundMappingBean();
    }

    @Override
    public @NotNull List<InboundMappingType> getInboundMappingBeans() {
        return getRefinedAttributeDefinition().getInboundMappingBeans();
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
    public boolean isExclusiveStrong() {
        return getRefinedAttributeDefinition().isExclusiveStrong();
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
    public @NotNull AttributeStorageStrategyType getStorageStrategy() {
        return getRefinedAttributeDefinition().getStorageStrategy();
    }

    @Override
    public @NotNull List<String> getTolerantValuePatterns() {
        return getRefinedAttributeDefinition().getTolerantValuePatterns();
    }

    @Override
    public @NotNull List<String> getIntolerantValuePatterns() {
        return getRefinedAttributeDefinition().getIntolerantValuePatterns();
    }

    @Override
    public boolean isVolatilityTrigger() {
        return getRefinedAttributeDefinition().isVolatilityTrigger();
    }

    @NotNull
    @Override
    public ResourceAttributeDefinition<T> clone() {
        return getRefinedAttributeDefinition().clone();
    }

    @Override
    public ResourceAttributeDefinition<T> deepClone(@NotNull DeepCloneOperation operation) {
        return getRefinedAttributeDefinition().deepClone(operation);
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
    public @NotNull ResourceAttributeDefinition<T> forLayer(@NotNull LayerType layer) {
        return getRefinedAttributeDefinition().forLayer(layer);
    }

    @Override
    public void setOverrideCanRead(Boolean value) {
        getRefinedAttributeDefinition().setOverrideCanRead(value);
    }

    @Override
    public void setOverrideCanAdd(Boolean value) {
        getRefinedAttributeDefinition().setOverrideCanAdd(value);
    }

    @Override
    public void setOverrideCanModify(Boolean value) {
        getRefinedAttributeDefinition().setOverrideCanModify(value);
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

    @Override
    public @Nullable ItemCorrelatorDefinitionType getCorrelatorDefinition() {
        return getRefinedAttributeDefinition().getCorrelatorDefinition();
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
    public @NotNull MutableRawResourceAttributeDefinition<T> toMutable() {
        return getRefinedAttributeDefinition().toMutable();
    }

    @Override
    public Optional<ComplexTypeDefinition> structuredType() {
        return getRefinedAttributeDefinition().structuredType();
    }

    @Override
    public @Nullable Boolean getReturnedByDefault() {
        return getRefinedAttributeDefinition().getReturnedByDefault();
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
    public @NotNull LayerType getCurrentLayer() {
        return getRefinedAttributeDefinition().getCurrentLayer();
    }
}
