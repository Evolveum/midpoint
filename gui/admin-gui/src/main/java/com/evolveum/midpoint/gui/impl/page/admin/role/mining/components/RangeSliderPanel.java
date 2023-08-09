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
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

public class RangeSliderPanel extends Panel {

    private static final String ID_TEXT_FIELD = "slider_label";
    private static final String ID_SLIDER = "slider";
    Integer sliderSimilarityValue;

    public RangeSliderPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        sliderSimilarityValue = getDefaultValue();

        TextField<String> sliderLabel = new TextField<>(ID_TEXT_FIELD, new LoadableModel<>() {
            @Override
            protected String load() {
                return sliderSimilarityValue + "%";
            }
        });
        sliderLabel.setOutputMarkupId(true);
        sliderLabel.setEnabled(false);
        add(sliderLabel);
        FormComponent<Integer> slider = new FormComponent<>(ID_SLIDER, Model.of(sliderSimilarityValue)) {
            @Override
            protected void onInitialize() {
                super.onInitialize();
                add(new AjaxFormComponentUpdatingBehavior("input") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        sliderSimilarityValue = Integer.valueOf(getFormComponent().getValue());
                        target.add(sliderLabel);
                    }
                });
            }
        };
        slider.add(new AttributeModifier("min", getMinValue()));
        slider.add(new AttributeModifier("max", getMaxValue()));
        slider.add(new AttributeModifier("value", getDefaultValue()));
        slider.add(new AttributeModifier("style", "width:" + getSliderWidth() + getSliderWidthUnit()));
        add(slider);

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

    public FormComponent<?> getSliderFormComponent() {
        return (FormComponent<?>) get(ID_SLIDER);
    }

    public TextField<?> getTextFieldFormComponent() {
        return (TextField<?>) get(ID_TEXT_FIELD);
    }
}
