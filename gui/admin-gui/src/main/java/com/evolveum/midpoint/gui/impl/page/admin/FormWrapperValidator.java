/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;

import com.evolveum.midpoint.gui.impl.prism.panel.DefaultContainerablePanel;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.util.visit.IVisit;
import org.apache.wicket.util.visit.IVisitor;
import org.apache.wicket.validation.Validatable;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.web.util.ExpressionValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class FormWrapperValidator<O extends ObjectType> implements IFormValidator {

    private final ModelServiceLocator modelServiceLocator;

    public FormWrapperValidator(@NotNull ModelServiceLocator modelServiceLocator) {
        this.modelServiceLocator = modelServiceLocator;
    }

    @Override
    public FormComponent<?>[] getDependentFormComponents() {
        return null;
    }

    @Override
    public void validate(Form<?> form) {
        List<ItemWrapper> iws = listWrappersWithFormValidator().stream()
                .filter(iw -> iw.getValues() != null)
                .collect(Collectors.toList());

        if (!iws.isEmpty()) {
            form.visitChildren(DefaultContainerablePanel.class, (componentParent, iVisitParent) -> {
                ((DefaultContainerablePanel)componentParent).visitChildren(FormComponent.class, (component, iVisit) -> {
                    FormComponent<?> formComponent = (FormComponent<?>)component;
                    if(formComponent.isVisible() && formComponent.isValid()) {
                        formComponent.updateModel();
                    }
                });
            });
        }

        for (ItemWrapper iw : iws) {
            List<PrismValueWrapper> values = iw.getValues();

            values.forEach(value -> validateItemWrapperWithFormValidator(form, iw, value));
        }
    }

    private List<ItemWrapper> listWrappersWithFormValidator() {
        PrismObjectWrapper pow = getObjectWrapper();

        List<ItemWrapper> iws = new ArrayList<>();
        WebPrismUtil.collectWrappers(pow, iws);

        return iws
                .stream()
                .filter(iw -> iw.getFormComponentValidator() != null)
                .toList();
    }

    private void validateItemWrapperWithFormValidator(Form form, ItemWrapper iw, PrismValueWrapper value) {
        if (iw.isValidated()) {
            return;
        }
        Validatable<Serializable> validatable = new Validatable<>() {

            @Override
            public Serializable getValue() {
                return value.getNewValue().getRealValue();
            }

            @Override
            public IModel<Serializable> getModel() {
                return () -> getValue();
            }
        };

        ExpressionValidator validator = new ExpressionValidator(iw, modelServiceLocator) {

            @Override
            protected ObjectType getObjectType() {
                return getObjectWrapper().getObject().asObjectable();
            }
        };

        validator.validate(validatable);
        if (!validatable.isValid()) {
            validatable.getErrors().forEach(e ->
                    form.error(e.getErrorMessage((key, vars) ->
                            new StringResourceModel(key)
                                    .setModel(new Model<String>())
                                    .setDefaultValue(key)
                                    .getString())));
        }
    }

    private boolean hasError(Form<?> form, String errorMessage) {
        if (!form.hasError()) {
            return false;
        }
        Boolean hasError = form.getFeedbackMessages()
                .messages(new ComponentFeedbackMessageFilter(form))
                .stream()
                .anyMatch(m -> errorMessage.equals(m.getMessage().toString()));
        if (!hasError) {
            hasError = form.visitChildren(Component.class, new IVisitor<Component, Boolean>() {
                @Override
                public void component(final Component component, final IVisit<Boolean> visit) {
                    if (!component.hasErrorMessage()) {
                        return;
                    }
                    boolean componentErrorExists = component.getFeedbackMessages()
                            .messages(new ComponentFeedbackMessageFilter(component))
                            .stream()
                            .anyMatch(m -> (m != null) && errorMessage.equals(m.getMessage().toString()));
                    if (componentErrorExists) {
                        visit.stop(true);
                    }
                }
            });
        }
        return Boolean.TRUE.equals(hasError);
    }

    protected abstract PrismObjectWrapper<O> getObjectWrapper();
}
