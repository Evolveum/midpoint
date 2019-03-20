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
package com.evolveum.midpoint.model.common.expression.script.velocity;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.model.common.expression.script.AbstractScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
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
public class VelocityScriptEvaluator extends AbstractScriptEvaluator {

	private static final String LANGUAGE_URL_BASE = MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX + "/expression/language#";

	public VelocityScriptEvaluator(PrismContext prismContext, Protector protector, LocalizationService localizationService) {
		super(prismContext, protector, localizationService);
		Properties properties = new Properties();
//		properties.put("runtime.references.strict", "true");
		Velocity.init(properties);
	}

	@Override
	public <T, V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluationContext context) throws ExpressionEvaluationException,
			ObjectNotFoundException, ExpressionSyntaxException, CommunicationException, ConfigurationException, SecurityViolationException {

		VelocityContext velocityCtx = createVelocityContext(context);

		String codeString = context.getExpressionType().getCode();
		if (codeString == null) {
			throw new ExpressionEvaluationException("No script code in " + context.getContextDescription());
		}

		boolean allowEmptyValues = false;
		if (context.getExpressionType().isAllowEmptyValues() != null) {
			allowEmptyValues = context.getExpressionType().isAllowEmptyValues();
		}

		StringWriter resultWriter = new StringWriter();
		try {
			InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
			Velocity.evaluate(velocityCtx, resultWriter, "", codeString);
		} catch (RuntimeException e) {
			throw new ExpressionEvaluationException(e.getMessage() + " in " + context.getContextDescription(), e);
		}

		if (context.getOutputDefinition() == null) {
			// No outputDefinition means "void" return type, we can return right now
			return null;
		}

		QName xsdReturnType = context.getOutputDefinition().getTypeName();

		Class<T> javaReturnType = XsdTypeMapper.toJavaType(xsdReturnType);
		if (javaReturnType == null) {
			javaReturnType = getPrismContext().getSchemaRegistry().getCompileTimeClass(xsdReturnType);
		}

		if (javaReturnType == null) {
			// TODO quick and dirty hack - because this could be because of enums defined in schema extension (MID-2399)
			// ...and enums (xsd:simpleType) are not parsed into ComplexTypeDefinitions
			javaReturnType = (Class) String.class;
		}

		T evalResult;
		try {
			evalResult = ExpressionUtil.convertValue(javaReturnType, context.getAdditionalConvertor(), resultWriter.toString(), getProtector(), getPrismContext());
		} catch (IllegalArgumentException e) {
			throw new ExpressionEvaluationException(e.getMessage()+" in "+context.getContextDescription(), e);
		}

		List<V> pvals = new ArrayList<>();
		if (allowEmptyValues || !ExpressionUtil.isEmpty(evalResult)) {
			pvals.add((V) ExpressionUtil.convertToPrismValue(evalResult, context.getOutputDefinition(), context.getContextDescription(), getPrismContext()));
		}
		return pvals;
	}

	private VelocityContext createVelocityContext(ScriptExpressionEvaluationContext context) throws ExpressionSyntaxException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		VelocityContext velocityCtx = new VelocityContext();
		Map<String,Object> scriptVariables = prepareScriptVariablesValueMap(context);
		for (Map.Entry<String,Object> scriptVariable : scriptVariables.entrySet()) {
			velocityCtx.put(scriptVariable.getKey(), scriptVariable.getValue());
		}
		return velocityCtx;
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
