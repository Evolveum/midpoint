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
package com.evolveum.midpoint.common.expression.script.jsr223;

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

import com.evolveum.midpoint.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.common.expression.script.ScriptEvaluator;
import com.evolveum.midpoint.common.expression.script.ScriptVariables;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ScriptExpressionReturnTypeType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;

/**
 * Expression evaluator that is using javax.script (JSR-223) engine.
 * 
 * @author Radovan Semancik
 *
 */
public class Jsr223ScriptEvaluator implements ScriptEvaluator {

	private static final String LANGUAGE_URL_BASE = MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX + "/expression/language#";

	private ScriptEngine scriptEngine;
	private PrismContext prismContext;
	
	public Jsr223ScriptEvaluator(String engineName, PrismContext prismContext) {
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		scriptEngine = scriptEngineManager.getEngineByName(engineName);
		if (scriptEngine == null) {
			throw new SystemException("The JSR-223 scripting engine for '"+engineName+"' was not found");
		}
		this.prismContext = prismContext;
	}
	
	@Override
	public <T> List<PrismPropertyValue<T>> evaluate(ScriptExpressionEvaluatorType expressionType,
			ScriptVariables variables, ItemDefinition outputDefinition, ScriptExpressionReturnTypeType suggestedReturnType, 
			ObjectResolver objectResolver, Collection<FunctionLibrary> functions,
			String contextDescription, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, ExpressionSyntaxException {
		
		Bindings bindings = convertToBindings(variables, objectResolver, functions, contextDescription, result);
		
		Element codeElement = expressionType.getCode();
		if (codeElement == null) {
			throw new ExpressionEvaluationException("No script code in " + contextDescription);
		}
		String codeString = codeElement.getTextContent();
		
		boolean allowEmptyValues = false;
		if (expressionType.isAllowEmptyValues() != null) {
			allowEmptyValues = expressionType.isAllowEmptyValues();
		}
		
		Object evalRawResult;
		try {
			evalRawResult = scriptEngine.eval(codeString, bindings);
		} catch (ScriptException e) {
			throw new ExpressionEvaluationException(e.getMessage() + " " + contextDescription, e);
		}
		
		QName xsdReturnType = outputDefinition.getTypeName();
		
		Class<T> javaReturnType = XsdTypeMapper.toJavaType(xsdReturnType);
		if (javaReturnType == null) {
			javaReturnType = prismContext.getPrismJaxbProcessor().getCompileTimeClass(xsdReturnType);
		}
        
		List<PrismPropertyValue<T>> pvals = new ArrayList<PrismPropertyValue<T>>();
		
		if (evalRawResult instanceof Collection) {
			for(Object evalRawResultElement : (Collection)evalRawResult) {
				T evalResult = convertScalarResult(javaReturnType, evalRawResultElement, contextDescription);
				if (allowEmptyValues || !isEmpty(evalResult)) {
					PrismPropertyValue<T> pval = new PrismPropertyValue<T>(evalResult);
					pvals.add(pval);
				}
			}
		} else if (evalRawResult instanceof PrismProperty<?>) {
			pvals.addAll(PrismPropertyValue.cloneCollection(((PrismProperty<T>)evalRawResult).getValues()));
		} else {
			T evalResult = convertScalarResult(javaReturnType, evalRawResult, contextDescription);
			if (allowEmptyValues || !isEmpty(evalResult)) {
				PrismPropertyValue<T> pval = new PrismPropertyValue<T>(evalResult);
				pvals.add(pval);
			}
		}
		
//		ScriptExpressionReturnTypeType definedReturnType = expressionType.getReturnType();
//		if (definedReturnType == ScriptExpressionReturnTypeType.LIST) {
//			
//		}
				
		return pvals;
	}
	
	private <T> T convertScalarResult(Class<T> expectedType, Object rawValue, String contextDescription) throws ExpressionEvaluationException {
		if (rawValue == null || expectedType.isInstance(rawValue)) {
			return (T)rawValue;
		}
		if (rawValue instanceof PrismPropertyValue<?>) {
			rawValue = ((PrismPropertyValue<?>)rawValue).getValue();
		}
		// This really needs to be checked twice
		if (rawValue == null || expectedType.isInstance(rawValue)) {
			return (T)rawValue;
		}
		
		// Primitive types
		if (expectedType == boolean.class && rawValue instanceof Boolean) {
			return (T) ((Boolean)rawValue);
		}
		if (expectedType.equals(int.class) && rawValue instanceof Integer) {
			return (T)((Integer)rawValue);
		}
		if (expectedType.equals(long.class) && rawValue instanceof Long) {
			return (T)((Long)rawValue);
		}
		if (expectedType.equals(float.class) && rawValue instanceof Float) {
			return (T)((Float)rawValue);
		}
		if (expectedType.equals(double.class) && rawValue instanceof Double) {
			return (T)((Double)rawValue);
		}
		if (expectedType.equals(byte.class) && rawValue instanceof Byte) {
			return (T)((Byte)rawValue);
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
		if (expectedType.equals(PolyString.class) && rawValue instanceof PolyStringType) {
			return (T) ((PolyStringType)rawValue).toPolyString();
		}
		if (expectedType.equals(PolyStringType.class) && rawValue instanceof PolyString) {
			PolyStringType polyStringType = new PolyStringType((PolyString)rawValue);
			return (T) polyStringType;
		}
		throw new ExpressionEvaluationException("Expected "+expectedType+" from expression, but got "+rawValue.getClass()+" "+contextDescription);
	}
	
	private <T> boolean isEmpty(T val) {
		if (val == null) {
			return true;
		}
		if (val instanceof String && ((String)val).isEmpty()) {
			return true;
		}
		if (val instanceof PolyString && ((PolyString)val).isEmpty()) {
			return true;
		}
		return false;
	}
	
	private Bindings convertToBindings(ScriptVariables variables, ObjectResolver objectResolver, 
			Collection<FunctionLibrary> functions,
			String contextDescription, OperationResult result) throws ExpressionSyntaxException, ObjectNotFoundException {
		Bindings bindings = scriptEngine.createBindings();
		// Functions
		if (functions != null) {
			for (FunctionLibrary funcLib: functions) {
				bindings.put(funcLib.getVariableName(), funcLib.getGenericFunctions());
			}
		}
		// Variables
		if (variables != null) {
			for (Entry<QName, Object> variableEntry: variables.entrySet()) {
				if (variableEntry.getKey() == null) {
					// This is the "root" node. We have no use for it in JSR223, just skip it
					continue;
				}
				String variableName = variableEntry.getKey().getLocalPart();
				Object variableValue = convertVariableValue(variableEntry.getValue(), variableName, objectResolver, contextDescription, result);
				bindings.put(variableName, variableValue);
			}
		}
		return bindings;
	}

	private Object convertVariableValue(Object originalValue, String variableName, ObjectResolver objectResolver,
			String contextDescription, OperationResult result) throws ExpressionSyntaxException, ObjectNotFoundException {
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
			OperationResult result) throws ExpressionSyntaxException, ObjectNotFoundException {
		if (ref.getOid() == null) {
    		throw new ExpressionSyntaxException("Null OID in reference in variable "+name+" in "+contextDescription);
    	} else {
	    	try {
	    		
				return objectResolver.resolve(ref, ObjectType.class, contextDescription, result);
				
			} catch (ObjectNotFoundException e) {
				throw new ObjectNotFoundException("Object not found during variable "+name+" resolution in "+contextDescription+": "+e.getMessage(),e, ref.getOid());
			} catch (SchemaException e) {
				throw new ExpressionSyntaxException("Schema error during variable "+name+" resolution in "+contextDescription+": "+e.getMessage(), e);
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
