/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ExpressionEnvironment;
import com.evolveum.midpoint.task.api.Task;

/**
 * Describes an environment in which an {@link Expression} is evaluated.
 * Points to lens/projection context, and/or the mapping involved (if applicable).
 *
 * Contains current task and operation result (if known - but it is usually so) - inheriting from {@link ExpressionEnvironment}.
 *
 * Usually contained in some kind of a thread-local holder.
 *
 * @author semancik
 *
 * @param <V> type of {@link PrismValue} the mapping produces
 * @param <D> type of {@link ItemDefinition} of the item the mapping produces
 */
public class ModelExpressionEnvironment<V extends PrismValue, D extends ItemDefinition<?>>
        extends ExpressionEnvironment {

    private final ModelContext<?> lensContext;
    private final ModelProjectionContext projectionContext;
    private final Mapping<V, D> mapping;

    private ModelExpressionEnvironment(
            ModelContext<?> lensContext,
            ModelProjectionContext projectionContext,
            Mapping<V, D> mapping,
            Task currentTask, OperationResult currentResult) {
        super(currentTask, currentResult);
        this.lensContext = lensContext;
        this.projectionContext = projectionContext;
        this.mapping = mapping;
    }

    public ModelExpressionEnvironment(Task currentTask, OperationResult currentResult) {
        this(null, null, null, currentTask, currentResult);
    }

    /** Consider using {@link ExpressionEnvironmentBuilder} instead. */
    public ModelExpressionEnvironment(
            ModelContext<?> lensContext,
            ModelProjectionContext projectionContext,
            Task currentTask,
            OperationResult currentResult) {
        this(lensContext, projectionContext, null, currentTask, currentResult);
    }

    public ModelContext<?> getLensContext() {
        return lensContext;
    }

    public ModelProjectionContext getProjectionContext() {
        return projectionContext;
    }

    public Mapping<V, D> getMapping() {
        return mapping;
    }

    @Override
    public String toString() {
        return "ModelExpressionEnvironment(lensContext=" + lensContext + ", projectionContext="
                + projectionContext + ", currentResult=" + getCurrentResult() + ", currentTask=" + getCurrentTask()
                + ")";
    }

    public static final class ExpressionEnvironmentBuilder<V extends PrismValue, D extends ItemDefinition<?>> {
        private ModelContext<?> lensContext;
        private ModelProjectionContext projectionContext;
        private Mapping<V, D> mapping;
        private OperationResult currentResult;
        private Task currentTask;

        public ExpressionEnvironmentBuilder<V, D> lensContext(ModelContext<?> lensContext) {
            this.lensContext = lensContext;
            return this;
        }

        public ExpressionEnvironmentBuilder<V, D> projectionContext(ModelProjectionContext projectionContext) {
            this.projectionContext = projectionContext;
            return this;
        }

        public ExpressionEnvironmentBuilder<V, D> mapping(Mapping<V, D> mapping) {
            this.mapping = mapping;
            return this;
        }

        public ExpressionEnvironmentBuilder<V, D> currentResult(OperationResult currentResult) {
            this.currentResult = currentResult;
            return this;
        }

        public ExpressionEnvironmentBuilder<V, D> currentTask(Task currentTask) {
            this.currentTask = currentTask;
            return this;
        }

        public ExpressionEnvironmentBuilder<V, D> provideExtraOptions(@NotNull ExtraOptionsProvider<V, D> provider) {
            return provider.provide(this);
        }

        public ModelExpressionEnvironment<V, D> build() {
            return new ModelExpressionEnvironment<>(lensContext, projectionContext, mapping, currentTask, currentResult);
        }
    }

    public interface ExtraOptionsProvider<V extends PrismValue, D extends ItemDefinition<?>> {

        ExpressionEnvironmentBuilder<V, D> provide(ExpressionEnvironmentBuilder<V, D> builder);

        static <V extends PrismValue, D extends ItemDefinition<?>> ExtraOptionsProvider<V, D> forProjectionContext(
                @NotNull ModelProjectionContext projectionContext) {
            return builder -> builder
                    .projectionContext(projectionContext)
                    .lensContext(projectionContext.getModelContext());
        }

        static <V extends PrismValue, D extends ItemDefinition<?>> ExtraOptionsProvider<V, D> forModelContext(
                @NotNull ModelContext<?> modelContext) {
            return builder -> builder.lensContext(modelContext);
        }

        static <V extends PrismValue, D extends ItemDefinition<?>> ExtraOptionsProvider<V, D> empty() {
            return builder -> builder;
        }
    }
}
