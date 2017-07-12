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

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.TaskEvent;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * One of interfaces of the notifier to midPoint.
 *
 * Used to catch task-related events.
 *
 * @author mederly
 */
@Component
public class NotificationTaskListener implements TaskListener {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationTaskListener.class);
	private static final String OPERATION_PROCESS_EVENT = NotificationTaskListener.class.getName() + "." + "processEvent";

	@Autowired
    private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    @Autowired
    private NotificationManager notificationManager;

    @Autowired
    private NotificationFunctionsImpl notificationsUtil;

	@Autowired
	private TaskManager taskManager;

    @PostConstruct
    public void init() {
        taskManager.registerTaskListener(this);
        LOGGER.trace("Task listener registered.");
    }

	@Override
	public void onTaskStart(Task task) {
		createAndProcessEvent(task, null, EventOperationType.ADD);
	}

	@Override
	public void onTaskFinish(Task task, TaskRunResult runResult) {
		createAndProcessEvent(task, runResult, EventOperationType.DELETE);
	}

	private void createAndProcessEvent(Task task, TaskRunResult runResult, EventOperationType operationType) {
		TaskEvent event = new TaskEvent(lightweightIdentifierGenerator, task, runResult, operationType, task.getChannel());

		if (task.getOwner() != null) {
			event.setRequester(new SimpleObjectRefImpl(notificationsUtil, task.getOwner().asObjectable()));
			event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, task.getOwner().asObjectable()));
		} else {
			LOGGER.debug("No owner for task " + task + ", therefore no requester and requestee will be set for event " + event.getId());
		}

		Task opTask = taskManager.createTaskInstance(OPERATION_PROCESS_EVENT);
		notificationManager.processEvent(event, opTask, opTask.getResult());
	}

	@Override
	public void onTaskThreadStart(Task task, boolean isRecovering) {
		// not implemented
	}

	@Override
	public void onTaskThreadFinish(Task task) {
		// not implemented
	}
}
