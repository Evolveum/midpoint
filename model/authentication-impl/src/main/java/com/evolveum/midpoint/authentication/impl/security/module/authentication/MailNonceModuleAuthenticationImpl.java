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

public class MailNonceModuleAuthenticationImpl extends CredentialModuleAuthenticationImplImpl {

    public MailNonceModuleAuthenticationImpl() {
        super(AuthenticationModuleNameConstants.MAIL_NONCE);
    }

    public ModuleAuthenticationImpl clone() {
        MailNonceModuleAuthenticationImpl module = new MailNonceModuleAuthenticationImpl();
        module.setAuthentication(this.getAuthentication());
        super.clone(module);
        return module;
    }

}
