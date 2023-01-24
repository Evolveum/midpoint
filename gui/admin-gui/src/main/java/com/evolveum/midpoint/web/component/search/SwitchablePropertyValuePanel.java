/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.component.search.panel.ReferenceValueSearchPanel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.ExpressionModel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.search.filter.ValueSearchFilterItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * @author Kateryna Honchar
 */
public class SwitchablePropertyValuePanel extends BasePanel<SelectableBean<ValueSearchFilterItem>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_PROPERTY_VALUE_PANEL = "propertyValuePanel";
    private static final String ID_VALUE_FIELD = "valueField";
    private static final String ID_EXPRESSION_FIELD = "expressionField";
    private static final String ID_SWITCH_BUTTON = "switchButton";

    private boolean isExpressionMode;
    private ExpressionWrapper tempExpressionWrapper;
    private Object tempValue;

    public SwitchablePropertyValuePanel(String id, IModel<SelectableBean<ValueSearchFilterItem>> model) {
        super(id, model);
        isExpressionMode = getExpressionWrapper() != null;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer valueContainer = new WebMarkupContainer(ID_PROPERTY_VALUE_PANEL);
        valueContainer.setOutputMarkupId(true);
        add(valueContainer);

        Component valueField = getValueField(ID_VALUE_FIELD);
        valueField.add(new VisibleBehaviour(() -> !isExpressionMode));
        valueContainer.add(valueField);

        ExpressionWrapper expression = getExpressionWrapper();
        ExpressionType expressionType = null;
        if (expression != null) {
            Object expressionValue = expression.getExpression();
            if (expressionValue instanceof ExpressionType) {
                expressionType = (ExpressionType) expressionValue;
            }
        }
        AceEditorPanel expressionField = new AceEditorPanel(ID_EXPRESSION_FIELD, null,
                new ExpressionModel(Model.of(expressionType), getPageBase()), 200);

        expressionField.getEditor().add(new EmptyOnBlurAjaxFormUpdatingBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getModelObject().getValue().setExpression(
                        new ExpressionWrapper(getPropertyItemDefinition().getItemName(),
                                ((ExpressionModel)expressionField.getModel()).getBaseModel().getObject()));

            }
        });
        expressionField.getEditor().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        expressionField.add(new VisibleBehaviour(() -> isExpressionMode));
        valueContainer.add(expressionField);

        AjaxButton switchButton = new AjaxButton(ID_SWITCH_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (isExpressionMode) {
                    tempExpressionWrapper = SwitchablePropertyValuePanel.this.getModelObject().getValue().getExpression();
                    SwitchablePropertyValuePanel.this.getModelObject().getValue().setExpression(null);
                    SwitchablePropertyValuePanel.this.getModelObject().getValue().setValue(tempValue);
//                    if (isReferenceFilterValue()) {
//                        SwitchablePropertyValuePanel.this.getModelObject().getValue().setValue(new ObjectReferenceType());
//                    }
                } else {
                    tempValue = SwitchablePropertyValuePanel.this.getModelObject().getValue().getValue();
                    SwitchablePropertyValuePanel.this.getModelObject().getValue().setExpression(tempExpressionWrapper);
                    SwitchablePropertyValuePanel.this.getModelObject().getValue().setValue(null);
                }
                isExpressionMode = !isExpressionMode;
                target.add(SwitchablePropertyValuePanel.this);
            }

        };
        switchButton.setOutputMarkupId(true);
        switchButton.add(new VisibleBehaviour(() -> {
            ItemDefinition propertyDef = getPropertyItemDefinition();
            return propertyDef == null || propertyDef.getTypeClass() != null && !boolean.class.equals(propertyDef.getTypeClass()) && !Boolean.class.isAssignableFrom(propertyDef.getTypeClass());
        }));
        switchButton.add(AttributeAppender.append("title", new LoadableModel<String>() {
            @Override
            protected String load() {
                return ""; //todo
            }
        }));
        valueContainer.add(switchButton);

    }

    private <T> Component getValueField(String id) {
        Component searchItemField = null;
        ItemDefinition propertyDef = getPropertyItemDefinition();
        if (propertyDef != null) {
            PrismObject<LookupTableType> lookupTable = WebComponentUtil.findLookupTable(propertyDef, getPageBase());
            if (propertyDef instanceof PrismReferenceDefinition) {
                searchItemField = new ReferenceValueSearchPanel(id, new PropertyModel<>(getModel(), "value.value"),
                        (PrismReferenceDefinition) propertyDef) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void referenceValueUpdated(ObjectReferenceType ort, AjaxRequestTarget target) {
                        SwitchablePropertyValuePanel.this.getModelObject().getValue().setValue(ort);
                    }
                };
            } else if (propertyDef instanceof PrismPropertyDefinition) {
                List<DisplayableValue> allowedValues = new ArrayList<>();
                if (((PrismPropertyDefinition) propertyDef).getAllowedValues() != null) {
                    allowedValues.addAll(((PrismPropertyDefinition) propertyDef).getAllowedValues());
                } else if (propertyDef.getTypeClass().equals(boolean.class) || Boolean.class.isAssignableFrom(propertyDef.getTypeClass())) {
                    allowedValues.add(new SearchValue<>(Boolean.TRUE, getString("Boolean.TRUE")));
                    allowedValues.add(new SearchValue<>(Boolean.FALSE, getString("Boolean.FALSE")));
                }
                if (lookupTable != null) {
                    searchItemField = new AutoCompleteTextPanel<String>(id,
                            new PropertyModel<>(getModel(), "value." + ValueSearchFilterItem.F_VALUE), String.class,
                            true, lookupTable.asObjectable()) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public Iterator<String> getIterator(String input) {
                            return WebComponentUtil.prepareAutoCompleteList(lookupTable.asObjectable(), input).iterator();
                        }
                    };
                } else if (CollectionUtils.isNotEmpty(allowedValues)) {
                    List<T> allowedValuesList = new ArrayList<>();
                    allowedValues.forEach(val -> allowedValuesList.add((T) val.getValue()));
                    searchItemField = new DropDownChoicePanel<T>(id,
                            new PropertyModel<>(getModel(), "value." + ValueSearchFilterItem.F_VALUE),
                            Model.ofList(allowedValuesList), new IChoiceRenderer<T>() {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public Object getDisplayValue(T val) {
                            if (val instanceof DisplayableValue) {
                                return ((DisplayableValue) val).getLabel();
                            }
                            return val;
                        }

                        @Override
                        public String getIdValue(T val, int index) {
                            return Integer.toString(index);
                        }

                        @Override
                        public T getObject(String id, IModel<? extends List<? extends T>> choices) {
                            return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
                        }
                    }, true);
                } else {
                    searchItemField = new TextPanel<String>(id, new PropertyModel<>(getModel(), "value." + ValueSearchFilterItem.F_VALUE));

                }
            }
        }
        if (searchItemField instanceof InputPanel) {
            ((InputPanel) searchItemField).getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        }
        return searchItemField != null ? searchItemField : new WebMarkupContainer(id);
    }

    private ItemDefinition getPropertyItemDefinition() {
        ValueSearchFilterItem value = getModelObject().getValue();
        return value != null ? value.getPropertyDef() : null;

    }

    private boolean isReferenceFilterValue() {
        ValueSearchFilterItem valueSearchFilter = getModelObject().getValue();
        ItemDefinition propertyDef = valueSearchFilter.getPropertyDef();
        return propertyDef instanceof PrismReferenceDefinition;
    }

    private ExpressionWrapper getExpressionWrapper() {
        SelectableBean<ValueSearchFilterItem> filterModelObj = getModelObject();
        if (filterModelObj == null || filterModelObj.getValue() == null || filterModelObj.getValue().getExpression() == null) {
            return null;
        }
        return filterModelObj.getValue().getExpression();
    }
}
