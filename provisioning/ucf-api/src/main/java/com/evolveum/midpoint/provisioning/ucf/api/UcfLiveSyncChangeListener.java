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
 * Processes live sync changes detected on a resource.
 *
 * For each sync delta fetched from a resource, either {@link #onChange(Change, OperationResult)}
 * is called. Even in error situations.
 *
 * TERMINOLOGY: Actually, it is not clear whether we should call this class (and its async version)
 * a handler or a listener. We use 'result handler' quite consistently in midPoint. On the other hand,
 * for async changes the use of 'listener' word is more appropriate. We have chosen listener-style naming,
 * at least for now.
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
}
