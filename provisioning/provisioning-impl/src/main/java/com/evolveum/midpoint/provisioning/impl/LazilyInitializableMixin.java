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
import com.evolveum.midpoint.provisioning.util.InitializationState.LifecycleState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.Checkable;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Implements support for entities with lazy (deferred) initialization.
 *
 * Such an entity has {@link #initialize(Task, OperationResult)} method, which can be invoked right after
 * its creation but - if needed - also later, e.g. from a worker thread independent from the coordinator
 * or connector-provided thread that processed the ConnId/UCF object found in search or sync operation
 * and created the Java object for the entity.
 *
 * Another feature of such entity is that it has a {@link InitializationState} that:
 *
 * 1. informs about object initialization-related life cycle: created, initializing, initialized;
 * 2. informs about the error status before/after initialization: ok, error (plus exception), not applicable.
 *
 * TODO where to put statistics related e.g. to the processing time?
 */
@Experimental
public interface LazilyInitializableMixin extends DebugDumpable, Checkable {

    /**
     * Initializes given object.
     *
     * - Precondition: lifecycle state is {@link LifecycleState#CREATED}.
     * - Postcondition (unless exception is thrown): lifecycle state is {@link LifecycleState#INITIALIZED}.
     */
    default void initialize(Task task, OperationResult result) {

        InitializationState initializationState = getInitializationState();
        if (initializationState.isInitialized()) {
            return;
        }

        initializePrerequisite(task, result);

        getLogger().trace("OWN INITIALIZATION STARTING. State before:\n{}", debugDumpLazily());

        try {
            initializationState.moveFromCreatedToInitializing();

            try {
                initializeInternal(task, result);
            } catch (NotApplicableException e) {
                initializationState.recordNotApplicable();
                result.setNotApplicable();
            }
            initializationState.moveFromInitializingToInitialized();

            checkConsistence();

        } catch (CommonException | RuntimeException e) {
            getLogger().debug("Got an exception during initialization of {}", this, e);
            initializationState.recordInitializationFailed(e);
            result.recordException(e);
        }

        initializationState.checkInitialized();
        getLogger().trace("INITIALIZATION FINISHED. State after:\n{}", debugDumpLazily());
    }

    private void initializePrerequisite(Task task, OperationResult result) {
        LazilyInitializableMixin prerequisite = getPrerequisite();
        if (prerequisite != null) {
            prerequisite.initialize(task, result); // no-op if already initialized
        }
    }

    /** The object can have a prerequisite that must be initialized before it. */
    default @Nullable LazilyInitializableMixin getPrerequisite() {
        return null;
    }

    /**
     * Initializes this object. Assumes the prerequisite is already initialized (successfully or not).
     *
     * If needed, the whole logic can be replaced - on your risk! Do not forget to propagate errors from the prerequisite.
     * A typical reason is when the whole initialization logic is so simple that it fits into a single method.
     */
    default void initializeInternal(Task task, OperationResult result)
            throws CommonException, NotApplicableException, EncryptionException {
        initializeInternalCommon(task, result);
        LazilyInitializableMixin prerequisite = getPrerequisite();
        if (prerequisite == null || prerequisite.isOk()) {
            initializeInternalForPrerequisiteOk(task, result);
        } else if (prerequisite.isError()) {
            getInitializationState().recordError(prerequisite.getExceptionEncountered());
            initializeInternalForPrerequisiteError(task, result);
        } else {
            assert prerequisite.isNotApplicable();
            initializeInternalForPrerequisiteNotApplicable(task, result);
        }
    }

    default void initializeInternalCommon(Task task, OperationResult result)
            throws SchemaException, ConfigurationException, EncryptionException {
        // to be overridden in the implementations
    }

    default void initializeInternalForPrerequisiteOk(Task task, OperationResult result)
            throws CommonException, NotApplicableException, EncryptionException {
        // to be overridden in the implementations
    }

    default void initializeInternalForPrerequisiteError(Task task, OperationResult result)
            throws CommonException, EncryptionException {
        // to be overridden in the implementations
    }

    default void initializeInternalForPrerequisiteNotApplicable(Task task, OperationResult result)
            throws CommonException, EncryptionException {
        // to be overridden in the implementations
    }

    Trace getLogger();

    @NotNull InitializationState getInitializationState();

    default boolean isOk() {
        return getInitializationState().isOk();
    }

    default boolean isError() {
        return getInitializationState().isError();
    }

    default boolean isNotApplicable() {
        return getInitializationState().isNotApplicable();
    }

    default boolean isInitialized() {
        return getInitializationState().isInitialized();
    }

    default void checkInitialized() {
        getInitializationState().checkInitialized();
    }

    default Throwable getExceptionEncountered() {
        return getInitializationState().getExceptionEncountered();
    }

    /**
     * Checks the consistence, taking into account the lifecycle and error state of the object.
     * Called from the inside. But can be called also from outside clients.
     *
     * TODO consider changing the name to something like "validate", because the current name suggests it is skipped in production
     */
    void checkConsistence() throws SchemaException;
}
