/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.prism.delta.PropertyDeltaCollectionsUtil.findPropertyDelta;
import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsNormalizationUtil.getMatchingRule;
import static com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil.determinePrimaryIdentifierValue;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.util.DebugUtil.debugDumpLazily;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.common.Clock;

import com.evolveum.midpoint.provisioning.api.ResourceObjectClassification;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ShadowDeathEvent;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowsNormalizationUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Updates shadows as needed. This is one of public classes of this package.
 */
@Component
public class ShadowUpdater {

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private Protector protector;
    @Autowired private ShadowDeltaComputerAbsolute shadowDeltaComputerAbsolute;
    @Autowired private ShadowFinder shadowFinder;
    @Autowired private PendingOperationsHelper pendingOperationsHelper;
    @Autowired private EventDispatcher eventDispatcher;
    @Autowired private Clock clock;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowUpdater.class);

    public void addTombstoneDeltas(ShadowType repoShadow, List<ItemDelta<?, ?>> shadowModifications)
            throws SchemaException {
        LOGGER.trace("Adding deltas that mark shadow {} as dead", repoShadow);
        if (ShadowUtil.isExists(repoShadow)) {
            shadowModifications.add(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_EXISTS).replace(false)
                            .asItemDelta());
        }
        if (!ShadowUtil.isDead(repoShadow)) {
            shadowModifications.addAll(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_DEAD).replace(true)
                            .item(ShadowType.F_DEATH_TIMESTAMP).replace(clock.currentTimeXMLGregorianCalendar())
                            .asItemDeltas());
        }
        if (repoShadow.getPrimaryIdentifierValue() != null) {
            // We need to free the identifier for further use by live shadows that may come later
            shadowModifications.add(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                            .asItemDelta());
        }
    }

    /**
     * Takes a list of modifications targeting the repository object and/or the resource object.
     * Derives modifications relevant to the repository object itself. See {@link ShadowDeltaComputerRelative}.
     *
     * Applies those repo modifications: into repository and into in-memory object representation.
     */
    public void modifyRepoShadow(
            ProvisioningContext ctx,
            ShadowType repoShadow,
            Collection<? extends ItemDelta<?, ?>> modifications,
            OperationResult result) throws SchemaException,
            ObjectNotFoundException, ConfigurationException {

        Collection<? extends ItemDelta<?, ?>> repoModifications =
                new ShadowDeltaComputerRelative(ctx, repoShadow, modifications, protector)
                        .computeShadowModifications();

        executeRepoShadowModifications(ctx, repoShadow, repoModifications, result);
    }

    void executeRepoShadowDeletion(ShadowType repoShadow, Task task, OperationResult result) {
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
            @NotNull ShadowType repoShadow,
            @NotNull Collection<? extends ItemDelta<?, ?>> repoModifications,   // todo this should be changed to Collection<ItemDelta<?, ?>> [viliam]
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        repoModifications = List.copyOf(repoModifications);

        if (!repoModifications.isEmpty()) {
            MetadataUtil.addModificationMetadataDeltas((Collection<ItemDelta<?,?>>) repoModifications, repoShadow); // todo not very nice [viliam]

            LOGGER.trace("Applying repository shadow modifications:\n{}", debugDumpLazily(repoModifications, 1));
            try {
                ConstraintsChecker.onShadowModifyOperation(repoModifications);
                repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), repoModifications, result);
                // Maybe we should catch ObjectNotFoundException here and issue death event. But unless such deletion occurred
                // in raw mode by the administrator, we shouldn't care, because the thread that deleted the shadow should have
                // updated the links accordingly.
                if (wasMarkedDead(repoShadow, repoModifications)) {
                    issueShadowDeathEvent(repoShadow.getOid(), ctx.getTask(), result);
                }
                // This is important e.g. to update opState.repoShadow content in case of ADD operation success
                // - to pass newly-generated primary identifier to other parts of the code.
                ItemDeltaCollectionsUtil.applyTo(repoModifications, repoShadow.asPrismObject());
                LOGGER.trace("Shadow changes processed successfully.");
            } catch (ObjectAlreadyExistsException ex) {
                throw SystemException.unexpected(ex, "when updating shadow in the repository");
            }
        }
    }

    private void issueShadowDeathEvent(String shadowOid, Task task, OperationResult result) {
        eventDispatcher.notify(
                ShadowDeathEvent.dead(shadowOid), task, result);
    }

    private void issueShadowDeletionEvent(String shadowOid, Task task, OperationResult result) {
        eventDispatcher.notify(
                ShadowDeathEvent.deleted(shadowOid), task, result);
    }

    private boolean wasMarkedDead(ShadowType stateBefore, Collection<? extends ItemDelta<?, ?>> changes) {
        return !ShadowUtil.isDead(stateBefore) && changedToDead(changes);
    }

    private boolean changedToDead(Collection<? extends ItemDelta<?, ?>> changes) {
        PropertyDelta<Object> deadDelta = findPropertyDelta(changes, (ItemPath) ShadowType.F_DEAD);
        return deadDelta != null &&
                (containsTrue(deadDelta.getRealValuesToAdd()) || containsTrue(deadDelta.getRealValuesToReplace()));
    }

    private boolean containsTrue(Collection<?> values) {
        return values != null && values.contains(Boolean.TRUE);
    }

    public ShadowType markShadowTombstone(ShadowType repoShadow, Task task, OperationResult result)
            throws SchemaException {
        if (repoShadow == null) {
            return null;
        }
        List<ItemDelta<?, ?>> shadowChanges = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_DEAD).replace(true)
                .item(ShadowType.F_DEATH_TIMESTAMP).replace(clock.currentTimeXMLGregorianCalendar()) // TODO what if already dead?
                .item(ShadowType.F_EXISTS).replace(false)
                // We need to free the identifier for further use by live shadows that may come later
                .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                .asItemDeltas();

        MetadataUtil.addModificationMetadataDeltas(shadowChanges, repoShadow);

        LOGGER.trace("Marking shadow {} as tombstone", repoShadow);
        try {
            repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), shadowChanges, result);
        } catch (ObjectAlreadyExistsException e) {
            // Should not happen, this is not a rename
            throw new SystemException(e.getMessage(), e);
        } catch (ObjectNotFoundException e) {
            // Cannot be more dead
            LOGGER.trace("Attempt to mark shadow {} as tombstone found that no such shadow exists", repoShadow);
            // Maybe we should catch ObjectNotFoundException here and issue death event. But unless such deletion occurred
            // in raw mode by the administrator, we shouldn't care, because the thread that deleted the shadow should have
            // updated the links accordingly.
            return null;
        }
        issueShadowDeathEvent(repoShadow.getOid(), task, result);
        ObjectDeltaUtil.applyTo(repoShadow.asPrismObject(), shadowChanges);
        repoShadow.setShadowLifecycleState(ShadowLifecycleStateType.TOMBSTONE);
        return repoShadow;
    }

    /**
     * Checks that the live shadow is marked as existing.
     *
     * Side effects: marks the live shadow as existing, if it is not marked as such yet.
     *
     * Returns `false` if the shadow has disappeared in the meantime.
     */
    public boolean markLiveShadowExistingIfNotMarkedSo(ShadowType liveShadow, OperationResult result) throws SchemaException {
        assert ShadowUtil.isNotDead(liveShadow);
        if (ShadowUtil.isExists(liveShadow)) {
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
    private boolean markShadowExists(ShadowType repoShadow, OperationResult parentResult) throws SchemaException {
        List<ItemDelta<?, ?>> shadowChanges = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_EXISTS).replace(true)
                .asItemDeltas();

        MetadataUtil.addModificationMetadataDeltas(shadowChanges, repoShadow);

        LOGGER.trace("Marking shadow {} as existent", repoShadow);
        try {
            repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), shadowChanges, parentResult);
        } catch (ObjectAlreadyExistsException e) {
            // Should not happen, this is not a rename
            throw new SystemException(e.getMessage(), e);
        } catch (ObjectNotFoundException e) {
            LOGGER.trace("Attempt to mark shadow {} as existent found that no such shadow exists", repoShadow);
            return false;
        }
        ObjectDeltaUtil.applyTo(repoShadow.asPrismObject(), shadowChanges);
        return true;
    }

    public void refreshProvisioningIndexes(
            ProvisioningContext ctx, ShadowType repoShadow, boolean resolveDuplicates, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        String currentPrimaryIdentifierValue = repoShadow.getPrimaryIdentifierValue();
        String expectedPrimaryIdentifierValue = determinePrimaryIdentifierValue(ctx, repoShadow);

        if (Objects.equals(currentPrimaryIdentifierValue, expectedPrimaryIdentifierValue)) {
            // Everything is all right
            return;
        }
        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace(expectedPrimaryIdentifierValue)
                .asItemDeltas();

        MetadataUtil.addModificationMetadataDeltas(modifications, repoShadow);

        LOGGER.trace("Correcting primaryIdentifierValue for {}: {} -> {}",
                repoShadow, currentPrimaryIdentifierValue, expectedPrimaryIdentifierValue);
        try {

            repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), modifications, result);

        } catch (ObjectAlreadyExistsException e) {
            if (!resolveDuplicates) {
                throw e; // Client will take care of this
            }

            // Boom! We have some kind of inconsistency here. There is not much we can do to fix it.
            // But let's try to find offending object.
            LOGGER.error("Error updating primaryIdentifierValue for {} to value {}: {}",
                    repoShadow, expectedPrimaryIdentifierValue, e.getMessage(), e);

            ShadowType potentialConflictingShadow =
                    asObjectable(shadowFinder.lookupShadowByIndexedPrimaryIdValue(ctx, expectedPrimaryIdentifierValue, result));
            LOGGER.debug("REPO CONFLICT: potential conflicting repo shadow (by primaryIdentifierValue)\n{}",
                    DebugUtil.debugDumpLazily(potentialConflictingShadow, 1));
            String conflictingShadowPrimaryIdentifierValue = determinePrimaryIdentifierValue(ctx, potentialConflictingShadow);

            if (Objects.equals(conflictingShadowPrimaryIdentifierValue, potentialConflictingShadow.getPrimaryIdentifierValue())) {
                // Whoohoo, the conflicting shadow has good identifier. And it is the same as ours.
                // We really have two conflicting shadows here.
                LOGGER.info("REPO CONFLICT: Found conflicting shadows that both claim the values of primaryIdentifierValue={}\n"
                                + "Shadow with existing value:\n{}\nShadow that should have the same value:\n{}",
                        expectedPrimaryIdentifierValue, potentialConflictingShadow, repoShadow);
                throw new SystemException("Duplicate shadow conflict with " + potentialConflictingShadow);
            }

            // The other shadow has wrong primaryIdentifierValue. Therefore let's reset it.
            // Even though we do know the correct value of primaryIdentifierValue, do NOT try to set it here. It may conflict with
            // another shadow and the we will end up in an endless loop of conflicts all the way down to hell. Resetting it to null
            // is safe. And as that shadow has a wrong value, it obviously haven't been refreshed yet. It's turn will come later.
            LOGGER.debug("Resetting primaryIdentifierValue in conflicting shadow {}", repoShadow);
            List<ItemDelta<?, ?>> resetModifications = prismContext.deltaFor(ShadowType.class)
                    .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                    .asItemDeltas();

            MetadataUtil.addModificationMetadataDeltas(resetModifications, repoShadow);

            try {
                repositoryService.modifyObject(ShadowType.class, potentialConflictingShadow.getOid(), resetModifications, result);
            } catch (ObjectAlreadyExistsException ee) {
                throw new SystemException(
                        String.format("Attempt to reset primaryIdentifierValue on %s failed: %s",
                                potentialConflictingShadow, ee.getMessage()),
                        ee);
            }

            // Now we should be free to set up correct identifier. Finally.
            try {
                repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), modifications, result);
            } catch (ObjectAlreadyExistsException ee) {
                // Oh no! Not again!
                throw new SystemException(
                        String.format("Despite all our best efforts, attempt to refresh primaryIdentifierValue on %s failed: %s",
                                repoShadow, ee.getMessage()),
                        ee);
            }
        }
        repoShadow.setPrimaryIdentifierValue(expectedPrimaryIdentifierValue);
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
    public PendingOperationType checkAndRecordPendingOperationBeforeExecution(
            @NotNull ProvisioningContext ctx,
            @NotNull ObjectDelta<ShadowType> proposedDelta,
            @NotNull ProvisioningOperationState<?> opState,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException {
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
     * @param currentResourceObject Current state of the resource object. Not shadowized yet.
     * @param resourceObjectDelta Delta coming from the resource (if known).
     *
     * @return repository shadow as it should look like after the update
     *
     * @see ShadowDeltaComputerAbsolute
     */
    public @NotNull ShadowType updateShadowInRepository(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType currentResourceObject,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta,
            @NotNull ShadowType repoShadow,
            ShadowLifecycleStateType shadowState, // TODO ensure this is filled-in
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {

        LOGGER.trace("updateShadowInRepository starting; shadowState = {}", shadowState);
        if (resourceObjectDelta == null) {
            repoShadow = retrieveIndexOnlyAttributesIfNeeded(ctx, repoShadow, result);
        } else {
            LOGGER.trace("Resource object delta is present. We assume we will be able to update the shadow without "
                    + "explicitly reading index-only attributes first."); // TODO check if this assumption is correct
        }

        ObjectDelta<ShadowType> computedShadowDelta =
                shadowDeltaComputerAbsolute.computeShadowDelta(
                        ctx, repoShadow, currentResourceObject, resourceObjectDelta, shadowState, true);

        if (!computedShadowDelta.isEmpty()) {
            LOGGER.trace("Updating repo shadow {} with delta:\n{}", repoShadow, computedShadowDelta.debugDumpLazily(1));
            executeRepoShadowModifications(ctx, repoShadow, computedShadowDelta.getModifications(), result);
            ShadowType updatedShadow = repoShadow.clone();
            computedShadowDelta.applyTo(updatedShadow.asPrismObject());
            return updatedShadow;
        } else {
            LOGGER.trace("No need to update repo shadow {} (empty delta)", repoShadow);
            return repoShadow;
        }
    }

    private @NotNull ShadowType retrieveIndexOnlyAttributesIfNeeded(
            @NotNull ProvisioningContext shadowCtx, @NotNull ShadowType repoShadow, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        ResourceObjectDefinition objectDefinition = shadowCtx.getObjectDefinition();
        if (objectDefinition == null) {
            // TODO consider throwing an exception
            LOGGER.warn("No resource object definition for {}", shadowCtx);
            return repoShadow;
        }

        if (!objectDefinition.hasIndexOnlyAttributes()) {
            LOGGER.trace("No index only attributes -> nothing to retrieve");
            return repoShadow;
        }

        if (ShadowUtil.getAttributes(repoShadow).stream()
                .noneMatch(Item::isIncomplete)) {
            LOGGER.trace("All repo attributes are complete -> nothing to retrieve");
            return repoShadow;
        }

        LOGGER.debug("Re-reading the shadow, retrieving all attributes (including index-only ones): {}", repoShadow);
        Collection<SelectorOptions<GetOperationOptions>> options =
                SchemaService.get().getOperationOptionsBuilder()
                        .item(ShadowType.F_ATTRIBUTES).retrieve(RetrieveOption.INCLUDE)
                        .build();

        ShadowType retrievedRepoShadow =
                repositoryService
                        .getObject(ShadowType.class, repoShadow.getOid(), options, result)
                        .asObjectable();

        shadowCtx.applyAttributesDefinition(retrievedRepoShadow);
        shadowCtx.updateShadowState(retrievedRepoShadow);

        LOGGER.trace("Full repo shadow:\n{}", retrievedRepoShadow.debugDumpLazily(1));

        return retrievedRepoShadow;
    }

    public void deleteShadow(ShadowType oldRepoShadow, Task task, OperationResult result) {
        executeRepoShadowDeletion(oldRepoShadow, task, result);
    }

    /**
     * Re-reads the shadow, re-evaluates the identifiers and stored values (including their normalization under matching rules),
     * updates them if necessary in the repository.
     *
     * Returns fixed shadow.
     *
     * See also {@link ShadowsNormalizationUtil}. (However, this code is too specific to be put there.)
     */
    public @NotNull ShadowType normalizeShadowAttributesInRepository(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType origRepoShadow,
            @Nullable ResourceObjectClassification newClassification,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        PrismObject<ShadowType> currentRepoShadow =
                repositoryService.getObject(ShadowType.class, origRepoShadow.getOid(), null, result);
        if (newClassification != null && ctx.areShadowChangesSimulated()) {
            // In shadow simulation, we would lose kind/intent on repository re-read here.
            // So we have to manually set the new values into the shadow.
            currentRepoShadow.asObjectable()
                    .kind(newClassification.getKind())
                    .intent(newClassification.getIntent());
        }
        ResourceObjectDefinition objectDef = ctx
                .spawnForShadow(currentRepoShadow.asObjectable())
                .getObjectDefinitionRequired();
        List<ItemDelta<?, ?>> normalizationDeltas = new ArrayList<>();
        for (Item<?, ?> attribute : ShadowUtil.getAttributesRaw(currentRepoShadow)) {
            addNormalizationDeltas(normalizationDeltas, attribute, objectDef);
        }
        if (!normalizationDeltas.isEmpty()) {
            LOGGER.trace("Normalizing shadow {} with deltas:\n{}", origRepoShadow, debugDumpLazily(normalizationDeltas));
            // TODO should we put there origRepoShadow or currentRepoShadow?
            executeRepoShadowModifications(ctx, origRepoShadow, normalizationDeltas, result);
        } else {
            LOGGER.trace("No need to normalize shadow {} (no differences)", origRepoShadow);
        }
        return currentRepoShadow.asObjectable();
    }

    private <T> void addNormalizationDeltas(
            List<ItemDelta<?, ?>> normalizationDeltas, Item<?, ?> attribute, ResourceObjectDefinition objectDef)
            throws SchemaException {
        if (!(attribute instanceof PrismProperty<?>)) {
            LOGGER.trace("Ignoring non-property item in attribute container: {}", attribute);
        } else {
            //noinspection unchecked
            ResourceAttributeDefinition<T> attrDef =
                    (ResourceAttributeDefinition<T>) objectDef.findAttributeDefinition(attribute.getElementName());
            if (attrDef != null) {
                //noinspection unchecked
                addNormalizationDeltas(normalizationDeltas, (PrismProperty<T>) attribute, attrDef);
            } else {
                addDeletionDelta(normalizationDeltas, (PrismProperty<?>) attribute);
            }
        }
    }

    private <T> void addNormalizationDeltas(
            List<ItemDelta<?, ?>> normalizationDeltas,
            PrismProperty<T> attribute,
            ResourceAttributeDefinition<T> attrDef) throws SchemaException {
        attribute.applyDefinition(attrDef);
        MatchingRule<T> matchingRule = getMatchingRule(attrDef);
        List<T> valuesToAdd = null;
        List<T> valuesToDelete = null;
        boolean anyChange = false;
        for (PrismPropertyValue<T> attrVal : attribute.getValues()) {
            T currentRealValue = attrVal.getValue();
            T normalizedRealValue = matchingRule.normalize(currentRealValue);
            if (!normalizedRealValue.equals(currentRealValue)) {
                if (attrDef.isSingleValue()) {
                    PropertyDelta<T> attrDelta = attribute.createDelta(attribute.getPath());
                    //noinspection unchecked
                    attrDelta.setRealValuesToReplace(normalizedRealValue);
                    normalizationDeltas.add(attrDelta);
                    return;
                } else {
                    if (!anyChange) {
                        valuesToAdd = new ArrayList<>();
                        valuesToDelete = new ArrayList<>();
                    }
                    valuesToAdd.add(normalizedRealValue);
                    valuesToDelete.add(currentRealValue);
                    anyChange = true;
                }
            }
        }
        if (anyChange) {
            PropertyDelta<T> attrDelta = attribute.createDelta(attribute.getPath());
            attrDelta.addRealValuesToAdd(valuesToAdd);
            attrDelta.addRealValuesToDelete(valuesToDelete);
            normalizationDeltas.add(attrDelta);
        }
    }

    private <T> void addDeletionDelta(List<ItemDelta<?, ?>> normalizationDeltas, PrismProperty<T> attribute) {
        // No definition for this property, it should not be in the shadow
        PropertyDelta<?> delta = attribute.createDelta();
        //noinspection unchecked,rawtypes
        delta.addValuesToDelete(
                (Collection) PrismValueCollectionsUtil.cloneCollection(attribute.getValues()));
        normalizationDeltas.add(delta);
    }

    public void cancelAllPendingOperations(ProvisioningContext ctx, ShadowType repoShadow, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        List<ItemDelta<?, ?>> shadowDeltas = pendingOperationsHelper.cancelAllPendingOperations(repoShadow);
        if (!shadowDeltas.isEmpty()) {
            LOGGER.debug("Cancelling pending operations on {}", repoShadow);
            executeRepoShadowModifications(ctx, repoShadow, shadowDeltas, result);
        }
    }
}
