/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceAttributeMappingValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceAttributeMappingWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author lskublik
 */
@Component
public class ResourceAttributeMappingWrapperFactory extends PrismContainerWrapperFactoryImpl<ResourceAttributeDefinitionType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(def.getTypeName(), ResourceAttributeDefinitionType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public PrismContainerValueWrapper<ResourceAttributeDefinitionType> createContainerValueWrapper(
            PrismContainerWrapper<ResourceAttributeDefinitionType> objectWrapper,
            PrismContainerValue<ResourceAttributeDefinitionType> objectValue,
            ValueStatus status,
            WrapperContext context) {
        ResourceAttributeMappingValueWrapper value = new ResourceAttributeMappingValueWrapper(objectWrapper, objectValue, status);
        @Nullable ResourceAttributeDefinitionType realValue = objectValue.getRealValue();
        if (status.equals(ValueStatus.NOT_CHANGED) && realValue != null) {
            value.addAttributeMappingType(MappingDirection.OVERRIDE);
            if (!realValue.getInbound().isEmpty()) {
                value.addAttributeMappingType(MappingDirection.INBOUND);
            }
            if (realValue.getOutbound() != null) {
                value.addAttributeMappingType(MappingDirection.OUTBOUND);
            }
        } else if (status.equals(ValueStatus.ADDED) && context.getAttributeMappingType() != null) {
            value.addAttributeMappingType(context.getAttributeMappingType());
        }
        return value;
    }

    @Override
    protected PrismContainerWrapper<ResourceAttributeDefinitionType> createWrapperInternal(
            PrismContainerValueWrapper<?> parent,
            PrismContainer<ResourceAttributeDefinitionType> childContainer,
            ItemStatus status,
            WrapperContext ctx) {

        status = recomputeStatus(childContainer, status, ctx);

        ResourceAttributeMappingWrapper containerWrapper =
                new ResourceAttributeMappingWrapper(parent, childContainer, status);
        VirtualContainersSpecificationType virtualContainerSpec = ctx.findVirtualContainerConfiguration(containerWrapper.getPath());
        if (virtualContainerSpec != null) {
            containerWrapper.setVirtual(true);
            containerWrapper.setShowInVirtualContainer(true);
        }

        return containerWrapper;
    }
}
