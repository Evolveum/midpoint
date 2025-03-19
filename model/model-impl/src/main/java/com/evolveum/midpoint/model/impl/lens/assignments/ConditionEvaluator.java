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
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.config.MappingConfigItem;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.config.ConfigurationItemOrigin;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.model.impl.lens.projector.mappings.MappingEvaluator.EvaluationContext.*;

/**
 * Evaluates assignment/role conditions, resulting in "relativity mode" updates.
 */
class ConditionEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(ConditionEvaluator.class);

    private final EvaluationContext<?> ctx;

    ConditionEvaluator(EvaluationContext<?> evaluationContext) {
        this.ctx = evaluationContext;
    }

    @NotNull ConditionState computeConditionState(
            @NotNull MappingType condition,
            @NotNull ConfigurationItemOrigin conditionOrigin, // [EP:M:ARC] DONE^
            ObjectType source,
            String description,
            OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (!ctx.task.canSee(condition)) {
            LOGGER.trace("Condition is not visible for the current task");
            return ConditionState.allTrue();
        } else {
            AssignmentPathVariables assignmentPathVariables = ctx.assignmentPath.computePathVariablesRequired();
            PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTripleAbsolute =
                    evaluateCondition( // [EP:M:ARC] DONE^
                            condition, conditionOrigin, source, assignmentPathVariables, description, true, result);
            PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTripleRelative =
                    evaluateCondition( // [EP:M:ARC] DONE^
                            condition, conditionOrigin, source, assignmentPathVariables, description, false, result);
            // TODO eliminate repeated "new" computation
            boolean condOld = ExpressionUtil.computeConditionResult(conditionTripleAbsolute.getNonPositiveValues());
            boolean condCurrent = ExpressionUtil.computeConditionResult(conditionTripleRelative.getNonPositiveValues());
            boolean condNew = ExpressionUtil.computeConditionResult(conditionTripleAbsolute.getNonNegativeValues());
            return ConditionState.from(condOld, condCurrent, condNew);
        }
    }

    private PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> evaluateCondition(
            @NotNull MappingType condition,
            ConfigurationItemOrigin mappingOrigin, // [EP:M:ARC] DONE 2/2
            ObjectType source,
            @NotNull AssignmentPathVariables assignmentPathVariables,
            String contextDescription,
            boolean absolute,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        MappingBuilder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder =
                ctx.ae.mappingFactory.createMappingBuilder();
        ObjectDeltaObject<?> focusOdo = absolute ? ctx.ae.focusOdoAbsolute : ctx.ae.focusOdoRelative;
        builder = builder.mapping(MappingConfigItem.of(condition, mappingOrigin)) // [EP:M:ARC] DONE^
                .mappingKind(MappingKindType.ASSIGNMENT_CONDITION)
                .contextDescription((absolute ? "(absolute) " : "(relative) ") + contextDescription)
                .defaultSourceContextIdi(focusOdo)
                .originType(OriginType.ASSIGNMENTS)
                .defaultTargetDefinition(LensUtil.createConditionDefinition())
                .addVariableDefinitions(ctx.ae.getAssignmentEvaluationVariables())
                .addRootVariableDefinition(focusOdo)
                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, focusOdo)
                .addVariableDefinition(ExpressionConstants.VAR_USER, focusOdo)
                .addAliasRegistration(ExpressionConstants.VAR_USER, null)
                .addAliasRegistration(ExpressionConstants.VAR_FOCUS, null)
                .addVariableDefinition(ExpressionConstants.VAR_SOURCE, source, ObjectType.class)
                .addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT_EVALUATOR, ctx.ae, AssignmentEvaluator.class)
                .ignoreValueMetadata();
        builder = LensUtil.addAssignmentPathVariables(builder, assignmentPathVariables);

        MappingImpl<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

        ctx.ae.mappingEvaluator.evaluateMapping(
                mapping,
                forModelContext(ctx.ae.lensContext),
                ctx.task,
                result);

        return mapping.getOutputTriple();
    }
}
