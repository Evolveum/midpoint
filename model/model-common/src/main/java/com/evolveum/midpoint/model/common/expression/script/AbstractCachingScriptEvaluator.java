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
package com.evolveum.midpoint.model.common.expression.script;

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

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.script.ScriptEvaluator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;

/**
 * Expression evaluator that is using javax.script (JSR-223) engine.
 *
 * @author Radovan Semancik
 * @param <C> compiled code
 *
 */
public abstract class AbstractCachingScriptEvaluator<C> extends AbstractScriptEvaluator {

	private static final Trace LOGGER = TraceManager.getTrace(AbstractCachingScriptEvaluator.class);

	private final Map<String, C> scriptCache;

	public AbstractCachingScriptEvaluator(PrismContext prismContext, Protector protector,
			LocalizationService localizationService) {
		super(prismContext, protector, localizationService);
		this.scriptCache = new ConcurrentHashMap<>();
	}
	
	public Map<String, C> getScriptCache() {
		return scriptCache;
	}

	@Override
	public <T, V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluatorType expressionType,
			ExpressionVariables variables, ItemDefinition outputDefinition,
			Function<Object, Object> additionalConvertor,
			ScriptExpressionReturnTypeType suggestedReturnType,
			ObjectResolver objectResolver, Collection<FunctionLibrary> functions,
			String contextDescription, Task task, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, ExpressionSyntaxException, CommunicationException, ConfigurationException, SecurityViolationException {

		String codeString = expressionType.getCode();
		if (codeString == null) {
			throw new ExpressionEvaluationException("No script code in " + contextDescription);
		}

		boolean allowEmptyValues = false;
		if (expressionType.isAllowEmptyValues() != null) {
			allowEmptyValues = expressionType.isAllowEmptyValues();
		}

		C compiledScript = getCompiledScript(codeString, contextDescription);

		Object evalRawResult;
		try {
			InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
			evalRawResult = evaluateScript(compiledScript, variables, objectResolver, functions, contextDescription, task, result);
		} catch (Throwable e) {
			throw getLocalizationService().translate(
					new ExpressionEvaluationException(e.getMessage() + " in " + contextDescription,
							e, ExceptionUtil.getUserFriendlyMessage(e)));
		}

		if (outputDefinition == null) {
			// No outputDefinition means "void" return type, we can return right now
			return null;
		}

		QName xsdReturnType = outputDefinition.getTypeName();

		Class<T> javaReturnType = XsdTypeMapper.toJavaType(xsdReturnType);
		if (javaReturnType == null) {
			javaReturnType = getPrismContext().getSchemaRegistry().getCompileTimeClass(xsdReturnType);
		}
		
		if (javaReturnType == null && (outputDefinition instanceof PrismContainerDefinition<?>)) {
			// This is the case when we need a container, but we do not have compile-time class for that
			// E.g. this may be container in object extension (MID-5080)
			javaReturnType = (Class<T>) PrismContainerValue.class;
		}

		if (javaReturnType == null) {
			// TODO quick and dirty hack - because this could be because of enums defined in schema extension (MID-2399)
			// ...and enums (xsd:simpleType) are not parsed into ComplexTypeDefinitions
			javaReturnType = (Class<T>) String.class;
		}
		LOGGER.trace("expected return type: XSD={}, Java={}", xsdReturnType, javaReturnType);

		List<V> pvals = new ArrayList<>();

		// TODO: what about PrismContainer and
		// PrismReference? Shouldn't they be processed in the same way as
		// PrismProperty?
		if (evalRawResult instanceof Collection) {
			for (Object evalRawResultElement : (Collection)evalRawResult) {
				T evalResult = convertScalarResult(javaReturnType, additionalConvertor, evalRawResultElement, contextDescription);
				if (allowEmptyValues || !ExpressionUtil.isEmpty(evalResult)) {
					pvals.add((V) ExpressionUtil.convertToPrismValue(evalResult, outputDefinition, contextDescription, getPrismContext()));
				}
			}
		} else if (evalRawResult instanceof PrismProperty<?>) {
			pvals.addAll((Collection<? extends V>) PrismValueCollectionsUtil.cloneCollection(((PrismProperty<T>)evalRawResult).getValues()));
		} else {
			T evalResult = convertScalarResult(javaReturnType, additionalConvertor, evalRawResult, contextDescription);
			if (allowEmptyValues || !ExpressionUtil.isEmpty(evalResult)) {
				pvals.add((V) ExpressionUtil.convertToPrismValue(evalResult, outputDefinition, contextDescription, getPrismContext()));
			}
		}

		return pvals;
	}

	protected C getCompiledScript(String codeString, String contextDescription) throws ExpressionEvaluationException {
		C compiledScript = scriptCache.get(codeString);
		if (compiledScript != null) {
			return compiledScript;
		}
		InternalMonitor.recordCount(InternalCounters.SCRIPT_COMPILE_COUNT);
		try {
			compiledScript = compileScript(codeString, contextDescription);
		} catch (Exception e) {
			throw new ExpressionEvaluationException(e.getMessage() + " while compiling " + contextDescription, e);
		}
		scriptCache.put(codeString, compiledScript);
		return compiledScript;
	}
	
	protected abstract C compileScript(String codeString, String contextDescription) throws Exception;
	
	protected abstract Object evaluateScript(C compiledScript, ExpressionVariables variables,
			ObjectResolver objectResolver, Collection<FunctionLibrary> functions, String contextDescription,
			Task task, OperationResult result)
				throws Exception;

	protected Map<String,Object> getVariableValuesMap(ExpressionVariables variables, ObjectResolver objectResolver,
			   Collection<FunctionLibrary> functions, String contextDescription, Task task, OperationResult result) throws ExpressionSyntaxException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		return prepareScriptVariablesValueMap(variables, objectResolver, functions, contextDescription, task, result);
	}
	
	private <T> T convertScalarResult(Class<T> expectedType, Function<Object, Object> additionalConvertor, Object rawValue, String contextDescription) throws ExpressionEvaluationException {
		try {
			T convertedValue = ExpressionUtil.convertValue(expectedType, additionalConvertor, rawValue, getProtector(), getPrismContext());
			return convertedValue;
		} catch (IllegalArgumentException e) {
			throw new ExpressionEvaluationException(e.getMessage() + " in " + contextDescription, e);
		}
	}

}
