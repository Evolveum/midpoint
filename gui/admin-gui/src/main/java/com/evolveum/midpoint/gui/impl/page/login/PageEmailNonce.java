/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.cycle.RequestCycle;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.WebAttributes;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.CredentialModuleAuthentication;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
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
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.prism.DynamicFormPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/emailNonce", matchUrlForSecurity = "/emailNonce")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.MAIL_NONCE)
public class PageEmailNonce extends PageAuthenticationBase {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageEmailNonce.class);

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
    public PageEmailNonce() {
        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal != null) {
            userIdentifierSubmitPerformed(null);
            submited = true;

//            throw new RestartResponseException(PageEmailNonce.class);
        }
    }


    protected void initCustomLayout() {
        MidpointForm form = new MidpointForm(ID_MAIN_FORM);
        form.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !submited;
            }

        });
        add(form);

        initStaticLayout(form);

        initDynamicLayout(form, PageEmailNonce.this);

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
    private void initButtons(MidpointForm form) {
        AjaxSubmitButton submitUserIdentifier = new AjaxSubmitButton(ID_SUBMIT_IDENTIFIER, createStringResource("PageBase.button.submit")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                userIdentifierSubmitPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(getFeedbackPanel());
            }

        };
        form.add(submitUserIdentifier);

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
                return createStringResource("PageEmailNonce.backButtonLabel").getString();
            }
        });
        backButton.add(backButtonLabel);
        return backButton;
    }

    private void userIdentifierSubmitPerformed(AjaxRequestTarget target) {
        if (user == null) {
            user = searchUser();
            validateUserNotNullOrFail();
        }
        LOGGER.trace("Reset Password user: {}", user);

        continuePasswordReset(target);
    }

    private void continuePasswordReset(AjaxRequestTarget target) {
        validateUserNotNullOrFail();
        NonceCredentialsPolicyType noncePolicy = getMailNoncePolicy(user.asPrismObject());
        if (noncePolicy == null) {
            LOGGER.debug("No policies for reset password defined");
            getSession().error(getString("pageForgetPassword.message.policy.not.found"));
            throw new RestartResponseException(PageEmailNonce.class);
        }

        OperationResult result = saveUserNonce(user, noncePolicy);
        if (result.getStatus() == OperationResultStatus.SUCCESS) {
            submited = true;
            if (target != null) {
                target.add(PageEmailNonce.this);
            }
        } else {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("Failed to send nonce to user: {} ", result.getMessage());
            throw new RestartResponseException(PageEmailNonce.this);
        }
    }


    private void validateUserNotNullOrFail() {
        if (user == null) {
            getSession().error(getString("pageForgetPassword.message.user.not.found"));
            throw new RestartResponseException(PageEmailNonce.class);
        }
    }

    private NonceCredentialsPolicyType getMailNoncePolicy(PrismObject<UserType> user) {
        SecurityPolicyType securityPolicy = resolveSecurityPolicy(user);
        LOGGER.trace("Found security policy: {}", securityPolicy);

        if (securityPolicy == null) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("No security policy, cannot process nonce credential");
            // Just log the error, but do not display it. We are still in unprivileged part of the web
            // we do not want to provide any information to the attacker.
            throw new RestartResponseException(PageEmailNonce.class);
        }
        if (securityPolicy.getCredentials() == null) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("No credential for security policy, cannot process nonce credential");
            // Just log the error, but do not display it. We are still in unprivileged part of the web
            // we do not want to provide any information to the attacker.
            throw new RestartResponseException(PageEmailNonce.class);
        }
        if (securityPolicy.getCredentials().getNonce() == null) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("No nonce credential for security policy, cannot process nonce credential");
            // Just log the error, but do not display it. We are still in unprivileged part of the web
            // we do not want to provide any information to the attacker.
            throw new RestartResponseException(PageEmailNonce.class);
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("Bad type of authentication, support only MidpointAuthentication, but is "
                    + authentication != null ? authentication.getClass().getName() : null);
            throw new RestartResponseException(PageEmailNonce.class);
        }

        ModuleAuthentication moduleAuthentication = ((MidpointAuthentication) authentication).getProcessingModuleAuthentication();
        if (!(moduleAuthentication instanceof CredentialModuleAuthentication)
                && !AuthenticationModuleNameConstants.MAIL_NONCE.equals(moduleAuthentication.getModuleTypeName())) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("Bad type of module authentication, support only EmailNonceModuleAuthentication, but is "
                    + moduleAuthentication != null ? moduleAuthentication.getClass().getName() : null);
            throw new RestartResponseException(PageEmailNonce.class);
        }
        CredentialModuleAuthentication nonceAuth = (CredentialModuleAuthentication) moduleAuthentication;
        String credentialName = nonceAuth.getCredentialName();

        if (credentialName == null) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("EmailNonceModuleAuthentication " + nonceAuth.getModuleIdentifier() + " haven't define name of credential");
            throw new RestartResponseException(PageEmailNonce.class);
        }

        NonceCredentialsPolicyType credentialByName = null;

        for (NonceCredentialsPolicyType credential : securityPolicy.getCredentials().getNonce()) {
            if (credentialName != null && credentialName.equals(credential.getName())) {
                credentialByName = credential;
            }
        }
        if (credentialByName == null) {
            getSession().error(getString("PageForgotPassword.send.nonce.failed"));
            LOGGER.error("Couldn't find nonce credentials by name " + credentialName);
            throw new RestartResponseException(PageEmailNonce.class);
        }

        return credentialByName;
    }

    private void initStaticLayout(MidpointForm form) {

        WebMarkupContainer staticLayout = new WebMarkupContainer(ID_STATIC_LAYOUT);
        staticLayout.setOutputMarkupId(true);
        staticLayout.add(new VisibleBehaviour(() -> !isDynamicForm()));
        form.add(staticLayout);

        RequiredTextField<String> visibleUsername = new RequiredTextField<>(ID_EMAIL, new Model<>());
        visibleUsername.setOutputMarkupId(true);
        staticLayout.add(visibleUsername);
    }


    public PageBase getPageBase() {
        return (PageBase) getPage();
    }

    @Override
    protected boolean isDynamicFormVisible() {
        return super.isDynamicFormVisible();
    }

    @Override
    protected ObjectQuery createStaticFormQuery() {
        RequiredTextField<String> emailTextFiled = getEmail();
        String email = emailTextFiled != null ? emailTextFiled.getModelObject() : null;
        LOGGER.debug("Reset Password user info form submitted. email={}", email);

        return getPrismContext().queryFor(UserType.class).item(UserType.F_EMAIL_ADDRESS)
                .eq(email).matchingCaseIgnore().build();

    }

    private MidpointForm getMainForm() {
        return (MidpointForm) get(ID_MAIN_FORM);
    }

    protected DynamicFormPanel getDynamicForm(){
        return (DynamicFormPanel) getMainForm().get(createComponentPath(ID_DYNAMIC_LAYOUT, ID_DYNAMIC_FORM));
    }

    private RequiredTextField<String> getEmail(){
        return (RequiredTextField<String>) getMainForm().get(createComponentPath(ID_STATIC_LAYOUT, ID_EMAIL));
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

    private OperationResult saveUserNonce(final UserType user, final NonceCredentialsPolicyType noncePolicy) {
        return runPrivileged(new Producer<>() {

            private static final long serialVersionUID = 1L;

            @Override
            public OperationResult run() {
                Task task = createAnonymousTask("generateUserNonce");
                task.setChannel(SchemaConstants.CHANNEL_RESET_PASSWORD_URI);
                task.setOwner(user.asPrismObject());
                OperationResult result = new OperationResult("generateUserNonce");
                ProtectedStringType nonceCredentials = new ProtectedStringType();
                try {
                    nonceCredentials
                            .setClearValue(generateNonce(noncePolicy, task, user.asPrismObject(), result));

                    ObjectDelta<UserType> nonceDelta = getPrismContext().deltaFactory().object()
                            .createModificationReplaceProperty(UserType.class, user.getOid(),
                                    SchemaConstants.PATH_NONCE_VALUE, nonceCredentials);

                    WebModelServiceUtils.save(nonceDelta, result, task, PageEmailNonce.this);
                } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException |
                         ConfigurationException | SecurityViolationException e) {
                    result.recordFatalError(getString("PageForgotPassword.message.saveUserNonce.fatalError"));
                    LoggingUtils.logException(LOGGER, "Failed to generate nonce for user: " + e.getMessage(),
                            e);
                }

                result.computeStatusIfUnknown();
                return result;
            }

        });
    }

    private <O extends ObjectType> String generateNonce(NonceCredentialsPolicyType noncePolicy, Task task,
                                                        PrismObject<O> user, OperationResult result)
            throws ExpressionEvaluationException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ValuePolicyType policy = null;

        if (noncePolicy != null && noncePolicy.getValuePolicyRef() != null) {
            PrismObject<ValuePolicyType> valuePolicy = WebModelServiceUtils.loadObject(ValuePolicyType.class,
                    noncePolicy.getValuePolicyRef().getOid(), PageEmailNonce.this, task, result);
            policy = valuePolicy.asObjectable();
        }

        return getModelInteractionService().generateValue(policy, 24, false, user, "nonce generation", task, result);
    }

}

