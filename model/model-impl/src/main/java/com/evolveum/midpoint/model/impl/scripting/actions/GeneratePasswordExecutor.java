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
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.scripting.PipelineData;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mederly
 */
@Component
public class GeneratePasswordExecutor extends BaseActionExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(GeneratePasswordExecutor.class);

    private static final String NAME = "generate-password";

    @Autowired private ModelInteractionService modelInteraction;
    @Autowired private ValuePolicyProcessor policyProcessor;

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public PipelineData execute(ActionExpressionType expression, PipelineData input, ExecutionContext context, OperationResult globalResult) throws ScriptExecutionException {

        //boolean dryRun = getParamDryRun(expression, input, context, globalResult);

        for (PipelineItem item: input.getData()) {
            PrismValue value = item.getValue();
            OperationResult result = operationsHelper.createActionResult(item, this, context, globalResult);
            context.checkTaskStop();
            if (value instanceof PrismObjectValue && UserType.class.isAssignableFrom(((PrismObjectValue) value).asPrismObject().getCompileTimeClass())) {
                PrismObject<UserType> userObject = ((PrismObjectValue) value).asPrismObject();
                UserType user = userObject.asObjectable();
                long started = operationsHelper.recordStart(context, user);
                Throwable exception = null;
                try {
                    LOGGER.trace("Generating password for {}", userObject);
                    // TODO unify with ModelRestService.generateValue
                    ValuePolicyType valuePolicy = getPasswordPolicy(context, result, userObject);
                    String newClearValue = policyProcessor.generate(SchemaConstants.PATH_PASSWORD_VALUE, valuePolicy.getStringPolicy(), 10, userObject,
                            "generating value for password", context.getTask(), result);
                    ProtectedStringType newValue = new ProtectedStringType();
                    newValue.setClearValue(newClearValue);
                    ItemDelta<?, ?> itemDelta = DeltaBuilder.deltaFor(UserType.class, prismContext)
                            .item(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE)
                            .replace(newValue)
                            .asItemDelta();
                    itemDelta.applyTo(userObject);
                    operationsHelper.recordEnd(context, user, started, null);
                } catch (Throwable e) {
                    operationsHelper.recordEnd(context, user, started, e);
					exception = processActionException(e, NAME, value, context);
                }
                context.println((exception != null ? "Attempted to generate password for " : "Generated password for ") + userObject.toString() + exceptionSuffix(exception));
            } else {
				//noinspection ThrowableNotThrown
				processActionException(new ScriptExecutionException("Item is not a PrismObject<UserType>"), NAME, value, context);
            }
            operationsHelper.trimAndCloneResult(result, globalResult, context);
        }
        return input;
    }

    @NotNull
    private ValuePolicyType getPasswordPolicy(ExecutionContext context, OperationResult result, PrismObject<UserType> userObject)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        CredentialsPolicyType policy = modelInteraction.getCredentialsPolicy(userObject, context.getTask(), result);
        if (policy != null && policy.getPassword() != null && policy.getPassword().getPasswordPolicyRef() != null) {
            return resolvePolicyRef(policy.getPassword().getPasswordPolicyRef().getOid(), context, result);
        }
        SystemConfigurationType systemConfiguration = modelInteraction.getSystemConfiguration(result);
        if (systemConfiguration.getGlobalPasswordPolicyRef() != null
                && systemConfiguration.getGlobalPasswordPolicyRef().getOid() != null) {
            return resolvePolicyRef(systemConfiguration.getGlobalPasswordPolicyRef().getOid(), context, result);
        }
        throw new ObjectNotFoundException("No applicable password policy found");
    }

    @NotNull
    private ValuePolicyType resolvePolicyRef(String oid, ExecutionContext context, OperationResult result)
            throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException {
        return modelService.getObject(ValuePolicyType.class,
                oid, null, context.getTask(), result).asObjectable();
    }
}
