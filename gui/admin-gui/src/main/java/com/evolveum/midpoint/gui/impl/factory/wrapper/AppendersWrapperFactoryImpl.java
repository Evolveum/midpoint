/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
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
        List<? extends ItemDefinition> defs = getComplexTypeDefinitions(value);
        if (defs != null) {
            return defs;
        }

        return parent.getDefinitions();
    }

    private List<? extends ItemDefinition> getComplexTypeDefinitions(PrismContainerValue<C> value) {
        if (value == null) {
            return null;
        }

        ComplexTypeDefinition ctd = value.getComplexTypeDefinition();
        if (ctd == null) {
            return null;
        }

        return ctd.getDefinitions();
    }
}
