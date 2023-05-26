/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.impl.events.TaskEventImpl;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;

/**
 * One of interfaces of the notifier to midPoint.
 *
 * Used to catch task-related events.
 */
@Component
public class NotificationTaskListener implements TaskListener {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationTaskListener.class);
    private static final String OPERATION_PROCESS_EVENT = NotificationTaskListener.class.getName() + "." + "processEvent";

    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired private NotificationManager notificationManager;
    @Autowired private TaskManager taskManager;

    @PostConstruct
    public void init() {
        taskManager.registerTaskListener(this);
        LOGGER.trace("Task listener registered.");
    }

    @Override
    public void onTaskStart(Task task, OperationResult result) {
        createAndProcessEvent(task, null, EventOperationType.ADD, result);
    }

    @Override
    public void onTaskFinish(Task task, TaskRunResult runResult, OperationResult result) {
        createAndProcessEvent(task, runResult, EventOperationType.DELETE, result);
    }

    private void createAndProcessEvent(Task task, TaskRunResult runResult, EventOperationType operationType, OperationResult result) {
        TaskEventImpl event = new TaskEventImpl(lightweightIdentifierGenerator, task, runResult, operationType, task.getChannel());
        event.setRequesterAndRequesteeAsTaskOwner(task, result);
        // TODO why not using the same task?
        Task opTask = taskManager.createTaskInstance(OPERATION_PROCESS_EVENT);
        notificationManager.processEvent(event, opTask, result);
    }

    @Override
    public void onTaskThreadStart(Task task, boolean isRecovering, OperationResult result) {
        // not implemented
    }

    @Override
    public void onTaskThreadFinish(Task task, OperationResult result) {
        // not implemented
    }
}
