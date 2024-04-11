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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.util.convert.ConversionException;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MatchType;

public class MatchRulePanel extends BasePanel<String> {

    private static final String ID_CONTAINER = "match_rule_container";
    private static final String ID_SLIDER_LABEL = "slider_label";
    private static final String ID_SLIDER = "slider";

    public MatchRulePanel(String id, ItemRealValueModel<MatchType> realValueModel, boolean isMultiValue) {
        super(id);

        if (getMatchTypeObject(realValueModel) == null) {
            MatchType matchType = new MatchType()
                    .matchSimilarity(getDefaultValue())
                    .isMultiValue(isMultiValue);

            realValueModel.setObject(matchType);
        }
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        initSliderPanel(realValueModel, container);
    }

    private void initSliderPanel(
            @NotNull ItemRealValueModel<MatchType> realValueModel,
            @NotNull WebMarkupContainer container) {

        LoadableDetachableModel<String> sliderSimilarityValue = new LoadableDetachableModel<>() {
            @Override
            protected @NotNull String load() {
                return realValueModel.getObject().getMatchSimilarity().intValue() + "%";
            }
        };

        TextField<String> sliderLabel = new TextField<>(ID_SLIDER_LABEL, sliderSimilarityValue);
        sliderLabel.setOutputMarkupId(true);
        sliderLabel.setEnabled(false);
        container.add(sliderLabel);

        FormComponent<MatchType> slider = new FormComponent<>(ID_SLIDER, realValueModel) {
            @Override
            protected @NotNull MatchType convertValue(String @NotNull [] value) throws ConversionException {
                MatchType object = getModel().getObject();
                object.setMatchSimilarity(Double.valueOf(value[0]));
                return object;
            }

            @Override
            protected void onInitialize() {
                super.onInitialize();
                add(new AjaxFormComponentUpdatingBehavior("input") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        target.add(sliderLabel);
                    }
                });
            }
        };
        slider.add(new AttributeModifier("min", getMinValueD()));
        slider.add(new AttributeModifier("max", getMaxValueD()));
        slider.add(new AttributeModifier("value", getSliderValue(realValueModel)));
        slider.add(new AttributeModifier("style", "width:" + getSliderWidth() + getSliderWidthUnit()));

        if (!realValueModel.getObject().isIsMultiValue()) {
            slider.setEnabled(false);
        }

        realValueModel.getObject().isIsMultiValue();
        slider.add(new EnableBehaviour(() -> realValueModel.getObject().isIsMultiValue()));
        container.add(slider);
    }

    public boolean isMultiValue() {
        return false;
    }

    public MatchType getMatchTypeObject(@NotNull ItemRealValueModel<MatchType> realValueModel) {
        return realValueModel.getObject();
    }

    public double getSliderValue(@NotNull ItemRealValueModel<MatchType> realValueModel) {
        if (realValueModel.getObject() != null) {
            if (realValueModel.getObject().getMatchSimilarity() != null) {
                return realValueModel.getObject().getMatchSimilarity();
            }
        }
        return getDefaultValue();
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

    public double getDefaultValue() {
        return 100.0;
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
