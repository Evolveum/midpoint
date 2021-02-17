/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.login;

import com.evolveum.midpoint.model.api.authentication.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.LdapModuleAuthentication;
import com.evolveum.midpoint.web.security.module.authentication.LoginFormModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.forgetpassword.PageForgotPassword;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.IModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;

/**
 * @author mserbak
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/login", matchUrlForSecurity = "/login")
}, permitAll = true, loginPage = true)
public class PageLogin extends AbstractPageLogin {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageLogin.class);

    private static final String ID_FORGET_PASSWORD = "forgetpassword";
    private static final String ID_SELF_REGISTRATION = "selfRegistration";
    private static final String ID_CSRF_FIELD = "csrfField";
    private static final String ID_FORM = "form";

    private static final String DOT_CLASS = PageLogin.class.getName() + ".";
    protected static final String OPERATION_LOAD_RESET_PASSWORD_POLICY = DOT_CLASS + "loadPasswordResetPolicy";
    private static final String OPERATION_LOAD_REGISTRATION_POLICY = DOT_CLASS + "loadRegistrationPolicy";

    public PageLogin() {
    }

    @Override
    protected void initCustomLayer() {
        MidpointForm form = new MidpointForm(ID_FORM);
        form.add(AttributeModifier.replace("action", new IModel<String>() {
            @Override
            public String getObject() {
                return getUrlProcessingLogin();
            }
        }));
        add(form);

        BookmarkablePageLink<String> link = new BookmarkablePageLink<>(ID_FORGET_PASSWORD, PageForgotPassword.class);
        OperationResult parentResult = new OperationResult(OPERATION_LOAD_RESET_PASSWORD_POLICY);
        SecurityPolicyType securityPolicy = null;
        try {
            securityPolicy = getModelInteractionService().getSecurityPolicy((PrismObject<? extends FocusType>) null, null, parentResult);
        } catch (CommonException e) {
            LOGGER.warn("Cannot read credentials policy: " + e.getMessage(), e);
        }
        SecurityPolicyType finalSecurityPolicy = securityPolicy;
        link.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {

                if (finalSecurityPolicy == null) {
                    return false;
                }

                CredentialsPolicyType creds = finalSecurityPolicy.getCredentials();

                // TODO: Not entirely correct. This means we have reset somehow configured, but not necessarily enabled.
                if (creds != null
                        && ((creds.getSecurityQuestions() != null
                        && creds.getSecurityQuestions().getQuestionNumber() != null) || (finalSecurityPolicy.getCredentialsReset() != null))) {
                    return true;
                }

                return false;
            }
        });
        if (securityPolicy != null && securityPolicy.getCredentialsReset() != null
                && StringUtils.isNotBlank(securityPolicy.getCredentialsReset().getAuthenticationSequenceName())) {
            AuthenticationSequenceType sequence = SecurityUtils.getSequenceByName(securityPolicy.getCredentialsReset().getAuthenticationSequenceName(), securityPolicy.getAuthentication());
            if (sequence != null) {
//                throw new IllegalArgumentException("Couldn't find sequence with name " + securityPolicy.getCredentialsReset().getAuthenticationSequenceName());

                if (sequence.getChannel() == null || StringUtils.isBlank(sequence.getChannel().getUrlSuffix())) {
                    throw new IllegalArgumentException("Sequence with name " + securityPolicy.getCredentialsReset().getAuthenticationSequenceName() + " doesn't contain urlSuffix");
                }
                link.add(AttributeModifier.replace("href", new IModel<String>() {
                    @Override
                    public String getObject() {
                        return "./" + ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE + "/" + sequence.getChannel().getUrlSuffix();
                    }
                }));
            }
        }
        form.add(link);

        BookmarkablePageLink<String> registration = new BookmarkablePageLink<>(ID_SELF_REGISTRATION, PageSelfRegistration.class);
        registration.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                OperationResult parentResult = new OperationResult(OPERATION_LOAD_REGISTRATION_POLICY);

                RegistrationsPolicyType registrationPolicies = null;
                try {
                    Task task = createAnonymousTask(OPERATION_LOAD_REGISTRATION_POLICY);
                    registrationPolicies = getModelInteractionService().getFlowPolicy(null, task, parentResult);

                    if (registrationPolicies == null || registrationPolicies.getSelfRegistration() == null) {
                        registrationPolicies = getModelInteractionService().getRegistrationPolicy(null, task, parentResult);
                    }

                } catch (CommonException e) {
                    LOGGER.warn("Cannot read credentials policy: " + e.getMessage(), e);
                }

                boolean linkIsVisible = false;
                if (registrationPolicies != null
                        && registrationPolicies.getSelfRegistration() != null) {
                    linkIsVisible = true;
                }

                return linkIsVisible;
            }
        });
        if (securityPolicy != null && securityPolicy.getRegistration() != null && securityPolicy.getRegistration().getSelfRegistration() != null
                && StringUtils.isNotBlank(securityPolicy.getRegistration().getSelfRegistration().getAdditionalAuthenticationName())) {
            AuthenticationSequenceType sequence = SecurityUtils.getSequenceByName(securityPolicy.getRegistration().getSelfRegistration().getAdditionalAuthenticationName(),
                    securityPolicy.getAuthentication());
            if (sequence != null) {
                registration.add(AttributeModifier.replace("href", new IModel<String>() {
                    @Override
                    public String getObject() {
                        return "./" + ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE + "/" + sequence.getChannel().getUrlSuffix();
                    }
                }));
            }
        }
        form.add(registration);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);
    }

    private String getUrlProcessingLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleAuthentication();
            if (moduleAuthentication != null
                    && (moduleAuthentication instanceof LoginFormModuleAuthentication
                    || moduleAuthentication instanceof LdapModuleAuthentication)){
                String prefix = moduleAuthentication.getPrefix();
                return stripSlashes(prefix) + "/spring_security_login";
            }
        }

        return "/midpoint/spring_security_login";
    }
}
