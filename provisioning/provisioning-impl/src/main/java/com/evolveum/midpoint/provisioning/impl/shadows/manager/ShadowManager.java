/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Responsibilities of the shadow manager package:
 *
 * Manage repository shadows, including:
 *
 * 1. Lookup, create, and update shadows (as part of the provisioning process).
 * 2. Getting and searching shadows (as part of get/search operations in higher layers).
 * 3. Management of pending operations
 *
 * Limitations:
 *
 * - Does NOT communicate with the resource (means: please do NOT do anything with the connector)
 *
 * Naming conventions:
 *
 * - _Search_ = plain search in the repository (no modifications)
 * - _Lookup_ = looking up single shadow in the repository; name = "lookup [Live] Shadow By [What]"
 * - _Acquire_ = lookup + create if needed
 * - When talking about _shadow_ we always mean _repository shadow_ here.
 *
 * This class is merely a facade. It delegates almost everything to the following classes:
 *
 * - {@link ShadowFinder}: looking up shadows
 * - {@link ShadowCreator}: creating (or looking up + creating) shadows
 * - {@link ShadowUpdater}: updating existing shadows
 * - plus some helpers
 *
 * @author Katarina Valalikova
 * @author Radovan Semancik
 */
@Component
public class ShadowManager {

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private ShadowFinder shadowFinder;
    @Autowired private ShadowCreator shadowCreator;
    @Autowired private ShadowUpdater shadowUpdater;
    @Autowired private Helper helper;
    @Autowired private QueryHelper queryHelper;

    /** Simply gets a repo shadow from the repository. No magic here. */
    public PrismObject<ShadowType> getShadow(String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        return repositoryService.getObject(ShadowType.class, oid, null, result);
    }

    /**
     * Iteratively searches for shadows in the repository.
     */
    public SearchResultMetadata searchShadowsIterative(ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, ResultHandler<ShadowType> repoHandler,
            OperationResult result) throws SchemaException {

        ObjectQuery repoQuery = queryHelper.applyMatchingRules(query, ctx.getObjectDefinition());
        return repositoryService.searchObjectsIterative(ShadowType.class, repoQuery, repoHandler, options, true, result);
    }

    /**
     * Non-iteratively searches for shadows in the repository.
     */
    public SearchResultList<PrismObject<ShadowType>> searchShadows(ProvisioningContext ctx, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult) throws SchemaException {

        ObjectQuery repoQuery = queryHelper.applyMatchingRules(query, ctx.getObjectDefinition());
        return repositoryService.searchObjects(ShadowType.class, repoQuery, options, parentResult);
    }

    /** Simply counts the shadows in repository. */
    public int countShadows(ProvisioningContext ctx, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result) throws SchemaException {
        ObjectQuery repoQuery = queryHelper.applyMatchingRules(query, ctx.getObjectDefinition());
        return repositoryService.countObjects(ShadowType.class, repoQuery, options, result);
    }

    /**
     * Looks up a live shadow by primary identifier. Differences from other methods:
     *
     * 1. Unlike {@link #lookupShadowByIndexedPrimaryIdValue(ProvisioningContext, String, OperationResult)} this method
     * uses stored attributes to execute the query.
     *
     * 2. Unlike other methods, this checks if the shadow returned has exists=true (and sets it in the repo if it does not).
     * TODO TODO TODO - we should perhaps defer this to the update method! Because that method takes shadow state into account.
     *
     * @param objectClass Intentionally not taken from the context - yet.
     */
    public PrismObject<ShadowType> lookupLiveShadowByPrimaryId(ProvisioningContext ctx,
            @NotNull PrismProperty<?> primaryIdentifier, @NotNull QName objectClass, OperationResult result)
            throws SchemaException {
        return shadowFinder.lookupLiveShadowByPrimaryId(ctx, primaryIdentifier, objectClass, result);
    }

    /**
     * Looks up live (or any other, if there's none) shadow by primary identifier(s).
     */
    public PrismObject<ShadowType> lookupLiveOrAnyShadowByPrimaryIds(ProvisioningContext ctx,
            Collection<ResourceAttribute<?>> identifiers, OperationResult result)
            throws SchemaException, ConfigurationException {
        return ProvisioningUtil.selectLiveOrAnyShadow(
                shadowFinder.searchShadowsByPrimaryIds(ctx, identifiers, result));
    }

