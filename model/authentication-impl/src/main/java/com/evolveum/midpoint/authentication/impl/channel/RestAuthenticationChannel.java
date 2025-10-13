/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.channel;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

/**
 * @author skublik
 */
public class RestAuthenticationChannel extends AuthenticationChannelImpl {

    public RestAuthenticationChannel(AuthenticationSequenceChannelType channel) {
        super(channel);
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_REST_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        return "/ws/rest/self";
    }

    @Override
    public boolean isSupportGuiConfigByChannel() {
        return false;
    }


}
