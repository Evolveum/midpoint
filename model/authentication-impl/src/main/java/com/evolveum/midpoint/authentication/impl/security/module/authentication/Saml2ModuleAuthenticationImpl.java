/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.module.authentication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.evolveum.midpoint.authentication.api.IdentityProvider;
import com.evolveum.midpoint.authentication.api.StateOfModule;
import com.evolveum.midpoint.authentication.api.authentication.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.authentication.Saml2ModuleAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.security.util.ModuleType;
import com.evolveum.midpoint.authentication.impl.security.util.RequestState;
import com.evolveum.midpoint.authentication.impl.security.module.configuration.SamlMidpointAdditionalConfiguration;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;

/**
 * @author skublik
 */

public class Saml2ModuleAuthenticationImpl extends ModuleAuthenticationImpl implements Saml2ModuleAuthentication,Serializable {

    private List<IdentityProvider> providers = new ArrayList<IdentityProvider>();
    private Map<String, SamlMidpointAdditionalConfiguration> additionalConfiguration;
    private RequestState requestState;

    public Saml2ModuleAuthenticationImpl() {
        super(AuthenticationModuleNameConstants.SAML_2);
        setType(ModuleType.REMOTE);
        setState(StateOfModule.LOGIN_PROCESSING);
    }

    public void setRequestState(RequestState requestState) {
        this.requestState = requestState;
    }

    public RequestState getRequestState() {
        return requestState;
    }

    public void setProviders(List<IdentityProvider> providers) {
        this.providers = providers;
    }

    public List<IdentityProvider> getProviders() {
        return providers;
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
            if (actualModule instanceof Saml2ModuleAuthentication
                    && actualModule.getAuthentication() instanceof Saml2AuthenticationToken) {
                newAuthentication = actualModule.getAuthentication();
            }
        }
        module.setAuthentication(newAuthentication);
        super.clone(module);
        return module;
    }
}
