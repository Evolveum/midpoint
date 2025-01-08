/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.loader;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;
import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;
import static com.evolveum.midpoint.schema.GetOperationOptions.isNoFetch;
import static com.evolveum.midpoint.schema.util.ShadowUtil.getShadowCachedStatus;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.LoggingUtils;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Updates the projection context:
 *
 * . Sets the "do reconciliation" flag for volatile resources.
 * . Loads the object (from repo or from resource), if needed. See {@link #loadCurrentObjectIfNeeded(OperationResult)}.
 * . Loads the resource, if not loaded yet.
 * . Sets projection security policy.
 * . Sets "can project" flag if limited propagation option is present.
 * . Sets the primary delta old value.
 *
 * See {@link #updateInternal(OperationResult)}.
 *
 * Note that full object can be loaded also in {@link ProjectionFullLoadOperation}.
 */
class ProjectionUpdateOperation<F extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(ProjectionUpdateOperation.class);

    private static final String OP_UPDATE = ProjectionUpdateOperation.class.getName() + "." + "update";

    @NotNull private final LensContext<F> context;
    @NotNull private final LensProjectionContext projectionContext;
    @NotNull private final Task task;
    @NotNull private final ModelBeans beans;

    /**
     * OID of the projection object. Remembered before manipulating with the projection.
     */
    private final String projectionObjectOid;

    /**
     * Current state of the projection object. Either loaded (if needed) or simply got from the context.
     */
    private ShadowType projectionObject;

    ProjectionUpdateOperation(
            @NotNull LensContext<F> context,
            @NotNull LensProjectionContext projectionContext,
            @NotNull Task task) {
        this.context = context;
        this.projectionContext = projectionContext;
        this.projectionObjectOid = projectionContext.getOid();
        this.task = task;
        this.beans = ModelBeans.get();
    }

    public void update(OperationResult parentResult)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        // TODO: not perfect. Practically, we want loadProjection operation (in context load operation) to contain
        //  all the projection results. But for that we would need code restructure.
        OperationResult result = parentResult.subresult(OP_UPDATE)
                .setMinor()
                .addArbitraryObjectAsParam("context", projectionContext)
                .build();
        try {
            updateInternal(result);
        } catch (Throwable e) {
            result.recordException(e);
            throw e;
        } finally {
            result.close();
        }
    }

    private void updateInternal(OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
            LOGGER.trace("Not updating broken context {}", projectionContext.getHumanReadableName());
            result.recordNotApplicable("Broken context");
            return;
        }

        loadResourceInContext(result);

        // Here we could skip loading if the projection is completed, but it would cause problems e.g. in wasProvisioned
        // method in dependency processor (it checks objectCurrent, among other things). So let's be conservative
        // and load also completed projections.

        projectionContext.setDoReconciliationFlagIfVolatile();

        if (loadCurrentObjectIfNeeded(result)) {
            return; // A non-critical error occurred.
        }

        setProjectionSecurityPolicy(result);
        setCanProjectFlag();

        projectionContext.setEstimatedOldValuesInPrimaryDelta();
    }

    /**
     * Loads the current object, if needed. See {@link #shouldLoadCurrentObject()} for the exact algorithm.
     *
     * Returns true if an error occurred.
     */
    private boolean loadCurrentObjectIfNeeded(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException, SecurityViolationException {

        projectionObject = asObjectable(projectionContext.getObjectCurrent());

        var loadingDepth = shouldLoadCurrentObject();
        if (loadingDepth != null) {
            return loadCurrentObject(loadingDepth, result);
        } else {
            if (projectionObjectOid != null) {
                projectionContext.setExists(
                        ShadowUtil.isExists(projectionObject));
            }
            return false;
        }
    }

    /**
     * Should the object be loaded or reloaded?
     *
     * Coupled with {@link #createProjectionLoadingOptions(LoadingDepth, OperationResult)} regarding whether `noFetch`
     * option should be used.
     *
     * There is an interesting side effect of "no fetch" loading of already-loaded object: the "full shadow" flag is discarded
     * in such cases. This may ensure the consistency at the cost of resource object re-loading.
     */
    private LoadingDepth shouldLoadCurrentObject() throws SchemaException, ConfigurationException {

        LOGGER.trace("Determining whether and how to load current object for {}", projectionContext);

        List<LensProjectionContext> modifiedDependees = projectionContext.getModifiedDataBoundDependees();
        if (!modifiedDependees.isEmpty()
                && projectionContext.hasProjectionWave()
                && projectionContext.isCurrentProjectionWave()) {
            // Reloading the projection if some of its data-dependees changed (and if it's wave has come). See MID-8929.
            // We do not reload if the wave is not known. This is to avoid useless reloading at the very beginning.
            //
            // In the future, we may consider optimizing the loading by removing the initial loading of these projections.
            // See MID-9083.
            LOGGER.trace(
                    "Will fully reload context with modified data-bound dependee because its wave has come. "
                            + "Projection ctx wave = {}, modified data-bound dependees = {}",
                    projectionContext.getWave(), modifiedDependees);
            return LoadingDepth.FULL;
        }

        if (projectionContext.getObjectCurrent() == null) {
            LOGGER.trace("Will load current object, as there is none loaded");
            return LoadingDepth.NORMAL;
        }

        if (projectionContext.isDoReconciliation() && !projectionContext.isFullShadow()) {
            LOGGER.trace("Will reload current object, because we are doing reconciliation and we do not have full shadow");
            // Note that the loading options will ensure that the full object is loaded, unless cache is used.
            return LoadingDepth.NORMAL;
        }

        // This is kind of brutal. But effective. We are reloading all higher-order dependencies
        // before they are processed. This makes sure we have fresh state when they are re-computed.
        // Because higher-order dependencies may have more than one projection context and the
        // changes applied to one of them are not automatically reflected on on other. therefore we need to reload.
        //
        // Note: we can safely assume that the projection wave is known, as the order is > 0
        // (order is determined by the dependency processor).
        if (projectionContext.getOrder() > 0
                && projectionContext.isCurrentProjectionWave()) {
            LOGGER.trace("Will reload higher-order context because its wave has come (projection ctx wave = {})",
                    projectionContext.getWave());
            return LoadingDepth.NORMAL; // the respective shadow should be already updated (except for volatile attributes)
        }

        if (projectionContext.isReloadNeeded()) {
            LOGGER.trace("Will reload current object because it was marked as to-be-reloaded earlier");
            return LoadingDepth.NORMAL;
        }

        LOGGER.trace("No explicit reason for reloading current object "
                        + "(recon: {}, full: {}, order: {}, wave: {}, modified deps: {})",
                projectionContext.isDoReconciliation(),
                projectionContext.isFullShadow(),
                projectionContext.getOrder(),
                projectionContext.getWave(),
                modifiedDependees);
        return null;
    }

    /**
     * If "limit propagation" option is set, we set `canProject` to `false` for resources other than triggering one.
     *
     * TODO what about multiple object types on the same resource? We should perhaps limit the propagation to specific
     *  object type, not just to the resource.
     */
    private void setCanProjectFlag() {
        String triggeringResourceOid = context.getTriggeringResourceOid();
        if (!ModelExecuteOptions.isLimitPropagation(context.getOptions()) || triggeringResourceOid == null) {
            return;
        }

        if (!triggeringResourceOid.equals(projectionContext.getResourceOid())) {
            projectionContext.setCanProject(false);
        }
    }

    private void setProjectionSecurityPolicy(OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        ResourceObjectDefinition structuralObjectDefinition = projectionContext.getStructuralObjectDefinition();
        if (structuralObjectDefinition != null) {
            LOGGER.trace("setProjectionSecurityPolicy: structural object class def = {}", structuralObjectDefinition);
            SecurityPolicyType projectionSecurityPolicy =
                    beans.securityPolicyFinder.locateResourceObjectSecurityPolicyLegacy(structuralObjectDefinition, result);
            LOGGER.trace("Located security policy for: {},\n {}", projectionContext, projectionSecurityPolicy);
            projectionContext.setProjectionSecurityPolicy(projectionSecurityPolicy);
        } else {
            LOGGER.trace("No structural object definition, skipping determining security policy");
        }
    }

    private void loadResourceInContext(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        if (projectionContext.getResource() != null) {
            return;
        }

        String resourceOid = projectionObject != null ?
                ShadowUtil.getResourceOidRequired(projectionObject) :
                projectionContext.getKey().getResourceOid();

        if (resourceOid != null) {
            projectionContext.setResource(
                    LensUtil.getResourceReadOnly(context, resourceOid, beans.provisioningService, task, result));
        }
    }

    /**
     * Loads the current object (objectOld)
     * Returns true if an error occurred.
     */
    private boolean loadCurrentObject(LoadingDepth loadingDepth, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException, SecurityViolationException {

        LOGGER.trace("Trying to load current object");

        if (projectionContext.isAdd() && !projectionContext.isCompleted()) {
            LOGGER.trace("No need to try to load old object, there is none");
            projectionContext.setExists(false);
            projectionObject = asObjectable(projectionContext.getObjectNew());
            return false;
        }

        if (projectionObjectOid == null) {
            LOGGER.trace("No OID, no loading");
            projectionContext.setExists(false);
            projectionContext.setFresh(true); // TODO why?
            if (projectionContext.getKey().getResourceOid() == null) {
                throw new SystemException(
                        "Projection " + projectionContext.getHumanReadableName() + " with null OID, no representation and "
                                + "no resource OID in projection context " + projectionContext);
            }
            return false;
        }

        var options = createProjectionLoadingOptions(loadingDepth, result);

        try {
            LOGGER.trace("Loading shadow {} for projection {}, options={}", projectionObjectOid, projectionContext, options);

            PrismObject<ShadowType> object =
                    beans.provisioningService.getObject(
                            ShadowType.class, projectionObjectOid, options, task, result);

            logLoadedShadow(object, options);
            checkLoadedShadowConsistency(object);

            projectionObject = object.asObjectable();
            projectionContext.setLoadedObject(object);

            projectionContext.setReloadNotNeeded();
            updateFullShadowFlag(options);
            updateExistsAndGoneFlags();

        } catch (ObjectNotFoundException ex) {

            LOGGER.debug("Could not find object with oid {} for projection context {}.",
                    projectionObjectOid, projectionContext.getHumanReadableName());

            // This does not mean BROKEN. The projection was there, but it gone now.
            // Consistency mechanism might have kicked in and fixed the shadow.
            // What we really want here is a "gone" projection or a refreshed projection.
            //
            // TODO if the shadow was deleted only on resource (not in repo), would we get ObjectNotFoundException here?
            //  Probably not. We need to reconsider the above comment.

            result.muteLastSubresultError();
            projectionContext.clearCurrentObject();
            projectionContext.markGone();
            projectionContext.setShadowExistsInRepo(false);
            projectionContext.setReloadNotNeeded();
            refreshContextAfterShadowNotFound(options, result);

        } catch (CommunicationException | SchemaException | ConfigurationException | SecurityViolationException
                | RuntimeException | Error e) {

            LOGGER.warn("Problem while getting object with oid {}. Projection context {} is marked as broken: {}: {}",
                    projectionObjectOid, projectionContext.getHumanReadableName(), e.getClass().getSimpleName(), e.getMessage());
            projectionContext.setBroken();
            projectionContext.setReloadNotNeeded();

            if (isExceptionFatal(e)) {
                throw e;
            } else {
                LOGGER.trace("Exception is not considered fatal: We'll stop updating the projection, "
                        + "but continue the clockwork execution.", e);
                return true;
            }
        }
        projectionContext.setFresh(true);
        return false;
    }

    private boolean isExceptionFatal(Throwable e) {
        ResourceType resource = projectionContext.getResource();
        if (resource == null) {
            return true;
        } else {
            ErrorSelectorType errorSelector = ResourceTypeUtil.getConnectorErrorCriticality(resource);
            if (errorSelector == null) {
                // In case of SchemaException: Just continue evaluation (in clockwork). The error is recorded in the result.
                // The consistency mechanism has (most likely) already done the best. We cannot do any better.
                return !(e instanceof SchemaException);
            } else {
                return ExceptionUtil.getCriticality(errorSelector, e, CriticalityType.FATAL) == CriticalityType.FATAL;
            }
        }
    }

    private void updateFullShadowFlag(Collection<SelectorOptions<GetOperationOptions>> options) {
        if (isNoFetch(options)) {
            projectionContext.setFullShadow(false);
        } else {
            projectionContext.determineFullShadowFlag(projectionObject);
        }
        LOGGER.trace("Full shadow flag: {}", projectionContext.isFullShadow());
    }

    private void updateExistsAndGoneFlags() {
        if (ShadowUtil.isExists(projectionObject)) {
            projectionContext.setExists(true);
        } else {
            projectionContext.setExists(false);
            if (ShadowUtil.isGone(projectionObject)) {
                projectionContext.markGone();
                LOGGER.debug("Found only dead {} for projection context {}.", projectionObject,
                        projectionContext.getHumanReadableName());
            } else {
                LOGGER.debug("Found only non-existing but non-dead {} for projection context {}.", projectionObject,
                        projectionContext.getHumanReadableName());
                // TODO Should we somehow mark this in the projection context?
            }
        }
    }

    private void logLoadedShadow(PrismObject<ShadowType> object, Collection<SelectorOptions<GetOperationOptions>> options) {
        LOGGER.trace("Shadow loaded (options: {}) for {}:\n{}",
                options, projectionContext.getHumanReadableName(), object.debugDumpLazily(1));
    }

    private void checkLoadedShadowConsistency(PrismObject<ShadowType> object) {
        Preconditions.checkNotNull(object.getOid());
        if (InternalsConfig.consistencyChecks) {
            String resourceOid = projectionContext.getResourceOid();
            if (resourceOid != null) {
                String shadowResourceOid = object.asObjectable().getResourceRef().getOid();
                if (!resourceOid.equals(shadowResourceOid)) {
                    throw new IllegalStateException(
                            String.format("Loaded shadow with wrong resourceRef. Loading shadow %s, got %s, "
                                            + "expected resourceRef %s, but was %s for context %s",
                                    projectionObjectOid, object.getOid(), resourceOid,
                                    shadowResourceOid, projectionContext.getHumanReadableName()));
                }
            }
        }
    }

    private Collection<SelectorOptions<GetOperationOptions>> createProjectionLoadingOptions(
            @NotNull LoadingDepth loadingDepth, OperationResult result)
            throws SchemaException, ConfigurationException {
        GetOperationOptionsBuilder builder = beans.schemaService.getOperationOptionsBuilder()
                //.readOnly() [not yet]
                .futurePointInTime()
                .allowNotFound();

        // Most probably reconciliation for all projections implies reconciliation for projContext
        // but we include both conditions just to be sure.
        var reconciliation = projectionContext.isDoReconciliation() || context.isDoReconciliationForAllProjections();

        if (projectionContext.isInMaintenance()) {

            LOGGER.trace("Using 'no fetch' mode because of resource maintenance (to avoid errors being reported)");
            builder = builder.noFetch();

        } else if (loadingDepth == LoadingDepth.FULL
                || reconciliation && shouldUseFresh(result)) {

            // TODO Regarding loading depth: this is quite a hack. The value of FULL means we cannot rely on the cached data.
            //  Maybe we should invalidated the cached data instead.

            builder = builder.forceRefresh();

            // We force operation retry "in hard way" only if we do full-scale reconciliation AND we are starting the clockwork.
            // This is to avoid useless repetition of retries (pushing attempt number quickly too high).
            if (context.isDoReconciliationForAllProjections() && context.getProjectionWave() == 0) {
                builder = builder.forceRetry();
            }

            if (SchemaConstants.CHANNEL_DISCOVERY_URI.equals(context.getChannel())) {
                // Avoid discovery loops
                builder = builder.doNotDiscovery();
            }
        } else {
            builder = builder.noFetch();
        }

        return builder.build();
    }

    private boolean shouldUseFresh(OperationResult result) throws SchemaException, ConfigurationException {
        var decision = switch (projectionContext.getCachedShadowsUse()) {
            case USE_FRESH -> {
                LOGGER.trace("Cached shadows use is not allowed, will use fresh data");
                yield true;
            }
            case USE_CACHED_OR_IGNORE, USE_CACHED_OR_FAIL -> {
                LOGGER.trace("Using fresh data is not allowed, will use cached ones (if available)");
                yield false;
            }
            case USE_CACHED_OR_FRESH -> {
                LOGGER.trace("Will determine if the data are fresh enough");
                yield null;
            }
        };
        if (decision != null) {
            return decision;
        }
        // We are to decide on using fresh/cached shadow now. We could try using staleness option instead, but we also need
        // to decide on refresh and retry, so let's do the determination here. This approach may change later.
        if (projectionObject == null) {
            try {
                // This is extra repo access; but it should be quite infrequent. TODO optimize this
                projectionObject =
                        beans.provisioningService
                                .getShadow(projectionObjectOid, createNoFetchCollection(), task, result)
                                .getBean();
            } catch (CommonException e) {
                LoggingUtils.logException(LOGGER, "Couldn't load shadow {} to determine the freshness", e, projectionObjectOid);
                return true; // Let's make the regular code throw the exception as usually (when caching is not involved)
            }
        }
        var cachedStatus = getShadowCachedStatus(
                projectionObject.asPrismObject(),
                ModelBeans.get().clock.currentTimeXMLGregorianCalendar());
        if (cachedStatus.isFresh()) {
            LOGGER.trace("Shadow is fresh ({}), no need to load it in full", cachedStatus);
            return false;
        } else {
            LOGGER.trace("Shadow is not fresh ({}), we'll load it in full:\n{}",
                    cachedStatus, projectionObject.debugDumpLazily(1));
            return true;
        }
    }

    private void refreshContextAfterShadowNotFound(Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        new MissingShadowContextRefresher<>(context, projectionContext, options, task)
                .refresh(result);
    }

    @Experimental
    enum LoadingDepth {

        /** The shadow has to be loaded "normally", i.e., from the repository, if possible. */
        NORMAL,

        /** The shadow must be loaded from the resource. */
        FULL
    }
}
