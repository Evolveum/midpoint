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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.RangeValidator;

import com.evolveum.midpoint.gui.api.component.password.PasswordPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.util.SerializableBiConsumer;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.HttpMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SmsGatewayConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SmsGatewayPanel extends ComplexPropertyInputPanel<SmsGatewayConfigurationType> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(SmsGatewayPanel.class);

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

    public SmsGatewayPanel(String id, IModel<SmsGatewayConfigurationType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        add(new TextPanel<>(ID_NAME, createEmbeddedModel(o -> o.getName(), (o, v) -> o.setName(v))), false, "SmsGatewayPanel.name");
        add(WebComponentUtil.createEnumPanel(HttpMethodType.class, ID_METHOD, createEmbeddedModel(o -> o.getMethod(), (o, v) -> o.setMethod(v)), this), "SmsGatewayPanel.method");
        add(new AceEditorPanel(ID_URL_EXPRESSION, null,
                createExpressionModel(createEmbeddedModel(o -> o.getUrlExpression(), (o, v) -> o.setUrlExpression(v))), 10), "SmsGatewayPanel.urlExpression");
        add(new AceEditorPanel(ID_HEADERS_EXPRESSION, null,
                createExpressionModel(createEmbeddedModel(o -> o.getHeadersExpression(), (o, v) -> o.setHeadersExpression(v))), 10), "SmsGatewayPanel.headersExpression");
        add(new AceEditorPanel(ID_BODY_EXPRESSION, null,
                createExpressionModel(createEmbeddedModel(o -> o.getBodyExpression(), (o, v) -> o.setBodyExpression(v))), 10), "SmsGatewayPanel.bodyExpression");
        add(new TextPanel<>(ID_BODY_ENCODING, createEmbeddedModel(o -> o.getBodyEncoding(), (o, v) -> o.setBodyEncoding(v))), "SmsGatewayPanel.bodyEncoding");
        add(new TextPanel<>(ID_USERNAME, createEmbeddedModel(o -> o.getUsername(), (o, v) -> o.setUsername(v))), "SmsGatewayPanel.username");
        add(new PasswordPropertyPanel(ID_PASSWORD, createEmbeddedModel(o -> o.getPassword(), (o, v) -> o.setPassword(v))), false, "SmsGatewayPanel.password");
        add(new TextPanel<>(ID_PROXY_HOST, createEmbeddedModel(o -> o.getProxyHost(), (o, v) -> o.setProxyHost(v))), "SmsGatewayPanel.proxyHost");

        TextPanel port = new TextPanel<>(ID_PROXY_PORT, createEmbeddedModel(o -> o.getProxyPort(), (o, v) -> o.setProxyPort(v)));
        FormComponent portFC = port.getBaseFormComponent();
        portFC.setType(Integer.class);
        portFC.add(new RangeValidator(0, 65535));
        add(port, "SmsGatewayPanel.proxyPort");

        add(new TextPanel<>(ID_PROXY_USERNAME, createEmbeddedModel(o -> o.getProxyUsername(), (o, v) -> o.setProxyUsername(v))), "SmsGatewayPanel.proxyUsername");
        add(new PasswordPropertyPanel(ID_PROXY_PASSWORD, createEmbeddedModel(o -> o.getProxyPassword(), (o, v) -> o.setProxyPassword(v))), false, "SmsGatewayPanel.proxyPassword");
        add(new TextPanel<>(ID_REDIRECT_TO_FILE, createEmbeddedModel(o -> o.getRedirectToFile(), (o, v) -> o.setRedirectToFile(v))), "SmsGatewayPanel.redirectToFile");
        add(new TextPanel<>(ID_LOG_TO_FILE, createEmbeddedModel(o -> o.getLogToFile(), (o, v) -> o.setLogToFile(v))), "SmsGatewayPanel.logToFile");
    }

    private IModel<String> createExpressionModel(IModel<ExpressionType> model) {
        return new IModel<>() {
            @Override
            public String getObject() {
                ExpressionType expression = model.getObject();
                if (expression == null) {
                    return null;
                }

                try {
                    return getPageBase().getPrismContext().xmlSerializer().serializeRealValue(expression);
                } catch (Exception e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot serialize filter", e);
                    ThreadContext.getSession().error("Cannot parse filter: " + e.getMessage() + ". For more details, please, see midpoint log");
                }

                return null;
            }

            @Override
            public void setObject(String object) {
                if (model == null) {
                    return;
                }

                if (StringUtils.isBlank(object)) {
                    model.setObject(null);
                    return;
                }

                try {
                    ExpressionType expression = getPageBase().getPrismContext().parserFor(object).parseRealValue(ExpressionType.class);
                    model.setObject(expression);
                } catch (Exception e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot parse filter", e);
                    ThreadContext.getSession().error("Cannot parse filter: " + e.getMessage() + ". For more details, please, see midpoint log");
                }
            }
        };
    }

    @Override
    public FormComponent getBaseFormComponent() {
        return ((TextPanel) get(ID_NAME)).getBaseFormComponent();
    }

    private <T extends Serializable> IModel<T> createEmbeddedModel(SerializableFunction<SmsGatewayConfigurationType, T> get, SerializableBiConsumer<SmsGatewayConfigurationType, T> set) {
        return new ComplexPropertyEmbeddedModel<>(getModel(), get, set) {

            @Override
            protected SmsGatewayConfigurationType createEmptyParentObject() {
                return new SmsGatewayConfigurationType();
            }

            @Override
            protected boolean isParentModelObjectEmpty(SmsGatewayConfigurationType object) {
                Object[] values = new Object[] {
                        object.getName(),
                        object.getMethod(),
                        object.getUrlExpression(),
                        object.getHeadersExpression(),
                        object.getBodyExpression(),
                        object.getBodyEncoding(),
                        object.getUsername(),
                        object.getPassword(),
                        object.getProxyHost(),
                        object.getProxyPort(),
                        object.getProxyUsername(),
                        object.getProxyPassword(),
                        object.getRedirectToFile(),
                        object.getLogToFile()
                };

                return Arrays.stream(values).allMatch(v -> v == null);
            }
        };
    }
}
