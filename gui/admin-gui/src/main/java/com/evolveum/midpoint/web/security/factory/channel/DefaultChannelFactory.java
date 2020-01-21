/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.factory.channel;

import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.web.security.channel.AuthenticationChannelImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

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
    public AuthenticationChannel createAuthChannel(AuthenticationSequenceChannelType channel) throws Exception {
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
