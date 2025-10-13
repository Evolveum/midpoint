/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
