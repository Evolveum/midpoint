/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.channel;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;

/**
 * @author skublik
 */

public class GuiAuthenticationChannel extends AuthenticationChannelImpl {

    private final TaskManager taskManager;
    private final ModelInteractionService modelInteractionService;

    public GuiAuthenticationChannel(AuthenticationSequenceChannelType channel, TaskManager taskManager, ModelInteractionService modelInteractionService) {
        super(channel);
        this.taskManager = taskManager;
        this.modelInteractionService = modelInteractionService;
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_USER_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        if (isPostAuthenticationEnabled()) {
                return "/self/postAuthentication";
        }

        return super.getPathAfterSuccessfulAuthentication();
    }

    @Override
    public boolean isPostAuthenticationEnabled() {
        return AuthUtil.isPostAuthenticationEnabled(taskManager, modelInteractionService);
    }

    @Override
    public Authorization getAdditionalAuthority() {
        if (isPostAuthenticationEnabled()) {
            return new Authorization(new AuthorizationType().action(AuthorizationConstants.AUTZ_UI_SELF_POST_AUTHENTICATION_URL));
        }
        return super.getAdditionalAuthority();
    }
}
