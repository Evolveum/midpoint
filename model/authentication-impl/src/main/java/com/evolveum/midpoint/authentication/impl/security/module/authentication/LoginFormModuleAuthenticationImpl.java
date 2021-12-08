/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.security.module.authentication;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

/**
 * @author skublik
 */

public class LoginFormModuleAuthenticationImpl extends CredentialModuleAuthenticationImplImpl {

    public LoginFormModuleAuthenticationImpl() {
        super(AuthenticationModuleNameConstants.LOGIN_FORM);
    }

    public ModuleAuthenticationImpl clone() {
        LoginFormModuleAuthenticationImpl module = new LoginFormModuleAuthenticationImpl();
        module.setAuthentication(this.getAuthentication());
        super.clone(module);
        return module;
    }
}
