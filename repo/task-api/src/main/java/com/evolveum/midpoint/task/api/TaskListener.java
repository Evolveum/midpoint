/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * Notifies external observers about task-related events.
 *
 * These methods are executed from within the task execution thread,
 * so they have to finish as quickly as possible.
 *
 * For the difference between task start/finish and task thread
 * start/finish, please see
 * https://wiki.evolveum.com/display/midPoint/Task+Manager#TaskManager-TaskExecution-aBitofTerminology.
 *
 * @author mederly
 */
public interface TaskListener {

    /**
     * Called when a task execution routine (i.e. task handler) starts.
     * Task handler URI can be determined via task.getHandlerUri() method.
     *
     * @param task task that is about to be started
     */
    void onTaskStart(Task task, OperationResult result);

    /**
     * Called when a task execution routine (i.e. task handler) finishes.
     * Please note that execution-related task attributes, like task's
     * operation result, last task run finish timestamp, are NOT updated
     * when this routine is called. These values have to be got from
     * runResult parameter.
     * @param task task that was just finished
     * @param runResult result of the task's run
     */
    void onTaskFinish(Task task, TaskRunResult runResult, OperationResult result);

    /**
     * Called when a task's execution thread is started.
     * @param task task whose thread was started
     * @param isRecovering true if the task was recovering from previous nodefailure
     */
    void onTaskThreadStart(Task task, boolean isRecovering, OperationResult result);

    /**
     * Called when task's execution thread is finishing
     *
     * @param task task whose thread is finishing
     */
    void onTaskThreadFinish(Task task, OperationResult result);
}
