/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.module.authentication.CredentialModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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

    @Override
    public boolean applicable() {
        if (!canSkipWhenEmptyCredentials()) {
            return super.applicable();
        }

        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal == null) {
            return true;
        }

        if (!(principal instanceof MidPointPrincipal)) {
            return false;
        }

        FocusType focus = principal.getFocus();
        CredentialsType credentials = focus.getCredentials();
        if (credentials == null) {
            return false;
        }

        OtpCredentialsType otpCredentials = credentials.getOtps();
        if (otpCredentials == null) {
            return false;
        }

        return !otpCredentials.getTotp().isEmpty();
    }
}
