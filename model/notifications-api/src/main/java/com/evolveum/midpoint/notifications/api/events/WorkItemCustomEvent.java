/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemNotificationActionType;

/**
 *
 */
public interface WorkItemCustomEvent {
    WorkItemNotificationActionType getNotificationAction();
}
