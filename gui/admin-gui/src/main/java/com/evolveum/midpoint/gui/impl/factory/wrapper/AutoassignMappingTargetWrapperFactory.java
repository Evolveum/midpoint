/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeUtils;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import org.springframework.stereotype.Component;

/**
 * Autoassign's {@code target} is never a real user choice - it's always the role's own assignment.
 * The schema-derived "Target" label is misleading, so this relabels it.
 */
@Component
public class AutoassignMappingTargetWrapperFactory extends PrismPropertyWrapperFactoryImpl<VariableBindingDefinitionType> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return def instanceof PrismPropertyDefinition
                && MappingType.F_TARGET.equivalent(def.getItemName())
                && parent != null
                && MappingRangeUtils.AUTOASSIGN_MAPPING_PATH.equivalent(parent.getPath().namedSegmentsOnly());
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    protected PrismPropertyWrapper<VariableBindingDefinitionType> createWrapperInternal(
            PrismContainerValueWrapper<?> parent, PrismProperty<VariableBindingDefinitionType> item,
            ItemStatus status, WrapperContext wrapperContext) {
        PrismPropertyWrapperImpl<VariableBindingDefinitionType> wrapper =
                (PrismPropertyWrapperImpl<VariableBindingDefinitionType>) super.createWrapperInternal(parent, item, status, wrapperContext);
        wrapper.setDisplayName(LocalizationUtil.translate("AutoassignPanel.mapping.target.label"));
        return wrapper;
    }
}
