/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.login;

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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author mserbak
 * @author lskublik
 */
@PageDescriptor(urls = {
        @Url(mountUrl = "/login", matchUrlForSecurity = "/login")
}, permitAll = true, loginPage = true, authModule = AuthenticationModuleNameConstants.LOGIN_FORM)
public class PageLogin extends AbstractPageLogin {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageLogin.class);

    private static final String ID_FORGET_PASSWORD = "forgetpassword";
    private static final String ID_SELF_REGISTRATION = "selfRegistration";
    private static final String ID_CSRF_FIELD = "csrfField";
    private static final String ID_FORM = "form";

    private static final String DOT_CLASS = PageLogin.class.getName() + ".";
    protected static final String OPERATION_LOAD_RESET_PASSWORD_POLICY = DOT_CLASS + "loadPasswordResetPolicy";

    private final LoadableDetachableModel<SecurityPolicyType> securityPolicyModel;

    public PageLogin() {

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

    private SecurityPolicyType getSecurityPolicy() {
        return securityPolicyModel.getObject();
    }

    @Override
    protected void initCustomLayer() {
        MidpointForm form = new MidpointForm(ID_FORM);
        form.add(AttributeModifier.replace("action", (IModel<String>) this::getUrlProcessingLogin));
        add(form);

        SecurityPolicyType securityPolicy = getSecurityPolicy();
        String urlResetPass = "";
        if (securityPolicy != null && securityPolicy.getCredentialsReset() != null
                && StringUtils.isNotBlank(securityPolicy.getCredentialsReset().getAuthenticationSequenceName())) {
            AuthenticationSequenceType sequence = SecurityUtils.getSequenceByName(securityPolicy.getCredentialsReset().getAuthenticationSequenceName(), securityPolicy.getAuthentication());
            if (sequence != null) {

                if (sequence.getChannel() == null || StringUtils.isBlank(sequence.getChannel().getUrlSuffix())) {
                    String message = "Sequence with name " + securityPolicy.getCredentialsReset().getAuthenticationSequenceName() + " doesn't contain urlSuffix";
                    LOGGER.error(message, new IllegalArgumentException(message));
                    error(message);
                }
                urlResetPass = "./" + ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE + "/" + sequence.getChannel().getUrlSuffix();
            }
        }

        ExternalLink link = new ExternalLink(ID_FORGET_PASSWORD, urlResetPass);

        String finalUrlResetPass = urlResetPass;
        link.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(finalUrlResetPass)));
        form.add(link);

        String urlRegistration = "";
        if (securityPolicy != null) {
            SelfRegistrationPolicyType policy = SecurityPolicyUtil.getSelfRegistrationPolicy(securityPolicy);
            if (policy != null) {
                String sequenceName = policy.getAdditionalAuthenticationSequence();
                if (StringUtils.isNotBlank(sequenceName)) {
                    AuthenticationSequenceType sequence = SecurityUtils.getSequenceByName(sequenceName, securityPolicy.getAuthentication());
                    if (sequence != null) {
                        urlRegistration = "./" + ModuleWebSecurityConfiguration.DEFAULT_PREFIX_OF_MODULE + "/" + sequence.getChannel().getUrlSuffix();
                    }
                }
            }
        }

        ExternalLink registration = new ExternalLink(ID_SELF_REGISTRATION, urlRegistration);
        String finalUrlRegistration = urlRegistration;
        registration.add(new VisibleBehaviour(() -> StringUtils.isNotBlank(finalUrlRegistration)));

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
                    && (AuthenticationModuleNameConstants.LOGIN_FORM.equals(moduleAuthentication.getNameOfModuleType())
                    || AuthenticationModuleNameConstants.LDAP.equals(moduleAuthentication.getNameOfModuleType()))){
                String prefix = moduleAuthentication.getPrefix();
                return AuthUtil.stripSlashes(prefix) + "/spring_security_login";
            }
        }

        return "./spring_security_login";
    }

    @Override
    protected void onDetach() {
        securityPolicyModel.detach();
        super.onDetach();
    }
}
