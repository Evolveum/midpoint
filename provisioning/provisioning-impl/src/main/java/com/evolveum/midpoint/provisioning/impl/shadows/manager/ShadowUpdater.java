/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil.determinePrimaryIdentifierValue;
import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaUtil;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;
import com.evolveum.midpoint.provisioning.api.ShadowDeathEvent;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.RepoShadowModifications;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.shadows.PendingOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.RepoShadowWithState;
import com.evolveum.midpoint.repo.api.ModifyObjectResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.ObjectOperationPolicyHelper.EffectiveMarksAndPolicies;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Updates shadows as needed. This is one of public classes of this package.
 */
@Component
public class ShadowUpdater {

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private ShadowFinder shadowFinder;
    @Autowired private PendingOperationsHelper pendingOperationsHelper;
    @Autowired private EventDispatcher eventDispatcher;
    @Autowired private Clock clock;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowUpdater.class);

    public @NotNull List<ItemDelta<?, ?>> createTombstoneDeltas(RepoShadow repoShadow) throws SchemaException {
        List<ItemDelta<?, ?>> deltas = new ArrayList<>();
        LOGGER.trace("Adding deltas that mark shadow {} as dead", repoShadow);
        if (repoShadow.doesExist()) {
            deltas.add(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_EXISTS).replace(false)
                            .asItemDelta());
        }
        if (!repoShadow.isDead()) {
            deltas.addAll(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_DEAD).replace(true)
                            .item(ShadowType.F_DEATH_TIMESTAMP).replace(clock.currentTimeXMLGregorianCalendar())
                            .asItemDeltas());
        }
        if (repoShadow.getBean().getPrimaryIdentifierValue() != null) {
            // We need to free the identifier for further use by live shadows that may come later
            deltas.add(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                            .asItemDelta());
        }
        return deltas;
    }

    /**
     * Takes a list of modifications targeting the repository object and/or the resource object.
     * Derives modifications relevant to the repository object itself. See {@link ShadowDeltaComputerRelative}.
     *
     * Applies those repo modifications: into repository and into in-memory object representation.
     */
    public void modifyRepoShadow(
            ProvisioningContext ctx,
            RepoShadow repoShadow,
            Collection<? extends ItemDelta<?, ?>> modifications,
            OperationResult result) throws SchemaException,
            ObjectNotFoundException {

        var repoModifications =
                new ShadowDeltaComputerRelative(ctx, repoShadow, modifications)
                        .computeShadowModifications(result);

        executeRepoShadowModifications(ctx, repoShadow, repoModifications, result);
    }

    void executeRepoShadowDeletion(RepoShadow repoShadow, Task task, OperationResult result) {
        try {
            LOGGER.trace("Deleting repository {}", repoShadow);
            repositoryService.deleteObject(ShadowType.class, repoShadow.getOid(), result);
            // Maybe we should issue death event even if the shadow was not found. But unless such previous deletion occurred
            // in raw mode by the administrator, we shouldn't care, because the thread that deleted the shadow should have
            // updated the links accordingly.
            issueShadowDeletionEvent(repoShadow.getOid(), task, result);
        } catch (ObjectNotFoundException e) {
            result.muteLastSubresultError();
            LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't delete already deleted shadow {}, continuing", e, repoShadow);
        }
    }

    /** This is the real delta executions. The in-memory shadow is updated with them as well. Must not contain resource mods! */
    public void executeRepoShadowModifications(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadow repoShadow,
            @NotNull RepoShadowModifications modifications,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        if (modifications.isEmpty()) {
            LOGGER.trace("No need to update repo shadow {} (empty delta)", repoShadow);
            return;
        }

        RepoShadowModifications clonedModifications = modifications.shallowCopy();

        MetadataUtil.addModificationMetadataDeltas(clonedModifications, repoShadow);

        LOGGER.trace("Applying repository shadow modifications:\n{}", debugDumpLazily(clonedModifications, 1));
        try {
            ConstraintsChecker.onShadowModifyOperation(clonedModifications);
            var modifyResult = executeRepoShadowModificationsRaw(repoShadow, clonedModifications.getRawItemDeltas(), result);
            // Maybe we should catch ObjectNotFoundException here and issue death event. But unless such deletion occurred
            // in raw mode by the administrator, we shouldn't care, because the thread that deleted the shadow should have
            // updated the links accordingly.
            if (wasMarkedDead(repoShadow, clonedModifications)) {
                issueShadowDeathEvent(repoShadow.getOid(), ctx.getTask(), result);
            }
            // This is important e.g. to update opState.repoShadow content in case of ADD operation success
            // - to pass newly-generated primary identifier to other parts of the code.
            List<ItemDelta<?, ?>> itemDeltas = clonedModifications.getItemDeltas();
            ctx.applyCurrentDefinition(itemDeltas); // Isn't this just too late?
            repoShadow.updateWith(itemDeltas);
            updateMetadataPcvId(repoShadow, modifyResult);
            LOGGER.trace("Shadow changes processed successfully.");
        } catch (ObjectAlreadyExistsException ex) {
            throw SystemException.unexpected(ex, "when updating shadow in the repository");
        }
    }

    private void updateMetadataPcvId(RepoShadow repoShadow, ModifyObjectResult<ShadowType> modifyResult) {
        var shadow = repoShadow.getBean();
        if (ValueMetadataTypeUtil.needsMetadataValuePcvIdUpdate(shadow)) {
            ValueMetadataTypeUtil.updatePcvId(shadow, modifyResult.getModifications());
        }
    }

    /**
     * Central place for updating the shadows in "shadows" package. Created to easy navigation to all such situations.
     *
     * Does *not* update the in-memory object.
     */
    private ModifyObjectResult<ShadowType> executeRepoShadowModificationsRaw(
            @NotNull RepoShadow repoShadow,
            @NotNull List<ItemDelta<?, ?>> modifications,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        return repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), modifications, result);
    }

    /** Does *not* update the in-memory object. */
    @SuppressWarnings("UnusedReturnValue")
    private ModifyObjectResult<ShadowType> executeRepoShadowModificationsRaw(
            @NotNull RepoShadow repoShadow,
            @NotNull RepoShadowModifications modifications,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        return executeRepoShadowModificationsRaw(repoShadow, modifications.getRawItemDeltas(), result);
    }

    /** A convenience method (updates object in repo and in memory). */
    private void executeRepoAndInMemoryShadowModificationsRaw(
            @NotNull RepoShadow repoShadow,
            @NotNull RepoShadowModifications modifications,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        executeRepoShadowModificationsRaw(repoShadow, modifications.getRawItemDeltas(), result);
        ObjectDeltaUtil.applyTo(repoShadow.getPrismObject(), modifications.getItemDeltas());
    }

    private void issueShadowDeathEvent(String shadowOid, Task task, OperationResult result) {
        eventDispatcher.notify(
                ShadowDeathEvent.dead(shadowOid), task, result);
    }

    private void issueShadowDeletionEvent(String shadowOid, Task task, OperationResult result) {
        eventDispatcher.notify(
                ShadowDeathEvent.deleted(shadowOid), task, result);
    }

    private boolean wasMarkedDead(RepoShadow stateBefore, RepoShadowModifications changes) {
        return !stateBefore.isDead() && changes.changedToDead();
    }

    /** Issues the shadow death event as well! */
    public @Nullable RepoShadow markShadowTombstone(RepoShadow repoShadow, Task task, OperationResult result)
            throws SchemaException {
        if (repoShadow == null) {
            return null;
        }
        RepoShadowModifications modifications = new RepoShadowModifications();
        modifications.addAll(
                prismContext.deltaFor(ShadowType.class)
                        .item(ShadowType.F_DEAD).replace(true)
                        // TODO what if already dead?
                        .item(ShadowType.F_DEATH_TIMESTAMP).replace(clock.currentTimeXMLGregorianCalendar())
                        .item(ShadowType.F_EXISTS).replace(false)
                        // We need to free the identifier for further use by live shadows that may come later
                        .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                        .asItemDeltas());

        MetadataUtil.addModificationMetadataDeltas(modifications, repoShadow);

        LOGGER.trace("Marking shadow {} as tombstone", repoShadow);
        try {
            executeRepoShadowModificationsRaw(repoShadow, modifications, result);
        } catch (ObjectAlreadyExistsException e) {
            // Should not happen, this is not a rename
            throw new SystemException(e.getMessage(), e);
        } catch (ObjectNotFoundException e) {
            // Cannot be more dead
            LOGGER.trace("Attempt to mark shadow {} as tombstone found that no such shadow exists", repoShadow);
            // Maybe we should catch ObjectNotFoundException here and issue death event. But unless such deletion occurred
            // in raw mode by the administrator, we shouldn't care, because the thread that deleted the shadow should have
            // updated the links accordingly.
            return null; // TODO mark shadow as deleted instead! MID-2119
        }
        issueShadowDeathEvent(repoShadow.getOid(), task, result);
        repoShadow.updateWith(modifications.getRawItemDeltas());
        repoShadow.getBean().setShadowLifecycleState(ShadowLifecycleStateType.TOMBSTONE);
        return repoShadow;
    }

    /**
     * Checks that the live shadow is marked as existing.
     *
     * Side effects: marks the live shadow as existing, if it is not marked as such yet.
     *
     * Returns `false` if the shadow has disappeared in the meantime.
     */
    public boolean markLiveShadowExistingIfNotMarkedSo(RepoShadow liveShadow, OperationResult result) throws SchemaException {
        assert !liveShadow.isDead();
        if (liveShadow.doesExist()) {
            return true;
        } else {
            // This is where gestation quantum state collapses.
            // Or maybe the account was created and we have found it before the original thread could mark the shadow as alive.
            // Marking the shadow as existent should not cause much harm. It should only speed up things a little.
            // And it also avoids shadow duplication.
            return markShadowExists(liveShadow, result);
        }
    }

    /** @return false if the shadow was not found. */
    private boolean markShadowExists(RepoShadow repoShadow, OperationResult parentResult) throws SchemaException {
        RepoShadowModifications shadowModifications = new RepoShadowModifications();
        shadowModifications.addAll(
                prismContext.deltaFor(ShadowType.class)
                        .item(ShadowType.F_EXISTS).replace(true)
                        .asItemDeltas());

        MetadataUtil.addModificationMetadataDeltas(shadowModifications, repoShadow);

        LOGGER.trace("Marking shadow {} as existent", repoShadow);
        try {
            executeRepoAndInMemoryShadowModificationsRaw(repoShadow, shadowModifications, parentResult);
            return true;
        } catch (ObjectAlreadyExistsException e) {
            // Should not happen, this is not a rename
            throw new SystemException(e.getMessage(), e);
        } catch (ObjectNotFoundException e) {
            LOGGER.trace("Attempt to mark shadow {} as existent found that no such shadow exists", repoShadow);
            return false;
        }
    }

    public void refreshProvisioningIndexes(
            ProvisioningContext ctx, RepoShadow repoShadow, boolean resolveDuplicates, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, ConfigurationException {

        var shadowBean = repoShadow.getBean();
        String currentPrimaryIdentifierValue = shadowBean.getPrimaryIdentifierValue();
        String expectedPrimaryIdentifierValue = determinePrimaryIdentifierValue(ctx, repoShadow);

        if (Objects.equals(currentPrimaryIdentifierValue, expectedPrimaryIdentifierValue)) {
            // Everything is all right
            return;
        }
        RepoShadowModifications shadowModifications = new RepoShadowModifications();
        shadowModifications.addAll(
                prismContext.deltaFor(ShadowType.class)
                        .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace(expectedPrimaryIdentifierValue)
                        .asItemDeltas());

        MetadataUtil.addModificationMetadataDeltas(shadowModifications, repoShadow);

        LOGGER.trace("Correcting primaryIdentifierValue for {}: {} -> {}",
                shadowBean, currentPrimaryIdentifierValue, expectedPrimaryIdentifierValue);
        try {

            executeRepoShadowModificationsRaw(repoShadow, shadowModifications, result);

        } catch (ObjectAlreadyExistsException e) {

            if (expectedPrimaryIdentifierValue == null) {
                throw SystemException.unexpected(
                        e, "Conflict while resetting primary identifier value? For: " + repoShadow + " in " + ctx);
            }

            if (!resolveDuplicates) {
                throw e; // Client will take care of this
            }

            // Boom! We have some kind of inconsistency here. We will try to fix it by resetting the primaryIdentifierValue
            // on the conflicting shadow. So let's try to find it.
            LOGGER.error("Error updating primaryIdentifierValue for {} to value {}: {}",
                    shadowBean, expectedPrimaryIdentifierValue, e.getMessage(), e);

            RepoShadow potentialConflictingShadow =
                    shadowFinder.lookupShadowByIndexedPrimaryIdValue(ctx, expectedPrimaryIdentifierValue, result);
            if (potentialConflictingShadow == null) {
                throw new SystemException("Duplicate shadow conflict, but no conflicting shadow found for: " + shadowBean);
            }

            LOGGER.debug("REPO CONFLICT: potential conflicting repo shadow (by primaryIdentifierValue)\n{}",
                    DebugUtil.debugDumpLazily(potentialConflictingShadow, 1));
            String conflictingShadowPrimaryIdentifierValue = determinePrimaryIdentifierValue(potentialConflictingShadow);

            if (Objects.equals(
                    conflictingShadowPrimaryIdentifierValue,
                    potentialConflictingShadow.getBean().getPrimaryIdentifierValue())) {
                // Whoohoo, the conflicting shadow has good identifier. And it is the same as ours.
                // We really have two conflicting shadows here.
                LOGGER.info("""
                                REPO CONFLICT: Found conflicting shadows that both claim the values of primaryIdentifierValue={}
                                Shadow with existing value:
                                {}
                                Shadow that should have the same value:
                                {}""",
                        expectedPrimaryIdentifierValue, potentialConflictingShadow, shadowBean);
                throw new SystemException("Duplicate shadow conflict with " + potentialConflictingShadow);
            }

            // The other shadow has wrong primaryIdentifierValue. Therefore let's reset it.
            // Even though we do know the correct value of primaryIdentifierValue, do NOT try to set it here. It may conflict with
            // another shadow and the we will end up in an endless loop of conflicts all the way down to hell. Resetting it to null
            // is safe. And as that shadow has a wrong value, it obviously haven't been refreshed yet. It's turn will come later.
            LOGGER.debug("Resetting primaryIdentifierValue in conflicting shadow {}", shadowBean);
            RepoShadowModifications resetModifications = new RepoShadowModifications();
            resetModifications.addAll(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                            .asItemDeltas());

            MetadataUtil.addModificationMetadataDeltas(resetModifications, repoShadow);

            try {
                executeRepoShadowModificationsRaw(potentialConflictingShadow, resetModifications, result);
            } catch (ObjectAlreadyExistsException ee) {
                throw new SystemException(
                        String.format("Attempt to reset primaryIdentifierValue on %s failed: %s",
                                potentialConflictingShadow, ee.getMessage()),
                        ee);
            }

            // Now we should be free to set up correct identifier. Finally.
            try {
                executeRepoShadowModificationsRaw(repoShadow, resetModifications, result);
            } catch (ObjectAlreadyExistsException ee) {
                // Oh no! Not again!
                throw new SystemException(
                        String.format("Despite all our best efforts, attempt to refresh primaryIdentifierValue on %s failed: %s",
                                shadowBean, ee.getMessage()),
                        ee);
            }
        }
        shadowBean.setPrimaryIdentifierValue(expectedPrimaryIdentifierValue);
    }

    /**
     * Returns conflicting operation (pending delta) if there is any.
     * The repo shadow in opState is updated.
     *
     * BEWARE: updated repo shadow is raw. ApplyDefinitions must be called on it before any serious use.
     *
     * For more information, see {@link PendingOperationsHelper#checkAndRecordPendingOperationBeforeExecution(ProvisioningContext,
     * ObjectDelta, ProvisioningOperationState, OperationResult)}.
     */
    public PendingOperation checkAndRecordPendingOperationBeforeExecution(
            @NotNull ProvisioningContext ctx,
            @NotNull ObjectDelta<ShadowType> proposedDelta,
            @NotNull ProvisioningOperationState opState,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        return pendingOperationsHelper.checkAndRecordPendingOperationBeforeExecution(ctx, proposedDelta, opState, result);
    }

    /**
     * Updates repository shadow based on an object or a delta obtained from the resource.
     * What is updated:
     *
     * - cached attributes and activation,
     * - shadow name,
     * - aux object classes,
     * - exists flag,
     * - "production" flag,
     * - caching metadata.
     *
     * Retrieves index-only attributes from repo if needed.
     *
     * The object definition in the context should correspond to the one in resource object.
     *
     * @param resourceObject Current state of the resource object. Not shadowized yet.
     * @param resourceObjectDelta Delta coming from the resource (if known).
     * @return repository shadow as it should look like after the update
     * @see ShadowDeltaComputerAbsolute
     */
    public @NotNull RepoShadowWithState updateShadowInRepositoryAndInMemory(
            @NotNull ProvisioningContext ctx,
            @NotNull RepoShadowWithState repoShadow,
            @NotNull ResourceObjectShadow resourceObject,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta,
            @Nullable ResourceObjectClassification newClassification,
            @NotNull EffectiveMarksAndPolicies effectiveMarksAndPolicies,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, EncryptionException {

        LOGGER.trace(
                """
                        updateShadowInRepository starting.
                        Repository shadow:
                        {}
                        Object fetched from the resource:
                        {}
                        Known resource object delta (for changes):
                        {}
                        New classification (if applicable): {}""",
                repoShadow.shadow().debugDumpLazily(1),
                resourceObject.debugDumpLazily(1),
                DebugUtil.debugDumpLazily(resourceObjectDelta, 1),
                newClassification);

        Preconditions.checkArgument(
                Objects.equals(ctx.getObjectDefinitionRequired(), resourceObject.getObjectDefinition()),
                "Inconsistent object definitions in ctx and object: %s vs %s",
                ctx.getObjectDefinition(), resourceObject.getObjectDefinition());

        if (resourceObjectDelta == null) {
            var refreshedShadow = retrieveIndexOnlyAttributesIfNeeded(ctx, repoShadow.shadow(), result);
            repoShadow = repoShadow.withShadow(refreshedShadow);
        } else {
            LOGGER.trace("Resource object delta is present. We assume we will be able to update the shadow without "
                    + "explicitly reading all index-only attributes."); // TODO check if this assumption is correct
        }

        RepoShadowModifications shadowModifications =
                ShadowDeltaComputerAbsolute.computeShadowModifications(
                        ctx, repoShadow.shadow(), resourceObject, resourceObjectDelta,
                        effectiveMarksAndPolicies, true, result);

        executeRepoShadowModifications(ctx, repoShadow.shadow(), shadowModifications, result);

        // The effectiveMarkRefs were applied (via modifications above), but transient properties were net. Let's do that here.
        repoShadow.getBean().setEffectiveOperationPolicy(effectiveMarksAndPolicies.effectiveOperationPolicy());
        repoShadow.getBean().setProtectedObject(effectiveMarksAndPolicies.isProtected());

        return repoShadow;
    }

    private @NotNull RepoShadow retrieveIndexOnlyAttributesIfNeeded(
            @NotNull ProvisioningContext shadowCtx, @NotNull RepoShadow repoShadow, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        ResourceObjectDefinition objectDefinition = shadowCtx.getObjectDefinitionRequired();

        if (!objectDefinition.hasIndexOnlyAttributes()) {
            LOGGER.trace("No index only attributes -> nothing to retrieve");
            return repoShadow;
        }

        if (repoShadow.getSimpleAttributes().stream()
                .noneMatch(Item::isIncomplete)) {
            LOGGER.trace("All repo attributes are complete -> nothing to retrieve");
            return repoShadow;
        }

        LOGGER.debug("Re-reading the shadow, retrieving all attributes (including index-only ones): {}", repoShadow);
        Collection<SelectorOptions<GetOperationOptions>> options =
                SchemaService.get().getOperationOptionsBuilder()
                        .item(ShadowType.F_ATTRIBUTES).retrieve(RetrieveOption.INCLUDE)
                        .build();

        RepoShadow retrievedRepoShadow = shadowFinder.getRepoShadow(shadowCtx, repoShadow.getOid(), options, result);

        LOGGER.trace("Full repo shadow:\n{}", retrievedRepoShadow.debugDumpLazily(1));

        return retrievedRepoShadow;
    }

    public void deleteShadow(RepoShadow oldRepoShadow, Task task, OperationResult result) {
        executeRepoShadowDeletion(oldRepoShadow, task, result);
    }

    public void cancelAllPendingOperations(ProvisioningContext ctx, RepoShadow repoShadow, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        var shadowModifications = pendingOperationsHelper.cancelAllPendingOperations(repoShadow);
        if (!shadowModifications.isEmpty()) {
            LOGGER.debug("Cancelling pending operations on {}", repoShadow);
            executeRepoShadowModifications(ctx, repoShadow, shadowModifications, result);
        }
    }
}
