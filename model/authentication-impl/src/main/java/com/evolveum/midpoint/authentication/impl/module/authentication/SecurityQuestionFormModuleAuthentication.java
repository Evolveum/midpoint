/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;

/**
 * @author skublik
 */

public class SecurityQuestionFormModuleAuthentication extends CredentialModuleAuthenticationImpl {

    public SecurityQuestionFormModuleAuthentication() {
        super(AuthenticationModuleNameConstants.SECURITY_QUESTIONS_FORM);
    }

    public ModuleAuthenticationImpl clone() {
        SecurityQuestionFormModuleAuthentication module = new SecurityQuestionFormModuleAuthentication();
        module.setAuthentication(this.getAuthentication());
        super.clone(module);
        return module;
    }
}
