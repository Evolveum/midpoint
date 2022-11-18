/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.shadows.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.AddOperationState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import static com.evolveum.midpoint.provisioning.impl.shadows.ShadowsNormalizationUtil.normalizeAttributes;
import static com.evolveum.midpoint.provisioning.impl.shadows.manager.PendingOperationsHelper.findPendingAddOperation;
import static com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil.determinePrimaryIdentifierValue;

/**
 * Creates shadows as needed. This is one of public classes of this package.
 */
@Component
public class ShadowCreator {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowCreator.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private Clock clock;
    @Autowired private Protector protector;
    @Autowired private PendingOperationsHelper pendingOperationsHelper;

    /**
     * Adds (without checking for existence) a shadow corresponding to a resource object that was discovered.
     * Used when searching for objects or when completing entitlements.
     */
    @NotNull
    public ShadowType addDiscoveredRepositoryShadow(
            ProvisioningContext ctx, ShadowType resourceObject, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectAlreadyExistsException, EncryptionException {
        LOGGER.trace("Adding new shadow from resource object:\n{}", resourceObject.debugDumpLazily(1));
        ShadowType repoShadow = createShadowForRepoStorage(ctx, resourceObject);
        ConstraintsChecker.onShadowAddOperation(repoShadow); // TODO eventually replace by repo cache invalidation
        String oid = repositoryService.addObject(repoShadow.asPrismObject(), null, result);
        repoShadow.setOid(oid);
        LOGGER.debug("Added new shadow (from resource object): {}", repoShadow);
        LOGGER.trace("Added new shadow (from resource object):\n{}", repoShadow.debugDumpLazily(1));
        return repoShadow;
    }

    /**
     * Adds new shadow in the `proposed` state (if proposed shadows processing is enabled).
     * The new shadow is recorded into the `opState`.
     */
    public void addNewProposedShadow(
            ProvisioningContext ctx, ShadowType shadowToAdd, AddOperationState opState, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectAlreadyExistsException, EncryptionException {

        if (!ctx.shouldUseProposedShadows()) {
            return;
        }

        ShadowType existingRepoShadow = opState.getRepoShadow();
        if (existingRepoShadow != null) {
            if (ctx.isPropagation()) {
                // In propagation we already have pending operation present in opState.
            } else {
                // The pending operation is most probably already in the shadow. Put it into opState to get it updated afterwards.
                PendingOperationType pendingAddOperation = findPendingAddOperation(existingRepoShadow);
                if (pendingAddOperation != null) {
                    opState.setCurrentPendingOperation(pendingAddOperation);
                }
            }
            return;
        }

        ShadowType newRepoShadow = createShadowForRepoStorage(ctx, shadowToAdd);
        assert newRepoShadow.getPendingOperation().isEmpty();

        opState.setExecutionStatus(PendingOperationExecutionStatusType.REQUESTED);
        pendingOperationsHelper.addPendingOperationIntoNewShadow(
                newRepoShadow, shadowToAdd, opState, ctx.getTask().getTaskIdentifier());

        ConstraintsChecker.onShadowAddOperation(newRepoShadow); // TODO migrate to cache invalidation process
        String oid = repositoryService.addObject(newRepoShadow.asPrismObject(), null, result);

        ShadowType shadowAfter;
        try {
            shadowAfter = repositoryService
                    .getObject(ShadowType.class, oid, null, result)
                    .asObjectable();
            ctx.applyAttributesDefinition(shadowAfter);
            opState.setRepoShadow(shadowAfter);
        } catch (ObjectNotFoundException e) {
            throw SystemException.unexpected(e, "when reading newly-created shadow back");
        }

        LOGGER.trace("Proposed shadow added to the repository (and read back): {}", shadowAfter);
        // We need the operation ID, hence the repo re-reading
        opState.setCurrentPendingOperation(
                MiscUtil.extractSingletonRequired(
                        shadowAfter.getPendingOperation(),
                        () -> new IllegalStateException("multiple pending operations"),
                        () -> new IllegalStateException("no pending operations")));
    }

    /**
     * Create a copy of a resource object (or another shadow) that is suitable for repository storage.
     *
     * @see ProvisioningUtil#shouldStoreAttributeInShadow(ResourceObjectDefinition, QName, CachingStrategyType)
     */
    @NotNull ShadowType createShadowForRepoStorage(ProvisioningContext ctx, ShadowType resourceObjectOrShadow)
            throws SchemaException, ConfigurationException, EncryptionException {

        ShadowType repoShadow = resourceObjectOrShadow.clone();
        repoShadow.setPrimaryIdentifierValue(
                determinePrimaryIdentifierValue(ctx, resourceObjectOrShadow));

        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(resourceObjectOrShadow);
        CachingStrategyType cachingStrategy = ctx.getCachingStrategy();
        if (cachingStrategy == CachingStrategyType.NONE) {
            ResourceAttributeContainer repoAttributesContainer = ShadowUtil.getAttributesContainer(repoShadow);
            // Clean all repoShadow attributes and add only those that should be there
            repoAttributesContainer.clear();
            for (PrismProperty<?> p : attributesContainer.getAllIdentifiers()) {
                repoAttributesContainer.add(p.clone());
            }

            // Also add all the attributes that act as association identifiers.
            // We will need them when the shadow is deleted (to remove the shadow from entitlements).
            // TODO is this behavior documented somewhere? Is it known well enough?
            ResourceObjectDefinition objectDefinition = ctx.getObjectDefinitionRequired();
            for (ResourceAssociationDefinition associationDef : objectDefinition.getAssociationDefinitions()) {
                if (associationDef.getDirection() == ResourceObjectAssociationDirectionType.OBJECT_TO_SUBJECT) {
                    QName valueAttributeName = associationDef.getDefinitionBean().getValueAttribute();
                    if (repoAttributesContainer.findAttribute(valueAttributeName) == null) {
                        ResourceAttribute<Object> valueAttribute = attributesContainer.findAttribute(valueAttributeName);
                        if (valueAttribute != null) {
                            repoAttributesContainer.add(valueAttribute.clone());
                        }
                    }
                }
            }

            repoShadow.setCachingMetadata(null);

            ProvisioningUtil.cleanupShadowActivation(repoShadow);

        } else if (cachingStrategy == CachingStrategyType.PASSIVE) {
            // Do not need to clear anything. Just store all attributes and add metadata.
            CachingMetadataType cachingMetadata = new CachingMetadataType();
            cachingMetadata.setRetrievalTimestamp(clock.currentTimeXMLGregorianCalendar());
            repoShadow.setCachingMetadata(cachingMetadata);

        } else {
            throw new ConfigurationException("Unknown caching strategy " + cachingStrategy);
        }

        // Store only password meta-data in repo - unless there is explicit caching
        CredentialsType credentials = repoShadow.getCredentials();
        if (credentials != null) {
            PasswordType password = credentials.getPassword();
            if (password != null) {
                preparePasswordForStorage(password, ctx);
                ObjectReferenceType owner = ctx.getTask().getOwnerRef();
                ProvisioningUtil.addPasswordMetadata(password, clock.currentTimeXMLGregorianCalendar(), owner);
            }
            // TODO: other credential types - later
        }

        // if shadow does not contain resource or resource reference, create it now
        if (repoShadow.getResourceRef() == null) {
            repoShadow.setResourceRef(ctx.getResourceRef());
        }

        if (repoShadow.getName() == null) {
            repoShadow.setName(
                    ShadowUtil.determineShadowNameRequired(resourceObjectOrShadow));
        }

        if (repoShadow.getObjectClass() == null) {
            repoShadow.setObjectClass(
                    attributesContainer.getDefinition().getTypeName());
        }

        if (repoShadow.isProtectedObject() != null) {
            repoShadow.setProtectedObject(null);
        }

        normalizeAttributes(repoShadow, ctx.getObjectDefinitionRequired());

        return repoShadow;
    }

    private void preparePasswordForStorage(PasswordType password, ProvisioningContext ctx)
            throws SchemaException, EncryptionException {
        ProtectedStringType passwordValue = password.getValue();
        if (passwordValue == null) {
            return;
        }
        CachingStrategyType cachingStrategy = ctx.getPasswordCachingStrategy();
        if (cachingStrategy != null && cachingStrategy != CachingStrategyType.NONE) {
            if (!passwordValue.isHashed()) {
                protector.hash(passwordValue);
            }
        } else {
            ProvisioningUtil.cleanupShadowPassword(password);
        }
    }
}
