/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.channel;

import java.util.Collection;
import java.util.Collections;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;

/**
 * @author skublik
 */

public class SelfRegistrationAuthenticationChannel extends AuthenticationChannelImpl {

    public SelfRegistrationAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_SELF_REGISTRATION_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return "/registration/result";
    }

    public String getPathAfterUnsuccessfulAuthentication() {
        return "/";
    }

    @Override
    public String getSpecificLoginUrl() {
        return "/registration";
    }

    @Override
    public boolean isSupportActivationByChannel() {
        return false;
    }

    @Override
    public Authorization resolveAuthorization(Authorization autz) {
        return null;
    }

    @Override
    public Authorization getAdditionalAuthority() {
        return new Authorization(new AuthorizationType().action(AuthorizationConstants.AUTZ_UI_SELF_REGISTRATION_FINISH_URL));
    }
}
