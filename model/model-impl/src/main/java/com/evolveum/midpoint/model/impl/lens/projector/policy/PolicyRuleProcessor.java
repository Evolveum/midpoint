/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens.projector.policy;

import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.context.DirectlyEvaluatedClockworkPolicyRule;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.projector.ProjectorProcessor;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRulesEvaluator.FocusPolicyRulesEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.policy.ObjectPolicyRulesEvaluator.ProjectionPolicyRulesEvaluator;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorExecution;
import com.evolveum.midpoint.model.impl.lens.projector.util.ProcessorMethod;
import com.evolveum.midpoint.repo.common.activity.policy.PolicyRuleCounterUpdater;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.ExecutionSupport;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * A facade for various actions related to handling of policy rules: evaluation, enforcement, and so on.
 */
@Component
@ProcessorExecution
public class PolicyRuleProcessor implements ProjectorProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(PolicyRuleProcessor.class);

    private static final String CLASS_DOT = PolicyRuleProcessor.class.getName() + ".";
    private static final String OP_EVALUATE_ASSIGNMENT_POLICY_RULES = CLASS_DOT + "evaluateAssignmentPolicyRules";
    private static final String OP_RECORD_ASSIGNMENT_POLICY_RULES = CLASS_DOT + "recordAssignmentPolicyRules";
    private static final String OP_ENFORCE = CLASS_DOT + "enforce";

    @Autowired private PolicyStatementProcessor policyStatementProcessor;

    public <F extends AssignmentHolderType> void evaluateAssignmentPolicyRules(
            @NotNull LensFocusContext<F> focusContext,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        OperationResult result = parentResult.createSubresult(OP_EVALUATE_ASSIGNMENT_POLICY_RULES);
        try {
            new AssignmentPolicyRuleEvaluator<>(focusContext, task)
                    .evaluate(result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * This is separate because assignments can be evaluated before and after pruning.
     */
    public <AH extends AssignmentHolderType> void recordAssignmentPolicyRules(
            @NotNull LensFocusContext<AH> focusContext,
            @NotNull Task task,
            @NotNull OperationResult parentResult) throws SchemaException {
        OperationResult result = parentResult.createSubresult(OP_RECORD_ASSIGNMENT_POLICY_RULES);
        try {
            new AssignmentPolicyRuleEvaluator<>(focusContext, task)
                    .record(result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @ProcessorMethod
    public <AH extends AssignmentHolderType> void evaluateAndRecordFocusPolicyRules(
            LensContext<AH> context, XMLGregorianCalendar ignoredNow, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        if (context.getFocusContextRequired().isDeleted()) {
            LOGGER.trace("Focus is gone, therefore we will skip processing focus policy rules");
            result.setNotApplicable("focus is gone");
        } else {
            // No need for custom operation result, as this already has one (because it's a projector component)
            LensFocusContext<AH> focusContext = context.getFocusContextRequired();
            FocusPolicyRulesEvaluator<AH> evaluator = new FocusPolicyRulesEvaluator<>(focusContext, task);
            evaluator.evaluate(result);
            policyStatementProcessor.processPolicyStatements(focusContext, task, result);
            evaluator.record(result);
        }
    }

    @ProcessorMethod
    public <AH extends AssignmentHolderType> void evaluateProjectionPolicyRules(
            LensContext<AH> ignoredContext,
            LensProjectionContext projectionContext,
            String ignoredActivityDescription,
            XMLGregorianCalendar ignoredNow,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException,
            ConfigurationException, CommunicationException {
        // No need for custom operation result, as this already has one (because it's a projector component)
        ProjectionPolicyRulesEvaluator evaluator = new ProjectionPolicyRulesEvaluator(projectionContext, task);
        evaluator.evaluate(result);

        // TODO statements for projections
        //  As for statements, they are currently processed by ObjectMarkHelper, which is invoked e.g. right from provisioning
        //  (when maintaining the shadows). We should integrate this with the policy rule processing eventually. (Fortunately,
        //  the object policy rules are not used for projections now, only the event policy rules are.)

//        policyStatementProcessor.processPolicyStatements(projectionContext, task, result);
        evaluator.record(result);
    }

    /** Updates counters for policy rules, with the goal of determining if rules' thresholds have been reached. */
    @ProcessorMethod
    public <AH extends AssignmentHolderType> void updateCounters(
            LensContext<AH> context, @SuppressWarnings("unused") XMLGregorianCalendar now, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        // No need for custom operation result, as this already has one

        new MyPolicyRuleCounterUpdater(context, task).updateCounters(result);
    }

    public <O extends ObjectType> void enforce(@NotNull LensContext<O> context, Task task, OperationResult parentResult)
            throws PolicyViolationException, ConfigurationException {
        OperationResult result = parentResult.createMinorSubresult(OP_ENFORCE);
        try {
            new PolicyRuleEnforcer<>(context, task.getExecutionSupport())
                    .enforce(task, result);
        } catch (Throwable t) {
            result.recordException(t);
            throw t;
        } finally {
            result.close();
        }
    }

    /**
     * This is a simple adapter that allows us to use the PolicyRuleCounterUpdater in the context of the projector,
     * where we don't have an activity run, but we do have a lens context and a task.
     *
     * It retrieves and stores the counters in the focus context of the lens context, and it retrieves
     * the policy rules from there as well.
     */
    private static class MyPolicyRuleCounterUpdater extends PolicyRuleCounterUpdater {

        @NotNull private final LensContext<?> context;

        @NotNull private final Task task;

        public MyPolicyRuleCounterUpdater(@NotNull LensContext<?> context, @NotNull Task task) {
            this.context = context;
            this.task = task;
        }

        @Override
        protected Integer getIncrementedPolicyRuleCounter(String ruleIdentifier) {
            LensFocusContext<?> focusContext = context.getFocusContext();
            if (focusContext == null) {
                return null;
            }

            return focusContext.getPolicyRuleCounter(ruleIdentifier);
        }

        /**
         * In the projector, we store the counters in the focus context of the lens context.
         * This "cache" helps later on when checking if counter was already updated.
         */
        @Override
        protected void storeIncrementedPolicyRuleCounter(String ruleIdentifier, Integer counter) {
            LensFocusContext<?> focusContext = context.getFocusContext();
            if (focusContext != null) {
                focusContext.setPolicyRuleCounter(ruleIdentifier, counter);
            }
        }

        @NotNull
        @Override
        protected Collection<? extends DirectlyEvaluatedClockworkPolicyRule> getPolicyRules() {
            LensFocusContext<?> focusContext = context.getFocusContext();
            if (focusContext == null) {
                return List.of();
            }

            return focusContext.getObjectPolicyRules();
        }

        @NotNull
        @Override
        protected ExecutionSupport getExecutionSupport() {
            return task.getExecutionSupport();
        }
    }
}
