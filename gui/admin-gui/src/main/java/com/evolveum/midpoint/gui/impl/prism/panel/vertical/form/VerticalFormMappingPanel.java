/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.impl.component.message.FeedbackLabels;
import com.evolveum.midpoint.web.component.input.validator.NotNullValidator;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.prism.Containerable;

import org.apache.wicket.validation.ValidatorAdapter;

import java.util.concurrent.atomic.AtomicReference;

public class VerticalFormMappingPanel<C extends MappingType> extends VerticalFormDefaultContainerablePanel<C> {

    public VerticalFormMappingPanel(String id, IModel<PrismContainerValueWrapper<C>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected boolean isVisibleSubContainer(PrismContainerWrapper<? extends Containerable> c) {
        return true;
    }

    @Override
    protected boolean isVisibleSubContainerHeader(PrismContainerWrapper<? extends Containerable> c) {
        return false;
    }

    @Override
    protected boolean isShowEmptyButtonVisible() {
        return false;
    }

    protected String getCssClassForFormSubContainer() {
        return "m-0";
    }

    protected String getCssClassForFormSubContainerOfValuePanel() {
        return "card-body mb-0 px-3 pt-0 pb-3";
    }

    @Override
    protected String getCssClassForFormContainer() {
        return "p-0";
    }

    @Override
    protected boolean isRemoveValueButtonVisible() {
        return false;
    }

    public boolean isFormValid(AjaxRequestTarget target) {
        AtomicReference<Boolean> valid = new AtomicReference<>(true);

        this.visitChildren(FormComponent.class, (component, visit) -> {
            if (!component.hasErrorMessage()) {
                component.getBehaviors().stream()
                        .filter(ValidatorAdapter.class::isInstance)
                        .map(ValidatorAdapter.class::cast)
                        .map(ValidatorAdapter::getValidator)
                        .filter(NotNullValidator.class::isInstance)
                        .map(NotNullValidator.class::cast)
                        .forEach(validator -> validator.setUseModel(true));

                ((FormComponent<?>) component).validate();
            }

            if (!component.hasErrorMessage()) {
                return;
            }

            valid.set(false);
            target.add(component);

            InputPanel inputPanel = component.findParent(InputPanel.class);
            if (inputPanel != null && inputPanel.getParent() != null) {
                target.addChildren(inputPanel.getParent(), FeedbackLabels.class);
            }
        });

        return valid.get();
    }
}
