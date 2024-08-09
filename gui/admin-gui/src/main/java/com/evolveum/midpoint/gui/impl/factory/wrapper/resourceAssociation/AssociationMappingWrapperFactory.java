/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper.resourceAssociation;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.factory.wrapper.PrismContainerWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AttributeMappingValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ObjectTypeAttributeMappingWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.association.AssociationAttributeMappingWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author lskublik
 */
@Component
public class AssociationMappingWrapperFactory extends PrismContainerWrapperFactoryImpl<AbstractAttributeMappingsDefinitionType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(def.getTypeName(), AttributeInboundMappingsDefinitionType.COMPLEX_TYPE)
                || QNameUtil.match(def.getTypeName(), AttributeOutboundMappingsDefinitionType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public PrismContainerValueWrapper<AbstractAttributeMappingsDefinitionType> createContainerValueWrapper(
            PrismContainerWrapper<AbstractAttributeMappingsDefinitionType> objectWrapper,
            PrismContainerValue<AbstractAttributeMappingsDefinitionType> objectValue,
            ValueStatus status,
            WrapperContext context) {
        AttributeMappingValueWrapper<AbstractAttributeMappingsDefinitionType> value = new AttributeMappingValueWrapper<>(objectWrapper, objectValue, status);
        @Nullable AbstractAttributeMappingsDefinitionType realValue = objectValue.getRealValue();

        MappingDirection mappingDirection = null;
        if (AssociationSynchronizationExpressionEvaluatorType.F_ATTRIBUTE.equivalent(objectWrapper.getItemName())) {
            mappingDirection = MappingDirection.ATTRIBUTE;
        } else if (AssociationSynchronizationExpressionEvaluatorType.F_OBJECT_REF.equivalent(objectWrapper.getItemName())) {
            mappingDirection = MappingDirection.OBJECTS;
        }
        if (status.equals(ValueStatus.NOT_CHANGED) && realValue != null) {
            value.addAttributeMappingType(MappingDirection.OVERRIDE);
            PrismContainer<Containerable> mappingContainer = objectValue.findContainer(AttributeInboundMappingsDefinitionType.F_MAPPING);
            if (mappingContainer != null && !mappingContainer.isEmpty()) {
                value.addAttributeMappingType(mappingDirection);
            }
        } else if (status.equals(ValueStatus.ADDED) && context.getAttributeMappingType() != null) {
            value.addAttributeMappingType(context.getAttributeMappingType());
        }
        return value;
    }

    @Override
    protected PrismContainerWrapper<AbstractAttributeMappingsDefinitionType> createWrapperInternal(
            PrismContainerValueWrapper<?> parent,
            PrismContainer<AbstractAttributeMappingsDefinitionType> childContainer,
            ItemStatus status,
            WrapperContext ctx) {

        status = recomputeStatus(childContainer, status, ctx);

        AssociationAttributeMappingWrapper containerWrapper =
                new AssociationAttributeMappingWrapper(parent, childContainer, status);
        VirtualContainersSpecificationType virtualContainerSpec = ctx.findVirtualContainerConfiguration(containerWrapper.getPath());
        if (virtualContainerSpec != null) {
            containerWrapper.setVirtual(true);
            containerWrapper.setShowInVirtualContainer(true);
        }

        return containerWrapper;
    }
}
