/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.loader;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

import static com.evolveum.midpoint.model.impl.lens.LensUtil.getExportType;

/**
 * Represents the loading of the lens context: both focus and projections.
 *
 * Delegates much to {@link FocusLoadOperation}, {@link ProjectionsLoadOperation}, and {@link ProjectionUpdateOperation}.
 *
 * Intentionally package-private.
 *
 * TODO This structure reflects the code before midPoint 4.4. We should perhaps restructure it further,
 *  e.g. to merge {@link ProjectionUpdateOperation} functionality into {@link ProjectionsLoadOperation}.
 */
class ContextLoadOperation<F extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(ContextLoadOperation.class);

    // For backwards compatibility reasons we use ContextLoader here (not ContextLoadOperation)
    public static final String CLASS_DOT = ContextLoader.class.getName() + ".";
    private static final String OPERATION_LOAD = CLASS_DOT + "load";

    @NotNull private final LensContext<F> context;
    @NotNull private final String activityDescription;
    @NotNull private final Task task;

    @NotNull private final ModelBeans beans = ModelBeans.get();

    /** Trace that is used during the context load operation (if any). */
    private ProjectorComponentTraceType trace;

    ContextLoadOperation(@NotNull LensContext<F> context, @NotNull String activityDescription, @NotNull Task task) {
        this.context = context;
        this.activityDescription = activityDescription;
        this.task = task;
    }

    public void load(OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {

        OperationResult result = parentResult.createMinorSubresult(OPERATION_LOAD);
        createTraceIfNeeded(result);

        try {
            context.checkConsistenceIfNeeded();

            loadFocusContext(result);

            LensFocusContext<F> focusContext = context.getFocusContext();
            if (focusContext != null) {

                updatePolicies(result);

                if (FocusType.class.isAssignableFrom(context.getFocusClass())) {
                    loadProjections(result);
                }

                for (var projectionContext : context.getProjectionContexts()) {
                    if (projectionContext.getSynchronizationIntent() != null) {
                        // Accounts with explicitly set intent are never rotten. These are explicitly requested actions
                        // if they fail then they really should fail.
                        projectionContext.setFresh(true);
                    }
                }

                focusContext.deleteEmptyPrimaryDelta();
                focusContext.setEstimatedOldValuesInPrimaryDelta();

            } else {

                // Projection contexts are not rotten in this case. There is no focus so there is no way to refresh them.
                for (var projectionContext : context.getProjectionContexts()) {
                    projectionContext.setFresh(true);
                }
            }

            removeRottenContexts();

            context.checkConsistenceIfNeeded();

            for (var projectionContext : context.getProjectionContexts()) {
                context.checkAbortRequested();
                updateProjection(projectionContext, result);
            }

            context.checkConsistenceIfNeeded();

            // Set the "fresh" mark now so following consistency check will be stricter
            context.setFresh(true);
            context.checkConsistenceIfNeeded();

            beans.medic.traceContext(LOGGER, activityDescription, "after load", false, context, false);

            result.computeStatusComposite();

        } catch (Throwable e) {
            result.recordFatalError(e);
            throw e;
        } finally {
            recordTraceAtEnd(result);
        }
    }

    private void updateProjection(LensProjectionContext projectionContext, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        new ProjectionUpdateOperation<>(context, projectionContext, task)
                .update(result);
    }

    private void loadProjections(OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException {
        //noinspection unchecked
        new ProjectionsLoadOperation<>((LensContext<? extends FocusType>) context, task)
                .load(result); // this also removes the accountRef deltas (???)
    }

    private void loadFocusContext(OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        new FocusLoadOperation<>(context, task)
                .load(result);
    }

    private void createTraceIfNeeded(OperationResult result) throws SchemaException {
        if (result.isTracingAny(ProjectorComponentTraceType.class)) {
            trace = new ProjectorComponentTraceType();
            if (result.isTracingNormal(ProjectorComponentTraceType.class)) {
                trace.setInputLensContextText(context.debugDump());
            }
            trace.setInputLensContext(context.toBean(getExportType(trace, result)));
            result.addTrace(trace);
        } else {
            trace = null;
        }
    }

    private void recordTraceAtEnd(OperationResult result) throws SchemaException {
        if (trace != null) {
            if (result.isTracingNormal(ProjectorComponentTraceType.class)) {
                trace.setOutputLensContextText(context.debugDump());
            }
            trace.setOutputLensContext(context.toBean(getExportType(trace, result)));
        }
    }

    /**
     * Updates various policies (archetype policy, focus template, account synchronization settings, security policy)
     * from system configuration and other sources.
     *
     * TODO Eliminate "upward" calls to {@link ContextLoader}.
     */
    private void updatePolicies(OperationResult result)
            throws ObjectNotFoundException, SchemaException, ConfigurationException, ExpressionEvaluationException,
            CommunicationException, SecurityViolationException, PolicyViolationException {

        // maybe not really needed; the update is also done at the beginning of the clockwork run
        context.updateSystemConfiguration(result);

        LensFocusContext<F> focusContext = context.getFocusContext();
        if (focusContext != null) {
            beans.contextLoader.updateArchetypePolicyAndRelatives(focusContext, false, task, result);
        }

        SystemConfigurationType systemConfiguration = context.getSystemConfigurationBean();
        if (systemConfiguration != null && context.getAccountSynchronizationSettings() == null) {
            ProjectionPolicyType globalSettings = systemConfiguration.getGlobalAccountSynchronizationSettings();
            LOGGER.trace("Applying globalAccountSynchronizationSettings to context: {}", globalSettings);
            context.setAccountSynchronizationSettings(globalSettings);
        }

        beans.contextLoader.loadSecurityPolicy(context, task, result);
    }

    /**
     * Removes projection contexts that are not fresh.
     * These are usually artifacts left after the context reload. E.g. an account that used to be linked to a user before
     * but was removed in the meantime.
     */
    private void removeRottenContexts() {
        LOGGER.trace("Starting the removal of rotten projection contexts");
        int removed = 0;
        Iterator<LensProjectionContext> projectionIterator = context.getProjectionContextsIterator();
        while (projectionIterator.hasNext()) {
            LensProjectionContext projectionContext = projectionIterator.next();
            if (!ObjectDelta.isEmpty(projectionContext.getPrimaryDelta())) {
                // We must never remove contexts with primary delta. Even though it fails later on.
                // What the user wishes should be done (or at least attempted) regardless of the consequences.
                // Vox populi vox dei.
                LOGGER.trace(" - not removing the context with a primary delta {}", projectionContext);
                continue;
            }
            if (projectionContext.getWave() >= context.getExecutionWave()) {
                // We must not remove context from this and later execution waves. These haven't had the
                // chance to be executed yet
                LOGGER.trace(" - not removing the context with projection wave {} not less than the current one ({}): {}",
                        projectionContext, projectionContext.getWave(), context.getExecutionWave());
                continue;
            }
            if (projectionContext.isHigherOrder()) {
                // HACK never rot higher-order context. TODO: check if lower-order context is rotten, the also rot this one
                LOGGER.trace(" - not removing a higher-order context: {}", projectionContext);
                continue;
            }
            if (projectionContext.isFresh()) {
                LOGGER.trace(" - not removing a fresh context: {}", projectionContext);
                continue;
            }

            LOGGER.trace(" + removing rotten context {}", projectionContext.getHumanReadableName());

            if (projectionContext.isToBeArchived()) {
                context.getHistoricResourceObjects().add(projectionContext.getKey());
            }

            context.getRottenExecutedDeltas().addAll(
                    projectionContext.getExecutedDeltas());

            projectionIterator.remove();
            removed++;
        }
        LOGGER.trace("Finished the removal of rotten projection contexts; removed: {}", removed);
    }
}