    /**
     * Looks up (any) shadow by all available identifiers.
     */
    public PrismObject<ShadowType> lookupLiveShadowByAllIds(ProvisioningContext ctx,
            ResourceAttributeContainer identifierContainer, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException {
        return shadowFinder.lookupLiveShadowByAllIds(ctx, identifierContainer, result);
    }

    /**
     * Looks up (any) shadow with given secondary identifiers.
     * If there are none, null is returned.
     */
    public PrismObject<ShadowType> lookupShadowBySecondaryIds(
            ProvisioningContext ctx, Collection<ResourceAttribute<?>> secondaryIdentifiers, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        return shadowFinder.lookupShadowBySecondaryIds(ctx, secondaryIdentifiers, result);
    }

    /**
     * Looks up (any) shadow by indexed primary identifier, i.e. `primaryIdentifierValue` property.
     */
    public PrismObject<ShadowType> lookupShadowByIndexedPrimaryIdValue(ProvisioningContext ctx,
            String primaryIdentifierValue, OperationResult result) throws SchemaException {
        return shadowFinder.lookupShadowByIndexedPrimaryIdValue(ctx, primaryIdentifierValue, result);
    }

    /**
     * Returns dead shadows "compatible" (having the same primary identifier) as given shadow that is to be added.
     */
    public Collection<PrismObject<ShadowType>> searchForPreviousDeadShadows(ProvisioningContext ctx,
            PrismObject<ShadowType> shadowToAdd, OperationResult result) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        return shadowFinder.searchForPreviousDeadShadows(ctx, shadowToAdd, result);
    }

    /**
     * Adds (without checking for existence) a shadow corresponding to a resource object that was discovered.
     * Used when searching for objects or when completing entitlements.
     */
    @NotNull
    public PrismObject<ShadowType> addDiscoveredRepositoryShadow(ProvisioningContext ctx,
            PrismObject<ShadowType> resourceObject, OperationResult result) throws SchemaException, ConfigurationException,
            ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException,
            EncryptionException {
        return shadowCreator.addDiscoveredRepositoryShadow(ctx, resourceObject, result);
    }

