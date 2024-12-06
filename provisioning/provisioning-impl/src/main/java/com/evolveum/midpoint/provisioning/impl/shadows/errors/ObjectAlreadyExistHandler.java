/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.provisioning.impl.shadows.RepoShadowWithState;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;

import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifier;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.RepoShadow;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowAddOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowModifyOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowProvisioningOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifiers;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@Component
class ObjectAlreadyExistHandler extends HardErrorHandler {

    private static final String OP_DISCOVERY = ObjectAlreadyExistHandler.class + ".discovery";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectAlreadyExistHandler.class);

    @Autowired ProvisioningService provisioningService;
    @Autowired ShadowFinder shadowFinder;

    @Override
    public OperationResultStatus handleAddError(
            @NotNull ShadowAddOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            OperationResult result)
            throws SchemaException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        ProvisioningContext ctx = operation.getCtx();

        if (ProvisioningUtil.isDiscoveryAllowed(ctx.getResource(), operation.getOptions())) {
            discoverConflictingShadow(ctx, operation.getResourceObjectToAdd(), result);
        }

        throwException(operation, cause, result);
        throw new AssertionError("not here");
    }

    @Override
    public OperationResultStatus handleModifyError(
            @NotNull ShadowModifyOperation operation,
            @NotNull Exception cause,
            OperationResult failedOperationResult,
            @NotNull OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ObjectAlreadyExistsException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {

        ProvisioningContext ctx = operation.getCtx();

        if (ProvisioningUtil.isDiscoveryAllowed(ctx.getResource(), operation.getOptions())) {
            RepoShadow updatedRepoShadow = operation.getOpState().getRepoShadow().clone();
            // This may or may not fully work (applying full delta to a repository shadow only).
            updatedRepoShadow.updateWith(operation.getRequestedModifications());
            discoverConflictingShadow(ctx, updatedRepoShadow, result);
        }

        throwException(operation, cause, result);
        throw new AssertionError("not here");
    }

    private void discoverConflictingShadow(
            ProvisioningContext ctx, AbstractShadow targetObjectState, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {

        OperationResult result = parentResult.createSubresult(OP_DISCOVERY);
        try {

            // In theory, during ADD conflict resolution, there may be no identifiers in object being added (targetObjectState).
            // But in practice, let's ignore this. We will simply throw an exception in that case here.
            ObjectQuery query = createQueryBySecondaryIdentifier(targetObjectState);
            List<PrismObject<ShadowType>> conflictingRepoShadows = shadowFinder.searchShadows(ctx, query, null, result);
            RawRepoShadow oldShadow = RawRepoShadow.selectLiveShadow(conflictingRepoShadows, "(conflicting repo shadows)");

            LOGGER.trace("DISCOVERY: looking for conflicting shadow for {}", targetObjectState.shortDumpLazily());

            final List<PrismObject<ShadowType>> conflictingResourceShadows =
                    findConflictingShadowsOnResource(query, ctx.getTask(), result);
            RawRepoShadow conflictingShadow =
                    RawRepoShadow.selectLiveShadow(conflictingResourceShadows, "(conflicting shadows)");

            LOGGER.trace("DISCOVERY: found conflicting shadow for {}:\n{}", targetObjectState,
                    conflictingShadow == null ? "  no conflicting shadow" : conflictingShadow.debugDumpLazily(1));
            LOGGER.debug("DISCOVERY: discovered new shadow {}", conflictingShadow != null ? conflictingShadow.shortDumpLazily() : null);
            LOGGER.trace("Processing \"already exists\" error for shadow:\n{}\nConflicting repo shadow:\n{}\nConflicting resource shadow:\n{}",
                    targetObjectState.debugDumpLazily(1),
                    oldShadow == null ? "  null" : oldShadow.debugDumpLazily(1),
                    conflictingShadow == null ? "  null" : conflictingShadow.debugDumpLazily(1));

            if (conflictingShadow != null) {
                var adoptedConflictingShadow = ctx.adoptRawRepoShadow(conflictingShadow);
                ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
                change.setResource(ctx.getResource().asPrismObject());
                change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_DISCOVERY));
                ctx.computeAndUpdateEffectiveMarksAndPolicies(
                        adoptedConflictingShadow, RepoShadowWithState.ShadowState.EXISTING, result);
                change.setShadowedResourceObject(adoptedConflictingShadow.getPrismObject());
                change.setShadowExistsInRepo(true);
                eventDispatcher.notifyChange(change, ctx.getTask(), result);
            }
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    static ObjectQuery createQueryBySecondaryIdentifier(AbstractShadow shadow) throws SchemaException {
        // Note that if the query is to be used against the repository, we should not provide matching rules here. See MID-5547.
        // We also do not need to deal with normalization, as shadow manager will be used to execute the query.
        ResourceObjectIdentifiers identifiers = shadow.getIdentifiersRequired();
        S_FilterEntry q = PrismContext.get().queryFor(ShadowType.class);
        q = q.block();
        Set<ResourceObjectIdentifier.Secondary<?>> secondaryIdentifiers = identifiers.getSecondaryIdentifiers();
        if (secondaryIdentifiers.isEmpty()) {
            ResourceObjectIdentifier.Primary<?> primaryIdentifier = identifiers.getPrimaryIdentifier();
            if (primaryIdentifier != null) {
                // A kind of fallback
                ShadowSimpleAttribute<?> attr = primaryIdentifier.getAttribute();
                q = q.item(ShadowType.F_ATTRIBUTES.append(attr.getElementName()), attr.getDefinition())
                        .eq(attr.getValue())
                        .or();
            } else {
                // the query will match nothing (empty "OR") clause
            }
        } else {
            for (var secondaryIdentifier : secondaryIdentifiers) {
                ShadowSimpleAttribute<?> attr = secondaryIdentifier.getAttribute();
                q = q.item(ShadowType.F_ATTRIBUTES.append(attr.getElementName()), attr.getDefinition())
                        .eq(attr.getValue())
                        .or();
            }
        }
        q = q.none().endBlock().and();
        q = q.item(ShadowType.F_RESOURCE_REF).ref(shadow.getResourceOidRequired()).and();
        return q.item(ShadowType.F_OBJECT_CLASS).eq(shadow.getObjectClassName()).build();
    }

    /**
     * Looks for conflicting account on the resource (not just repository). We will get conflicting shadow.
     * But a side-effect of this search is that the shadow for the conflicting account is created in the repo.
     */
    private List<PrismObject<ShadowType>> findConflictingShadowsOnResource(ObjectQuery query, Task task, OperationResult parentResult)
        throws ObjectNotFoundException, CommunicationException, ConfigurationException, SchemaException,
                SecurityViolationException, ExpressionEvaluationException {
        // noDiscovery option to avoid calling notifyChange from ShadowManager (in case that new resource object is discovered)
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery());
        return provisioningService.searchObjects(ShadowType.class, query, options, task, parentResult);
    }

    @Override
    protected void throwException(@Nullable ShadowProvisioningOperation operation, Exception cause, OperationResult result)
            throws ObjectAlreadyExistsException {
        recordCompletionError(operation, cause, result);
        if (cause instanceof ObjectAlreadyExistsException objectAlreadyExistsException) {
            throw objectAlreadyExistsException;
        } else {
            throw new ObjectAlreadyExistsException(cause.getMessage(), cause);
        }
    }
}
