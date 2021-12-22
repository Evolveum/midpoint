/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import java.io.Serializable;
import java.util.Map;

import com.evolveum.midpoint.authentication.api.config.RemoteModuleAuthentication;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.config.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.util.ModuleType;
import com.evolveum.midpoint.authentication.impl.util.RequestState;
import com.evolveum.midpoint.authentication.impl.module.configuration.SamlMidpointAdditionalConfiguration;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;

/**
 * @author skublik
 */

public class Saml2ModuleAuthenticationImpl extends RemoteModuleAuthenticationImpl implements RemoteModuleAuthentication, Serializable {

    private Map<String, SamlMidpointAdditionalConfiguration> additionalConfiguration;
    private RequestState requestState;

    public Saml2ModuleAuthenticationImpl() {
        super(AuthenticationModuleNameConstants.SAML_2);
        setType(ModuleType.REMOTE);
        setState(AuthenticationModuleState.LOGIN_PROCESSING);
    }

    public void setRequestState(RequestState requestState) {
        this.requestState = requestState;
    }

    public RequestState getRequestState() {
        return requestState;
    }

    public Map<String, SamlMidpointAdditionalConfiguration> getAdditionalConfiguration() {
        return additionalConfiguration;
    }

    public void setAdditionalConfiguration(Map<String, SamlMidpointAdditionalConfiguration> additionalConfiguration) {
        this.additionalConfiguration = additionalConfiguration;
    }

    @Override
    public ModuleAuthenticationImpl clone() {
        Saml2ModuleAuthenticationImpl module = new Saml2ModuleAuthenticationImpl();
        module.setAdditionalConfiguration(this.getAdditionalConfiguration());
        module.setProviders(this.getProviders());
        Authentication actualAuth = SecurityContextHolder.getContext().getAuthentication();
        Authentication newAuthentication = this.getAuthentication();
        if (actualAuth instanceof MidpointAuthentication
                && ((MidpointAuthentication) actualAuth).getAuthentications() != null
                && !((MidpointAuthentication) actualAuth).getAuthentications().isEmpty()) {
            ModuleAuthentication actualModule = ((MidpointAuthentication) actualAuth).getAuthentications().get(0);
            if (actualModule instanceof Saml2ModuleAuthenticationImpl
                    && actualModule.getAuthentication() instanceof Saml2AuthenticationToken) {
                newAuthentication = actualModule.getAuthentication();
            }
        }
        module.setAuthentication(newAuthentication);
        super.clone(module);
        return module;
    }
}
