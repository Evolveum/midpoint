/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.authentication;

import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleType;
import com.evolveum.midpoint.model.api.authentication.NameOfModuleType;
import com.evolveum.midpoint.model.api.authentication.StateOfModule;

/**
 * @author skublik
 */

public class PasswordModuleAuthentication  extends ModuleAuthentication {

    private String credentialName = null;

    public PasswordModuleAuthentication(NameOfModuleType nameOfType) {
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

    public ModuleAuthentication clone() {
        PasswordModuleAuthentication module = new PasswordModuleAuthentication(this.getNameOfModuleType());
        module.setCredentialName(getCredentialName());
        super.clone(module);
        return module;
    }

    @Override
    protected void clone(ModuleAuthentication module) {
        if (module instanceof PasswordModuleAuthentication) {
            ((PasswordModuleAuthentication)module).setCredentialName(getCredentialName());
        }
        super.clone(module);
    }
}
