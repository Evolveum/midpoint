/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.DisableActionExpressionType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.EnableActionExpressionType;
import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;

/**
 * Implements "enable" and "disable" actions.
 * It is ObjectType-typed just because it handles both FocusType and ShadowType objects.
 */
public abstract class EnableDisableExecutor extends AbstractObjectBasedActionExecutor<ObjectType> {

    @Override
    public PipelineData execute(ActionExpressionType action, PipelineData input, ExecutionContext context,
            OperationResult globalResult) throws ScriptExecutionException, SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {

        ModelExecuteOptions options = operationsHelper.getOptions(action, input, context, globalResult);
        boolean dryRun = operationsHelper.getDryRun(action, input, context, globalResult);

        iterateOverObjects(input, context, globalResult,
                (object, item, result) ->
                        enableOrDisable(object.asObjectable(), dryRun, options, context, result),
                (object, exception) ->
                        context.println("Failed to " + getVerb() + " " + object + drySuffix(dryRun) + exceptionSuffix(exception))
        );

        return input;
    }

    abstract String getVerb();

    abstract String getVerbPast();

    abstract ActivationStatusType getTargetStatus();

    private void enableOrDisable(
            ObjectType object, boolean dryRun, ModelExecuteOptions options,
            ExecutionContext context, OperationResult result) throws ScriptExecutionException, SchemaException {
        if (object instanceof FocusType || object instanceof ShadowType) {
            ObjectDelta<? extends ObjectType> delta = createEnableDisableDelta(object);
            operationsHelper.applyDelta(delta, options, dryRun, context, result);
            context.println(getVerbPast() + " " + object + optionsSuffix(options, dryRun));
        } else {
            throw new ScriptExecutionException("Object is not a FocusType nor ShadowType: " + object);
        }
    }

    private ObjectDelta<? extends ObjectType> createEnableDisableDelta(ObjectType object)
            throws SchemaException {
        return prismContext.deltaFor(object.getClass())
                .item(PATH_ACTIVATION_ADMINISTRATIVE_STATUS).replace(getTargetStatus())
                .asObjectDelta(object.getOid());
    }

    @Override
    Class<ObjectType> getObjectType() {
        return ObjectType.class;
    }

    @Component
    static class Enable extends EnableDisableExecutor {

        private static final String NAME = "enable";

        @PostConstruct
        public void init() {
            actionExecutorRegistry.register(NAME, EnableActionExpressionType.class, this);
        }

        @Override
        @NotNull
        String getLegacyActionName() {
            return NAME;
        }

        @Override
        @NotNull String getConfigurationElementName() {
            return SchemaConstantsGenerated.SC_ENABLE.getLocalPart();
        }

        @Override
        String getVerb() {
            return "enabled";
        }

        @Override
        String getVerbPast() {
            return "Enabled";
        }

        @Override
        ActivationStatusType getTargetStatus() {
            return ActivationStatusType.ENABLED;
        }
    }

    @Component
    static class Disable extends EnableDisableExecutor {

        private static final String NAME = "disable";

        @PostConstruct
        public void init() {
            actionExecutorRegistry.register(NAME, DisableActionExpressionType.class, this);
        }
        @Override
        @NotNull
        String getLegacyActionName() {
            return NAME;
        }

        @Override
        @NotNull String getConfigurationElementName() {
            return SchemaConstantsGenerated.SC_DISABLE.getLocalPart();
        }

        @Override
        String getVerb() {
            return "disable";
        }

        @Override
        String getVerbPast() {
            return "Disabled";
        }

        @Override
        ActivationStatusType getTargetStatus() {
            return ActivationStatusType.DISABLED;
        }
    }
}
