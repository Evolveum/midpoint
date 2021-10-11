/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.common.mapping.MappingFactory;
import com.evolveum.midpoint.model.impl.ModelObjectResolver;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Executes mappings in diagnostic mode.
 *
 * @author mederly
 */
@Component
public class MappingDiagEvaluator {

    @Autowired
    private MappingFactory mappingFactory;

    @Autowired
    private ModelService modelService;

    @Autowired
    private ModelObjectResolver objectResolver;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private Clock clock;

    public MappingEvaluationResponseType evaluateMapping(@NotNull MappingEvaluationRequestType request, @NotNull Task task,
            @NotNull OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {

        MappingImpl.Builder<?,?> builder = mappingFactory.createMappingBuilder();

        ObjectDeltaObject<?> sourceContext = createSourceContext(request, task, result);

        builder = builder
                .mappingType(request.getMapping())
                .mappingKind(MappingKindType.OTHER)
                .contextDescription("mapping diagnostic execution")
                .sourceContext(sourceContext)
                .targetContext(createTargetContext(request, sourceContext))
                .profiling(true)
                .now(clock.currentTimeXMLGregorianCalendar());

        MappingImpl<?,?> mapping = builder.build();

        ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
        try {
            mapping.evaluate(task, result);
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Output triple: ");
        dumpOutputTriple(sb, mapping.getOutputTriple());
        sb.append("Condition output triple: ");
        dumpOutputTriple(sb, mapping.getConditionOutputTriple());
        sb.append("Time constraint valid: ").append(mapping.evaluateTimeConstraintValid(task, result)).append("\n");
        sb.append("Next recompute time: ").append(mapping.getNextRecomputeTime()).append("\n");
        sb.append("\n");
        sb.append("Evaluation time: ").append(mapping.getEtime()).append(" ms\n");

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

    private ObjectDeltaObject<?> createSourceContext(MappingEvaluationRequestType request, Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (request.getSourceContext() == null) {
            return null;
        }
        MappingEvaluationSourceContextType ctx = request.getSourceContext();

        PrismObject<?> oldObject;
        if (ctx.getObject() != null) {
            oldObject = ctx.getObject().getValue().asPrismObject();
        } else if (ctx.getObjectRef() != null) {
            oldObject = objectResolver.resolve(ctx.getObjectRef(), ObjectType.class, null, "resolving default source", task, result).asPrismObject();
        } else {
            oldObject = null;
        }
        ObjectDelta<?> delta;
        if (ctx.getDelta() != null) {
            delta = DeltaConvertor.createObjectDelta(ctx.getDelta(), prismContext);
        } else {
            delta = null;
        }
        return new ObjectDeltaObject(oldObject, delta, null, oldObject.getDefinition());
    }
}
