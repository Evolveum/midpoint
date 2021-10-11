/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.prism.PrismObject;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxButton;

import java.io.Serializable;

/**
 * @author lskublik
 */
public class ConfigureTaskConfirmationPanel extends ConfirmationPanel {

    private static final long serialVersionUID = 1L;
    private static final String ID_CONFIGURE = "configure";

    public ConfigureTaskConfirmationPanel(String id) {
        this(id, null);
    }

    public ConfigureTaskConfirmationPanel(String id, IModel<String> message) {
        super(id, message);
    }

    @Override
    protected void customInitLayout(WebMarkupContainer panel) {
        AjaxButton configuredButton = new AjaxButton(ID_CONFIGURE,
                new StringResourceModel("ConfigureTaskConfirmationPanel.configure", this, null)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ((PageBase) getPage()).hideMainPopup(target);
                WebComponentUtil.dispatchToObjectDetailsPage(getTask(target), true, ConfigureTaskConfirmationPanel.this);
            }
        };
        panel.add(configuredButton);
    }

    protected PrismObject<TaskType> getTask(AjaxRequestTarget target) {
        return null;
    }
}
