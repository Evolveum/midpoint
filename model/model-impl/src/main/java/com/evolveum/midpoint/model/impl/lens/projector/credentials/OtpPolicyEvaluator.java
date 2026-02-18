/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.credentials;

import java.util.List;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialsPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OtpCredentialsType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class OtpPolicyEvaluator<F extends FocusType> extends
        CredentialPolicyEvaluator<OtpCredentialsType, OtpCredentialsPolicyType, F> {

    // todo implement similar to SecurityQuestionsPolicyEvaluator
    private OtpPolicyEvaluator(Builder<F> builder) {
        super(builder);
    }

    @Override
    protected ItemPath getCredentialsContainerPath() {
        return SchemaConstants.PATH_OTPS;
    }

    @Override
    protected String getCredentialHumanReadableName() {
        return "OTP credentials";
    }

    @Override
    protected String getCredentialHumanReadableKey() {
        return "otpCredentials";
    }

    @Override
    protected OtpCredentialsPolicyType determineEffectiveCredentialPolicy() {
        return SecurityUtil.getEffectiveOtpCredentialsPolicy(getSecurityPolicy());
    }

    @Override
    protected void validateCredentialContainerValues(PrismContainerValue<OtpCredentialsType> cVal)
            throws PolicyViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {
        List<OtpCredentialType> otps = cVal.asContainerable().getOtp();
        for (OtpCredentialType otp : otps) {
            ProtectedStringType secret = otp.getSecret();
            validateProtectedStringValue(secret);
        }
    }

    public static class Builder<F extends FocusType> extends CredentialPolicyEvaluator.Builder<F> {
        public OtpPolicyEvaluator<F> build() {
            return new OtpPolicyEvaluator<>(this);
        }
    }
}
