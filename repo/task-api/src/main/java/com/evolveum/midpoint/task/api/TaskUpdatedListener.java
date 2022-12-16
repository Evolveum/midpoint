/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.api;

import com.evolveum.midpoint.schema.result.OperationResult;

import com.google.common.annotations.VisibleForTesting;

@VisibleForTesting
public interface TaskUpdatedListener {

    /**
     * Called when a task is updated.
     *
     * BEWARE, not all task updates (not even all task status updates) are acted upon by this method.
     * Hence, please use this method only in tests (for now).
     */
    void onTaskUpdated(Task task, OperationResult result);

}
