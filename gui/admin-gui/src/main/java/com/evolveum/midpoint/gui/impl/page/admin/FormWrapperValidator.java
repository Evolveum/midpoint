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

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
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

        for (ItemWrapper iw : iws) {
            List<PrismValueWrapper> values = iw.getValues();

            values.forEach(value -> validateItemWrapperWithFormValidator(form, iw, value));
        }
    }

    private List<ItemWrapper> listWrappersWithFormValidator() {
        PrismObjectWrapper pow = getObjectWrapper();

        List<ItemWrapper> iws = new ArrayList<>();
        visitWrapper(pow, iws);

        return iws;
    }

    private void visitWrapper(ItemWrapper iw, List<ItemWrapper> iws) {
        if (iw.getFormComponentValidator() != null) {
            iws.add(iw);
        }

        if (!(iw instanceof PrismContainerWrapper)) {
            return;
        }

        PrismContainerWrapper pcw = (PrismContainerWrapper) iw;
        List<PrismContainerValueWrapper> pcvws = pcw.getValues();
        if (pcvws == null) {
            return;
        }

        pcvws.forEach(pcvw -> {
            pcvw.getItems().forEach(childIW -> {
                visitWrapper((ItemWrapper) childIW, iws);
            });
        });
    }

    private void validateItemWrapperWithFormValidator(Form form, ItemWrapper iw, PrismValueWrapper value) {
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

        ExpressionValidator validator = new ExpressionValidator(
                LambdaModel.of(() -> iw.getFormComponentValidator()), modelServiceLocator) {

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

    protected abstract PrismObjectWrapper<O> getObjectWrapper();
}
