/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.provisioning.impl.sync;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.provisioning.api.GenericConnectorException;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.provisioning.impl.*;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.util.ProvisioningUtil;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.util.RepoCommonUtils;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.SchemaHelper;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;

/**
 *  This is the common "engine" that processes changes coming from live synchronization, asynchronous updates,
 *  and model.notifyChange calls. All the auxiliary pre-processing (finding old shadow, determining object class,
 *  etc) is placed here.
 *
 *  This is the final stage of processing, executing given change in a single thread. It is called
 *  from ChangeProcessingCoordinator, either in the caller thread (in single-threaded scenarios) or from worker threads/LATs
 *  (in multi-threaded scenarios).
 */
@Component
public class ChangeProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(ChangeProcessor.class);

    private static String OP_PROCESS_SYNCHRONIZATION = ChangeProcessor.class.getName() + ".processSynchronization";

    @Autowired private ShadowCaretaker shadowCaretaker;
    @Autowired private ShadowManager shadowManager;
    @Autowired private ShadowCache shadowCache;
    @Autowired private MatchingRuleRegistry matchingRuleRegistry;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private PrismContext prismContext;
    @Autowired private SchemaHelper schemaHelper;

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    public void execute(ProcessChangeRequest request, Task task, TaskPartitionDefinitionType partition,
            OperationResult parentResult) {

        ProvisioningContext globalCtx = request.getGlobalContext();
        Change change = request.getChange();

        if (change.isTokenOnly()) {
            return;         // nothing to do here ... token management is carried out elsewhere
        }

        OperationResult result = parentResult.createMinorSubresult(OP_PROCESS_SYNCHRONIZATION);

        try {

            ProvisioningContext ctx = determineProvisioningContext(globalCtx, change, result);
            if (ctx == null) {
                request.setSuccess(true);
                return;
            }

            if (change.getObjectDelta() != null) {
                shadowCaretaker.applyAttributesDefinition(ctx, change.getObjectDelta());
            }
            if (change.getOldShadow() != null) {
                shadowCaretaker.applyAttributesDefinition(ctx, change.getOldShadow());
            }
            if (change.getCurrentShadow() != null) {
                shadowCaretaker.applyAttributesDefinition(ctx, change.getCurrentShadow());  // maybe redundant
            }

            preProcessChange(ctx, change, result);

            // This is the case when we want to skip processing of change because the shadow was not found nor created.
            // The usual reason is that this is the delete delta and the object was already deleted on both resource and in repo.
            if (change.getOldShadow() == null) {
                assert change.isDelete();
                LOGGER.debug("Skipping processing change. Can't find appropriate shadow (e.g. the object was "
                        + "deleted on the resource meantime).");
                request.setSuccess(true);
                // Are we OK with the result being automatically computed here? (i.e. most probably SUCCESS?)
                return;
            }

            ResourceObjectShadowChangeDescription shadowChangeDescription = createResourceShadowChangeDescription(
                    change, ctx.getResource(), ctx.getChannel());
            shadowChangeDescription.setSimulate(request.isSimulate());

            LOGGER.trace("Created resource object shadow change description {}",
                    SchemaDebugUtil.prettyPrintLazily(shadowChangeDescription));
            OperationResult notifyChangeResult = result.createMinorSubresult(ShadowCache.class.getName() + ".notifyChange");
            notifyChangeResult.addParam("resourceObjectShadowChangeDescription", shadowChangeDescription.toString());

            try {
                shadowCache.notifyResourceObjectChangeListeners(shadowChangeDescription, ctx.getTask(), notifyChangeResult);
                notifyChangeResult.computeStatusIfUnknown();
            } catch (RuntimeException ex) {
                saveAccountResult(change, notifyChangeResult, result);
                throw new SystemException("Synchronization error: " + ex.getMessage(), ex);
            }

            notifyChangeResult.computeStatus("Error in notify change operation.");

            if (notifyChangeResult.isSuccess() || notifyChangeResult.isHandledError()) {
                // Do not delete dead shadows. Keep dead shadow around because they contain results
                // of the synchronization. Usual shadow refresh process should delete them eventually.
                // TODO: review. Maybe make this configuration later on.
                // But in that case model (SynchronizationService) needs to know whether shadow is
                // going to be deleted or whether it stays. Model needs to adjust links accordingly.
                // And we need to modify ResourceObjectChangeListener for that. Keeping all dead
                // shadows is much easier.
                //				deleteShadowFromRepoIfNeeded(change, result);
                request.setSuccess(true);
            } else {
                request.setSuccess(false);
                saveAccountResult(change, notifyChangeResult, result);
            }

            if (result.isUnknown()) {
                result.computeStatus();
            }

            try {
                validateResult(notifyChangeResult, task, partition);
            } catch (PreconditionViolationException e) {
                throw new SystemException(e.getMessage(), e);
            }

            if (request.isSuccess()) {
                request.onSuccess();
            } else {
                request.onError(result);
            }

        } catch (SchemaException | ObjectNotFoundException | ObjectAlreadyExistsException | CommunicationException |
                ConfigurationException | ExpressionEvaluationException | SecurityViolationException | EncryptionException |
                PolicyViolationException | RuntimeException | Error e) {
            result.recordFatalError(e);
            request.setSuccess(false);
            request.onError(e, result);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    /**
     * Returns null if the context cannot be determined because the change is DELETE and there's no shadow in repo.
     *
     * As a side effect it may fill-in change.oldShadow.
     */
    @Nullable
    private ProvisioningContext determineProvisioningContext(ProvisioningContext globalCtx, Change change,
            OperationResult parentResult)
            throws SchemaException, CommunicationException, ConfigurationException, ObjectNotFoundException,
            ExpressionEvaluationException, EncryptionException {
        if (change.getObjectClassDefinition() == null) {
            if (!change.isDelete()) {
                throw new SchemaException("No object class definition in change " + change);
            } else {
                // A specialty for DELETE changes: we try to determine the object class if not specified in the change
                // It is needed e.g. for some LDAP servers doing live synchronization.
                if (change.getOldShadow() == null) {
                    PrismObject<ShadowType> existing = shadowManager.findOrAddShadowFromChange(globalCtx, change, parentResult);
                    if (existing == null) {
                        LOGGER.debug("No old shadow for delete synchronization event {}, we probably did not know about "
                                + "that object anyway, so well be ignoring this event", change);
                        return null;
                    } else {
                        change.setOldShadow(existing);
                    }
                }
                return globalCtx.spawn(change.getOldShadow());
            }
        } else {
            return globalCtx.spawn(change.getObjectClassDefinition().getTypeName());
        }
    }

    /**
     * Manages repository representation of the shadow affected by the change.
     * Polishes the change itself.
     *
     * Does no synchronization.
     */
    private void preProcessChange(ProvisioningContext ctx, Change change, OperationResult parentResult)
            throws SchemaException, CommunicationException, ConfigurationException, SecurityViolationException,
            ObjectNotFoundException, GenericConnectorException, ExpressionEvaluationException, EncryptionException {

        if (change.getOldShadow() == null) {
            PrismObject<ShadowType> repoShadow = shadowManager.findOrAddShadowFromChange(ctx, change, parentResult);
            if (repoShadow != null) {
                shadowCaretaker.applyAttributesDefinition(ctx, repoShadow);
                change.setOldShadow(repoShadow);
            } else {
                assert change.isDelete();
                LOGGER.debug("No old shadow for synchronization event {}, the shadow must be gone in the meantime "
                        + "(this is probably harmless)", change);
                return;
            }
        }

        @NotNull PrismObject<ShadowType> oldShadow = change.getOldShadow();
        LOGGER.trace("Processing change, old shadow: {}", ShadowUtil.shortDumpShadow(oldShadow));

        ProvisioningUtil.setProtectedFlag(ctx, oldShadow, matchingRuleRegistry, relationRegistry);

        if (change.getCurrentShadow() == null && !change.isDelete()) {
            // Temporary measure: let us determine the current shadow; either by fetching it from the resource
            // (if possible) or by taking cached values and applying the delta. In the future we might implement
            // pure delta changes that do not need to know the current state.
            if (change.isAdd()) {
                change.setCurrentShadow(change.getObjectDelta().getObjectToAdd().clone());
            } else {
                if (ctx.getCachingStrategy() == CachingStategyType.PASSIVE) {
                    PrismObject<ShadowType> currentShadow = oldShadow.clone();
                    if (change.getObjectDelta() != null) {
                        change.getObjectDelta().applyTo(currentShadow);
                    }
                    change.setCurrentShadow(currentShadow);
                } else {
                    // no caching; let us retrieve the object from the resource
                    Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
                            .doNotDiscovery().build();
                    PrismObject<ShadowType> currentShadow;
                    try {
                        currentShadow = shadowCache.getShadow(oldShadow.getOid(), oldShadow, options, ctx.getTask(), parentResult);
                    } catch (ObjectNotFoundException e) {
                        // The object on the resource does not exist (any more?).
                        LOGGER.warn("Object {} does not exist on the resource any more", oldShadow);
                        throw e;            // TODO
                    }
                    change.setCurrentShadow(currentShadow);
                }
            }
        }
        assert change.getCurrentShadow() != null || change.isDelete();
        if (change.getCurrentShadow() != null) {
            PrismObject<ShadowType> currentShadow = shadowCache.completeShadow(ctx, change.getCurrentShadow(), oldShadow, false, parentResult);
            change.setCurrentShadow(currentShadow);
            // TODO: shadowState
            shadowManager.updateShadow(ctx, currentShadow, oldShadow, null, parentResult);
        }

        if (change.getObjectDelta() != null && change.getObjectDelta().getOid() == null) {
            change.getObjectDelta().setOid(oldShadow.getOid());
        }

        if (change.isDelete()) {
            PrismObject<ShadowType> currentShadow = change.getCurrentShadow();
            if (currentShadow == null) {
                currentShadow = oldShadow.clone();
                change.setCurrentShadow(currentShadow);         // todo why is this? [pmed]
            }
            if (!ShadowUtil.isDead(currentShadow) || ShadowUtil.isExists(currentShadow)) {
                shadowManager.markShadowTombstone(currentShadow, parentResult);
            }
        }
    }

    private ResourceObjectShadowChangeDescription createResourceShadowChangeDescription(
            Change change, ResourceType resourceType, String channel) {
        ResourceObjectShadowChangeDescription shadowChangeDescription = new ResourceObjectShadowChangeDescription();
        shadowChangeDescription.setObjectDelta(change.getObjectDelta());
        shadowChangeDescription.setResource(resourceType.asPrismObject());
        shadowChangeDescription.setOldShadow(change.getOldShadow());
        shadowChangeDescription.setCurrentShadow(change.getCurrentShadow());
        shadowChangeDescription.setSourceChannel(channel != null ? channel : SchemaConstants.CHANGE_CHANNEL_LIVE_SYNC_URI);
        return shadowChangeDescription;
    }

    private void saveAccountResult(Change change, OperationResult notifyChangeResult, OperationResult parentResult) {
        Collection<? extends ItemDelta> shadowModification = createShadowResultModification(change, notifyChangeResult);
        String oid = change.getOid();
        // maybe better error handling is needed
        try {
            ConstraintsChecker.onShadowModifyOperation(shadowModification);
            repositoryService.modifyObject(ShadowType.class, oid, shadowModification, parentResult);
        } catch (SchemaException ex) {
            parentResult.recordPartialError("Couldn't modify object: schema violation: " + ex.getMessage(), ex);
        } catch (ObjectNotFoundException ex) {
            parentResult.recordWarning("Couldn't modify object: object not found: " + ex.getMessage(), ex);
        } catch (ObjectAlreadyExistsException ex) {
            parentResult.recordPartialError("Couldn't modify object: object already exists: " + ex.getMessage(), ex);
        }
    }

    private Collection<? extends ItemDelta> createShadowResultModification(Change change, OperationResult shadowResult) {
        PrismObjectDefinition<ShadowType> shadowDefinition = getShadowDefinition();

        Collection<ItemDelta> modifications = new ArrayList<>();
        PropertyDelta resultDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(ShadowType.F_RESULT,
                shadowDefinition, shadowResult.createOperationResultType());
        modifications.add(resultDelta);
        if (change.getObjectDelta() != null && change.getObjectDelta().getChangeType() == ChangeType.DELETE) {
            PropertyDelta failedOperationTypeDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                    ShadowType.F_FAILED_OPERATION_TYPE, shadowDefinition, FailedOperationTypeType.DELETE);
            modifications.add(failedOperationTypeDelta);
        }
        return modifications;
    }

    private void validateResult(OperationResult result, Task task, TaskPartitionDefinitionType partition)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException,
            PreconditionViolationException {

        if (result.isSuccess() || result.isHandledError()) {
            return;
        }

        Throwable ex = RepoCommonUtils.getResultException(result);
        ErrorSelectorType selector = partition != null ? partition.getErrorCriticality() : null;
        CriticalityType criticality = ExceptionUtil.getCriticality(selector, ex, CriticalityType.PARTIAL);
        RepoCommonUtils.processErrorCriticality(task, criticality, ex, result);
    }

    private PrismObjectDefinition<ShadowType> getShadowDefinition() {
        return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
    }

}
