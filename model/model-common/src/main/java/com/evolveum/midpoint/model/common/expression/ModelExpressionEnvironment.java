/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression;

import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.task.api.ExpressionEnvironment;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

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
 * @param <F> type of focus object if {@link ModelContext} is involved
 * @param <V> type of {@link PrismValue} the mapping produces
 * @param <D> type of {@link ItemDefinition} of the item the mapping produces
 */
public class ModelExpressionEnvironment<F extends ObjectType, V extends PrismValue, D extends ItemDefinition<?>>
        extends ExpressionEnvironment {

    private final ModelContext<F> lensContext;
    private final ModelProjectionContext projectionContext;
    private final Mapping<V, D> mapping;

    private ModelExpressionEnvironment(
            ModelContext<F> lensContext,
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
            ModelContext<F> lensContext,
            ModelProjectionContext projectionContext,
            Task currentTask,
            OperationResult currentResult) {
        this(lensContext, projectionContext, null, currentTask, currentResult);
    }

    public ModelContext<F> getLensContext() {
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

    public static final class ExpressionEnvironmentBuilder
            <F extends ObjectType, V extends PrismValue, D extends ItemDefinition<?>> {
        private ModelContext<F> lensContext;
        private ModelProjectionContext projectionContext;
        private Mapping<V, D> mapping;
        private OperationResult currentResult;
        private Task currentTask;

        public ExpressionEnvironmentBuilder<F, V, D>  lensContext(ModelContext<F> lensContext) {
            this.lensContext = lensContext;
            return this;
        }

        public ExpressionEnvironmentBuilder<F, V, D>  projectionContext(ModelProjectionContext projectionContext) {
            this.projectionContext = projectionContext;
            return this;
        }

        public ExpressionEnvironmentBuilder<F, V, D>  mapping(Mapping<V, D> mapping) {
            this.mapping = mapping;
            return this;
        }

        public ExpressionEnvironmentBuilder<F, V, D>  currentResult(OperationResult currentResult) {
            this.currentResult = currentResult;
            return this;
        }

        public ExpressionEnvironmentBuilder<F, V, D>  currentTask(Task currentTask) {
            this.currentTask = currentTask;
            return this;
        }

        public ModelExpressionEnvironment<F, V, D> build() {
            return new ModelExpressionEnvironment<>(lensContext, projectionContext, mapping, currentTask, currentResult);
        }
    }
}
