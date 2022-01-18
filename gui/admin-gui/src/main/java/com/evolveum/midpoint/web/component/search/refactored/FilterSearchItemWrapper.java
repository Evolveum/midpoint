/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search.refactored;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchFilterParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class FilterSearchItemWrapper extends AbstractSearchItemWrapper {

    private static final Trace LOGGER = TraceManager.getTrace(FilterSearchItemWrapper.class);
    private SearchItemType searchItem;
    private Class<? extends Containerable> typeClass;

    public FilterSearchItemWrapper(SearchItemType searchItem, Class<? extends Containerable> typeClass) {
        this.searchItem = searchItem;
        this.typeClass = typeClass;
        setApplyFilter(true);
    }

    public Class<FilterSearchItemPanel> getSearchItemPanelClass() {
        return FilterSearchItemPanel.class;
    }

    public String getName() {
        return "";
    }

    public String getHelp() {
        return "";
    }

    public String getTitle() {
        return "";
    }

    @Override
    public DisplayableValue<?> getDefaultValue() {
        return null;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap var) {
        PrismContext ctx = PrismContext.get();
        VariablesMap variables = getFilterVariables();
        if (isEnabled() && isApplyFilter(SearchBoxModeType.BASIC)) {
            SearchFilterType filter = getSearchItem().getFilter();
            if (filter == null && getSearchItem().getFilterExpression() != null) {
                ItemDefinition outputDefinition = ctx.definitionFactory().createPropertyDefinition(
                        ExpressionConstants.OUTPUT_ELEMENT_NAME, SearchFilterType.COMPLEX_TYPE);
                Task task = pageBase.createSimpleTask("evaluate filter expression");
                try {
                    PrismValue filterValue = ExpressionUtil.evaluateExpression(variables, outputDefinition, getSearchItem().getFilterExpression(),
                            MiscSchemaUtil.getExpressionProfile(), pageBase.getExpressionFactory(), "", task, task.getResult());
                    if (filterValue == null || filterValue.getRealValue() == null) {
                        LOGGER.error("FilterExpression return null, ", getSearchItem().getFilterExpression());
                    }
                    filter = filterValue.getRealValue();
                } catch (Exception e) {
                    LOGGER.error("Unable to evaluate filter expression, {} ", getSearchItem().getFilterExpression());
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
        SearchFilterParameterType functionParameter = getSearchItem().getParameter();
        if (functionParameter != null && functionParameter.getType() != null) {
            Class<?> inputClass = PrismContext.get().getSchemaRegistry().determineClassForType(functionParameter.getType());
            TypedValue value = new TypedValue(getValue().getValue(), inputClass);
            variables.put(functionParameter.getName(), value);
        }
        return variables;
    }

    public SearchItemType getSearchItem() {
        return searchItem;
    }
}
