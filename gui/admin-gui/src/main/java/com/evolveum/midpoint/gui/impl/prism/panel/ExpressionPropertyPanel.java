/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.util.Collections;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.component.ExpressionTypeSelectPopup;
import com.evolveum.midpoint.gui.impl.prism.panel.component.ExpressionValueTypes;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ExpressionWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * @author katka
 */
public class ExpressionPropertyPanel extends PrismPropertyPanel<ExpressionType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionPropertyPanel.class);

    private static final String ID_HEADER = "header";

    private boolean isExpanded;

    public ExpressionPropertyPanel(String id, IModel<PrismPropertyWrapper<ExpressionType>> model, ItemPanelSettings settings) {
        super(id, model, settings);
        isExpanded = model.getObject() != null && CollectionUtils.isNotEmpty(model.getObject().getValues());
        //todo how to set displayOrder ? to display expression property the last, in the same way as containers
    }

    @Override
    protected Component createHeaderPanel() {
        ExpressionWrapper expressionWrapper = (ExpressionWrapper) getModelObject();
        if (expressionWrapper != null && (expressionWrapper.isAssociationExpression() || expressionWrapper.isAttributeExpression())) {
            return new ExpressionPropertyHeaderPanel(ID_HEADER, getModel()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onExpandClick(AjaxRequestTarget target) {
                    super.onExpandClick(target);
                    isExpanded = !isExpanded;
                    target.add(ExpressionPropertyPanel.this);
                }

                @Override
                protected void addExpressionValuePerformed(AjaxRequestTarget target) {
                    ExpressionPropertyPanel.this.addExpressionValuePerformed(target);
                }

                @Override
                protected void removeExpressionValuePerformed(AjaxRequestTarget target) {
                    ExpressionPropertyPanel.this.getModelObject().getValues().clear();
                    target.add(ExpressionPropertyPanel.this);
                }
            };
        } else {
            return super.createHeaderPanel();
        }
    }

    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<ExpressionType>> item) {
        Component expressionPanel = super.createValuePanel(item);
        expressionPanel.add(new VisibleBehaviour(() -> isExpanded));
        return expressionPanel;
    }

    private void addExpressionValuePerformed(AjaxRequestTarget target) {
        ExpressionWrapper expressionWrapper = (ExpressionWrapper) getModelObject();
        if (expressionWrapper.isAttributeExpression()) {
            expressionValueAddPerformed(target, ExpressionValueTypes.LITERAL_VALUE_EXPRESSION);
        } else {
            ExpressionTypeSelectPopup expressionTypeSelectPopup = new ExpressionTypeSelectPopup(getPageBase().getMainPopupBodyId()) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void addExpressionPerformed(AjaxRequestTarget target, ExpressionValueTypes expressionType) {
                    expressionValueAddPerformed(target, expressionType);
                }
            };
            getPageBase().showMainPopup(expressionTypeSelectPopup, target);
        }
    }

    private void expressionValueAddPerformed(AjaxRequestTarget target, ExpressionValueTypes expressionType) {
        getPageBase().hideMainPopup(target);
        try {
            ExpressionType newExpressionValue = new ExpressionType();
            if (ExpressionValueTypes.SHADOW_REF_EXPRESSION.equals(expressionType)) {
                ExpressionUtil.addShadowRefEvaluatorValue(newExpressionValue, null, getPrismContext());
            } else if (ExpressionValueTypes.ASSOCIATION_TARGET_SEARCH_EXPRESSION.equals(expressionType)) {
                ExpressionUtil.getOrCreateAssociationTargetSearchValues(newExpressionValue, getPrismContext());
            } else if (ExpressionValueTypes.LITERAL_VALUE_EXPRESSION.equals(expressionType)) {
                ExpressionUtil.updateLiteralExpressionValue(newExpressionValue, Collections.singletonList(""), getPrismContext());
            }

            WrapperContext context = new WrapperContext(null, null);
            PrismPropertyValue<ExpressionType> expressionValue = getPageBase().getPrismContext().itemFactory().createPropertyValue(newExpressionValue);
            PrismPropertyValueWrapper<ExpressionType> newExpressionValueWrapper = getPageBase().createValueWrapper(getModelObject(), expressionValue, ValueStatus.ADDED, context);

            getModelObject().getValues().clear();
            getModelObject().getValues().add(newExpressionValueWrapper);
            getModelObject().getItem().setRealValue(newExpressionValue);
        } catch (SchemaException ex) {
            LOGGER.error("Unable to create new expression value: {}", ex.getLocalizedMessage());
        }

        target.add(ExpressionPropertyPanel.this);

    }

}
