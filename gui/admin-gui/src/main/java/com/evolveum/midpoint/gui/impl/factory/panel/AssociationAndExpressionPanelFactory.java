/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ExpressionWrapper;
import com.evolveum.midpoint.web.component.input.AssociationExpressionValuePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

@Component
public class AssociationAndExpressionPanelFactory extends AbstractGuiComponentFactory<ExpressionType> {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<ExpressionType> panelCtx) {
        ExpressionWrapper expressionWrapper = (ExpressionWrapper) panelCtx.unwrapWrapperModel();

        if (expressionWrapper.isAttributeExpression()) {
            return new ExpressionPanel(panelCtx.getComponentId(), (IModel)panelCtx.getItemWrapperModel(), panelCtx.getRealValueModel());
        }

        return new AssociationExpressionValuePanel(panelCtx.getComponentId(), panelCtx.getRealValueModel(), expressionWrapper.getConstruction());
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        if (wrapper == null) {
            return false;
        }

        if (!(wrapper instanceof ExpressionWrapper)) {
            return false;
        }
        ExpressionWrapper expressionWrapper = (ExpressionWrapper) wrapper;
        return expressionWrapper.isAssociationExpression()
                || (expressionWrapper.isAttributeExpression());
    }
}
