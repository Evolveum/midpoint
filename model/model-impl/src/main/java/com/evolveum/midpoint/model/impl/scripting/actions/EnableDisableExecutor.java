/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import static com.evolveum.midpoint.schema.util.ScriptingBeansUtil.getActionType;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.DisableActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.EnableActionExpressionType;

/**
 * Implements "enable" and "disable" actions.
 * It is ObjectType-typed just because it handles both FocusType and ShadowType objects.
 */
@Component
public class EnableDisableExecutor extends AbstractObjectBasedActionExecutor<ObjectType> {

    private static final String NAME_ENABLE = "enable";
    private static final String NAME_DISABLE = "disable";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME_ENABLE, EnableActionExpressionType.class, this);
        actionExecutorRegistry.register(NAME_DISABLE, DisableActionExpressionType.class, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult globalResult) throws ScriptExecutionException, SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ModelExecuteOptions options = operationsHelper.getOptions(action, input, context, globalResult);
        boolean dryRun = operationsHelper.getDryRun(action, input, context, globalResult);
        boolean isEnable = NAME_ENABLE.equals(getActionType(action));

        iterateOverObjects(input, context, globalResult,
                (object, item, result) ->
                        enableOrDisable(object.asObjectable(), dryRun, options, isEnable, context, result),
                (object, exception) ->
                        context.println("Failed to " + (isEnable?"enable":"disable") + object + drySuffix(dryRun) + exceptionSuffix(exception))
        );

        return input;
    }

    private void enableOrDisable(ObjectType object, boolean dryRun, ModelExecuteOptions options, boolean isEnable,
            ExecutionContext context, OperationResult result) throws ScriptExecutionException, SchemaException {
        if (object instanceof FocusType || object instanceof ShadowType) {
            ObjectDelta<? extends ObjectType> delta = createEnableDisableDelta(object, isEnable);
            operationsHelper.applyDelta(delta, options, dryRun, context, result);
            context.println((isEnable ? "Enabled " : "Disabled ") + object + optionsSuffix(options, dryRun));
        } else {
            throw new ScriptExecutionException("Object is not a FocusType nor ShadowType: " + object);
        }
    }

    private ObjectDelta<? extends ObjectType> createEnableDisableDelta(ObjectType object, boolean isEnable)
            throws SchemaException {
        return prismContext.deltaFor(object.getClass())
                .item(PATH_ACTIVATION_ADMINISTRATIVE_STATUS)
                    .replace(isEnable ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED)
                .asObjectDelta(object.getOid());
    }

    @Override
    Class<ObjectType> getObjectType() {
        return ObjectType.class;
    }

    @Override
    String getActionName() {
        return "enable-disable";
    }
}
