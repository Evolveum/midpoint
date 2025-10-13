/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

public interface TaskManagerAware {

    void setTaskManager(TaskManager taskManager);

    TaskManager getTaskManager();

}
