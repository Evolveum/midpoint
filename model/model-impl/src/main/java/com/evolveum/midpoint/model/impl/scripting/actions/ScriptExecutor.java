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
import com.evolveum.midpoint.model.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpression;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionFactory;
import com.evolveum.midpoint.model.impl.scripting.Data;
import com.evolveum.midpoint.model.impl.scripting.ExecutionContext;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ActionExpressionType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
@Component
public class ScriptExecutor extends BaseActionExecutor {

    //private static final Trace LOGGER = TraceManager.getTrace(ScriptExecutor.class);

	@Autowired
	private ScriptExpressionFactory scriptExpressionFactory;

    private static final String NAME = "execute-script";
    private static final String PARAM_SCRIPT = "script";
    private static final String PARAM_OUTPUT_ITEM = "outputItem";		// item name or type (as URI!) -- EXPERIMENTAL

    @PostConstruct
    public void init() {
        scriptingExpressionEvaluator.registerActionExecutor(NAME, this);
    }

    @Override
    public Data execute(ActionExpressionType expression, Data input, ExecutionContext context, OperationResult result) throws ScriptExecutionException {

		ScriptExpressionEvaluatorType script = expressionHelper.getSingleArgumentValue(expression.getParameter(), PARAM_SCRIPT, true, true,
				NAME, input, context, ScriptExpressionEvaluatorType.class, result);
		String outputItem = expressionHelper.getSingleArgumentValue(expression.getParameter(), PARAM_OUTPUT_ITEM, false, false,
				NAME, input, context, String.class, result);

		ItemDefinition<?> outputDefinition = getItemDefinition(outputItem);

		ScriptExpression scriptExpression;
		try {
			scriptExpression = scriptExpressionFactory.createScriptExpression(script, outputDefinition, "script");
		} catch (ExpressionSyntaxException e) {
			throw new ScriptExecutionException("Couldn't parse script expression: " + e.getMessage(), e);
		}

		Data output = Data.createEmpty();
        for (PrismValue value: input.getData()) {
			context.checkTaskStop();
			String valueDescription;
        	long started;
			if (value instanceof PrismObjectValue) {
				started = operationsHelper.recordStart(context, asObjectType(value));
				valueDescription = asObjectType(value).asPrismObject().toString();
			} else {
				started = 0;
				valueDescription = value.toHumanReadableString();
			}
			try {
				Object outObject = executeScript(scriptExpression, value, context, result);
				if (outObject != null) {
					addToData(outObject, output);
				}
				if (value instanceof PrismObjectValue) {
					operationsHelper.recordEnd(context, asObjectType(value), started, null);
				}
			} catch (Throwable ex) {
				if (value instanceof PrismObjectValue) {
					operationsHelper.recordEnd(context, asObjectType(value), started, ex);
				}
				throw new ScriptExecutionException("Couldn't execute script action: " + ex.getMessage(), ex);
			}
			context.println("Executed script on " + valueDescription);
        }
        return output;
    }

	private void addToData(Object outObject, Data output) throws SchemaException {
		if (outObject == null) {
			return;
		} else if (outObject instanceof Collection) {
			for (Object o : (Collection) outObject) {
				addToData(o, output);
			}
		} else {
			PrismValue value;
			if (outObject instanceof PrismValue) {
				value = (PrismValue) outObject;
			} else if (outObject instanceof Objectable) {
				value = new PrismObjectValue<>((Objectable) outObject, prismContext);
			} else if (outObject instanceof Containerable) {
				value = new PrismContainerValue<>((Containerable) outObject, prismContext);
			} else {
				value = new PrismPropertyValue<>(outObject, prismContext);
			}
			output.addValue(value);
		}
	}

	private ItemDefinition<?> getItemDefinition(String itemUri) throws ScriptExecutionException {
		if (StringUtils.isBlank(itemUri)) {
			return null;
		}

		QName itemName = QNameUtil.uriToQName(itemUri, true);
		ItemDefinition def = prismContext.getSchemaRegistry().findItemDefinitionByElementName(itemName);
		if (def != null) {
			return def;
		}
		def = prismContext.getSchemaRegistry().findItemDefinitionByType(itemName);
		if (def != null) {
			return def;
		}
		throw new ScriptExecutionException("Supplied item identification " + itemUri + " corresponds neither to item name nor type name");
	}

	private Object executeScript(ScriptExpression scriptExpression, PrismValue prismValue, ExecutionContext context, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, prismValue);
		variables.addVariableDefinition(ExpressionConstants.VAR_PRISM_CONTEXT, prismContext);
		ExpressionUtil.addActorVariable(variables, securityEnforcer);
		
		List<?> rv = Utils.evaluateScript(scriptExpression, null, variables, true, "script action", context.getTask(), result);
		
		if (rv == null || rv.size() == 0) {
			return null;
		} else if (rv.size() == 1) {
			return rv.get(0);
		} else {
			return rv;		// shouldn't occur; would cause problems
		}
	}

	private ObjectType asObjectType(PrismValue value) {
		return (ObjectType) ((PrismObjectValue) value).asObjectable();
	}

}
