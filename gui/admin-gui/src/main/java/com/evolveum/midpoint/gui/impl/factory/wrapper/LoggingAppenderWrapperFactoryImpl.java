/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
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
        return super.getOrder() - 10;
    }

    @Override
    protected LookupTableType getPredefinedValues(PrismProperty<T> item, WrapperContext wrapperContext) {
        PrismContainerValue<?> parent = item.getParent();
        if (parent == null || parent.getParent() == null) {
            return null;
        }

        //TODO change matchMethid to be able to check path istead of def???
        PrismContainerable<?> parentParent = parent.getParent();
        if (!(parentParent instanceof PrismContainer)) {
            return null;
        }

        PrismContainerValue<?> parentValue = ((PrismContainer<?>) parentParent).getParent();

        if(parentValue == null || isNotLoggingConfiguration(parentValue)) {
            throw new IllegalArgumentException("LoggingConfigurationType not found in parent for Appender");
        }

        LoggingConfigurationType loggingConfig = parentValue.getRealValue();

        LookupTableType lookupTable = new LookupTableType();
        List<LookupTableRowType> list = lookupTable.createRowList();
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

    private boolean isNotLoggingConfiguration(PrismContainerValue<?> parentValue) {
        return !(parentValue.getRealValue() instanceof LoggingConfigurationType);
    }
}
