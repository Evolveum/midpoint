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
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.convert.ConversionException;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MatchType;

public class MatchRulePanel extends BasePanel<String> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_SLIDER_LABEL = "slider_label";
    private static final String ID_SLIDER = "slider";
    private static final String ID_CHECK_BOX = "check_box";

    Integer sliderSimilarityValue;

    public MatchRulePanel(String id, ItemRealValueModel<MatchType> realValueModel, boolean isMultiValue) {
        super(id);

        if (getMatchTypeObject(realValueModel) == null) {
            realValueModel.setObject(new MatchType().matchSimilarity(getDefaultValue()).isMultiValue(isMultiValue));
            sliderSimilarityValue = (int) getDefaultValue();
        } else {
            sliderSimilarityValue = realValueModel.getObject().getMatchSimilarity().intValue();
        }

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        TextField<String> sliderLabel = new TextField<>(ID_SLIDER_LABEL, new LoadableModel<>() {
            @Override
            protected String load() {
                return sliderSimilarityValue + "%";
            }
        });
        sliderLabel.setOutputMarkupId(true);
        sliderLabel.setEnabled(false);
        container.add(sliderLabel);

        FormComponent<MatchType> slider = new FormComponent<>(ID_SLIDER, realValueModel) {
            @Override
            protected MatchType convertValue(String[] value) throws ConversionException {
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
                        sliderSimilarityValue = getModel().getObject().getMatchSimilarity().intValue();
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
        container.add(slider);

        IModel<Boolean> iModel = new IModel<>() {
            @Override
            public Boolean getObject() {
                return realValueModel.getObject().isIsMultiValue();
            }

            @Override
            public void setObject(Boolean object) {
                realValueModel.getObject().setIsMultiValue(object);

            }
        };

        CheckBox components = new CheckBox(ID_CHECK_BOX, iModel);
        components.setOutputMarkupId(true);

        components.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                FormComponent<MatchType> sliderFormComponent = MatchRulePanel.this.getSliderFormComponent();
                MatchType object = realValueModel.getObject();
                sliderFormComponent.setEnabled(object.isIsMultiValue());
                target.add(sliderFormComponent);
            }
        });
        container.add(components);
        add(container);

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

    private FormComponent<MatchType> getSliderFormComponent() {
        return (FormComponent<MatchType>) get(getPageBase().createComponentPath(ID_CONTAINER, ID_SLIDER));
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
