/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.gui.api.Validatable;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.page.PageBase;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author lazyman
 */
public abstract class InputPanel extends Panel implements Validatable {

    private static final long serialVersionUID = 1L;

    private List<Behavior> behaviors = new ArrayList<>();
    private IModel<String> label;
    private boolean required;

    public InputPanel(String id) {
        super(id);
    }

    public List<FormComponent> getFormComponents() {
        return Arrays.asList(getBaseFormComponent());
    }

    public abstract FormComponent getBaseFormComponent();

    @Override
    public FormComponent getValidatableComponent() {
        return getBaseFormComponent();
    }

    public void append(Behavior behavior) {
        behaviors.add(behavior);
    }

    public void setComponentLabel(IModel<String> label) {
        this.label = label;
    }

    public void required(boolean required) {
        this.required = required;
    }

    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    public PageAdminLTE getPageAdminLTE() {
        return (PageAdminLTE) getPage();
    }

    public StringResourceModel createStringResource(String key) {
        return getPageAdminLTE().createStringResource(key);
    }

    public LocalizationService getLocalizationService() {
        return getPageAdminLTE().getLocalizationService();
    }


    //    @Override
//    protected void onConfigure() {
//        super.onConfigure();
//        for (FormComponent formComponent : getFormComponents()) {
//            formComponent.add(behaviors.toArray(new Behavior[]{}));
//            formComponent.setRequired(required);
//            formComponent.setLabel(label);
//        }
//
//    }

}
