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
package com.evolveum.midpoint.model.common.expression.script;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * The expressions should be created by ExpressionFactory. They expect correct setting of
 * expression evaluator and proper conversion form the XML ExpressionType. Factory does this.
 *
 * @author Radovan Semancik
 */
public class ScriptExpression {

    private ScriptExpressionEvaluatorType scriptType;
    private ScriptEvaluator evaluator;
    private ItemDefinition outputDefinition;
	private Function<Object, Object> additionalConvertor;
    private ObjectResolver objectResolver;
    private Collection<FunctionLibrary> functions;
    
    private static final Trace LOGGER = TraceManager.getTrace(ScriptExpression.class);
	private static final int MAX_CODE_CHARS = 42;

    ScriptExpression(ScriptEvaluator evaluator, ScriptExpressionEvaluatorType scriptType) {
        this.scriptType = scriptType;
        this.evaluator = evaluator;
    }

    public ItemDefinition getOutputDefinition() {
		return outputDefinition;
	}

	public void setOutputDefinition(ItemDefinition outputDefinition) {
		this.outputDefinition = outputDefinition;
	}

	public ObjectResolver getObjectResolver() {
        return objectResolver;
    }

    public void setObjectResolver(ObjectResolver objectResolver) {
        this.objectResolver = objectResolver;
    }

	public Collection<FunctionLibrary> getFunctions() {
		return functions;
	}

	public void setFunctions(Collection<FunctionLibrary> functions) {
		this.functions = functions;
	}

	public Function<Object, Object> getAdditionalConvertor() {
		return additionalConvertor;
	}

	public void setAdditionalConvertor(Function<Object, Object> additionalConvertor) {
		this.additionalConvertor = additionalConvertor;
	}
	
	public <V extends PrismValue> List<V> evaluate(ExpressionVariables variables, ScriptExpressionReturnTypeType suggestedReturnType,
			boolean useNew, String contextDescription, Task task, OperationResult result)
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		ScriptExpressionEvaluationContext context = new ScriptExpressionEvaluationContext(variables, contextDescription, result, task, this);
		context.setEvaluateNew(useNew);

		try {
			context.setupThreadLocal();

			List<V> expressionResult = evaluator.evaluate(scriptType, variables, outputDefinition, additionalConvertor, suggestedReturnType, objectResolver, functions, contextDescription, task, result);

			traceExpressionSuccess(variables, contextDescription, expressionResult);
	        return expressionResult;

		} catch (ExpressionEvaluationException | ObjectNotFoundException | SchemaException | RuntimeException ex) {
			traceExpressionFailure(variables, contextDescription, ex);
			throw ex;
		} finally {
			context.cleanupThreadLocal();
		}
	}

    private void traceExpressionSuccess(ExpressionVariables variables, String shortDesc, Object returnValue) {
    	if (!isTrace()) {
    		return;
    	}
        trace("Script expression trace:\n"+
        		"---[ SCRIPT expression {}]---------------------------\n"+
        		"Language: {}\n"+
        		"Relativity mode: {}\n"+
        		"Variables:\n{}\n"+
        		"Code:\n{}\n"+
        		"Result: {}", shortDesc, evaluator.getLanguageName(), scriptType.getRelativityMode(), formatVariables(variables),
				formatCode(), SchemaDebugUtil.prettyPrint(returnValue));
    }

    private void traceExpressionFailure(ExpressionVariables variables, String shortDesc, Exception exception) {
        LOGGER.error("Expression error: {}", exception.getMessage(), exception);
        if (!isTrace()) {
    		return;
    	}
        trace("Script expression failure:\n"+
        		"---[ SCRIPT expression {}]---------------------------\n"+
        		"Language: {}\n"+
        		"Relativity mode: {}\n"+
        		"Variables:\n{}\n"+
        		"Code:\n{}\n"+
        		"Error: {}", shortDesc, evaluator.getLanguageName(), scriptType.getRelativityMode(), formatVariables(variables),
				formatCode(), SchemaDebugUtil.prettyPrint(exception));
    }

    private boolean isTrace() {
		return LOGGER.isTraceEnabled() || (scriptType != null && scriptType.isTrace() == Boolean.TRUE);
	}

	private void trace(String msg, Object... args) {
		if (scriptType != null && scriptType.isTrace() == Boolean.TRUE) {
			LOGGER.info(msg, args);
		} else {
			LOGGER.trace(msg, args);
		}
	}

	private String formatVariables(ExpressionVariables variables) {
		if (variables == null) {
			return "null";
		}
		return variables.formatVariables();
	}

	private String formatCode() {
		return DebugUtil.excerpt(scriptType.getCode().replaceAll("[\\s\\r\\n]+", " "), MAX_CODE_CHARS);
    }

	public ItemPath parsePath(String path) {
		if (path == null) {
			return null;
		}
        ItemPathType itemPathType = new ItemPathType(path);
        return itemPathType.getItemPath();
        // TODO what about namespaces?
//		Element codeElement = scriptType.getCode();
//		XPathHolder xPathHolder = new XPathHolder(path, codeElement);
//		if (xPathHolder == null) {
//			return null;
//		}
//		return xPathHolder.toItemPath();
	}

	@Override
	public String toString() {
		return "ScriptExpression(" + formatCode() + ")";
	}

}
