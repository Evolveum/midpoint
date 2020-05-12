/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ComponentLoggerType;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.StandardLoggerType;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClassLoggerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author skublik
 *
 */
@Component
public class LoggingPackageWrapperFactoryImpl<T> extends PrismPropertyWrapperFactoryImpl<T>{

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismPropertyDefinition
                && QNameUtil.match(def.getItemName(), ClassLoggerConfigurationType.F_PACKAGE);
    }

    @Override
    public int getOrder() {
        return super.getOrder() - 10;
    }


    public LookupTableType getPredefinedValues(PrismProperty<T> item, WrapperContext wrapperContext) {
        LookupTableType lookupTable = new LookupTableType();
        List<LookupTableRowType> list = lookupTable.createRowList();

        List<StandardLoggerType> standardLoggers = EnumUtils.getEnumList(StandardLoggerType.class);
        for(StandardLoggerType standardLogger : standardLoggers) {
            LookupTableRowType row = new LookupTableRowType();
            row.setKey(standardLogger.getValue());
            row.setValue(standardLogger.getValue());
            PolyStringType label = new PolyStringType("StandardLoggerType." + standardLogger.name());
            PolyStringTranslationType translation = new PolyStringTranslationType();
            translation.setKey("StandardLoggerType." + standardLogger.name());
            label.setTranslation(translation);
            row.setLabel(label);
            list.add(row);
        }

        List<LoggingComponentType> componentLoggers = EnumUtils.getEnumList(LoggingComponentType.class);
        for(LoggingComponentType componentLogger : componentLoggers) {
            LookupTableRowType row = new LookupTableRowType();
                String value = ComponentLoggerType.getPackageByValue(componentLogger);
            row.setKey(value);
            row.setValue(value);
            PolyStringType label = new PolyStringType("LoggingComponentType." + componentLogger.name());
            PolyStringTranslationType translation = new PolyStringTranslationType();
            translation.setKey("LoggingComponentType." + componentLogger.name());
            label.setTranslation(translation);
            row.setLabel(label);
            list.add(row);
        }
        return lookupTable;

    }

}
