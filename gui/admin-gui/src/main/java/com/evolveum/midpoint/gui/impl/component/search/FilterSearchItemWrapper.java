/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import javax.xml.namespace.QName;
import java.io.Serializable;

public class FilterSearchItemWrapper extends AbstractSearchItemWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(FilterSearchItemWrapper.class);
    private SearchFilterType filter;
    private ExpressionType filterExpression;
    private String name;
    private String help;
    private QName filterParameterType;
    private String filterParameterName;
    private ExpressionType allowedValuesExpression;
    private String allowedValuesLookupTableOid;
    private Class<? extends Containerable> typeClass;

    public FilterSearchItemWrapper(SearchItemType searchItem, Class<? extends Containerable> typeClass) {
        this.filter = searchItem.getFilter();
        filterExpression = searchItem.getFilterExpression();
        initName(searchItem);
        initHelp(searchItem);
        this.typeClass = typeClass;
        if (searchItem.getParameter() != null) {
            filterParameterType = searchItem.getParameter().getType();
            filterParameterName = searchItem.getParameter().getName();
            allowedValuesExpression = searchItem.getParameter().getAllowedValuesExpression();
            allowedValuesLookupTableOid = searchItem.getParameter().getAllowedValuesLookupTable() != null ?
                    searchItem.getParameter().getAllowedValuesLookupTable().getOid() : null;
        }
        setApplyFilter(true);
        setVisible(true);
    }

    public Class<FilterSearchItemPanel> getSearchItemPanelClass() {
        return FilterSearchItemPanel.class;
    }

    private void initName(SearchItemType searchItem) {
        name = searchItem.getDisplayName() != null ? WebComponentUtil.getTranslatedPolyString(searchItem.getDisplayName()) : null;
        if (name == null && searchItem.getParameter() != null) {
            DisplayType displayType = searchItem.getParameter().getDisplay();
            if (displayType != null) {
                name = WebComponentUtil.getTranslatedPolyString(displayType.getLabel());
            }
        }
    }

    private void initHelp(SearchItemType searchItem) {
        help = searchItem.getDescription();
        if (help == null && searchItem.getParameter() != null) {
            DisplayType displayType = searchItem.getParameter().getDisplay();
            if (displayType != null) {
                help = WebComponentUtil.getTranslatedPolyString(displayType.getHelp());
            }
        }
    }

    @Override
    public String getName() {
        return name != null ? name : "";
    }

    public String getHelp() {
        return help != null ? help : "";
    }

    public String getTitle() {
        return "";
    }

    public DisplayableValue getInput() {
        return getValue();
    }

    public void setInput(DisplayableValue<? extends Serializable> input) {
        setValue(input);
    }

    public SearchFilterType getFilter() {
        return filter;
    }

    public QName getFilterParameterType() {
        return filterParameterType;
    }

    public String getFilterParameterName() {
        return filterParameterName;
    }

    public ExpressionType getAllowedValuesExpression() {
        return allowedValuesExpression;
    }

    public String getAllowedValuesLookupTableOid() {
        return allowedValuesLookupTableOid;
    }

    @Override
    public DisplayableValue<?> getDefaultValue() {
        return null;
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        PrismContext ctx = PrismContext.get();
        variables = getFilterVariables();   //todo which variables to use?
        if (isEnabled() && isApplyFilter(SearchBoxModeType.BASIC)) {
            if (filter == null && filterExpression != null) {
                ItemDefinition outputDefinition = ctx.definitionFactory().createPropertyDefinition(
                        ExpressionConstants.OUTPUT_ELEMENT_NAME, SearchFilterType.COMPLEX_TYPE);
                Task task = pageBase.createSimpleTask("evaluate filter expression");
                try {
                    PrismValue filterValue = ExpressionUtil.evaluateExpression(variables, outputDefinition, filterExpression,
                            MiscSchemaUtil.getExpressionProfile(), pageBase.getExpressionFactory(), "", task, task.getResult());
                    if (filterValue == null || filterValue.getRealValue() == null) {
                        LOGGER.error("FilterExpression return null, ", filterExpression);
                    }
                    filter = filterValue.getRealValue();
                } catch (Exception e) {
                    LOGGER.error("Unable to evaluate filter expression, {} ", filterExpression);
                }
            }
            if (filter != null) {
                try {
                    ObjectFilter convertedFilter = ctx.getQueryConverter().parseFilter(filter, typeClass);
                    convertedFilter = WebComponentUtil.evaluateExpressionsInFilter(convertedFilter, variables, new OperationResult("evaluated filter"), pageBase);
                    if (convertedFilter != null) {
                        return convertedFilter;
                    }
                } catch (SchemaException e) {
                    LOGGER.error("Unable to parse filter {}, {} ", filter, e);
                }
            }
        }
        return null;
    }

    public VariablesMap getFilterVariables() {
        VariablesMap variables = new VariablesMap();
        if (filterParameterType != null) {
            TypedValue value;
            if (getInput() == null || getInput().getValue() == null) {
                Class<?> inputClass = PrismContext.get().getSchemaRegistry().determineClassForType(filterParameterType);
                value = new TypedValue(null, inputClass);
            } else {
                value = new TypedValue(getInput().getValue(), getInput().getValue().getClass());
            }
            variables.put(filterParameterName, value);
        }
        return variables;
    }

}
