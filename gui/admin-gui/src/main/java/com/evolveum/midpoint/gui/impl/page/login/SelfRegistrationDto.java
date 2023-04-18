/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.login;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.evolveum.midpoint.authentication.api.config.CredentialModuleAuthentication;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.schema.util.SecurityPolicyUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class SelfRegistrationDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private List<ObjectReferenceType> defaultRoles;
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
        SelfRegistrationPolicyType selfRegistration = SecurityPolicyUtil.getSelfRegistrationPolicy(securityPolicy);
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
        this.additionalAuthentication = selfRegistration.getAdditionalAuthenticationSequence();
        this.authenticationPolicy = securityPolicy.getAuthentication();

        this.formRef = selfRegistration.getFormRef();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CredentialModuleAuthentication mailModuleAuthentication = null;
        if (authentication instanceof MidpointAuthentication) {
            ModuleAuthentication moduleAuthentication = ((MidpointAuthentication) authentication).getProcessingModuleAuthentication();
            if (moduleAuthentication instanceof CredentialModuleAuthentication
                    && AuthenticationModuleNameConstants.MAIL_NONCE.equals(moduleAuthentication.getModuleTypeName())) {
                mailModuleAuthentication = (CredentialModuleAuthentication) moduleAuthentication;
            }
        }
        if (mailModuleAuthentication != null && mailModuleAuthentication.getCredentialName() != null) {
            noncePolicy = SecurityPolicyUtil.getCredentialPolicy(mailModuleAuthentication.getCredentialName(), securityPolicy);
        }
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(name) && CollectionUtils.isEmpty(defaultRoles)
                && noncePolicy == null;
    }

    private SelfRegistrationPolicyType getPostAuthenticationPolicy(SecurityPolicyType securityPolicyType) {
        RegistrationsPolicyType flowPolicy = securityPolicyType.getFlow();
        SelfRegistrationPolicyType selfRegistrationPolicy = null;
        if (flowPolicy != null) {
            selfRegistrationPolicy = flowPolicy.getPostAuthentication();
        }

        return selfRegistrationPolicy;
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

    public NonceCredentialsPolicyType getNoncePolicy() {
        return noncePolicy;
    }

    public String getInitialLifecycleState() {
        return initialLifecycleState;
    }

    public String getRequiredLifecycleState() {
        return requiredLifecycleState;
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
