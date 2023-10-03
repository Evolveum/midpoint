/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.channel;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

/**
 * @author skublik
 */

public class ResetPasswordAuthenticationChannel extends AuthenticationChannelImpl {


    public ResetPasswordAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_RESET_PASSWORD_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return "/resetPassword";
    }

    @Override
    public Authorization resolveAuthorization(Authorization autz) {
        if (autz == null) {
            return null;
        }
        Authorization retAutz = autz.clone();
        retAutz.getAction().removeIf(action ->
                !AuthorizationConstants.AUTZ_UI_RESET_PASSWORD_URL.equals(action)
                        && action.contains(AuthorizationConstants.NS_AUTHORIZATION_UI));

        if (retAutz.getAction().isEmpty()) {
            return null;
        }

        return retAutz;
    }
}
