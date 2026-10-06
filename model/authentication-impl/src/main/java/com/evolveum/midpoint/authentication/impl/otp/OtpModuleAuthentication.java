/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.otp;

import org.apache.commons.lang3.BooleanUtils;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.api.config.MidpointAuthentication;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.module.authentication.CredentialModuleAuthenticationImpl;
import com.evolveum.midpoint.authentication.impl.module.authentication.ModuleAuthenticationImpl;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class OtpModuleAuthentication extends CredentialModuleAuthenticationImpl {

    private static final Trace LOGGER = TraceManager.getTrace(OtpModuleAuthentication.class);

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
        boolean forceSetup = isForceSetupWhenEmptyCredentials();
        if (!canSkipWhenEmptyCredentials() && !forceSetup) {
            return super.applicable();
        }

        GuiProfiledPrincipal principal = AuthUtil.getPrincipalUser();
        if (principal == null) {
            return true;
        }

        if (hasVerifiedTotp(principal.getFocus())) {
            return true;
        }

        if (forceSetup && !isGuiUserChannel()) {
            // There's no place where the setup could be enforced, this is most probably a misconfiguration.
            LOGGER.debug("Empty credentials policy 'forceSetup' of module '{}' is supported only in GUI (user) channel, "
                    + "authentication will fail for users without TOTP credentials.", getModuleIdentifier());
            return true;
        }

        return false;
    }

    private boolean hasVerifiedTotp(FocusType focus) {
        CredentialsType credentials = focus.getCredentials();
        OtpCredentialsType otpCredentials = credentials != null ? credentials.getOtps() : null;
        if (otpCredentials == null) {
            return false;
        }

        return otpCredentials.getTotp().stream().anyMatch(otp -> BooleanUtils.isTrue(otp.isVerified()));
    }

    private boolean isGuiUserChannel() {
        MidpointAuthentication authentication = AuthUtil.getMidpointAuthenticationNotRequired();
        AuthenticationChannel channel = authentication != null ? authentication.getAuthenticationChannel() : null;

        return channel != null && SchemaConstants.CHANNEL_USER_URI.equals(channel.getChannelId());
    }
}
