/**
 * Copyright (c) 2019 Evolveum
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

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;

import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 * Inspired by work of Cédric Champeau (http://melix.github.io/blog/2015/03/sandboxing.html)
 *
 */
public class SandboxTypeCheckingExtension extends AbstractTypeCheckingExtension {
	
	private static final Trace LOGGER = TraceManager.getTrace(SandboxTypeCheckingExtension.class);

	public SandboxTypeCheckingExtension(StaticTypeCheckingVisitor typeCheckingVisitor) {
		super(typeCheckingVisitor);
	}
	
	private CompileOptions getCompileOptions() {
		return SandboxedGroovyScriptEvaluator.COMPILE_OPTIONS.get();
	}

	@Override
    public void onMethodSelection(final Expression expression, final MethodNode target) {
		ClassNode targetDeclaringClass = target.getDeclaringClass();
		LOGGER.info("GROOVY:onMethodSelection: targetDeclaringClass={}", targetDeclaringClass);
	}
	
	@Override
	public boolean handleUnresolvedVariableExpression(VariableExpression vexp) {
		String variableName = vexp.getName();
		LOGGER.info("GROOVY:handleUnresolvedVariableExpression: variableName={}", variableName);
		if (!isDynamic(vexp)) {
			return false;
		}
		ExpressionVariables variables = getCompileOptions().getVariables();
		if (!variables.containsKey(variableName)) {
			return false;
		}
//		Object variableValue = variables.getVariable(variableName);
//		ClassHelper.make(c)
//		makeDynamic(vexp, returnType);
		return false;
    }
}
