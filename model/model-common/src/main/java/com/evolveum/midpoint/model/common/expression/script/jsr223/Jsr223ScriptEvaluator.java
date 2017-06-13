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
package com.evolveum.midpoint.model.common.expression.script.jsr223;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.script.ScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

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
		this.scriptCache = new ConcurrentHashMap<>();
	}
	
	@Override
	public <T, V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluatorType expressionType,
			ExpressionVariables variables, ItemDefinition outputDefinition,
			Function<Object, Object> additionalConvertor,
			ScriptExpressionReturnTypeType suggestedReturnType,
			ObjectResolver objectResolver, Collection<FunctionLibrary> functions,
			String contextDescription, Task task, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, ExpressionSyntaxException {
		
		Bindings bindings = convertToBindings(variables, objectResolver, functions, contextDescription, task, result);
		
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
			InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
			evalRawResult = compiledScript.eval(bindings);
		} catch (Throwable e) {
			throw new ExpressionEvaluationException(e.getMessage() + " in " + contextDescription, e);
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

		if (javaReturnType == null) {
			// TODO quick and dirty hack - because this could be because of enums defined in schema extension (MID-2399)
			// ...and enums (xsd:simpleType) are not parsed into ComplexTypeDefinitions
			javaReturnType = (Class) String.class;
		}
        
		List<V> pvals = new ArrayList<V>();
		
		// TODO: what about PrismContainer and
		// PrismReference? Shouldn't they be processed in the same way as
		// PrismProperty?
		if (evalRawResult instanceof Collection) {
			for (Object evalRawResultElement : (Collection)evalRawResult) {
				T evalResult = convertScalarResult(javaReturnType, additionalConvertor, evalRawResultElement, contextDescription);
				if (allowEmptyValues || !ExpressionUtil.isEmpty(evalResult)) {
					pvals.add((V) ExpressionUtil.convertToPrismValue(evalResult, outputDefinition, contextDescription, prismContext));
				}
			}
		} else if (evalRawResult instanceof PrismProperty<?>) {
			pvals.addAll((Collection<? extends V>) PrismPropertyValue.cloneCollection(((PrismProperty<T>)evalRawResult).getValues()));
		} else {
			T evalResult = convertScalarResult(javaReturnType, additionalConvertor, evalRawResult, contextDescription);
			if (allowEmptyValues || !ExpressionUtil.isEmpty(evalResult)) {
				pvals.add((V) ExpressionUtil.convertToPrismValue(evalResult, outputDefinition, contextDescription, prismContext));
			}
		}
		
		return pvals;
	}
	
	public <T> Object evaluateReportScript(String codeString, ExpressionVariables variables, ObjectResolver objectResolver, Collection<FunctionLibrary> functions,
			String contextDescription, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, ExpressionSyntaxException {
		
		Bindings bindings = convertToBindings(variables, objectResolver, functions, contextDescription, (Task) null, result);
		
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
			InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
			evalRawResult = compiledScript.eval(bindings);
		} catch (Throwable e) {
			throw new ExpressionEvaluationException(e.getMessage() + " in " + contextDescription, e);
		}
		
		
				
		return evalRawResult;
	}
	
	private CompiledScript createCompiledScript(String codeString, String contextDescription) throws ExpressionEvaluationException {
		CompiledScript compiledScript = scriptCache.get(codeString);
		if (compiledScript != null) {
			return compiledScript;
		}
		try {
			InternalMonitor.recordCount(InternalCounters.SCRIPT_COMPILE_COUNT);
			compiledScript = ((Compilable)scriptEngine).compile(codeString);
		} catch (ScriptException e) {
			throw new ExpressionEvaluationException(e.getMessage() + " in " + contextDescription, e);
		}
		scriptCache.put(codeString, compiledScript);
		return compiledScript;
	}

	private <T> T convertScalarResult(Class<T> expectedType, Function<Object, Object> additionalConvertor, Object rawValue, String contextDescription) throws ExpressionEvaluationException {
		try {
			T convertedValue = ExpressionUtil.convertValue(expectedType, additionalConvertor, rawValue, protector, prismContext);
			return convertedValue;
		} catch (IllegalArgumentException e) {
			throw new ExpressionEvaluationException(e.getMessage() + " in " + contextDescription, e);
		}
	}
	
	private Bindings convertToBindings(ExpressionVariables variables, ObjectResolver objectResolver,
									   Collection<FunctionLibrary> functions,
									   String contextDescription, Task task, OperationResult result) throws ExpressionSyntaxException, ObjectNotFoundException {
		Bindings bindings = scriptEngine.createBindings();
		bindings.putAll(ScriptExpressionUtil.prepareScriptVariables(variables, objectResolver, functions, contextDescription, prismContext, task, result));
		return bindings;
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
