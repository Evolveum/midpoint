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

import java.lang.reflect.Method;
import java.util.Collections;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import groovy.transform.CompileStatic;

/**
 * PROTOTYPE
 * 
 * @author Radovan Semancik
 * Inspired by work of Cédric Champeau (http://melix.github.io/blog/2015/03/sandboxing.html)
 */
@Experimental
public class SandboxedGroovyScriptEvaluator extends GroovyScriptEvaluator {

	public static final String BUILTIN_EXPRESSION_PROFILE_NAME = "_groovyBuiltIn";
	
	/**
	 * Expression profile for built-in groovy functions that always needs to be allowed
	 * or denied.
	 */
	private static final ExpressionProfile BUILTIN_EXPRESSION_PROFILE = new ExpressionProfile(BUILTIN_EXPRESSION_PROFILE_NAME);
	
	public SandboxedGroovyScriptEvaluator(PrismContext prismContext, Protector protector, LocalizationService localizationService) {
		super(prismContext, protector, localizationService);
	}

	static final ThreadLocal<CompileOptions> COMPILE_OPTIONS = new ThreadLocal<>();
	private static final Trace LOGGER = TraceManager.getTrace(SandboxedGroovyScriptEvaluator.class);
	
	@Override
	protected void configureCompiler(CompilerConfiguration compilerConfiguration) {
		
		SecureASTCustomizer sAstCustomizer = new SecureASTCustomizer();
		compilerConfiguration.addCompilationCustomizers(sAstCustomizer);	
		
		ASTTransformationCustomizer astTransCustomizer = new ASTTransformationCustomizer(
                Collections.singletonMap("extensions", Collections.singletonList(SandboxTypeCheckingExtension.class.getName())),			
                CompileStatic.class);
		compilerConfiguration.addCompilationCustomizers(astTransCustomizer);
	}
	
	@Override
	protected void beforeCompileScript(String codeString, ScriptExpressionEvaluationContext context) {
	}
	
	@Override
	protected void afterCompileScript(Class compiledScript, String codeString, ScriptExpressionEvaluationContext context) {
	}
	
	@Override
	protected void beforeEvaluation(Class compiledScriptClass, ScriptExpressionEvaluationContext context) {
	}
	
	@Override
	protected void afterEvaluation(Object resultObject, Class compiledScriptClass, ScriptExpressionEvaluationContext context) {
	}
	
	static AccessDecision decideGroovyBuiltin(String className, String methodName) {
		return BUILTIN_EXPRESSION_PROFILE.decideClassAccess(className, methodName);
	}

//	private void setCompileOptions(ExpressionVariables variables, Collection<FunctionLibrary> functions, String contextDescription) {
//		CompileOptions options = new CompileOptions();
//		options.setVariables(variables);
//		options.setFunctions(functions);
//		options.setContextDescription(contextDescription);
//		COMPILE_OPTIONS.set(options);
//	}
//	
//	private void resetCompileOptions() {
//		COMPILE_OPTIONS.remove();
//	}
	
	static {
		// Allow script initialization
		BUILTIN_EXPRESSION_PROFILE.addClassAccessRule("groovy.lang.Script", "<init>", AccessDecision.ALLOW);
		BUILTIN_EXPRESSION_PROFILE.addClassAccessRule("org.codehaus.groovy.runtime.InvokerHelper", "runScript", AccessDecision.ALLOW);
		
		// Deny access to reflection. Reflection can circumvent the sandbox protection.
		BUILTIN_EXPRESSION_PROFILE.addClassAccessRule(Class.class, null, AccessDecision.DENY);
		BUILTIN_EXPRESSION_PROFILE.addClassAccessRule(Method.class, null, AccessDecision.DENY);
	}
	
}
