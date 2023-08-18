/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.AddActionExpressionType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.util.Collection;

import jakarta.annotation.PostConstruct;

/**
 *
 */
@Component
public class AddExecutor extends AbstractObjectBasedActionExecutor<ObjectType> {

    private static final String NAME = "add";

    @PostConstruct
    public void init() {
        actionExecutorRegistry.register(NAME, AddActionExpressionType.class, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult globalResult) throws ScriptExecutionException, SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ModelExecuteOptions options = operationsHelper.getOptions(action, input, context, globalResult);
        boolean dryRun = operationsHelper.getDryRun(action, input, context, globalResult);

        iterateOverObjects(input, context, globalResult,
                (object, item, result) ->
                        add(object, dryRun, options, context, result),
                (object, exception) ->
                        context.println("Failed to add " + object + drySuffix(dryRun) + exceptionSuffix(exception))
        );

        return input;
    }

    private void add(PrismObject<? extends ObjectType> object, boolean dryRun, ModelExecuteOptions options, ExecutionContext context,
            OperationResult result) throws ScriptExecutionException {
        ObjectDelta<? extends ObjectType> addDelta = DeltaFactory.Object.createAddDelta(object);
        Collection<ObjectDeltaOperation<? extends ObjectType>> executedDeltas =
                operationsHelper.applyDelta(addDelta, options, dryRun, context, result);
        if (executedDeltas != null) {
            String newObjectOid = ObjectDeltaOperation.findAddDeltaOid(executedDeltas, object);
            object.setOid(newObjectOid);
        }
        context.println("Added " + object + drySuffix(dryRun));
    }

    @Override
    Class<ObjectType> getObjectType() {
        return ObjectType.class;
    }

    @Override
    @NotNull String getLegacyActionName() {
        return NAME;
    }

    @Override
    @NotNull String getConfigurationElementName() {
        return SchemaConstantsGenerated.SC_ADD.getLocalPart();
    }
}
