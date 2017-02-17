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

package com.evolveum.midpoint.notifications.api;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;

/**
 * @author mederly
 */

public interface NotificationManager {

    void registerTransport(String name, Transport transport);
    Transport getTransport(String name);

    // event may be null
    void processEvent(Event event);

    // event may be null
    void processEvent(Event event, Task task, OperationResult result);


    boolean processEvent(Event event, EventHandlerType eventHandlerType, Task task, OperationResult result);

    boolean isDisabled();
    void setDisabled(boolean disabled);
}
