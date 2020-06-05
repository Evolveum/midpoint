/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.mapping.MappingBuilder;
import com.evolveum.midpoint.model.common.mapping.MappingImpl;
import com.evolveum.midpoint.model.impl.lens.AssignmentPathVariables;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
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

    private static final QName CONDITION_OUTPUT_NAME = new QName(SchemaConstants.NS_C, "condition");

    private final EvaluationContext<?> ctx;

    ConditionEvaluator(EvaluationContext<?> evaluationContext) {
        this.ctx = evaluationContext;
    }

    PlusMinusZero computeModeFromCondition(PlusMinusZero initialMode, MappingType condition,
            ObjectType source, String description, Object loggingDesc, OperationResult result)
            throws SchemaException, CommunicationException, ObjectNotFoundException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        if (condition != null) {
            AssignmentPathVariables assignmentPathVariables = LensUtil.computeAssignmentPathVariables(ctx.assignmentPath);
            PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> conditionTriple = evaluateCondition(condition,
                    source, assignmentPathVariables,
                    description, ctx, result);
            boolean condOld = ExpressionUtil.computeConditionResult(conditionTriple.getNonPositiveValues());
            boolean condNew = ExpressionUtil.computeConditionResult(conditionTriple.getNonNegativeValues());
            PlusMinusZero modeFromCondition = ExpressionUtil.computeConditionResultMode(condOld, condNew);
            if (modeFromCondition == null) {
                LOGGER.trace("Evaluated condition in {}: {} -> {}: null (not continuing further)", loggingDesc, condOld, condNew);
                return null;
            } else {
                PlusMinusZero updatedMode = PlusMinusZero.compute(initialMode, modeFromCondition);
                LOGGER.trace("Evaluated condition in {}: {} -> {}: {} + {} = {}", loggingDesc, condOld, condNew,
                        initialMode, modeFromCondition, updatedMode);
                return updatedMode;
            }
        } else {
            return initialMode;
        }
    }

    private PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> evaluateCondition(MappingType condition,
            ObjectType source, AssignmentPathVariables assignmentPathVariables, String contextDescription, EvaluationContext<?> ctx,
            OperationResult result) throws ExpressionEvaluationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, CommunicationException {
        MappingBuilder<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> builder =
                ctx.ae.mappingFactory.createMappingBuilder();
        builder = builder.mappingBean(condition)
                .mappingKind(MappingKindType.ASSIGNMENT_CONDITION)
                .contextDescription(contextDescription)
                .sourceContext(ctx.ae.focusOdo)
                .originType(OriginType.ASSIGNMENTS)
                .originObject(source)
                .defaultTargetDefinition(ctx.ae.prismContext.definitionFactory().createPropertyDefinition(CONDITION_OUTPUT_NAME, DOMUtil.XSD_BOOLEAN))
                .addVariableDefinitions(ctx.ae.getAssignmentEvaluationVariables())
                .rootNode(ctx.ae.focusOdo)
                .addVariableDefinition(ExpressionConstants.VAR_FOCUS, ctx.ae.focusOdo)
                .addVariableDefinition(ExpressionConstants.VAR_USER, ctx.ae.focusOdo)
                .addAliasRegistration(ExpressionConstants.VAR_USER, null)
                .addAliasRegistration(ExpressionConstants.VAR_FOCUS, null)
                .addVariableDefinition(ExpressionConstants.VAR_SOURCE, source, ObjectType.class)
                .addVariableDefinition(ExpressionConstants.VAR_ASSIGNMENT_EVALUATOR, this, AssignmentEvaluator.class);
        builder = LensUtil.addAssignmentPathVariables(builder, assignmentPathVariables, ctx.ae.prismContext);

        MappingImpl<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> mapping = builder.build();

        ctx.ae.mappingEvaluator.evaluateMapping(mapping, ctx.ae.lensContext, ctx.task, result);

        return mapping.getOutputTriple();
    }
}
