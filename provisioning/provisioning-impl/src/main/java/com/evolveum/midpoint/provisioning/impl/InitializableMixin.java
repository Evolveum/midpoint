/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.SkipProcessingException;
import com.evolveum.midpoint.provisioning.util.ProcessingState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;

/**
 * Implements primitive "life cycle" of an initializable item.
 *
 * Such an item has deferrable {@link #initialize(Task, OperationResult)} method, which can be invoked right after
 * item creation but - if needed - also later, e.g. from a worker thread.
 *
 * Another feature of such item is that it has a {@link ProcessingState} that:
 *
 * 1. tells if the item was (successfully) initialized or not,
 * 2. ...
 *
 * TODO TODO TODO Re-think the whole lifecycle! It is not consistent today.
 */
@Experimental
public interface InitializableMixin extends DebugDumpable {

    default void initialize(Task task, OperationResult result) {

        getLogger().trace("Item before initialization:\n{}", debugDumpLazily());

        try {

            if (getProcessingState().isSkipFurtherProcessing()) {
                getLogger().trace("Skipping initialization because skipFurtherProcessing is true.");
                skipInitialization(task, result);
                getProcessingState().setInitializationSkipped();
            } else {
                initializeInternal(task, result);
                getProcessingState().setInitialized();
            }
            checkConsistence();

        } catch (Exception e) {
            processException(e, result);
        }

        getLogger().trace("Item after initialization (initialized: {}):\n{}", getProcessingState().isInitialized(),
                debugDumpLazily());
    }

    void initializeInternal(Task task, OperationResult result) throws CommonException, SkipProcessingException, EncryptionException;

    /**
     * If we need to do some processing even if initialization is skipped.
     * For example we might want to create a shadow even for malformed objects.
     */
    default void skipInitialization(Task task, OperationResult result)
            throws CommonException, SkipProcessingException, EncryptionException {
    }

    default void processException(Throwable t, OperationResult result) {
        getLogger().warn("Got an exception, skipping further processing in {}", this, t); // TODO change to debug
        Throwable cause = getProcessingState().recordException(t);
        recordIntoResult(result, cause);
    }

    private void recordIntoResult(OperationResult result, Throwable cause) {
        if (cause != null) {
            result.recordFatalError(cause);
        } else {
            result.recordNotApplicable();
        }
    }

    Trace getLogger();

    ProcessingState getProcessingState();

    /**
     * NOTE: this is called only after successful initialization or initialization-skip.
     *
     * TODO that is quite misleading. This method should provide reasonable results even if the initialization/init-skip
     *  processing failed!
     */
    void checkConsistence() throws SchemaException;
}
