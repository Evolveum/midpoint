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
package com.evolveum.midpoint.repo.common.expression.evaluator;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

/**
 * @author Radovan Semancik
 *
 */
public class LiteralExpressionEvaluator<V extends PrismValue,D extends ItemDefinition> implements ExpressionEvaluator<V,D> {

	private final QName elementName;
	private final PrismValueDeltaSetTriple<V> outputTriple;

	/**
	 * @param deltaSetTriple
	 */
	public LiteralExpressionEvaluator(QName elementName, PrismValueDeltaSetTriple<V> outputTriple) {
		this.elementName = elementName;
		this.outputTriple = outputTriple;
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#evaluate(java.util.Collection, java.util.Map, boolean, java.lang.String, com.evolveum.midpoint.schema.result.OperationResult)
	 */
	@Override
	public PrismValueDeltaSetTriple<V> evaluate(ExpressionEvaluationContext context) 
			throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, SecurityViolationException {
		ExpressionUtil.checkEvaluatorProfileSimple(this, context);
		
		if (outputTriple == null) {
			return null;
		}
		return outputTriple.clone();
	}

	@Override
	public QName getElementName() {
		return elementName;
	}
	
	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
	 */
	@Override
	public String shortDebugDump() {
		return "literal: "+outputTriple;
	}


}
