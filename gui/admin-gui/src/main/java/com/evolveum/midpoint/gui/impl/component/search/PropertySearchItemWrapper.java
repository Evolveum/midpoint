/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import java.io.Serializable;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class PropertySearchItemWrapper<T extends Serializable> extends AbstractSearchItemWrapper<T> {

    public static final Trace LOGGER = TraceManager.getTrace(PropertySearchItemWrapper.class);

    // TODO consider making these final

    private ItemPath path;

    /**
     * The definition for given item. Usually needed e.g. for account attributes.
     * In other cases not systematically filled-in (yet).
     */
    private final ItemDefinition<?> itemDef;

    private QName valueTypeName;
    private String name;
    private String help;

    public PropertySearchItemWrapper () {
        this.itemDef = null;
    }

    public PropertySearchItemWrapper(ItemPath path) {
        this.path = path;
        this.itemDef = null;
    }

    public PropertySearchItemWrapper(ItemPath path, ItemDefinition<?> itemDef) {
        this.path = path;
        this.itemDef = itemDef;
    }

    @Override
    public Class<? extends AbstractSearchItemPanel> getSearchItemPanelClass() {
        return TextSearchItemPanel.class;
    }

    @Override
    public DisplayableValue<T> getDefaultValue() {
        return new SearchValue<>();
    }

    @Override
    public boolean canRemoveSearchItem() {
        return super.canRemoveSearchItem() && !isResourceRefSearchItem();
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    private boolean isObjectClassSearchItem() {
        return ShadowType.F_OBJECT_CLASS.equivalent(path);
    }

    private boolean isResourceRefSearchItem() {
        return ShadowType.F_RESOURCE_REF.equivalent(path);
    }


    @Override
    public String getTitle() {
        return ""; //todo
    }

//    public ItemDefinition<?> getItemDef() {
//        return itemDef;
//    }
//
//    public void setItemDef(ItemDefinition<?> itemDef) {
//        this.itemDef = itemDef;
//    }

    public ItemPath getPath() {
        return path;
    }

    public void setPath(ItemPath path) {
        this.path = path;
    }

    public QName getValueTypeName() {
        return valueTypeName;
    }

    public void setValueTypeName(QName valueTypeName) {
        this.valueTypeName = valueTypeName;
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        if (getPredefinedFilter() != null) {
            if (!applyPredefinedFilter()) {
                return null;
            }
            SearchFilterType filter = getPredefinedFilter();
            ExpressionType filterExpression = getFilterExpression();
            if (isEnabled()) {
                if (filter == null && filterExpression != null) {
                    ItemDefinition<?> outputDefinition = pageBase.getPrismContext().definitionFactory().createPropertyDefinition(
                            ExpressionConstants.OUTPUT_ELEMENT_NAME, SearchFilterType.COMPLEX_TYPE);
                    Task task = pageBase.createSimpleTask("evaluate filter expression");
                    try {
                        PrismValue filterValue = ExpressionUtil.evaluateExpression(variables, outputDefinition, filterExpression,
                                MiscSchemaUtil.getExpressionProfile(), pageBase.getExpressionFactory(), "", task, task.getResult());
                        if (filterValue == null || filterValue.getRealValue() == null) {
                            LOGGER.error("FilterExpression returned null: {}", filterExpression);
                        } else {
                            filter = filterValue.getRealValue();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Unable to evaluate filter expression, {} ", filterExpression);
                    }
                }
                if (filter != null) {
                    try {
                        //noinspection unchecked
                        ObjectFilter convertedFilter = pageBase.getPrismContext().getQueryConverter().parseFilter(filter, type);
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
        if (getValue().getValue() == null) {
            return null;
        }
        PrismContext ctx = PrismContext.get();
        if (DOMUtil.XSD_INT.equals(valueTypeName)
                || DOMUtil.XSD_INTEGER.equals(valueTypeName)
                || DOMUtil.XSD_LONG.equals(valueTypeName)
                || DOMUtil.XSD_SHORT.equals(valueTypeName)) {

            String text = (String) getValue().getValue();
            if (!StringUtils.isNumeric(text) && (getValue() instanceof SearchValue)) {
                ((SearchValue<?>) getValue()).clear();
                return null;
            }
            Object parsedValue = Long.parseLong((String) getValue().getValue());
            //noinspection unchecked
            return ctx.queryFor(type)
                    .item(path, itemDef).eq(parsedValue).buildFilter();
        } else if (DOMUtil.XSD_STRING.equals(valueTypeName)) {
            String text = (String) getValue().getValue();
            //noinspection unchecked
            return ctx.queryFor(type)
                    .item(path, itemDef).contains(text).matchingCaseIgnore().buildFilter();
        } else if (DOMUtil.XSD_QNAME.equals(valueTypeName)) {
            Object qnameValue = getValue().getValue();
            QName qName;
            if (qnameValue instanceof QName) {
                qName = (QName) qnameValue;
            } else {
                qName = new QName((String) qnameValue);
            }
            //noinspection unchecked
            return ctx.queryFor(type)
                    .item(path, itemDef).eq(qName).buildFilter();
        } else if (SchemaConstants.T_POLY_STRING_TYPE.equals(valueTypeName)) {
                //we're looking for string value, therefore substring filter should be used
                String text = (String) getValue().getValue();
            //noinspection unchecked
            return ctx.queryFor(type)
                    .item(path, itemDef).contains(text).matchingNorm().buildFilter();
        }
        return null;
    }
}
