/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.RangeAwareMappingTargetValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/** Supplies range-aware target values for inbound attribute, object template, and focus mappings. */
@Component
public class RangeAwareMappingTargetWrapperFactory
        extends PrismPropertyWrapperFactoryImpl<VariableBindingDefinitionType> {

    private static final List<ItemPath> MAPPING_PATHS = List.of(
            ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE,
                    ResourceObjectTypeDefinitionType.F_ATTRIBUTE, ResourceAttributeDefinitionType.F_INBOUND),
            ItemPath.create(ObjectTemplateType.F_MAPPING),
            ItemPath.create(ObjectTemplateType.F_ITEM, ObjectTemplateItemDefinitionType.F_MAPPING),
            ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_FOCUS_MAPPINGS, MappingsType.F_MAPPING),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_FOCUS_MAPPINGS, MappingsType.F_MAPPING));

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return def instanceof PrismPropertyDefinition
                && QNameUtil.match(def.getTypeName(), VariableBindingDefinitionType.COMPLEX_TYPE)
                && MappingType.F_TARGET.equivalent(def.getItemName())
                && parent != null
                && parent.asContainerable() instanceof MappingType
                && MAPPING_PATHS.stream().anyMatch(path -> path.equivalent(parent.getPath().namedSegmentsOnly()));
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    public PrismPropertyValueWrapper<VariableBindingDefinitionType> createValueWrapper(
            PrismPropertyWrapper<VariableBindingDefinitionType> parent,
            PrismPropertyValue<VariableBindingDefinitionType> value, ValueStatus status, WrapperContext context) {
        return new RangeAwareMappingTargetValueWrapper(parent, value, status);
    }
}
