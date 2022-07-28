/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.exception.*;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ModifyActionExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.ChangeTypeType;
import com.evolveum.prism.xml.ns._public.types_3.EvaluationTimeType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

/**
 * Executor for 'modify' actions.
 */
@Component
public class ModifyExecutor extends AbstractObjectBasedActionExecutor<ObjectType> {

    private static final String NAME = "modify";
    private static final String PARAM_DELTA = "delta";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, ModifyActionExpressionType.class, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult globalResult) throws ScriptExecutionException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ModelExecuteOptions options = operationsHelper.getOptions(action, input, context, globalResult);
        boolean dryRun = operationsHelper.getDryRun(action, input, context, globalResult);
        ObjectDeltaType deltaBean = expressionHelper.getActionArgument(ObjectDeltaType.class, action,
                ModifyActionExpressionType.F_DELTA, PARAM_DELTA, input, context, null,
                PARAM_DELTA, globalResult);
        if (deltaBean == null) {
            Throwable ex = new SchemaException("Found no delta to be applied");
            processActionException(ex, NAME, null, context); // TODO value for error reporting (3rd parameter)
            context.println("Found no delta to be applied");
            return input;
        }

        iterateOverObjects(input, context, globalResult,
                (object, item, result) ->
                        modify(object, dryRun, options, deltaBean, context, result),
                (object, exception) ->
                        context.println("Failed to modify " + object + drySuffix(dryRun) + exceptionSuffix(exception))
        );

        return input;
    }

    private void modify(PrismObject<? extends ObjectType> object, boolean dryRun, ModelExecuteOptions options,
            ObjectDeltaType deltaBean, ExecutionContext context, OperationResult result)
            throws ScriptExecutionException, SchemaException {
        ObjectDelta<? extends ObjectType> delta = createDelta(object.asObjectable(), deltaBean);
        result.addParam("delta", delta);

        // This is only a preliminary solution for MID-4138. There are few things to improve:
        // 1. References could be resolved earlier (before the main cycle); however it would require much more
        //    coding, as we have only skeleton of ObjectDeltaType there - we don't know the specific object type
        //    the delta will be applied to. It is not a big problem, but still a bit of work.
        // 2. If the evaluation time is IMPORT, and the bulk action is part of a task that is being imported into
        //    repository, it should be perhaps resolved at that time. But again, it is a lot of work and it does
        //    not cover bulk actions which are not part of a task.
        // We consider this solution to be adequate for now.
        ModelImplUtils.resolveReferences(delta, cacheRepositoryService, false, false,
                EvaluationTimeType.IMPORT, true, prismContext, result);

        operationsHelper.applyDelta(delta, options, dryRun, context, result);
        context.println("Modified " + object + optionsSuffix(options, dryRun));
    }

    private ObjectDelta<? extends ObjectType> createDelta(ObjectType object, ObjectDeltaType deltaBean)
            throws ScriptExecutionException, SchemaException {
        ObjectDeltaType deltaBeanClone = deltaBean.clone();
        if (deltaBeanClone.getChangeType() == null) {
            deltaBeanClone.setChangeType(ChangeTypeType.MODIFY);
        }
        if (deltaBeanClone.getOid() == null && deltaBeanClone.getChangeType() != ChangeTypeType.ADD) {
            deltaBeanClone.setOid(object.getOid());
        }
        if (deltaBeanClone.getObjectType() == null) {
            PrismObjectDefinition<? extends ObjectType> definition = object.asPrismObject().getDefinition();
            if (definition == null) {
                throw new ScriptExecutionException("No definition for prism object " + object);
            }
            deltaBeanClone.setObjectType(definition.getTypeName());
        }
        return DeltaConvertor.createObjectDelta(deltaBeanClone, prismContext);
    }

    @Override
    Class<ObjectType> getObjectType() {
        return ObjectType.class;
    }

    @Override
    String getActionName() {
        return NAME;
    }
}
