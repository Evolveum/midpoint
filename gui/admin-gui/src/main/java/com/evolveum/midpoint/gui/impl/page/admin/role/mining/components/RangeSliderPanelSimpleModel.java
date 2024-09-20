/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.components;

import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.prism.InputPanel;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class RangeSliderPanelSimpleModel extends InputPanel {
    private static final String ID_TEXT_FIELD = "slider_label";
    private static final String ID_SLIDER = "slider";
    Double sliderSimilarityValue;
    private final ItemRealValueModel<Integer> model;
    LoadableModel<Integer> maximumValue;

    public RangeSliderPanelSimpleModel(String id, ItemRealValueModel<Integer> realValueModel, LoadableModel<Integer> maximumValue) {
        super(id);

        this.maximumValue = maximumValue;

        this.model = realValueModel;

        if (getModelSimilarity() == null) {
            model.setObject(getDefaultValue());
            sliderSimilarityValue = (double) getDefaultValue();
        } else {
            sliderSimilarityValue = model.getObject().doubleValue();
        }

        TextField<String> sliderLabel = new TextField<>(ID_TEXT_FIELD, new LoadableModel<>() {
            @Override
            protected String load() {

                double maxValueD = getMaxValueD();
                Integer object = model.getObject();
                if (object == null || object == 0 || maxValueD == 0) {
                    return 0 + "% (" + 0 + ")";
                }
                double value = (object * 100 / maxValueD);
                BigDecimal bd = BigDecimal.valueOf(value);
                bd = bd.setScale(1, RoundingMode.UP);
                value = bd.doubleValue();
                return value + "% (" + object + ")";
            }
        });
        sliderLabel.setOutputMarkupId(true);
        sliderLabel.setEnabled(false);
        add(sliderLabel);

        FormComponent<Integer> slider = new FormComponent<>(ID_SLIDER, model) {
            @Override
            public void convertInput() {
                String input = getInput();
                if (input != null && !input.isEmpty()) {
                    Integer value = Integer.valueOf(input);
                    setConvertedInput(value);
                }
            }

            @Override
            protected void onInitialize() {
                super.onInitialize();
                add(new AjaxFormComponentUpdatingBehavior("input") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        double value = Double.parseDouble(getBaseFormComponent().getValue());
                        BigDecimal bd = BigDecimal.valueOf(value);
                        bd = bd.setScale(1, RoundingMode.HALF_UP);
                        value = bd.doubleValue();
                        sliderSimilarityValue = value;
                        getBaseFormComponent().getValue();
                        target.add(sliderLabel);
                        onUpdatePerform(target);
                    }
                });
            }
        };
        add(new Behavior() {
            @Override
            public void onConfigure(Component component) {
                slider.add(AttributeModifier.replace("max", getMaxValueD()));
                slider.add(AttributeModifier.replace("value", getModelSimilarity()));
                slider.add(AttributeModifier.replace("style", "width:" + getSliderWidth() + getSliderWidthUnit()));
            }
        });

        slider.add(new AttributeModifier("min", getMinValueD()));
        slider.add(new AttributeModifier("max", getMaxValueD()));
        slider.add(new AttributeModifier("value", getModelSimilarity()));
        slider.add(new AttributeModifier("style", "width:" + getSliderWidth() + getSliderWidthUnit()));
        add(slider);
    }

    protected void onUpdatePerform(AjaxRequestTarget target) {
        //override in subclass
    }

    public Integer getModelSimilarity() {
        return model.getObject();
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return (FormComponent<?>) get(ID_SLIDER);
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
        return maximumValue.getObject();
    }

}
