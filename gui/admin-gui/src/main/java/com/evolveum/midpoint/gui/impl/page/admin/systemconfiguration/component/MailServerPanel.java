/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import java.io.Serializable;
import java.util.Arrays;

import com.evolveum.midpoint.gui.api.component.password.PasswordPropertyPanel;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.RangeValidator;

import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailServerConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MailTransportSecurityType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MailServerPanel extends ComplexPropertyInputPanel<MailServerConfigurationType> {

    private static final long serialVersionUID = 1L;

    private static final String ID_HOST = "host";
    private static final String ID_PORT = "port";
    private static final String ID_USERNAME = "username";
    private static final String ID_PASSWORD = "password";
    private static final String ID_TRANSPORT_SECURITY = "transportSecurity";

    public MailServerPanel(String id, IModel<MailServerConfigurationType> model) {
        super(id, model);

        initLayout();
    }

    private void initLayout() {
        add(new TextPanel<>(ID_HOST, createEmbeddedModel(c -> c.getHost(), (c, o) -> c.setHost(o))), false, "MailServerPanel.host");

        TextPanel port = new TextPanel<>(ID_PORT, createEmbeddedModel(c -> c.getPort(), (c, o) -> c.setPort(o)));
        FormComponent portFC = port.getBaseFormComponent();
        portFC.setType(Integer.class);
        portFC.add(new RangeValidator(0, 65535));
        add(port, "MailServerPanel.port");

        add(new TextPanel<>(ID_USERNAME, createEmbeddedModel(c -> c.getUsername(), (c, o) -> c.setUsername(o))), "MailServerPanel.username");

        add(new PasswordPropertyPanel(ID_PASSWORD, createEmbeddedModel(c -> c.getPassword(), (c, o) -> c.setPassword(o))), false, "MailServerPanel.password");

        DropDownChoicePanel transportSecurity = WebComponentUtil.createEnumPanel(MailTransportSecurityType.class, ID_TRANSPORT_SECURITY,
                createEmbeddedModel(c -> c.getTransportSecurity(), (c, o) -> c.setTransportSecurity(o)), this);
        add(transportSecurity, "MailServerPanel.transportSecurity");
    }

    private <T extends Serializable> IModel<T> createEmbeddedModel(SerializableFunction<MailServerConfigurationType, T> get, SerializableBiConsumer<MailServerConfigurationType, T> set) {
        return new ComplexPropertyEmbeddedModel<>(getModel(), get, set) {

            @Override
            protected MailServerConfigurationType createEmptyParentObject() {
                return new MailServerConfigurationType();
            }

            @Override
            protected boolean isParentModelObjectEmpty(MailServerConfigurationType object) {
                Object[] values = new Object[] {
                        object.getHost(),
                        object.getPassword(),
                        object.getUsername(),
                        object.getPassword()
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
