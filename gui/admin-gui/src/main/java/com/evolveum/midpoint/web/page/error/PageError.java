/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.error;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.self.PageSelfDashboard;
import com.evolveum.midpoint.web.session.SessionStorage;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.http.WebResponse;
import org.springframework.http.HttpStatus;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Base class for error web pages.
 *
 * @author lazyman
 */
@PageDescriptor(url = "/error", permitAll = true)
public class PageError extends PageBase {

    private static final String ID_CODE = "code";
    private static final String ID_LABEL = "label";
    private static final String ID_MESSAGE = "message";
    private static final String ID_ERROR_MESSAGE = "errorMessage";
    private static final String ID_BACK = "back";
    private static final String ID_HOME = "home";

    private static final Trace LOGGER = TraceManager.getTrace(PageError.class);

    private Integer code;
    private String exClass;
    private String exMessage;
    protected String errorMessageKey;

    public PageError() {
        this(500);
    }

    public PageError(Integer code) {
        this(code, null);
    }

    public PageError(Exception ex) {
        this(500, ex);
    }

    public PageError(Integer code, Exception ex) {
        this.code = code;

        if (ex == null) {
            // Log this on debug level, this is normal during application initialization
            LOGGER.debug("Creating error page for code {}, no exception", code);
        } else {
            LOGGER.warn("Creating error page for code {}, exception {}: {}", ex.getClass().getName(), ex.getMessage(), ex);
        }

        Label codeLabel = new Label(ID_CODE, code);
        add(codeLabel);

        Label errorMessage = new Label(ID_ERROR_MESSAGE, createStringResource(getErrorMessageKey()));
        add(errorMessage);

        String errorLabel = "Unexpected error";
        if (code != null) {
            HttpStatus httpStatus = HttpStatus.valueOf(code);
            if (httpStatus != null) {
                errorLabel = httpStatus.getReasonPhrase();
            }
        }
        Label labelLabel = new Label(ID_LABEL, errorLabel);
        add(labelLabel);

        if (ex != null) {
            exClass = ex.getClass().getName();
            exMessage = ex.getMessage();
        }

        final IModel<String> message = new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                if (exClass == null) {
                    return null;
                }

                SimpleDateFormat df = new SimpleDateFormat();
                return df.format(new Date()) + "\t" + exClass + ": " + exMessage;
            }
        };

        Label label = new Label(ID_MESSAGE, message);
        label.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return StringUtils.isNotEmpty(message.getObject());
            }
        });
        add(label);

        AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageError.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                backPerformed(target);
            }
        };
        add(back);

        AjaxButton home = new AjaxButton(ID_HOME, createStringResource("PageError.button.home")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                homePerformed(target);
            }
        };
        add(home);
    }

    private int getCode() {
        return code != null ? code : 500;
    }

    @Override
    protected void configureResponse(WebResponse response) {
        super.configureResponse(response);

        response.setStatus(getCode());
    }

    @Override
    public boolean isVersioned() {
        return false;
    }

    @Override
    public boolean isErrorPage() {
        return true;
    }

    private void homePerformed(AjaxRequestTarget target) {
        setResponsePage(getMidpointApplication().getHomePage());
    }

    private void backPerformed(AjaxRequestTarget target) {
        redirectBack();
    }

    protected String getErrorMessageKey() {
        return "PageError.message";
    }
}
