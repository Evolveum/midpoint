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
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.UUID;

import com.evolveum.midpoint.model.common.stringpolicy.AbstractValuePolicyOriginResolver;
import com.evolveum.midpoint.model.common.stringpolicy.ShadowValuePolicyOriginResolver;
import com.evolveum.midpoint.model.common.stringpolicy.UserValuePolicyOriginResolver;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.ValuePolicyResolver;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * @author semancik
 *
 */
public class GenerateExpressionEvaluator<V extends PrismValue, D extends ItemDefinition>
		implements ExpressionEvaluator<V, D> {

	public static final int DEFAULT_LENGTH = 8;

	private GenerateExpressionEvaluatorType generateEvaluatorType;
	private D outputDefinition;
	private Protector protector;
	private PrismContext prismContext;
	private ObjectResolver objectResolver;
	private ValuePolicyProcessor valuePolicyGenerator;
	private ValuePolicyType elementValuePolicy;

	GenerateExpressionEvaluator(GenerateExpressionEvaluatorType generateEvaluatorType, D outputDefinition,
			Protector protector, ObjectResolver objectResolver, ValuePolicyProcessor valuePolicyGenerator, PrismContext prismContext) {
		this.generateEvaluatorType = generateEvaluatorType;
		this.outputDefinition = outputDefinition;
		this.protector = protector;
		this.objectResolver = objectResolver;
		this.valuePolicyGenerator = valuePolicyGenerator;
		this.prismContext = prismContext;
	}

	private boolean isNotEmptyMinLength(ValuePolicyType policy) {
		StringPolicyType stringPolicy = policy.getStringPolicy();
		if (stringPolicy == null) {
			return false;
		}
		Integer minLength = stringPolicy.getLimitations().getMinLength();
		if (minLength != null) {
			if (minLength.intValue() == 0) {
				return false;
			}
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluate(java
	 * .util.Collection, java.util.Map, boolean, java.lang.String,
	 * com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

		ValuePolicyType valuePolicyType = null;


		ObjectReferenceType generateEvaluatorValuePolicyRef = generateEvaluatorType.getValuePolicyRef();
		if (generateEvaluatorValuePolicyRef != null) {
			if (generateEvaluatorType.getValuePolicyRef() != null) {
	        	ValuePolicyType resolvedValuePolicyType = objectResolver.resolve(generateEvaluatorValuePolicyRef, ValuePolicyType.class,
	        			null, "resolving value policy reference in generateExpressionEvaluator", context.getTask(), context.getResult());
	        	valuePolicyType = resolvedValuePolicyType;
	        }

		}

		// if (elementStringPolicy == null) {
		// if the policy was changed, the most fresh copy is needed, therefore
		// it must be resolved all time the value is generated..if it was not
		// resolved each time, the cached policy would be used and so bad values
		// would be generated
		if (valuePolicyType == null) {
			ValuePolicyResolver valuePolicyResolver = context.getValuePolicyResolver();
			if (valuePolicyResolver != null) {
				valuePolicyType = valuePolicyResolver.resolve();
			}
		}

		elementValuePolicy = valuePolicyType;
		// } else {
		// stringPolicyType = elementStringPolicy;
		// }

		//
		String stringValue = null;
		GenerateExpressionEvaluatorModeType mode = generateEvaluatorType.getMode();
		Item<V, D> output = outputDefinition.instantiate();
		if (mode == null || mode == GenerateExpressionEvaluatorModeType.POLICY) {

			AbstractValuePolicyOriginResolver<? extends ObjectType> originResolver = getOriginResolver(context);

			// TODO: generate value based on stringPolicyType (if not null)
			if (valuePolicyType != null) {
				if (isNotEmptyMinLength(valuePolicyType)) {
					stringValue = valuePolicyGenerator.generate(output.getPath(), valuePolicyType, DEFAULT_LENGTH, true, originResolver,
							context.getContextDescription(), context.getTask(), context.getResult());
				} else {
					stringValue = valuePolicyGenerator.generate(output.getPath(), valuePolicyType, DEFAULT_LENGTH, false, originResolver,
							context.getContextDescription(), context.getTask(), context.getResult());
				}
				context.getResult().computeStatus();
				if (context.getResult().isError()) {
					throw new ExpressionEvaluationException("Failed to generate value according to policy: "
							+ valuePolicyType.getDescription() + ". " + context.getResult().getMessage());
				}
			}

			if (stringValue == null) {
				int length = DEFAULT_LENGTH;
				RandomString randomString = new RandomString(length);
				stringValue = randomString.nextString();
			}

		} else if (mode == GenerateExpressionEvaluatorModeType.UUID) {
			UUID randomUUID = UUID.randomUUID();
			stringValue = randomUUID.toString();

		} else {
			throw new ExpressionEvaluationException("Unknown mode for generate expression: " + mode);
		}

		Object value = ExpressionUtil.convertToOutputValue(stringValue, outputDefinition, protector);


		if (output instanceof PrismProperty) {
			PrismPropertyValue<Object> pValue = new PrismPropertyValue<Object>(value);
			((PrismProperty<Object>) output).add(pValue);
		} else {
			throw new UnsupportedOperationException(
					"Can only generate values of property, not " + output.getClass());
		}

		return ItemDelta.toDeltaSetTriple(output, null);
	}

	// determine object from the variables
	@SuppressWarnings("unchecked")
	private <O extends ObjectType> AbstractValuePolicyOriginResolver<O> getOriginResolver(ExpressionEvaluationContext params) throws SchemaException {
		ExpressionVariables variables = params.getVariables();
		if (variables == null) {
			return null;
		}
		PrismObject<O> object = variables.getObjectNew(ExpressionConstants.VAR_PROJECTION);
		if (object != null) {
			return (AbstractValuePolicyOriginResolver<O>) new ShadowValuePolicyOriginResolver((PrismObject<ShadowType>) object, objectResolver);
		}
		object = variables.getObjectNew(ExpressionConstants.VAR_FOCUS);
		return (AbstractValuePolicyOriginResolver<O>) new UserValuePolicyOriginResolver((PrismObject<UserType>) object, objectResolver);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#
	 * shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		if (elementValuePolicy != null) {
			return "generate: " + elementValuePolicy;
		}
		return "generate";
	}

}
