package com.evolveum.midpoint.notifications.handlers;

import com.evolveum.midpoint.notifications.NotificationManager;
import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventHandlerType;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
public interface EventHandler {

    // true if we should continue with processing, false otherwise
    boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager, OperationResult result) throws SchemaException;
}
