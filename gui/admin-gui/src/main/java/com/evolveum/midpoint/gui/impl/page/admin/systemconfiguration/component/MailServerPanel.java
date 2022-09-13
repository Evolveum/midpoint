/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailServerConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MailServerPanel extends InputPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_HOST = "host";
    private static final String ID_PORT = "port";
    private static final String ID_USERNAME = "username";
    private static final String ID_PASSWORD = "password";

    private IModel<MailServerConfigurationType> model;

    public MailServerPanel(String id, IModel<MailServerConfigurationType> model) {
        super(id);

        this.model = new LoadableModel<>() {

            @Override
            protected MailServerConfigurationType load() {
                if (model == null) {
                    return new MailServerConfigurationType();
                }

                MailServerConfigurationType config = model.getObject();

                return config != null ? config : new MailServerConfigurationType();
            }
        };

        initLayout();
    }

    public IModel<MailServerConfigurationType> getModel() {
        return model;
    }

    private void initLayout() {
        add(new TextPanel<>(ID_HOST, new PropertyModel<>(model, MailServerConfigurationType.F_HOST.getLocalPart())));
        add(new TextPanel<>(ID_PORT, new PropertyModel<>(model, MailServerConfigurationType.F_PORT.getLocalPart())));
        add(new TextPanel<>(ID_USERNAME, new PropertyModel<>(model, MailServerConfigurationType.F_USERNAME.getLocalPart())));
        add(new PasswordPanel(ID_PASSWORD, new PropertyModel<>(model, MailServerConfigurationType.F_PASSWORD.getLocalPart())));
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return ((TextPanel) get(ID_HOST)).getBaseFormComponent();
    }
}
