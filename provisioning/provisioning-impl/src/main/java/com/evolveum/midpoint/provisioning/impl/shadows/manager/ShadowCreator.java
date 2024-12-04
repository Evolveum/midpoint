/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.manager;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObjectShadow;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ShadowAuditHelper;
import com.evolveum.midpoint.provisioning.impl.shadows.ConstraintsChecker;
import com.evolveum.midpoint.provisioning.impl.shadows.ProvisioningOperationState.AddOperationState;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Creates shadows as needed. This is one of public classes of this package.
 */
@Component
public class ShadowCreator {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowCreator.class);

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private PendingOperationsHelper pendingOperationsHelper;
    @Autowired private ShadowAuditHelper shadowAuditHelper;
    @Autowired private ShadowFinder shadowFinder;
    @Autowired private ShadowObjectComputer shadowObjectComputer;

    /**
     * Adds (without checking for existence) a shadow corresponding to a resource object that was discovered.
     * Used when searching for objects or when completing entitlements.
     */
    public @NotNull RepoShadow addShadowForDiscoveredResourceObject(
            ProvisioningContext ctx, ExistingResourceObjectShadow resourceObject, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, EncryptionException, ConfigurationException {

        LOGGER.trace("Adding new shadow from resource object:\n{}", resourceObject.debugDumpLazily(1));

        ShadowType repoShadowBean =
                shadowObjectComputer
                        .createShadowForRepoStorage(ctx, resourceObject, result)
                        .getBean();
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
            ProvisioningContext ctx, ResourceObjectShadow objectToAdd, AddOperationState opState, OperationResult result)
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
                var pendingAddOperation = existingRepoShadow.findPendingAddOperation();
                if (pendingAddOperation != null) {
                    opState.setCurrentPendingOperation(pendingAddOperation);
                }
            }
            return;
        }

        ShadowType newRawRepoShadow = shadowObjectComputer
                .createShadowForRepoStorage(ctx, objectToAdd, result)
                .getBean();
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
                        shadowAfter.getPendingOperations().getOperations(),
                        () -> new IllegalStateException("multiple pending operations"),
                        () -> new IllegalStateException("no pending operations")));
    }
}
