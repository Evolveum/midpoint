/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.provider;

import com.evolveum.midpoint.authentication.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.security.module.authentication.CredentialModuleAuthenticationImplImpl;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.model.api.context.AbstractAuthenticationContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;

/**
 * @author skublik
 */

public abstract class AbstractCredentialProvider<T extends AbstractAuthenticationContext> extends MidPointAbstractAuthenticationProvider<T> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractCredentialProvider.class);

    public abstract Class getTypeOfCredential();

    public boolean supports(Class<?> authenticationClass, Authentication authentication) {
        if (!(authentication instanceof MidpointAuthentication)) {
            return supports(authenticationClass);
        }
        MidpointAuthentication mpAuthentication = (MidpointAuthentication) authentication;
        ModuleAuthenticationImpl moduleAuthentication = (ModuleAuthenticationImpl) getProcessingModule(mpAuthentication);
        if (mpAuthentication == null || moduleAuthentication == null || moduleAuthentication.getAuthentication() == null) {
            return false;
        }
        if (moduleAuthentication.getAuthentication() instanceof AnonymousAuthenticationToken) {
            return true; // hack for specific situation when user is anonymous, but accessDecisionManager resolve it
        }

        if (moduleAuthentication instanceof CredentialModuleAuthenticationImplImpl) {
            Class<? extends CredentialPolicyType> moduleCredentialType = ((CredentialModuleAuthenticationImplImpl) moduleAuthentication).getCredentialType();
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
