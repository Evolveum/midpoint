/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api.async;

import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

/**
 * Processes asynchronous changes encountered on a resource.
 */
public interface AsyncChangeListener {

    /**
     * Called when the connector learns about a resource change.
     * @param change The change.
     * @return true if the change was successfully processed and can be acknowledged on the resource;
     * false (or a runtime exception) should be returned otherwise
     */
    boolean onChange(Change change, Task task, OperationResult result);
}
