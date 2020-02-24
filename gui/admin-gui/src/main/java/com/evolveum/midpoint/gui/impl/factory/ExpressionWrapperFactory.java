/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.impl.prism.*;
import com.evolveum.midpoint.gui.impl.prism.component.ExpressionPropertyPanel;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.gui.impl.prism.ExpressionWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import net.sf.jasperreports.olap.mapping.Mapping;
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
                                                                 ItemStatus status) {

        if (parent != null && parent.getParent() != null && QNameUtil.match(parent.getParent().getTypeName(), MappingType.COMPLEX_TYPE)) {
            return super.createWrapper(parent, item, status);
        }
        ExpressionWrapper propertyWrapper = new ExpressionWrapper(parent, item, status);
        getRegistry().registerWrapperPanel(item.getDefinition().getTypeName(), ExpressionPropertyPanel.class);
        return propertyWrapper;
    }

}
