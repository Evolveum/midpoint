/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.util.List;

import com.evolveum.midpoint.web.component.input.validator.NotNullValidator;

import com.evolveum.midpoint.web.util.ExpressionValidator;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.util.convert.IConverter;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;

/**
 * abstract factory for all InputPanel panels
 * @param <T>
 */
public abstract class AbstractInputGuiComponentFactory<T> implements GuiComponentFactory<PrismPropertyPanelContext<T>> {

    @Autowired private GuiComponentRegistry componentRegistry;

    public GuiComponentRegistry getRegistry() {
        return componentRegistry;
    }

    @Override
    public Component createPanel(PrismPropertyPanelContext<T> panelCtx) {
        InputPanel panel = getPanel(panelCtx);
        return panel;
    }

    @Override
    public void configure(PrismPropertyPanelContext<T> panelCtx, Component component) {
        if (!(component instanceof InputPanel)) {
            return;
        }
        InputPanel panel = (InputPanel) component;
        final List<FormComponent> formComponents = panel.getFormComponents();
        for (FormComponent<T> formComponent : formComponents) {
            PrismPropertyWrapper<T> propertyWrapper = panelCtx.unwrapWrapperModel();
            IModel<String> label = LambdaModel.of(propertyWrapper::getDisplayName);
            formComponent.setLabel(label);
            if (panelCtx.isMandatory()) {
                formComponent.add(new NotNullValidator<>("Required"));
            }

            if (formComponent instanceof TextField) {
                formComponent.add(new AttributeModifier("size", "42"));
            }
            formComponent.add(panelCtx.getVisibleEnableBehavior());
            if (panelCtx.getAttributeValuesMap() != null) {
                panelCtx.getAttributeValuesMap().keySet().stream()
                        .forEach(a -> formComponent.add(AttributeAppender.replace(a, panelCtx.getAttributeValuesMap().get(a))));
            }
        }

        if (panelCtx.getAjaxEventBehavior() != null) {
            panel.getBaseFormComponent().add(panelCtx.getAjaxEventBehavior());
        }

        ExpressionValidator ev = panelCtx.getExpressionValidator();
        if (ev != null) {
            panel.getValidatableComponent().add(ev);
        }
        panelCtx.getFeedback().setFilter(new ComponentFeedbackMessageFilter(panel.getValidatableComponent()));

    }

    @Override
    public Integer getOrder() {
        return Integer.MAX_VALUE;
    }

    protected abstract InputPanel getPanel(PrismPropertyPanelContext<T> panelCtx);
}
