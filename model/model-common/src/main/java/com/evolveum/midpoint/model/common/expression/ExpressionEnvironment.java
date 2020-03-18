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
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 *
 */
public class ExpressionEnvironment<F extends ObjectType,V extends PrismValue, D extends ItemDefinition> {

    private ModelContext<F> lensContext;
    private ModelProjectionContext projectionContext;
    private Mapping<V, D> mapping;
    private OperationResult currentResult;
    private Task currentTask;

    public ExpressionEnvironment() {
    }

    public ExpressionEnvironment(Task currentTask, OperationResult currentResult) {
        this.currentResult = currentResult;
        this.currentTask = currentTask;
    }

    public ExpressionEnvironment(ModelContext<F> lensContext, ModelProjectionContext projectionContext,
            Task currentTask, OperationResult currentResult) {
        this.lensContext = lensContext;
        this.projectionContext = projectionContext;
        this.currentResult = currentResult;
        this.currentTask = currentTask;
    }

    public ExpressionEnvironment(ModelContext<F> lensContext, ModelProjectionContext projectionContext, Mapping<V, D> mapping,
            Task currentTask, OperationResult currentResult) {
        this.lensContext = lensContext;
        this.projectionContext = projectionContext;
        this.mapping = mapping;
        this.currentResult = currentResult;
        this.currentTask = currentTask;
    }

    public ModelContext<F> getLensContext() {
        return lensContext;
    }

    public void setLensContext(ModelContext<F> lensContext) {
        this.lensContext = lensContext;
    }

    public ModelProjectionContext getProjectionContext() {
        return projectionContext;
    }

    public void setProjectionContext(ModelProjectionContext projectionContext) {
        this.projectionContext = projectionContext;
    }

    public Mapping<V, D> getMapping() {
        return mapping;
    }

    public void setMapping(Mapping<V, D> mapping) {
        this.mapping = mapping;
    }

    public OperationResult getCurrentResult() {
        return currentResult;
    }

    public void setCurrentResult(OperationResult currentResult) {
        this.currentResult = currentResult;
    }

    public Task getCurrentTask() {
        return currentTask;
    }

    public void setCurrentTask(Task currentTask) {
        this.currentTask = currentTask;
    }

    @Override
    public String toString() {
        return "ExpressionEnvironment(lensContext=" + lensContext + ", projectionContext="
                + projectionContext + ", currentResult=" + currentResult + ", currentTask=" + currentTask
                + ")";
    }

}
