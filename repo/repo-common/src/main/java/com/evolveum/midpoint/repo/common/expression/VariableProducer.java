package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.prism.PrismValue;

@FunctionalInterface
public interface VariableProducer<V extends PrismValue> {

	public void produce(V value, ExpressionVariables variables);
	
}
