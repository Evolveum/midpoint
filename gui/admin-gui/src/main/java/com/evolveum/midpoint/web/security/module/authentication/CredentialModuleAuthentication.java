/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.authentication;

import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleType;
import com.evolveum.midpoint.model.api.authentication.StateOfModule;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;

/**
 * @author skublik
 */

public class CredentialModuleAuthentication extends ModuleAuthentication {

    private String credentialName = null;
    private Class<? extends CredentialPolicyType> credentialType;

    public CredentialModuleAuthentication(String nameOfType) {
        super(nameOfType);
        setType(ModuleType.LOCAL);
        setState(StateOfModule.LOGIN_PROCESSING);
    }

    public String getCredentialName() {
        return credentialName;
    }

    public void setCredentialName(String credentialName) {
        this.credentialName = credentialName;
    }

    public Class<? extends CredentialPolicyType> getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(Class<? extends CredentialPolicyType> credentialType) {
        this.credentialType = credentialType;
    }

    public ModuleAuthentication clone() {
        CredentialModuleAuthentication module = new CredentialModuleAuthentication(this.getNameOfModuleType());
        module.setCredentialName(getCredentialName());
        super.clone(module);
        return module;
    }

    @Override
    protected void clone(ModuleAuthentication module) {
        if (module instanceof CredentialModuleAuthentication) {
            ((CredentialModuleAuthentication)module).setCredentialName(getCredentialName());
        }
        super.clone(module);
    }
}
