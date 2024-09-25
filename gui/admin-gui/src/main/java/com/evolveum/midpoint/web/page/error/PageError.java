/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.error;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.evolveum.midpoint.web.application.Url;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FeedbackMessagesHookType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.http.WebResponse;
import org.springframework.http.HttpStatus;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

/**
 * Base class for error web pages.
 *
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/error")
        },
        permitAll = true)
public class PageError extends PageBase {

    private static final String ID_CODE = "code";
    private static final String ID_LABEL = "label";
    private static final String ID_MESSAGE = "message";
    private static final String ID_ERROR_MESSAGE = "errorMessage";
    private static final String ID_BACK = "back";
    private static final String ID_HOME = "home";
    private static final String ID_LOGOUT_FORM = "logoutForm";
    private static final String ID_CSRF_FIELD = "csrfField";

    private static final Trace LOGGER = TraceManager.getTrace(PageError.class);

    private final Integer code;

    String exClass;
    String exMessage;

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

        if (ex != null) {
            exClass = ex.getClass().getName();
            exMessage = ex.getMessage();
            LOGGER.warn("Creating error page for code {}, exception {}: {}", exClass, exMessage, ex);
        } else {
            // Log this on debug level, this is normal during application initialization
            LOGGER.debug("Creating error page for code {}, no exception", code);
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

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

        final IModel<String> message = () -> {
            if (exClass == null || !isStackTraceVisible()) {
                return null;
            }

            SimpleDateFormat df = new SimpleDateFormat();
            return df.format(new Date()) + "\t" + exClass + ": " + exMessage;
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

        MidpointForm form = new MidpointForm(ID_LOGOUT_FORM);
        form.add(AttributeModifier.replace("action", (IModel<String>) () -> getUrlForLogout()));
        form.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return SecurityUtils.getPrincipalUser() != null;
            }
        });
        add(form);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);
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
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(OnDomReadyHeaderItem.forScript("$('div.content-wrapper').css('margin-left', '0');"));
    }

    private boolean isStackTraceVisible() {
        UserInterfaceElementVisibilityType stackTraceVisibility = null;


        FeedbackMessagesHookType feedbackConfig = getCompiledGuiProfile().getFeedbackMessagesHook();
        if (feedbackConfig != null) {
            stackTraceVisibility = feedbackConfig.getStackTraceVisibility();
        }

        if (stackTraceVisibility == null) {
            stackTraceVisibility = UserInterfaceElementVisibilityType.VISIBLE;
        }

        if (stackTraceVisibility == UserInterfaceElementVisibilityType.VISIBLE) {
            return true;
        }

        if (stackTraceVisibility == UserInterfaceElementVisibilityType.HIDDEN) {
            return false;
        }

        return true;
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

    private String getUrlForLogout() {
        ModuleAuthentication moduleAuthentication = SecurityUtils.getAuthenticatedModule();

        if (moduleAuthentication == null) {
            return SecurityUtils.DEFAULT_LOGOUT_PATH;
        }
        return SecurityUtils.getPathForLogoutWithContextPath(getRequest().getContextPath(), moduleAuthentication);
    }
}
