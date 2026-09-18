/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Content of the "Protocol" tab for a log entry whose {@link HttpProtocol} detail was captured - the request
 * (method/URL/body) and response (status code/body).
 */
public class HttpProtocolPanel extends BasePanel<HttpProtocol> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_METHOD = "method";
    private static final String ID_URL = "url";
    private static final String ID_REQUEST_BODY = "requestBody";
    private static final String ID_STATUS_CODE = "statusCode";
    private static final String ID_RESPONSE_BODY = "responseBody";

    public HttpProtocolPanel(String id, IModel<HttpProtocol> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        add(createMethodLabel());
        add(createUrlLabel());
        add(createRequestBodyBlock());

        add(createStatusCodeLabel());
        add(createResponseBodyBlock());
    }

    private @NotNull Label createMethodLabel() {
        IModel<String> methodModel = () -> {
            HttpRequest request = getModelObject().request();
            return request != null && request.method() != null ? request.method().name() : null;
        };
        return new Label(ID_METHOD, methodModel);
    }

    private @NotNull Label createUrlLabel() {
        IModel<String> urlModel = () -> {
            HttpRequest request = getModelObject().request();
            return request != null ? request.url() : null;
        };
        return new Label(ID_URL, urlModel);
    }

    private @NotNull CodeBlockPanel createRequestBodyBlock() {
        IModel<String> requestBodyModel = () -> {
            HttpRequest request = getModelObject().request();
            return request != null ? request.body() : null;
        };
        return createBodyBlock(ID_REQUEST_BODY, requestBodyModel);
    }

    private @NotNull Label createStatusCodeLabel() {
        IModel<String> statusCodeModel = () -> {
            HttpResponse response = getModelObject().response();
            return response != null ? String.valueOf(response.statusCode()) : null;
        };
        return new Label(ID_STATUS_CODE, statusCodeModel);
    }

    private @NotNull CodeBlockPanel createResponseBodyBlock() {
        IModel<String> responseBodyModel = () -> {
            HttpResponse response = getModelObject().response();
            return response != null ? response.body() : null;
        };
        return createBodyBlock(ID_RESPONSE_BODY, responseBodyModel);
    }

    private @NotNull CodeBlockPanel createBodyBlock(String id, IModel<String> bodyModel) {
        CodeBlockPanel block = new CodeBlockPanel(id, createStringResource("OperationLogPanel.protocol.body"), bodyModel);
        block.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(bodyModel.getObject())));
        return block;
    }
}
