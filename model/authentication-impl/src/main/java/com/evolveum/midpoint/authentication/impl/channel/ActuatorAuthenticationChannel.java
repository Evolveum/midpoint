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

public class ActuatorAuthenticationChannel extends AuthenticationChannelImpl {

    public ActuatorAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_ACTUATOR_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return "/actuator";
    }

    @Override
    public boolean isSupportGuiConfigByChannel() {
        return false;
    }

    @Override
    public Authorization resolveAuthorization(Authorization autz) {
        if (autz == null) {
            return null;
        }

        Authorization retAutz = autz.clone();
        retAutz.getAction().removeIf(action ->
                !action.startsWith(AuthorizationConstants.NS_AUTHORIZATION_ACTUATOR)
                        && !action.equals(AuthorizationConstants.AUTZ_ALL_URL)
                        && !action.equals(AuthorizationConstants.NS_AUTHORIZATION_UI));

        if (retAutz.getAction().isEmpty()) {
            return null;
        }

        return retAutz;
    }
}
