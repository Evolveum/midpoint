/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleNecessityType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

/**
 * @author skublik
 */

public class MailNonceModuleAuthenticationImpl extends CredentialModuleAuthenticationImpl {

    public MailNonceModuleAuthenticationImpl(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.MAIL_NONCE, sequenceModule);
    }

    public ModuleAuthenticationImpl clone() {
        MailNonceModuleAuthenticationImpl module = new MailNonceModuleAuthenticationImpl(this.getSequenceModule());
        module.setAuthentication(this.getAuthentication());
        super.clone(module);
        return module;
    }

}
