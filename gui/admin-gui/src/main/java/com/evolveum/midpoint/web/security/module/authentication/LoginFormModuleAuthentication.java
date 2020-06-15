/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.authentication;

import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.AuthenticationModuleNameConstants;

/**
 * @author skublik
 */

public class LoginFormModuleAuthentication extends CredentialModuleAuthentication {

    public LoginFormModuleAuthentication() {
        super(AuthenticationModuleNameConstants.LOGIN_FORM);
    }

    public ModuleAuthentication clone() {
        LoginFormModuleAuthentication module = new LoginFormModuleAuthentication();
        super.clone(module);
        return module;
    }
}
