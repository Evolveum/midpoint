/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.result.OperationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Processes changes detected on a resource.
 *
 * For each sync delta fetched from a resource, either handleChange or handleError is called.
 * This is crucial for determination of last token processed.
 *
 * Note
 * ====
 *
 * This interface is similar to ChangeListener but with some differences:
 *  - semantics of handleChange return value
 *  - the presence of handleChange OperationResult parameter
 *  - handleError method
 *
 * Is this class eligible to be merged with ChangeListener? Probably not.
 */
public interface ChangeHandler {

    /**
     * Called when given change has to be processed.
     *
     * @param change The change.
     * @return false if the processing of changes has to be stopped
     */
    boolean handleChange(Change change, OperationResult result);

    /**
     * Called when given change cannot be prepared for processing (or created altogether).
     *
     * @param token The token, if determinable.
     * @param change The change, if determinable.
     * @param exception Exception encountered.
     * @param result Context of the operation.
     * @return false if the processing of changes has to be stopped (this is the usual case)
     */
    boolean handleError(@Nullable PrismProperty<?> token, @Nullable Change change, @NotNull Throwable exception,
            @NotNull OperationResult result);

    /**
     * Called when all changes from the resource were fetched. This is meant to let the caller know that
     * it can update the token in the task (if token values are not known to be "precise".)
     *
     * Note that finalToken value might or might not be present; depending on the connector implementation.
     */
    void handleAllChangesFetched(PrismProperty<?> finalToken, OperationResult result);
}
