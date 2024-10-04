/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.component;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.validator.RangeValidator;

import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

public class NumberFormatSelectorPanel extends InputPanel {

    private static final String ID_PANEL = "panel";
    public static final String ID_PRE_GROUP = "preGroup";
    IModel<Double> valueModel;

    public NumberFormatSelectorPanel(String id, IModel<Double> valueModel) {
        super(id);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        ImagePanel preGroup = new ImagePanel(ID_PRE_GROUP, Model.of(getImage()));
        preGroup.setOutputMarkupId(true);
        preGroup.setVisible(isPreGroupVisible());
        add(preGroup);

        NumberTextField<Double> field = new NumberTextField<>(ID_PANEL, getValueModel(), Double.class);
        field.setEnabled(true);
        field.setMinimum(getRangeValidator().getMinimum());
        field.setMaximum(getRangeValidator().getMaximum());
        field.setStep(stepValue());
        field.add(AttributeModifier.append("onkeydown", "return false;"));

        field.add(getRangeValidator());
        add(field);

        field.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onChangePerform(field.getModelObject());
            }
        });
    }

    public double stepValue(){
        return 0.05;
    }
    public void onChangePerform(Double newValue) {
        valueModel.setObject(newValue);
    }

    public DisplayType getImage() {
        return new DisplayType();
    }

    public RangeValidator<Double> getRangeValidator() {
        return RangeValidator.range(0.0, 1.0);
    }

    public IModel<Double> getValueModel() {
        return valueModel;
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return getFormC();
    }

    private NumberTextField getFormC() {
        return (NumberTextField) get(getPageBase().createComponentPath(ID_PANEL));
    }

    protected boolean isPreGroupVisible() {
        return true;
    }
}
