/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedTimeValidityTrigger;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.task.ItemProcessingRequest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TimeValidityPolicyConstraintType;

/**
 * Item processor for the validity scanner.
 */
public class FocusValidityScannerItemProcessor
        extends AbstractScannerItemProcessor
        <FocusType,
                FocusValidityScannerTaskHandler,
                FocusValidityScannerTaskExecution,
                FocusValidityScannerTaskPartExecution,
                FocusValidityScannerItemProcessor> {

    public FocusValidityScannerItemProcessor(FocusValidityScannerTaskPartExecution taskExecution) {
        super(taskExecution);
    }

    @Override
    protected boolean processObject(PrismObject<FocusType> object,
            ItemProcessingRequest<PrismObject<FocusType>> request,
            RunningTask workerTask, OperationResult result)
            throws CommonException, PreconditionViolationException {
        recomputeFocus(object, workerTask, result);
        return true;
    }

    private void recomputeFocus(PrismObject<FocusType> focus, RunningTask workerTask, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, PolicyViolationException, SecurityViolationException, PreconditionViolationException {
        LensContext<FocusType> lensContext = createLensContext(focus, workerTask, result);
        logger.trace("Recomputing of focus {}: context:\n{}", focus, lensContext.debugDumpLazily());
        taskHandler.clockwork.run(lensContext, workerTask, result);
    }

    private LensContext<FocusType> createLensContext(PrismObject<FocusType> focus, RunningTask workerTask, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, SecurityViolationException {

        // We want reconcile option here. There may be accounts that are in wrong activation state.
        // We will not notice that unless we go with reconcile.
        ModelExecuteOptions options = new ModelExecuteOptions(taskHandler.getPrismContext()).reconcile();

        LensContext<FocusType> lensContext = taskHandler.contextFactory.createRecomputeContext(focus, options, workerTask, result);
        if (taskExecution.doCustomValidityCheck()) {
            addTriggeredPolicyRuleToContext(focus, lensContext, workerTask, result);
        }

        return lensContext;
    }

    private void addTriggeredPolicyRuleToContext(PrismObject<FocusType> focus, LensContext<FocusType> lensContext,
            RunningTask workerTask, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
            SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        PrismContext prismContext = taskHandler.getPrismContext();
        TimeValidityPolicyConstraintType constraint = taskExecution.getValidityConstraint();
        EvaluatedPolicyRuleImpl policyRule = new EvaluatedPolicyRuleImpl(workerTask.getPolicyRule(), null, null);
        policyRule.computeEnabledActions(null, focus, taskHandler.getExpressionFactory(), prismContext, workerTask, result);
        EvaluatedPolicyRuleTrigger<TimeValidityPolicyConstraintType> evaluatedTrigger = new EvaluatedTimeValidityTrigger(
                Boolean.TRUE.equals(constraint.isAssignment()) ? PolicyConstraintKindType.ASSIGNMENT_TIME_VALIDITY : PolicyConstraintKindType.OBJECT_TIME_VALIDITY,
                constraint,
                LocalizableMessageBuilder.buildFallbackMessage("Applying time validity constraint for focus"),
                LocalizableMessageBuilder.buildFallbackMessage("Time validity"));
        policyRule.getTriggers().add(evaluatedTrigger);
        lensContext.getFocusContext().addPolicyRule(policyRule);
    }
}
