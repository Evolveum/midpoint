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

public interface EventHandler<E extends Event, C extends EventHandlerType> {

    /**
     * @return true if we should continue with processing, false otherwise
     */
    boolean processEvent(E event, C eventHandlerType, Task task, OperationResult result) throws SchemaException;

    /**
     * @return Type of events this handler is capable of handling.
     */
    Class<E> getEventType();

    /**
     * @return Type of configuration objects for this event handler. The handler is selected based on exact match of this type.
     */
    Class<C> getEventHandlerConfigurationType();
}
