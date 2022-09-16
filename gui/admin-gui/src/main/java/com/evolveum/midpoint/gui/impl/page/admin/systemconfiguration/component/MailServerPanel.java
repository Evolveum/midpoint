/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import java.io.Serializable;
import java.util.Arrays;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.RangeValidator;

import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailServerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailTransportSecurityType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MailServerPanel extends InputPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_HOST = "host";
    private static final String ID_PORT = "port";
    private static final String ID_USERNAME = "username";
    private static final String ID_PASSWORD = "password";
    private static final String ID_TRANSPORT_SECURITY = "transportSecurity";

    private IModel<MailServerConfigurationType> model;

    public MailServerPanel(String id, IModel<MailServerConfigurationType> model) {
        super(id);

        this.model = model;

        initLayout();
    }

    public IModel<MailServerConfigurationType> getModel() {
        return model;
    }

    private void initLayout() {
        add(new TextPanel<>(ID_HOST, createEmbeddedModel(c -> c.getHost(), (c, o) -> c.setHost(o))));

        TextPanel port = new TextPanel<>(ID_PORT, createEmbeddedModel(c -> c.getPort(), (c, o) -> c.setPort(o)));
        FormComponent portFC = port.getBaseFormComponent();
        portFC.setType(Integer.class);
        portFC.add(new RangeValidator(0, 2 ^ 16 - 1)); // 65535

        port.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(port);

        TextPanel username = new TextPanel<>(ID_USERNAME, createEmbeddedModel(c -> c.getUsername(), (c, o) -> c.setUsername(o)));
        username.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(username);

        add(new PasswordPanel(ID_PASSWORD, createEmbeddedModel(c -> c.getPassword(), (c, o) -> c.setPassword(o))));

        DropDownChoicePanel transportSecurity = WebComponentUtil.createEnumPanel(MailTransportSecurityType.class, ID_TRANSPORT_SECURITY,
                createEmbeddedModel(c -> c.getTransportSecurity(), (c, o) -> c.setTransportSecurity(o)), this);
        transportSecurity.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        add(transportSecurity);
    }

    private <T extends Serializable> IModel<T> createEmbeddedModel(SerializableFunction<MailServerConfigurationType, T> get, SerializableBiConsumer<MailServerConfigurationType, T> set) {
        return new IModel<T>() {
            @Override
            public T getObject() {
                MailServerConfigurationType config = model.getObject();
                if (config == null) {
                    return null;
                }

                return get.apply(config);
            }

            @Override
            public void setObject(T object) {
                MailServerConfigurationType config = model.getObject();

                if (object == null && config == null) {
                    return;
                }

                if (config == null) {
                    config = new MailServerConfigurationType();
                    model.setObject(config);
                }

                set.accept(config, object);

                if (isParentModelObjectEmpty(config)) {
                    model.setObject(null);
                }
            }

            private boolean isParentModelObjectEmpty(MailServerConfigurationType config) {
                Object[] values = new Object[] {
                        config.getHost(),
                        config.getPassword(),
                        config.getUsername(),
                        config.getPassword()
                };

                return Arrays.stream(values).allMatch(v -> v == null);
            }
        };
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return ((TextPanel) get(ID_HOST)).getBaseFormComponent();
    }
}
