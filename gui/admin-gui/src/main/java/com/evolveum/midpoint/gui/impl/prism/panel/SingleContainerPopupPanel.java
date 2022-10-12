/*
 * Copyright (c) 2020-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.prism.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemEditabilityHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemMandatoryHandler;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.List;

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
