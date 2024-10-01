/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel.expression;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;

import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ExpressionWrapper;
import com.evolveum.midpoint.web.component.input.AssociationExpressionValuePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import java.io.Serializable;
import java.util.List;

//TODO resolve Serializable (move select choices for expression to ExpressionWrapper)
@Component
public class AssociationAndExpressionPanelFactory extends AbstractGuiComponentFactory<ExpressionType> implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ExpressionType> panelCtx) {
        ExpressionWrapper expressionWrapper = (ExpressionWrapper) panelCtx.unwrapWrapperModel();

        if (expressionWrapper.isAttributeExpression() || expressionWrapper.isFocusMappingExpression()) {
            return new ExpressionPanel(panelCtx.getComponentId(), (IModel)panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel()) {
                @Override
                protected List<ExpressionPanel.RecognizedEvaluator> getChoices() {
                    return AssociationAndExpressionPanelFactory.this.getChoices(expressionWrapper, super.getChoices());
                }
            };
        }

        return new AssociationExpressionValuePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel(), expressionWrapper.getConstruction());
    }

    protected List<ExpressionPanel.RecognizedEvaluator> getChoices(ExpressionWrapper wrapper, List<ExpressionPanel.RecognizedEvaluator> parentChoices) {
        parentChoices.removeIf(choice ->
                ExpressionPanel.RecognizedEvaluator.ASSOCIATION_FROM_LINK == choice
                || ExpressionPanel.RecognizedEvaluator.SHADOW_OWNER_REFERENCE_SEARCH == choice);
        return parentChoices;
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (wrapper == null) {
            return false;
        }

        if (!(wrapper instanceof ExpressionWrapper)) {
            return false;
        }
        ExpressionWrapper expressionWrapper = (ExpressionWrapper) wrapper;
        return expressionWrapper.isAssociationExpression()
                || expressionWrapper.isAttributeExpression()
                || expressionWrapper.isFocusMappingExpression();
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
