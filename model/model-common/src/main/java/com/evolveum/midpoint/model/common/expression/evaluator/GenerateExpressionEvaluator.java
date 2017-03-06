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

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.ExpressionUtil;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.StringPolicyResolver;
import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyGenerator;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.util.RandomString;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.StringPolicyType;
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
	private ValuePolicyGenerator valuePolicyGenerator;
	private StringPolicyType elementStringPolicy;

	GenerateExpressionEvaluator(GenerateExpressionEvaluatorType generateEvaluatorType, D outputDefinition,
			Protector protector, ObjectResolver objectResolver, ValuePolicyGenerator valuePolicyGenerator, PrismContext prismContext) {
		this.generateEvaluatorType = generateEvaluatorType;
		this.outputDefinition = outputDefinition;
		this.protector = protector;
		this.objectResolver = objectResolver;
		this.valuePolicyGenerator = valuePolicyGenerator;
		this.prismContext = prismContext;
	}

	private boolean isNotEmptyMinLength(StringPolicyType policy) {
		Integer minLength = policy.getLimitations().getMinLength();
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
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext params)
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		StringPolicyType stringPolicyType = null;

		
		ObjectReferenceType generateEvaluatorValuePolicyRef = generateEvaluatorType.getValuePolicyRef();
		if (generateEvaluatorValuePolicyRef != null) {
			if (generateEvaluatorType.getValuePolicyRef() != null) {
	        	ValuePolicyType valuePolicyType = objectResolver.resolve(generateEvaluatorValuePolicyRef, ValuePolicyType.class,
	        			null, "resolving value policy reference in generateExpressionEvaluator", params.getTask(), params.getResult());
	        	stringPolicyType = valuePolicyType.getStringPolicy();
	        }
			
		}

		// if (elementStringPolicy == null) {
		// if the policy was changed, the most fresh copy is needed, therefore
		// it must be resolved all time the value is generated..if it was not
		// resolved each time, the cached policy would be used and so bad values
		// would be generated
		if (stringPolicyType == null) {
			StringPolicyResolver stringPolicyResolver = params.getStringPolicyResolver();
			if (stringPolicyResolver != null) {
				stringPolicyType = stringPolicyResolver.resolve();
			}
		}
		
		elementStringPolicy = stringPolicyType;
		// } else {
		// stringPolicyType = elementStringPolicy;
		// }

		//
		String stringValue = null;
		GenerateExpressionEvaluatorModeType mode = generateEvaluatorType.getMode();
		if (mode == null || mode == GenerateExpressionEvaluatorModeType.POLICY) {

			PrismObject<? extends ObjectType> object = getObject(params);

			// TODO: generate value based on stringPolicyType (if not null)
			if (stringPolicyType != null) {
				if (isNotEmptyMinLength(stringPolicyType)) {
					stringValue = valuePolicyGenerator.generate(stringPolicyType, DEFAULT_LENGTH, true, object,
							params.getContextDescription(), params.getTask(), params.getResult());
				} else {
					stringValue = valuePolicyGenerator.generate(stringPolicyType, DEFAULT_LENGTH, false, object,
							params.getContextDescription(), params.getTask(), params.getResult());
				}
				params.getResult().computeStatus();
				if (params.getResult().isError()) {
					throw new ExpressionEvaluationException("Failed to generate value according to policy: "
							+ stringPolicyType.getDescription() + ". " + params.getResult().getMessage());
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

		Item<V, D> output = outputDefinition.instantiate();
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
	private <O extends ObjectType> PrismObject<O> getObject(ExpressionEvaluationContext params) throws SchemaException {
		ExpressionVariables variables = params.getVariables();
		if (variables == null) {
			return null;
		}
		PrismObject<O> object = variables.get(ExpressionConstants.VAR_PROJECTION, PrismObject.class);
		if (object != null) {
			return object;
		}
		object = variables.get(ExpressionConstants.VAR_FOCUS, PrismObject.class);
		return object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#
	 * shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		if (elementStringPolicy != null) {
			return "generate: " + toHumanReadableString(elementStringPolicy);
		}
		return "generate";
	}

	private String toHumanReadableString(StringPolicyType stringPolicy) {
		if (stringPolicy.getDescription() != null) {
			return "StringPolicy: " + StringUtils.abbreviate(stringPolicy.getDescription(), 60);
		}
		return "StringPolicy";
	}

}
