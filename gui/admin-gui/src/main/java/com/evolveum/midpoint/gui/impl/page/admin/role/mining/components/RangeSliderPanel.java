/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.util.convert.ConversionException;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.web.component.prism.InputPanel;

public class RangeSliderPanel extends InputPanel {
    private static final String ID_TEXT_FIELD = "slider_label";
    private static final String ID_SLIDER = "slider";
    Integer sliderSimilarityValue;
    private final ItemRealValueModel<Double> model;

    public RangeSliderPanel(String id, ItemRealValueModel<Double> realValueModel) {
        super(id);

        this.model = realValueModel;

        if (getModelSimilarity() == null) {
            model.setObject((double) getDefaultValue());
            sliderSimilarityValue = getDefaultValue();
        } else {
            sliderSimilarityValue = model.getObject().intValue();
        }

        TextField<String> sliderLabel = new TextField<>(ID_TEXT_FIELD, new LoadableModel<>() {
            @Override
            protected String load() {
                return sliderSimilarityValue + "%";
            }
        });
        sliderLabel.setOutputMarkupId(true);
        sliderLabel.setEnabled(false);
        add(sliderLabel);

        FormComponent<Double> slider = new FormComponent<>(ID_SLIDER, model) {
            @Override
            public void convertInput() {
                String input = getInput();
                if (input != null && !input.isEmpty()) {
                    Double value = Double.parseDouble(input);
                    setConvertedInput(value);
                }
            }

            @Override
            protected Double convertValue(String[] value) throws ConversionException {
                return super.convertValue(value);
            }

            @Override
            protected void onInitialize() {
                super.onInitialize();
                add(new AjaxFormComponentUpdatingBehavior("input") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        sliderSimilarityValue = Integer.valueOf(getBaseFormComponent().getValue());
                        getBaseFormComponent().getValue();
                        target.add(sliderLabel);
                    }
                });
            }
        };
        slider.add(new AttributeModifier("min", getMinValueD()));
        slider.add(new AttributeModifier("max", getMaxValueD()));
        slider.add(new AttributeModifier("value", getModelSimilarity()));
        slider.add(new AttributeModifier("style", "width:" + getSliderWidth() + getSliderWidthUnit()));
        add(slider);
    }

    public Double getModelSimilarity() {
        return model.getObject();
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return (FormComponent<?>) get(ID_SLIDER);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    public int getMinValue() {
        return 0;
    }

    public int getMaxValue() {
        return 100;
    }

    public int getDefaultValue() {
        return 80;
    }

    public int getSliderWidth() {
        return 200;
    }

    public String getSliderWidthUnit() {
        return "px";
    }

    public double getMinValueD() {
        return 0;
    }

    public double getMaxValueD() {
        return 100;
    }

}
