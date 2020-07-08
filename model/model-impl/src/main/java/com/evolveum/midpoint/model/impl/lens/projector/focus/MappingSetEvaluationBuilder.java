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
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingEvaluationPhaseType;

public final class MappingSetEvaluationBuilder<F extends AssignmentHolderType, T extends AssignmentHolderType> {

    ModelBeans beans;
    LensContext<F> context;
    List<? extends FocalMappingEvaluationRequest<?, ?>> evaluationRequests;
    ObjectTemplateMappingEvaluationPhaseType phase;
    ObjectDeltaObject<F> focusOdo;
    TargetObjectSpecification<T> targetSpecification;
    MappingSetEvaluation.TripleCustomizer<PrismValue, ItemDefinition> tripleCustomizer;
    MappingSetEvaluation.EvaluatedMappingConsumer<PrismValue, ItemDefinition> mappingConsumer;
    int iteration;
    String iterationToken;
    MappingEvaluationEnvironment env;
    OperationResult result;

    MappingSetEvaluationBuilder() {
    }

    public MappingSetEvaluationBuilder<F, T> beans(ModelBeans val) {
        beans = val;
        return this;
    }

    public MappingSetEvaluationBuilder<F, T> context(LensContext<F> val) {
        context = val;
        return this;
    }

    MappingSetEvaluationBuilder<F, T> evaluationRequests(List<? extends FocalMappingEvaluationRequest<?, ?>> val) {
        evaluationRequests = val;
        return this;
    }

    MappingSetEvaluationBuilder<F, T> phase(ObjectTemplateMappingEvaluationPhaseType val) {
        phase = val;
        return this;
    }

    MappingSetEvaluationBuilder<F, T> focusOdo(ObjectDeltaObject<F> val) {
        focusOdo = val;
        return this;
    }

    MappingSetEvaluationBuilder<F, T> targetSpecification(TargetObjectSpecification<T> val) {
        targetSpecification = val;
        return this;
    }

    MappingSetEvaluationBuilder<F, T> tripleCustomizer(MappingSetEvaluation.TripleCustomizer<PrismValue, ItemDefinition> val) {
        tripleCustomizer = val;
        return this;
    }

    MappingSetEvaluationBuilder<F, T> mappingConsumer(MappingSetEvaluation.EvaluatedMappingConsumer<PrismValue, ItemDefinition> val) {
        mappingConsumer = val;
        return this;
    }

    public MappingSetEvaluationBuilder<F, T> iteration(int val) {
        iteration = val;
        return this;
    }

    public MappingSetEvaluationBuilder<F, T> iterationToken(String val) {
        iterationToken = val;
        return this;
    }

    public MappingSetEvaluationBuilder<F, T> env(MappingEvaluationEnvironment val) {
        env = val;
        return this;
    }

    public MappingSetEvaluationBuilder<F, T> result(OperationResult val) {
        result = val;
        return this;
    }

    public MappingSetEvaluation<F, T> build() {
        return new MappingSetEvaluation<>(this);
    }
}
