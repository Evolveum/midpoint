/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.config.CredentialModuleAuthentication;
import com.evolveum.midpoint.authentication.impl.util.ModuleType;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialPolicyType;

/**
 * @author skublik
 */

public class CredentialModuleAuthenticationImpl extends ModuleAuthenticationImpl implements CredentialModuleAuthentication {

    private String credentialName = null;
    private Class<? extends CredentialPolicyType> credentialType;

    public CredentialModuleAuthenticationImpl(String nameOfType, AuthenticationSequenceModuleType sequenceModule) {
        super(nameOfType, sequenceModule);
        setType(ModuleType.LOCAL);
        setState(AuthenticationModuleState.LOGIN_PROCESSING);
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
        CredentialModuleAuthenticationImpl module =
                new CredentialModuleAuthenticationImpl(this.getModuleTypeName(), this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        clone(module);
        return module;
    }

    @Override
    protected void clone(ModuleAuthenticationImpl module) {
        if (module instanceof CredentialModuleAuthenticationImpl) {
            ((CredentialModuleAuthenticationImpl)module).setCredentialName(getCredentialName());
            ((CredentialModuleAuthenticationImpl)module).setCredentialType(getCredentialType());
        }
        module.setAuthentication(this.getAuthentication());
        super.clone(module);
    }
}
