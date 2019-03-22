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

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.script.AbstractCachingScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
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
 * This evaluator does not really support expression profiles. It has just one
 * global almighty compiler (ScriptEngine).
 *
 * @author Radovan Semancik
 *
 */
public class Jsr223ScriptEvaluator extends AbstractCachingScriptEvaluator<ScriptEngine,CompiledScript> {
	
	private static final Trace LOGGER = TraceManager.getTrace(Jsr223ScriptEvaluator.class);

	private final ScriptEngine scriptEngine;

	public Jsr223ScriptEvaluator(String engineName, PrismContext prismContext, Protector protector,
			LocalizationService localizationService) {
		super(prismContext, protector, localizationService);
		
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		scriptEngine = scriptEngineManager.getEngineByName(engineName);
		if (scriptEngine == null) {
			throw new SystemException("The JSR-223 scripting engine for '"+engineName+"' was not found");
		}
	}
	
	@Override
	protected CompiledScript compileScript(String codeString, ScriptExpressionEvaluationContext context) throws Exception {
		return ((Compilable)scriptEngine).compile(codeString);
	}
	
	@Override
	protected Object evaluateScript(CompiledScript compiledScript, ScriptExpressionEvaluationContext context) throws Exception {
		
		Bindings bindings = convertToBindings(context);
		return compiledScript.eval(bindings);
	}
	
	private Bindings convertToBindings(ScriptExpressionEvaluationContext context) 
					   throws ExpressionSyntaxException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Bindings bindings = scriptEngine.createBindings();
		bindings.putAll(prepareScriptVariablesValueMap(context));
		return bindings;
	}
	

	public <T> Object evaluateReportScript(String codeString, ScriptExpressionEvaluationContext context) throws ExpressionEvaluationException,
			ObjectNotFoundException, ExpressionSyntaxException, CommunicationException, ConfigurationException, SecurityViolationException {

		Bindings bindings = convertToBindings(context);

//		String codeString = code;
		if (codeString == null) {
			throw new ExpressionEvaluationException("No script code in " + context.getContextDescription());
		}

		boolean allowEmptyValues = true;
//		if (expressionType.isAllowEmptyValues() != null) {
//			allowEmptyValues = expressionType.isAllowEmptyValues();
//		}

		CompiledScript compiledScript = getCompiledScript(codeString, context);

		Object evalRawResult;
		try {
			InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
			evalRawResult = compiledScript.eval(bindings);
		} catch (Throwable e) {
			throw new ExpressionEvaluationException(e.getMessage() + " in " + context.getContextDescription(), e);
		}



		return evalRawResult;
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
		return MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + getLanguageName();
	}

}
