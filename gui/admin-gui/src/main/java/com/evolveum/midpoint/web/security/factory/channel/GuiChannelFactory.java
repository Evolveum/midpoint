/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.factory.channel;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.authentication.AuthenticationChannel;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.security.channel.AuthenticationChannelImpl;
import com.evolveum.midpoint.web.security.channel.GuiAuthenticationChannel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    public AuthenticationChannel createAuthChannel(AuthenticationSequenceChannelType channel) throws Exception {
        return new GuiAuthenticationChannel(channel, taskManager, modelInteractionService);
    }

    @Override
    protected Integer getOrder() {
        return 10;
    }
}
