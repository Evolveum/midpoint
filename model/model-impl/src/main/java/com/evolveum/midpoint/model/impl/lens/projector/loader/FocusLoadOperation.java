/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.loader;

import static com.evolveum.midpoint.model.impl.lens.LensUtil.getExportType;
import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchCollection;
import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusLoadedTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Responsible for loading the focus context. Intentionally not public.
 */
class FocusLoadOperation<F extends ObjectType> {

    private static final Trace LOGGER = TraceManager.getTrace(FocusLoadOperation.class);

    // For backwards compatibility reasons we use old class and method name here.
    private static final String OP_LOAD = ContextLoader.class.getName() + "." + "determineFocusContext";

    @NotNull private final LensContext<F> context;
    @NotNull private final Task task;
    @NotNull private final ModelBeans beans;

    /** Trace of the operation (if any). */
    private FocusLoadedTraceType trace;

    FocusLoadOperation(@NotNull LensContext<F> context, @NotNull Task task) {
        this.context = context;
        this.task = task;
        this.beans = ModelBeans.get();
    }

    /**
     * Tries to load focus context from oid, delta, projections (e.g. by determining account owners).
     */
    public void load(OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        OperationResult result = parentResult.subresult(OP_LOAD)
                .setMinor()
                .build();
        createTraceIfNeeded(result);
        try {
            LensFocusContext<F> focusContext;
            if (context.getFocusContext() != null) {
                focusContext = context.getFocusContext();
            } else {
                focusContext = determineFocusContextFromProjections(result);
            }

            if (focusContext == null) {
                result.addReturnComment("Nothing to load");
                return;
            }

            // Make sure that we RELOAD the focus object if the context is not fresh,
            // as the focus may have changed in the meantime.
            if (focusContext.getObjectCurrent() != null && focusContext.isFresh()) {
                result.addReturnComment("Already loaded");
                return;
            }

            ObjectDelta<F> objectDelta = focusContext.getSummaryDelta(); // TODO check this
            if (ObjectDelta.isAdd(objectDelta) && focusContext.getExecutedDeltas().isEmpty()) {
                // We're adding the focal object. No need to load it, it is in the delta.
                focusContext.setFresh(true);
                result.addReturnComment("Present in delta");
                return;
            }

            if (focusContext.isDeleted()) {
                focusContext.clearCurrentObject();
                focusContext.setFresh(true);
                result.addReturnComment("Not loading as the focus was deleted");
                return;
            }

            PrismObject<F> object = reallyLoadFocus(focusContext, result);
            focusContext.setLoadedObject(object);
            focusContext.setFresh(true);
            LOGGER.trace("Focal object loaded: {}", object);

        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            recordTraceAtEnd(result);
            result.computeStatusIfUnknown();
        }
    }

    private @NotNull PrismObject<F> reallyLoadFocus(LensFocusContext<F> focusContext, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        String focusOid = focusContext.getOid();
        argCheck(!StringUtils.isBlank(focusOid), "No OID in primary focus delta");

        PrismObject<F> object;
        if (ObjectTypes.isClassManagedByProvisioning(focusContext.getObjectTypeClass())) {
            var options = createNoFetchCollection(); // or also readonly?
            object = beans.provisioningService.getObject(focusContext.getObjectTypeClass(), focusOid, options, task, result);
            result.addReturnComment("Loaded via provisioning");
        } else {
            // Always load a complete object here, including the not-returned-by-default properties.
            // This is temporary measure to make sure that the mappings will have all they need.
            // See MID-2635
            object = beans.cacheRepositoryService.getObject(
                    focusContext.getObjectTypeClass(),
                    focusOid,
                    GetOperationOptionsBuilder.create()
                            .retrieve()
                            .readOnly()
                            .build(),
                    result);
            result.addReturnComment("Loaded from repository");
        }
        setLoadedFocusInTrace(object);
        return object;
    }

    private void createTraceIfNeeded(OperationResult result) throws SchemaException {
        if (result.isTracingAny(FocusLoadedTraceType.class)) {
            trace = new FocusLoadedTraceType();
            if (result.isTracingNormal(FocusLoadedTraceType.class)) {
                trace.setInputLensContextText(context.debugDump());
            }
            trace.setInputLensContext(context.toBean(getExportType(trace, result)));
            result.addTrace(trace);
        } else {
            trace = null;
        }
    }

    private void setLoadedFocusInTrace(PrismObject<F> object) {
        if (trace != null) {
            trace.setFocusLoadedRef(ObjectTypeUtil.createObjectRefWithFullObject(object));
        }
    }

    private void recordTraceAtEnd(OperationResult result) throws SchemaException {
        if (trace != null) {
            if (result.isTracingNormal(FocusLoadedTraceType.class)) {
                trace.setOutputLensContextText(context.debugDump());
            }
            trace.setOutputLensContext(context.toBean(getExportType(trace, result)));
        }
    }

    private LensFocusContext<F> determineFocusContextFromProjections(OperationResult result) {
        String focusOid = null;
        LensProjectionContext projectionContextThatYieldedFocusOid = null;
        PrismObject<F> focus = null;
        for (LensProjectionContext projectionContext : context.getProjectionContexts()) {
            String projectionOid = projectionContext.getOid();
            if (projectionOid == null) {
                continue;
            }

            PrismObject<F> shadowOwner = findShadowOwner(projectionOid, result);
            if (shadowOwner == null) {
                continue;
            }

            if (focusOid == null || focusOid.equals(shadowOwner.getOid())) {
                focusOid = shadowOwner.getOid();
                focus = shadowOwner;
                projectionContextThatYieldedFocusOid = projectionContext;
            } else {
                throw new IllegalArgumentException("The context does not have explicit focus. Attempt to determine focus failed because two " +
                        "projections points to different foci: " + projectionContextThatYieldedFocusOid + "->" + focusOid + "; " +
                        projectionContext + "->" + shadowOwner);
            }
        }

        if (focusOid != null) {
            LensFocusContext<F> focusCtx = context.getOrCreateFocusContext(focus.getCompileTimeClass());
            focusCtx.setOid(focusOid);
            return focusCtx;
        } else {
            return null;
        }
    }

    private PrismObject<F> findShadowOwner(String projectionOid, OperationResult result) {
        // TODO change to regular search
        //noinspection unchecked
        return (PrismObject<F>) beans.cacheRepositoryService.searchShadowOwner(
                projectionOid,
                GetOperationOptions.createAllowNotFoundCollection(),
                result);
    }
}
