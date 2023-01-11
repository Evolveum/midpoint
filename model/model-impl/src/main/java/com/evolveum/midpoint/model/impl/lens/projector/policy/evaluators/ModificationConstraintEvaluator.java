/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import javax.xml.bind.JAXBElement;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.expression.VariablesMap;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

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

import java.util.Collection;

@Component
public abstract class ModificationConstraintEvaluator<T extends ModificationPolicyConstraintType> implements PolicyConstraintEvaluator<T> {

    private static final Trace LOGGER = TraceManager.getTrace(ModificationConstraintEvaluator.class);

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
        VariablesMap variables = evaluatorHelper.createVariablesMap(ctx, constraintElement);
        String contextDescription = "expression in modification constraint " + constraint.getName() + " (" + ctx.state + ")";
        return evaluatorHelper.evaluateBoolean(constraint.getExpression(), variables, contextDescription, ctx, result);
    }

    boolean pathMatchesExactly(
            @NotNull Collection<? extends ItemDelta<?, ?>> deltas, @NotNull ItemPath path, int segmentsToSkip) {
        for (ItemDelta<?, ?> delta : deltas) {
            ItemPath modifiedPath = delta.getPath().rest(segmentsToSkip).removeIds(); // because of extension/cities[2]/name (in delta) vs. extension/cities/name (in spec)
            if (path.equivalent(modifiedPath)) {
                return true;
            }
        }
        return false;
    }

    boolean valuesChanged(
            @NotNull PrismContainerValue<?> oldContainerValue,
            @NotNull PrismContainerValue<?> newContainerValue,
            @NotNull ItemPath path) {
        Collection<PrismValue> oldValues = oldContainerValue.getAllValues(path);
        Collection<PrismValue> newValues = newContainerValue.getAllValues(path);
        boolean different = !MiscUtil.unorderedCollectionEquals(oldValues, newValues);
        LOGGER.trace("valuesChanged considering '{}': oldValues: {}, newValues: {}, different: {}",
                path, oldValues, newValues, different);
        return different;
    }
}
