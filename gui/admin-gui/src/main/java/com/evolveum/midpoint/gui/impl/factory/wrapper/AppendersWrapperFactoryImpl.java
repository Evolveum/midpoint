/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;


import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderConfigurationType;

/**
 * @author skublik
 *
 */
@Component
public class AppendersWrapperFactoryImpl<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(def.getTypeName(), AppenderConfigurationType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    protected List<? extends ItemDefinition> getItemDefinitions(PrismContainerWrapper<C> parent,
            PrismContainerValue<C> value) {
        if(value != null && value.getComplexTypeDefinition() != null
                && value.getComplexTypeDefinition().getDefinitions() != null) {
            return value.getComplexTypeDefinition().getDefinitions();
        }
        return parent.getDefinitions();
    }
}
