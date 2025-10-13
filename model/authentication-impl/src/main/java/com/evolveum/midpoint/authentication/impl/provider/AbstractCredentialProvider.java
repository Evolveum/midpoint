/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.provider;

import com.evolveum.midpoint.authentication.api.evaluator.AuthenticationEvaluator;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.module.authentication.CredentialModuleAuthenticationImpl;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.authentication.api.evaluator.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;

/**
 * @author skublik
 */

public abstract class AbstractCredentialProvider<T extends AbstractAuthenticationContext> extends MidpointAbstractAuthenticationProvider {

    protected abstract AuthenticationEvaluator<T, UsernamePasswordAuthenticationToken> getEvaluator();

    public abstract Class<? extends CredentialPolicyType> getTypeOfCredential();

    public boolean supports(Class<?> authenticationClass, Authentication authentication) {
        if (!(authentication instanceof MidpointAuthentication mpAuthentication)) {
            return supports(authenticationClass);
        }
        ModuleAuthentication moduleAuthentication = mpAuthentication.getProcessingModuleOrThrowException();
        if (moduleAuthentication == null || moduleAuthentication.getAuthentication() == null) {
            return false;
        }
        if (moduleAuthentication.getAuthentication() instanceof AnonymousAuthenticationToken) {
            return true; // hack for specific situation when user is anonymous, but accessDecisionManager resolve it
        }

        if (moduleAuthentication instanceof CredentialModuleAuthenticationImpl) {
            Class<? extends CredentialPolicyType> moduleCredentialType = ((CredentialModuleAuthenticationImpl) moduleAuthentication).getCredentialType();
            if (moduleCredentialType == null) {
                return false;
            }
            if (!getTypeOfCredential().equals(moduleCredentialType)) {
                return false;
            }
        }

        return supports(moduleAuthentication.getAuthentication().getClass());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((getEvaluator() == null) ? 0 : getEvaluator().hashCode())
                + ((getTypeOfCredential() == null) ? 0 : getTypeOfCredential().hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

}
