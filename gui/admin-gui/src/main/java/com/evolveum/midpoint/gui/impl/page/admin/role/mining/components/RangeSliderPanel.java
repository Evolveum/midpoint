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

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.web.component.prism.InputPanel;

public class RangeSliderPanel extends InputPanel {
    private static final String ID_TEXT_FIELD = "slider_label";
    private static final String ID_SLIDER = "slider";

    public RangeSliderPanel(String id, ItemRealValueModel<Double> realValueModel) {
        super(id);

        if (getModelSimilarity(realValueModel) == null) {
            realValueModel.setObject((double) getDefaultValue());
        }

        TextField<String> sliderLabel = new TextField<>(ID_TEXT_FIELD, new LoadableModel<>() {
            @Override
            protected String load() {
                return realValueModel.getObject().intValue() + "%";
            }
        });
        sliderLabel.setOutputMarkupId(true);
        sliderLabel.setEnabled(false);
        add(sliderLabel);

        FormComponent<Double> slider = new FormComponent<>(ID_SLIDER, realValueModel) {
            @Override
            public void convertInput() {
                String input = getInput();
                if (input != null && !input.isEmpty()) {
                    Double value = Double.parseDouble(input.replace(',', '.'));
                    setConvertedInput(value);
                }
            }

            @Override
            protected void onInitialize() {
                super.onInitialize();
                add(new AjaxFormComponentUpdatingBehavior("input") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        getBaseFormComponent().getValue();
                        target.add(sliderLabel);
                    }
                });
            }
        };
        slider.add(new AttributeModifier("min", getMinValueD()));
        slider.add(new AttributeModifier("max", getMaxValueD()));
        slider.add(new AttributeModifier("value", getModelSimilarity(realValueModel)));
        slider.add(new AttributeModifier("style", "width:" + getSliderWidth() + getSliderWidthUnit()));
        add(slider);
    }

    public Double getModelSimilarity(ItemRealValueModel<Double> realValueModel) {
        return realValueModel.getObject();
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return (FormComponent<?>) get(ID_SLIDER);
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
