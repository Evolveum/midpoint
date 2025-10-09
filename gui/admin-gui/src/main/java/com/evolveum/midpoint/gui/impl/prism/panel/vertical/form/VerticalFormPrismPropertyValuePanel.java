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

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.util.AiUtil;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.model.IModel;

import java.util.Objects;

public class VerticalFormPrismPropertyValuePanel<T> extends PrismPropertyValuePanel<T> {

    private final static String INVALID_FIELD_CLASS = "is-invalid";

    public VerticalFormPrismPropertyValuePanel(String id, IModel<PrismPropertyValueWrapper<T>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        Component valuePanel = getValuePanel();
        if (valuePanel instanceof InputPanel) {
            ((InputPanel) valuePanel).getFormComponents().forEach((baseFormComponent) -> {
                baseFormComponent.add(AttributeAppender.append("class", () -> {
                    if (baseFormComponent.hasErrorMessage()) {
                        return INVALID_FIELD_CLASS;
                    }
                    return "";
                }));
                baseFormComponent.add(new AjaxFormComponentUpdatingBehavior("change") {

                    private boolean lastValidationWasError = false;

                    @Override
                    protected void onComponentTag(ComponentTag tag) {
                        super.onComponentTag(tag);
                        if (tag.getAttribute("class").contains(INVALID_FIELD_CLASS)) {
                            lastValidationWasError = true;
                        }
                    }

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        if (lastValidationWasError) {
                            lastValidationWasError = false;
                            updateFeedbackPanel(target);
                            target.focusComponent(null);
                        }

                        //TODO improve
                        PrismPropertyValue<T> oldValue = getModelObject().getOldValue();
                        PrismPropertyValue<T> newValue = getModelObject().getNewValue();
                        if (AiUtil.isMarkedAsAiProvided(oldValue)) {
//                            if(!Objects.equals(oldValue.getRealValue(), newValue.getRealValue())){
                                target.add(this.getComponent());
//                            }
                        }

                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, RuntimeException e) {
                        updateFeedbackPanel(target);
                    }
                });

            });
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
            ((InputPanel) valuePanel).getFormComponents().forEach(target::add);
        }
    }

    protected FeedbackAlerts createFeedbackPanel(String idFeedback) {
        return new FeedbackLabels(idFeedback);
    }

    protected String getCssClassForValueContainer() {
        return "";
    }
}
