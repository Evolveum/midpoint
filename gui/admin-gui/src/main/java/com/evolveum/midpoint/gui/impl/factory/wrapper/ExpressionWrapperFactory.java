/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.panel.ExpressionPropertyPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ExpressionWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by honchar
 */
@Component
public class ExpressionWrapperFactory  extends PrismPropertyWrapperFactoryImpl<ExpressionType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(ExpressionType.COMPLEX_TYPE, def.getTypeName());
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    protected PrismPropertyWrapper<ExpressionType> createWrapper(PrismContainerValueWrapper<?> parent, PrismProperty<ExpressionType> item,
                                                                 ItemStatus status, WrapperContext ctx) {

        ExpressionWrapper propertyWrapper = new ExpressionWrapper(parent, item, status);
        if (propertyWrapper.isConstructionExpression() || propertyWrapper.isAttributeExpression() || propertyWrapper.isAssociationExpression()) {
            getRegistry().registerWrapperPanel(propertyWrapper.getTypeName(), ExpressionPropertyPanel.class);
        } else {
            return super.createWrapper(parent, item, status, ctx);
        }
        return propertyWrapper;
    }

}
