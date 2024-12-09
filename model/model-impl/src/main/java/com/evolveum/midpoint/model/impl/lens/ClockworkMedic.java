/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens;

import java.util.function.Supplier;

import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.util.*;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProjectorComponentTraceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.util.ClockworkInspector;
import com.evolveum.midpoint.model.api.util.DiagnosticContextManager;
import com.evolveum.midpoint.model.common.util.ProfilingModelInspector;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.DiagnosticContext;
import com.evolveum.midpoint.schema.util.DiagnosticContextHolder;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType;

import javax.xml.datatype.XMLGregorianCalendar;

import static com.evolveum.midpoint.model.impl.lens.LensUtil.getExportType;

/**
 * @author semancik
 *
 */
@Component
public class ClockworkMedic {

    private static final Trace LOGGER = TraceManager.getTrace(ClockworkMedic.class);

    @Autowired private CacheConfigurationManager cacheConfigurationManager;

    public void enterModelMethod(boolean enterCache) {
        if (InternalsConfig.isModelProfiling()) {
            DiagnosticContextManager manager = getDiagnosticContextManager();
            DiagnosticContext ctx;
            if (manager == null) {
                ctx = new ProfilingModelInspector();
                ((ProfilingModelInspector)ctx).recordStart();
            } else {
                ctx = manager.createNewContext();
            }
            DiagnosticContextHolder.push(ctx);
        }

        if (enterCache) {
            RepositoryCache.enterLocalCaches(cacheConfigurationManager);
        }
    }

    public void exitModelMethod(boolean exitCache) {
        if (exitCache) {
            RepositoryCache.exitLocalCaches();
        }

        DiagnosticContext ctx = DiagnosticContextHolder.pop();
        if (ctx != null) {
            DiagnosticContextManager manager = getDiagnosticContextManager();
            if (manager == null) {
                if (ctx instanceof ProfilingModelInspector) {
                    ((ProfilingModelInspector)ctx).recordFinish();
                }
                LOGGER.info("Model diagnostics:{}", ctx.debugDump(1));
            } else {
                manager.processFinishedContext(ctx);
            }
        }
    }

    // Maybe we need to find a better place for this
    private DiagnosticContextManager diagnosticContextManager = null;

    private DiagnosticContextManager getDiagnosticContextManager() {
        return diagnosticContextManager;
    }

    public void setDiagnosticContextManager(DiagnosticContextManager diagnosticContextManager) {
        this.diagnosticContextManager = diagnosticContextManager;
    }

    ClockworkInspector getClockworkInspector() {
        return DiagnosticContextHolder.get(ClockworkInspector.class);
    }


    <F extends ObjectType> void clockworkStart(LensContext<F> context) {
        ClockworkInspector clockworkInspector = getClockworkInspector();
        if (clockworkInspector != null) {
            clockworkInspector.clockworkStart(context);
        }
    }

    public <F extends ObjectType> void clockworkStateSwitch(LensContext<F> contextBefore, ModelState newState) {
        ClockworkInspector clockworkInspector = getClockworkInspector();
        if (clockworkInspector != null) {
            clockworkInspector.clockworkStateSwitch(contextBefore, newState);
        }
    }

    public <F extends ObjectType> void clockworkFinish(LensContext<F> context) {
        ClockworkInspector clockworkInspector = getClockworkInspector();
        if (clockworkInspector != null) {
            clockworkInspector.clockworkFinish(context);
        }
    }

    public <F extends ObjectType> void projectorStart(LensContext<F> context) {
        ClockworkInspector clockworkInspector = getClockworkInspector();
        if (clockworkInspector != null) {
            clockworkInspector.projectorStart(context);
        }
    }

    public <F extends ObjectType> void projectorFinish(LensContext<F> context) {
        ClockworkInspector clockworkInspector = getClockworkInspector();
        if (clockworkInspector != null) {
            clockworkInspector.projectorFinish(context);
        }
    }

    public <F extends ObjectType> void afterMappingEvaluation(LensContext<F> context,
            MappingImpl<?, ?> evaluatedMapping) {
        ClockworkInspector clockworkInspector = getClockworkInspector();
        if (clockworkInspector != null) {
            clockworkInspector.afterMappingEvaluation(context, evaluatedMapping);
        }
    }

