/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.UcfResourceObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Represents a *lazily-initializable* resource object (e.g. an account) found by the *search* or *fetch* operation.
 */
@Experimental
public class ResourceObjectFound extends AbstractLazilyInitializableResourceEntity {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectFound.class);

    /** The initial information that came from UCF. Used for some clients. */
    @NotNull private final UcfResourceObject initialUcfResourceObject;

    /**
     * The resource object, as provided by UCF, with the simple conversion only (no simulated associations, activation, etc).
     * Its content may be modified during processing here (no cloning is done).
     * Ultimately, it should be transformed into {@link #completeResourceObject}.
     */
    @NotNull private final ExistingResourceObjectShadow initialResourceObject;

    /** Result of the processing. */
    private CompleteResourceObject completeResourceObject;

    private ResourceObjectFound(
            @NotNull ProvisioningContext ctx,
            @NotNull UcfResourceObject initialUcfResourceObject,
            boolean fetchAssociations) {
        super(ctx, fetchAssociations);
        this.initialUcfResourceObject = initialUcfResourceObject;
        this.initialResourceObject = ExistingResourceObjectShadow.fromUcf(initialUcfResourceObject, ctx.getResourceRef());
    }

    static ResourceObjectFound fromUcf(
            @NotNull UcfResourceObject ucfResourceObject,
            @NotNull ProvisioningContext ctx,
            boolean fetchAssociations) {
        return new ResourceObjectFound(ctx, ucfResourceObject, fetchAssociations);
    }

    /**
     * We do not need to deal specifically with OK/error states here.
     * {@link ResourceObjectCompleter} handles both cases, and sets the state in {@link #completeResourceObject} appropriately.
     *
     * We only have to set the resulting state to {@link #initializationState}.
     */
    @Override
    public void initializeInternal(Task task, OperationResult result) throws CommonException {
        effectiveCtx = originalCtx.spawn(
                initialResourceObject.bean.getAuxiliaryObjectClass(),
                task);
        completeResourceObject =
                ResourceObjectCompleter.completeResourceObject(effectiveCtx, initialResourceObject, fetchAssociations, result);
        getInitializationState().recordError(completeResourceObject.errorState());
    }

    /** For clients that want to access the "raw" data. */
    @NotNull UcfResourceObject getInitialUcfResourceObject() {
        return initialUcfResourceObject;
    }

    /** Returns the best available resource object. */
    public @NotNull ExistingResourceObjectShadow getResourceObject() {
        return getCompleteResourceObject().resourceObject();
    }

    private @NotNull CompleteResourceObject getCompleteResourceObject() {
        checkInitialized();
        return Objects.requireNonNullElseGet(
                completeResourceObject,
                () -> CompleteResourceObject.of(initialResourceObject, initializationState.getErrorState()));
    }

    @Override
    public Trace getLogger() {
        return LOGGER;
    }

    @Override
    public void checkConsistence() {
        initialResourceObject.checkConsistence();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObject=" + initialResourceObject +
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
        DebugUtil.debugDumpWithLabelLn(sb, "initializationState", String.valueOf(initializationState), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", initialResourceObject, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "completeResourceObject", completeResourceObject, indent + 1);
        return sb.toString();
    }
}
