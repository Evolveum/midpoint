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

public class SecurityQuestionFormModuleAuthentication extends CredentialModuleAuthentication {

    public SecurityQuestionFormModuleAuthentication() {
        super(AuthenticationModuleNameConstants.SECURITY_QUESTIONS_FORM);
    }

    public ModuleAuthentication clone() {
        SecurityQuestionFormModuleAuthentication module = new SecurityQuestionFormModuleAuthentication();
        super.clone(module);
        return module;
    }
}
