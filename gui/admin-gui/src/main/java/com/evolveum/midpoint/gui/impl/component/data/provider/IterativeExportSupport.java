/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.provider;

import com.evolveum.midpoint.schema.ObjectHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Interface for DataProviders that support iterative export.
 * This allows streaming export without loading all data into memory.
 *
 * @param <T> The type of items being exported
 */
public interface IterativeExportSupport<T> {

    /**
     * Execute iterative search and pass each item to the handler.
     * The search will stop if the handler returns false.
     *
     * @param handler Handler to process each item. Returns true to continue, false to stop.
     * @param task Task for the operation
     * @param result Operation result
     * @throws CommonException if an error occurs during the search
     */
    void exportIterative(ObjectHandler<T> handler, Task task, OperationResult result) throws CommonException;

    /**
     * Returns true if this provider actually supports iterative export.
     * Default is true, but BaseSearchDataProvider overrides to return false
     * so that subclasses must explicitly enable support.
     */
    default boolean supportsIterativeExport() {
        return true;
    }
}
