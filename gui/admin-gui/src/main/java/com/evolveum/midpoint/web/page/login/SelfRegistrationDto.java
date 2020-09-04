/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.login;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.model.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.security.module.authentication.MailNonceModuleAuthentication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class SelfRegistrationDto implements Serializable {

    enum AuthenticationPolicy {
        MAIL, SMS, NONE
    }

    private static final long serialVersionUID = 1L;

    private String name;
    private List<ObjectReferenceType> defaultRoles;
    private MailAuthenticationPolicyType mailAuthenticationPolicy;
    private SmsAuthenticationPolicyType smsAuthenticationPolicy;
    private NonceCredentialsPolicyType noncePolicy;
    private String additionalAuthentication;
    private AuthenticationsPolicyType authenticationPolicy;

    private String requiredLifecycleState;
    private String initialLifecycleState;

    private ObjectReferenceType formRef;

    public void initSelfRegistrationDto(SecurityPolicyType securityPolicy) throws SchemaException {
        if (securityPolicy == null) {
            return;
        }
        SelfRegistrationPolicyType selfRegistration = getSelfRegistrationPolicy(securityPolicy);
        if (selfRegistration == null) {
            return;
        }

        init(securityPolicy, selfRegistration);

    }

    public void initPostAuthenticationDto(SecurityPolicyType securityPolicy) throws SchemaException {
        if (securityPolicy == null) {
            return;
        }
        SelfRegistrationPolicyType selfRegistration = getPostAuthenticationPolicy(securityPolicy);
        if (selfRegistration == null) {
            return;
        }

        init(securityPolicy, selfRegistration);
    }

    private void init(SecurityPolicyType securityPolicy, SelfRegistrationPolicyType selfRegistration) throws SchemaException {
        this.name = selfRegistration.getName();
        this.defaultRoles = selfRegistration.getDefaultRole();
        this.initialLifecycleState = selfRegistration.getInitialLifecycleState();
        this.requiredLifecycleState = selfRegistration.getRequiredLifecycleState();
        this.additionalAuthentication = selfRegistration.getAdditionalAuthenticationName();
        this.authenticationPolicy = securityPolicy.getAuthentication();

        this.formRef = selfRegistration.getFormRef();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        MailNonceModuleAuthentication mailModuleAuthentication = null;
        if (authentication instanceof MidpointAuthentication) {
            ModuleAuthentication moduleAuthentication = ((MidpointAuthentication) authentication).getProcessingModuleAuthentication();
            if (moduleAuthentication instanceof MailNonceModuleAuthentication) {
                mailModuleAuthentication = (MailNonceModuleAuthentication) moduleAuthentication;
            }
        }
        if (mailModuleAuthentication != null && mailModuleAuthentication.getCredentialName() != null) {
            noncePolicy = SecurityPolicyUtil.getCredentialPolicy(mailModuleAuthentication.getCredentialName(), securityPolicy);
        } else {
            AbstractAuthenticationPolicyType authPolicy = SecurityPolicyUtil.getAuthenticationPolicy(
                    selfRegistration.getAdditionalAuthenticationName(), securityPolicy);

            if (authPolicy instanceof MailAuthenticationPolicyType) {
                this.mailAuthenticationPolicy = (MailAuthenticationPolicyType) authPolicy;
                noncePolicy = SecurityPolicyUtil.getCredentialPolicy(((MailAuthenticationPolicyType) authPolicy).getMailNonce(), securityPolicy);
            } else if (authPolicy instanceof SmsAuthenticationPolicyType) {
                this.smsAuthenticationPolicy = (SmsAuthenticationPolicyType) authPolicy;
                noncePolicy = SecurityPolicyUtil.getCredentialPolicy(((SmsAuthenticationPolicyType) authPolicy).getSmsNonce(), securityPolicy);
            }
        }
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(name) && CollectionUtils.isEmpty(defaultRoles)
                && mailAuthenticationPolicy == null && smsAuthenticationPolicy == null && noncePolicy == null;
    }

    public AuthenticationPolicy getAuthenticationMethod() {
        if (mailAuthenticationPolicy != null) {
            return AuthenticationPolicy.MAIL;
        }

        if (smsAuthenticationPolicy != null) {
            return AuthenticationPolicy.SMS;
        }

        return AuthenticationPolicy.NONE;
    }

    private SelfRegistrationPolicyType getSelfRegistrationPolicy(SecurityPolicyType securityPolicyType) {
        RegistrationsPolicyType flowPolicy = securityPolicyType.getFlow();
        SelfRegistrationPolicyType selfRegistrationPolicy = null;
        if (flowPolicy != null) {
            selfRegistrationPolicy = flowPolicy.getSelfRegistration();
        }

        if (selfRegistrationPolicy != null) {
            return selfRegistrationPolicy;
        }

        RegistrationsPolicyType registrationPolicy = securityPolicyType.getRegistration();

        if (registrationPolicy == null) {
            return null;
        }

        return registrationPolicy.getSelfRegistration();
    }

    private SelfRegistrationPolicyType getPostAuthenticationPolicy(SecurityPolicyType securityPolicyType) {
        RegistrationsPolicyType flowPolicy = securityPolicyType.getFlow();
        SelfRegistrationPolicyType selfRegistrationPolicy = null;
        if (flowPolicy != null) {
            selfRegistrationPolicy = flowPolicy.getPostAuthentication();
        }

        return selfRegistrationPolicy;
    }

    public boolean isMailMailAuthentication() {
        return mailAuthenticationPolicy != null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ObjectReferenceType> getDefaultRoles() {
        return defaultRoles;
    }

    public void setDefaultRoles(List<ObjectReferenceType> defaultRoles) {
        this.defaultRoles = defaultRoles;
    }

    public MailAuthenticationPolicyType getMailAuthenticationPolicy() {
        return mailAuthenticationPolicy;
    }

    public void setMailAuthenticationPolicy(MailAuthenticationPolicyType mailAuthenticationPolicy) {
        this.mailAuthenticationPolicy = mailAuthenticationPolicy;
    }

    public SmsAuthenticationPolicyType getSmsAuthenticationPolicy() {
        return smsAuthenticationPolicy;
    }

    public void setSmsAuthenticationPolicy(SmsAuthenticationPolicyType smsAuthenticationPolicy) {
        this.smsAuthenticationPolicy = smsAuthenticationPolicy;
    }

    public NonceCredentialsPolicyType getNoncePolicy() {
        return noncePolicy;
    }

    public void setNoncePolicy(NonceCredentialsPolicyType noncePolicy) {
        this.noncePolicy = noncePolicy;
    }

    public String getInitialLifecycleState() {
        return initialLifecycleState;
    }

    public void setInitialLifecycleState(String initialLifecycleState) {
        this.initialLifecycleState = initialLifecycleState;
    }

    public String getRequiredLifecycleState() {
        return requiredLifecycleState;
    }

    public void setRequiredLifecycleState(String requiredLifecycleState) {
        this.requiredLifecycleState = requiredLifecycleState;
    }

    public ObjectReferenceType getFormRef() {
        return formRef;
    }

    public String getAdditionalAuthentication() {
        return additionalAuthentication;
    }

    public AuthenticationsPolicyType getAuthenticationPolicy() {
        return authenticationPolicy;
    }
}
