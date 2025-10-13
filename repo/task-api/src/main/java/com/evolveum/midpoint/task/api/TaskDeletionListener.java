/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;

@FunctionalInterface
public interface TaskDeletionListener {

    /**
     * Called when a given task is to be deleted.
     * Currently unused.
     */
    void onTaskDelete(Task task, OperationResult result);
}
