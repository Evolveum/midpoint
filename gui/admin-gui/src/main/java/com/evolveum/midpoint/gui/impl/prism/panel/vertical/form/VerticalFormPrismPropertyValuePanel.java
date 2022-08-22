/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyValuePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;

import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

public class VerticalFormPrismPropertyValuePanel<T> extends PrismPropertyValuePanel<T> {


    public VerticalFormPrismPropertyValuePanel(String id, IModel<PrismPropertyValueWrapper<T>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        Component valuePanel = getValuePanel();
        if (valuePanel instanceof InputPanel) {
            FormComponent baseFormComponent = ((InputPanel) valuePanel).getBaseFormComponent();
            baseFormComponent.add(AttributeAppender.append("class", () -> {
                if (baseFormComponent.hasErrorMessage()) {
                    return "is-invalid";
                }
                return "";
            }));
        }
    }

    protected boolean isRemoveButtonVisible() {
        if (getModelObject() != null && getModelObject().getOldValue() != null
                && getModelObject().getOldValue().getValueMetadata().isSingleValue()) {
            return false;
        }
        return super.isRemoveButtonVisible();
    }

    public void updateFeedbackPanel(AjaxRequestTarget target) {
        target.add(getFeedback());
        Component valuePanel = getValuePanel();
        if (valuePanel instanceof InputPanel) {
            FormComponent baseFormComponent = ((InputPanel) valuePanel).getBaseFormComponent();
            target.add(baseFormComponent);
        }
    }

    protected FeedbackAlerts createFeedbackPanel(String idFeedback) {
        return new FeedbackLabels(idFeedback);
    }

    protected AjaxEventBehavior createEventBehavior() {
        return new AjaxFormComponentUpdatingBehavior("change") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                updateFeedbackPanel(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                updateFeedbackPanel(target);
            }
        };
    }

    protected String getCssClassForValueContainer() {
        return "";
    }
}
