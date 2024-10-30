/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.prism.deleg.ContainerDefinitionDelegator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationsType;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;


@Component
public class ShadowAssociationsContainerWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<ShadowAssociationsType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof ContainerDefinitionDelegator associationDef
                && associationDef.getCompileTimeClass() != null
                && ShadowAssociationsType.class.isAssignableFrom(associationDef.getCompileTimeClass());
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
