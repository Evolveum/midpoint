/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
