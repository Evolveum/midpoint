/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications;

import com.evolveum.midpoint.notifications.events.Event;
import com.evolveum.midpoint.notifications.transports.Transport;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.EventHandlerType;

/**
 * @author mederly
 */

public interface NotificationManager {

    void registerEventHandler(Class<? extends EventHandlerType> clazz, EventHandler handler);
    EventHandler getEventHandler(EventHandlerType eventHandlerType);
    void registerTransport(String name, Transport transport);
    Transport getTransport(String name);

    // event may be null
    void processEvent(Event event);

    // event may be null
    void processEvent(Event event, OperationResult result);


    boolean processEvent(Event event, EventHandlerType eventHandlerType, OperationResult result);

    boolean isDisabled();
    void setDisabled(boolean disabled);
}
