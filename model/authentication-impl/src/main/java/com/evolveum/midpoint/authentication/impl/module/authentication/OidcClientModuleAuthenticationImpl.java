/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.config.RemoteModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

import com.evolveum.midpoint.authentication.impl.util.ModuleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import java.io.Serializable;

/**
 * @author skublik
 */

public class OidcClientModuleAuthenticationImpl extends RemoteModuleAuthenticationImpl implements RemoteModuleAuthentication, Serializable {

    public OidcClientModuleAuthenticationImpl(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.OIDC, sequenceModule);
        setType(ModuleType.REMOTE);
        setState(AuthenticationModuleState.LOGIN_PROCESSING);
    }

    @Override
    public ModuleAuthenticationImpl clone() {
        OidcClientModuleAuthenticationImpl module = new OidcClientModuleAuthenticationImpl(this.getSequenceModule());
        module.setProviders(this.getProviders());
        Authentication actualAuth = SecurityContextHolder.getContext().getAuthentication();
        Authentication newAuthentication = this.getAuthentication();
        if (actualAuth instanceof MidpointAuthentication
                && ((MidpointAuthentication) actualAuth).getAuthentications() != null
                && !((MidpointAuthentication) actualAuth).getAuthentications().isEmpty()) {
            ModuleAuthentication actualModule = ((MidpointAuthentication) actualAuth).getAuthentications().get(0);
            if (actualModule instanceof OidcClientModuleAuthenticationImpl
                    && actualModule.getAuthentication() instanceof OAuth2LoginAuthenticationToken) {
                newAuthentication = actualModule.getAuthentication();
            }
        }
        module.setAuthentication(newAuthentication);
        super.clone(module);
        return module;
    }
}
