/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.impl.shadows.sync.NotApplicableException;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;

/**
 * Implements primitive "life cycle" of an object with deferred initialization.
 *
 * Such an object has {@link #initialize(Task, OperationResult)} method, which can be invoked right after
 * object creation but - if needed - also later, e.g. from a worker thread.
 *
 * Another feature of such item is that it has a {@link InitializationState} that:
 *
 * 1. informs about object initialization-related life cycle: created, initializing, initialized, failed;
 * 2. informs about the error status before/after initialization: ok, error (plus exception), not applicable.
 *
 * TODO where to put statistics related e.g. to the processing time?
 */
@Experimental
public interface InitializableMixin extends DebugDumpable {

    /**
     * Initializes given object.
     *
     * - Precondition: lifecycle state is CREATED.
     * - Postcondition (unless exception is thrown): lifecycle state is INITIALIZED or INITIALIZATION_FAILED.
     */
    default void initialize(Task task, OperationResult result) {

        InitializationState state = getInitializationState();

        getLogger().trace("Item before initialization (state: {}):\n{}", state, debugDumpLazily());

        try {
            state.moveFromCreatedToInitializing();

            try {
                initializeInternal(task, result);
            } catch (NotApplicableException e) {
                state.recordNotApplicable();
                result.recordNotApplicable();
            }
            state.moveFromInitializingToInitialized();

            checkConsistence();

        } catch (CommonException | EncryptionException | RuntimeException e) {
            getLogger().warn("Got an exception during initialization of {}", this, e); // TODO change to debug
            getInitializationState().recordInitializationFailed(e);
            result.recordFatalError(e);
        }

        state.checkAfterInitialization();
        getLogger().trace("Item after initialization (state: {}):\n{}", state, debugDumpLazily());
    }

    void initializeInternal(Task task, OperationResult result) throws CommonException, NotApplicableException, EncryptionException;

    Trace getLogger();

    InitializationState getInitializationState();

    /**
     * Checks the consistence, taking into account the lifecycle and error state of the object.
     * Called from the inside. But can be called also from outside clients.
     */
    void checkConsistence() throws SchemaException;
}
