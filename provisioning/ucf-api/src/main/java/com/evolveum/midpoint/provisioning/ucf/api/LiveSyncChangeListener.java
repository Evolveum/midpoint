/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.ucf.api.async.AsyncChangeListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * <p>Processes live sync changes detected on a resource.</p>
 *
 * <p>For each sync delta fetched from a resource, either {@link #onChange(Change, OperationResult)}
 * or {@link #onChangePreparationError(PrismProperty, Change, Throwable, OperationResult)} is called.
 * This is crucial for determination of last token processed.</p>
 *
 * <p>NOTE: This interface is similar to ChangeListener but with some differences:
 *  (1) semantics of {@link #onChange(Change, OperationResult)} return value,
 *  (2) the presence of OperationResult parameter,
 *  (3) {@link #onChangePreparationError(PrismProperty, Change, Throwable, OperationResult)} method.</p>
 *
 * <p>Is this class eligible to be merged with {@link AsyncChangeListener}? Not yet.</p>
 *
 * <p>TERMINOLOGY: Actually, it is not clear whether we should call this class (and its async version)
 * a handler or a listener. We use 'result handler' quite consistently in midPoint. On the other hand,
 * for async changes the use of 'listener' word is more appropriate. Also, the
 * {@link com.evolveum.midpoint.provisioning.impl.sync.ProcessChangeRequest} class has listener-like
 * methods (onXXX) so we opted for listener-style naming, at least for now.</p>
 */
@SuppressWarnings("JavadocReference")
public interface LiveSyncChangeListener {

    /**
     * Called when given change has to be processed.
     *
     * @param change The change.
     * @return false if the processing of changes has to be stopped
     */
    boolean onChange(Change change, OperationResult result);

    /**
     * Called when given change cannot be prepared for processing (or created altogether).
     *
     * @param token The token, if determinable.
     * @param change The change, if determinable.
     * @param exception Exception encountered.
     * @param result Context of the operation.
     * @return false if the processing of changes has to be stopped (this is the usual case)
     */
    boolean onChangePreparationError(@Nullable PrismProperty<?> token, @Nullable Change change, @NotNull Throwable exception,
            @NotNull OperationResult result);

    /**
     * Called when all changes from the resource were fetched. This is meant to let the caller know that
     * it can update the token in the task (if token values are not known to be "precise".)
     *
     * Note that finalToken value might or might not be present; depending on the connector implementation.
     */
    void onAllChangesFetched(PrismProperty<?> finalToken, OperationResult result);
}
