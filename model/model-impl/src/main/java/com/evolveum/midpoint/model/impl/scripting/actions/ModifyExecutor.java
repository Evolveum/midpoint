/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionParameterValueType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class ModifyExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(ModifyExecutor.class);

    private static final String NAME = "modify";
    private static final String PARAM_DELTA = "delta";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        ModelExecuteOptions executionOptions = getOptions(expression, input, context, globalResult);
        boolean dryRun = getParamDryRun(expression, input, context, globalResult);

        ActionParameterValueType deltaParameterValue = expressionHelper.getArgument(expression.getParameter(), PARAM_DELTA, true, true, NAME);
        PipelineData deltaData = expressionHelper.evaluateParameter(deltaParameterValue, ObjectDeltaType.class, input, context, globalResult);

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
            context.checkTaskStop();
            if (value instanceof PrismObjectValue) {
                @SuppressWarnings({"unchecked", "raw"})
                PrismObject<? extends ObjectType> prismObject = ((PrismObjectValue) value).asPrismObject();
                ObjectType objectType = prismObject.asObjectable();
                long started = operationsHelper.recordStart(context, objectType);
                Throwable exception = null;
                try {
                    ObjectDelta<? extends ObjectType> delta = createDelta(objectType, deltaData);
                    result.addParam("delta", delta);
                    // This is only a preliminary solution for MID-4138. There are few things to improve:
                    // 1. References could be resolved earlier (before the main cycle); however it would require much more
                    //    coding, as we have only skeleton of ObjectDeltaType there - we don't know the specific object type
                    //    the delta will be applied to. It is not a big problem, but still a bit of work.
                    // 2. If the evaluation time is IMPORT, and the bulk action is part of a task that is being imported into
                    //    repository, it should be perhaps resolved at that time. But again, it is a lot of work and it does
                    //    not cover bulk actions which are not part of a task.
                    // We consider this solution to be adequate for now.
                    ModelImplUtils.resolveReferences(delta, cacheRepositoryService, false, false, EvaluationTimeType.IMPORT, true, prismContext, result);
                    operationsHelper.applyDelta(delta, executionOptions, dryRun, context, result);
                    operationsHelper.recordEnd(context, objectType, started, null);
                } catch (Throwable ex) {
                    operationsHelper.recordEnd(context, objectType, started, ex);
                    exception = processActionException(ex, NAME, value, context);
                }
                context.println((exception != null ? "Attempted to modify " : "Modified ") + prismObject.toString() + optionsSuffix(executionOptions, dryRun) + exceptionSuffix(exception));
            } else {
                //noinspection ThrowableNotThrown
                processActionException(new ScriptExecutionException("Item is not a PrismObject"), NAME, value, context);
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
        }
        return input;
    }

    private ObjectDelta<? extends ObjectType> createDelta(ObjectType objectType, PipelineData deltaData) throws ScriptExecutionException {
        if (deltaData.getData().size() != 1) {
            throw new ScriptExecutionException("Expected exactly one delta to apply, found "  + deltaData.getData().size() + " instead.");
        }
        @SuppressWarnings({"unchecked", "raw"})
        ObjectDeltaType deltaType = ((PrismPropertyValue<ObjectDeltaType>) deltaData.getData().get(0).getValue()).clone().getRealValue();
        if (deltaType.getChangeType() == null) {
            deltaType.setChangeType(ChangeTypeType.MODIFY);
        }
        if (deltaType.getOid() == null && deltaType.getChangeType() != ChangeTypeType.ADD) {
            deltaType.setOid(objectType.getOid());
        }
        if (deltaType.getObjectType() == null) {
            if (objectType.asPrismObject().getDefinition() == null) {
                throw new ScriptExecutionException("No definition for prism object " + objectType);
            }
            deltaType.setObjectType(objectType.asPrismObject().getDefinition().getTypeName());
        }
        try {
            return DeltaConvertor.createObjectDelta(deltaType, prismContext);
        } catch (SchemaException e) {
            throw new ScriptExecutionException("Couldn't process delta due to schema exception", e);
        }
    }
}
