/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.prism.polystring.PolyString.toPolyStringType;

import java.util.List;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObject;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.AbstractShadow;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ShadowAuditHelper;
import com.evolveum.midpoint.provisioning.impl.shadows.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.AddOperationState;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
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

/**
 * Creates shadows as needed. This is one of public classes of this package.
 */
@Component
public class ShadowCreator {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowCreator.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private Clock clock;
    @Autowired private Protector protector;
    @Autowired private PendingOperationsHelper pendingOperationsHelper;
    @Autowired private ShadowAuditHelper shadowAuditHelper;
    @Autowired private RepoShadowFinder repoShadowFinder;

    /**
     * Adds (without checking for existence) a shadow corresponding to a resource object that was discovered.
     * Used when searching for objects or when completing entitlements.
     */
    public @NotNull RepoShadow addShadowForDiscoveredResourceObject(
            ProvisioningContext ctx, ExistingResourceObject resourceObject, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, ConfigurationException {
        LOGGER.trace("Adding new shadow from resource object:\n{}", resourceObject.debugDumpLazily(1));
        ShadowType repoShadow = createShadowForRepoStorage(ctx, resourceObject);
        ConstraintsChecker.onShadowAddOperation(repoShadow); // TODO eventually replace by repo cache invalidation
        String oid = repositoryService.addObject(repoShadow.asPrismObject(), null, result);
        repoShadow.setOid(oid);
        LOGGER.debug("Added new shadow (from resource object): {}", repoShadow);
        LOGGER.trace("Added new shadow (from resource object):\n{}", repoShadow.debugDumpLazily(1));

        shadowAuditHelper.auditEvent(AuditEventType.DISCOVER_OBJECT, repoShadow, ctx, result);

        return ctx.adoptRepoShadow(repoShadow);
    }

    /**
     * Adds new shadow in the `proposed` state (if proposed shadows processing is enabled).
     * The new shadow is recorded into the `opState`.
     */
    public void addNewProposedShadow(
            ProvisioningContext ctx, ResourceObject objectToAdd, AddOperationState opState, OperationResult result)
            throws SchemaException, ConfigurationException, ObjectAlreadyExistsException, EncryptionException {

        if (!ctx.shouldUseProposedShadows()) {
            return;
        }

        RepoShadow existingRepoShadow = opState.getRepoShadow();
        if (existingRepoShadow != null) {
            if (ctx.isPropagation()) {
                // In propagation we already have pending operation present in opState.
            } else {
                // The pending operation is most probably already in the shadow. Put it into opState to get it updated afterwards.
                PendingOperationType pendingAddOperation = existingRepoShadow.findPendingAddOperation();
                if (pendingAddOperation != null) {
                    opState.setCurrentPendingOperation(pendingAddOperation);
                }
            }
            return;
        }

        ShadowType newRepoShadow = createShadowForRepoStorage(ctx, objectToAdd);
        newRepoShadow.setExists(false);
        assert newRepoShadow.getPendingOperation().isEmpty();

        opState.setExecutionStatus(PendingOperationExecutionStatusType.REQUESTED);
        pendingOperationsHelper.addPendingOperationIntoNewShadow(
                newRepoShadow, objectToAdd.getBean(), opState, ctx.getTask().getTaskIdentifier());

        ConstraintsChecker.onShadowAddOperation(newRepoShadow); // TODO migrate to cache invalidation process
        String oid = repositoryService.addObject(newRepoShadow.asPrismObject(), null, result);

        RepoShadow shadowAfter;
        try {
            shadowAfter = repoShadowFinder.getRepoShadow(ctx, oid, result);
            opState.setRepoShadow(shadowAfter);
        } catch (ObjectNotFoundException e) {
            throw SystemException.unexpected(e, "when reading newly-created shadow back");
        }

        LOGGER.trace("Proposed shadow added to the repository (and read back): {}", shadowAfter);
        // We need the operation ID, hence the repo re-reading
        opState.setCurrentPendingOperation(
                MiscUtil.extractSingletonRequired(
                        shadowAfter.getBean().getPendingOperation(),
                        () -> new IllegalStateException("multiple pending operations"),
                        () -> new IllegalStateException("no pending operations")));
    }

    /**
     * Create a copy of a resource object (or another shadow) that is suitable for repository storage.
     */
    @NotNull ShadowType createShadowForRepoStorage(ProvisioningContext ctx, AbstractShadow resourceObjectOrShadow)
            throws SchemaException, EncryptionException {

        ShadowType abstractShadowBean = resourceObjectOrShadow.getBean().clone();

        // For resource objects, this information is obviously limited, as there are no pending operations known.
        // But the exists and dead flags can tell us something.
        var shadowLifecycleState = ctx.determineShadowState(abstractShadowBean);

        abstractShadowBean.setPrimaryIdentifierValue(
                resourceObjectOrShadow.determinePrimaryIdentifierValue(shadowLifecycleState));

        ResourceObjectDefinition objectDef = resourceObjectOrShadow.getObjectDefinition();

        ResourceAttributeContainer repoAttributesContainer = ShadowUtil.getAttributesContainer(abstractShadowBean);

        // We keep all the attributes that act as association identifiers.
        // We will need them when the shadow is deleted (to remove the shadow from entitlements).
        // TODO is this behavior documented somewhere? Is it known well enough?
        var associationValueAttributes = objectDef.getAssociationValueAttributes();

        for (ResourceAttribute<?> repoAttribute : List.copyOf(repoAttributesContainer.getAttributes())) {
            var attrDef = objectDef.findAttributeDefinitionRequired(repoAttribute.getElementName());
            if (ctx.shouldStoreAttributeInShadow(objectDef, attrDef, associationValueAttributes)) {
                var updatedOrNewAttribute = repoAttribute.forceDefinitionWithNormalization(attrDef);
                if (updatedOrNewAttribute != repoAttribute) {
                    repoAttributesContainer.remove(repoAttribute);
                    repoAttributesContainer.add(updatedOrNewAttribute);
                } else {
                    // the attribute was updated in place
                }
            } else {
                repoAttributesContainer.remove(repoAttribute);
            }
        }

        if (ctx.isCachingEnabled()) {
            CachingMetadataType cachingMetadata = new CachingMetadataType();
            cachingMetadata.setRetrievalTimestamp(clock.currentTimeXMLGregorianCalendar());
            abstractShadowBean.setCachingMetadata(cachingMetadata);
        } else {
            abstractShadowBean.setCachingMetadata(null);
            ProvisioningUtil.cleanupShadowActivation(abstractShadowBean); // TODO deal with this more precisely
        }

        // Store only password meta-data in repo - unless there is explicit caching
        CredentialsType credentials = abstractShadowBean.getCredentials();
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
        if (abstractShadowBean.getResourceRef() == null) {
            abstractShadowBean.setResourceRef(ctx.getResourceRef());
        }

        if (abstractShadowBean.getName() == null) {
            PolyString name = MiscUtil.requireNonNull(
                    resourceObjectOrShadow.determineShadowName(),
                    () -> "Cannot determine the shadow name for " + resourceObjectOrShadow);
            abstractShadowBean.setName(toPolyStringType(name));
        }

        if (abstractShadowBean.getObjectClass() == null) {
            abstractShadowBean.setObjectClass(
                    resourceObjectOrShadow.getObjectDefinition().getTypeName());
        }

        if (abstractShadowBean.isProtectedObject() != null) {
            abstractShadowBean.setProtectedObject(null);
        }

        if (abstractShadowBean.getEffectiveOperationPolicy() != null) {
            abstractShadowBean.setEffectiveOperationPolicy(null);
        }

        MetadataUtil.addCreationMetadata(abstractShadowBean);

        return abstractShadowBean;
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
