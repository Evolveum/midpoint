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

public class LoginFormModuleAuthentication extends PasswordModuleAuthentication {

    public LoginFormModuleAuthentication() {
        super(NameOfModuleType.LOGIN_FORM);
    }

    public ModuleAuthentication clone() {
        LoginFormModuleAuthentication module = new LoginFormModuleAuthentication();
        super.clone(module);
        return module;
    }
}
