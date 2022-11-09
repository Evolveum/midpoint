/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.shadows.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Creates shadows as needed.
 *
 * This is a result of preliminary split of {@link ShadowManager} functionality that was done in order
 * to make it more understandable. Most probably it is not good enough and should be improved.
 */
@Component
@Experimental
class ShadowCreator {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowManager.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private Clock clock;
    @Autowired private Protector protector;
    @Autowired private Helper helper;
    @Autowired private CreatorUpdaterHelper creatorUpdaterHelper;
    @Autowired private PendingOperationsHelper pendingOperationsHelper;

    @NotNull ShadowType addDiscoveredRepositoryShadow(
            ProvisioningContext ctx, ShadowType resourceObject, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectAlreadyExistsException, EncryptionException {
        LOGGER.trace("Adding new shadow from resource object:\n{}", resourceObject.debugDumpLazily(1));
        ShadowType repoShadow = createRepositoryShadow(ctx, resourceObject);
        ConstraintsChecker.onShadowAddOperation(repoShadow); // TODO eventually replace by repo cache invalidation
        String oid = repositoryService.addObject(repoShadow.asPrismObject(), null, result);
        repoShadow.setOid(oid);
        LOGGER.debug("Added new shadow (from resource object): {}", repoShadow);
        LOGGER.trace("Added new shadow (from resource object):\n{}", repoShadow.debugDumpLazily(1));
        return repoShadow;
    }

    void addNewProposedShadow(ProvisioningContext ctx, ShadowType shadowToAdd,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, ObjectAlreadyExistsException, EncryptionException {

        if (!creatorUpdaterHelper.isUseProposedShadows(ctx)) {
            return;
        }

        PrismObject<ShadowType> existingRepoShadow = opState.getRepoShadow();
        if (existingRepoShadow != null) {
            // TODO: should we add pending operation here?
            return;
        }

        // This is wrong: MID-4833
        ShadowType newRepoShadow = createRepositoryShadow(ctx, shadowToAdd);
        newRepoShadow.setLifecycleState(SchemaConstants.LIFECYCLE_PROPOSED);
        opState.setExecutionStatus(PendingOperationExecutionStatusType.REQUESTED);
        pendingOperationsHelper.addPendingOperationAdd(newRepoShadow, shadowToAdd, opState, task.getTaskIdentifier());

        ConstraintsChecker.onShadowAddOperation(newRepoShadow); // TODO migrate to cache invalidation process
        String oid = repositoryService.addObject(newRepoShadow.asPrismObject(), null, result);
        newRepoShadow.setOid(oid);
        LOGGER.trace("Proposed shadow added to the repository: {}", newRepoShadow);
        opState.setRepoShadow(newRepoShadow.asPrismObject());
    }

    /**
     * Create a copy of a resource object (or another shadow) that is suitable for repository storage.
     */
    @NotNull ShadowType createRepositoryShadow(ProvisioningContext ctx, ShadowType resourceObjectOrShadow)
            throws SchemaException, ConfigurationException, EncryptionException {

        ResourceAttributeContainer attributesContainer = ShadowUtil.getAttributesContainer(resourceObjectOrShadow);

        ShadowType repoShadow = resourceObjectOrShadow.clone();

        ResourceAttributeContainer repoAttributesContainer = ShadowUtil.getAttributesContainer(repoShadow);
        repoShadow.setPrimaryIdentifierValue(helper.determinePrimaryIdentifierValue(ctx, resourceObjectOrShadow));

        CachingStrategyType cachingStrategy = ctx.getCachingStrategy();
        if (cachingStrategy == CachingStrategyType.NONE) {
            // Clean all repoShadow attributes and add only those that should be there
            repoAttributesContainer.clear();
            Collection<ResourceAttribute<?>> primaryIdentifiers = attributesContainer.getPrimaryIdentifiers();
            for (PrismProperty<?> p : primaryIdentifiers) {
                repoAttributesContainer.add(p.clone());
            }

            Collection<ResourceAttribute<?>> secondaryIdentifiers = attributesContainer.getSecondaryIdentifiers();
            for (PrismProperty<?> p : secondaryIdentifiers) {
                repoAttributesContainer.add(p.clone());
            }

            // Also add all the attributes that act as association identifiers.
            // We will need them when the shadow is deleted (to remove the shadow from entitlements).
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

        helper.setKindIfNecessary(repoShadow, ctx);
//        setIntentIfNecessary(repoShadowType, objectClassDefinition);

        // Store only password meta-data in repo - unless there is explicit caching
        CredentialsType credentials = repoShadow.getCredentials();
        if (credentials != null) {
            PasswordType passwordType = credentials.getPassword();
            if (passwordType != null) {
                preparePasswordForStorage(passwordType, ctx);
                ObjectReferenceType owner = ctx.getTask().getOwnerRef();
                ProvisioningUtil.addPasswordMetadata(passwordType, clock.currentTimeXMLGregorianCalendar(), owner);
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
            repoShadow.setObjectClass(attributesContainer.getDefinition().getTypeName());
        }

        if (repoShadow.isProtectedObject() != null) {
            repoShadow.setProtectedObject(null);
        }

        helper.normalizeAttributes(repoShadow, ctx.getObjectDefinitionRequired());

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
