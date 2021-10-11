/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ModificationPolicyConstraintType;

@Component
public abstract class ModificationConstraintEvaluator<T extends ModificationPolicyConstraintType> implements PolicyConstraintEvaluator<T> {

    @Autowired protected ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired protected PrismContext prismContext;
    @Autowired protected RelationRegistry relationRegistry;

    @NotNull <AH extends AssignmentHolderType> String createStateKey(PolicyRuleEvaluationContext<AH> rctx) {
        ModelState state = rctx.lensContext.getState();

        // TODO derive more precise information from executed deltas, if needed
        if (state == ModelState.INITIAL || state == ModelState.PRIMARY) {
            return "toBe";
        } else {
            return "was";
        }
    }

    // TODO retrieve localization messages from return (it should be Object then, not Boolean)
    boolean expressionPasses(JAXBElement<T> constraintElement, PolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        return constraintElement.getValue().getExpression() == null || expressionEvaluatesToTrue(constraintElement, ctx, result);
    }

    private boolean expressionEvaluatesToTrue(JAXBElement<T> constraintElement, PolicyRuleEvaluationContext<?> ctx,
            OperationResult result)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        T constraint = constraintElement.getValue();
        ExpressionVariables variables = evaluatorHelper.createExpressionVariables(ctx, constraintElement);
        String contextDescription = "expression in modification constraint " + constraint.getName() + " (" + ctx.state + ")";
        return evaluatorHelper.evaluateBoolean(constraint.getExpression(), variables, contextDescription, ctx.task, result);
    }
}
