/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.channel;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

import java.util.Collection;
import java.util.Collections;

public class LoginRecoveryAuthenticationChannel extends AuthenticationChannelImpl {

    public LoginRecoveryAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_LOGIN_RECOVERY_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return "/loginRecovery";
    }

    @Override
    protected Collection<String> getAdditionalAuthoritiesList() {
        return Collections.singletonList(AuthorizationConstants.AUTZ_UI_USERNAME_RECOVERY_URL);
    }

}
