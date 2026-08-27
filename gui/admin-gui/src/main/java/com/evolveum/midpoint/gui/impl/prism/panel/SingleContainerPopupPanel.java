/*
 * Copyright (c) 2020-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

public abstract class SingleContainerPopupPanel<C extends Containerable> extends BasePanel<PrismContainerWrapper<C>> implements Popupable {

    private static final String ID_CONTAINER = "container";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_OK_BUTTON = "okButton";

    public SingleContainerPopupPanel(String id, IModel<PrismContainerWrapper<C>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        mainForm.setOutputMarkupId(true);
        add(mainForm);

        getModelObject().setExpanded(true);
        getModelObject().getValues().forEach(value -> value.setExpanded(true));
        getModelObject().setShowEmpty(true, true);
        SingleContainerPanel<C> container = new SingleContainerPanel<C>(ID_CONTAINER, getModel(), getModelObject().getTypeName());
        container.setOutputMarkupId(true);
        mainForm.add(container);

        AjaxSubmitButton submit = new AjaxSubmitButton(ID_OK_BUTTON, createStringResource("Button.ok")) {

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                onSubmitPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getPageBase().getFeedbackPanel());
            }
        };
        submit.setOutputMarkupId(true);
        mainForm.add(submit);
    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 80;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    protected abstract void onSubmitPerformed(AjaxRequestTarget target);
}
