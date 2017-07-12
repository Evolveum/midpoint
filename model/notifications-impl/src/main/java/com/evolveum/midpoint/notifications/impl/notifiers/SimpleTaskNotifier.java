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

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.TaskEvent;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleTaskNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * @author mederly
 */
@Component
public class SimpleTaskNotifier extends GeneralNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleTaskNotifier.class);

    @PostConstruct
    public void init() {
        register(SimpleTaskNotifierType.class);
    }

    @Override
    protected boolean quickCheckApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        if (!(event instanceof TaskEvent)) {
            LOGGER.trace("{} is not applicable for this kind of event, continuing in the handler chain; event class = {}", getClass().getSimpleName(), event.getClass());
            return false;
        }
        return true;
    }

    @Override
    protected boolean checkApplicability(Event event, GeneralNotifierType generalNotifierType, OperationResult result) {
        return true;
    }

    @Override
    protected String getSubject(Event event, GeneralNotifierType generalNotifierType, String transport, Task task, OperationResult result) {
		final TaskEvent taskEvent = (TaskEvent) event;
		final String taskName = PolyString.getOrig(taskEvent.getTask().getName());

        if (event.isAdd()) {
            return "Task '" + taskName + "' start notification";
        } else if (event.isDelete()) {
            return "Task '" + taskName + "' finish notification: " + taskEvent.getOperationResultStatus();
        } else {
            return "(unknown " + taskName + " operation)";
        }
    }

    @Override
    protected String getBody(Event event, GeneralNotifierType generalNotifierType, String transport, Task opTask, OperationResult opResult) throws SchemaException {
		final TaskEvent taskEvent = (TaskEvent) event;
		final Task task = taskEvent.getTask();
		final String taskName = PolyString.getOrig(task.getName());

        StringBuilder body = new StringBuilder();

        body.append("Notification about task-related operation.\n\n");
		body.append("Task: ").append(taskName).append("\n");
		body.append("Handler: ").append(task.getHandlerUri()).append("\n\n");
		if (taskEvent.getTaskRunResult() != null) {
			body.append("Run result status: ").append(taskEvent.getTaskRunResult().getRunResultStatus()).append("\n");
		}
		body.append("Status: ").append(taskEvent.getOperationResultStatus()).append("\n");
		String message = taskEvent.getMessage();
		if (StringUtils.isNotBlank(message)) {
			body.append("Message: ").append(message).append("\n");
		}
		body.append("Progress: ").append(taskEvent.getProgress()).append("\n");
		body.append("\n");
        body.append("Notification created on: ").append(new Date()).append("\n\n");

		if (task.getOwner() != null) {
			UserType owner = task.getOwner().asObjectable();
			body.append("Task owner: ");
			body.append(owner.getFullName()).append(" (").append(owner.getName()).append(")");
			body.append("\n");
		}
		body.append("Channel: ").append(event.getChannel()).append("\n\n");

        return body.toString();
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }

}
