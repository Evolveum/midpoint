/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import com.evolveum.midpoint.prism.PrismValue;

import java.util.List;

/**
 * @author Pavol Mederly
 */
public class QueryResult<V extends PrismValue> {

    private List<V> resultingList;

    public QueryResult(List<V> resultingList) {
        this.resultingList = resultingList;
    }

    public List<V> getResultingList() {
        return resultingList;
    }
}