    /**
     * Adds new shadow in the `proposed` state (if proposed shadows processing is enabled).
     * The new shadow is recorded into the `opState`.
     */
    public void addNewProposedShadow(ProvisioningContext ctx, PrismObject<ShadowType> shadowToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, ObjectAlreadyExistsException, EncryptionException {
        shadowCreator.addNewProposedShadow(ctx, shadowToAdd, opState, task, result);
    }

    /**
     * Record results of ADD operation to the shadow: creates a shadow or updates an existing one.
     */
    public void recordAddResult(ProvisioningContext ctx, PrismObject<ShadowType> shadowToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ObjectAlreadyExistsException, ExpressionEvaluationException, EncryptionException {
        shadowUpdater.recordAddResult(ctx, shadowToAdd, opState, parentResult);
    }

    public void addDeadShadowDeltas(PrismObject<ShadowType> repoShadow, List<ItemDelta<?, ?>> shadowModifications)
            throws SchemaException {
        shadowUpdater.addDeadShadowDeltas(repoShadow, shadowModifications);
    }

    /**
     * Record results of an operation that have thrown exception.
     * This happens after the error handler is processed - and only for those
     * cases when the handler has re-thrown the exception.
     */
    public void recordOperationException(ProvisioningContext ctx,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState, ObjectDelta<ShadowType> delta,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {
        shadowUpdater.recordOperationException(ctx, opState, delta, result);
    }

    /**
     * Returns conflicting operation (pending delta) if there is any.
     * Updates the repo shadow in opState.
     *
     * BEWARE: updated repo shadow is raw. ApplyDefinitions must be called on it before any serious use.
     */
    public PendingOperationType checkAndRecordPendingDeleteOperationBeforeExecution(ProvisioningContext ctx,
            @NotNull ProvisioningOperationState<AsynchronousOperationResult> opState,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return shadowUpdater.checkAndRecordPendingDeleteOperationBeforeExecution(ctx, opState, result);
    }

    /**
     * Returns conflicting operation (pending delta) if there is any.
     * Updates the repo shadow in opState.
     *
     * BEWARE: updated repo shadow is raw. ApplyDefinitions must be called on it before any serious use.
     */
    public PendingOperationType checkAndRecordPendingModifyOperationBeforeExecution(ProvisioningContext ctx,
            Collection<? extends ItemDelta<?, ?>> modifications,
            @NotNull ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return shadowUpdater
                .checkAndRecordPendingModifyOperationBeforeExecution(ctx, modifications, opState, result);
    }

    public <A extends AsynchronousOperationResult> void updatePendingOperations(ProvisioningContext ctx,
            PrismObject<ShadowType> shadow, ProvisioningOperationState<A> opState,
            List<PendingOperationType> pendingExecutionOperations, XMLGregorianCalendar now,
            OperationResult result) throws ObjectNotFoundException, SchemaException {
        shadowUpdater.updatePendingOperations(ctx, shadow, opState, pendingExecutionOperations, now, result);
    }

    public <T> T determinePrimaryIdentifierValue(ProvisioningContext ctx, PrismObject<ShadowType> shadow)
            throws SchemaException {
        return helper.determinePrimaryIdentifierValue(ctx, shadow);
    }

    /**
     * @throws ObjectAlreadyExistsException Only if `resolveConflicts` is `false`
     */
    public void refreshProvisioningIndexes(
            ProvisioningContext ctx, PrismObject<ShadowType> repoShadow, boolean resolveConflicts, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
        shadowUpdater.refreshProvisioningIndexes(ctx, repoShadow, resolveConflicts, result);
    }

    public void recordModifyResult(ProvisioningContext ctx, PrismObject<ShadowType> oldRepoShadow,
            Collection<? extends ItemDelta> requestedModifications,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            XMLGregorianCalendar now, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException,
            EncryptionException {
        shadowUpdater.recordModifyResult(ctx, oldRepoShadow, requestedModifications, opState, now, parentResult);
    }

    /**
     * Really modifies shadow attributes. It applies the changes. It is used for synchronous operations and also for
     * applying the results of completed asynchronous operations.
     */
    public void modifyShadowAttributes(
            ProvisioningContext ctx, PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> modifications,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
        shadowUpdater.modifyShadowAttributes(ctx, shadow, modifications, parentResult);
    }

    public boolean isRepositoryOnlyModification(Collection<? extends ItemDelta> modifications) {
        return helper.isRepositoryOnlyModification(modifications);
    }

    /**
     * Updates repository shadow based on object or delta from resource.
     * Updates: cached attributes and activation, shadow name, aux object classes, exists flag, caching metadata.
     *
     * Retrieves index-only attributes from repo if needed.
     *
     * @param currentResourceObject Current state of the resource object. Not shadowized yet.
     * @param resourceObjectDelta Delta coming from the resource (if known).
     *
     * @return repository shadow as it should look like after the update
     */
    public PrismObject<ShadowType> updateShadow(@NotNull ProvisioningContext ctx,
            @NotNull PrismObject<ShadowType> currentResourceObject, ObjectDelta<ShadowType> resourceObjectDelta,
            @NotNull PrismObject<ShadowType> repoShadow, ShadowLifecycleStateType shadowState, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException,
            ExpressionEvaluationException {
        return shadowUpdater.updateShadow(ctx, currentResourceObject, resourceObjectDelta, repoShadow, shadowState, result);
    }

    /**
     * Returns updated repo shadow, or null if shadow is deleted from repository.
     */
    public PrismObject<ShadowType> recordDeleteResult(ProvisioningContext ctx,
            ProvisioningOperationState<AsynchronousOperationResult> opState, ProvisioningOperationOptions options,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, EncryptionException {
        return shadowUpdater.recordDeleteResult(ctx, opState, options, parentResult);
    }

    public void deleteShadow(
            @NotNull PrismObject<ShadowType> oldRepoShadow,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        shadowUpdater.deleteShadow(oldRepoShadow, task, result);
    }

    public PrismObject<ShadowType> markShadowTombstone(PrismObject<ShadowType> repoShadow, Task task,
            OperationResult result) throws SchemaException {
        return shadowUpdater.markShadowTombstone(repoShadow, task, result);
    }

    /**
     * Re-reads the shadow, re-evaluates the identifiers and stored values
     * (including their normalization under matching rules), updates them if necessary.
     *
     * Returns fixed shadow.
     */
    public @NotNull PrismObject<ShadowType> fixShadow(
            @NotNull ProvisioningContext ctx,
            @NotNull PrismObject<ShadowType> origRepoShadow,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        return shadowUpdater.fixShadow(ctx, origRepoShadow, result);
    }

    public void setKindIfNecessary(ShadowType repoShadowType, ProvisioningContext ctx) {
        helper.setKindIfNecessary(repoShadowType, ctx);
    }

    @SafeVarargs
    public final <T> boolean compareAttribute(ResourceObjectDefinition objectDefinition,
            ResourceAttribute<T> attributeA, T... valuesB) throws SchemaException {
        return helper.compareAttribute(objectDefinition, attributeA, valuesB);
    }

    public <T> boolean compareAttribute(ResourceObjectDefinition objectDefinition,
            ResourceAttribute<T> attributeA, ResourceAttribute<T> attributeB) throws SchemaException {
        return helper.compareAttribute(objectDefinition, attributeA, attributeB);
    }
}
