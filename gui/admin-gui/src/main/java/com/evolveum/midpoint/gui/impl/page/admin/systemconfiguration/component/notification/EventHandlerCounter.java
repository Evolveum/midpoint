/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.notification;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.web.application.SimpleCounter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class EventHandlerCounter extends SimpleCounter<AssignmentHolderDetailsModel<SystemConfigurationType>, SystemConfigurationType> {

    public EventHandlerCounter() {
        super();
    }

    @Override
    public int count(AssignmentHolderDetailsModel<SystemConfigurationType> model, PageBase pageBase) {
        SystemConfigurationType object = model.getObjectType();
        NotificationConfigurationType notification = object.getNotificationConfiguration();

        return notification != null ? notification.getHandler().size() : 0;
    }
}

