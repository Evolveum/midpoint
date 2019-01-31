/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.notifications.impl.handlers;

import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.notifications.impl.helpers.NotificationExpressionHelper;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.notifications.api.EventHandler;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.NotificationManagerImpl;
import com.evolveum.midpoint.notifications.impl.NotificationFunctionsImpl;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationMessageAttachmentType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author mederly
 */
@Component
public abstract class BaseHandler implements EventHandler {

    private static final Trace LOGGER = TraceManager.getTrace(BaseHandler.class);

    @Autowired protected NotificationManagerImpl notificationManager;
	@Autowired protected NotificationFunctionsImpl notificationsUtil;
	@Autowired protected PrismContext prismContext;
	@Autowired protected ExpressionFactory expressionFactory;
	@Autowired protected TextFormatter textFormatter;
	@Autowired protected NotificationExpressionHelper expressionHelper;

	protected void register(Class<? extends EventHandlerType> clazz) {
        notificationManager.registerEventHandler(clazz, this);
    }

    protected void logStart(Trace LOGGER, Event event, EventHandlerType eventHandlerType) {
        logStart(LOGGER, event, eventHandlerType, null);
    }

    protected void logStart(Trace LOGGER, Event event, EventHandlerType eventHandlerType, Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event " + event.shortDump() + " with handler " +
            		getHumanReadableHandlerDescription(eventHandlerType) + "\n  parameters: " +
            		(additionalData != null ? ("\n  parameters: " + additionalData) :
                        ("\n  configuration: " + eventHandlerType)));

        }
    }

    protected void logNotApplicable(Event event, String reason) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(
				"{} is not applicable for event {}, continuing in the handler chain; reason: {}",
				this.getClass().getSimpleName(), event.shortDump(), reason);
		}
	}

    protected String getHumanReadableHandlerDescription(EventHandlerType eventHandlerType) {
    	if (eventHandlerType.getName() != null) {
    		return eventHandlerType.getName();
    	} else {
    		return eventHandlerType.getClass().getSimpleName();
    	}
	}

	public static void staticLogStart(Trace LOGGER, Event event, String description, Object additionalData) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Starting processing event " + event + " with handler " +
                    description +
                    (additionalData != null ? (", parameters: " + additionalData) : ""));
        }
    }

    public void logEnd(Trace LOGGER, Event event, EventHandlerType eventHandlerType, boolean result) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Finishing processing event " + event + " result = " + result);
        }
    }

	protected List<String> evaluateExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables,
			String shortDesc, Task task, OperationResult result) {
		return expressionHelper.evaluateExpressionChecked(expressionType, expressionVariables, shortDesc, task, result);
	}
	
	protected List<NotificationMessageAttachmentType> evaluateNotificationMessageAttachmentTypeExpressionChecked(ExpressionType expressionType, ExpressionVariables expressionVariables,
			String shortDesc, Task task, OperationResult result) {
		return expressionHelper.evaluateNotificationMessageAttachmentTypeExpressionChecked(expressionType, expressionVariables, shortDesc, task, result);
	}

	protected ExpressionVariables getDefaultVariables(Event event, OperationResult result) {
		return expressionHelper.getDefaultVariables(event, result);
    }
}
