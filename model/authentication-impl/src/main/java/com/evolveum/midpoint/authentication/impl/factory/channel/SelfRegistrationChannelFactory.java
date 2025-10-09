/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.factory.channel;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.authentication.impl.channel.SelfRegistrationAuthenticationChannel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

/**
 * @author skublik
 */
@Component
public class SelfRegistrationChannelFactory extends AbstractChannelFactory {
    @Override
    public boolean match(String channelId) {
        return SchemaConstants.CHANNEL_SELF_REGISTRATION_URI.equals(channelId);
    }

    @Override
    public AuthenticationChannel createAuthChannel(AuthenticationSequenceChannelType channel) {
        return new SelfRegistrationAuthenticationChannel(channel);
    }

    @Override
    protected Integer getOrder() {
        return 10;
    }
}
