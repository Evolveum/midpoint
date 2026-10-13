/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/** Creates empty single-valued outbound mappings for inducement constructions */
@Component
public class ConstructionOutboundMappingWrapperFactory extends PrismContainerWrapperFactoryImpl<MappingType> {

    private static final ItemPath OUTBOUND_PATH = ItemPath.create(
            AbstractRoleType.F_INDUCEMENT,
            AssignmentType.F_CONSTRUCTION,
            ConstructionType.F_ATTRIBUTE,
            ResourceAttributeDefinitionType.F_OUTBOUND);

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return def instanceof PrismContainerDefinition
                && def.getTypeClass() != null
                && MappingType.class.isAssignableFrom(def.getTypeClass())
                && parent != null
                && OUTBOUND_PATH.equivalent(parent.getPath().namedSegmentsOnly().append(def.getItemName()));
    }

    @Override
    public int getOrder() {
        return 990;
    }

    @Override
    public PrismContainerValueWrapper<MappingType> createContainerValueWrapper(
            PrismContainerWrapper<MappingType> parent, PrismContainerValue<MappingType> value,
            ValueStatus status, WrapperContext context) {
        // TODO: #12672 & #12671
        return super.createContainerValueWrapper(parent, value, status, context);
    }
}
