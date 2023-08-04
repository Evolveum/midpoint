/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.channel;

import java.util.Collection;
import java.util.Collections;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

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
    protected Collection<String> getAdditionalAuthoritiesList() {
        return Collections.singletonList(AuthorizationConstants.AUTZ_UI_SELF_REGISTRATION_FINISH_URL);
    }
}
