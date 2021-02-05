/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.provisioning.impl.sync.SkipProcessingException;
import com.evolveum.midpoint.provisioning.util.ProcessingState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;

/**
 * TODO better name?
 */
@Experimental
public interface InitializableMixin extends DebugDumpable {

    default void initialize(Task task, OperationResult result) {

        if (getProcessingState().isSkipFurtherProcessing()) {
            getLogger().debug("Skipping initialization because skipFurtherProcessing is true. Item:\n{}", debugDumpLazily());
            return;
        }

        getLogger().trace("Item before initialization:\n{}", debugDumpLazily());

        try {
            initializeInternal(task, result);
            getProcessingState().setInitialized();
        } catch (Exception e) {
            processException(e);
        }

        getLogger().trace("Change after initialization (initialized: {})\n:{}", getProcessingState().isInitialized(),
                debugDumpLazily());
    }

    void initializeInternal(Task task, OperationResult result) throws CommonException, SkipProcessingException;

    default void processException(Throwable t) {
        getLogger().warn("Got an exception, skipping further processing in {}", this, t); // TODO change to debug
        getProcessingState().recordException(t);
    }

    Trace getLogger();

    ProcessingState getProcessingState();
}
