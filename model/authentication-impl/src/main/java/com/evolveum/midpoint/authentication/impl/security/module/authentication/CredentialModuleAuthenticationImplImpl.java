/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.module.authentication;

import com.evolveum.midpoint.authentication.api.authentication.CredentialModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.security.util.ModuleType;
import com.evolveum.midpoint.authentication.api.StateOfModule;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;

/**
 * @author skublik
 */

public class CredentialModuleAuthenticationImplImpl extends ModuleAuthenticationImpl implements CredentialModuleAuthentication {

    private String credentialName = null;
    private Class<? extends CredentialPolicyType> credentialType;

    public CredentialModuleAuthenticationImplImpl(String nameOfType) {
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

    @Override
    public ModuleAuthenticationImpl clone() {
        CredentialModuleAuthenticationImplImpl module = new CredentialModuleAuthenticationImplImpl(this.getNameOfModuleType());
        module.setAuthentication(this.getAuthentication());
        clone(module);
        return module;
    }

    @Override
    protected void clone(ModuleAuthenticationImpl module) {
        if (module instanceof CredentialModuleAuthenticationImplImpl) {
            ((CredentialModuleAuthenticationImplImpl)module).setCredentialName(getCredentialName());
            ((CredentialModuleAuthenticationImplImpl)module).setCredentialType(getCredentialType());
        }
        module.setAuthentication(this.getAuthentication());
        super.clone(module);
    }
}
