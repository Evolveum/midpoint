/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.provisioning.impl.LazilyInitializableMixin;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.InitializationState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.NotHereAssertionError;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Auxiliary class that ensures proper resource-objects-level processing for "raw" objects obtained from UCF or other sources.
 *
 * @see ResourceObjectChange
 * @see ResourceObjectFound
 * @see CompleteResourceObject
 */
@Experimental
public class ResourceObjectFetched extends AbstractLazilyInitializableResourceEntity {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectFetched.class);

    @NotNull private final ExistingResourceObject resourceObject;

    /** State of the initialization of this object. */
    @NotNull private final InitializationState initializationState = InitializationState.created();

    /** Should the associations be fetched during initialization? Probably will be replaced by "retrieve" options. */
    private final boolean fetchAssociations;

    /** Useful beans from Resource Objects layer. */
    private final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    ResourceObjectFetched(
            @NotNull ExistingResourceObject resourceObject,
            @NotNull ProvisioningContext ctx,
            boolean fetchAssociations) {
        super(ctx);
        this.resourceObject = resourceObject;
        this.fetchAssociations = fetchAssociations;
    }

    @Override
    public @Nullable LazilyInitializableMixin getPrerequisite() {
        return null;
    }

    @Override
    public void initializeInternalForPrerequisiteOk(Task task, OperationResult result) throws CommonException {
        completeResourceObject(globalCtx, resourceObject, fetchAssociations, result);
    }

    @Override
    public void initializeInternalForPrerequisiteError(Task task, OperationResult result) throws CommonException {
        throw new NotHereAssertionError();
    }

    @Override
    public void initializeInternalForPrerequisiteNotApplicable(Task task, OperationResult result) {
        throw new NotHereAssertionError();
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    @NotNull CompleteResourceObject asCompleteResourceObject() {
        checkInitialized();
        return CompleteResourceObject.of(resourceObject, initializationState.getErrorState());
    }

    @Override
    public void checkConsistence() {
        // nothing here now
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObject=" + resourceObject +
                ", initializationState=" + initializationState +
                '}';
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", resourceObject, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        return sb.toString();
    }
}
