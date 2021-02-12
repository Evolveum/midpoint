/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.provisioning.ucf.api.async.UcfAsyncUpdateChangeListener;
import com.evolveum.midpoint.schema.result.OperationResult;

/**
 * TODO REMOVE THIS CLASS
 *
 *
 * Processes live sync changes detected on a resource.
 *
 * For each sync delta fetched from a resource, either {@link #onChange(Change, OperationResult)}
 * or {@link #onChangePreparationError(PrismProperty, Change, Throwable, OperationResult)} is called.
 * This is crucial for determination of last token processed.
 *
 * NOTE: This interface is similar to ChangeListener but with some differences:
 *  1. semantics of {@link #onChange(Change, OperationResult)} return value,
 *  2. the presence of OperationResult parameter,
 *  3. {@link #onChangePreparationError(PrismProperty, Change, Throwable, OperationResult)} method.</p>
 *
 * <p>Is this class eligible to be merged with {@link UcfAsyncUpdateChangeListener}? Not yet.</p>
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
    boolean onChange(Object change, OperationResult result);

}
