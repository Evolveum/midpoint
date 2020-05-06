/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.List;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author skublik
 *
 */
@Component
public class LoggingAppenderWrapperFactoryImpl<T> extends PrismPropertyWrapperFactoryImpl<T>{

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismPropertyDefinition
                && QNameUtil.match(def.getItemName(), ClassLoggerConfigurationType.F_APPENDER);
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE-1;
    }

    @Override
    protected PrismPropertyWrapper<T> createWrapper(PrismContainerValueWrapper<?> parent, PrismProperty<T> item,
            ItemStatus status, WrapperContext ctx) {
        getRegistry().registerWrapperPanel(item.getDefinition().getTypeName(), PrismPropertyPanel.class);
        PrismPropertyWrapper<T> propertyWrapper = new PrismPropertyWrapperImpl<>(parent, item, status);
        propertyWrapper.setPredefinedValues(getPredefinedValues(parent));
        return propertyWrapper;
    }

    private LookupTableType getPredefinedValues(PrismContainerValueWrapper<?> parent) {
        LookupTableType lookupTable = new LookupTableType();
        List<LookupTableRowType> list = lookupTable.createRowList();

        if(parent == null || parent.getParent() == null || parent.getParent().getParent() == null) {
            return lookupTable;
        }


        if(!(parent.getParent().getParent().getRealValue() instanceof LoggingConfigurationType)) {
            throw new IllegalArgumentException("LoggingConfigurationType not found in parent for Appender");
        }

        LoggingConfigurationType loggingConfig = (LoggingConfigurationType) parent.getParent().getParent().getRealValue();

        for (AppenderConfigurationType appender : loggingConfig.getAppender()) {
                LookupTableRowType row = new LookupTableRowType();
                String name = appender.getName();
                row.setKey(name);
                row.setValue(name);
                row.setLabel(new PolyStringType(name));
                list.add(row);
        }
        return lookupTable;
    }

}
