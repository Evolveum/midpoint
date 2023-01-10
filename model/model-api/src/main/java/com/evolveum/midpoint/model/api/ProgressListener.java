/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.model.api.context.ModelContext;

/**
 * An interface that model uses to report operation progress to any interested party (e.g. GUI or WS client).
 * Useful for long-running operations, like provisioning a focus object with many projections.
 *
 * EXPERIMENTAL.
 */
public interface ProgressListener {

    /**
     * Reports a progress achieved. The idea is to provide as much information as practically possible,
     * so the client could take whatever it wants.
     *
     * Obviously, the method should not take too much time in order not to slow down the main execution routine.
     *
     * @param modelContext Current context of the model operation.
     * @param progressInformation Specific progress information.
     */
    void onProgressAchieved(ModelContext<?> modelContext, ProgressInformation progressInformation);

    boolean isAbortRequested();
}
