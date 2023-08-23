/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RangeType;

import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidatorAdapter;

public class RangeSimplePanel extends InputPanel {

    private static final String ID_CONTAINER = "container";
    private static final String ID_MIN = "min";
    private static final String ID_MAX = "max";

    IModel<PrismPropertyValueWrapper<RangeType>> model;

    double max;

    public RangeSimplePanel(String id, IModel<PrismPropertyValueWrapper<RangeType>> model, double max) {
        super(id);
        this.model = model;
        this.max = max;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);

        TextField<Double> minField = new TextField<>(ID_MIN,
                new PropertyModel<>(new ItemRealValueModel<>(getModel()), "min"));
        minField.setOutputMarkupId(true);
        minField.setEnabled(isEnabled());
        minField.add(new ValidatorAdapter<>((var) -> {
            Double value = var.getValue();
            if (value > maximumValue()) {
                var.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.positive.exception"));
            } else if (value < minimumValue()) {
                var.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.negative.exception"));
            } else if (value > getMaxField().getModelObject()) {
                var.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.bigger.exception"));
            }
        }));
        container.add(minField);

        TextField<Double> maxField = new TextField<>(ID_MAX,
                new PropertyModel<>(new ItemRealValueModel<>(getModel()), "max"));
        maxField.setOutputMarkupId(true);
        maxField.setEnabled(isEnabled());

        maxField.add(new ValidatorAdapter<>((var) -> {
            Double value = var.getValue();
            if (value > maximumValue()) {
                var.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.positive.exception"));
            } else if (value < minimumValue()) {
                var.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.negative.exception"));
            } else if (value < getMinField().getModelObject()) {
                var.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.lower.exception"));
            }
        }));
        container.add(maxField);

        add(container);

    }


    @Override
    public boolean isEnabled() {
        return super.isEnabled();
    }

    private TextField<Double> getMinField() {
        return (TextField<Double>) get(getPageBase().createComponentPath(ID_CONTAINER, ID_MIN));
    }

    private TextField<Double> getMaxField() {
        return (TextField<Double>) get(getPageBase().createComponentPath(ID_CONTAINER, ID_MAX));
    }

    public Double maximumValue() {
        return max;
    }

    public Double minimumValue() {
        return 0.0;
    }

    public IModel<PrismPropertyValueWrapper<RangeType>> getModel() {
        return model;
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return null;
    }

    @Override
    public List<FormComponent> getFormComponents() {
        return List.of(getMinField(),
                getMaxField());
    }

}
