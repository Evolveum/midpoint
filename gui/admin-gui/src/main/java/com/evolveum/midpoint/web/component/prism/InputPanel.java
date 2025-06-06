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

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.web.security.MidPointApplication;

import org.apache.commons.lang3.StringUtils;
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

    /**
     * Use getParentPage instead of getPageBase if it's suitable
     * @return
     */
    public PageBase getPageBase() {
        return WebComponentUtil.getPageBase(this);
    }

    /**
     *  InputPanel component not necessarily has PageBase as a parent page. For example,
     *  such pages as Self Registration, Identity Recovery pages are extending PageAdminLTE.
     *  It is better to use this method instead of getPageBase not to get in trouble on non-PageBase-extending pages
     */
    public PageAdminLTE getParentPage() {
        return WebComponentUtil.getPage(this, PageAdminLTE.class);
    }

    public StringResourceModel createStringResource(String key) {
        return WebComponentUtil.getPage(InputPanel.this, PageAdminLTE.class).createStringResource(key);
    }

    public LocalizationService getLocalizationService() {
        return WebComponentUtil.getPage(InputPanel.this, PageAdminLTE.class).getLocalizationService();
    }

    public String createComponentPath(String... components) {
        return StringUtils.join(components, ":");
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
