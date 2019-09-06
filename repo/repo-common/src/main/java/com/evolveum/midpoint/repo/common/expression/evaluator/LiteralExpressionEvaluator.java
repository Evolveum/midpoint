/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
