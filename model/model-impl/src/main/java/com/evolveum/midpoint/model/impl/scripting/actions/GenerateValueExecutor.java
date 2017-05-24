/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemTargetType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.PolicyItemsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class GenerateValueExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(GenerateValueExecutor.class);

    private static final String NAME = "generate-value";
    public static final String PARAMETER_ITEMS = "items";

    @Autowired private ModelInteractionService modelInteraction;

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        PolicyItemsDefinitionType itemsDefinition = expressionHelper.getSingleArgumentValue(expression.getParameter(),
                PARAMETER_ITEMS, false, false, PARAMETER_ITEMS, input, context,
                PolicyItemsDefinitionType.class, globalResult);
        if (itemsDefinition == null) {
            itemsDefinition = new PolicyItemsDefinitionType().policyItemDefinition(
                    new PolicyItemDefinitionType()
                        .target(new PolicyItemTargetType().path(new ItemPathType(new ItemPath(UserType.F_CREDENTIALS,
                                CredentialsType.F_PASSWORD, PasswordType.F_VALUE))))
                        .execute(false));
        }

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
            context.checkTaskStop();
            if (value instanceof PrismObjectValue) {
                PrismObject<? extends ObjectType> object = ((PrismObjectValue) value).asPrismObject();
                ObjectType objectBean = object.asObjectable();
                long started = operationsHelper.recordStart(context, objectBean);
                Throwable exception = null;
                try {
                    LOGGER.trace("Generating value(s) for {}", objectBean);
                    modelInteraction.generateValue(object, itemsDefinition, context.getTask(), result);
                    operationsHelper.recordEnd(context, objectBean, started, null);
                } catch (Throwable e) {
                    operationsHelper.recordEnd(context, objectBean, started, e);
					exception = processActionException(e, NAME, value, context);
                }
                context.println((exception != null ? "Attempted to generate value(s) for " : "Generated value(s) for ") + objectBean.toString() + exceptionSuffix(exception));
            } else {
				//noinspection ThrowableNotThrown
				processActionException(new ScriptExecutionException("Item is not a PrismObject"), NAME, value, context);
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
        }
        return input;
    }
}
