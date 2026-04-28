/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

@Component
public class OrderConstraintsWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<OrderConstraintsType> {

    @Override
    public <C extends Containerable > boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return QNameUtil.match(def.getTypeName(), OrderConstraintsType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 100;
    }
}
