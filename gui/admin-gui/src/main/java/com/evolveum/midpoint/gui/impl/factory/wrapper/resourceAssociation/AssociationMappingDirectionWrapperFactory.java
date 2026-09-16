/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper.resourceAssociation;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.impl.factory.wrapper.PrismContainerWrapperFactoryImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.association.AssociationMappingDirectionWrapper;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.ItemDefinition;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.PrismContainer;

@Component
public class AssociationMappingDirectionWrapperFactory extends PrismContainerWrapperFactoryImpl<MappingType> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {

        if (!super.match(def, parent)) {
            return false;
        }

        if (parent == null) {
            return false;
        }

        boolean equivalent = ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION).equivalent(parent.getPath().namedSegmentsOnly());

        if (!equivalent) {
            return false;
        }

        return ShadowAssociationDefinitionType.F_INBOUND.equivalent(def.getItemName())
                || ShadowAssociationDefinitionType.F_OUTBOUND.equivalent(def.getItemName());
    }

    @Override
    protected PrismContainerWrapper<MappingType> createWrapperInternal(
            PrismContainerValueWrapper<?> parent,
            PrismContainer<MappingType> childContainer,
            ItemStatus status,
            WrapperContext ctx) {

        return new AssociationMappingDirectionWrapper(parent, childContainer, status);
    }

    @Override
    public int getOrder() {
        return 90;
    }
}
