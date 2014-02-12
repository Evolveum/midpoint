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

package com.evolveum.midpoint.task.api;

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
    void onTaskStart(Task task);

    /**
     * Called when a task execution routine (i.e. task handler) finishes.
     * Please note that execution-related task attributes, like task's
     * operation result, last task run finish timestamp, are NOT updated
     * when this routine is called. These values have to be got from
     * runResult parameter.
     *
     * @param task task that was just finished
     * @param runResult result of the task's run
     */
    void onTaskFinish(Task task, TaskRunResult runResult);

    /**
     * Called when a task's execution thread is started.
     *
     * @param task task whose thread was started
     * @param isRecovering true if the task was recovering from previous nodefailure
     */
    void onTaskThreadStart(Task task, boolean isRecovering);

    /**
     * Called when task's execution thread is finishing
     *
     * @param task task whose thread is finishing
     */
    void onTaskThreadFinish(Task task);
}
