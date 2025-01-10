/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.page.admin.workflow.WorkItemDetailsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;

import org.springframework.stereotype.Component;

/**
 * Wrapper factory for attribute/volatility/incoming(outgoing)/operation
 *
 * @author lskublik
 */
@Component
public class VolatilityDependencyOperationWrapperFactoryImpl extends PrismPropertyWrapperFactoryImpl<ChangeTypeType> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return def instanceof PrismPropertyDefinition
                && ShadowItemDependencyType.F_OPERATION.equivalent(def.getItemName())
                && parent != null
                && (ItemPath.create(
                        ResourceType.F_SCHEMA_HANDLING,
                        SchemaHandlingType.F_OBJECT_TYPE,
                        ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                        ResourceAttributeDefinitionType.F_VOLATILITY,
                        ShadowItemVolatilityType.F_INCOMING)
                .equivalent(parent.getPath().namedSegmentsOnly()) ||
                ItemPath.create(
                                ResourceType.F_SCHEMA_HANDLING,
                                SchemaHandlingType.F_OBJECT_TYPE,
                                ResourceObjectTypeDefinitionType.F_ATTRIBUTE,
                                ResourceAttributeDefinitionType.F_VOLATILITY,
                                ShadowItemVolatilityType.F_OUTGOING)
                        .equivalent(parent.getPath().namedSegmentsOnly()));
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    protected PrismPropertyWrapper<ChangeTypeType> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismProperty<ChangeTypeType> item, ItemStatus status, WrapperContext wrapperContext) {
        PrismPropertyWrapperImpl<ChangeTypeType> wrapper =
                (PrismPropertyWrapperImpl<ChangeTypeType>) super.createWrapperInternal(parent, item, status, wrapperContext);
        wrapper.setDisplayName(LocalizationUtil.translate(
                "ShadowItemVolatilityType." + parent.getDefinition().getItemName().getLocalPart() + ".operation.displayName"));
        wrapper.setHelp(LocalizationUtil.translate(
                "ShadowItemVolatilityType." + parent.getDefinition().getItemName().getLocalPart() + ".operation.help"));
        return wrapper;
    }
}
