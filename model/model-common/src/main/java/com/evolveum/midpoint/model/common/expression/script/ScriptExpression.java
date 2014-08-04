/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.parser.XPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.w3c.dom.Element;

import javax.xml.namespace.QName;

import java.util.*;
import java.util.Map.Entry;

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

	public <T> List<PrismPropertyValue<T>> evaluate(ExpressionVariables variables, ScriptExpressionReturnTypeType suggestedReturnType, 
			boolean useNew, String contextDescription, OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		ScriptExpressionEvaluationContext context = new ScriptExpressionEvaluationContext(variables, contextDescription, result, this);
		context.setEvaluateNew(useNew);
		
		try {
			context.setupThreadLocal();
			
			List<PrismPropertyValue<T>> expressionResult = evaluator.evaluate(scriptType, variables, outputDefinition, suggestedReturnType, objectResolver, functions, contextDescription, result);
			
			traceExpressionSuccess(variables, contextDescription, expressionResult);
	        return expressionResult;

		} catch (ExpressionEvaluationException ex) {
			traceExpressionFailure(variables, contextDescription, ex);
			throw ex;
		} catch (ObjectNotFoundException ex) {
			traceExpressionFailure(variables, contextDescription, ex);
			throw ex;
		} catch (SchemaException ex) {
			traceExpressionFailure(variables, contextDescription, ex);
			throw ex;
		} catch (RuntimeException ex) {
			traceExpressionFailure(variables, contextDescription, ex);
			throw ex;
		} finally {
			context.cleanupThreadLocal();
		}
	}

    private void traceExpressionSuccess(ExpressionVariables variables, String shortDesc, Object returnValue) {
        if (LOGGER.isTraceEnabled()) {
        	LOGGER.trace("Script expression trace:\n"+
            		"---[ SCRIPT expression {}]---------------------------\n"+
            		"Language: {}\n"+
            		"Relativity mode: {}\n"+
            		"Variables:\n{}\n"+
            		"Code:\n{}\n"+
            		"Result: {}", new Object[]{
                    shortDesc, evaluator.getLanguageName(), scriptType.getRelativityMode(), formatVariables(variables), formatCode(),
                    SchemaDebugUtil.prettyPrint(returnValue)
            });
        }
    }

    private void traceExpressionFailure(ExpressionVariables variables, String shortDesc, Exception exception) {
        LOGGER.error("Expression error: {}", exception.getMessage(), exception);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Script expression failure:\n"+
            		"---[ SCRIPT expression {}]---------------------------\n"+
            		"Language: {}\n"+
            		"Relativity mode: {}\n"+
            		"Variables:\n{}\n"+
            		"Code:\n{}\n"+
            		"Error: {}", new Object[]{
                    shortDesc, evaluator.getLanguageName(), scriptType.getRelativityMode(), formatVariables(variables), formatCode(),
                    SchemaDebugUtil.prettyPrint(exception)
            });
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
