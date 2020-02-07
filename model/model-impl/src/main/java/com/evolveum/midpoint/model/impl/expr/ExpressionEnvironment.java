/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.expr;

import com.evolveum.midpoint.model.api.context.Mapping;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
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

    private LensContext<F> lensContext;
    private LensProjectionContext projectionContext;
    private Mapping<V, D> mapping;
    private OperationResult currentResult;
    private Task currentTask;

    public ExpressionEnvironment() {
    }

    public ExpressionEnvironment(Task currentTask, OperationResult currentResult) {
        this.currentResult = currentResult;
        this.currentTask = currentTask;
    }

    public ExpressionEnvironment(LensContext<F> lensContext, LensProjectionContext projectionContext,
            Task currentTask, OperationResult currentResult) {
        this.lensContext = lensContext;
        this.projectionContext = projectionContext;
        this.currentResult = currentResult;
        this.currentTask = currentTask;
    }

    public ExpressionEnvironment(LensContext<F> lensContext, LensProjectionContext projectionContext, Mapping<V, D> mapping,
            Task currentTask, OperationResult currentResult) {
        this.lensContext = lensContext;
        this.projectionContext = projectionContext;
        this.mapping = mapping;
        this.currentResult = currentResult;
        this.currentTask = currentTask;
    }

    public LensContext<F> getLensContext() {
        return lensContext;
    }

    public void setLensContext(LensContext<F> lensContext) {
        this.lensContext = lensContext;
    }

    public LensProjectionContext getProjectionContext() {
        return projectionContext;
    }

    public void setProjectionContext(LensProjectionContext projectionContext) {
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
