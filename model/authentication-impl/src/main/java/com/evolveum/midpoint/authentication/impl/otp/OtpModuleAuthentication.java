/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.module.authentication.CredentialModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpAuthenticationModuleType;

public class OtpModuleAuthentication extends CredentialModuleAuthenticationImpl {

    private OtpAuthenticationModuleType module;

    public OtpModuleAuthentication(AuthenticationSequenceModuleType sequenceModule) {
        super(AuthenticationModuleNameConstants.OTP, sequenceModule);
    }

    public OtpAuthenticationModuleType getModule() {
        return module;
    }

    public void setModule(OtpAuthenticationModuleType module) {
        this.module = module;
    }

    @Override
    public ModuleAuthenticationImpl clone() {
        OtpModuleAuthentication module = new OtpModuleAuthentication(this.getSequenceModule());
        clone(module);

        return module;
    }

    @Override
    protected void clone(ModuleAuthenticationImpl module) {
        super.clone(module);

        if (module instanceof OtpModuleAuthentication otp) {
            otp.setModule(this.getModule());
        }
    }
}
