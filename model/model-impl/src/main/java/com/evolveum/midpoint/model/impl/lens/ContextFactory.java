/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.model.api.context.ProjectionContextKeyFactory;
import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.processor.ShadowCoordinatesQualifiedObjectDelta;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * @author semancik
 *
 */
@Component
public class ContextFactory {

    private static final Trace LOGGER = TraceManager.getTrace(ContextFactory.class);

    @Autowired PrismContext prismContext;
    @Autowired private ProvisioningService provisioningService;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired Protector protector;
    @Autowired ProjectionContextKeyFactory projectionContextKeyFactory;

    /**
     * Creates a {@link LensContext} from a set of deltas (to be executed or previewed).
     */
    public <F extends ObjectType> LensContext<F> createContext(
            @NotNull Collection<ObjectDelta<? extends ObjectType>> deltas,
            @Nullable ModelExecuteOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {

        CategorizedDeltas<F> categorizedDeltas = new CategorizedDeltas<>(deltas);

        LensContext<F> context = new LensContext<>(categorizedDeltas.focusContextClass, task.getExecutionMode());
        context.setChannel(task.getChannel());
        context.setOptions(options);
        context.setDoReconciliationForAllProjections(ModelExecuteOptions.isReconcile(options));

        if (categorizedDeltas.focusContextDelta != null) {
            LensFocusContext<F> focusContext = context.createFocusContext();
            focusContext.setPrimaryDelta(categorizedDeltas.focusContextDelta);
        }

        for (ObjectDelta<ShadowType> projectionDelta : categorizedDeltas.projectionDeltas) {
            // We are little bit more liberal regarding projection deltas.
            // If the deltas represent shadows we tolerate missing attribute definitions.
            // We try to add the definitions by calling provisioning.
            // (Note that provisioning may fetch the shadow.)
            provisioningService.applyDefinition(projectionDelta, task, result);

            LensProjectionContext projectionContext =
                    context.createProjectionContext(
                            createKey(projectionDelta, task, result));

            if (context.isDoReconciliationForAllProjections()) {
                projectionContext.setDoReconciliation(true);
            }
            projectionContext.setPrimaryDelta(projectionDelta);
        }

        // This forces context reload before the next projection
        context.rot("context initialization");

        context.finishBuild();
        return context;
    }

    private @NotNull ProjectionContextKey createKey(
            @NotNull ObjectDelta<ShadowType> delta,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        if (delta instanceof ShadowCoordinatesQualifiedObjectDelta<ShadowType> qualifiedDelta) {
            ResourceShadowCoordinates coordinates = MiscUtil.requireNonNull(qualifiedDelta.getCoordinates(),
                    () -> new IllegalArgumentException("ShadowCoordinatesQualifiedObjectDelta without coordinates: " + delta));
            // Let us be strict here. This feature is not documented (probably not used) anyway, so we can safely do it.
            argCheck(coordinates.isTypeSpecified(), "No object type specified in the delta coordinates: %s", coordinates);
            return ProjectionContextKey.fromCoordinates(coordinates);
        }
        ShadowType shadow;
        if (delta.isAdd()) {
            shadow = delta.getObjectToAdd().asObjectable();
            argCheck(ShadowUtil.isClassified(shadow),
                    "Shadow to be added is not classified. Since midPoint 4.6 it should be. Please set the kind and"
                            + " intent appropriately. Shadow: %s", shadow);
            return ProjectionContextKey.fromClassifiedShadow(shadow);
        } else {
            String oid = MiscUtil.requireNonNull(
                    delta.getOid(),
                    () -> new IllegalArgumentException("Non-add delta " + delta + " does not have an OID"));
            try {
                // TODO We should call the provisioning service here instead. (Or do the whole thing more intelligently, like
                //  asking for the basic items (resource, OC, kind, intent) first, and only if they are not available, checking
                //  with the provisioning.) The reason why calling repository in the current form is bad is that the
                //  representation of attributes in the repository and in the provisioning API levels now differs significantly.
                shadow = repositoryService
                        .getObject(
                                ShadowType.class,
                                oid,
                                SchemaService.get().getOperationOptionsBuilder()
                                        .allowNotFound()
                                        .readOnly()
                                        .build(),
                                result)
                        .asObjectable();
                return projectionContextKeyFactory.createKey(shadow, task, result);
            } catch (ObjectNotFoundException e) {
                if (delta.isDelete()) {
                    LOGGER.debug("Object-to-be deleted does not exist: {}", delta);
                    return ProjectionContextKey.missing();
                } else {
                    throw e;
                }
            }
        }
    }

    public <F extends ObjectType, O extends ObjectType> LensContext<F> createRecomputeContext(
            @NotNull PrismObject<O> object,
            ModelExecuteOptions options,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        Class<O> typeClass = Objects.requireNonNull(object.getCompileTimeClass(), "no object class");
        LensContext<F> context;
        if (AssignmentHolderType.class.isAssignableFrom(typeClass)) {
            //noinspection unchecked
            context = createRecomputeFocusContext((Class<F>)typeClass, (PrismObject<F>) object, options, task);
        } else if (ShadowType.class.isAssignableFrom(typeClass)) {
            context = createRecomputeProjectionContext((ShadowType) object.asObjectable(), options, task, result);
        } else {
            throw new IllegalArgumentException("Cannot create recompute context for "+object);
        }
        context.setOptions(options);
        context.setLazyAuditRequest(true);
        return context;
    }

    private <F extends ObjectType> LensContext<F> createRecomputeFocusContext(
            Class<F> focusType, PrismObject<F> focus, ModelExecuteOptions options, @NotNull Task task) {
        LensContext<F> lensContext = new LensContext<>(focusType, task.getExecutionMode());
        LensFocusContext<F> focusContext = lensContext.createFocusContext();
        focusContext.setInitialObject(focus);
        focusContext.setOid(focus.getOid());
        lensContext.setChannel(SchemaConstants.CHANNEL_RECOMPUTE_URI);
        lensContext.setDoReconciliationForAllProjections(ModelExecuteOptions.isReconcile(options));
        return lensContext;
    }

    private <F extends ObjectType> LensContext<F> createRecomputeProjectionContext(
            @NotNull ShadowType shadow, ModelExecuteOptions options, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        provisioningService.applyDefinition(shadow.asPrismObject(), task, result);
        LensContext<F> lensContext = new LensContext<>(null, task.getExecutionMode());
        LensProjectionContext projectionContext =
                lensContext.createProjectionContext(
                        projectionContextKeyFactory.createKey(shadow, task, result));
        projectionContext.setInitialObject(shadow.asPrismObject());
        projectionContext.setOid(shadow.getOid());
        projectionContext.setDoReconciliation(ModelExecuteOptions.isReconcile(options));
        lensContext.setChannel(SchemaConstants.CHANNEL_RECOMPUTE_URI);
        return lensContext;
    }

     /**
     * Creates empty lens context for synchronization purposes, filling in only the very basic metadata (such as channel).
     */
    public <F extends ObjectType> LensContext<F> createSyncContext(
            Class<F> focusClass, ResourceObjectShadowChangeDescription change, Task task) {
        LensContext<F> context = new LensContext<>(focusClass, task.getExecutionMode());
        context.setChannel(change.getSourceChannel());
        return context;
    }

    /**
     * Deltas categorized into focus/projections/configuration ones. Plus info about focus class.
     */
    @SuppressWarnings("FieldCanBeLocal")
    private static class CategorizedDeltas<F extends ObjectType> {

        /** Deltas of {@link FocusType} - they will form {@link LensFocusContext} object. */
        private final List<ObjectDelta<F>> focusDeltas = new ArrayList<>();

        /** Deltas of {@link ShadowType} - they will form {@link LensProjectionContext} objects. */
        private final List<ObjectDelta<ShadowType>> projectionDeltas = new ArrayList<>();

        /** Other deltas - they will form {@link LensFocusContext} object. */
        private final List<ObjectDelta<? extends ObjectType>> confDeltas = new ArrayList<>();

        private final ObjectDelta<F> focusContextDelta;
        private final Class<F> focusContextClass;

        /**
         * Sorts the deltas provided by client into categories; checking also object type compatibility.
         */
        CategorizedDeltas(Collection<ObjectDelta<? extends ObjectType>> deltas) {
            for (ObjectDelta<? extends ObjectType> delta: deltas) {
                Class<? extends ObjectType> typeClass = delta.getObjectTypeClass();
                Validate.notNull(typeClass, "Object type class is null in " + delta);
                if (FocusType.class.isAssignableFrom(typeClass)) {
                    if (!delta.isAdd() && delta.getOid() == null) {
                        throw new IllegalArgumentException("Delta " + delta + " does not have an OID");
                    }
                    if (InternalsConfig.consistencyChecks) {
                        // Focus delta has to be complete now with all the definition already in place
                        delta.checkConsistence(
                                false, true, true, ConsistencyCheckScope.THOROUGH);
                    } else {
                        // TODO is this necessary? Perhaps it would be sufficient to check on model/repo entry
                        delta.checkConsistence(ConsistencyCheckScope.MANDATORY_CHECKS_ONLY);
                    }
                    // Make sure we clone request delta here. Clockwork will modify the delta (e.g. normalize it).
                    // And we do not want to touch request delta. It may even be immutable.
                    //noinspection unchecked
                    focusDeltas.add((ObjectDelta<F>) delta.clone());
                } else if (ShadowType.class.isAssignableFrom(typeClass)) {
                    // Make sure we clone request delta here. Clockwork will modify the delta (e.g. normalize it).
                    // And we do not want to touch request delta. It may even be immutable.
                    //noinspection unchecked
                    projectionDeltas.add((ObjectDelta<ShadowType>) delta.clone());
                } else {
                    // Make sure we clone request delta here. Clockwork will modify the delta (e.g. normalize it).
                    // And we do not want to touch request delta. It may even be immutable.
                    confDeltas.add(delta.clone());
                }
            }

            if (!confDeltas.isEmpty()) {
                argCheck(focusDeltas.isEmpty(),
                        "Mixed configuration and focus deltas in one executeChanges invocation: %s/%s",
                        confDeltas, focusDeltas);
                argCheck(projectionDeltas.isEmpty(),
                        "Mixed configuration and projection deltas in one executeChanges invocation: %s/%s",
                        confDeltas, projectionDeltas);
                argCheck(confDeltas.size() == 1,
                        "More than one configuration delta in a single executeChanges invocation: %s", confDeltas);
                //noinspection unchecked
                focusContextDelta = (ObjectDelta<F>) confDeltas.get(0);
            } else if (!focusDeltas.isEmpty()) {
                argCheck(focusDeltas.size() == 1,
                        "More than one focus delta used in model operation: %s", focusDeltas);
                focusContextDelta = focusDeltas.get(0);
            } else {
                focusContextDelta = null;
            }
            focusContextClass = focusContextDelta != null ? focusContextDelta.getObjectTypeClass() : null;
        }
    }
}
