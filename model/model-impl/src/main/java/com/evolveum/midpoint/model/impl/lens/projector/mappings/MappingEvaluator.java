/*
 * Copyright (c) 2013-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.lens.projector.mappings;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;

import com.evolveum.midpoint.model.impl.lens.LensContext;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.api.util.ClockworkInspector;
import com.evolveum.midpoint.model.api.util.MappingInspector;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment.ExpressionEnvironmentBuilder;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment.ExtraOptionsProvider;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.projector.focus.ProjectionMappingSetEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.StatisticsCollector;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Evaluates {@link Mapping} objects.
 *
 * Currently, it is largely a _wrapper_ around {@link MappingImpl#evaluate(Task, OperationResult)} method.
 *
 * Responsibilities besides calling that method:
 *
 * 1. Checking if mapping is enabled.
 * 2. Creating and pushing {@link ModelExpressionEnvironment} to {@link ExpressionEnvironmentThreadLocalHolder}
 * (and popping it afterwards).
 * 3. Informing the watchers:
 *    - recording mapping evaluation in {@link StatisticsCollector},
 *    - invoking {@link ClockworkInspector}.
 *
 * This class _no longer_ parses mappings i.e. no longer translates {@link AbstractMappingType} objects into
 * {@link Mapping} objects. See {@link ProjectionMappingSetEvaluator} for this.
 *
 * @author Radovan Semancik
 */
@Component
public class MappingEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(MappingEvaluator.class);

    /**
     * Evaluates parsed mapping in given lens and projection context (if available - they may be null).
     */
    public <V extends PrismValue, D extends ItemDefinition<?>> void evaluateMapping(
            @NotNull MappingImpl<V, D> mapping,
            @NotNull EvaluationContext<V, D> context,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        if (!mapping.isEnabled()) {
            LOGGER.debug("Skipping mapping evaluation, because mapping is disabled: {}", mapping);
            return;
        }

        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                new ExpressionEnvironmentBuilder<V, D>()
                        .mapping(mapping)
                        .currentResult(result)
                        .currentTask(task)
                        .provideExtraOptions(context.expressionEnvironmentExtraOptionsProvider)
                        .build());

        long start = System.currentTimeMillis();
        try {
            task.recordStateMessage("Started evaluation of mapping " + mapping.getMappingContextDescription() + ".");
            mapping.evaluate(task, result);
            task.recordStateMessage("Successfully finished evaluation of mapping " + mapping.getMappingContextDescription()
                    + " in " + (System.currentTimeMillis() - start) + " ms.");
        } catch (Exception e) {
            task.recordStateMessage("Evaluation of mapping " + mapping.getMappingContextDescription() + " finished with error in "
                    + (System.currentTimeMillis() - start) + " ms.");
            //noinspection IfStatementWithIdenticalBranches
            if (e instanceof ExpressionEvaluationException) {
                // The exception probably contains the correct context description
                throw e;
            } else {
                // Let us add the context information , as it is probably not there
                MiscUtil.throwAsSame(e, e.getMessage() + " in " + mapping.getContextDescription());
                throw e; // To make compiler happy
            }
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
            recordMappingOperation(mapping, task, start);
            context.mappingInspector.afterMappingEvaluation(mapping);
        }
    }

    private <V extends PrismValue, D extends ItemDefinition<?>> void recordMappingOperation(
            MappingImpl<V, D> mapping, Task task, long start) {
        try {
            String objectOid, objectName, objectTypeName;
            ObjectType originObject = mapping.getOriginObject();
            if (originObject != null) {
                objectOid = originObject.getOid();
                objectName = String.valueOf(originObject.getName());
                objectTypeName = originObject.getClass().getSimpleName();
            } else {
                objectOid = objectName = objectTypeName = null;
            }
            String mappingName = mapping.getItemName() != null ? mapping.getItemName().getLocalPart() : null;
            task.recordMappingOperation(
                    objectOid, objectName, objectTypeName, mappingName, System.currentTimeMillis() - start);
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't record mapping operation on {}", e, mapping);
            // Not propagating the exception, as there's no real harm done.
        }
    }

    public record EvaluationContext<V extends PrismValue, D extends ItemDefinition<?>> (
            @NotNull ExtraOptionsProvider<V, D> expressionEnvironmentExtraOptionsProvider,
            @NotNull MappingInspector mappingInspector) {

        public static <V extends PrismValue, D extends ItemDefinition<?>> EvaluationContext<V, D> forProjectionContext(
                @NotNull ModelProjectionContext projectionContext) {
            return new EvaluationContext<>(
                    ExtraOptionsProvider.forProjectionContext(projectionContext),
                    ((LensContext<?>) projectionContext.getModelContext()).getMappingInspector());
        }

        public static <V extends PrismValue, D extends ItemDefinition<?>> EvaluationContext<V, D> forModelContext(
                @NotNull ModelContext<?> modelContext) {
            return new EvaluationContext<>(
                    ExtraOptionsProvider.forModelContext(modelContext),
                    ((LensContext<?>) modelContext).getMappingInspector());
        }

        public static <V extends PrismValue, D extends ItemDefinition<?>> EvaluationContext<V, D> empty() {
            return new EvaluationContext<>(
                    ExtraOptionsProvider.empty(),
                    MappingInspector.empty());
        }
    }
}
