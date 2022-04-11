/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.forgetpassword.PageForgotPassword;
import com.evolveum.midpoint.web.page.login.AbstractPageLogin;
import com.evolveum.midpoint.web.page.login.PageSelfRegistration;
import com.evolveum.midpoint.web.security.module.authentication.LdapModuleAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.LoginFormModuleAuthentication;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author mserbak
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/login", matchUrlForSecurity = "/login")
}, permitAll = true, loginPage = true)
public class PageLogin extends AbstractPageLogin {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageLoginOld.class);

    private static final String ID_FORGET_PASSWORD = "forgotPassword";
    private static final String ID_SELF_REGISTRATION = "selfRegistration";
    private static final String ID_CSRF_FIELD = "csrfField";
    private static final String ID_FORM = "form";

    private static final String DOT_CLASS = PageLoginOld.class.getName() + ".";
    protected static final String OPERATION_LOAD_RESET_PASSWORD_POLICY = DOT_CLASS + "loadPasswordResetPolicy";
    private static final String OPERATION_LOAD_REGISTRATION_POLICY = DOT_CLASS + "loadRegistrationPolicy";

    public PageLogin() {
    }

    @Override
    protected IModel<String> getBodyCssClass() {
        return new ReadOnlyModel<>(() -> "hold-transition login-page");
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return null;
    }

    @Override
    protected void initCustomLayer() {
        MidpointForm form = new MidpointForm(ID_FORM);
        form.add(AttributeModifier.replace("action", (IModel<String>) this::getUrlProcessingLogin));
        add(form);

        SecurityPolicyType securityPolicyType = loadSecurityPolicyType();

        addForgotPasswordLink(securityPolicyType);
        addRegistrationLink(securityPolicyType);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);
    }

    private void addForgotPasswordLink(SecurityPolicyType securityPolicyType) {
        BookmarkablePageLink<String> link = new BookmarkablePageLink<>(ID_FORGET_PASSWORD, PageForgotPassword.class);
        link.add(new VisibleBehaviour(() -> isForgotPasswordVisible(securityPolicyType)));
        link.add(AttributeModifier.replace("href", (IModel<String>) () -> getForgotPasswordHref(securityPolicyType)));
        add(link);
    }

    private void addRegistrationLink(SecurityPolicyType securityPolicyType) {
        BookmarkablePageLink<String> registration = new BookmarkablePageLink<>(ID_SELF_REGISTRATION, PageSelfRegistration.class);
        registration.add(new VisibleBehaviour(this::isRegistrationVisible));
        registration.add(AttributeModifier.replace("href", (IModel<String>) () -> getRegistrationHref(securityPolicyType)));
        add(registration);
    }

    private SecurityPolicyType loadSecurityPolicyType() {
        OperationResult parentResult = new OperationResult(OPERATION_LOAD_RESET_PASSWORD_POLICY);
        try {
            return getModelInteractionService().getSecurityPolicy((PrismObject<? extends FocusType>) null, null, parentResult);
        } catch (Exception e) {
            LOGGER.warn("Cannot read credentials policy: " + e.getMessage(), e);
        }
        return null;
    }

    private boolean isForgotPasswordVisible(SecurityPolicyType securityPolicyType) {
        if (securityPolicyType == null) {
            return false;
        }

        if (hasNoSequenceConfiguredForReset(securityPolicyType)) {
            return false;
        }

        CredentialsPolicyType creds = securityPolicyType.getCredentials();
        // TODO: Not entirely correct. This means we have reset somehow configured, but not necessarily enabled.
        return creds != null
                && ((creds.getSecurityQuestions() != null
                && creds.getSecurityQuestions().getQuestionNumber() != null) || (securityPolicyType.getCredentialsReset() != null));
    }

    private boolean hasNoSequenceConfiguredForReset(SecurityPolicyType securityPolicyType) {
        String sequenceName = getResetAuthenticationSequenceName(securityPolicyType);
        if (StringUtils.isBlank(sequenceName)) {
            return true;
        }

        return getSequenceUrlSuffix(sequenceName, securityPolicyType) == null;
    }

    private String getSequenceUrlSuffix(String sequenceName, SecurityPolicyType securityPolicyType) {
        AuthenticationSequenceType sequence = SecurityUtils.getSequenceByName(sequenceName, securityPolicyType.getAuthentication());
        if (sequence == null) {
            return null;
        }
        return sequence.getChannel() != null ? sequence.getChannel().getUrlSuffix() : null;
    }

    private String getResetAuthenticationSequenceName(SecurityPolicyType securityPolicyType) {
        if (securityPolicyType == null) {
            return null;
        }

        CredentialsResetPolicyType credentialsResetPolicyType = securityPolicyType.getCredentialsReset();
        if (credentialsResetPolicyType == null) {
            return null;
        }

        return credentialsResetPolicyType.getAuthenticationSequenceName();
    }

    private String getForgotPasswordHref(SecurityPolicyType securityPolicyType) {
        String sequenceSuffix = getSequenceUrlSuffix(getResetAuthenticationSequenceName(securityPolicyType), securityPolicyType);
        return "./" + ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE + "/" + sequenceSuffix;
    }

    private boolean isRegistrationVisible() {
        OperationResult parentResult = new OperationResult(OPERATION_LOAD_REGISTRATION_POLICY);

        RegistrationsPolicyType registrationPolicies = null;
        try {
            Task task = createAnonymousTask(OPERATION_LOAD_REGISTRATION_POLICY);
            registrationPolicies = getModelInteractionService().getFlowPolicy(null, task, parentResult);

        } catch (CommonException e) {
            LOGGER.warn("Cannot read credentials policy: " + e.getMessage(), e);
        }

        return registrationPolicies != null
                && registrationPolicies.getSelfRegistration() != null;
    }

    private String getRegistrationHref(SecurityPolicyType securityPolicy) {
        SelfRegistrationPolicyType selfRegistrationPolicy = SecurityPolicyUtil.getSelfRegistrationPolicy(securityPolicy);
        if (selfRegistrationPolicy == null || StringUtils.isBlank(selfRegistrationPolicy.getAdditionalAuthenticationName())) {
            return null;
        }

        AuthenticationSequenceType sequence = SecurityUtils.getSequenceByName(selfRegistrationPolicy.getAdditionalAuthenticationName(),
                securityPolicy.getAuthentication());
        if (sequence == null || sequence.getChannel() == null || sequence.getChannel().getUrlSuffix() == null) {
            return null;
        }

        return "./" + ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE + "/" + sequence.getChannel().getUrlSuffix();

    }

    private String getUrlProcessingLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof MidpointAuthentication)) {
            return "./spring_security_login";
        }

        MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
        ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
        if (isModuleApplicable(moduleAuthentication)) {
            String prefix = moduleAuthentication.getPrefix();
            return SecurityUtils.stripSlashes(prefix) + "/spring_security_login";
        }

        return "./spring_security_login";
    }

    private boolean isModuleApplicable(ModuleAuthentication moduleAuthentication) {
        return (moduleAuthentication instanceof LoginFormModuleAuthentication
                || moduleAuthentication instanceof LdapModuleAuthentication);
    }

}
