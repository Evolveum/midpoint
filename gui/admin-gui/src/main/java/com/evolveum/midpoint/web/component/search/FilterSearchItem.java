/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

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
    private DisplayableValue<? extends Serializable> input = new SearchValue<>();
    private List<DisplayableValue<?>> allowedValues = null;

    public FilterSearchItem(Search search, @NotNull SearchItemType predefinedFilter) {
        super(search);
        Validate.notNull(predefinedFilter, "Filter must not be null.");
        this.predefinedFilter = predefinedFilter;
        setApplyFilter(false);
    }

    @Override
    public String getName() {
        return WebComponentUtil.getTranslatedPolyString(predefinedFilter.getDisplayName());
    }

    @Override
    public Type getSearchItemType() {
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

    public Type getInputType(Class clazz, PageBase pageBase) {
        if (CollectionUtils.isNotEmpty(getAllowedValues(pageBase))) {
            return Type.ENUM;
        }
        if (Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) {
            return Type.BOOLEAN;
        }
        if (Enum.class.isAssignableFrom(clazz)) {
            return Type.ENUM;
        }
        if (ObjectReferenceType.class.isAssignableFrom(clazz)) {
            return Type.REFERENCE;
        }
        if (ItemPathType.class.isAssignableFrom(clazz)) {
            return Type.ITEM_PATH;
        }
        return Type.TEXT;
    }

    @Override
    public String toString() {
        return "FilterSearchItem{" +
                "applyFilter=" + isApplyFilter() +
                ", predefinedFilter=" + predefinedFilter +
                ", input=" + input +
                '}';
    }

    public List<DisplayableValue<?>> getAllowedValues(PageBase pageBase) {
        if (allowedValues != null) {
            return allowedValues;
        }
        if (predefinedFilter == null) {
            return Collections.EMPTY_LIST;
        }
        List<DisplayableValue<?>> values = WebComponentUtil.getAllowedValues(predefinedFilter.getParameter(), pageBase);
        return values;
    }

    public LookupTableType getLookupTable(PageBase pageBase) {
        if (predefinedFilter != null && predefinedFilter.getParameter() != null
                && predefinedFilter.getParameter().getAllowedValuesLookupTable() != null
                && predefinedFilter.getParameter().getAllowedValuesLookupTable().getOid() != null) {
            PrismObject<LookupTableType> lokupTable = WebComponentUtil.findLookupTable(predefinedFilter.getParameter().getAllowedValuesLookupTable().asReferenceValue(), pageBase);
            if (lokupTable != null) {
                return lokupTable.asObjectable();
            }
        }
        return null;
    }
}
