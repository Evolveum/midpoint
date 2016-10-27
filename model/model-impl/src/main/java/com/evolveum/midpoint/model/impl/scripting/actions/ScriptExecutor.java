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
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
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
        for (Item item : input.getData()) {
        	long started;
			if (item instanceof PrismObject) {
				started = operationsHelper.recordStart(context, asObjectType(item));
			} else {
				started = 0;
			}
			try {
				Object outObject = executeScript(scriptExpression, item, context, result);
				if (outObject != null && outputDefinition != null) {
					output.addItem(createItem(outObject, outputDefinition));
				}
				if (item instanceof PrismObject) {
					operationsHelper.recordEnd(context, asObjectType(item), started, null);
				}
			} catch (Throwable ex) {
				if (item instanceof PrismObject) {
					operationsHelper.recordEnd(context, asObjectType(item), started, ex);
				}
				throw new ScriptExecutionException("Couldn't execute script action: " + ex.getMessage(), ex);
			}
			context.println("Executed script on " + item);
        }
        return output;
    }

    // UGLY HACKING
	private Item createItem(Object outObject, ItemDefinition<?> itemDefinition) throws SchemaException {
		if (outObject instanceof Item) {
			return (Item) outObject;			// probably won't occur
		}
		Item item = itemDefinition.instantiate();
		if (outObject instanceof Collection) {
			for (Object o : (Collection) outObject) {
				addToItem(item, o);
			}
		} else {
			addToItem(item, outObject);
		}
		return item;
	}

	private void addToItem(Item item, Object value) throws SchemaException {
		if (value instanceof PrismValue) {
			PrismValue pv = (PrismValue) value;
			if (pv.getParent() instanceof PrismObject && item instanceof PrismObject) {
				((PrismObject) item).setOid(((PrismObject) (pv.getParent())).getOid());
			}
			item.add(((PrismValue) value).clone());
		} else {
			throw new UnsupportedOperationException("Adding " + value + " to " + item + " is not supported yet.");
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

	private Object executeScript(ScriptExpression scriptExpression, Item inputItem, ExecutionContext context, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, inputItem);
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

	private ObjectType asObjectType(Item item) {
		return (ObjectType) ((PrismObject) item).asObjectable();
	}

}
