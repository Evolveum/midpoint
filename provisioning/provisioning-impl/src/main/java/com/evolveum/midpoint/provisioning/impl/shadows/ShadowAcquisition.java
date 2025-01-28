/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows;

import static com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil.determinePrimaryIdentifierValue;

import com.evolveum.midpoint.provisioning.impl.RepoShadow;

import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObjectShadow;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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

    /**
     * The provisioning context. Corresponds to the original resource object.
     * Not updated after (eventual) shadow classification.
     */
    @NotNull private final ProvisioningContext ctx;

    /** Primary identifier of the shadow. */
    @NotNull private final ResourceObjectIdentification.WithPrimary primaryIdentification;

    /** The resource object we try to acquire shadow for. May be minimalistic in extreme cases (sync changes, emergency). */
    @NotNull private final ExistingResourceObjectShadow resourceObject;

    /*
     * When acquiring embedded shadows, we can skip some items, like operation execution information,
     * sparing some SQL queries (for native repo).
     */
    private final boolean embedded;

    private final ShadowsLocalBeans b = ShadowsLocalBeans.get();

    private ShadowAcquisition(
            @NotNull ProvisioningContext ctx,
            @NotNull ExistingResourceObjectShadow resourceObject,
            boolean embedded) throws SchemaException {
        this.ctx = ctx;
        this.primaryIdentification = resourceObject.getPrimaryIdentification();
        this.resourceObject = resourceObject;
        this.embedded = embedded;
    }


    /**
     * Acquires repository shadow for a provided resource object. The repository shadow is located or created.
     * In case that the shadow is created, all additional ceremonies for a new shadow are done, e.g. invoking
     * change notifications (discovery).
     *
     * It may look like this method would rather belong to ShadowManager. But it does not. It does too much stuff
     * (e.g. change notification).
     *
     * When acquiring embedded shadows, we can skip some items, like operation execution information,
     * sparing some SQL queries (for native repo).
     */
    static @NotNull RepoShadowWithState acquireRepoShadow(
            @NotNull ProvisioningContext ctx,
            @NotNull ExistingResourceObjectShadow resourceObject,
            boolean embedded,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, EncryptionException {

        return new ShadowAcquisition(ctx, resourceObject, embedded)
                .execute(result);
    }

    private @NotNull RepoShadowWithState execute(OperationResult result)
            throws SchemaException, ConfigurationException, EncryptionException {

        var existingLiveRepoShadow = b.shadowFinder.lookupLiveRepoShadowByPrimaryId(ctx, primaryIdentification, embedded, result);
        if (existingLiveRepoShadow != null) {
            LOGGER.trace("Found live shadow object in the repository {}", existingLiveRepoShadow.shortDumpLazily());
            return RepoShadowWithState.existing(existingLiveRepoShadow);
        }

        LOGGER.trace("Shadow object (in repo) corresponding to the resource object (on the resource) was not found. "
                + "The repo shadow will be created. The resource object:\n{}", resourceObject.debugDumpLazily(1));

        // The resource object obviously exists on the resource, but appropriate shadow does not exist in the repository.
        // We need to create the shadow to align repo state to the reality (resource).

        try {
            return RepoShadowWithState.discovered(
                    b.shadowCreator.addShadowForDiscoveredResourceObject(ctx, resourceObject, result));
        } catch (ObjectAlreadyExistsException e) {
            return findConflictingShadow(e, result);
        }
    }

    private @NotNull RepoShadowWithState findConflictingShadow(ObjectAlreadyExistsException e, OperationResult result)
            throws SchemaException, ConfigurationException {

        // Conflict! But we haven't supplied an OID and we have checked for existing shadow before,
        // therefore there should not conflict. Unless someone managed to create the same shadow
        // between our check and our create attempt. In that case try to re-check for shadow existence
        // once more.

        OperationResult originalRepoAddSubresult = result.getLastSubresult();

        LOGGER.debug("Attempt to create new repo shadow for {} ended up in conflict, re-trying the search for repo shadow",
                resourceObject);
        var conflictingLiveShadow = b.shadowFinder.lookupLiveRepoShadowByPrimaryId(ctx, primaryIdentification, embedded, result);

        if (conflictingLiveShadow != null) {
            if (b.shadowUpdater.markLiveShadowExistingIfNotMarkedSo(conflictingLiveShadow, result)) {
                originalRepoAddSubresult.muteError();
                return RepoShadowWithState.existing(conflictingLiveShadow);
            } else {
                // logged later
            }
        }

        // This is really strange. The shadow should not have disappeared in the meantime, dead shadow would remain instead.
        // Maybe we have broken "indexes"? (e.g. primaryIdentifierValue column)

        // Do some "research" and log the results, so we have good data to diagnose this situation.
        String determinedPrimaryIdentifierValue = determinePrimaryIdentifierValue(ctx, resourceObject);

        LOGGER.error("Unexpected repository behavior: object already exists error even after we double-checked "
                + "shadow uniqueness: {}", e.getMessage(), e);
        if (conflictingLiveShadow != null) {
            LOGGER.error("The conflicting shadow was there, but is there no longer. A transitional state? Shadow: {}",
                    conflictingLiveShadow);
        }
        LOGGER.debug("REPO CONFLICT: resource shadow\n{}", resourceObject.debugDumpLazily(1));
        LOGGER.debug("REPO CONFLICT: resource shadow: determined primaryIdentifierValue: {}", determinedPrimaryIdentifierValue);
        if (determinedPrimaryIdentifierValue != null) {
            RepoShadow potentialConflictingShadow =
                    b.shadowFinder.lookupShadowByIndexedPrimaryIdValue(ctx, determinedPrimaryIdentifierValue, result);
            LOGGER.debug("REPO CONFLICT: potential conflicting repo shadow (by primaryIdentifierValue)\n{}",
                    DebugUtil.debugDumpLazily(potentialConflictingShadow, 1));
        }

        throw new SystemException(
                "Unexpected repository behavior: object already exists error even after we double-checked shadow uniqueness: "
                        + e.getMessage(), e);
    }
}
