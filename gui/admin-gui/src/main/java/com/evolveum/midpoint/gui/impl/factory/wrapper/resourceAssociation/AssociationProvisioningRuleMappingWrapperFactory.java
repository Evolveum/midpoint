/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper.resourceAssociation;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.wrapper.PrismContainerWrapperFactoryImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

@Component
public class AssociationProvisioningRuleMappingWrapperFactory<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C> {

    @Override
    protected boolean shouldCreateEmptyValue(PrismContainer<C> item, WrapperContext context) {
        return false; //TODO fix zero delta (something with expressions?)
    }

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {

        if (!ShadowAssociationDefinitionType.F_INBOUND.equivalent(def.getItemName())
                && !ShadowAssociationDefinitionType.F_OUTBOUND.equivalent(def.getItemName())) {
            return false;
        }

        if (parent == null) {
            return false;
        }

        return ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION).equivalent(parent.getPath().namedSegmentsOnly());
    }

    @Override
    public PrismContainerValueWrapper<C> createValueWrapper(
            PrismContainerWrapper<C> parent,
            PrismContainerValue<C> value,
            ValueStatus status,
            WrapperContext context) throws SchemaException {

        return super.createValueWrapper(parent, value, status, context);
    }

    @Override
    public int getOrder() {
        return 90;
    }

}
