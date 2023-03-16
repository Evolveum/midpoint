/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy.evaluators;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType.ORPHANED;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.EvaluatedOrphanedTrigger;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.lens.projector.policy.PolicyRuleEvaluationContext;
import com.evolveum.midpoint.model.impl.scripting.ScriptingExpressionEvaluator;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrphanedPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@Component
@Experimental
public class OrphanedConstraintEvaluator implements PolicyConstraintEvaluator<OrphanedPolicyConstraintType> {

    private static final Trace LOGGER = TraceManager.getTrace(OrphanedConstraintEvaluator.class);

    private static final String OP_EVALUATE = OrphanedConstraintEvaluator.class.getName() + ".evaluate";

    private static final String CONSTRAINT_KEY_PREFIX = "orphaned.";
    private static final String KEY_NAMED = "named";
    private static final String KEY_UNNAMED = "unnamed";

    @Autowired protected ExpressionFactory expressionFactory;
    @Autowired protected ConstraintEvaluatorHelper evaluatorHelper;
    @Autowired protected ScriptingExpressionEvaluator scriptingExpressionEvaluator;
    @Autowired protected TaskManager taskManager;

    @Override
    public <O extends ObjectType> EvaluatedOrphanedTrigger evaluate(
            @NotNull JAXBElement<OrphanedPolicyConstraintType> constraint,
            @NotNull PolicyRuleEvaluationContext<O> rctx, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .setMinor()
                .build();
        try {
            if (rctx instanceof ObjectPolicyRuleEvaluationContext<?>) {
                return evaluateForObject(constraint, (ObjectPolicyRuleEvaluationContext<O>) rctx, result);
            } else {
                return null;
            }
        } catch (Throwable t) {
            result.recordFatalError(t.getMessage(), t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private EvaluatedOrphanedTrigger evaluateForObject(
            @NotNull JAXBElement<OrphanedPolicyConstraintType> constraintElement,
            ObjectPolicyRuleEvaluationContext<?> ctx, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        PrismObject<?> object = ctx.getObject();
        if (object == null || !(object.asObjectable() instanceof TaskType)) {
            return null;
        }

        //noinspection unchecked
        boolean orphaned = taskManager.isOrphaned((PrismObject<TaskType>) object, result);
        LOGGER.debug("Orphaned status for {} is {}", object, orphaned);

        if (orphaned) {
            return new EvaluatedOrphanedTrigger(
                    ORPHANED, constraintElement.getValue(),
                    createMessage(constraintElement, ctx, result),
                    createShortMessage(constraintElement, ctx, result));
        } else {
            return null;
        }
    }

    @NotNull
    private LocalizableMessage createMessage(
            @NotNull JAXBElement<OrphanedPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> ctx,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = createBuiltInMessage(
                SchemaConstants.DEFAULT_POLICY_CONSTRAINT_KEY_PREFIX + CONSTRAINT_KEY_PREFIX,
                constraintElement,
                ctx,
                result);
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }

    @NotNull
    private LocalizableMessage createShortMessage(
            JAXBElement<OrphanedPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> ctx,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        LocalizableMessage builtInMessage = createBuiltInMessage(
                SchemaConstants.DEFAULT_POLICY_CONSTRAINT_SHORT_MESSAGE_KEY_PREFIX + CONSTRAINT_KEY_PREFIX,
                constraintElement,
                ctx,
                result);
        return evaluatorHelper.createLocalizableShortMessage(constraintElement, ctx, builtInMessage, result);
    }

    @NotNull
    private LocalizableMessage createBuiltInMessage(
            String keyPrefix,
            JAXBElement<OrphanedPolicyConstraintType> constraintElement,
            PolicyRuleEvaluationContext<?> ctx,
            OperationResult result)
            throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        OrphanedPolicyConstraintType constraint = constraintElement.getValue();
        List<Object> args = new ArrayList<>();
        args.add(evaluatorHelper.createBeforeAfterMessage(ctx));
        String keySuffix;
        if (constraint.getName() != null) {
            args.add(constraint.getName());
            keySuffix = KEY_NAMED;
        } else {
            keySuffix = KEY_UNNAMED;
        }
        LocalizableMessage builtInMessage = new LocalizableMessageBuilder()
                .key(keyPrefix + keySuffix)
                .args(args)
                .build();
        return evaluatorHelper.createLocalizableMessage(constraintElement, ctx, builtInMessage, result);
    }
}
