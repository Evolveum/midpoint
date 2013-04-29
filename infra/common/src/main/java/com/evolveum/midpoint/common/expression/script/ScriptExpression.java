/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression.script;

import com.evolveum.midpoint.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScriptExpressionReturnTypeType;

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

    private ScriptExpressionEvaluatorType expressionType;
    private ScriptEvaluator evaluator;
    private ItemDefinition outputDefinition;
    private ObjectResolver objectResolver;
    private Collection<FunctionLibrary> functions;

    private static final Trace LOGGER = TraceManager.getTrace(ScriptExpression.class);

    ScriptExpression(ScriptEvaluator evaluator, ScriptExpressionEvaluatorType expressionType) {
        this.expressionType = expressionType;
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

	public <T> List<PrismPropertyValue<T>> evaluate(ScriptVariables variables, ScriptExpressionReturnTypeType suggestedReturnType, 
			String contextDescription, OperationResult result) 
			throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {

		ScriptExpressionEvaluationContext context = new ScriptExpressionEvaluationContext(variables, contextDescription, result, this);
		
		try {
			context.setupThreadLocal();
			
			List<PrismPropertyValue<T>> expressionResult = evaluator.evaluate(expressionType, variables, outputDefinition, suggestedReturnType, objectResolver, functions, contextDescription, result);
			
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

    private void traceExpressionSuccess(ScriptVariables variables, String shortDesc, Object returnValue) {
        if (LOGGER.isTraceEnabled()) {
        	LOGGER.trace("Expression trace:\n"+
            		"---[ EXPRESSION {}]---------------------------\n"+
            		"Language: {}\n"+
            		"Variables:\n{}\n"+
            		"Code:\n{}\n"+
            		"Result: {}", new Object[]{
                    shortDesc, evaluator.getLanguageName(), formatVariables(variables), formatCode(),
                    SchemaDebugUtil.prettyPrint(returnValue)
            });
        }
    }

    private void traceExpressionFailure(ScriptVariables variables, String shortDesc, Exception exception) {
        LOGGER.error("Expression error: {}", exception.getMessage(), exception);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Expression trace:\n"+
            		"---[ EXPRESSION {}]---------------------------\n"+
            		"Language: {}\n"+
            		"Variables:\n{}\n"+
            		"Code:\n{}\n"+
            		"Error: {}", new Object[]{
                    shortDesc, evaluator.getLanguageName(), formatVariables(variables), formatCode(),
                    SchemaDebugUtil.prettyPrint(exception)
            });
        }
    }

	private String formatVariables(ScriptVariables variables) {
		if (variables == null) {
			return "null";
		}
		return variables.formatVariables();
	}

	private String formatCode() {
        return DOMUtil.serializeDOMToString(expressionType.getCode());
    }
	
	public ItemPath parsePath(String path) {
		if (path == null) {
			return null;
		}
		Element codeElement = expressionType.getCode();
		XPathHolder xPathHolder = new XPathHolder(path, codeElement);
		if (xPathHolder == null) {
			return null;
		}
		return xPathHolder.toItemPath();
	}

}
