/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ShadowDeathEvent;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadows.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.prism.delta.PropertyDeltaCollectionsUtil.findPropertyDelta;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;
import static com.evolveum.midpoint.util.DebugUtil.*;

/**
 * Updates shadows as needed.
 *
 * This is a result of preliminary split of {@link ShadowManager} functionality that was done in order
 * to make it more understandable. Most probably it is not good enough and should be improved.
 */
@Component
@Experimental
class ShadowUpdater {

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private Clock clock;
    @Autowired private PrismContext prismContext;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private Protector protector;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private ShadowDeltaComputer shadowDeltaComputer;
    @Autowired private ShadowFinder shadowFinder;
    @Autowired private ShadowCreator shadowCreator;
    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired private Helper helper;
    @Autowired private CreatorUpdaterHelper creatorUpdaterHelper;
    @Autowired private PendingOperationsHelper pendingOperationsHelper;
    @Autowired private EventDispatcher eventDispatcher;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowUpdater.class);

    /**
     * Record results of ADD operation to the shadow.
     */
    void recordAddResult(ProvisioningContext ctx, ShadowType shadowToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ObjectAlreadyExistsException, ExpressionEvaluationException, EncryptionException {
        if (opState.getRepoShadow() == null) {
            recordAddResultNewShadow(ctx, shadowToAdd, opState, result);
        } else {
            // We know that we have existing shadow. This may be proposed shadow,
            // or a shadow with failed add operation that was just re-tried
            recordAddResultExistingShadow(ctx, shadowToAdd, opState, result);
        }
    }

    /**
     * Add new active shadow to repository. It is executed after ADD operation on resource.
     * There are several scenarios. The operation may have been executed (synchronous operation),
     * it may be executing (asynchronous operation) or the operation may be delayed due to grouping.
     * This is indicated by the execution status in the opState parameter.
     */
    private void recordAddResultNewShadow(ProvisioningContext ctx, ShadowType resourceObjectToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectAlreadyExistsException, EncryptionException {

        // TODO: check for proposed Shadow. There may be a proposed shadow even if we do not have explicit proposed shadow OID
        //  (e.g. in case that the add operation failed). If proposed shadow is present do modify instead of add.

        ShadowType resourceObject;
        if (opState.wasStarted() && opState.getAsyncResult().getReturnValue() != null) {
            resourceObject = opState.getAsyncResult().getReturnValue().asObjectable();
        } else {
            resourceObject = resourceObjectToAdd;
        }

        ShadowType repoShadow = shadowCreator.createRepositoryShadow(ctx, resourceObject);
        opState.setRepoShadow(asPrismObject(repoShadow));

        if (!opState.isCompleted()) {
            pendingOperationsHelper.addPendingOperationAdd(repoShadow, resourceObject, opState, null);
        }

        creatorUpdaterHelper.addCreateMetadata(repoShadow);

        LOGGER.trace("Adding repository shadow\n{}", repoShadow.debugDumpLazily(1));
        String oid;

        try {

            ConstraintsChecker.onShadowAddOperation(repoShadow); // TODO migrate to repo cache invalidation
            oid = repositoryService.addObject(repoShadow.asPrismObject(), null, result);

        } catch (ObjectAlreadyExistsException ex) {
            // This should not happen. The OID is not supplied and it is generated by the repo.
            // If it happens, it must be a repo bug.
            result.recordFatalErrorNotFinish(
                    "Couldn't add shadow object to the repository. Shadow object already exist. Reason: " + ex.getMessage(), ex);
            throw new ObjectAlreadyExistsException(
                    "Couldn't add shadow object to the repository. Shadow object already exist. Reason: " + ex.getMessage(), ex);
        }
        repoShadow.setOid(oid);
        opState.setRepoShadow(repoShadow.asPrismObject());

        LOGGER.trace("Active shadow added to the repository: {}", repoShadow);

        result.setSuccess();
    }

    private void recordAddResultExistingShadow(ProvisioningContext ctx, ShadowType shadowToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ExpressionEvaluationException {

        final ShadowType resourceShadow;
        if (opState.wasStarted() && opState.getAsyncResult().getReturnValue() != null) {
            resourceShadow = opState.getAsyncResult().getReturnValue().asObjectable();
        } else {
            resourceShadow = shadowToAdd;
        }

        PrismObject<ShadowType> repoShadow = opState.getRepoShadow();

        ObjectDelta<ShadowType> requestDelta = resourceShadow.asPrismObject().createAddDelta();
        Collection<ItemDelta<?, ?>> internalShadowModifications = computeInternalShadowModifications(ctx, opState, requestDelta);
        computeUpdateShadowAttributeChanges(ctx, internalShadowModifications, resourceShadow, asObjectable(repoShadow));
        creatorUpdaterHelper.addModifyMetadataDeltas(repoShadow, internalShadowModifications);

        executeShadowModification(ctx, repoShadow, internalShadowModifications, result);

        result.setSuccess();
    }

    void recordModifyResult(
            ProvisioningContext ctx,
            PrismObject<ShadowType> oldRepoShadow,
            Collection<? extends ItemDelta> requestedModifications,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            XMLGregorianCalendar now,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException {

        ObjectDelta<ShadowType> requestDelta = opState.getRepoShadow().createModifyDelta();
        requestDelta.addModifications(ItemDeltaCollectionsUtil.cloneCollection(requestedModifications));

        List<ItemDelta<?, ?>> internalShadowModifications = computeInternalShadowModifications(ctx, opState, requestDelta);

        List<ItemDelta<?, ?>> modifications;
        if (opState.isCompleted()) {
            modifications = MiscUtil.join(requestedModifications, (List) internalShadowModifications);
        } else {
            modifications = internalShadowModifications;
        }
        if (shouldApplyModifyMetadata()) {
            creatorUpdaterHelper.addModifyMetadataDeltas(opState.getRepoShadow(), modifications);
        }
        LOGGER.trace("Updating repository {} after MODIFY operation {}, {} repository shadow modifications", oldRepoShadow, opState, requestedModifications.size());

        modifyShadowAttributes(ctx, oldRepoShadow, modifications, parentResult);
    }

    private boolean shouldApplyModifyMetadata() {
        SystemConfigurationType config = provisioningService.getSystemConfiguration();
        InternalsConfigurationType internals = config != null ? config.getInternals() : null;
        return internals == null || internals.getShadowMetadataRecording() == null ||
                !Boolean.TRUE.equals(internals.getShadowMetadataRecording().isSkipOnModify());
    }

    private void computeUpdateShadowAttributeChanges(ProvisioningContext ctx, Collection<ItemDelta<?, ?>> repoShadowChanges,
            ShadowType resourceShadow, ShadowType repoShadow) throws SchemaException, ConfigurationException {
        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired();
        CachingStrategyType cachingStrategy = ctx.getCachingStrategy();
        for (ResourceAttributeDefinition<?> attrDef : objectDefinition.getAttributeDefinitions()) {
            if (ProvisioningUtil.shouldStoreAttributeInShadow(objectDefinition, attrDef.getItemName(), cachingStrategy)) {
                ResourceAttribute<Object> resourceAttr =
                        ShadowUtil.getAttribute(asPrismObject(resourceShadow), attrDef.getItemName());
                PrismProperty<Object> repoAttr =
                        repoShadow.asPrismObject().findProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, attrDef.getItemName()));
                PropertyDelta attrDelta;
                if (resourceAttr == null && repoAttr == null) {
                    continue;
                }
                ResourceAttribute<Object> normalizedResourceAttribute = resourceAttr.clone();
                helper.normalizeAttribute(normalizedResourceAttribute, attrDef);
                if (repoAttr == null) {
                    attrDelta = attrDef.createEmptyDelta(ItemPath.create(ShadowType.F_ATTRIBUTES, attrDef.getItemName()));
                    attrDelta.setValuesToReplace(PrismValueCollectionsUtil.cloneCollection(normalizedResourceAttribute.getValues()));
                } else {
                    attrDelta = repoAttr.diff(normalizedResourceAttribute);
//                    LOGGER.trace("DIFF:\n{}\n-\n{}\n=:\n{}", repoAttr==null?null:repoAttr.debugDump(1), normalizedResourceAttribute==null?null:normalizedResourceAttribute.debugDump(1), attrDelta==null?null:attrDelta.debugDump(1));
                }
                if (attrDelta != null && !attrDelta.isEmpty()) {
                    helper.normalizeDelta(attrDelta, attrDef);
                    repoShadowChanges.add(attrDelta);
                }
            }
        }

        String newPrimaryIdentifierValue = helper.determinePrimaryIdentifierValue(ctx, resourceShadow);
        String existingPrimaryIdentifierValue = repoShadow.getPrimaryIdentifierValue();
        if (!Objects.equals(existingPrimaryIdentifierValue, newPrimaryIdentifierValue)) {
            repoShadowChanges.add(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace(newPrimaryIdentifierValue)
                            .asItemDelta()
            );
        }

        // TODO: reflect activation updates on cached shadow
    }

    /**
     * Returns updated repo shadow, or null if shadow is deleted from repository.
     */
    PrismObject<ShadowType> recordDeleteResult(
            ProvisioningContext ctx,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            ProvisioningOperationOptions options,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        PrismObject<ShadowType> repoShadow = opState.getRepoShadow();

        if (ProvisioningOperationOptions.isForce(options)) {
            LOGGER.trace("Deleting repository {} (force delete): {}", repoShadow, opState);
            executeShadowDeletion(repoShadow, ctx.getTask(), result);
            // TODO why not setting repo shadow null in opState?
            return null;
        }

        if (!opState.hasPendingOperations() && opState.isCompleted()) {
            if (repoShadow.asObjectable().getPendingOperation().isEmpty() && opState.isSuccess()) {
                LOGGER.trace("Deleting repository {}: {}", repoShadow, opState);
                executeShadowDeletion(repoShadow, ctx.getTask(), result);
                opState.setRepoShadow(null);
                return null;
            } else {
                // There are unexpired pending operations in the shadow. We cannot delete the shadow yet.
                // Therefore just mark shadow as dead.
                LOGGER.trace("Keeping dead {} because of pending operations or operation result", repoShadow);
                PrismObject<ShadowType> updatedShadow = markShadowTombstone(repoShadow, ctx.getTask(), result);
                opState.setRepoShadow(updatedShadow);
                return updatedShadow;
            }
        }
        LOGGER.trace("Recording pending delete operation in repository {}: {}", repoShadow, opState);
        ObjectDelta<ShadowType> requestDelta = repoShadow.createDeleteDelta();
        List<ItemDelta<?, ?>> internalShadowModifications = computeInternalShadowModifications(ctx, opState, requestDelta);
        creatorUpdaterHelper.addModifyMetadataDeltas(opState.getRepoShadow(), internalShadowModifications);

        if (repoShadow.asObjectable().getPrimaryIdentifierValue() != null) {
            // State goes to reaping or corpse or tombstone -> primaryIdentifierValue must be freed (if not done so yet)
            ItemDeltaCollectionsUtil.addNotEquivalent(
                    internalShadowModifications,
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                            .asItemDeltas());
        }

        LOGGER.trace("Updating repository {} after DELETE operation {}, {} repository shadow modifications",
                repoShadow, opState, internalShadowModifications.size());
        modifyShadowAttributes(ctx, repoShadow, internalShadowModifications, result);
        ObjectDeltaUtil.applyTo(repoShadow, internalShadowModifications);
        return repoShadow; // The shadow is obviously updated also in opState (it is the same Java object).
    }

    private List<ItemDelta<?, ?>> computeInternalShadowModifications(
            ProvisioningContext ctx,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            ObjectDelta<ShadowType> requestDelta)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        PrismObject<ShadowType> repoShadow = opState.getRepoShadow();
        List<ItemDelta<?, ?>> shadowModifications = new ArrayList<>();

        if (opState.hasPendingOperations()) {

            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            pendingOperationsHelper.collectPendingOperationUpdates(shadowModifications, opState, null, now);

        } else {

            if (!opState.isCompleted()) {

                PrismContainerDefinition<PendingOperationType> containerDefinition =
                        repoShadow.getDefinition().findContainerDefinition(ShadowType.F_PENDING_OPERATION);
                ContainerDelta<PendingOperationType> pendingOperationDelta =
                        containerDefinition.createEmptyDelta(ShadowType.F_PENDING_OPERATION);
                PendingOperationType pendingOperation =
                        pendingOperationsHelper.createPendingOperation(requestDelta, opState, null);
                pendingOperationDelta.addValuesToAdd(pendingOperation.asPrismContainerValue());
                shadowModifications.add(pendingOperationDelta);
                opState.addPendingOperation(pendingOperation);

            }
        }

        if (opState.isCompleted() && opState.isSuccess()) {
            if (requestDelta.isDelete()) {
                addDeadShadowDeltas(repoShadow, shadowModifications);
            } else {
                if (!ShadowUtil.isExists(repoShadow.asObjectable())) {
                    shadowModifications.add(
                            createShadowPropertyReplaceDelta(repoShadow, ShadowType.F_EXISTS, null));
                }
            }
        }

        // TODO: this is wrong. Provisioning should not change lifecycle states. Just for compatibility. MID-4833
        if (creatorUpdaterHelper.isUseProposedShadows(ctx)) {
            String currentLifecycleState = repoShadow.asObjectable().getLifecycleState();
            if (currentLifecycleState != null && !currentLifecycleState.equals(SchemaConstants.LIFECYCLE_ACTIVE)) {
                shadowModifications.add(
                        createShadowPropertyReplaceDelta(
                                repoShadow, ShadowType.F_LIFECYCLE_STATE, SchemaConstants.LIFECYCLE_ACTIVE));
            }
        }

        return shadowModifications;
    }

    void addDeadShadowDeltas(
            PrismObject<ShadowType> repoShadow, List<ItemDelta<?, ?>> shadowModifications)
            throws SchemaException {
        LOGGER.trace("Adding deltas that mark shadow {} as dead", repoShadow);
        if (ShadowUtil.isExists(repoShadow.asObjectable())) {
            shadowModifications.add(createShadowPropertyReplaceDelta(repoShadow, ShadowType.F_EXISTS, Boolean.FALSE));
        }
        if (!ShadowUtil.isDead(repoShadow.asObjectable())) {
            shadowModifications.add(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_DEAD).replace(true)
                            .asItemDelta());
        }
        if (repoShadow.asObjectable().getPrimaryIdentifierValue() != null) {
            // We need to free the identifier for further use by live shadows that may come later
            shadowModifications.add(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                            .asItemDelta());
        }
    }

    private <T> PropertyDelta<T> createShadowPropertyReplaceDelta(PrismObject<ShadowType> repoShadow, QName propName, T value) {
        PrismPropertyDefinition<T> def = repoShadow.getDefinition().findPropertyDefinition(ItemName.fromQName(propName));
        PropertyDelta<T> delta = def.createEmptyDelta(ItemPath.create(propName));
        if (value == null) {
            delta.setValueToReplace();
        } else {
            delta.setRealValuesToReplace(value);
        }
        return delta;
    }

    void modifyShadowAttributes(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
            Collection<? extends ItemDelta> modifications, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ConfigurationException {
        Collection<? extends ItemDelta<?, ?>> shadowChanges = extractRepoShadowChanges(ctx, shadow, modifications);
        executeShadowModification(ctx, shadow, shadowChanges, result);
    }

    private void executeShadowDeletion(PrismObject<ShadowType> repoShadow, Task task, OperationResult result) {
        try {
            LOGGER.trace("Deleting repository {}", repoShadow);
            repositoryService.deleteObject(ShadowType.class, repoShadow.getOid(), result);
            // Maybe we should issue death event even if the shadow was not found. But unless such previous deletion occurred
            // in raw mode by the administrator, we shouldn't care, because the thread that deleted the shadow should have
            // updated the links accordingly.
            eventDispatcher.notify(ShadowDeathEvent.deleted(repoShadow.getOid()), task, result);
        } catch (ObjectNotFoundException e) {
            result.muteLastSubresultError();
            LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't delete already deleted shadow {}, continuing", e, repoShadow);
        }
    }

    private void executeShadowModification(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
            Collection<? extends ItemDelta<?, ?>> modifications, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        if (!modifications.isEmpty()) {
            LOGGER.trace("Applying repository shadow modifications:\n{}", debugDumpLazily(modifications, 1));
            try {
                ConstraintsChecker.onShadowModifyOperation(modifications);
                repositoryService.modifyObject(ShadowType.class, shadow.getOid(), modifications, result);
                // Maybe we should catch ObjectNotFoundException here and issue death event. But unless such deletion occurred
                // in raw mode by the administrator, we shouldn't care, because the thread that deleted the shadow should have
                // updated the links accordingly.
                if (wasMarkedDead(shadow, modifications)) {
                    eventDispatcher.notify(ShadowDeathEvent.dead(shadow.getOid()), ctx.getTask(), result);
                }
                // This is important e.g. to update opState.repoShadow content in case of ADD operation success
                // - to pass newly-generated primary identifier to other parts of the code.
                ItemDeltaCollectionsUtil.applyTo(modifications, shadow);
                LOGGER.trace("Shadow changes processed successfully.");
            } catch (ObjectAlreadyExistsException ex) {
                throw new SystemException(ex);
            }
        }
    }

    private boolean wasMarkedDead(PrismObject<ShadowType> stateBefore, Collection<? extends ItemDelta<?, ?>> changes) {
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

    @SuppressWarnings("rawtypes")
    @NotNull
    private Collection<? extends ItemDelta<?, ?>> extractRepoShadowChanges(
            ProvisioningContext ctx, PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> objectChange)
            throws SchemaException, ConfigurationException {

        ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired(); // If type is not present, OC def is fine
        CachingStrategyType cachingStrategy = ctx.getCachingStrategy();
        ItemDelta<?, ?> attributeBasedNameChange = null;
        ItemDelta<?, ?> explicitNameChange = null;
        Collection<ItemDelta<?, ?>> repoChanges = new ArrayList<>();
        for (ItemDelta itemDelta : objectChange) {
            if (ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {
                QName attrName = itemDelta.getElementName();
                if (objectDefinition.isSecondaryIdentifier(attrName)
                        || (objectDefinition.getAllIdentifiers().size() == 1 && objectDefinition.isPrimaryIdentifier(attrName))) {
                    // Change of secondary identifier, or primary identifier when it is only one, means object rename. We also need to change $shadow/name
                    // TODO: change this to displayName attribute later
                    // TODO what if there are multiple secondary identifiers (like dn and samAccountName)?
                    String newName = null;
                    if (itemDelta.getValuesToReplace() != null && !itemDelta.getValuesToReplace().isEmpty()) {
                        newName = ((PrismPropertyValue) itemDelta.getValuesToReplace().iterator().next()).getValue().toString();
                    } else if (itemDelta.getValuesToAdd() != null && !itemDelta.getValuesToAdd().isEmpty()) {
                        newName = ((PrismPropertyValue) itemDelta.getValuesToAdd().iterator().next()).getValue().toString();
                    }
                    attributeBasedNameChange =
                            prismContext.deltaFactory().property()
                                    .createReplaceDelta(shadow.getDefinition(), ShadowType.F_NAME, new PolyString(newName));
                }
                if (objectDefinition.isPrimaryIdentifier(attrName)) {
                    // Change of primary identifier $shadow/primaryIdentifier.
                    String newPrimaryIdentifier = null;
                    if (itemDelta.getValuesToReplace() != null && !itemDelta.getValuesToReplace().isEmpty()) {
                        newPrimaryIdentifier = ((PrismPropertyValue) itemDelta.getValuesToReplace().iterator().next()).getValue().toString();
                    } else if (itemDelta.getValuesToAdd() != null && !itemDelta.getValuesToAdd().isEmpty()) {
                        newPrimaryIdentifier = ((PrismPropertyValue) itemDelta.getValuesToAdd().iterator().next()).getValue().toString();
                    }
                    ResourceAttribute<String> primaryIdentifier = helper.getPrimaryIdentifier(asObjectable(shadow));
                    //noinspection unchecked
                    ResourceAttributeDefinition<String> rDef =
                            (ResourceAttributeDefinition<String>) objectDefinition.findAttributeDefinitionRequired(
                                    primaryIdentifier.getElementName());
                    String normalizedNewPrimaryIdentifier = helper.getNormalizedAttributeValue(rDef, newPrimaryIdentifier);
                    PropertyDelta<String> primaryIdentifierDelta =
                            prismContext.deltaFactory().property()
                                    .createReplaceDelta(shadow.getDefinition(), ShadowType.F_PRIMARY_IDENTIFIER_VALUE,
                                            normalizedNewPrimaryIdentifier);
                    repoChanges.add(primaryIdentifierDelta);
                }
                if (!ProvisioningUtil.shouldStoreAttributeInShadow(objectDefinition, attrName, cachingStrategy)) {
                    continue;
                }
            } else if (ShadowType.F_ACTIVATION.equivalent(itemDelta.getParentPath())) {
                if (!ProvisioningUtil.shouldStoreActivationItemInShadow(itemDelta.getElementName(), cachingStrategy)) {
                    continue;
                }
            } else if (ShadowType.F_ACTIVATION.equivalent(itemDelta.getPath())) {// should not occur, but for completeness...
                if (((ContainerDelta<ActivationType>) itemDelta).getValuesToAdd() != null) {
                    for (PrismContainerValue<ActivationType> valueToAdd : ((ContainerDelta<ActivationType>) itemDelta).getValuesToAdd()) {
                        ProvisioningUtil.cleanupShadowActivation(valueToAdd.asContainerable());
                    }
                }
                if (((ContainerDelta<ActivationType>) itemDelta).getValuesToReplace() != null) {
                    for (PrismContainerValue<ActivationType> valueToReplace : ((ContainerDelta<ActivationType>) itemDelta).getValuesToReplace()) {
                        ProvisioningUtil.cleanupShadowActivation(valueToReplace.asContainerable());
                    }
                }
            } else if (SchemaConstants.PATH_PASSWORD.equivalent(itemDelta.getParentPath())) {
                addPasswordDelta(repoChanges, itemDelta, objectDefinition);
                continue;
            }
            helper.normalizeDelta(itemDelta, objectDefinition);
            if (isShadowNameDelta(itemDelta)) {
                explicitNameChange = itemDelta;
            } else {
                repoChanges.add(itemDelta);
            }
        }

        if (explicitNameChange != null) {
            repoChanges.add(explicitNameChange);
        } else if (attributeBasedNameChange != null) {
            repoChanges.add(attributeBasedNameChange);
        }

        return repoChanges;
    }

    private boolean isShadowNameDelta(ItemDelta<?, ?> itemDelta) {
        return itemDelta instanceof PropertyDelta<?>
                && ShadowType.F_NAME.equivalent(itemDelta.getPath());
    }

    private void addPasswordDelta(Collection<ItemDelta<?, ?>> repoChanges, ItemDelta<?, ?> requestedPasswordDelta,
            ResourceObjectDefinition objectDefinition) throws SchemaException {
        if (!(requestedPasswordDelta.getPath().equivalent(SchemaConstants.PATH_PASSWORD_VALUE))) {
            return;
        }
        CachingStrategyType cachingStrategy = ProvisioningUtil.getPasswordCachingStrategy(objectDefinition);
        if (cachingStrategy == null || cachingStrategy == CachingStrategyType.NONE) {
            return;
        }
        //noinspection unchecked
        PropertyDelta<ProtectedStringType> passwordValueDelta = (PropertyDelta<ProtectedStringType>) requestedPasswordDelta;
        hashValues(passwordValueDelta.getValuesToAdd());
        hashValues(passwordValueDelta.getValuesToReplace());
        repoChanges.add(requestedPasswordDelta);
    }

    private void hashValues(Collection<PrismPropertyValue<ProtectedStringType>> pvals) throws SchemaException {
        if (pvals == null) {
            return;
        }
        for (PrismPropertyValue<ProtectedStringType> pval : pvals) {
            ProtectedStringType psVal = pval.getValue();
            if (psVal == null) {
                return;
            }
            if (psVal.isHashed()) {
                return;
            }
            try {
                protector.hash(psVal);
            } catch (EncryptionException e) {
                throw new SchemaException("Cannot hash value", e);
            }
        }
    }

    PrismObject<ShadowType> markShadowTombstone(PrismObject<ShadowType> repoShadow, Task task, OperationResult result)
            throws SchemaException {
        if (repoShadow == null) {
            return null;
        }
        List<ItemDelta<?, ?>> shadowChanges = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_DEAD).replace(true)
                .item(ShadowType.F_EXISTS).replace(false)
                // We need to free the identifier for further use by live shadows that may come later
                .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                .asItemDeltas();
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
        eventDispatcher.notify(ShadowDeathEvent.dead(repoShadow.getOid()), task, result);
        ObjectDeltaUtil.applyTo(repoShadow, shadowChanges);
        repoShadow.asObjectable().setShadowLifecycleState(ShadowLifecycleStateType.TOMBSTONE);
        return repoShadow;
    }

    /** @return false if the shadow was not found. */
    boolean markShadowExists(ShadowType repoShadow, OperationResult parentResult) throws SchemaException {
        List<ItemDelta<?, ?>> shadowChanges = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_EXISTS).replace(true)
                .asItemDeltas();
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

    /**
     * Record results of an operation that have thrown exception.
     * This happens after the error handler is processed - and only for those
     * cases when the handler has re-thrown the exception.
     */
    void recordOperationException(
            ProvisioningContext ctx,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            ObjectDelta<ShadowType> delta,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        PrismObject<ShadowType> repoShadow = opState.getRepoShadow();
        if (repoShadow == null) {
            // Shadow does not exist. As this operation immediately ends up with an error then
            // we not even bother to create a shadow.
            return;
        }

        Collection<ItemDelta<?, ?>> shadowChanges = new ArrayList<>();

        if (opState.hasPendingOperations()) {
            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            pendingOperationsHelper.collectPendingOperationUpdates(shadowChanges, opState, OperationResultStatus.FATAL_ERROR, now);
        }

        if (delta.isAdd()) {
            // This means we have failed add operation here. We tried to add object,
            // but we have failed. Which means that this shadow is now dead.
            Duration deadRetentionPeriod = ProvisioningUtil.getDeadShadowRetentionPeriod(ctx);
            if (XmlTypeConverter.isZero(deadRetentionPeriod)) {
                // Do not bother with marking the shadow as dead. It should be gone immediately.
                // Deleting it now saves one modify operation.
                LOGGER.trace("Deleting repository shadow (after error handling)\n{}", debugDumpLazily(shadowChanges, 1));
                deleteShadow(opState.getRepoShadow(), ctx.getTask(), result);
                return;
            }

            shadowChanges.addAll(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_DEAD).replace(true)
                            // We need to free the identifier for further use by live shadows that may come later
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace()
                            .asItemDeltas()
            );
        }

        if (shadowChanges.isEmpty()) {
            return;
        }

        LOGGER.trace("Updating repository shadow (after error handling)\n{}", debugDumpLazily(shadowChanges, 1));

        executeShadowModification(ctx, opState.getRepoShadow(), shadowChanges, result);
    }

    <A extends AsynchronousOperationResult> void updatePendingOperations(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
            ProvisioningOperationState<A> opState, List<PendingOperationType> pendingExecutionOperations,
            XMLGregorianCalendar now, OperationResult result) throws ObjectNotFoundException, SchemaException {

        Collection<? extends ItemDelta<?, ?>> repoDeltas = new ArrayList<>();
        OperationResultStatusType resultStatus = opState.getResultStatusType();
        String asynchronousOperationReference = opState.getAsynchronousOperationReference();
        PendingOperationExecutionStatusType executionStatus = opState.getExecutionStatus();

        for (PendingOperationType existingPendingOperation : pendingExecutionOperations) {
            ItemPath containerPath = existingPendingOperation.asPrismContainerValue().getPath();
            addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_EXECUTION_STATUS, executionStatus, shadow.getDefinition());
            addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_RESULT_STATUS, resultStatus, shadow.getDefinition());
            addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_ASYNCHRONOUS_OPERATION_REFERENCE, asynchronousOperationReference, shadow.getDefinition());
            if (existingPendingOperation.getRequestTimestamp() == null) {
                // This is mostly failsafe. We do not want operations without timestamps. Those would be quite difficult to cleanup.
                // Therefore unprecise timestamp is better than no timestamp.
                addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_REQUEST_TIMESTAMP, now, shadow.getDefinition());
            }
            if (executionStatus == PendingOperationExecutionStatusType.COMPLETED && existingPendingOperation.getCompletionTimestamp() == null) {
                addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_COMPLETION_TIMESTAMP, now, shadow.getDefinition());
            }
            if (executionStatus == PendingOperationExecutionStatusType.EXECUTING && existingPendingOperation.getOperationStartTimestamp() == null) {
                addPropertyDelta(repoDeltas, containerPath, PendingOperationType.F_OPERATION_START_TIMESTAMP, now, shadow.getDefinition());
            }
        }

        LOGGER.trace("Updating pending operations in {}:\n{}\nbased on opstate: {}",
                shadow, debugDumpLazily(repoDeltas, 1), opState.shortDumpLazily());

        executeShadowModification(ctx, shadow, repoDeltas, result);
    }

    private <T> void addPropertyDelta(Collection repoDeltas, ItemPath containerPath, QName propertyName, T propertyValue, PrismObjectDefinition<ShadowType> shadowDef) {
        ItemPath propPath = containerPath.append(propertyName);
        PropertyDelta<T> delta;
        if (propertyValue == null) {
            delta = prismContext.deltaFactory().property().createModificationReplaceProperty(propPath, shadowDef /* no value */);
        } else {
            delta = prismContext.deltaFactory().property().createModificationReplaceProperty(propPath, shadowDef,
                    propertyValue);
        }
        repoDeltas.add(delta);
    }

    void refreshProvisioningIndexes(
            ProvisioningContext ctx, PrismObject<ShadowType> repoShadow, boolean resolveDuplicates, OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        ShadowType shadowType = repoShadow.asObjectable();
        String currentPrimaryIdentifierValue = shadowType.getPrimaryIdentifierValue();

        String expectedPrimaryIdentifierValue = helper.determinePrimaryIdentifierValue(ctx, shadowType);

        if (Objects.equals(currentPrimaryIdentifierValue, expectedPrimaryIdentifierValue)) {
            // Everything is all right
            return;
        }
        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace(expectedPrimaryIdentifierValue)
                .asItemDeltas();

        LOGGER.trace("Correcting primaryIdentifierValue for {}: {} -> {}", repoShadow, currentPrimaryIdentifierValue, expectedPrimaryIdentifierValue);
        try {

            repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), modifications, result);

        } catch (ObjectAlreadyExistsException e) {
            if (!resolveDuplicates) {
                throw e; // Client will take care of this
            }

            // Boom! We have some kind of inconsistency here. There is not much we can do to fix it. But let's try to find offending object.
            LOGGER.error("Error updating primaryIdentifierValue for " + repoShadow + " to value " + expectedPrimaryIdentifierValue + ": " + e.getMessage(), e);

            PrismObject<ShadowType> potentialConflictingShadow = shadowFinder.lookupShadowByIndexedPrimaryIdValue(ctx, expectedPrimaryIdentifierValue, result);
            LOGGER.debug("REPO CONFLICT: potential conflicting repo shadow (by primaryIdentifierValue)\n{}", potentialConflictingShadow == null ? null : potentialConflictingShadow.debugDump(1));
            String conflictingShadowPrimaryIdentifierValue =
                    helper.determinePrimaryIdentifierValue(ctx, asObjectable(potentialConflictingShadow));

            if (Objects.equals(conflictingShadowPrimaryIdentifierValue, potentialConflictingShadow.asObjectable().getPrimaryIdentifierValue())) {
                // Whoohoo, the conflicting shadow has good identifier. And it is the same as ours. We really have two conflicting shadows here.
                LOGGER.info("REPO CONFLICT: Found conflicting shadows that both claim the values of primaryIdentifierValue={}\nShadow with existing value:\n{}\nShadow that should have the same value:\n{}",
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
            try {
                repositoryService.modifyObject(ShadowType.class, potentialConflictingShadow.getOid(), resetModifications, result);
            } catch (ObjectAlreadyExistsException ee) {
                throw new SystemException("Attempt to reset primaryIdentifierValue on " + potentialConflictingShadow + " failed: " + ee.getMessage(), ee);
            }

            // Now we should be free to set up correct identifier. Finally.
            try {
                repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), modifications, result);
            } catch (ObjectAlreadyExistsException ee) {
                // Oh no! Not again!
                throw new SystemException("Despite all our best efforts, attempt to refresh primaryIdentifierValue on " + repoShadow + " failed: " + ee.getMessage(), ee);
            }
        }
        shadowType.setPrimaryIdentifierValue(expectedPrimaryIdentifierValue);
    }

    /**
     * Returns conflicting operation (pending delta) if there is any.
     * Updates the repo shadow in opState.
     *
     * BEWARE: updated repo shadow is raw. ApplyDefinitions must be called on it before any serious use.
     */
    private <A extends AsynchronousOperationResult> PendingOperationType checkAndRecordPendingOperationBeforeExecution(
            ProvisioningContext ctx, ObjectDelta<ShadowType> proposedDelta, @NotNull ProvisioningOperationState<A> opState,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        ResourceType resource = ctx.getResource();
        ResourceConsistencyType consistency = resource.getConsistency();

        boolean avoidDuplicateOperations;
        if (ctx.isInMaintenance()) {
            avoidDuplicateOperations = true; // in resource maintenance, always check for duplicates:
        } else if (consistency == null) {
            return null;
        } else {
            avoidDuplicateOperations = Boolean.TRUE.equals(consistency.isAvoidDuplicateOperations());
        }

        OptimisticLockingRunner<ShadowType, PendingOperationType> runner =
                new OptimisticLockingRunner.Builder<ShadowType, PendingOperationType>()
                        .object(opState.getRepoShadow())
                        .result(result)
                        .repositoryService(repositoryService)
                        .maxNumberOfAttempts(10)
                        .delayRange(20)
                        .build();

        try {

            return runner.run(
                    (object) -> {

                        // The runner itself could have updated the shadow (in case of precondition violation).
                        opState.setRepoShadow(runner.getObject());

                        if (avoidDuplicateOperations) {
                            PendingOperationType existingPendingOperation =
                                    pendingOperationsHelper.findExistingPendingOperation(object, proposedDelta, true);
                            if (existingPendingOperation != null) {
                                LOGGER.debug("Found duplicate operation for {} of {}: {}", proposedDelta.getChangeType(),
                                        object, existingPendingOperation);
                                return existingPendingOperation;
                            }
                        }

                        if (ResourceTypeUtil.getRecordPendingOperations(resource) != RecordPendingOperationsType.ALL) {
                            return null;
                        }

                        LOGGER.trace("Storing pending operation for {} of {}", proposedDelta.getChangeType(), object);
                        recordRequestedPendingOperationDelta(object, proposedDelta, opState, object.getVersion(), result);
                        LOGGER.trace("Stored pending operation for {} of {}", proposedDelta.getChangeType(), object);

                        // Yes, really return null. We are supposed to return conflicting operation (if found).
                        // But in this case there is no conflict. This operation does not conflict with itself.
                        return null;
                    }
            );

        } catch (ObjectAlreadyExistsException e) {
            // should not happen
            throw new SystemException(e);
        }
    }

    private <A extends AsynchronousOperationResult> void recordRequestedPendingOperationDelta(PrismObject<ShadowType> shadow,
            ObjectDelta<ShadowType> pendingDelta, @NotNull ProvisioningOperationState<A> opState, String readVersion,
            OperationResult result) throws SchemaException, ObjectNotFoundException, PreconditionViolationException {

        PendingOperationType pendingOperation = new PendingOperationType();
        pendingOperation.setDelta(DeltaConvertor.toObjectDeltaType(pendingDelta));
        pendingOperation.setRequestTimestamp(clock.currentTimeXMLGregorianCalendar());
        pendingOperation.setExecutionStatus(opState.getExecutionStatus());
        pendingOperation.setResultStatus(opState.getResultStatusType());
        pendingOperation.setAsynchronousOperationReference(opState.getAsynchronousOperationReference());

        var repoDeltas = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_PENDING_OPERATION).add(pendingOperation)
                .asItemDeltas();

        ModificationPrecondition<ShadowType> precondition =
                readVersion != null ? new VersionPrecondition<>(readVersion) : null;

        try {
            repositoryService.modifyObject(ShadowType.class, shadow.getOid(), repoDeltas, precondition, null, result);
        } catch (ObjectAlreadyExistsException e) {
            // should not happen
            throw new SystemException(e);
        }

        // We have to re-read shadow here. We need to get the pending operation in a form as it was stored.
        // We need id in the operation. Otherwise we won't be able to update it.
        PrismObject<ShadowType> updatedShadow = repositoryService.getObject(ShadowType.class, shadow.getOid(), null, result);
        PendingOperationType storedPendingOperation =
                Objects.requireNonNull(
                        pendingOperationsHelper.findExistingPendingOperation(updatedShadow, pendingDelta, true),
                        "Cannot find my own operation " + pendingOperation + " in " + updatedShadow);
        opState.addPendingOperation(storedPendingOperation);
        opState.setRepoShadow(updatedShadow);
    }

    /**
     * Returns conflicting operation (pending delta) if there is any.
     * The repo shadow in opState is updated.
     *
     * BEWARE: updated repo shadow is raw. ApplyDefinitions must be called on it before any serious use.
     */
    PendingOperationType checkAndRecordPendingDeleteOperationBeforeExecution(ProvisioningContext ctx,
            @NotNull ProvisioningOperationState<AsynchronousOperationResult> opState,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        ObjectDelta<ShadowType> proposedDelta = opState.getRepoShadow().createDeleteDelta();
        return checkAndRecordPendingOperationBeforeExecution(ctx, proposedDelta, opState, parentResult);
    }

    /**
     * Returns conflicting operation (pending delta) if there is any.
     * Updates the repo shadow in opState.
     *
     * BEWARE: updated repo shadow is raw. ApplyDefinitions must be called on it before any serious use.
     */
    PendingOperationType checkAndRecordPendingModifyOperationBeforeExecution(ProvisioningContext ctx,
            Collection<? extends ItemDelta> modifications,
            @NotNull ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectDelta<ShadowType> proposedDelta = createProposedModifyDelta(opState.getRepoShadow(), modifications);
        if (proposedDelta == null) {
            return null;
        }
        return checkAndRecordPendingOperationBeforeExecution(ctx, proposedDelta, opState, parentResult);
    }

    private ObjectDelta<ShadowType> createProposedModifyDelta(PrismObject<ShadowType> repoShadow, Collection<? extends ItemDelta> modifications) {
        Collection<ItemDelta> resourceModifications = new ArrayList<>(modifications.size());
        for (ItemDelta modification : modifications) {
            if (ProvisioningUtil.isResourceModification(modification)) {
                resourceModifications.add(modification);
            }
        }
        if (resourceModifications.isEmpty()) {
            return null;
        }
        return createModifyDelta(repoShadow, resourceModifications);
    }

    private ObjectDelta<ShadowType> createModifyDelta(PrismObject<ShadowType> repoShadow, Collection<? extends ItemDelta> modifications) {
        ObjectDelta<ShadowType> delta = repoShadow.createModifyDelta();
        delta.addModifications(ItemDeltaCollectionsUtil.cloneCollection(modifications));
        return delta;
    }

    @NotNull ShadowType updateShadow(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType currentResourceObject,
            @Nullable ObjectDelta<ShadowType> resourceObjectDelta,
            @NotNull ShadowType repoShadow,
            ShadowLifecycleStateType shadowState, // TODO ensure this is filled-in
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException {

        if (resourceObjectDelta == null) {
            repoShadow = retrieveIndexOnlyAttributesIfNeeded(ctx, repoShadow, result);
        } else {
            LOGGER.trace("Resource object delta is present. We assume we will be able to update the shadow without "
                    + "explicitly reading index-only attributes first."); // TODO check if this assumption is correct
        }

        ObjectDelta<ShadowType> computedShadowDelta =
                shadowDeltaComputer.computeShadowDelta(ctx, repoShadow, currentResourceObject, resourceObjectDelta, shadowState);

        if (!computedShadowDelta.isEmpty()) {
            LOGGER.trace("Updating repo shadow {} with delta:\n{}", repoShadow, computedShadowDelta.debugDumpLazily(1));
            executeShadowModification(ctx, repoShadow.asPrismObject(), computedShadowDelta.getModifications(), result);
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

    public void deleteShadow(PrismObject<ShadowType> oldRepoShadow, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        executeShadowDeletion(oldRepoShadow, task, result);
    }

    /**
     * Re-reads the shadow, re-evaluates the identifiers and stored values
     * (including their normalization under matching rules), updates them if necessary.
     *
     * Returns fixed shadow.
     */
    @NotNull PrismObject<ShadowType> fixShadow(
            @NotNull ProvisioningContext ctx,
            @NotNull PrismObject<ShadowType> origRepoShadow,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException {
        PrismObject<ShadowType> currentRepoShadow =
                repositoryService.getObject(ShadowType.class, origRepoShadow.getOid(), null, result);
        ProvisioningContext shadowCtx = ctx.spawnForShadow(currentRepoShadow.asObjectable());
        ResourceObjectDefinition oDef = shadowCtx.getObjectDefinitionRequired();
        PrismContainer<Containerable> attributesContainer = currentRepoShadow.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer != null) {
            ObjectDelta<ShadowType> shadowDelta = currentRepoShadow.createModifyDelta();
            for (Item<?, ?> item : attributesContainer.getValue().getItems()) {
                fixAttribute(oDef, item, shadowDelta);
            }
            if (!shadowDelta.isEmpty()) {
                LOGGER.trace("Fixing shadow {} with delta:\n{}", origRepoShadow, shadowDelta.debugDumpLazily());
                executeShadowModification(ctx, origRepoShadow, shadowDelta.getModifications(), result);
                shadowDelta.applyTo(currentRepoShadow);
            } else {
                LOGGER.trace("No need to fixing shadow {} (empty delta)", origRepoShadow);
            }
        } else {
            LOGGER.trace("No need to fixing shadow {} (no attributes)", origRepoShadow);
        }
        return currentRepoShadow;
    }

    private void fixAttribute(ResourceObjectDefinition oDef, Item<?, ?> item, ObjectDelta<ShadowType> shadowDelta)
            throws SchemaException {
        if (item instanceof PrismProperty<?>) {
            fixAttributeProperty(oDef, (PrismProperty<?>) item, shadowDelta);
        } else {
            LOGGER.trace("Ignoring non-property item in attribute container: {}", item);
        }
    }

    private <T> void fixAttributeProperty(
            ResourceObjectDefinition oDef,
            PrismProperty<T> attrProperty,
            ObjectDelta<ShadowType> shadowDelta) throws SchemaException {
        //noinspection unchecked
        ResourceAttributeDefinition<T> attrDef =
                (ResourceAttributeDefinition<T>) oDef.findAttributeDefinition(attrProperty.getElementName());

        if (attrDef != null) {
            normalizeAttribute(attrProperty, attrDef, shadowDelta);
        } else {
            deleteAttribute(attrProperty, shadowDelta);
        }
    }

    private <T> void normalizeAttribute(
            PrismProperty<T> attrProperty,
            ResourceAttributeDefinition<T> attrDef,
            ObjectDelta<ShadowType> shadowDelta) throws SchemaException {
        attrProperty.applyDefinition(attrDef);
        MatchingRule<T> matchingRule =
                matchingRuleRegistry.getMatchingRule(attrDef.getMatchingRuleQName(), attrDef.getTypeName());
        List<T> valuesToAdd = null;
        List<T> valuesToDelete = null;
        boolean anyChange = false;
        for (PrismPropertyValue<T> attrVal : attrProperty.getValues()) {
            T currentRealValue = attrVal.getValue();
            T normalizedRealValue = matchingRule.normalize(currentRealValue);
            if (!normalizedRealValue.equals(currentRealValue)) {
                if (attrDef.isSingleValue()) {
                    //noinspection unchecked
                    shadowDelta.addModificationReplaceProperty(attrProperty.getPath(), normalizedRealValue);
                    break;
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
            PropertyDelta<T> attrDelta = attrProperty.createDelta(attrProperty.getPath());
            attrDelta.addRealValuesToAdd(valuesToAdd);
            attrDelta.addRealValuesToDelete(valuesToDelete);
            shadowDelta.addModification(attrDelta);
        }
    }

    private <T> void deleteAttribute(PrismProperty<T> attrProperty, ObjectDelta<ShadowType> shadowDelta) {
        // No definition for this property, it should not be in the shadow
        PropertyDelta<?> oldRepoAttrPropDelta = attrProperty.createDelta();
        //noinspection unchecked,rawtypes
        oldRepoAttrPropDelta.addValuesToDelete(
                (Collection) PrismValueCollectionsUtil.cloneCollection(attrProperty.getValues()));
        shadowDelta.addModification(oldRepoAttrPropDelta);
    }
}

