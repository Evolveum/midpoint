/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.channel;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.authentication.impl.channel.AuthenticationChannelImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

/**
 * @author skublik
 */
@Component
public class DefaultChannelFactory extends AbstractChannelFactory {
    @Override
    public boolean match(String channelId) {
        return true;
    }

    @Override
    public AuthenticationChannel createAuthChannel(AuthenticationSequenceChannelType channel) {
        if (channel != null) {
            return new AuthenticationChannelImpl(channel);
        }
        return new AuthenticationChannelImpl();
    }

    @Override
    protected Integer getOrder() {
        return Integer.MAX_VALUE;
    }
}
