/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.panel.SingleSearchItemPanel;
import com.evolveum.midpoint.gui.impl.component.search.panel.TextSearchItemPanel;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class PropertySearchItemWrapper<T> extends FilterableSearchItemWrapper<T> {

    public static final Trace LOGGER = TraceManager.getTrace(PropertySearchItemWrapper.class);

    // TODO consider making these final

    private ItemPath path;

    /**
     * The definition for given item. Usually needed e.g. for account attributes.
     * In other cases not systematically filled-in (yet).
     */
    private ItemDefinition<?> itemDef;

    private QName valueTypeName;
    private IModel<String> name = Model.of();
    private IModel<String> help = Model.of();

    public PropertySearchItemWrapper () {
        this.itemDef = null;
    }

    public PropertySearchItemWrapper(ItemPath path) {
        this.path = path;
        this.itemDef = null;
    }

    public PropertySearchItemWrapper(ItemPath path, ItemDefinition<?> itemDef) {
        this.path = path;
        // for, now only shadow attributes definitions are added. the reason is to minimize the session size
        if (path != null && path.startsWith(ShadowType.F_ATTRIBUTES)) {
            this.itemDef = itemDef;
        }
    }

    @Override
    public Class<? extends SingleSearchItemPanel> getSearchItemPanelClass() {
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
    public IModel<String> getName() {
        return name;
    }

    public void setName(IModel<String> name) {
        this.name = name;
    }

    @Override
    public IModel<String> getHelp() {
        return help;
    }

    public void setHelp(IModel<String> help) {
        this.help = help;
    }

    private boolean isObjectClassSearchItem() {
        return ShadowType.F_OBJECT_CLASS.equivalent(path);
    }

    private boolean isResourceRefSearchItem() {
        return ShadowType.F_RESOURCE_REF.equivalent(path);
    }


    @Override
    public IModel<String> getTitle() {
        return Model.of(""); //todo
    }

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
        if (variables == null) {
            variables = new VariablesMap();
        }
        SearchFilterType filter = null;
        ExpressionType filterExpression = getFilterExpression();
        if (filterExpression != null) {
            ItemDefinition<?> outputDefinition = PrismContext.get().definitionFactory().createPropertyDefinition(
                    ExpressionConstants.OUTPUT_ELEMENT_NAME, SearchFilterType.COMPLEX_TYPE);
            Task task = pageBase.createSimpleTask("evaluate filter expression");
            try {
                variables.put(parameterName, new TypedValue(getValue().getValue(), parameterValueType));
                PrismValue filterValue = ExpressionUtil.evaluateExpression(variables, outputDefinition, filterExpression,
                        MiscSchemaUtil.getExpressionProfile(), pageBase.getExpressionFactory(), "", task, task.getResult());
                if (filterValue == null || filterValue.getRealValue() == null) {
                    LOGGER.error("FilterExpression returned null: {}", filterExpression);
                } else {
                    filter = filterValue.getRealValue();
                }
                if (filter != null) {
                    return pageBase.getPrismContext().getQueryConverter().parseFilter(filter, type);
                }
            } catch (Exception e) {
                LOGGER.error("Unable to evaluate filter expression, {} ", filterExpression);
            }

        } else

        if (getPredefinedFilter() != null) {
            return evaluatePredefinedFilter(type, variables, pageBase);
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
        } else if (DOMUtil.XSD_STRING.equals(valueTypeName) || DOMUtil.XSD_ANYURI.equals(valueTypeName)) {
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

    protected ObjectFilter evaluatePredefinedFilter(Class type, VariablesMap variables, PageBase pageBase) {
        if (!applyPredefinedFilter()) {
            return null;
        }
        SearchFilterType filter = getPredefinedFilter();

        if (isEnabled()) {

            if (filter != null) {
                try {
                    //noinspection unchecked
                    if (parameterName != null && parameterValueType != null) {
                        if (variables == null) {
                            variables = new VariablesMap();
                        }

                        // FIXME TODO cleanup needed. While cleaning up, look also at createFilter method.
                        T realValue = getValue().getValue();
                        if (realValue instanceof ObjectReferenceType) {
                            if (((ObjectReferenceType) realValue).getOid() == null) {
                                realValue = null;
                            }
                        }

                        variables.put(parameterName, new TypedValue(realValue, parameterValueType));
                    }
                    ObjectFilter convertedFilter = pageBase.getPrismContext().getQueryConverter().parseFilter(filter, type);
                    convertedFilter = WebComponentUtil.evaluateExpressionsInFilter(convertedFilter, variables, new OperationResult("evaluated filter"), pageBase);
                    if (convertedFilter != null) {
                        return convertedFilter;
                    }
                } catch (Exception e) {
                    LOGGER.error("Unable to parse filter {}, {} ", filter, e);
                }
            }
        }
        return null;
    }

}