    @SuppressWarnings({ "rawtypes", "UnusedReturnValue" })
    public boolean partialExecute(
            String componentName,
            @Nullable ProjectorProcessor processor,
            ProjectionAwareProcessorMethodRef method,
            Supplier<PartialProcessingTypeType> optionSupplier,
            Class<?> executingClass,
            LensContext<?> context,
            LensProjectionContext projectionContext,
            String activityDescription,
            XMLGregorianCalendar now,
            Task task,
            OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
            PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, ConflictDetectedException {
        if (shouldExecute(componentName, processor, context, projectionContext)) {
            partialExecute(componentName, (result1) -> {
                //noinspection unchecked
                method.run(context, projectionContext, activityDescription, now, task, result1);
            }, optionSupplier, executingClass, context, projectionContext, parentResult);
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("rawtypes")
    public boolean partialExecute(String componentName, ProjectorProcessor processor,
            ProcessorMethodRef method, Supplier<PartialProcessingTypeType> optionSupplier,
            Class<?> executingClass, LensContext<?> context, @NotNull String activityDescription,
            XMLGregorianCalendar now, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
            PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, ConflictDetectedException {
        if (shouldExecute(componentName, processor, context, null)) {
            partialExecute(componentName, (result1) -> {
                //noinspection unchecked
                method.run(context, activityDescription, now, task, result1);
            }, optionSupplier, executingClass, context, null, parentResult);
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings({ "UnusedReturnValue", "rawtypes" })
    public boolean partialExecute(String componentName, ProjectorProcessor processor,
            SimplifiedProcessorMethodRef method, Supplier<PartialProcessingTypeType> optionSupplier,
            Class<?> executingClass, LensContext<?> context,
            XMLGregorianCalendar now, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
            PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, ConflictDetectedException {
        if (shouldExecute(componentName, processor, context, null)) {
            partialExecute(componentName, (result1) -> {
                //noinspection unchecked
                method.run(context, now, task, result1);
            }, optionSupplier, executingClass, context, null, parentResult);
            return true;
        } else {
            return false;
        }
    }

    private boolean shouldExecute(
            String componentName, ProjectorProcessor processor, LensContext<?> context, LensProjectionContext projectionContext) {
        if (processor == null) {
            return true;
        }
        ProcessorExecution processorExecution = processor.getClass().getAnnotation(ProcessorExecution.class);
        return processorExecution == null ||
                focusPresenceAndTypeCheckPasses(componentName, context, processorExecution)
                        && focusDeletionCheckPasses(componentName, context.getFocusContext(), processorExecution)
                        && projectionDeletionCheckPasses(componentName, projectionContext, processorExecution)
                        && projectionCurrentCheckPasses(componentName, projectionContext);
    }

    private boolean focusPresenceAndTypeCheckPasses(
            String componentName, LensContext<?> context, ProcessorExecution processorExecution) {
        if (!processorExecution.focusRequired()) {
            // intentionally skipping focus type check
            return true;
        }
        LensFocusContext<?> focusContext = context.getFocusContext();
        if (focusContext == null) {
            LOGGER.trace("Skipping {} processing because focus context is null", componentName);
            return false;
        }
        Class<? extends ObjectType> requiredFocusType = processorExecution.focusType();
        if (!focusContext.isOfType(requiredFocusType)) {
            LOGGER.trace("Skipping {} processing because focus context is not of required focus class ({})",
                    componentName, requiredFocusType.getSimpleName());
            return false;
        }
        return true;
    }

    private boolean focusDeletionCheckPasses(String componentName, LensFocusContext<?> focusContext,
            ProcessorExecution processorExecution) {
        if (focusContext == null) {
            // If we are OK with no focus context, then the deletion is irrelevant
            return true;
        }

        if (!processorExecution.skipWhenFocusDeleted()) {
            return true;
        }

        if (focusContext.isDelete()) {
            LOGGER.trace("Skipping '{}' because focus is being deleted (primary delta)", componentName);
            return false;
        } else {
            return true;
        }
    }

    private boolean projectionDeletionCheckPasses(
            String componentName, LensProjectionContext projectionContext, ProcessorExecution processorExecution) {
        if (processorExecution.skipWhenProjectionDeleted() && projectionContext != null && projectionContext.isDelete()) {
            LOGGER.trace("Skipping '{}' because projection is being deleted", componentName);
            return false;
        } else {
            return true;
        }
    }

    /**
     * Actually, in the current code (4.2) all methods that check this status are within
     * {@link Projector#projectProjection(LensContext, LensProjectionContext, PartialProcessingOptionsType, XMLGregorianCalendar,
     * String, Task, OperationResult)} method - and it checks for both projection completed flag and its execution wave.
     * Nevertheless, let us keep the check here for future use.
     */
    private boolean projectionCurrentCheckPasses(String componentName, LensProjectionContext projectionContext) {
        if (projectionContext != null && !projectionContext.isCurrentForProjection()) {
            LOGGER.trace("Skipping '{}' because projection is not current (already completed or wrong wave)", componentName);
            return false;
        } else {
            return true;
        }
    }

    public void partialExecute(String baseComponentName, ProjectorComponentRunnable runnable,
            Supplier<PartialProcessingTypeType> optionSupplier,
            Class<?> executingClass, LensContext<?> context, LensProjectionContext projectionContext, OperationResult initialParentResult)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
            PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, ConflictDetectedException {

        context.checkAbortRequested();

        OperationResult parentResult;
        if (initialParentResult == null) {
            LOGGER.warn("No parentResult in ClockworkMedic.partialExecute! Creating dummy one");
            parentResult = new OperationResult(ClockworkMedic.class.getName() + ".partialExecute");
        } else {
            parentResult = initialParentResult;
        }

        String componentName;
        if (projectionContext != null) {
            componentName = baseComponentName + " " + projectionContext.getHumanReadableName();
        } else {
            componentName = baseComponentName;
        }
        ClockworkInspector clockworkInspector = getClockworkInspector();
        PartialProcessingTypeType option = optionSupplier.get();
        if (option == PartialProcessingTypeType.SKIP) {
            LOGGER.debug("Skipping projector component {} because partial execution option is set to {}", componentName, option);
            if (clockworkInspector != null) {
                clockworkInspector.projectorComponentSkip(componentName);
            }
        } else {
            String operationName = executingClass.getName() + "." + baseComponentName;
            String qualifier = context.getOperationQualifier();
            if (projectionContext != null) {
                qualifier += "." + projectionContext.getResourceOid() + "." +
                        projectionContext.getKey().getKind() + "." +
                        projectionContext.getKey().getIntent();
            }
            OperationResult result = parentResult.subresult(operationName)
                    .addQualifier(qualifier)
                    .build();
            ProjectorComponentTraceType trace;
            if (result.isTracingAny(ProjectorComponentTraceType.class)) {
                trace = new ProjectorComponentTraceType();
                if (result.isTracingNormal(ProjectorComponentTraceType.class)) {
                    trace.setInputLensContextText(context.debugDump());
                }
                trace.setInputLensContext(context.toBean(getExportType(trace, result)));
                if (projectionContext != null) {
                    trace.setResourceShadowDiscriminator(
                            LensUtil.createDiscriminatorBean(projectionContext.getKey(), context));
                }
                result.addTrace(trace);
            } else {
                trace = null;
            }
            try {
                LOGGER.trace("Projector component started: {}", componentName);
                if (clockworkInspector != null) {
                    clockworkInspector.projectorComponentStart(componentName);
                }
                runnable.run(result);
                LOGGER.trace("Projector component finished: {}", componentName);
            } catch (SchemaException | ObjectNotFoundException | CommunicationException | ConfigurationException |
                    SecurityViolationException | PolicyViolationException | ExpressionEvaluationException |
                    ObjectAlreadyExistsException | ConflictDetectedException | RuntimeException | Error e) {
                LOGGER.trace("Projector component error: {}: {}: {}", componentName, e.getClass().getSimpleName(), e.getMessage());
                result.recordException(e);
                throw e;
            } finally {
                result.close();
                if (trace != null) {
                    if (result.isTracingNormal(ProjectorComponentTraceType.class)) {
                        trace.setOutputLensContextText(context.debugDump());
                    }
                    trace.setOutputLensContext(context.toBean(getExportType(trace, result)));
                }
                if (clockworkInspector != null) {
                    clockworkInspector.projectorComponentFinish(componentName);
                }
            }
        }
    }

    public <F extends ObjectType> void traceContext(Trace logger, String activity, String phase,
            boolean important,  LensContext<F> context, boolean showTriples) {
        if (logger.isTraceEnabled()) {
            logger.trace("Lens context:\n"+
                    "---[ {} context {} ]--------------------------------\n"+
                    "{}\n",
                    activity, phase, context.dump(showTriples));
        }
    }

}
