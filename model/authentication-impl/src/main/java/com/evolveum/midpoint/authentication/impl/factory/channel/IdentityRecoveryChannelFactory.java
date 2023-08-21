/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.factory.channel;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;
import com.evolveum.midpoint.authentication.impl.channel.IdentityRecoveryAuthenticationChannel;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class IdentityRecoveryChannelFactory extends AbstractChannelFactory {

    @Autowired TaskManager taskManager;
    @Autowired RepositoryService repositoryService;

    @Override
    public boolean match(String channelId) {
        return SchemaConstants.CHANNEL_IDENTITY_RECOVERY_URI.equals(channelId);
    }

    @Override
    public AuthenticationChannel createAuthChannel(AuthenticationSequenceChannelType channel) {
        return new IdentityRecoveryAuthenticationChannel(channel, taskManager, repositoryService);
    }

    @Override
    protected Integer getOrder() {
        return 10;
    }
}
