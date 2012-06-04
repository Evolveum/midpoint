/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression.jsr223;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.common.expression.MidPointFunctions;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ObjectType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * Expression evaluator that is using javax.script (JSR-223) engine.
 * 
 * @author Radovan Semancik
 *
 */
public class Jsr223ExpressionEvaluator implements ExpressionEvaluator {

	private static final String LANGUAGE_URL_BASE = MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX + "/expression/language#";

	private static final String FUNCTION_LIBRARY_VARIABLE_NAME = "func";
	
	private ScriptEngine scriptEngine;
	private PrismContext prismContext;
	
	public Jsr223ExpressionEvaluator(String engineName, PrismContext prismContext) {
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		scriptEngine = scriptEngineManager.getEngineByName(engineName);
		if (scriptEngine == null) {
			throw new SystemException("The JSR-223 scripting engine for '"+engineName+"' was not found");
		}
		this.prismContext = prismContext;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluateScalar(java.lang.Class, org.w3c.dom.Element, java.util.Map, com.evolveum.midpoint.schema.util.ObjectResolver, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <T> PrismPropertyValue<T> evaluateScalar(Class<T> type, Element code,
			Map<QName, Object> variables, ObjectResolver objectResolver, MidPointFunctions functionLibrary,
			String contextDescription, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
			SchemaException {
		Object evalRawResult = evaluate(type, code, variables, objectResolver, functionLibrary, contextDescription, result);
		T evalResult = convertScalarResult(type, evalRawResult, contextDescription);
		PrismPropertyValue<T> pval = new PrismPropertyValue<T>(evalResult);
		return pval;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluateList(java.lang.Class, org.w3c.dom.Element, java.util.Map, com.evolveum.midpoint.schema.util.ObjectResolver, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public <T> List<PrismPropertyValue<T>> evaluateList(Class<T> expectedType, Element code,
			Map<QName, Object> variables, ObjectResolver objectResolver, MidPointFunctions functionLibrary, 
			String contextDescription, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
			SchemaException {
		Object evalRawResult = evaluate(expectedType, code, variables, objectResolver, functionLibrary, contextDescription, result);
		List<PrismPropertyValue<T>> pvals = new ArrayList<PrismPropertyValue<T>>();
		if (evalRawResult instanceof Collection) {
			for(Object evalRawResultElement : (Collection)evalRawResult) {
				T evalResult = convertScalarResult(expectedType, evalRawResultElement, contextDescription);
				PrismPropertyValue<T> pval = new PrismPropertyValue<T>(evalResult);
				pvals.add(pval);
			}
		} else {
			T evalResult = convertScalarResult(expectedType, evalRawResult, contextDescription);
			PrismPropertyValue<T> pval = new PrismPropertyValue<T>(evalResult);
			pvals.add(pval);
		}
		return pvals;
	}
	
	private <T> T convertScalarResult(Class<T> expectedType, Object rawValue, String contextDescription) throws ExpressionEvaluationException {
		if (rawValue == null || expectedType.isInstance(rawValue)) {
			return (T)rawValue;
		}
		if (expectedType.equals(PolyString.class) && rawValue instanceof String) {
			return (T) new PolyString((String)rawValue);
		}
		if (expectedType.equals(PolyStringType.class) && rawValue instanceof String) {
			PolyStringType polyStringType = new PolyStringType();
			polyStringType.setOrig((String)rawValue);
			return (T) polyStringType;
		}
		if (expectedType.equals(String.class) && rawValue instanceof PolyString) {
			return (T)((PolyString)rawValue).getOrig();
		}
		if (expectedType.equals(String.class) && rawValue instanceof PolyStringType) {
			return (T)((PolyStringType)rawValue).getOrig();
		}
		throw new ExpressionEvaluationException("Expected "+expectedType+" from expression, but got "+rawValue.getClass()+" "+contextDescription);
	}
	
	private <T> Object evaluate(Class<T> type, Element codeElement, Map<QName, Object> variables, 
			ObjectResolver objectResolver, MidPointFunctions functionLibrary, 
			String contextDescription, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException,
			SchemaException {
		Bindings bindings = convertToBindings(variables, objectResolver, functionLibrary, contextDescription, result);
		String codeString = codeElement.getTextContent(); 
		try {
			return scriptEngine.eval(codeString, bindings);
		} catch (ScriptException e) {
			throw new ExpressionEvaluationException(e.getMessage() + " " + contextDescription, e);
		}
	}

	private Bindings convertToBindings(Map<QName, Object> variables, ObjectResolver objectResolver, MidPointFunctions functionLibrary,
			String contextDescription, OperationResult result) throws SchemaException, ObjectNotFoundException {
		Bindings bindings = scriptEngine.createBindings();
		bindings.put(FUNCTION_LIBRARY_VARIABLE_NAME, functionLibrary);
		for (Entry<QName, Object> variableEntry: variables.entrySet()) {
			if (variableEntry.getKey() == null) {
				// This is the "root" node. We have no use for it in JSR223, just skip it
				continue;
			}
			String variableName = variableEntry.getKey().getLocalPart();
			Object variableValue = convertVariableValue(variableEntry.getValue(), variableName, objectResolver, contextDescription, result);
			bindings.put(variableName, variableValue);
		}
		return bindings;
	}

	private Object convertVariableValue(Object originalValue, String variableName, ObjectResolver objectResolver,
			String contextDescription, OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (originalValue instanceof ObjectReferenceType) {
			originalValue = resolveReference((ObjectReferenceType)originalValue, objectResolver, variableName, 
					contextDescription, result);
		}
		if (originalValue instanceof PrismObject<?>) {
			return ((PrismObject<?>)originalValue).asObjectable();
		}
		if (originalValue instanceof PrismContainerValue<?>) {
			return ((PrismContainerValue<?>)originalValue).asContainerable();
		}
		if (originalValue instanceof PrismPropertyValue<?>) {
			return ((PrismPropertyValue<?>)originalValue).getValue();
		}
		return originalValue;
	}

	private Object resolveReference(ObjectReferenceType ref, ObjectResolver objectResolver, String name, String contextDescription, 
			OperationResult result) throws SchemaException, ObjectNotFoundException {
		if (ref.getOid() == null) {
    		throw new SchemaException("Null OID in reference in variable "+name+" in "+contextDescription);
    	} else {
	    	try {
	    		
				return objectResolver.resolve(ref, ObjectType.class, contextDescription, result);
				
			} catch (ObjectNotFoundException e) {
				throw new ObjectNotFoundException("Object not found during variable "+name+" resolution in "+contextDescription+": "+e.getMessage(),e, ref.getOid());
			} catch (SchemaException e) {
				throw new SchemaException("Schema error during variable "+name+" resolution in "+contextDescription+": "+e.getMessage(), e);
			}
    	}
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#getLanguageName()
	 */
	@Override
	public String getLanguageName() {
		return scriptEngine.getFactory().getLanguageName();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#getLanguageUrl()
	 */
	@Override
	public String getLanguageUrl() {
		return LANGUAGE_URL_BASE + getLanguageName();
	}

}
