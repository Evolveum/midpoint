/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.CredentialModuleAuthentication;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/hint", matchUrlForSecurity = "/hint")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.HINT)
public class PagePasswordHint extends PageAuthenticationBase {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PagePasswordHint.class);

    private static final String ID_STATIC_LAYOUT = "staticLayout";
    private static final String ID_EMAIL = "email";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_HINT_PANEL = "hintPanel";
    private static final String ID_HINT_LABEL = "hintLabel";
    private static final String ID_BACK_BUTTON = "back";
    private static final String ID_BACK_BUTTON_LABEL = "backButtonLabel";
    private static final String ID_SUBMIT_IDENTIFIER = "submitIdentifier";
    private static final String ID_CONTINUE_RESET_PASSWORD = "continueResetPassword";
    private static final String ID_PASSWORD_RESET_SUBMITED = "resetPasswordInfo";

    private boolean submited;
    private UserType user = null;
    private String hint = null;

    public PagePasswordHint() {
    }


    protected void initCustomLayout() {
        MidpointForm form = new MidpointForm(ID_MAIN_FORM);
        form.add(AttributeModifier.replace("action", (IModel<String>) this::getUrlProcessingLogin));
//        form.add(new VisibleEnableBehaviour() {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isVisible() {
//                return !submited;
//            }
//
//        });
        add(form);

        initHintPanel(form);

        initButtons(form);

        MultiLineLabel label = new MultiLineLabel(ID_PASSWORD_RESET_SUBMITED,
                createStringResource("PageForgotPassword.form.submited.message"));
        add(label);
        label.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return submited;
            }

            @Override
            public boolean isEnabled() {
                return submited;
            }

        });

    }

    private void initHintPanel(MidpointForm<?> form) {
        WebMarkupContainer hintPanel = new WebMarkupContainer(ID_HINT_PANEL);
        hintPanel.setOutputMarkupId(true);
        hintPanel.add(new VisibleBehaviour(this::isHintPresent));
        form.add(hintPanel);

        Label hintLabel = new Label(ID_HINT_LABEL, new LoadableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return createStringResource("PageEmailNonce.passwordHintLabel", getHintValue()).getString();
            }
        });
        hintLabel.setOutputMarkupId(true);
        hintPanel.add(hintLabel);
    }

    private boolean isHintPresent() {
        return StringUtils.isNotEmpty(getHintValue());
    }

    private String getHintValue() {
        return hint != null ? hint : null;
    }

    private void initButtons(MidpointForm form) {
//        AjaxSubmitButton continuePasswordResetButton = new AjaxSubmitButton(ID_CONTINUE_RESET_PASSWORD, createStringResource("PageEmailNonce.continuePasswordResetLabel")) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected void onSubmit(AjaxRequestTarget target) {
//                continuePasswordReset(target);
//            }
//
//            @Override
//            protected void onError(AjaxRequestTarget target) {
//                target.add(getFeedbackPanel());
//            }
//
//        };
////        continuePasswordResetButton.add(new VisibleBehaviour(this::isHintPresent));
//        form.add(continuePasswordResetButton);

        form.add(createBackButton(ID_BACK_BUTTON));
    }

    @Override
    protected AjaxButton createBackButton(String id){
        AjaxButton backButton = new AjaxButton(id) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed();
            }
        };
        backButton.setOutputMarkupId(true);

        Label backButtonLabel = new Label(ID_BACK_BUTTON_LABEL, new LoadableModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                return createStringResource(isHintPresent() ? "PageEmailNonce.backButtonAfterHintLabel" : "PageEmailNonce.backButtonLabel").getString();
            }
        });
        backButton.add(backButtonLabel);
        return backButton;
    }

    @Override
    protected ObjectQuery createStaticFormQuery() {
        return null;
    }


    private String getUserPasswordHint(@NotNull UserType user) {
        return user.getCredentials() != null && user.getCredentials().getPassword() != null ?
                user.getCredentials().getPassword().getHint() : null;
    }
    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    @Override
    protected boolean isDynamicFormVisible() {
        return super.isDynamicFormVisible() && !isHintPresent();
    }

    private MidpointForm getMainForm() {
        return (MidpointForm) get(ID_MAIN_FORM);
    }

    protected DynamicFormPanel getDynamicForm(){
        return (DynamicFormPanel) getMainForm().get(createComponentPath(ID_DYNAMIC_LAYOUT, ID_DYNAMIC_FORM));
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        ServletWebRequest req = (ServletWebRequest) RequestCycle.get().getRequest();
        HttpServletRequest httpReq = req.getContainerRequest();
        HttpSession httpSession = httpReq.getSession();

        Exception ex = (Exception) httpSession.getAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
        if (ex == null) {
            return;
        }

        String msg = ex.getMessage();
        if (StringUtils.isEmpty(msg)) {
            msg = "web.security.provider.unavailable";
        }

        String[] msgs = msg.split(";");
        for (String message : msgs) {
            message = getLocalizationService().translate(message, null, getLocale(), message);
            error(message);
        }

        httpSession.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

//        if (AuthUtil.getPrincipalUser() != null) {
//            MidPointApplication app = getMidpointApplication();
//            throw new RestartResponseException(app.getHomePage());
//        }
    }

    private String getUrlProcessingLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null
                    && AuthenticationModuleNameConstants.HINT.equals(moduleAuthentication.getModuleTypeName())){
                String prefix = moduleAuthentication.getPrefix();
                return AuthUtil.stripSlashes(prefix) + "/spring_security_login";
            }
        }

        String key = "web.security.flexAuth.unsupported.auth.type";
        error(getString(key));
        return "/midpoint/spring_security_login";
    }

}

