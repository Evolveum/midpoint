/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Evaluates assignment/role conditions, resulting in "relativity mode" updates.
 */
class ConditionEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(ConditionEvaluator.class);

    private final EvaluationContext<?> ctx;

    ConditionEvaluator(EvaluationContext<?> evaluationContext) {
        this.ctx = evaluationContext;
    }

    ConditionState computeConditionState(
            MappingType condition, ObjectType source, String description, Object loggingDesc, OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (condition == null) {
            return ConditionState.allTrue();
        } else if (!ctx.task.canSee(condition)) {
            LOGGER.trace("Condition is not visible for the current task");
            return ConditionState.allTrue();
        } else {
            AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);
            PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTripleAbsolute =
                    evaluateConditionAbsolute(condition, source, assignmentPathVariables, description, result);
            PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTripleRelative =
                    evaluateConditionRelative(condition, source, assignmentPathVariables, description, result);
            // TODO eliminate repeated "new" computation
            boolean condOld = ExpressionUtil.computeConditionResult(conditionTripleAbsolute.getNonPositiveValues());
            boolean condCurrent = ExpressionUtil.computeConditionResult(conditionTripleRelative.getNonPositiveValues());
            boolean condNew = ExpressionUtil.computeConditionResult(conditionTripleAbsolute.getNonNegativeValues());
            return ConditionState.from(condOld, condCurrent, condNew);
        }
    }

    private PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> evaluateConditionAbsolute(
            MappingType condition,
            ObjectType source,
            AssignmentPathVariables assignmentPathVariables,
            String contextDescription,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        MappingBuilder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder =
                ctx.ae.mappingFactory.createMappingBuilder();
        builder = builder.mappingBean(condition)
                .mappingKind(MappingKindType.ASSIGNMENT_CONDITION)
                .contextDescription("(absolute) " + contextDescription)
                .sourceContext(ctx.ae.focusOdoAbsolute)
                .originType(OriginType.ASSIGNMENTS)
                .originObject(source)
                .defaultTargetDefinition(LensUtil.createConditionDefinition(ctx.ae.prismContext))
                .addVariableDefinitions(ctx.ae.getAssignmentEvaluationVariables())
                .rootNode(ctx.ae.focusOdoAbsolute)
                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, ctx.ae.focusOdoAbsolute)
                .addVariableDefinition(ExpressionConstants.VAR_USER, ctx.ae.focusOdoAbsolute)
                .addAliasRegistration(ExpressionConstants.VAR_USER, null)
                .addAliasRegistration(ExpressionConstants.VAR_FOCUS, null)
                .addVariableDefinition(ExpressionConstants.VAR_SOURCE, source, ObjectType.class)
                .addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT_EVALUATOR, ctx.ae, AssignmentEvaluator.class);
        builder = LensUtil.addAssignmentPathVariables(builder, assignmentPathVariables, ctx.ae.prismContext);

        MappingImpl<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

        ctx.ae.mappingEvaluator.evaluateMapping(mapping, ctx.ae.lensContext, ctx.task, result);

        return mapping.getOutputTriple();
    }

    // TODO deduplicate, optimize
    private PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> evaluateConditionRelative(
            MappingType condition,
            ObjectType source,
            AssignmentPathVariables assignmentPathVariables,
            String contextDescription,
            OperationResult result) throws ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        MappingBuilder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder =
                ctx.ae.mappingFactory.createMappingBuilder();
        builder = builder.mappingBean(condition)
                .mappingKind(MappingKindType.ASSIGNMENT_CONDITION)
                .contextDescription("(relative) " + contextDescription)
                .sourceContext(ctx.ae.focusOdoRelative)
                .originType(OriginType.ASSIGNMENTS)
                .originObject(source)
                .defaultTargetDefinition(LensUtil.createConditionDefinition(ctx.ae.prismContext))
                .addVariableDefinitions(ctx.ae.getAssignmentEvaluationVariables())
                .rootNode(ctx.ae.focusOdoRelative)
                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, ctx.ae.focusOdoRelative)
                .addVariableDefinition(ExpressionConstants.VAR_USER, ctx.ae.focusOdoRelative)
                .addAliasRegistration(ExpressionConstants.VAR_USER, null)
                .addAliasRegistration(ExpressionConstants.VAR_FOCUS, null)
                .addVariableDefinition(ExpressionConstants.VAR_SOURCE, source, ObjectType.class)
                .addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT_EVALUATOR, ctx.ae, AssignmentEvaluator.class);
        builder = LensUtil.addAssignmentPathVariables(builder, assignmentPathVariables, ctx.ae.prismContext);

        MappingImpl<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

        ctx.ae.mappingEvaluator.evaluateMapping(mapping, ctx.ae.lensContext, ctx.task, result);

        return mapping.getOutputTriple();
    }
}
