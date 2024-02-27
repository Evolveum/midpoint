/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import static com.evolveum.midpoint.prism.polystring.PolyString.toPolyStringType;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
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
import com.evolveum.midpoint.schema.util.AbstractShadow;
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
    @Autowired private ShadowFinder shadowFinder;

    /**
     * Adds (without checking for existence) a shadow corresponding to a resource object that was discovered.
     * Used when searching for objects or when completing entitlements.
     */
    public @NotNull RepoShadow addShadowForDiscoveredResourceObject(
            ProvisioningContext ctx, ExistingResourceObject resourceObject, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, ConfigurationException {

        LOGGER.trace("Adding new shadow from resource object:\n{}", resourceObject.debugDumpLazily(1));

        ShadowType repoShadowBean = createShadowForRepoStorage(ctx, resourceObject).getBean();
        LOGGER.trace("Shadow to add (from resource object):\n{}", repoShadowBean.debugDumpLazily(1));

        ConstraintsChecker.onShadowAddOperation(repoShadowBean); // TODO eventually replace by repo cache invalidation
        String oid = repositoryService.addObject(repoShadowBean.asPrismObject(), null, result);

        repoShadowBean.setOid(oid);
        LOGGER.debug("Added new shadow (from resource object): {}", repoShadowBean); // showing OID but not the content

        shadowAuditHelper.auditEvent(AuditEventType.DISCOVER_OBJECT, repoShadowBean, ctx, result);

        return ctx.adoptRawRepoShadow(repoShadowBean);
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

        ShadowType newRawRepoShadow = createShadowForRepoStorage(ctx, objectToAdd).getBean();
        newRawRepoShadow.setExists(false);
        assert newRawRepoShadow.getPendingOperation().isEmpty();

        opState.setExecutionStatus(PendingOperationExecutionStatusType.REQUESTED);
        pendingOperationsHelper.addPendingOperationIntoNewShadow(
                newRawRepoShadow, objectToAdd.getBean(), opState, ctx.getTask().getTaskIdentifier());

        ConstraintsChecker.onShadowAddOperation(newRawRepoShadow); // TODO migrate to cache invalidation process
        String oid = repositoryService.addObject(newRawRepoShadow.asPrismObject(), null, result);

        RepoShadow shadowAfter;
        try {
            shadowAfter = shadowFinder.getRepoShadow(ctx, oid, result);
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
     *
     * @see ShadowDeltaComputerAbsolute
     */
    @NotNull RawRepoShadow createShadowForRepoStorage(ProvisioningContext ctx, AbstractShadow resourceObjectOrShadow)
            throws SchemaException, EncryptionException {

        resourceObjectOrShadow.checkConsistence();

        ResourceObjectDefinition objectDef = resourceObjectOrShadow.getObjectDefinition();
        ResourceAttributeContainer originalAttributesContainer = resourceObjectOrShadow.getAttributesContainer();

        // An alternative would be to start with a clean shadow and fill-in the data from resource object.
        // But we could easily miss something. So let's clone the shadow instead.
        ShadowType repoShadowBean = resourceObjectOrShadow.getBean().clone();

        // Attributes will be created anew, not as RAC but as PrismContainer. This is because normalization-aware
        // attributes are no longer ResourceAttribute instances. We delete them also because the application of the
        // raw PCD definition (below) would fail on a RAC. The same reasons for associations.
        repoShadowBean.asPrismObject().removeContainer(ShadowType.F_ATTRIBUTES);
        repoShadowBean.asPrismObject().removeContainer(ShadowType.F_ASSOCIATIONS);

        // For similar reason, we remove any traces of RACD from the definition.
        PrismObjectDefinition<ShadowType> standardDefinition =
                PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        repoShadowBean.asPrismObject().applyDefinition(standardDefinition);

        // For resource objects, this information is obviously limited, as there are no pending operations known.
        // But the exists and dead flags can tell us something.
        var shadowLifecycleState = ctx.determineShadowState(repoShadowBean);

        Object primaryIdentifierValue =
                ShadowManagerMiscUtil.determinePrimaryIdentifierValue(resourceObjectOrShadow, shadowLifecycleState);
        repoShadowBean.setPrimaryIdentifierValue(primaryIdentifierValue != null ? primaryIdentifierValue.toString() : null);

        var repoAttributesContainer = repoShadowBean.asPrismObject().findOrCreateContainer(ShadowType.F_ATTRIBUTES);
        for (ResourceAttribute<?> attribute : originalAttributesContainer.getAttributes()) {
            // TODO or should we use attribute.getDefinition()?
            var attrDef = objectDef.findAttributeDefinitionRequired(attribute.getElementName());
            if (ctx.shouldStoreAttributeInShadow(objectDef, attrDef)) {
                var repoAttrDef = attrDef.toNormalizationAware();
                var repoAttr = repoAttrDef.adoptRealValuesAndInstantiate(attribute.getRealValues());
                repoAttributesContainer.add(repoAttr);
            }
        }

        if (ctx.isCachingEnabled()) {
            CachingMetadataType cachingMetadata = new CachingMetadataType();
            cachingMetadata.setRetrievalTimestamp(clock.currentTimeXMLGregorianCalendar());
            repoShadowBean.setCachingMetadata(cachingMetadata);
        } else {
            repoShadowBean.setCachingMetadata(null);
            ProvisioningUtil.cleanupShadowActivation(repoShadowBean); // TODO deal with this more precisely
        }

        // Store only password meta-data in repo - unless there is explicit caching
        CredentialsType credentials = repoShadowBean.getCredentials();
        if (credentials != null) {
            PasswordType password = credentials.getPassword();
            if (password != null) {
                preparePasswordForStorage(password, ctx);
                ObjectReferenceType owner = ctx.getTask().getOwnerRef();
                ProvisioningUtil.addPasswordMetadata(password, clock.currentTimeXMLGregorianCalendar(), owner);
            }
            // TODO: other credential types - later
        }

        if (repoShadowBean.getName() == null) {
            PolyString name = MiscUtil.requireNonNull(
                    resourceObjectOrShadow.determineShadowName(),
                    () -> "Cannot determine the shadow name for " + resourceObjectOrShadow);
            repoShadowBean.setName(toPolyStringType(name));
        }

        repoShadowBean.setProtectedObject(null);
        repoShadowBean.setEffectiveOperationPolicy(null);

        MetadataUtil.addCreationMetadata(repoShadowBean);

        // the resource ref and object class are always there

        return RawRepoShadow.of(repoShadowBean);
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
