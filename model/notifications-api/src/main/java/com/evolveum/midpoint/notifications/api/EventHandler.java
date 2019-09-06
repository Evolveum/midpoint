/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;

/**
 * @author mederly
 */
@FunctionalInterface
public interface EventHandler {

    // true if we should continue with processing, false otherwise
    boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager,
    		Task task, OperationResult result) throws SchemaException;
}
