/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper.resourceAssociation;

import com.evolveum.midpoint.gui.impl.factory.wrapper.HeterogenousContainerWrapperFactory;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemSynchronizationActionsType;

import org.springframework.stereotype.Component;

@Component
public class AssociationSynchronizationActionFactory<C extends Containerable> extends HeterogenousContainerWrapperFactory<C> {

    @Override
    public <C1 extends Containerable> boolean match(ItemDefinition<?> itemDef, PrismContainerValue<C1> parent) {
        return itemDef.getTypeClass() != null
                && itemDef.getTypeClass().isAssignableFrom(ItemSynchronizationActionsType.class);
    }

    protected boolean filterDefinitions(PrismContainerValue<C> value, ItemDefinition<?> def) {
        Item<?, ?> child = value.findItem(def.getItemName());
        return (child != null) || !(def instanceof PrismContainerDefinition);
    }

    @Override
    public int getOrder() {
        return 109;
    }
}
