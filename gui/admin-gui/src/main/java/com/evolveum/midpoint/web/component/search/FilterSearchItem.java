/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.util.DisplayableValue;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import java.io.Serializable;

/**
 * @author honchar
 */
public class FilterSearchItem extends SearchItem {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(FilterSearchItem.class);

    public static final String F_APPLY_FILTER = "applyFilter";
    public static final String F_INPUT_VALUE = "input.value";
    public static final String F_INPUT = "input";


    private SearchItemType predefinedFilter;
    private boolean applyFilter;
    private DisplayableValue<? extends Serializable> input = new SearchValue<>();

    public FilterSearchItem(Search search, @NotNull SearchItemType predefinedFilter) {
        super(search);
        Validate.notNull(predefinedFilter, "Filter must not be null.");
        this.predefinedFilter = predefinedFilter;
    }

    @Override
    public String getName() {
        return WebComponentUtil.getTranslatedPolyString(predefinedFilter.getDisplayName());
    }

    @Override
    public Type getType() {
        return Type.FILTER;
    }

    @Override
    protected String getTitle(PageBase pageBase) {
        if (getPredefinedFilter() == null || getPredefinedFilter().getFilter() == null) {
            return null;
        }
        try {
            return pageBase.getPrismContext().xmlSerializer().serializeRealValue(getPredefinedFilter().getFilter());
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize filter", e);
        }
        return null;
    }

    public SearchItemType getPredefinedFilter() {
        return predefinedFilter;
    }

    public void setPredefinedFilter(SearchItemType predefinedFilter) {
        this.predefinedFilter = predefinedFilter;
    }

    public boolean isApplyFilter() {
        return applyFilter;
    }

    public void setApplyFilter(boolean applyFilter) {
        this.applyFilter = applyFilter;
    }

    @Override
    public String getHelp(PageBase pageBase) {
        return predefinedFilter.getDescription();
    }

    public DisplayableValue getInput() {
        return input;
    }

    public void setInput(DisplayableValue<? extends Serializable> input) {
        this.input = input;
    }

    public Type getInputType(Class clazz) {
        if(Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) {
            return Type.BOOLEAN;
        }
        if(Enum.class.isAssignableFrom(clazz)) {
            return Type.ENUM;
        }
        return Type.TEXT;
    }

    @Override
    public String toString() {
        return "FilterSearchItem{" +
                "applyFilter=" + applyFilter +
                ", predefinedFilter=" + predefinedFilter +
                ", input=" + input +
                '}';
    }
}
