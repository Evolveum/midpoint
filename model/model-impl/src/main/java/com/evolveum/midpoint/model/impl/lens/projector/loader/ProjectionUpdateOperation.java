/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.loader;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
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

import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Updates the projection context:
 *
 * 1. Sets the "do reconciliation" flag for volatile resources.
 * 2. Loads the object (from repo or from resource), if needed. See {@link #loadCurrentObjectIfNeeded(OperationResult)}
 * and {@link #needToReload()}.
 * 3. Loads the resource, if not loaded yet.
 * 4. Sets or updates the discriminator.
 * 5. Sets projection security policy.
 * 6. Sets "can project" flag if limited propagation option is present.
 * 7. Sets the primary delta old value.
 *
 * Note that full object can be loaded also in {@link ProjectionFullLoadOperation}.
 */
public class ProjectionUpdateOperation<F extends ObjectType> {

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
    private PrismObject<ShadowType> projectionObject;

    /**
     * True if the current projection was found to be a gone during {@link #loadCurrentObject(OperationResult)}
     * operation.
     */
    private boolean foundToBeGone;

    /**
     * Resource OID corresponding to the context. Set up in {@link #determineAndLoadResource(OperationResult)}.
     */
    private String resourceOid;

    public ProjectionUpdateOperation(
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
        OperationResult result = parentResult.createMinorSubresult(OP_UPDATE);
        try {
            updateInternal(result);
        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        } finally {
            result.close();
        }
    }

    private void updateInternal(OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        if (projectionContext.getSynchronizationPolicyDecision() == SynchronizationPolicyDecision.BROKEN) {
            LOGGER.trace("Skipping loading of broken context {}", projectionContext.getHumanReadableName());
            result.recordNotApplicable();
            return;
        }

        // Here we could skip loading if the projection is completed, but it would cause problems e.g. in wasProvisioned
        // method in dependency processor (it checks objectCurrent, among other things). So let's be conservative
        // and load also completed projections.

        projectionContext.setDoReconciliationFlagIfVolatile();

        if (loadCurrentObjectIfNeeded(result)) {
            return; // A non-critical error occurred.
        }

        determineAndLoadResource(result);

        determineDiscriminator();
        setProjectionSecurityPolicy(result);
        setCanProjectFlag();

        projectionContext.setEstimatedOldValuesInPrimaryDelta();
    }

    /**
     * Loads the current object, if it's not loaded or if it needs to be reloaded.
     *
     * Returns true if an error occurred.
     */
    private boolean loadCurrentObjectIfNeeded(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException, SecurityViolationException {
        projectionObject = projectionContext.getObjectCurrent();
        if (projectionContext.getObjectCurrent() == null || needToReload()) {
            return loadCurrentObject(result);
        } else {
            LOGGER.trace("No need to reload the object");
            if (projectionObjectOid != null) {
                projectionContext.setExists(ShadowUtil.isExists(projectionObject.asObjectable()));
            }
            return false;
        }
    }

    /**
     * If "limit propagation" option is set, we set `canProject` to `false` for resources other than triggering one.
     */
    private void setCanProjectFlag() {
        if (ModelExecuteOptions.isLimitPropagation(context.getOptions())) {
            if (context.getTriggeringResourceOid() != null && !context.getTriggeringResourceOid().equals(resourceOid)) {
                projectionContext.setCanProject(false);
            }
        }
    }

    private void setProjectionSecurityPolicy(OperationResult result)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        ResourceObjectDefinition structuralObjectDefinition = projectionContext.getStructuralObjectDefinition();
        if (structuralObjectDefinition != null) {
            LOGGER.trace("setProjectionSecurityPolicy: structural object class def = {}", structuralObjectDefinition);
            SecurityPolicyType projectionSecurityPolicy =
                    beans.securityHelper.locateProjectionSecurityPolicy(structuralObjectDefinition, task, result);
            LOGGER.trace("Located security policy for: {},\n {}", projectionContext, projectionSecurityPolicy);
            projectionContext.setProjectionSecurityPolicy(projectionSecurityPolicy);
        } else {
            LOGGER.trace("No structural object class definition, skipping determining security policy");
        }
    }

    private void determineDiscriminator() {
        if (projectionContext.getResourceShadowDiscriminator() == null) {
            ResourceShadowDiscriminator rsd;
            if (projectionObject != null) {
                ShadowType accountShadowType = projectionObject.asObjectable();
                String intent = ShadowUtil.getIntent(accountShadowType);
                ShadowKindType kind = ShadowUtil.getKind(accountShadowType);
                rsd = new ResourceShadowDiscriminator(resourceOid, kind, intent, accountShadowType.getTag(), foundToBeGone);
            } else {
                rsd = new ResourceShadowDiscriminator(null, null, null, null, foundToBeGone);
            }
            projectionContext.setResourceShadowDiscriminator(rsd);
        } else {
            if (foundToBeGone) {
                // We do not want to reset gone flag if it was set before
                projectionContext.markGone();
            }
        }
    }

    private void determineAndLoadResource(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {

        ResourceType existingResource = projectionContext.getResource();
        if (existingResource != null) {
            resourceOid = existingResource.getOid();
            return;
        }

        if (projectionObject != null) {
            ShadowType shadow = projectionObject.asObjectable();
            resourceOid = ShadowUtil.getResourceOid(shadow);
        } else if (projectionContext.getResourceShadowDiscriminator() != null) {
            resourceOid = projectionContext.getResourceShadowDiscriminator().getResourceOid();
        } else if (!foundToBeGone) {
            throw new IllegalStateException("No shadow, no discriminator and not gone? That won't do."
                    + " Projection "+projectionContext.getHumanReadableName());
        }

        if (resourceOid != null) {
            projectionContext.setResource(
                    LensUtil.getResourceReadOnly(context, resourceOid, beans.provisioningService, task, result));
        }
    }

    /**
     * Loads the current object (objectOld)
     * Returns true if an error occurred.
     */
    private boolean loadCurrentObject(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            ObjectNotFoundException, SecurityViolationException {

        if (projectionContext.isAdd() && !projectionContext.isCompleted()) {
            LOGGER.trace("No need to load old object, there is none");
            projectionContext.setExists(false);
            projectionContext.recompute();
            projectionObject = projectionContext.getObjectNew();
            return false;
        }

        if (projectionObjectOid == null) {
            projectionContext.setExists(false);
            projectionContext.setFresh(true); // TODO why?
            if (projectionContext.getResourceShadowDiscriminator() == null ||
                    projectionContext.getResourceShadowDiscriminator().getResourceOid() == null) {
                throw new SystemException(
                        "Projection " + projectionContext.getHumanReadableName() + " with null OID, no representation and "
                                + "no resource OID in projection context " + projectionContext);
            }
            return false;
        }

        Collection<SelectorOptions<GetOperationOptions>> options = createProjectionLoadingOptions();

        try {
            LOGGER.trace("Loading shadow {} for projection {}, options={}", projectionObjectOid,
                    projectionContext.getHumanReadableName(), options);

            PrismObject<ShadowType> object = beans.provisioningService.getObject(
                    projectionContext.getObjectTypeClass(), projectionObjectOid, options, task, result);

            logLoadedShadow(object, options);
            checkLoadedShadowConsistency(object);

            projectionObject = object;
            projectionContext.setLoadedObject(object);

            updateFullShadowFlag();
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
            projectionContext.setShadowExistsInRepo(false);
            refreshContextAfterShadowNotFound(options, result);

        } catch (CommunicationException | SchemaException | ConfigurationException | SecurityViolationException
                | RuntimeException | Error e) {

            LOGGER.warn("Problem while getting object with oid {}. Projection context {} is marked as broken: {}: {}",
                    projectionObjectOid, projectionContext.getHumanReadableName(), e.getClass().getSimpleName(), e.getMessage());
            projectionContext.setBroken();

            if (isExceptionFatal(e)) {
                throw e;
            } else {
                LOGGER.trace("Exception is not considered fatal: We'll stop updating the projection, "
                        + "but continue the clockwork execution.", e);
                return true;
            }
        }
        projectionContext.setFresh(true);
        projectionContext.recompute();
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

    private void updateFullShadowFlag() {
        if (projectionContext.isDoReconciliation()) { // TODO rather check using options (!noFetch), not via this condition
            projectionContext.determineFullShadowFlag(projectionObject);
        } else {
            projectionContext.setFullShadow(false);
        }
    }

    private void updateExistsAndGoneFlags() {
        if (ShadowUtil.isExists(projectionObject.asObjectable())) {
            projectionContext.setExists(true);
        } else {
            projectionContext.setExists(false);
            if (ShadowUtil.isGone(projectionObject.asObjectable())) {
                projectionContext.markGone();
                LOGGER.debug("Found only dead {} for projection context {}.", projectionObject,
                        projectionContext.getHumanReadableName());
                foundToBeGone = true;
            } else {
                LOGGER.debug("Found only non-existing but non-dead {} for projection context {}.", projectionObject,
                        projectionContext.getHumanReadableName());
                // TODO Should we somehow mark this in the projection context?
            }
        }
    }

    private void logLoadedShadow(PrismObject<ShadowType> object, Collection<SelectorOptions<GetOperationOptions>> options) {
        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        if (!GetOperationOptions.isNoFetch(rootOptions) && !GetOperationOptions.isRaw(rootOptions)) {
            LOGGER.trace("Full shadow loaded for {}:\n{}",
                    projectionContext.getHumanReadableName(), object.debugDumpLazily(1));
        }
    }

    private void checkLoadedShadowConsistency(PrismObject<ShadowType> object) {
        Validate.notNull(object.getOid());
        if (InternalsConfig.consistencyChecks) {
            String resourceOid = projectionContext.getResourceOid();
            if (resourceOid != null && !resourceOid.equals(object.asObjectable().getResourceRef().getOid())) {
                throw new IllegalStateException("Loaded shadow with wrong resourceRef. Loading shadow " + projectionObjectOid +
                        ", got " + object.getOid() + ", expected resourceRef " + resourceOid + ", but was " +
                        object.asObjectable().getResourceRef().getOid() +
                        " for context " + projectionContext.getHumanReadableName());
            }
        }
    }

    /**
     * Do we need to reload already-loaded object?
     *
     * TODO reconsider this algorithm
     */
    private boolean needToReload() {
        if (projectionContext.isDoReconciliation() && !projectionContext.isFullShadow()) {
            return true;
        }

        ResourceShadowDiscriminator rsd = projectionContext.getResourceShadowDiscriminator();
        if (rsd == null) {
            return false;
        }
        // This is kind of brutal. But effective. We are reloading all higher-order dependencies
        // before they are processed. This makes sure we have fresh state when they are re-computed.
        // Because higher-order dependencies may have more than one projection context and the
        // changes applied to one of them are not automatically reflected on on other. therefore we need to reload.
        if (rsd.getOrder() == 0) {
            return false;
        }
        int executionWave = context.getExecutionWave();
        int projCtxWave = projectionContext.getWave();
        if (executionWave == projCtxWave - 1) {
            // Reload right before its execution wave
            return true;
        }
        return false;
    }

    private Collection<SelectorOptions<GetOperationOptions>> createProjectionLoadingOptions() {
        GetOperationOptionsBuilder builder = beans.schemaService.getOperationOptionsBuilder()
                //.readOnly() [not yet]
                .futurePointInTime()
                .allowNotFound();

        // Most probably reconciliation for all projections implies reconciliation for projContext
        // but we include both conditions just to be sure.
        if (projectionContext.isDoReconciliation() || context.isDoReconciliationForAllProjections()) {
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

    private void refreshContextAfterShadowNotFound(Collection<SelectorOptions<GetOperationOptions>> options,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        new MissingShadowContextRefresher<>(context, projectionContext, options, task)
                .refresh(result);
    }
}
