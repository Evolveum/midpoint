/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.login;

import com.evolveum.midpoint.authentication.api.AuthModule;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.stream.Collectors;

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

    private static final String ID_RESET_PASSWORD = "resetPassword";
    private static final String ID_SELF_REGISTRATION = "selfRegistration";
    private static final String ID_CSRF_FIELD = "csrfField";
    private static final String ID_FORM = "form";

    private static final String DOT_CLASS = PageLogin.class.getName() + ".";
    protected static final String OPERATION_LOAD_RESET_PASSWORD_POLICY = DOT_CLASS + "loadPasswordResetPolicy";

    private final LoadableDetachableModel<SecurityPolicyType> securityPolicyModel;


    public PageLogin() {
        super(null);

        securityPolicyModel = new LoadableDetachableModel<>() {
            @Override
            protected SecurityPolicyType load() {
                Task task = createAnonymousTask(OPERATION_LOAD_RESET_PASSWORD_POLICY);
                OperationResult parentResult = new OperationResult(OPERATION_LOAD_RESET_PASSWORD_POLICY);
                try {
                    return getModelInteractionService().getSecurityPolicy((PrismObject<? extends FocusType>) null, task, parentResult);
                } catch (CommonException e) {
                    LOGGER.warn("Cannot read credentials policy: " + e.getMessage(), e);
                }
                return null;
            }
        };
    }

    @Override
    protected void initCustomLayout() {
        MidpointForm form = new MidpointForm(ID_FORM);
        form.add(AttributeModifier.replace("action", (IModel<String>) this::getUrlProcessingLogin));
        add(form);

        SecurityPolicyType securityPolicy = loadSecurityPolicyType();
        addForgotPasswordLink(securityPolicy);
        addRegistrationLink(securityPolicy);

        WebMarkupContainer csrfField = SecurityUtils.createHiddenInputForCsrf(ID_CSRF_FIELD);
        form.add(csrfField);
    }

    private void addForgotPasswordLink(SecurityPolicyType securityPolicy) {
        String urlResetPass = getPasswordResetUrl(securityPolicy);
        ExternalLink link = new ExternalLink(ID_RESET_PASSWORD, urlResetPass);

        link.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(urlResetPass)));
        add(link);
    }

    private String getPasswordResetUrl(SecurityPolicyType securityPolicy) {
        String resetSequenceIdOrName = getResetPasswordAuthenticationSequenceName(securityPolicy);
        if (StringUtils.isBlank(resetSequenceIdOrName)) {
            return "";
        }

        AuthenticationsPolicyType authenticationPolicy = securityPolicy.getAuthentication();
        AuthenticationSequenceType sequence = SecurityUtils.getSequenceByIdentifier(resetSequenceIdOrName, authenticationPolicy);
        if (sequence == null) {
            // this lookup by name will be (probably) eventually removed
            sequence = SecurityUtils.getSequenceByName(resetSequenceIdOrName, authenticationPolicy);
        }
        if (sequence == null) {
            LOGGER.warn("Password reset authentication sequence '{}' does not exist", resetSequenceIdOrName);
            return "";
        }

        if (sequence.getChannel() == null || StringUtils.isBlank(sequence.getChannel().getUrlSuffix())) {
            String message = "Sequence with name " + resetSequenceIdOrName + " doesn't contain urlSuffix";
            LOGGER.error(message, new IllegalArgumentException(message));
            error(message);
            return "";
        }
        return "./" + ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE + "/" + sequence.getChannel().getUrlSuffix();
    }

    private void addRegistrationLink(SecurityPolicyType securityPolicyType) {

        String urlRegistration = getRegistrationUrl(securityPolicyType);
        ExternalLink registration = new ExternalLink(ID_SELF_REGISTRATION, urlRegistration);

        registration.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(urlRegistration)));

        add(registration);
    }

    private SecurityPolicyType loadSecurityPolicyType() {
        return securityPolicyModel.getObject();
    }

    private String getResetPasswordAuthenticationSequenceName(SecurityPolicyType securityPolicyType) {
        if (securityPolicyType == null) {
            return null;
        }

        CredentialsResetPolicyType credentialsResetPolicyType = securityPolicyType.getCredentialsReset();
        if (credentialsResetPolicyType == null) {
            return null;
        }

        return credentialsResetPolicyType.getAuthenticationSequenceName();
    }

    private String getRegistrationUrl(SecurityPolicyType securityPolicy) {
        if (securityPolicy == null) {
            return "";
        }
        SelfRegistrationPolicyType selfRegistrationPolicy = SecurityPolicyUtil.getSelfRegistrationPolicy(securityPolicy);
        if (selfRegistrationPolicy == null || StringUtils.isBlank(selfRegistrationPolicy.getAdditionalAuthenticationSequence())) {
            return "";
        }

        AuthenticationSequenceType sequence = SecurityUtils.getSequenceByIdentifier(selfRegistrationPolicy.getAdditionalAuthenticationSequence(), securityPolicy.getAuthentication());
        if (sequence == null) {
            sequence = SecurityUtils.getSequenceByName(selfRegistrationPolicy.getAdditionalAuthenticationSequence(),
                    securityPolicy.getAuthentication());
        }
        if (sequence == null || sequence.getChannel() == null || sequence.getChannel().getUrlSuffix() == null) {
            return "";
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
        if (isModuleApplicable(moduleAuthentication)){
            String prefix = moduleAuthentication.getPrefix();
            return AuthUtil.stripSlashes(prefix) + "/spring_security_login";
        }

        return "./spring_security_login";
    }

    private boolean isModuleApplicable(ModuleAuthentication moduleAuthentication) {
        return moduleAuthentication != null && (AuthenticationModuleNameConstants.LOGIN_FORM.equals(moduleAuthentication.getModuleTypeName())
                || AuthenticationModuleNameConstants.LDAP.equals(moduleAuthentication.getModuleTypeName()));
    }

    @Override
    protected void onDetach() {
        securityPolicyModel.detach();
        super.onDetach();
    }

    @Override
    protected IModel<String> getLoginPanelTitleModel() {
        return createStringResource("PageLogin.loginToYourAccount");
    }

    @Override
    protected IModel<String> getLoginPanelDescriptionModel() {
        return severalLoginFormModulesExist() ?
                createStringResource("PageLogin.panelDescriptionWithModuleName", getProcessingModuleName())
                : createStringResource("PageLogin.enterAccountDetails");
    }

    private boolean severalLoginFormModulesExist() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            int loginFormModulesCount = (int) mpAuthentication.getAuthModules()
                    .stream()
                    .filter(module -> isModuleApplicable(module.getBaseModuleAuthentication()))
                    .count();
            return loginFormModulesCount > 1;
        }
        return false;
    }

    private String getProcessingModuleName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof MidpointAuthentication) {
            MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
            ModuleAuthentication module = mpAuthentication.getProcessingModuleAuthentication();
            return module != null ? module.getModuleIdentifier() : "";
        }
        return "";
    }
}
