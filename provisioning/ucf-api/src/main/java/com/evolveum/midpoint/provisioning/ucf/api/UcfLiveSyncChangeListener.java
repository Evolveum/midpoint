/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * <p>Processes live sync changes detected on a resource.</p>
 *
 * <p>For each sync delta fetched from a resource, either {@link #onChange(Change, OperationResult)}
 * or {@link #onError(int, Object, PrismProperty, Throwable, OperationResult)} is called.
 * This is crucial for determination of last token processed.</p>
 *
 * --- TODO TODO TODO
 * <p>NOTE: This interface is similar to ChangeListener but with some differences:
 *  (1) semantics of {@link #onChange(Change, OperationResult)} return value,
 *  (2) the presence of OperationResult parameter,
 *  (3) {@link #onChangePreparationError(PrismProperty, Change, Throwable, OperationResult)} method.</p>
 * --- TODO TODO TODO
 *
 * <p>TERMINOLOGY: Actually, it is not clear whether we should call this class (and its async version)
 * a handler or a listener. We use 'result handler' quite consistently in midPoint. On the other hand,
 * for async changes the use of 'listener' word is more appropriate. We have chosen listener-style naming,
 * at least for now.</p>
 */
@SuppressWarnings("JavadocReference")
public interface UcfLiveSyncChangeListener {

    /**
     * Called when given change was detected and should be processed.
     *
     * @param change The change.
     * @return false if the processing of changes has to be stopped
     */
    boolean onChange(UcfLiveSyncChange change, OperationResult result);

    /**
     * Called when something arrived from the source, but no change could be created because
     * of an error.
     *
     * Note that there could be situations when no change can be created, but the situation is not
     * considered to be an error. But let's ignore that for the moment.
     *
     * @return false if the processing of changes has to be stopped (this is the usual case)
     */
    boolean onError(int localSequentialNumber, @NotNull Object primaryIdentifierRealValue,
            @NotNull PrismProperty<?> token, @NotNull Throwable exception, @NotNull OperationResult result);
}
