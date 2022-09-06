/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.autocomplete;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.ComponentLoggerType;
import com.evolveum.midpoint.gui.impl.page.admin.configuration.component.StandardLoggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingComponentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class LoggingPackageAutocompletePanel extends AutoCompleteTextPanel<String> {

    public LoggingPackageAutocompletePanel(String id, IModel<String> model) {
        super(id, model, String.class, false);
    }

    @Override
    public Iterator<String> getIterator(String input) {
        return WebComponentUtil.prepareAutoCompleteList(getLookupTable(), input).iterator();
    }

    @Override
    protected LookupTableType getLookupTable() {
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
