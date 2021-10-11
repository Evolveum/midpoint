/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.errorhandling;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.prism.delta.ObjectDeltaUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterEntry;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.impl.ProvisioningOperationState;
import com.evolveum.midpoint.provisioning.impl.ShadowCaretaker;
import com.evolveum.midpoint.provisioning.impl.shadowmanager.ShadowManager;
import com.evolveum.midpoint.provisioning.ucf.api.GenericFrameworkException;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.AsynchronousOperationResult;
import com.evolveum.midpoint.schema.result.AsynchronousOperationReturnValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

@Component
public class ObjectAlreadyExistHandler extends HardErrorHandler {

    private static final String OP_DISCOVERY = ObjectAlreadyExistHandler.class + ".discovery";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectAlreadyExistHandler.class);

    @Autowired ProvisioningService provisioningService;
    @Autowired ShadowCaretaker shadowCaretaker;
    @Autowired ShadowManager shadowManager;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Override
    public OperationResultStatus handleAddError(ProvisioningContext ctx, PrismObject<ShadowType> shadowToAdd,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<PrismObject<ShadowType>>> opState,
            Exception cause, OperationResult failedOperationResult, Task task, OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), options)) {
            discoverConflictingShadow(ctx, shadowToAdd, options, opState, cause, failedOperationResult, task, parentResult);
        }

        return super.handleAddError(ctx, shadowToAdd, options, opState, cause, failedOperationResult, task, parentResult);
    }


    @Override
    public OperationResultStatus handleModifyError(ProvisioningContext ctx,
            PrismObject<ShadowType> repoShadow, Collection<? extends ItemDelta> modifications,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<AsynchronousOperationReturnValue<Collection<PropertyDelta<PrismPropertyValue>>>> opState,
            Exception cause, OperationResult failedOperationResult, Task task, OperationResult parentResult)
            throws SchemaException, GenericFrameworkException, CommunicationException,
            ObjectNotFoundException, ObjectAlreadyExistsException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException {

        if (ProvisioningUtil.isDoDiscovery(ctx.getResource(), options)) {
            PrismObject<ShadowType> newShadow = repoShadow.clone();
            ObjectDeltaUtil.applyTo(newShadow, (Collection) modifications);
            discoverConflictingShadow(ctx, newShadow, options, opState, cause, failedOperationResult, task, parentResult);
        }

        return super.handleModifyError(ctx, repoShadow, modifications, options, opState, cause, failedOperationResult,
                task, parentResult);
    }



    private void discoverConflictingShadow(ProvisioningContext ctx, PrismObject<ShadowType> newShadow,
            ProvisioningOperationOptions options,
            ProvisioningOperationState<? extends AsynchronousOperationResult> opState,
            Exception cause, OperationResult failedOperationResult, Task task, OperationResult parentResult)
                    throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException {

        // TODO: this probably should NOT be a subresult of parentResult. We probably want new result (and maybe also task) here.
        OperationResult result = parentResult.createSubresult(OP_DISCOVERY);
        try {

            ObjectQuery query = createQueryBySecondaryIdentifier(newShadow.asObjectable());

            final List<PrismObject<ShadowType>> conflictingRepoShadows = findConflictingShadowsInRepo(query, task, result);
            PrismObject<ShadowType> oldShadow = shadowManager.eliminateDeadShadows(conflictingRepoShadows, result);
            if (oldShadow != null) {
                shadowCaretaker.applyAttributesDefinition(ctx, oldShadow);
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("DISCOVERY: looking for conflicting shadow for {}", ShadowUtil.shortDumpShadow(newShadow));
            }

            final List<PrismObject<ShadowType>> conflictingResourceShadows = findConflictingShadowsOnResource(query, task, result);
            PrismObject<ShadowType> conflictingShadow = shadowManager.eliminateDeadShadows(conflictingResourceShadows, result);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("DISCOVERY: found conflicting shadow for {}:\n{}", newShadow, conflictingShadow==null?"  no conflicting shadow":conflictingShadow.debugDump(1));
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("DISCOVERY: discovered new shadow {}", ShadowUtil.shortDumpShadow(conflictingShadow));
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Processing \"already exists\" error for shadow:\n{}\nConflicting repo shadow:\n{}\nConflicting resource shadow:\n{}",
                        newShadow.debugDump(1),
                        oldShadow==null ? "  null" : oldShadow.debugDump(1),
                        conflictingShadow==null ? "  null" : conflictingShadow.debugDump(1));
            }

            if (conflictingShadow != null) {
                // Original object and found object share the same object class, therefore they must
                // also share a kind. We can use this short-cut.
                conflictingShadow.asObjectable().setKind(newShadow.asObjectable().getKind());

                ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
                change.setResource(ctx.getResource().asPrismObject());
                change.setSourceChannel(QNameUtil.qNameToUri(SchemaConstants.CHANGE_CHANNEL_DISCOVERY));
                change.setOldShadow(oldShadow);
                change.setCurrentShadow(conflictingShadow);
                changeNotificationDispatcher.notifyChange(change, task, result);
            }
        } finally {
            result.computeStatus();
        }
    }

    // TODO: maybe move to ShadowManager?


    private boolean isFresher(PrismObject<ShadowType> theShadow, PrismObject<ShadowType> refShadow) {
        return XmlTypeConverter.isFresher(
                ObjectTypeUtil.getLastTouchTimestamp(theShadow), ObjectTypeUtil.getLastTouchTimestamp(refShadow));
    }


    private ObjectQuery createQueryBySecondaryIdentifier(ShadowType shadow) {
        // TODO ensure that the identifiers are normalized here
        // Note that if the query is to be used against the repository, we should not provide matching rules here. See MID-5547.
        Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
        S_AtomicFilterEntry q = prismContext.queryFor(ShadowType.class);
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
     * Note: this may return dead shadow.
     */
    private List<PrismObject<ShadowType>> findConflictingShadowsInRepo(ObjectQuery query, Task task, OperationResult parentResult)
            throws SchemaException {
        return repositoryService.searchObjects(ShadowType.class, query, null, parentResult);
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
    protected void throwException(Exception cause, ProvisioningOperationState<? extends AsynchronousOperationResult> opState, OperationResult result)
            throws ObjectAlreadyExistsException {
        recordCompletionError(cause, opState, result);
        if (cause instanceof ObjectAlreadyExistsException) {
            throw (ObjectAlreadyExistsException)cause;
        } else {
            throw new ObjectAlreadyExistsException(cause.getMessage(), cause);
        }
    }

}
