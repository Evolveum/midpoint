/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.module.authentication.CredentialModuleAuthenticationImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpAuthenticationModuleType;

public class OtpModuleAuthenticationImpl extends CredentialModuleAuthenticationImpl {

    private OtpAuthenticationModuleType module;

    public OtpModuleAuthenticationImpl(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.OTP, sequenceModule);
    }

    public OtpAuthenticationModuleType getModule() {
        return module;
    }

    public void setModule(OtpAuthenticationModuleType module) {
        this.module = module;
    }
}
