/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerCreationType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import com.evolveum.midpoint.xml.ns._public.model.scripting_3.RecomputeActionExpressionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Executes "recompute" action.
 */
@Component
public class RecomputeExecutor extends AbstractObjectBasedActionExecutor<AssignmentHolderType> {

    private static final String NAME = "recompute";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, RecomputeActionExpressionType.class, this);
    }

    @Override
    public PipelineData execute(
            ActionExpressionType action, PipelineData input, ExecutionContext context, OperationResult globalResult)
            throws ScriptExecutionException, SchemaException, ObjectNotFoundException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {

        boolean dryRun = operationsHelper.getDryRun(action, input, context, globalResult);
        ModelExecuteOptions options = operationsHelper.getOptions(action, input, context, globalResult);
        options.reconcile();
        TriggerCreationType triggerCreation = action instanceof RecomputeActionExpressionType ?
                ((RecomputeActionExpressionType) action).getTriggered() : null;

        iterateOverObjects(input, context, globalResult,
                (object, item, result) ->
                        recompute(object, dryRun, options, triggerCreation, context, result),
                (object, exception) ->
                        context.println("Failed to recompute " + object + drySuffix(dryRun) + exceptionSuffix(exception))
        );

        return input;
    }

    private void recompute(PrismObject<? extends AssignmentHolderType> object, boolean dryRun, ModelExecuteOptions options,
            TriggerCreationType triggerCreation, ExecutionContext context, OperationResult result)
            throws ScriptExecutionException, SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {

        AssignmentHolderType objectable = object.asObjectable();
        if (triggerCreation == null) {
            ObjectDelta<? extends AssignmentHolderType> emptyDelta = prismContext.deltaFactory().object()
                    .createEmptyModifyDelta(objectable.getClass(), objectable.getOid());
            operationsHelper.applyDelta(emptyDelta, options, dryRun, context, result);
            context.println("Recomputed " + object.toString() + drySuffix(dryRun));
        } else if (dryRun) {
            context.println("Skipping dry run of triggered-recompute of " + object.toString());
        } else if (triggerCreation.getFireAfter() == null) {
            // direct trigger creation
            midpointFunctions.createRecomputeTrigger(objectable.getClass(), objectable.getOid());
            context.println("Triggered recompute of " + object.toString());
        } else {
            // optimized trigger creation
            long fireAfter = XmlTypeConverter.toMillis(triggerCreation.getFireAfter());
            long safetyMargin = triggerCreation.getSafetyMargin() != null ?
                    XmlTypeConverter.toMillis(triggerCreation.getSafetyMargin()) : 0;
            boolean triggerCreated = midpointFunctions.getOptimizingTriggerCreator(fireAfter, safetyMargin)
                    .createForObject(objectable.getClass(), objectable.getOid());
            if (triggerCreated) {
                context.println("Triggered recompute of " + object.toString());
            } else {
                context.println("Skipped triggering recompute of " + object.toString() + " because a trigger was already present");
            }
        }
    }

    @Override
    Class<AssignmentHolderType> getObjectType() {
        return AssignmentHolderType.class;
    }

    @Override
    protected @NotNull String getLegacyActionName() {
        return NAME;
    }

    @Override
    @NotNull String getConfigurationElementName() {
        return SchemaConstantsGenerated.SC_RECOMPUTE.getLocalPart();
    }
}
