/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeStorageStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author mederly
 */
public interface RefinedAttributeDefinition<T> extends ResourceAttributeDefinition<T> {
    boolean isTolerant();

    Boolean isSecondaryIdentifierOverride();

    boolean canAdd(LayerType layer);

    boolean canRead(LayerType layer);

    boolean canModify(LayerType layer);

    @Deprecated // Remove in 4.2
    boolean isIgnored(LayerType layer);

    ItemProcessing getProcessing(LayerType layer);

    String getDescription();

    ResourceAttributeDefinition<T> getAttributeDefinition();

    MappingType getOutboundMappingType();

    boolean hasOutboundMapping();

    List<MappingType> getInboundMappingTypes();

    int getMaxOccurs(LayerType layer);

    int getMinOccurs(LayerType layer);

    boolean isOptional(LayerType layer);

    boolean isMandatory(LayerType layer);

    boolean isMultiValue(LayerType layer);

    boolean isSingleValue(LayerType layer);

    boolean isExclusiveStrong();

    PropertyLimitations getLimitations(LayerType layer);

    AttributeFetchStrategyType getFetchStrategy();

    AttributeStorageStrategyType getStorageStrategy();

    List<String> getTolerantValuePattern();

    List<String> getIntolerantValuePattern();

    boolean isVolatilityTrigger();

    @NotNull
    @Override
    RefinedAttributeDefinition<T> clone();

    @Override
    RefinedAttributeDefinition<T> deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction);

    String debugDump(int indent, LayerType layer);

    Integer getModificationPriority();

    Boolean getReadReplaceMode();

    boolean isDisplayNameAttribute();
}
