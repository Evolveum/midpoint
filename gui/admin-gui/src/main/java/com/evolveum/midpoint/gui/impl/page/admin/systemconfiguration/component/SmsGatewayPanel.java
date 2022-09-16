/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.ExpressionPropertyPanel;

import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.ExpressionEditorPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.HttpMethodType;

import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmsGatewayConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SmsGatewayPanel extends InputPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_METHOD = "method";
    private static final String ID_URL_EXPRESSION = "urlExpression";
    private static final String ID_HEADERS_EXPRESSION = "headersExpression";
    private static final String ID_BODY_EXPRESSION = "bodyExpression";
    private static final String ID_BODY_ENCODING = "bodyEncoding";
    private static final String ID_USERNAME = "username";
    private static final String ID_PASSWORD = "password";
    private static final String ID_PROXY_HOST = "proxyHost";
    private static final String ID_PROXY_PORT = "proxyPort";
    private static final String ID_PROXY_USERNAME = "proxyUsername";
    private static final String ID_PROXY_PASSWORD = "proxyPassword";
    private static final String ID_REDIRECT_TO_FILE = "redirectToFile";
    private static final String ID_LOG_TO_FILE = "logToFile";
    private static final String ID_NAME = "name";


    private IModel<SmsGatewayConfigurationType> model;

    public SmsGatewayPanel(String id, IModel<SmsGatewayConfigurationType> model) {
        super(id);

        this.model = new LoadableModel<>() {

            @Override
            protected SmsGatewayConfigurationType load() {
                if (model == null) {
                    return new SmsGatewayConfigurationType();
                }

                SmsGatewayConfigurationType config = model.getObject();

                return config != null ? config : new SmsGatewayConfigurationType();
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    public IModel<SmsGatewayConfigurationType> getModel() {
        return model;
    }

    private void initLayout() {
        add(new TextPanel<>(ID_NAME, new PropertyModel<>(model, SmsGatewayConfigurationType.F_NAME.getLocalPart())));
        add(WebComponentUtil.createEnumPanel(HttpMethodType.class, ID_METHOD, new PropertyModel<>(model, SmsGatewayConfigurationType.F_METHOD.getLocalPart()), this));
        add(new ExpressionEditorPanel(ID_URL_EXPRESSION, new PropertyModel<>(model, SmsGatewayConfigurationType.F_URL_EXPRESSION.getLocalPart()), getPageBase()));

//        add(new TextPanel<>(ID_USERNAME, new PropertyModel<>(model, SmsGatewayConfigurationType.F_USERNAME.getLocalPart())));
//        add(new PasswordPanel(ID_PASSWORD, new PropertyModel<>(model, SmsGatewayConfigurationType.F_PASSWORD.getLocalPart())));
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return ((TextPanel) get(ID_NAME)).getBaseFormComponent();
    }
}
