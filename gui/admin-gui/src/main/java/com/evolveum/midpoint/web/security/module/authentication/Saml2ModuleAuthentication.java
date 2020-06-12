/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.authentication;

import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.model.api.authentication.ModuleType;
import com.evolveum.midpoint.model.api.authentication.StateOfModule;
import com.evolveum.midpoint.web.security.util.IdentityProvider;
import com.evolveum.midpoint.web.security.util.RequestState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author skublik
 */

public class Saml2ModuleAuthentication extends ModuleAuthentication {

    private List<IdentityProvider> providers = new ArrayList<IdentityProvider>();
    private Map<String, String> namesOfUsernameAttributes;
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

    public Map<String, String> getNamesOfUsernameAttributes() {
        return namesOfUsernameAttributes;
    }

    public void setNamesOfUsernameAttributes(Map<String, String> namesOfUsernameAttributes) {
        this.namesOfUsernameAttributes = namesOfUsernameAttributes;
    }

    @Override
    public ModuleAuthentication clone() {
        Saml2ModuleAuthentication module = new Saml2ModuleAuthentication();
        module.setNamesOfUsernameAttributes(this.getNamesOfUsernameAttributes());
        module.setProviders(this.getProviders());
        super.clone(module);
        return module;
    }
}
