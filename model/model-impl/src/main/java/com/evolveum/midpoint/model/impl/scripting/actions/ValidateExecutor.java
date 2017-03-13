/*
 * Copyright (c) 2010-2016 Evolveum
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

import com.evolveum.midpoint.model.api.ScriptExecutionException;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.model.api.validator.Scope;
import com.evolveum.midpoint.model.api.validator.ValidationResult;
import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class ValidateExecutor extends BaseActionExecutor {

    @Autowired
    private ResourceValidator resourceValidator;

    private static final Trace LOGGER = TraceManager.getTrace(ValidateExecutor.class);

    private static final String NAME = "validate";

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public Data execute(ActionExpressionType expression, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {

        Data output = Data.createEmpty();

        for (PrismValue value : input.getData()) {
            context.checkTaskStop();
            if (value instanceof PrismObjectValue && ((PrismObjectValue) value).asObjectable() instanceof ResourceType) {
                PrismObject<ResourceType> resourceTypePrismObject = ((PrismObjectValue) value).asPrismObject();
                ResourceType resourceType = resourceTypePrismObject.asObjectable();
                long started = operationsHelper.recordStart(context, resourceType);
                try {
                    ValidationResult validationResult = resourceValidator.validate(resourceTypePrismObject, Scope.THOROUGH, null, context.getTask(), result);

                    PrismContainer pc = prismContext.getSchemaRegistry().findContainerDefinitionByElementName(SchemaConstantsGenerated.C_VALIDATION_RESULT).instantiate();
                    pc.add(validationResult.toValidationResultType().asPrismContainerValue());

                    context.println("Validated " + resourceTypePrismObject + ": " + validationResult.getIssues().size() + " issue(s)");
                    operationsHelper.recordEnd(context, resourceType, started, null);
                    output.addItem(pc);
                } catch (SchemaException|RuntimeException e) {
                    operationsHelper.recordEnd(context, resourceType, started, e);
                    context.println("Error validation " + resourceTypePrismObject + ": " + e.getMessage());
                    //noinspection ThrowableNotThrown
                    processActionException(e, NAME, value, context);
                }
            } else {
                //noinspection ThrowableNotThrown
                processActionException(new ScriptExecutionException("Item is not a PrismObject<ResourceType>"), NAME, value, context);
            }
        }
        return output;
    }
}
