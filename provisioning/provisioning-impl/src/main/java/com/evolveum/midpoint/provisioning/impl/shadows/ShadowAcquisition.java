/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil.determinePrimaryIdentifierValue;
import static com.evolveum.midpoint.schema.util.ShadowUtil.shortDumpShadowLazily;

import static java.util.Objects.requireNonNull;

/**
 * Takes care of the _shadow acquisition_ process. We look up an appropriate live shadow,
 * and if it is not found, we try to create one.
 *
 * This process is invoked in several situations:
 *
 * 1. Resource object is found during `searchObjects` call.
 * 2. Resource object appeared as part of live sync or async update process.
 * 3. Resource object was found during entitlement conversion (attribute -> association).
 *
 * Note that a different process is followed during {@link ShadowGetOperation}: we have a shadow first (otherwise we could not
 * ask for `getObject`), so we have no need to acquire one there. We just update the shadow after getting the resource object.
 *
 * This class also takes care of _object classification_. I am not sure if this is the right approach, though.
 */
class ShadowAcquisition {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowAcquisition.class);

    /** The provisioning context. Not updated after (eventual) shadow classification. */
    @NotNull private final ProvisioningContext ctx;

    /** Primary identifier of the shadow. */
    @NotNull private final PrismProperty<?> primaryIdentifier;

    /**
     * In theory, this is the same as the object class in the provisioning context.
     * But e.g. in the case of entitlements completion the current implementation creates context from definitions,
     * not from the actual objects. So let's be safe and use the actual object class name here.
     */
    @NotNull private final QName objectClass;

    /** The resource object we try to acquire shadow for. May be minimalistic in extreme cases (sync changes, emergency). */
    @NotNull private final ShadowType resourceObject;

    /** Whether we want to skip the classification. It is used e.g. in emergency shadow creation. */
    private final boolean skipClassification;

    private final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    private ShadowAcquisition(
            @NotNull ProvisioningContext ctx,
            @NotNull PrismProperty<?> primaryIdentifier,
            @NotNull QName objectClass,
            @NotNull ShadowType resourceObject,
            boolean skipClassification) {
        this.ctx = ctx;
        this.primaryIdentifier = primaryIdentifier;
        this.objectClass = objectClass;
        this.resourceObject = resourceObject;
        this.skipClassification = skipClassification;
    }

    /**
     * Acquires repository shadow for a provided resource object. The repository shadow is located or created.
     * In case that the shadow is created, all additional ceremonies for a new shadow are done, e.g. invoking
     * change notifications (discovery).
     *
     * Returned shadow is NOT guaranteed to have all the attributes aligned and updated. That is only possible after
     * completeShadow(). But maybe, this method can later invoke completeShadow() and do all the necessary stuff?
     *
     * TODO completeShadow method does not exist any more, update the docs
     *
     * It may look like this method would rather belong to ShadowManager. But it does not. It does too much stuff
     * (e.g. change notification).
     */
    @NotNull static ShadowType acquireRepoShadow(
            @NotNull ProvisioningContext ctx,
            @NotNull ShadowType resourceObject,
            boolean skipClassification,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        PrismProperty<?> primaryIdentifier = ProvisioningUtil.getSingleValuedPrimaryIdentifierRequired(resourceObject);
        QName objectClass = requireNonNull(
                resourceObject.getObjectClass(),
                () -> "No object class in " + ShadowUtil.shortDumpShadow(resourceObject));

        return new ShadowAcquisition(ctx, primaryIdentifier, objectClass, resourceObject, skipClassification)
                .execute(result);
    }

    public @NotNull ShadowType execute(OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            GenericConnectorException, ExpressionEvaluationException, EncryptionException, SecurityViolationException {

        ShadowType repoShadow = acquireRawRepoShadow(result);

        setOidAndResourceRefToResourceObject(repoShadow);

        if (skipClassification) {
            LOGGER.trace("Acquired repo shadow (skipping classification as requested):\n{}", repoShadow.debugDumpLazily(1));
            return repoShadow;
        } else if (!b.classificationHelper.shouldClassify(ctx, repoShadow)) {
            LOGGER.trace("Acquired repo shadow (no need to classify):\n{}", repoShadow.debugDumpLazily(1));
            return repoShadow;
        } else {
            ShadowType fixedRepoShadow = classifyAndFixTheShadow(repoShadow, result);
            LOGGER.trace("Acquired repo shadow (after classification and re-reading):\n{}",
                    fixedRepoShadow.debugDumpLazily(1));
            return fixedRepoShadow;
        }
    }

    private @NotNull ShadowType classifyAndFixTheShadow(ShadowType repoShadow, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ConfigurationException, CommunicationException,
            ExpressionEvaluationException, SecurityViolationException {

        var classification = b.classificationHelper.classify(ctx, repoShadow, resourceObject, result);

        // TODO We probably can avoid re-reading the shadow
        return b.shadowUpdater.normalizeShadowAttributesInRepositoryAfterClassification(ctx, repoShadow, classification, result);
    }

    // TODO is it OK to do it here? OID maybe. But resourceRef should have been there already (although without full object)
    private void setOidAndResourceRefToResourceObject(ShadowType repoShadow) {
        resourceObject.setOid(repoShadow.getOid());
        if (resourceObject.getResourceRef() == null) {
            resourceObject.setResourceRef(new ObjectReferenceType());
        }
        resourceObject.getResourceRef().asReferenceValue().setObject(ctx.getResource().asPrismObject());
    }

    private @NotNull ShadowType acquireRawRepoShadow(OperationResult result)
            throws SchemaException, EncryptionException {

        var existingLiveRepoShadow = b.shadowFinder.lookupLiveShadowByPrimaryId(ctx, primaryIdentifier, objectClass, result);
        if (existingLiveRepoShadow != null) {
            LOGGER.trace("Found live shadow object in the repository {}", shortDumpShadowLazily(existingLiveRepoShadow));
            if (b.shadowUpdater.markLiveShadowExistingIfNotMarkedSo(existingLiveRepoShadow, result)) {
                return existingLiveRepoShadow;
            } else {
                LOGGER.trace("The shadow disappeared, we will create a new one: {}", existingLiveRepoShadow);
            }
        }

        LOGGER.trace("Shadow object (in repo) corresponding to the resource object (on the resource) was not found. "
                + "The repo shadow will be created. The resource object:\n{}", resourceObject);

        // The resource object obviously exists on the resource, but appropriate shadow does not exist in the repository.
        // We need to create the shadow to align repo state to the reality (resource).

        try {
            return b.shadowCreator.addDiscoveredRepositoryShadow(ctx, resourceObject, result);
        } catch (ObjectAlreadyExistsException e) {
            return findConflictingShadow(resourceObject, e, result);
        }
    }

    private @NotNull ShadowType findConflictingShadow(
            ShadowType resourceObject, ObjectAlreadyExistsException e, OperationResult result)
            throws SchemaException {

        // Conflict! But we haven't supplied an OID and we have checked for existing shadow before,
        // therefore there should not conflict. Unless someone managed to create the same shadow
        // between our check and our create attempt. In that case try to re-check for shadow existence
        // once more.

        OperationResult originalRepoAddSubresult = result.getLastSubresult();

        LOGGER.debug("Attempt to create new repo shadow for {} ended up in conflict, re-trying the search for repo shadow",
                resourceObject);
        var conflictingLiveShadow = b.shadowFinder.lookupLiveShadowByPrimaryId(ctx, primaryIdentifier, objectClass, result);

        if (conflictingLiveShadow != null) {
            if (b.shadowUpdater.markLiveShadowExistingIfNotMarkedSo(conflictingLiveShadow, result)) {
                originalRepoAddSubresult.muteError();
                return conflictingLiveShadow;
            } else {
                // logged later
            }
        }

        // This is really strange. The shadow should not have disappeared in the meantime, dead shadow would remain instead.
        // Maybe we have broken "indexes"? (e.g. primaryIdentifierValue column)

        // Do some "research" and log the results, so we have good data to diagnose this situation.
        String determinedPrimaryIdentifierValue = determinePrimaryIdentifierValue(ctx, resourceObject);
        ShadowType potentialConflictingShadow =
                b.shadowFinder.lookupShadowByIndexedPrimaryIdValue(ctx, determinedPrimaryIdentifierValue, result);

        LOGGER.error("Unexpected repository behavior: object already exists error even after we double-checked "
                + "shadow uniqueness: {}", e.getMessage(), e);
        if (conflictingLiveShadow != null) {
            LOGGER.error("The conflicting shadow was there, but is there no longer. A transitional state? Shadow: {}",
                    conflictingLiveShadow);
        }
        LOGGER.debug("REPO CONFLICT: resource shadow\n{}", resourceObject.debugDumpLazily(1));
        LOGGER.debug("REPO CONFLICT: resource shadow: determined primaryIdentifierValue: {}", determinedPrimaryIdentifierValue);
        LOGGER.debug("REPO CONFLICT: potential conflicting repo shadow (by primaryIdentifierValue)\n{}",
                DebugUtil.debugDumpLazily(potentialConflictingShadow, 1));

        throw new SystemException(
                "Unexpected repository behavior: object already exists error even after we double-checked shadow uniqueness: "
                        + e.getMessage(), e);
    }
}
