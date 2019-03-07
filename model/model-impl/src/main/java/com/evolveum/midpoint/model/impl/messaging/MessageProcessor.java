/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.model.impl.messaging;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.expression.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Component
public class MessageProcessor {

	private static final Trace LOGGER = TraceManager.getTrace(MessageProcessor.class);

	public static final String DOT_CLASS = MessageProcessor.class.getName() + ".";

	@Autowired ModelService modelService;
	@Autowired ExpressionFactory expressionFactory;
	@Autowired PrismContext prismContext;
	@Autowired SecurityContextManager securityContextManager;

//	public void processMessage(DataMessageType message, MessageProcessingConfigurationType processing,
//			ResourceType resource, Task task, OperationResult parentResult)
//			throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
//			ConfigurationException, ExpressionEvaluationException, PreconditionViolationException, PolicyViolationException,
//			ObjectAlreadyExistsException {
//		OperationResult result = parentResult.createSubresult(DOT_CLASS + "processMessage");
//		try {
//			ExpressionVariables variables = createVariables(message, resource);
//			if (processing.getConsumerExpression() != null && processing.getTransformerExpression() != null) {
//				throw new IllegalStateException("Both consumerExpression and transformerExpression cannot be specified at once");
//			}
//			if (processing.getConsumerExpression() != null) {
//				evaluateExpression(processing.getConsumerExpression(), variables, "consumer expression", task, result);
//			} else {
//				List<ResourceObjectShadowChangeDescriptionType> descriptions = evaluateExpression(processing.getTransformerExpression(),
//						variables, "transformer expression", task, result);
//				LOGGER.trace("Change description computation returned {} description(s)", descriptions.size());
//				for (ResourceObjectShadowChangeDescriptionType description : descriptions) {
//					modelService.notifyChange(description, task, result);
//				}
//			}
//			result.computeStatusIfUnknown();
//		} catch (Throwable t) {
//			result.recordFatalError("Couldn't process message: " + t.getMessage(), t);
//			throw t;
//		}
//	}
//
//	@NotNull
//	private <T> List<T> evaluateExpression(ExpressionType expressionBean, ExpressionVariables variables, String contextDescription,
//			Task task, OperationResult result)
//			throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException,
//			ConfigurationException, SecurityViolationException {
//		Expression<PrismPropertyValue<T>, PrismPropertyDefinition<T>> expression = expressionFactory.makePropertyExpression(expressionBean,
//				SchemaConstantsGenerated.C_RESOURCE_OBJECT_SHADOW_CHANGE_DESCRIPTION, contextDescription, task, result);
//		ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDescription, task, result);
//		PrismValueDeltaSetTriple<PrismPropertyValue<T>> exprResultTriple =
//				ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, context, task, result);
//		List<T> list = new ArrayList<>();
//		for (PrismPropertyValue<T> pv : exprResultTriple.getZeroSet()) {
//			list.add(pv.getRealValue());
//		}
//		return list;
//	}
//
//	@NotNull
//	private ExpressionVariables createVariables(DataMessageType message,
//			ResourceType resource) {
//		ExpressionVariables variables = new ExpressionVariables();
//		variables.addVariableDefinition(ExpressionConstants.VAR_MESSAGE, new MessageWrapper(message));
//		variables.addVariableDefinition(ExpressionConstants.VAR_RESOURCE, resource);
//		return variables;
//	}
}
