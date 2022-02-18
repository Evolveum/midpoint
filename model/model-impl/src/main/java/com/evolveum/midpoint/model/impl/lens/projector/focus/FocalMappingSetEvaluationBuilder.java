/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.List;

import com.evolveum.midpoint.model.common.mapping.MappingEvaluationEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.FocalMappingEvaluationRequest;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.TargetObjectSpecification;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;

/**
 * Builder for {@link FocalMappingSetEvaluation} objects.
 *
 * @param <F> type of the focus, in which the whole operation is carried out, i.e. type of {@link LensContext}
 * @param <T> type of the target object
 */
public final class FocalMappingSetEvaluationBuilder<F extends AssignmentHolderType, T extends AssignmentHolderType> {

    ModelBeans beans;
    LensContext<F> context;
    List<? extends FocalMappingEvaluationRequest<?, ?>> evaluationRequests;
    ObjectTemplateMappingEvaluationPhaseType phase;
    ObjectDeltaObject<F> focusOdo;
    TargetObjectSpecification<T> targetSpecification;
    FocalMappingSetEvaluation.TripleCustomizer<?, ?> tripleCustomizer;
    FocalMappingSetEvaluation.EvaluatedMappingConsumer mappingConsumer;
    int iteration;
    String iterationToken;
    MappingEvaluationEnvironment env;
    OperationResult result;

    FocalMappingSetEvaluationBuilder() {
    }

    public FocalMappingSetEvaluationBuilder<F, T> beans(ModelBeans val) {
        beans = val;
        return this;
    }

    public FocalMappingSetEvaluationBuilder<F, T> context(LensContext<F> val) {
        context = val;
        return this;
    }

    FocalMappingSetEvaluationBuilder<F, T> evaluationRequests(List<? extends FocalMappingEvaluationRequest<?, ?>> val) {
        evaluationRequests = val;
        return this;
    }

    FocalMappingSetEvaluationBuilder<F, T> phase(ObjectTemplateMappingEvaluationPhaseType val) {
        phase = val;
        return this;
    }

    FocalMappingSetEvaluationBuilder<F, T> focusOdo(ObjectDeltaObject<F> val) {
        focusOdo = val;
        return this;
    }

    FocalMappingSetEvaluationBuilder<F, T> targetSpecification(TargetObjectSpecification<T> val) {
        targetSpecification = val;
        return this;
    }

    FocalMappingSetEvaluationBuilder<F, T> tripleCustomizer(FocalMappingSetEvaluation.TripleCustomizer<?, ?> val) {
        tripleCustomizer = val;
        return this;
    }

    FocalMappingSetEvaluationBuilder<F, T> mappingConsumer(FocalMappingSetEvaluation.EvaluatedMappingConsumer val) {
        mappingConsumer = val;
        return this;
    }

    public FocalMappingSetEvaluationBuilder<F, T> iteration(int val) {
        iteration = val;
        return this;
    }

    public FocalMappingSetEvaluationBuilder<F, T> iterationToken(String val) {
        iterationToken = val;
        return this;
    }

    public FocalMappingSetEvaluationBuilder<F, T> env(MappingEvaluationEnvironment val) {
        env = val;
        return this;
    }

    public FocalMappingSetEvaluationBuilder<F, T> result(OperationResult val) {
        result = val;
        return this;
    }

    public FocalMappingSetEvaluation<F, T> build() {
        return new FocalMappingSetEvaluation<>(this);
    }
}
