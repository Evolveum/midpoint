/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import com.evolveum.midpoint.provisioning.ucf.api.UcfChange;
import com.evolveum.midpoint.provisioning.ucf.api.UcfErrorState;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.provisioning.util.ErrorState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents the status of an object that was already initialized by other (external) means.
 *
 * For example, {@link UcfResourceObject} and {@link UcfChange} are not of type {@link LazilyInitializableMixin}, yet they have
 * some initialization that results in an {@link ErrorState}. They can be represented
 * as {@link AlreadyInitializedObject} instances.
 *
 * Note that "initializeInternal..." methods are no-ops. This is because there is nothing to do here; the underlying object
 * was already initialized, e.g., by UCF.
 */
public class AlreadyInitializedObject implements LazilyInitializableMixin {

    private static final Trace LOGGER = TraceManager.getTrace(AlreadyInitializedObject.class);

    /** Always "initialized". */
    @NotNull private final InitializationState initializationState;

    private AlreadyInitializedObject(@NotNull ErrorState errorState) {
        this.initializationState = InitializationState.initialized(errorState);
    }

    public static AlreadyInitializedObject of(ErrorState errorState) {
        return new AlreadyInitializedObject(errorState);
    }

    public static AlreadyInitializedObject fromUcfErrorState(@NotNull UcfErrorState errorState) {
        return of(
                ErrorState.fromUcfErrorState(errorState));
    }

    @Override
    public void initializeInternalForPrerequisiteOk(Task task, OperationResult result) {
        // no-op
    }

    @Override
    public @Nullable LazilyInitializableMixin getPrerequisite() {
        return null;
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    @Override
    public @NotNull InitializationState getInitializationState() {
        return initializationState;
    }

    @Override
    public void checkConsistence() throws SchemaException {
        // no-op
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(this.getClass(), indent);
        DebugUtil.debugDumpWithLabelToString(sb, "state", initializationState, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        // Currently there is no other relevant information. Please update if there is any.
        return initializationState.getErrorState().toString();
    }
}
