/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.autocomplete;

import java.util.Iterator;
import java.util.List;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AppenderConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class AppenderAutocompletePanel extends AutoCompleteTextPanel<String> {

    private final IModel<PrismPropertyValueWrapper<String>> valueWrapperModel;

    public AppenderAutocompletePanel(String id, IModel<String> model, IModel<PrismPropertyValueWrapper<String>> valueWrapperModel) {
        super(id, model, String.class, false);
        this.valueWrapperModel = valueWrapperModel;
    }

    @Override
    public Iterator<String> getIterator(String input) {
        return WebComponentUtil.prepareAutoCompleteList(getLookupTable(), input).iterator();
    }

    @Override
    protected LookupTableType getLookupTable() {
        PrismPropertyWrapper<String> itemWrapper = valueWrapperModel.getObject().getParent();
        if (itemWrapper == null) {
            return null;
        }
        PrismProperty<String> item = itemWrapper.getItem();
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
        if (loggingConfig == null) {
            return null;
        }

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

    @Override
    protected void onDetach() {
        valueWrapperModel.detach();
        super.onDetach();
    }
}
