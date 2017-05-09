/*
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.model.common.expression.script.velocity;

import com.evolveum.midpoint.model.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.script.ScriptEvaluator;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptExpressionReturnTypeType;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

import javax.xml.namespace.QName;
import java.io.StringWriter;
import java.util.*;
import java.util.function.Function;

/**
 * Expression evaluator that is using Apache Velocity engine.
 * 
 * @author mederly
 *
 */
public class VelocityScriptEvaluator implements ScriptEvaluator {

	private static final String LANGUAGE_URL_BASE = MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX + "/expression/language#";

	private PrismContext prismContext;
	private Protector protector;

	public VelocityScriptEvaluator(PrismContext prismContext, Protector protector) {
		this.prismContext = prismContext;
		this.protector = protector;
		Properties properties = new Properties();
//		properties.put("runtime.references.strict", "true");
		Velocity.init(properties);
	}
	
	@Override
	public <T, V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluatorType expressionType,
			ExpressionVariables variables, ItemDefinition outputDefinition,
			Function<Object, Object> additionalConvertor,
			ScriptExpressionReturnTypeType suggestedReturnType,
			ObjectResolver objectResolver, Collection<FunctionLibrary> functions,
			String contextDescription, Task task, OperationResult result) throws ExpressionEvaluationException,
			ObjectNotFoundException, ExpressionSyntaxException {
		
		VelocityContext context = createVelocityContext(variables, objectResolver, functions, contextDescription, task, result);
		
		String codeString = expressionType.getCode();
		if (codeString == null) {
			throw new ExpressionEvaluationException("No script code in " + contextDescription);
		}

		boolean allowEmptyValues = false;
		if (expressionType.isAllowEmptyValues() != null) {
			allowEmptyValues = expressionType.isAllowEmptyValues();
		}
		
		StringWriter resultWriter = new StringWriter();
		try {
			InternalMonitor.recordScriptExecution();
			Velocity.evaluate(context, resultWriter, "", codeString);
		} catch (RuntimeException e) {
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
        
		T evalResult;
		try {
			evalResult = ExpressionUtil.convertValue(javaReturnType, additionalConvertor, resultWriter.toString(), protector, prismContext);
		} catch (IllegalArgumentException e) {
			throw new ExpressionEvaluationException(e.getMessage()+" in "+contextDescription, e);
		}

		List<V> pvals = new ArrayList<V>();
		if (allowEmptyValues || !ExpressionUtil.isEmpty(evalResult)) {
			pvals.add((V) ExpressionUtil.convertToPrismValue(evalResult, outputDefinition, contextDescription, prismContext));
		}
		return pvals;
	}

	private VelocityContext createVelocityContext(ExpressionVariables variables, ObjectResolver objectResolver,
									   Collection<FunctionLibrary> functions,
									   String contextDescription, Task task, OperationResult result) throws ExpressionSyntaxException, ObjectNotFoundException {
		VelocityContext context = new VelocityContext();
		Map<String,Object> scriptVariables = ExpressionUtil.prepareScriptVariables(variables, objectResolver, functions, contextDescription,
				prismContext, task, result);
		for (Map.Entry<String,Object> scriptVariable : scriptVariables.entrySet()) {
			context.put(scriptVariable.getKey(), scriptVariable.getValue());
		}
		return context;
	}


	@Override
	public String getLanguageName() {
		return "velocity";
	}

	@Override
	public String getLanguageUrl() {
		return LANGUAGE_URL_BASE + getLanguageName();
	}

}
