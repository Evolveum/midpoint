/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType.EXECUTING;

import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.schema.util.*;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * A shadow that was fetched from the repository and adapted by applying the resource object definition.
 * It may have the information about the original repo shadow, if its update is needed.
 *
 * Conditions (TODO):
 *
 * . the object definition is known
 * . the resource is known
 * . the bean has the definitions applied
 * . the shadow state is up-to-date
 */
@Experimental
public class RepoShadow implements Cloneable, DebugDumpable, AbstractShadow {

    private static final Trace LOGGER = TraceManager.getTrace(RepoShadow.class);

    @NotNull private final ShadowType bean;

    /**
     * Shadow as it was originally fetched from the repository (if applicable).
     */
    @Nullable private final RawRepoShadow rawRepoShadow;

    @NotNull private final Resource resource;

    /** True if the shadow is known to be deleted from the repository. TODO reconsider */
    boolean deleted;

    private RepoShadow(
            @NotNull ShadowType bean,
            @Nullable RawRepoShadow rawRepoShadow,
            @NotNull Resource resource) {
        this.rawRepoShadow = rawRepoShadow;
        var attributesContainer = ShadowUtil.getAttributesContainer(bean);
        Preconditions.checkNotNull(attributesContainer, "No attributes container in %s", bean);
        Preconditions.checkNotNull(bean.getShadowLifecycleState(), "No LC state in %s", bean);

        String shadowResourceOid = bean.getResourceRef().getOid();
        String realResourceOid = resource.getBean().getOid();
        Preconditions.checkArgument(
                shadowResourceOid.equals(realResourceOid),
                "Mismatching resource OID: shadow %s, real %s", shadowResourceOid, realResourceOid);

        this.bean = bean;
        this.resource = resource;

        checkConsistence();
    }

    private @NotNull ResourceObjectDefinition determineObjectDefinition(ShadowType bean) {
        return ShadowUtil.getAttributesContainer(bean).getDefinition().getComplexTypeDefinition();
    }

    public static @NotNull RepoShadow of(
            @NotNull ShadowType bean, @Nullable RawRepoShadow rawRepoShadow, @NotNull ResourceType resourceBean) {
        return new RepoShadow(bean, rawRepoShadow, Resource.of(resourceBean));
    }

    @Nullable
    public static ShadowType getBean(@Nullable RepoShadow repoShadow) {
        return repoShadow != null ? repoShadow.getBean() : null;
    }

    public @NotNull ShadowType getBean() {
        return bean;
    }

    public @NotNull ResourceType getResourceBean() {
        return resource.getBean();
    }

    public @NotNull String getOid() {
        return MiscUtil.stateNonNull(bean.getOid(), "No OID in %s", bean);
    }

    public static String getOid(@Nullable RepoShadow repoShadow) {
        return repoShadow != null ? repoShadow.getOid() : null;
    }

    @Override
    public @NotNull RepoShadow withNewContent(@NotNull ShadowType newBean) {
        return new RepoShadow(newBean, null, resource);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public RepoShadow clone() {
        return new RepoShadow(bean.clone(), rawRepoShadow != null ? rawRepoShadow.clone() : null, resource);
    }

    @Override
    public String debugDump(int indent) {
        return bean.debugDump(indent); // TODO resource
    }

    @Override
    public String toString() {
        return "%s on %s [%s] (repo)%s".formatted(bean, resource, getShadowLifecycleState(), deleted ? " (deleted)" : "");
    }

    /** Returns freely modifiable (detached) list. */
    public @NotNull List<PendingOperationType> getPendingOperationsSorted() {
        return ShadowUtil.sortPendingOperations(bean.getPendingOperation());
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean hasPendingOperations() {
        return !bean.getPendingOperation().isEmpty();
    }

    public @NotNull PrismObjectDefinition<ShadowType> getPrismDefinition() {
        return Objects.requireNonNull(getPrismObject().getDefinition(), "no prism definition");
    }

    public @NotNull ShadowLifecycleStateType getShadowLifecycleState() {
        return MiscUtil.stateNonNull(
                bean.getShadowLifecycleState(),
        "No LC state in %s", this);
    }

    public PolyStringType getName() {
        return bean.getName();
    }

    public PendingOperationType findPendingAddOperation() {
        return ShadowManagerMiscUtil.findPendingAddOperation(bean);
    }

    public @NotNull String getResourceOid() {
        return Objects.requireNonNull(resource.getBean().getOid());
    }

    public boolean hasRetryableOperation() {
        return emptyIfNull(bean.getPendingOperation()).stream()
                .anyMatch(RepoShadow::isRetryableOperation);
    }

    /**
     * The `true` return value currently implies the operation type is {@link PendingOperationTypeType#RETRY}.
     * (Manual nor asynchronous operation have no attempt number set.)
     */
    public static boolean isRetryableOperation(PendingOperationType pendingOperation) {
        return pendingOperation.getExecutionStatus() == EXECUTING
                && pendingOperation.getAttemptNumber() != null;
    }

    public ResourceObject asResourceObject() throws SchemaException {
        return ResourceObject.fromRepoShadow(this);
    }

    public @NotNull ShadowKindType getKind() {
        return ShadowUtil.getKind(bean);
    }

    public @Nullable String getIntent() {
        return ShadowUtil.getIntent(bean);
    }

    public @NotNull ObjectReferenceType objectRef() {
        return ObjectTypeUtil.createObjectRef(bean);
    }

    public boolean isClassified() {
        return ShadowUtil.isClassified(bean);
    }

    public boolean hasPendingAddOperation() {
        return ProvisioningUtil.hasPendingAddOperation(bean);
    }

    public boolean hasPendingDeleteOperation() {
        return ProvisioningUtil.hasPendingDeleteOperation(bean);
    }

    public boolean isInQuantumState() {
        var state = getShadowLifecycleState();
        return state == ShadowLifecycleStateType.GESTATING || state == ShadowLifecycleStateType.CORPSE;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted() {
        this.deleted = true;
    }

    public @Nullable RawRepoShadow getRawRepoShadow() {
        return rawRepoShadow;
    }
}
