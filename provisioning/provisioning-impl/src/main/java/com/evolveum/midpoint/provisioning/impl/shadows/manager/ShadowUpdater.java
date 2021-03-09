/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
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
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.impl.shadows.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.ShadowState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.*;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;
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
    @Autowired private Helper helper;
    @Autowired private CreatorUpdaterHelper creatorUpdaterHelper;
    @Autowired private PendingOperationsHelper pendingOperationsHelper;

    private static final Trace LOGGER = TraceManager.getTrace(ShadowUpdater.class);

    /**
     * Record results of ADD operation to the shadow.
     */
    public void recordAddResult(ProvisioningContext ctx, PrismObject<ShadowType> shadowToAdd,
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
    private void recordAddResultNewShadow(ProvisioningContext ctx, PrismObject<ShadowType> resourceObjectToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            ObjectAlreadyExistsException, ExpressionEvaluationException, EncryptionException {

        // TODO: check for proposed Shadow. There may be a proposed shadow even if we do not have explicit proposed shadow OID
        //  (e.g. in case that the add operation failed). If proposed shadow is present do modify instead of add.

        PrismObject<ShadowType> resourceObject;
        if (opState.wasStarted() && opState.getAsyncResult().getReturnValue() != null) {
            resourceObject = opState.getAsyncResult().getReturnValue();
        } else {
            resourceObject = resourceObjectToAdd;
        }

        PrismObject<ShadowType> repoShadow = shadowCreator.createRepositoryShadow(ctx, resourceObject);
        opState.setRepoShadow(repoShadow);

        if (!opState.isCompleted()) {
            pendingOperationsHelper.addPendingOperationAdd(repoShadow, resourceObject, opState, null);
        }

        creatorUpdaterHelper.addCreateMetadata(repoShadow);

        LOGGER.trace("Adding repository shadow\n{}", repoShadow.debugDumpLazily(1));
        String oid;

        try {

            ConstraintsChecker.onShadowAddOperation(repoShadow.asObjectable()); // TODO migrate to repo cache invalidation
            oid = repositoryService.addObject(repoShadow, null, result);

        } catch (ObjectAlreadyExistsException ex) {
            // This should not happen. The OID is not supplied and it is generated by the repo.
            // If it happens, it must be a repo bug.
            result.recordFatalError(
                    "Couldn't add shadow object to the repository. Shadow object already exist. Reason: " + ex.getMessage(), ex);
            throw new ObjectAlreadyExistsException(
                    "Couldn't add shadow object to the repository. Shadow object already exist. Reason: " + ex.getMessage(), ex);
        }
        repoShadow.setOid(oid);
        opState.setRepoShadow(repoShadow);

        LOGGER.trace("Active shadow added to the repository: {}", repoShadow);

        result.recordSuccess();
    }

    private void recordAddResultExistingShadow(
            ProvisioningContext ctx,
            PrismObject<ShadowType> shadowToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException {

        final PrismObject<ShadowType> resourceShadow;
        if (opState.wasStarted() && opState.getAsyncResult().getReturnValue() != null) {
            resourceShadow = opState.getAsyncResult().getReturnValue();
        } else {
            resourceShadow = shadowToAdd;
        }

        PrismObject<ShadowType> repoShadow = opState.getRepoShadow();

        ObjectDelta<ShadowType> requestDelta = resourceShadow.createAddDelta();
        Collection<ItemDelta<?, ?>> internalShadowModifications = computeInternalShadowModifications(ctx, opState, requestDelta);
        computeUpdateShadowAttributeChanges(ctx, internalShadowModifications, resourceShadow, repoShadow);
        creatorUpdaterHelper.addModifyMetadataDeltas(repoShadow, internalShadowModifications);

        LOGGER.trace("Updating repository shadow\n{}", DebugUtil.debugDumpLazily(internalShadowModifications, 1));
        repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), internalShadowModifications, parentResult);
        LOGGER.trace("Repository shadow updated");

        parentResult.recordSuccess();
    }

    public void recordModifyResult(
            ProvisioningContext ctx,
            PrismObject<ShadowType> oldRepoShadow,
            Collection<? extends ItemDelta> requestedModifications,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            XMLGregorianCalendar now,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException, EncryptionException {

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

    private <T extends ObjectType> boolean shouldApplyModifyMetadata() {
        SystemConfigurationType config = provisioningService.getSystemConfiguration();
        InternalsConfigurationType internals = config != null ? config.getInternals() : null;
        return internals == null || internals.getShadowMetadataRecording() == null ||
                !Boolean.TRUE.equals(internals.getShadowMetadataRecording().isSkipOnModify());
    }

    private void computeUpdateShadowAttributeChanges(ProvisioningContext ctx, Collection<ItemDelta<?, ?>> repoShadowChanges,
            PrismObject<ShadowType> resourceShadow, PrismObject<ShadowType> repoShadow) throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {
        RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
        CachingStategyType cachingStrategy = ProvisioningUtil.getCachingStrategy(ctx);
        for (RefinedAttributeDefinition attrDef : objectClassDefinition.getAttributeDefinitions()) {
            if (ProvisioningUtil.shouldStoreAttributeInShadow(objectClassDefinition, attrDef.getItemName(), cachingStrategy)) {
                ResourceAttribute<Object> resourceAttr = ShadowUtil.getAttribute(resourceShadow, attrDef.getItemName());
                PrismProperty<Object> repoAttr = repoShadow.findProperty(ItemPath.create(ShadowType.F_ATTRIBUTES, attrDef.getItemName()));
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
        String existingPrimaryIdentifierValue = repoShadow.asObjectable().getPrimaryIdentifierValue();
        if (!Objects.equals(existingPrimaryIdentifierValue, newPrimaryIdentifierValue)) {
            repoShadowChanges.add(
                    prismContext.deltaFor(ShadowType.class)
                            .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace(newPrimaryIdentifierValue)
                            .asItemDelta()
            );
        }

        // TODO: reflect activation updates on cached shadow
    }

    PrismObject<ShadowType> recordDeleteResult(
            ProvisioningContext ctx,
            PrismObject<ShadowType> oldRepoShadow,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            ProvisioningOperationOptions options,
            XMLGregorianCalendar now,
            OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, EncryptionException {

        if (ProvisioningOperationOptions.isForce(options)) {
            LOGGER.trace("Deleting repository {} (force delete): {}", oldRepoShadow, opState);
            repositoryService.deleteObject(ShadowType.class, oldRepoShadow.getOid(), parentResult);
            return null;
        }

        if (!opState.hasPendingOperations() && opState.isCompleted()) {
            if (oldRepoShadow.asObjectable().getPendingOperation().isEmpty() && opState.isSuccess()) {
                LOGGER.trace("Deleting repository {}: {}", oldRepoShadow, opState);
                repositoryService.deleteObject(ShadowType.class, oldRepoShadow.getOid(), parentResult);
                return null;
            } else {
                // There are unexpired pending operations in the shadow. We cannot delete the shadow yet.
                // Therefore just mark shadow as dead.
                LOGGER.trace("Keeping dead {} because of pending operations or operation result", oldRepoShadow);
                return markShadowTombstone(oldRepoShadow, parentResult);
            }
        }
        LOGGER.trace("Recording pending delete operation in repository {}: {}", oldRepoShadow, opState);
        ObjectDelta<ShadowType> requestDelta = oldRepoShadow.createDeleteDelta();
        List<ItemDelta<?, ?>> internalShadowModifications = computeInternalShadowModifications(ctx, opState, requestDelta);
        creatorUpdaterHelper.addModifyMetadataDeltas(opState.getRepoShadow(), internalShadowModifications);

        LOGGER.trace("Updating repository {} after DELETE operation {}, {} repository shadow modifications", oldRepoShadow, opState, internalShadowModifications.size());
        modifyShadowAttributes(ctx, oldRepoShadow, internalShadowModifications, parentResult);
        ObjectDeltaUtil.applyTo(oldRepoShadow, internalShadowModifications);
        return oldRepoShadow;
    }

    private List<ItemDelta<?, ?>> computeInternalShadowModifications(ProvisioningContext ctx,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            ObjectDelta<ShadowType> requestDelta) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        PrismObject<ShadowType> repoShadow = opState.getRepoShadow();
        List<ItemDelta<?, ?>> shadowModifications = new ArrayList<>();

        if (opState.hasPendingOperations()) {

            XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
            pendingOperationsHelper.collectPendingOperationUpdates(shadowModifications, opState, null, now);

        } else {

            if (!opState.isCompleted()) {

                PrismContainerDefinition<PendingOperationType> containerDefinition = repoShadow.getDefinition().findContainerDefinition(ShadowType.F_PENDING_OPERATION);
                ContainerDelta<PendingOperationType> pendingOperationDelta = containerDefinition.createEmptyDelta(ShadowType.F_PENDING_OPERATION);
                PendingOperationType pendingOperation = pendingOperationsHelper.createPendingOperation(requestDelta, opState, null);
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
                    shadowModifications.add(createShadowPropertyReplaceDelta(repoShadow, ShadowType.F_EXISTS, null));
                }
            }
        }

        // TODO: this is wrong. Provisioning should not change lifecycle states. Just for compatibility. MID-4833
        if (creatorUpdaterHelper.isUseProposedShadows(ctx)) {
            String currentLifecycleState = repoShadow.asObjectable().getLifecycleState();
            if (currentLifecycleState != null && !currentLifecycleState.equals(SchemaConstants.LIFECYCLE_ACTIVE)) {
                shadowModifications.add(createShadowPropertyReplaceDelta(repoShadow, ShadowType.F_LIFECYCLE_STATE, SchemaConstants.LIFECYCLE_ACTIVE));
            }
        }

        return shadowModifications;
    }

    public void addDeadShadowDeltas(
            PrismObject<ShadowType> repoShadow, List<ItemDelta<?, ?>> shadowModifications)
            throws SchemaException {
        LOGGER.trace("Marking shadow {} as dead", repoShadow);
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

    public void modifyShadowAttributes(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> modifications,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
        Collection<? extends ItemDelta<?, ?>> shadowChanges = extractRepoShadowChanges(ctx, shadow, modifications);
        if (!shadowChanges.isEmpty()) {
            LOGGER.trace(
                    "There are repository shadow changes, applying modifications {}",
                    DebugUtil.debugDumpLazily(shadowChanges));
            try {
                ConstraintsChecker.onShadowModifyOperation(shadowChanges);
                repositoryService.modifyObject(ShadowType.class, shadow.getOid(), shadowChanges, parentResult);
                LOGGER.trace("Shadow changes processed successfully.");
            } catch (ObjectAlreadyExistsException ex) {
                throw new SystemException(ex);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    @NotNull
    private Collection<? extends ItemDelta<?, ?>> extractRepoShadowChanges(ProvisioningContext ctx, PrismObject<ShadowType> shadow, Collection<? extends ItemDelta> objectChange)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ExpressionEvaluationException {

        RefinedObjectClassDefinition objectClassDefinition = ctx.getObjectClassDefinition();
        CachingStategyType cachingStrategy = ProvisioningUtil.getCachingStrategy(ctx);
        Collection<ItemDelta<?, ?>> repoChanges = new ArrayList<>();
        for (ItemDelta itemDelta : objectChange) {
            if (ShadowType.F_ATTRIBUTES.equivalent(itemDelta.getParentPath())) {
                QName attrName = itemDelta.getElementName();
                if (objectClassDefinition.isSecondaryIdentifier(attrName)
                        || (objectClassDefinition.getAllIdentifiers().size() == 1 && objectClassDefinition.isPrimaryIdentifier(attrName))) {
                    // Change of secondary identifier, or primary identifier when it is only one, means object rename. We also need to change $shadow/name
                    // TODO: change this to displayName attribute later
                    String newName = null;
                    if (itemDelta.getValuesToReplace() != null && !itemDelta.getValuesToReplace().isEmpty()) {
                        newName = ((PrismPropertyValue) itemDelta.getValuesToReplace().iterator().next()).getValue().toString();
                    } else if (itemDelta.getValuesToAdd() != null && !itemDelta.getValuesToAdd().isEmpty()) {
                        newName = ((PrismPropertyValue) itemDelta.getValuesToAdd().iterator().next()).getValue().toString();
                    }
                    PropertyDelta<PolyString> nameDelta = prismContext.deltaFactory().property().createReplaceDelta(shadow.getDefinition(), ShadowType.F_NAME, new PolyString(newName));
                    repoChanges.add(nameDelta);
                }
                if (objectClassDefinition.isPrimaryIdentifier(attrName)) {
                    // Change of primary identifier $shadow/primaryIdentifier.
                    String newPrimaryIdentifier = null;
                    if (itemDelta.getValuesToReplace() != null && !itemDelta.getValuesToReplace().isEmpty()) {
                        newPrimaryIdentifier = ((PrismPropertyValue) itemDelta.getValuesToReplace().iterator().next()).getValue().toString();
                    } else if (itemDelta.getValuesToAdd() != null && !itemDelta.getValuesToAdd().isEmpty()) {
                        newPrimaryIdentifier = ((PrismPropertyValue) itemDelta.getValuesToAdd().iterator().next()).getValue().toString();
                    }
                    ResourceAttribute<String> primaryIdentifier = helper.getPrimaryIdentifier(shadow);
                    RefinedAttributeDefinition<String> rDef;
                    try {
                        rDef = ctx.getObjectClassDefinition().findAttributeDefinition(primaryIdentifier.getElementName());
                    } catch (ConfigurationException | ObjectNotFoundException | CommunicationException
                            | ExpressionEvaluationException e) {
                        // Should not happen at this stage. And we do not want to pollute throws clauses all the way up.
                        throw new SystemException(e.getMessage(), e);
                    }
                    String normalizedNewPrimaryIdentifier = helper.getNormalizedAttributeValue(rDef, newPrimaryIdentifier);
                    PropertyDelta<String> primaryIdentifierDelta = prismContext.deltaFactory().property().createReplaceDelta(shadow.getDefinition(),
                            ShadowType.F_PRIMARY_IDENTIFIER_VALUE, normalizedNewPrimaryIdentifier);
                    repoChanges.add(primaryIdentifierDelta);
                }
                if (!ProvisioningUtil.shouldStoreAttributeInShadow(objectClassDefinition, attrName, cachingStrategy)) {
                    continue;
                }
            } else if (ShadowType.F_ACTIVATION.equivalent(itemDelta.getParentPath())) {
                if (!ProvisioningUtil.shouldStoreActivationItemInShadow(itemDelta.getElementName(), cachingStrategy)) {
                    continue;
                }
            } else if (ShadowType.F_ACTIVATION.equivalent(itemDelta.getPath())) {        // should not occur, but for completeness...
                for (PrismContainerValue<ActivationType> valueToAdd : ((ContainerDelta<ActivationType>) itemDelta).getValuesToAdd()) {
                    ProvisioningUtil.cleanupShadowActivation(valueToAdd.asContainerable());
                }
                for (PrismContainerValue<ActivationType> valueToReplace : ((ContainerDelta<ActivationType>) itemDelta).getValuesToReplace()) {
                    ProvisioningUtil.cleanupShadowActivation(valueToReplace.asContainerable());
                }
            } else if (SchemaConstants.PATH_PASSWORD.equivalent(itemDelta.getParentPath())) {
                addPasswordDelta(repoChanges, itemDelta, objectClassDefinition);
                continue;
            }
            helper.normalizeDelta(itemDelta, objectClassDefinition);
            repoChanges.add(itemDelta);
        }

        return repoChanges;
    }

    private void addPasswordDelta(Collection<ItemDelta<?, ?>> repoChanges, ItemDelta<?, ?> requestedPasswordDelta,
            RefinedObjectClassDefinition objectClassDefinition) throws SchemaException {
        if (!(requestedPasswordDelta.getPath().equivalent(SchemaConstants.PATH_PASSWORD_VALUE))) {
            return;
        }
        CachingStategyType cachingStrategy = ProvisioningUtil.getPasswordCachingStrategy(objectClassDefinition);
        if (cachingStrategy == null || cachingStrategy == CachingStategyType.NONE) {
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

    PrismObject<ShadowType> markShadowTombstone(PrismObject<ShadowType> repoShadow, OperationResult parentResult) throws SchemaException {
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
            repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), shadowChanges, parentResult);
        } catch (ObjectAlreadyExistsException e) {
            // Should not happen, this is not a rename
            throw new SystemException(e.getMessage(), e);
        } catch (ObjectNotFoundException e) {
            // Cannot be more dead
            LOGGER.trace("Attempt to mark shadow {} as tombstone found that no such shadow exists", repoShadow);
            return null;
        }
        ObjectDeltaUtil.applyTo(repoShadow, shadowChanges);
        return repoShadow;
    }


    /** @return true if the shadow was found and marked as existing */
    public boolean markShadowExists(PrismObject<ShadowType> repoShadow, OperationResult parentResult) throws SchemaException {
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
            // Cannot be more dead
            LOGGER.trace("Attempt to mark shadow {} as existent found that no such shadow exists", repoShadow);
            return false;
        }
        ObjectDeltaUtil.applyTo(repoShadow, shadowChanges);
        return true;
    }


    /**
     * Record results of an operation that have thrown exception.
     * This happens after the error handler is processed - and only for those
     * cases when the handler has re-thrown the exception.
     */
    public void recordOperationException(
            ProvisioningContext ctx,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            ObjectDelta<ShadowType> delta,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
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
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Deleting repository shadow (after error handling)\n{}", DebugUtil.debugDump(shadowChanges, 1));
                }
                repositoryService.deleteObject(ShadowType.class, opState.getRepoShadow().getOid(), parentResult);
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

        LOGGER.trace("Updating repository shadow (after error handling)\n{}", DebugUtil.debugDumpLazily(shadowChanges, 1));

        repositoryService.modifyObject(ShadowType.class, opState.getRepoShadow().getOid(), shadowChanges, parentResult);
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
                shadow, DebugUtil.debugDumpLazily(repoDeltas, 1), opState.shortDumpLazily());

        try {
            repositoryService.modifyObject(ShadowType.class, shadow.getOid(), repoDeltas, result);
        } catch (ObjectAlreadyExistsException ex) {
            throw new SystemException(ex);
        }
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

    public PrismObject<ShadowType> refreshProvisioningIndexes(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException {
        ShadowType shadowType = repoShadow.asObjectable();
        String currentPrimaryIdentifierValue = shadowType.getPrimaryIdentifierValue();

        String expectedPrimaryIdentifierValue = helper.determinePrimaryIdentifierValue(ctx, repoShadow);

        if (Objects.equals(currentPrimaryIdentifierValue, expectedPrimaryIdentifierValue)) {
            // Everything is all right
            return repoShadow;
        }
        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_PRIMARY_IDENTIFIER_VALUE).replace(expectedPrimaryIdentifierValue)
                .asItemDeltas();

        LOGGER.trace("Correcting primaryIdentifierValue for {}: {} -> {}", repoShadow, currentPrimaryIdentifierValue, expectedPrimaryIdentifierValue);
        try {

            repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), modifications, parentResult);

        } catch (ObjectAlreadyExistsException e) {
            // Boom! We have some kind of inconsistency here. There is not much we can do to fix it. But let's try to find offending object.
            LOGGER.error("Error updating primaryIdentifierValue for " + repoShadow + " to value " + expectedPrimaryIdentifierValue + ": " + e.getMessage(), e);

            PrismObject<ShadowType> potentialConflictingShadow = shadowFinder.lookupShadowByIndexedPrimaryIdValue(ctx, expectedPrimaryIdentifierValue, parentResult);
            LOGGER.debug("REPO CONFLICT: potential conflicting repo shadow (by primaryIdentifierValue)\n{}", potentialConflictingShadow == null ? null : potentialConflictingShadow.debugDump(1));
            String conflictingShadowPrimaryIdentifierValue = helper.determinePrimaryIdentifierValue(ctx, potentialConflictingShadow);

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
                repositoryService.modifyObject(ShadowType.class, potentialConflictingShadow.getOid(), modifications, parentResult);
            } catch (ObjectAlreadyExistsException ee) {
                throw new SystemException("Attempt to reset primaryIdentifierValue on " + potentialConflictingShadow + " failed: " + ee.getMessage(), ee);
            }

            // Now we should be free to set up correct identifier. Finally.
            try {
                repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), modifications, parentResult);
            } catch (ObjectAlreadyExistsException ee) {
                // Oh no! Not again!
                throw new SystemException("Despite all our best efforts, attempt to refresh primaryIdentifierValue on " + repoShadow + " failed: " + ee.getMessage(), ee);
            }
        }
        shadowType.setPrimaryIdentifierValue(expectedPrimaryIdentifierValue);
        return repoShadow;
    }

    // returns conflicting operation (pending delta) if there is any
    private <A extends AsynchronousOperationResult> PendingOperationType checkAndRecordPendingOperationBeforeExecution(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow, ObjectDelta<ShadowType> proposedDelta,
            ProvisioningOperationState<A> opState,
            Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ResourceType resource = ctx.getResource();
        ResourceConsistencyType consistencyType = resource.getConsistency();

        boolean isInMaintenance = ResourceTypeUtil.isInMaintenance(ctx.getResource());

        Boolean avoidDuplicateOperations;
        if (isInMaintenance) {
            avoidDuplicateOperations = Boolean.TRUE; // in resource maintenance, always check for duplicates:
        } else if (consistencyType == null) {
            return null;
        } else {
            avoidDuplicateOperations = consistencyType.isAvoidDuplicateOperations();
        }

        OptimisticLockingRunner<ShadowType, PendingOperationType> runner = new OptimisticLockingRunner.Builder<ShadowType, PendingOperationType>()
                .object(repoShadow)
                .result(parentResult)
                .repositoryService(repositoryService)
                .maxNumberOfAttempts(10)
                .delayRange(20)
                .build();

        try {

            return runner.run(
                    (object) -> {
                        if (BooleanUtils.isTrue(avoidDuplicateOperations)) {
                            PendingOperationType existingPendingOperation = pendingOperationsHelper.findExistingPendingOperation(object, proposedDelta, true);
                            if (existingPendingOperation != null) {
                                LOGGER.debug("Found duplicate operation for {} of {}: {}", proposedDelta.getChangeType(), object, existingPendingOperation);
                                return existingPendingOperation;
                            }
                        }

                        if (ResourceTypeUtil.getRecordPendingOperations(resource) != RecordPendingOperationsType.ALL) {
                            return null;
                        }

                        LOGGER.trace("Storing pending operation for {} of {}", proposedDelta.getChangeType(), object);
                        recordRequestedPendingOperationDelta(ctx, object, proposedDelta, opState, object.getVersion(), parentResult);
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

    private <A extends AsynchronousOperationResult> void recordRequestedPendingOperationDelta(ProvisioningContext ctx, PrismObject<ShadowType> shadow,
            ObjectDelta<ShadowType> pendingDelta, ProvisioningOperationState<A> opState, String readVersion,
            OperationResult parentResult) throws SchemaException, ObjectNotFoundException, PreconditionViolationException {
        ObjectDeltaType pendingDeltaType = DeltaConvertor.toObjectDeltaType(pendingDelta);

        PendingOperationType pendingOperation = new PendingOperationType();
        pendingOperation.setDelta(pendingDeltaType);
        pendingOperation.setRequestTimestamp(clock.currentTimeXMLGregorianCalendar());
        if (opState != null) {
            pendingOperation.setExecutionStatus(opState.getExecutionStatus());
            pendingOperation.setResultStatus(opState.getResultStatusType());
            pendingOperation.setAsynchronousOperationReference(opState.getAsynchronousOperationReference());
        }

        Collection repoDeltas = new ArrayList<>(1);
        ContainerDelta<PendingOperationType> cdelta = prismContext.deltaFactory().container().createDelta(ShadowType.F_PENDING_OPERATION, shadow.getDefinition());
        cdelta.addValuesToAdd(pendingOperation.asPrismContainerValue());
        repoDeltas.add(cdelta);

        ModificationPrecondition<ShadowType> precondition = null;

        if (readVersion != null) {
            precondition = new VersionPrecondition<>(readVersion);
        }

        try {
            repositoryService.modifyObject(ShadowType.class, shadow.getOid(), repoDeltas, precondition, null, parentResult);
        } catch (ObjectAlreadyExistsException e) {
            // should not happen
            throw new SystemException(e);
        }

        // We have re-read shadow here. We need to get the pending operation in a form as it was stored. We need id in the operation.
        // Otherwise we won't be able to update it.
        PrismObject<ShadowType> newShadow = repositoryService.getObject(ShadowType.class, shadow.getOid(), null, parentResult);
        PendingOperationType storedPendingOperation = pendingOperationsHelper.findExistingPendingOperation(newShadow, pendingDelta, true);
        if (storedPendingOperation == null) {
            // cannot find my own operation?
            throw new IllegalStateException("Cannot find my own operation " + pendingOperation + " in " + newShadow);
        }
        opState.addPendingOperation(storedPendingOperation);
    }

    // returns conflicting operation (pending delta) if there is any
    PendingOperationType checkAndRecordPendingDeleteOperationBeforeExecution(ProvisioningContext ctx,
            PrismObject<ShadowType> shadow,
            ProvisioningOperationState<AsynchronousOperationResult> opState,
            Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ObjectDelta<ShadowType> proposedDelta = shadow.createDeleteDelta();
        return checkAndRecordPendingOperationBeforeExecution(ctx, shadow, proposedDelta, opState, task, parentResult);
    }

    PendingOperationType checkAndRecordPendingModifyOperationBeforeExecution(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow,
            Collection<? extends ItemDelta> modifications,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            Task task, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectDelta<ShadowType> proposedDelta = createProposedModifyDelta(repoShadow, modifications);
        if (proposedDelta == null) {
            return null;
        }
        return checkAndRecordPendingOperationBeforeExecution(ctx, repoShadow, proposedDelta, opState, task, parentResult);
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

    public PrismObject<ShadowType> updateShadow(@NotNull ProvisioningContext ctx,
            @NotNull PrismObject<ShadowType> currentResourceObject, ObjectDelta<ShadowType> resourceObjectDelta,
            @NotNull PrismObject<ShadowType> repoShadow, ShadowState shadowState, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException,
            ExpressionEvaluationException {

        ObjectDelta<ShadowType> computedShadowDelta = shadowDeltaComputer.computeShadowDelta(ctx, repoShadow, currentResourceObject,
                resourceObjectDelta, shadowState);

        if (!computedShadowDelta.isEmpty()) {
            LOGGER.trace("Updating repo shadow {} with delta:\n{}", repoShadow, computedShadowDelta.debugDumpLazily(1));
            ConstraintsChecker.onShadowModifyOperation(computedShadowDelta.getModifications());
            try {
                repositoryService.modifyObject(ShadowType.class, repoShadow.getOid(), computedShadowDelta.getModifications(), parentResult);
            } catch (ObjectAlreadyExistsException e) {
                throw new SystemException(e.getMessage(), e); // This should not happen for shadows
            }
            PrismObject<ShadowType> updatedShadow = repoShadow.clone();
            computedShadowDelta.applyTo(updatedShadow);
            return updatedShadow;
        } else {
            LOGGER.trace("No need to update repo shadow {} (empty delta)", repoShadow);
            return repoShadow;
        }
    }

    public void deleteShadow(ProvisioningContext ctx, PrismObject<ShadowType> oldRepoShadow, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        LOGGER.trace("Deleting repository {}", oldRepoShadow);
        try {
            repositoryService.deleteObject(ShadowType.class, oldRepoShadow.getOid(), parentResult);
        } catch (ObjectNotFoundException e) {
            // Attempt to delete shadow that is already deleted. No big deal.
            parentResult.muteLastSubresultError();
            LOGGER.trace("Attempt to delete repository {} that is already deleted. Ignoring error.", oldRepoShadow);
        }
    }

    @NotNull PrismObject<ShadowType> fixShadow(ProvisioningContext ctx, PrismObject<ShadowType> origRepoShadow,
            OperationResult parentResult) throws ObjectNotFoundException, SchemaException, ConfigurationException, CommunicationException, ExpressionEvaluationException {
        PrismObject<ShadowType> currentRepoShadow = repositoryService.getObject(ShadowType.class, origRepoShadow.getOid(), null, parentResult);
        ProvisioningContext shadowCtx = ctx.spawn(currentRepoShadow);
        RefinedObjectClassDefinition ocDef = shadowCtx.getObjectClassDefinition();
        PrismContainer<Containerable> attributesContainer = currentRepoShadow.findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer != null) {
            ObjectDelta<ShadowType> shadowDelta = currentRepoShadow.createModifyDelta();
            for (Item<?, ?> item : attributesContainer.getValue().getItems()) {
                if (item instanceof PrismProperty<?>) {
                    PrismProperty<Object> attrProperty = (PrismProperty<Object>) item;
                    RefinedAttributeDefinition<Object> attrDef = ocDef.findAttributeDefinition(attrProperty.getElementName());
                    if (attrDef == null) {
                        // No definition for this property, it should not be in the shadow
                        PropertyDelta<?> oldRepoAttrPropDelta = attrProperty.createDelta();
                        oldRepoAttrPropDelta.addValuesToDelete((Collection) PrismValueCollectionsUtil.cloneCollection(attrProperty.getValues()));
                        shadowDelta.addModification(oldRepoAttrPropDelta);
                    } else {
                        attrProperty.applyDefinition(attrDef);
                        MatchingRule matchingRule = matchingRuleRegistry.getMatchingRule(attrDef.getMatchingRuleQName(), attrDef.getTypeName());
                        List<Object> valuesToAdd = null;
                        List<Object> valuesToDelete = null;
                        for (PrismPropertyValue attrVal : attrProperty.getValues()) {
                            Object currentRealValue = attrVal.getValue();
                            Object normalizedRealValue = matchingRule.normalize(currentRealValue);
                            if (!normalizedRealValue.equals(currentRealValue)) {
                                if (attrDef.isSingleValue()) {
                                    shadowDelta.addModificationReplaceProperty(attrProperty.getPath(), normalizedRealValue);
                                    break;
                                } else {
                                    if (valuesToAdd == null) {
                                        valuesToAdd = new ArrayList<>();
                                    }
                                    valuesToAdd.add(normalizedRealValue);
                                    if (valuesToDelete == null) {
                                        valuesToDelete = new ArrayList<>();
                                    }
                                    valuesToDelete.add(currentRealValue);
                                }
                            }
                        }
                        PropertyDelta<Object> attrDelta = attrProperty.createDelta(attrProperty.getPath());
                        if (valuesToAdd != null) {
                            attrDelta.addRealValuesToAdd(valuesToAdd);
                        }
                        if (valuesToDelete != null) {
                            attrDelta.addRealValuesToDelete(valuesToDelete);
                        }
                        shadowDelta.addModification(attrDelta);
                    }
                }
            }
            if (!shadowDelta.isEmpty()) {
                LOGGER.trace("Fixing shadow {} with delta:\n{}", origRepoShadow, shadowDelta.debugDumpLazily());
                try {
                    repositoryService.modifyObject(ShadowType.class, origRepoShadow.getOid(), shadowDelta.getModifications(), parentResult);
                } catch (ObjectAlreadyExistsException e) {
                    // This should not happen for shadows
                    throw new SystemException(e.getMessage(), e);
                }
                shadowDelta.applyTo(currentRepoShadow);
            } else {
                LOGGER.trace("No need to fixing shadow {} (empty delta)", origRepoShadow);
            }
        } else {
            LOGGER.trace("No need to fixing shadow {} (no attributes)", origRepoShadow);
        }
        return currentRepoShadow;
    }

}

