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
package com.evolveum.midpoint.model.common.expression.script.jsr223;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.model.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.functions.BasicExpressionFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.script.ScriptEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
	private Protector protector;
	
	private Map<String, CompiledScript> scriptCache;
	
	public Jsr223ScriptEvaluator(String engineName, PrismContext prismContext, Protector protector) {
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		scriptEngine = scriptEngineManager.getEngineByName(engineName);
		if (scriptEngine == null) {
			throw new SystemException("The JSR-223 scripting engine for '"+engineName+"' was not found");
		}
		this.prismContext = prismContext;
		this.protector = protector;
		this.scriptCache = new ConcurrentHashMap<String, CompiledScript>();
	}
	
	@Override
	public <T, V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluatorType expressionType,
			ExpressionVariables variables, ItemDefinition outputDefinition, ScriptExpressionReturnTypeType suggestedReturnType, 
			ObjectResolver objectResolver, Collection<FunctionLibrary> functions,
			String contextDescription, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, ExpressionSyntaxException {
		
		Bindings bindings = convertToBindings(variables, objectResolver, functions, contextDescription, result);
		
		String codeString = expressionType.getCode();
		if (codeString == null) {
			throw new ExpressionEvaluationException("No script code in " + contextDescription);
		}

		boolean allowEmptyValues = false;
		if (expressionType.isAllowEmptyValues() != null) {
			allowEmptyValues = expressionType.isAllowEmptyValues();
		}
		
		CompiledScript compiledScript = createCompiledScript(codeString, contextDescription);
		
		Object evalRawResult;
		try {
			InternalMonitor.recordScriptExecution();
			evalRawResult = compiledScript.eval(bindings);
		} catch (ScriptException e) {
			throw new ExpressionEvaluationException(e.getMessage() + " " + contextDescription, e);
		}
		
		if (outputDefinition == null) {
			// No outputDefinition means "void" return type, we can return right now
			return null;
		}
		
		QName xsdReturnType = outputDefinition.getTypeName();
		
		Class<T> javaReturnType = XsdTypeMapper.toJavaType(xsdReturnType);
		if (javaReturnType == null) {
			javaReturnType = prismContext.getSchemaRegistry().getCompileTimeClass(xsdReturnType);
		}
        
		List<V> pvals = new ArrayList<V>();
		
		if (evalRawResult instanceof Collection) {
			for(Object evalRawResultElement : (Collection)evalRawResult) {
				T evalResult = convertScalarResult(javaReturnType, evalRawResultElement, contextDescription);
				V pval = null;
				if (allowEmptyValues || !isEmpty(evalResult)) {
					if (outputDefinition instanceof PrismReferenceDefinition){
						pval = (V) ((ObjectReferenceType)evalResult).asReferenceValue();
					} else {
						pval = (V) new PrismPropertyValue<T>(evalResult);
					}
					pvals.add(pval);
				}
			}
		} else if (evalRawResult instanceof PrismProperty<?>) {
			pvals.addAll((Collection<? extends V>) PrismPropertyValue.cloneCollection(((PrismProperty<T>)evalRawResult).getValues()));
		} else {
			T evalResult = convertScalarResult(javaReturnType, evalRawResult, contextDescription);
			V pval = null;
			if (allowEmptyValues || !isEmpty(evalResult)) {
				if (outputDefinition instanceof PrismReferenceDefinition){
					pval = (V) ((ObjectReferenceType)evalResult).asReferenceValue();
				} else {
					pval = (V) new PrismPropertyValue<T>(evalResult);
				}
				pvals.add(pval);
			}
		}
		
//		ScriptExpressionReturnTypeType definedReturnType = expressionType.getReturnType();
//		if (definedReturnType == ScriptExpressionReturnTypeType.LIST) {
//			
//		}
				
		return pvals;
	}
	
	public <T> Object evaluateReportScript(String codeString, ExpressionVariables variables, ObjectResolver objectResolver, Collection<FunctionLibrary> functions,
			String contextDescription, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, ExpressionSyntaxException {
		
		Bindings bindings = convertToBindings(variables, objectResolver, functions, contextDescription, result);
		
//		String codeString = code;
		if (codeString == null) {
			throw new ExpressionEvaluationException("No script code in " + contextDescription);
		}

		boolean allowEmptyValues = true;
//		if (expressionType.isAllowEmptyValues() != null) {
//			allowEmptyValues = expressionType.isAllowEmptyValues();
//		}
		
		CompiledScript compiledScript = createCompiledScript(codeString, contextDescription);
		
		Object evalRawResult;
		try {
			InternalMonitor.recordScriptExecution();
			evalRawResult = compiledScript.eval(bindings);
		} catch (ScriptException e) {
			throw new ExpressionEvaluationException(e.getMessage() + " " + contextDescription, e);
		}
		
		
				
		return evalRawResult;
	}
	
	private CompiledScript createCompiledScript(String codeString, String contextDescription) throws ExpressionEvaluationException {
		CompiledScript compiledScript = scriptCache.get(codeString);
		if (compiledScript != null) {
			return compiledScript;
		}
		try {
			InternalMonitor.recordScriptCompile();
			compiledScript = ((Compilable)scriptEngine).compile(codeString);
		} catch (ScriptException e) {
			throw new ExpressionEvaluationException(e.getMessage() + " " + contextDescription, e);
		}
		scriptCache.put(codeString, compiledScript);
		return compiledScript;
	}

	private <T> T convertScalarResult(Class<T> expectedType, Object rawValue, String contextDescription) throws ExpressionEvaluationException {
		try {
			T convertedValue = ExpressionUtil.convertValue(expectedType, rawValue, protector, prismContext);
			return convertedValue;
		} catch (IllegalArgumentException e) {
			throw new ExpressionEvaluationException(e.getMessage()+" in "+contextDescription, e);
		}
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
	
	private Bindings convertToBindings(ExpressionVariables variables, ObjectResolver objectResolver, 
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
		if (originalValue instanceof PrismProperty<?>) {
			PrismProperty<?> prop = (PrismProperty<?>)originalValue;
			PrismPropertyDefinition<?> def = prop.getDefinition();
			if (def != null) {
				if (def.isSingleValue()) {
					return prop.getRealValue();
				} else {
					return prop.getRealValues();
				}
			} else {
				return prop.getValues();
			}
		}
		return originalValue;
	}

	private Object resolveReference(ObjectReferenceType ref, ObjectResolver objectResolver, String name, String contextDescription, 
			OperationResult result) throws ExpressionSyntaxException, ObjectNotFoundException {
		if (ref.getOid() == null) {
    		throw new ExpressionSyntaxException("Null OID in reference in variable "+name+" in "+contextDescription);
    	} else {
	    	try {
	    		
				return objectResolver.resolve(ref, ObjectType.class, null, contextDescription, result);
				
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
