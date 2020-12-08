/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
    private List<DisplayableValue<?>> allowedValues = null;

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
                "applyFilter=" + applyFilter +
                ", predefinedFilter=" + predefinedFilter +
                ", input=" + input +
                '}';
    }

    public List<DisplayableValue<?>> getAllowedValues(PageBase pageBase) {
        if (allowedValues != null) {
            return allowedValues;
        }

        if (predefinedFilter == null || predefinedFilter.getParameter() == null
                || predefinedFilter.getParameter().getAllowedValuesExpression() == null) {
            return null;
        }
        Task task = pageBase.createSimpleTask("evaluate expression for allowed values");
        ExpressionType expression = predefinedFilter.getParameter().getAllowedValuesExpression();
        Object value = null;
        try {

            value = ExpressionUtil.evaluateExpression(new ExpressionVariables(), null,
                    expression, MiscSchemaUtil.getExpressionProfile(),
                    pageBase.getExpressionFactory(), "evaluate expression for allowed values", task, task.getResult());
        } catch (Exception e) {
            LOGGER.error("Couldn't execute expression " + expression, e);
            pageBase.error(pageBase.createStringResource("FilterSearchItem.message.error.evaluateAllowedValuesExpression", expression).getString());
            return null;
        }
        if (value instanceof PrismPropertyValue) {
            value = ((PrismPropertyValue) value).getRealValue();
        }

        if (!(value instanceof List)) {
            LOGGER.error("Exception return unexpected type, expected List<DisplayableValue>, but was " + (value == null ? null : value.getClass()));
            pageBase.error(pageBase.createStringResource("FilterSearchItem.message.error.wrongType", expression).getString());
            return null;
        }

        if (!((List<?>) value).isEmpty()){
            if (!(((List<?>) value).get(0) instanceof DisplayableValue)) {
                LOGGER.error("Exception return unexpected type, expected List<DisplayableValue>, but was " + (value == null ? null : value.getClass()));
                pageBase.error(pageBase.createStringResource("FilterSearchItem.message.error.wrongType", expression).getString());
                return null;
            }
        }
        this.allowedValues = (List<DisplayableValue<?>>) value;
        return (List<DisplayableValue<?>>) value;
    }

    public LookupTableType getLookupTable() {
//        if (predefinedFilter != null && predefinedFilter.getParameter() != null) {
//            return predefinedFilter.getParameter().getAllowedValuesLookupTable();
//        }
        return null;
    }
}
