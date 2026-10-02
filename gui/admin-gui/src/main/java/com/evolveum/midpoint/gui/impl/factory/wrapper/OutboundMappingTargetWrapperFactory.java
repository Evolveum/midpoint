/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.List;

import com.evolveum.midpoint.util.QNameUtil;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.impl.component.input.range.MappingRangeUtils;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/** Supplies a target with the computed default range for new mappings without a target. */
@Component
public class OutboundMappingTargetWrapperFactory extends NoEmptyValueContainerWrapperFactoryImpl<MappingType> {

    private static final List<ItemPath> MAPPING_PATHS = List.of(
            ItemPath.create(AssignmentHolderType.F_ASSIGNMENT, AssignmentType.F_FOCUS_MAPPINGS, MappingsType.F_MAPPING),
            ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_FOCUS_MAPPINGS, MappingsType.F_MAPPING),
            ItemPath.create(ObjectTemplateType.F_MAPPING),
            ItemPath.create(ObjectTemplateType.F_ITEM, ObjectTemplateItemDefinitionType.F_MAPPING));

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return def instanceof PrismContainerDefinition
                && def.getTypeClass() != null
                && MappingType.class.isAssignableFrom(def.getTypeClass())
                && parent != null
                && QNameUtil.match(def.getItemName(),ResourceAttributeDefinitionType.F_OUTBOUND)
                && MAPPING_PATHS.stream().anyMatch(path -> path.equivalent(
                        parent.getPath().namedSegmentsOnly().append(def.getItemName())));
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }

    @Override
    public PrismContainerValueWrapper<MappingType> createContainerValueWrapper(
            PrismContainerWrapper<MappingType> parent, PrismContainerValue<MappingType> value,
            ValueStatus status, WrapperContext context) {
        PrismContainerValueWrapper<MappingType> wrapper =
                super.createContainerValueWrapper(parent, value, status, context);
        initializeMissingTarget(wrapper, status);
        return wrapper;
    }

    static void initializeMissingTarget(PrismContainerValueWrapper<MappingType> wrapper, ValueStatus status) {
        if (status == ValueStatus.ADDED && wrapper != null) {
            MappingType mapping = wrapper.getRealValue();
            if (mapping != null && mapping.getTarget() == null) {
                VariableBindingDefinitionType target = new VariableBindingDefinitionType();
                target.setSet(MappingRangeUtils.defaultRange(wrapper, target));
                mapping.setTarget(target);
            }
        }
    }
}
