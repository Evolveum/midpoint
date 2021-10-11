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
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.impl.scripting.helpers.OperationsHelper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS;

/**
 * @author mederly
 */
@Component
public class EnableDisableExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(EnableDisableExecutor.class);

    private static final String NAME_ENABLE = "enable";
    private static final String NAME_DISABLE = "disable";

    @Autowired
    private OperationsHelper operationsHelper;

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME_ENABLE, this);
        scriptingExpressionEvaluator.registerActionExecutor(NAME_DISABLE, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        boolean isEnable = NAME_ENABLE.equals(expression.getType());
        ModelExecuteOptions executionOptions = getOptions(expression, input, context, globalResult);
        boolean dryRun = getParamDryRun(expression, input, context, globalResult);

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
            result.addParam("operation", expression.getType());
            context.checkTaskStop();
            if (value instanceof PrismObjectValue) {
                @SuppressWarnings({"unchecked", "raw"})
                PrismObject<? extends ObjectType> prismObject = ((PrismObjectValue) value).asPrismObject();
                ObjectType objectType = prismObject.asObjectable();
                long started = operationsHelper.recordStart(context, objectType);
                Throwable exception = null;
                try {
                    if (objectType instanceof FocusType) {
                        operationsHelper.applyDelta(createEnableDisableDelta((FocusType) objectType, isEnable), executionOptions, dryRun, context, result);
                    } else if (objectType instanceof ShadowType) {
                        operationsHelper.applyDelta(createEnableDisableDelta((ShadowType) objectType, isEnable), executionOptions, dryRun, context, result);
                    } else {
                        throw new ScriptExecutionException("Item is not a FocusType nor ShadowType: " + value.toString());
                    }
                    operationsHelper.recordEnd(context, objectType, started, null);
                } catch (Throwable ex) {
                    operationsHelper.recordEnd(context, objectType, started, ex);
                    exception = processActionException(ex, expression.getType(), value, context);
                }
                context.println((exception != null ? "Attempted to " + expression.getType() : (isEnable ? "Enabled " : "Disabled "))
                        + prismObject.toString() + optionsSuffix(executionOptions, dryRun) + exceptionSuffix(exception));
            } else {
                //noinspection ThrowableNotThrown
                processActionException(new ScriptExecutionException("Item is not a PrismObject"), expression.getType(), value, context);
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
        }
        return input;
    }

    private ObjectDelta<? extends ObjectType> createEnableDisableDelta(FocusType focus, boolean isEnable) {
        return prismContext.deltaFactory().object().createModificationReplaceProperty(focus.getClass(),
                focus.getOid(), PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                isEnable ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED);
    }

    private ObjectDelta<? extends ObjectType> createEnableDisableDelta(ShadowType shadow, boolean isEnable) {
        return prismContext.deltaFactory().object().createModificationReplaceProperty(shadow.getClass(),
                shadow.getOid(), PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                isEnable ? ActivationStatusType.ENABLED : ActivationStatusType.DISABLED);
    }

}
