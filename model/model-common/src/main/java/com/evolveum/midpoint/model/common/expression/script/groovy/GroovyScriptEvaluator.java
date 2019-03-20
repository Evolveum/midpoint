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
package com.evolveum.midpoint.model.common.expression.script.groovy;

import java.util.Collection;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;
import org.codehaus.groovy.runtime.InvokerHelper;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.script.AbstractCachingScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.lang.Script;

/**
 * Expression evaluator that is using Groovy scripting engine.
 *
 * @author Radovan Semancik
 */
public class GroovyScriptEvaluator extends AbstractCachingScriptEvaluator<Class> {

	public static final String LANGUAGE_NAME = "Groovy";
	public static final String LANGUAGE_URL = MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + LANGUAGE_NAME;
	
	private static final Trace LOGGER = TraceManager.getTrace(GroovyScriptEvaluator.class);
	
	private GroovyClassLoader groovyLoader;

	public GroovyScriptEvaluator(PrismContext prismContext, Protector protector,
			LocalizationService localizationService) {
		super(prismContext, protector, localizationService);
		
		CompilerConfiguration compilerConfiguration = new CompilerConfiguration(CompilerConfiguration.DEFAULT);
		configureCompiler(compilerConfiguration);
		SecureASTCustomizer sAstCustomizer = new SecureASTCustomizer();
		compilerConfiguration.addCompilationCustomizers(sAstCustomizer);
		groovyLoader = new GroovyClassLoader(GroovyScriptEvaluator.class.getClassLoader(), compilerConfiguration);
	}

	protected void configureCompiler(CompilerConfiguration compilerConfiguration) {
		
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#getLanguageName()
	 */
	@Override
	public String getLanguageName() {
		return LANGUAGE_NAME;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#getLanguageUrl()
	 */
	@Override
	public String getLanguageUrl() {
		return LANGUAGE_URL;
	}


	@Override
	protected Class compileScript(String codeString, ScriptExpressionEvaluationContext context)
			throws ExpressionEvaluationException {
		return groovyLoader.parseClass(codeString, context.getContextDescription());
	}


	@Override
	protected Object evaluateScript(Class compiledScriptClass, ScriptExpressionEvaluationContext context) throws Exception {
		
		if (!Script.class.isAssignableFrom(compiledScriptClass)) {
            throw new ExpressionEvaluationException("Expected groovy script class, but got "+compiledScriptClass);
		}
		
		Binding binding = new Binding(prepareScriptVariablesValueMap(context));
		
		Script scriptResultObject = InvokerHelper.createScript(compiledScriptClass, binding);
		
		Object resultObject = scriptResultObject.run();
		
		return resultObject;
	}
	

}
