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

import java.util.Collections;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.control.customizers.SecureASTCustomizer;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import groovy.lang.Binding;
import groovy.transform.CompileStatic;

/**
 * PROTOTYPE
 * 
 * @author Radovan Semancik
 */
@Experimental
public class SandboxedGroovyScriptEvaluator extends GroovyScriptEvaluator {

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
	protected void beforeEvaluation(Class compiledScriptClass, ExpressionVariables variables, Binding binding, String contextDescription, Task task, OperationResult result) {
		CompileOptions options = new CompileOptions();
		options.setVariables(variables);
		COMPILE_OPTIONS.set(options);
	}
	
	@Override
	protected void afterEvaluation(Object resultObject, Class compiledScriptClass, ExpressionVariables variables, Binding binding, String contextDescription, Task task, OperationResult result) {
		COMPILE_OPTIONS.remove();
	}

}
