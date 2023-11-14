/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.shadows.errors;

import static com.evolveum.midpoint.provisioning.util.ProvisioningUtil.selectLiveShadow;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.provisioning.impl.shadows.ShadowAddOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowModifyOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.ShadowProvisioningOperation;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowFinder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDeltaUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ShadowUtil;
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

        if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), operation.getOptions())) {
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

        if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), operation.getOptions())) {
            ShadowType newShadow = operation.getOpState().getRepoShadow().clone();
            ObjectDeltaUtil.applyTo(newShadow.asPrismObject(), operation.getRequestedModifications());
            discoverConflictingShadow(ctx, newShadow, result);
        }

        throwException(operation, cause, result);
        throw new AssertionError("not here");
    }

    private void discoverConflictingShadow(
            ProvisioningContext ctx, ShadowType newShadow, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {

        OperationResult result = parentResult.createSubresult(OP_DISCOVERY);
        try {

            ObjectQuery query = createQueryBySecondaryIdentifier(newShadow);
            List<PrismObject<ShadowType>> conflictingRepoShadows = shadowFinder.searchShadows(ctx, query, null, result);
            PrismObject<ShadowType> oldShadow = selectLiveShadow(conflictingRepoShadows, "(conflicting repo shadows)");
            if (oldShadow != null) {
                ctx.applyAttributesDefinition(oldShadow);
            }

            LOGGER.trace("DISCOVERY: looking for conflicting shadow for {}", ShadowUtil.shortDumpShadowLazily(newShadow));

            final List<PrismObject<ShadowType>> conflictingResourceShadows =
                    findConflictingShadowsOnResource(query, ctx.getTask(), result);
            PrismObject<ShadowType> conflictingShadow =
                    selectLiveShadow(conflictingResourceShadows, "(conflicting shadows)");

            LOGGER.trace("DISCOVERY: found conflicting shadow for {}:\n{}", newShadow,
                    conflictingShadow == null ? "  no conflicting shadow" : conflictingShadow.debugDumpLazily(1));
            LOGGER.debug("DISCOVERY: discovered new shadow {}", ShadowUtil.shortDumpShadowLazily(conflictingShadow));
            LOGGER.trace("Processing \"already exists\" error for shadow:\n{}\nConflicting repo shadow:\n{}\nConflicting resource shadow:\n{}",
                    newShadow.debugDumpLazily(1),
                    oldShadow == null ? "  null" : oldShadow.debugDumpLazily(1),
                    conflictingShadow == null ? "  null" : conflictingShadow.debugDumpLazily(1));

            if (conflictingShadow != null) {
                // Original object and found object share the same object class, therefore they must
                // also share a kind. We can use this short-cut.
                conflictingShadow.asObjectable().setKind(newShadow.getKind());

                ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
                change.setResource(ctx.getResource().asPrismObject());
                change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANNEL_DISCOVERY));
                change.setShadowedResourceObject(conflictingShadow);
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

    static ObjectQuery createQueryBySecondaryIdentifier(ShadowType shadow) {
        // Note that if the query is to be used against the repository, we should not provide matching rules here. See MID-5547.
        // We also do not need to deal with normalization, as shadow manager will be used to execute the query.
        Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
        S_FilterEntry q = PrismContext.get().queryFor(ShadowType.class);
        q = q.block();
        if (secondaryIdentifiers.isEmpty()) {
            for (ResourceAttribute<?> primaryIdentifier: ShadowUtil.getPrimaryIdentifiers(shadow)) {
                q = q.itemAs(primaryIdentifier).or();
            }
        } else {
            // secondary identifiers connected by 'or' clause
            for (ResourceAttribute<?> secondaryIdentifier : secondaryIdentifiers) {
                q = q.itemAs(secondaryIdentifier).or();
            }
        }
        q = q.none().endBlock().and();
        // resource + object class
        q = q.item(ShadowType.F_RESOURCE_REF).ref(shadow.getResourceRef().getOid()).and();
        return q.item(ShadowType.F_OBJECT_CLASS).eq(shadow.getObjectClass()).build();
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
    protected void throwException(@Nullable ShadowProvisioningOperation<?> operation, Exception cause, OperationResult result)
            throws ObjectAlreadyExistsException {
        recordCompletionError(operation, cause, result);
        if (cause instanceof ObjectAlreadyExistsException) {
            throw (ObjectAlreadyExistsException)cause;
        } else {
            throw new ObjectAlreadyExistsException(cause.getMessage(), cause);
        }
    }
}
