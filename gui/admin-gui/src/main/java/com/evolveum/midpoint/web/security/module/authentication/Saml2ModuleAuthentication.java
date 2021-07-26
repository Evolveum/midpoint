/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.authentication;

import com.evolveum.midpoint.model.api.authentication.*;
import com.evolveum.midpoint.web.security.module.configuration.SamlMidpointAdditionalConfiguration;
import com.evolveum.midpoint.web.security.util.IdentityProvider;
import com.evolveum.midpoint.web.security.util.RequestState;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml.SamlAuthentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author skublik
 */

public class Saml2ModuleAuthentication extends ModuleAuthentication {

    private List<IdentityProvider> providers = new ArrayList<IdentityProvider>();
    private Map<String, SamlMidpointAdditionalConfiguration> additionalConfiguration;
    private RequestState requestState;

    public Saml2ModuleAuthentication() {
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
    public ModuleAuthentication clone() {
        Saml2ModuleAuthentication module = new Saml2ModuleAuthentication();
        module.setAdditionalConfiguration(this.getAdditionalConfiguration());
        module.setProviders(this.getProviders());
        Authentication actualAuth = SecurityContextHolder.getContext().getAuthentication();
        Authentication newAuthentication = this.getAuthentication();
        if (actualAuth instanceof MidpointAuthentication
                && ((MidpointAuthentication) actualAuth).getAuthentications() != null
                && !((MidpointAuthentication) actualAuth).getAuthentications().isEmpty()) {
            ModuleAuthentication actualModule = ((MidpointAuthentication) actualAuth).getAuthentications().get(0);
            if (actualModule instanceof Saml2ModuleAuthentication
                    && actualModule.getAuthentication() instanceof SamlAuthentication) {
                newAuthentication = actualModule.getAuthentication();
            }
        }
        module.setAuthentication(newAuthentication);
        super.clone(module);
        return module;
    }
}
