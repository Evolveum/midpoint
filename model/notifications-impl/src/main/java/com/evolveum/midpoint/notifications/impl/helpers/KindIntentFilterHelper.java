/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.springframework.stereotype.Component;

/**
 * @author mederly
 */
@Component
public class KindIntentFilterHelper extends BaseHelper {

    private static final Trace LOGGER = TraceManager.getTrace(KindIntentFilterHelper.class);

    @Override
    public boolean processEvent(Event event, EventHandlerType eventHandlerType, NotificationManager notificationManager,
    		Task task, OperationResult result) {

        if (eventHandlerType.getObjectKind().isEmpty() && eventHandlerType.getObjectIntent().isEmpty()) {
            return true;
        }

        if (!(event instanceof ResourceObjectEvent)) {
            return true;            // or should we return false?
        }
        ResourceObjectEvent resourceObjectEvent = (ResourceObjectEvent) event;

        logStart(LOGGER, event, eventHandlerType, eventHandlerType.getStatus());

        boolean retval = true;

        if (!eventHandlerType.getObjectKind().isEmpty()) {
            retval = false;
            for (ShadowKindType shadowKindType : eventHandlerType.getObjectKind()) {
                if (shadowKindType == null) {
                    LOGGER.warn("Filtering on null shadowKindType; filter = " + eventHandlerType);
                } else if (resourceObjectEvent.isShadowKind(shadowKindType)) {
                    retval = true;
                    break;
                }
            }
        }

        if (retval) {
            // now check the intent
            if (!eventHandlerType.getObjectIntent().isEmpty()) {
                retval = false;
                for (String intent : eventHandlerType.getObjectIntent()) {
                    if (intent == null) {
                        LOGGER.warn("Filtering on null intent; filter = " + eventHandlerType);
                    } else if (resourceObjectEvent.isShadowIntent(intent)) {
                        retval = true;
                        break;
                    }
                }
            }
        }

        logEnd(LOGGER, event, eventHandlerType, retval);
        return retval;
    }
}
