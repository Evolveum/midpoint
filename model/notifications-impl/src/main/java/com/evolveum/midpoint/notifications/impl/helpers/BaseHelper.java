/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.notifications.impl.helpers;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public abstract class BaseHelper {

    protected void logStart(Trace LOGGER, Event event, EventHandlerType eventHandlerType) {
        logStart(LOGGER, event, eventHandlerType, null);
    }

    protected void logStart(Trace LOGGER, Event event, EventHandlerType eventHandlerType, Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event " + event + " with handler " +
                    eventHandlerType.getClass() + " (name: " + eventHandlerType.getName() +
                    (additionalData != null ? (", parameters: " + additionalData) :
                                             (", configuration: " + eventHandlerType)) +
                    ")");
        }
    }

    public void logEnd(Trace LOGGER, Event event, EventHandlerType eventHandlerType, boolean result) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finishing processing event " + event + " result = " + result);
        }
    }

}
