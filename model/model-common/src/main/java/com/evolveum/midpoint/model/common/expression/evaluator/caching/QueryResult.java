/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.model.common.expression.evaluator.AbstractSearchExpressionEvaluator.ObjectFound;
import com.evolveum.midpoint.prism.PrismValue;

class QueryResult<V extends PrismValue> {

    private final Collection<? extends ObjectFound<?, V>> resultingList;

    QueryResult(Collection<? extends ObjectFound<?, V>> resultingList) {
        this.resultingList = resultingList;
    }

    List<V> getResultingList() {
        return ObjectFound.unwrap(resultingList);
    }
}
