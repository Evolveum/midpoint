/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.factory.channel;

import com.evolveum.midpoint.authentication.api.AuthenticationChannel;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.authentication.impl.channel.GuiAuthenticationChannel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

/**
 * @author skublik
 */
@Component
public class GuiChannelFactory extends AbstractChannelFactory {

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private ModelInteractionService modelInteractionService;

    @Override
    public boolean match(String channelId) {
        return SchemaConstants.CHANNEL_USER_URI.equals(channelId);
    }

    @Override
    public AuthenticationChannel createAuthChannel(AuthenticationSequenceChannelType channel) {
        return new GuiAuthenticationChannel(channel, taskManager, modelInteractionService);
    }

    @Override
    protected Integer getOrder() {
        return 10;
    }
}
