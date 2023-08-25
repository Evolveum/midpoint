/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.task.api.ExpressionEnvironment;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Executes mappings in diagnostic mode.
 */
@Component
public class MappingDiagEvaluator {

    @Autowired private MappingFactory mappingFactory;
    @Autowired private ModelObjectResolver objectResolver;
    @Autowired private PrismContext prismContext;
    @Autowired private Clock clock;

    public MappingEvaluationResponseType evaluateMapping(
            @NotNull MappingEvaluationRequestType request, @NotNull Task task, @NotNull OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, CommunicationException {

        MappingBuilder<?,?> builder = mappingFactory.createMappingBuilder();

        ObjectDeltaObject<?> sourceContext = createSourceContext(request, task, result);

        StringBuilder sb = new StringBuilder();
        MappingType mappingBean = request.getMapping();
        if (task.canSee(mappingBean)) {
            builder
                    .mappingBean(mappingBean, ConfigurationItemOrigin.external(task.getChannel()))
                    .mappingKind(MappingKindType.OTHER)
                    .contextDescription("mapping diagnostic execution")
                    .sourceContext(sourceContext)
                    .targetContext(createTargetContext(request, sourceContext))
                    .profiling(true)
                    .now(clock.currentTimeXMLGregorianCalendar());

            MappingImpl<?, ?> mapping = builder.build();

            ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment(task, result));
            try {
                mapping.evaluate(task, result);
            } finally {
                ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
            }

            sb.append("Output triple: ");
            dumpOutputTriple(sb, mapping.getOutputTriple());
            sb.append("Condition output triple: ");
            dumpOutputTriple(sb, mapping.getConditionOutputTriple());
            sb.append("Time constraint valid: ").append(mapping.isTimeConstraintValid()).append("\n");
            sb.append("Next recompute time: ").append(mapping.getNextRecomputeTime()).append("\n");
            sb.append("\n");
            sb.append("Evaluation time: ").append(mapping.getEtime()).append(" ms\n");
        } else {
            sb.append("Mapping is not visible for the current task");
        }

        MappingEvaluationResponseType response = new MappingEvaluationResponseType();
        response.setResponse(sb.toString());
        return response;
    }

    private void dumpOutputTriple(StringBuilder sb, PrismValueDeltaSetTriple<?> triple) {
        if (triple != null) {
            sb.append("\n").append(triple.debugDump(1)).append("\n\n");
        } else {
            sb.append("(null)\n\n");
        }
    }

    private PrismObjectDefinition<?> createTargetContext(MappingEvaluationRequestType request, ObjectDeltaObject<?> sourceContext) {
        if (request.getTargetContext() == null) {
            return sourceContext.getDefinition();
        }
        return prismContext.getSchemaRegistry().findObjectDefinitionByType(request.getTargetContext());
    }

    private <O extends Objectable> ObjectDeltaObject<O> createSourceContext(MappingEvaluationRequestType request, Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (request.getSourceContext() == null) {
            return null;
        }
        MappingEvaluationSourceContextType ctx = request.getSourceContext();

        PrismObject<O> oldObject;
        if (ctx.getObject() != null) {
            //noinspection unchecked
            oldObject = (PrismObject<O>) ctx.getObject().getValue().asPrismObject();
        } else if (ctx.getObjectRef() != null) {
            //noinspection unchecked
            oldObject = (PrismObject<O>) objectResolver.resolve(ctx.getObjectRef(), ObjectType.class, null,
                    "resolving default source", task, result).asPrismObject();
        } else {
            oldObject = null;
        }
        ObjectDelta<O> delta;
        if (ctx.getDelta() != null) {
            delta = DeltaConvertor.createObjectDelta(ctx.getDelta(), prismContext);
        } else {
            delta = null;
        }
        return new ObjectDeltaObject<>(
                oldObject,
                delta,
                null,
                oldObject != null ? oldObject.getDefinition() : null);
    }
}
