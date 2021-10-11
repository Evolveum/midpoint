/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.prism.PrismValue;

@FunctionalInterface
public interface VariableProducer<V extends PrismValue> {

    public void produce(V value, ExpressionVariables variables);

}
