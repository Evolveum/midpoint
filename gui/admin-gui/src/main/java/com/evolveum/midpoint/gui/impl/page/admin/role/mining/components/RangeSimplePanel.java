/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RangeDto;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RangeType;

import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.IValidationError;
import org.apache.wicket.validation.ValidatorAdapter;
import org.jetbrains.annotations.NotNull;

public class RangeSimplePanel extends InputPanel {

    private static final String ID_CONTAINER = "container";
    private static final String ID_MIN = "min";
    private static final String ID_MAX = "max";
    private static final String ID_MIN_LABEL = "minLabel";
    private static final String ID_MAX_LABEL = "maxLabel";

    IModel<RangeDto> rangeDto;

    public RangeSimplePanel(String id, IModel<RangeDto> rangeDto) {
        super(id);
        this.rangeDto = rangeDto;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);

        Label minLabel = new Label(ID_MIN_LABEL, getMinTitle());
        minLabel.setOutputMarkupId(true);
        container.add(minLabel);

        Label maxLabel = new Label(ID_MAX_LABEL, getMaxTitle());
        maxLabel.setOutputMarkupId(true);
        container.add(maxLabel);

        TextField<Double> minField = new TextField<>(ID_MIN,
                new PropertyModel<>(new ItemRealValueModel<>(getModel()), "min"));
        minField.setOutputMarkupId(true);
        minField.setEnabled(isEnabled());
        minField.add(buildMinFieldValidator());
        container.add(minField);

        TextField<Double> maxField = new TextField<>(ID_MAX,
                new PropertyModel<>(new ItemRealValueModel<>(getModel()), "max"));
        maxField.setOutputMarkupId(true);
        maxField.setEnabled(isEnabled());

        maxField.add(buildMaxFieldValidator());
        container.add(maxField);

        add(container);

    }

    private @NotNull ValidatorAdapter<Double> buildMaxFieldValidator() {
        return new ValidatorAdapter<>(fieldValue -> {
            Double value = fieldValue.getValue();
            boolean doubleType = rangeDto.getObject().isDoubleType();
            if (value == null) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.max.exception"));
            } else if (doubleType && !isDouble(String.valueOf(value))) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.double.exception"));
            } else if (!doubleType && isInteger(String.valueOf(value))) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.int.exception"));
            } else if (value > maximumValue()) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.positive.exception"));
            } else if (value < minimumValue()) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.negative.exception"));
            } else if (value < getMinField().getModelObject()) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.lower.exception"));
            }
        });
    }

    private @NotNull ValidatorAdapter<Double> buildMinFieldValidator() {
        return new ValidatorAdapter<>(fieldValue -> {
            Double value = fieldValue.getValue();
            boolean doubleType = rangeDto.getObject().isDoubleType();
            if (value == null) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.min.exception"));
            } else if (doubleType && !isDouble(String.valueOf(value))) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.double.exception"));
            } else if (!doubleType && isInteger(String.valueOf(value))) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.int.exception"));
            } else if (value > maximumValue()) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.positive.exception"));
            } else if (value < minimumValue()) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.negative.exception"));
            } else if (value > getMaxField().getModelObject()) {
                fieldValue.error((IValidationError) iErrorMessageSource -> getString("RangeSimplePanel.bigger.exception"));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private TextField<Double> getMinField() {
        return (TextField<Double>) get(getPageBase().createComponentPath(ID_CONTAINER, ID_MIN));
    }

    @SuppressWarnings("unchecked")
    private TextField<Double> getMaxField() {
        return (TextField<Double>) get(getPageBase().createComponentPath(ID_CONTAINER, ID_MAX));
    }

    public Double maximumValue() {
        return rangeDto.getObject().getMax();
    }

    public Double minimumValue() {
        return 0.0;
    }

    public IModel<PrismPropertyValueWrapper<RangeType>> getModel() {
        return rangeDto.getObject().getModel();
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<FormComponent> getFormComponents() {
        return List.of(getMinField(),
                getMaxField());
    }

    public boolean isDouble(@NotNull String value) {
        try {
            Double.parseDouble(value.replace(',', '.'));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isInteger(@NotNull String value) {
        try {
            double parsedValue = Double.parseDouble(value.replace(',', '.'));
            return parsedValue % 1 > 0.0;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    public StringResourceModel getMinTitle() {
        return  rangeDto.getObject().getMinTitle((PageBase) getPage());
    }

    public StringResourceModel getMaxTitle() {
        return  rangeDto.getObject().getMaxTitle((PageBase) getPage());
    }

}
