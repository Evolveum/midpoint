/*
 * Copyright (c) 2017 Evolveum
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

import com.evolveum.midpoint.model.common.ConstantsManager;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstExpressionEvaluatorType;

/**
 * @author semancik
 *
 */
public class ConstExpressionEvaluator<V extends PrismValue, D extends ItemDefinition>
		implements ExpressionEvaluator<V, D> {

	private ConstExpressionEvaluatorType constEvaluatorType;
	private D outputDefinition;
	private Protector protector;
	private ConstantsManager constantsManager;
	private PrismContext prismContext;

	ConstExpressionEvaluator(ConstExpressionEvaluatorType evaluatorType, D outputDefinition,
			Protector protector, ConstantsManager constantsManager, PrismContext prismContext) {
		this.constEvaluatorType = evaluatorType;
		this.outputDefinition = outputDefinition;
		this.protector = protector;
		this.constantsManager = constantsManager;
		this.prismContext = prismContext;
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
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException {

		String constName = constEvaluatorType.getValue();
		String stringValue = constantsManager.getConstantValue(constName);

		Item<V, D> output = outputDefinition.instantiate();

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

	/*
	 * (non-Javadoc)
	 *
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#
	 * shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "const:"+constEvaluatorType.getValue();
	}


}
