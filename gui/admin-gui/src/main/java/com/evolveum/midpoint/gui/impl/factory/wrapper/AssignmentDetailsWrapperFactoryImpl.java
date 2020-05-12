/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PersonaConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * @author katka
 *
 */
@Component
public class AssignmentDetailsWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        QName typeName = def.getTypeName();
        return PersonaConstructionType.COMPLEX_TYPE.equals(typeName) || MappingsType.COMPLEX_TYPE.equals(typeName) || PolicyRuleType.COMPLEX_TYPE.equals(typeName);
    }

    @Override
    protected boolean canCreateWrapper(ItemDefinition<?> def, ItemStatus status, WrapperContext context, boolean isEmptyValue) {
        return !isEmptyValue;
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }
}
