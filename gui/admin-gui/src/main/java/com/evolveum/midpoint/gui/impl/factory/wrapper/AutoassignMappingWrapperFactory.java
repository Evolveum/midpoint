/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocalAutoassignSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.springframework.stereotype.Component;

/**
 * A new autoassign mapping should start with a default range Matching provenance,
 * not an empty dropdown. Only fires for newly added values - existing ones are left untouched.
 */
@Component
public class AutoassignMappingWrapperFactory extends NoEmptyValueContainerWrapperFactoryImpl<MappingType> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return def instanceof PrismContainerDefinition
                && QNameUtil.match(def.getTypeName(), AutoassignMappingType.COMPLEX_TYPE)
                && FocalAutoassignSpecificationType.F_MAPPING.equivalent(def.getItemName())
                && parent != null
                && MappingRangeUtils.AUTOASSIGN_FOCUS_PATH.equivalent(parent.getPath().namedSegmentsOnly());
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    public PrismContainerValueWrapper<MappingType> createValueWrapper(
            PrismContainerWrapper<MappingType> parent, PrismContainerValue<MappingType> value,
            ValueStatus status, WrapperContext context) throws SchemaException {
        PrismContainerValueWrapper<MappingType> valueWrapper = super.createValueWrapper(parent, value, status, context);
        if (ValueStatus.ADDED == status && context.isCreateIfEmpty()) {
            MappingRangeUtils.initializeRange(valueWrapper);
        }
        return valueWrapper;
    }
}
