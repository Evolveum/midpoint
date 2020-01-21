/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.component;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.model.ReadOnlyValueModel;
import com.evolveum.midpoint.gui.impl.factory.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.*;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.AssociationExpressionValuePanel;
import com.evolveum.midpoint.web.component.input.SimpleValueExpressionPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.Arrays;

/**
 * @author katka
 *
 */
public class ExpressionPropertyPanel extends PrismPropertyPanel<ExpressionType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionPropertyPanel.class);

    private static final String ID_EXPRESSION_PANEL = "expressionPanel";
    private static final String ID_HEADER = "header";

    private boolean isExpanded;

    public ExpressionPropertyPanel(String id, IModel<PrismPropertyWrapper<ExpressionType>> model, ItemPanelSettings settings) {
        super(id, model, settings);
        isExpanded = model.getObject() != null && CollectionUtils.isNotEmpty(model.getObject().getValues());
        //todo how to set displayOrder ? to display expression property the last, in the same way as containers
    }

    @Override
    protected Panel createHeaderPanel() {
        ExpressionWrapper expressionWrapper = (ExpressionWrapper) getModelObject();
        if (expressionWrapper != null && (expressionWrapper.isAssociationExpression() || expressionWrapper.isAttributeExpression())){
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
                protected void removeExpressionValuePerformed(AjaxRequestTarget target){
                        ExpressionPropertyPanel.this.getModelObject().getValues().clear();
                        target.add(ExpressionPropertyPanel.this);
                }
            };
        } else {
            return super.createHeaderPanel();
        }
    }

    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<ExpressionType>> item, GuiComponentFactory factory, ItemVisibilityHandler visibilityHandler) {
        ExpressionWrapper expressionWrapper = (ExpressionWrapper) getModelObject();
        Component expressionPanel = null;
        if (expressionWrapper != null && (expressionWrapper.isAssociationExpression() || expressionWrapper.isAttributeExpression())) {
            ItemRealValueModel<ExpressionType> realValueModel = new ItemRealValueModel<ExpressionType>(item.getModel());
            if (expressionWrapper.isAttributeExpression()) {
                expressionPanel = new SimpleValueExpressionPanel(ID_EXPRESSION_PANEL, realValueModel);
            } else if (expressionWrapper.isAssociationExpression()) {
                expressionPanel = new AssociationExpressionValuePanel(ID_EXPRESSION_PANEL, realValueModel, expressionWrapper.getConstruction());
            }
            expressionPanel.setOutputMarkupId(true);
            expressionPanel.add(new VisibleBehaviour(() -> isExpanded));
            item.add(expressionPanel);
        } else {
            expressionPanel = new TextPanel<ExpressionType>(ID_EXPRESSION_PANEL, Model.of(getModelObject().getItem().getRealValue()));
            expressionPanel.add(new VisibleBehaviour(() -> isExpanded));
            item.add(expressionPanel);
        }

        return expressionPanel;
    }

    private void addExpressionValuePerformed(AjaxRequestTarget target){
        ExpressionWrapper expressionWrapper = (ExpressionWrapper) getModelObject();
        if (expressionWrapper.isAttributeExpression()){
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

    private void expressionValueAddPerformed(AjaxRequestTarget target, ExpressionValueTypes expressionType){
        getPageBase().hideMainPopup(target);
        try {
            ExpressionType newExpressionValue = new ExpressionType();
            if (ExpressionValueTypes.SHADOW_REF_EXPRESSION.equals(expressionType)){
                ExpressionUtil.addShadowRefEvaluatorValue(newExpressionValue, null, getPrismContext());
            } else if (ExpressionValueTypes.ASSOCIATION_TARGET_SEARCH_EXPRESSION.equals(expressionType)){
                ExpressionUtil.getOrCreateAssociationTargetSearchValues(newExpressionValue, getPrismContext());
            } else if (ExpressionValueTypes.LITERAL_VALUE_EXPRESSION.equals(expressionType)){
                ExpressionUtil.updateLiteralExpressionValue(newExpressionValue, Arrays.asList(""), getPrismContext());
            }

            WrapperContext context = new WrapperContext(null, null);
            PrismPropertyValue<ExpressionType> expressionValue = getPageBase().getPrismContext().itemFactory().createPropertyValue(newExpressionValue);
            PrismPropertyValueWrapper<ExpressionType> newExpressionValueWrapper = (PrismPropertyValueWrapper<ExpressionType>) getPageBase()
                    .createValueWrapper(getModelObject(), expressionValue, ValueStatus.ADDED, context);

            getModelObject().getValues().clear();
            getModelObject().getValues().add(newExpressionValueWrapper);
            getModelObject().getItem().setRealValue(newExpressionValue);
        } catch (SchemaException ex){
            LOGGER.error("Unable to create new expression value: {}", ex.getLocalizedMessage());
        }

        target.add(ExpressionPropertyPanel.this);

    }

    @Override
    protected void createButtons(ListItem<PrismPropertyValueWrapper<ExpressionType>> item) {
        //nothing to do.. buttons are in the prism container panel header/ prism container value header
    }

}
