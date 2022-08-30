/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext.AttributeMappingType;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceAttributeMappingValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ResourceAttributeMappingWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author lskublik
 */
@Component
public class OutboundAttributeMappingWrapperFactory extends PrismContainerWrapperFactoryImpl<MappingType> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return QNameUtil.match(def.getTypeName(), MappingType.COMPLEX_TYPE)
                && QNameUtil.match(def.getItemName(),ResourceAttributeDefinitionType.F_OUTBOUND)
                && parent != null
                && ItemPath.create(ResourceType.F_SCHEMA_HANDLING,
                    SchemaHandlingType.F_OBJECT_TYPE,
                    ResourceObjectTypeDefinitionType.F_ATTRIBUTE).equivalent(parent.getPath().namedSegmentsOnly());
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<MappingType> item, WrapperContext context) {
        if (context.isConfigureMappingType()) {
            return false;
        }
        return super.shouldCreateEmptyValue(item, context);
    }
}
