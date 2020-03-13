/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.impl.events.TaskEventImpl;
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
        TaskEventImpl event = new TaskEventImpl(lightweightIdentifierGenerator, task, runResult, operationType, task.getChannel());

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
