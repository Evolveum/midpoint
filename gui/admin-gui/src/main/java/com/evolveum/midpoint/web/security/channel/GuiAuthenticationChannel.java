/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.channel;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceChannelType;

/**
 * @author skublik
 */

public class GuiAuthenticationChannel extends AuthenticationChannelImpl {

    private TaskManager taskManager;
    private ModelInteractionService modelInteractionService;

    public GuiAuthenticationChannel(AuthenticationSequenceChannelType channel, TaskManager taskManager, ModelInteractionService modelInteractionService) {
        super(channel);
        this.taskManager = taskManager;
        this.modelInteractionService = modelInteractionService;
    }

    public String getChannelId() {
        return SchemaConstants.CHANNEL_USER_URI;
    }

    public String getPathAfterSuccessfulAuthentication() {
        if (WebModelServiceUtils.isPostAuthenticationEnabled(taskManager, modelInteractionService)) {
                return "/self/postAuthentication";
        }

        return super.getPathAfterSuccessfulAuthentication();
    }

}
