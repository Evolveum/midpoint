/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.util.caching.AbstractCache;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache for search expression-based evaluators.
 *
 * It needs to be customized in the following ways:
 * - what's in the query key besides basic data - namely, what parts of ExpressionEvaluationContext should be part of the key?
 * - should we store anything in addition to the resulting list of values? E.g. shadow kind in case of associationTargetSearch that is used for invalidation?
 *
 * V - type of cached result items
 * RV - type of raw values that we are searching for
 * QK, QR - customized query keys / values
 *
 * After refactoring, this class contains almost nothing ;) Consider removing it altogether.
 *
 * @author Pavol Mederly
 */
public abstract class AbstractSearchExpressionEvaluatorCache<V extends PrismValue, RV extends PrismObject, QK extends QueryKey, QR extends QueryResult> extends AbstractCache {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchExpressionEvaluatorCache.class);

    // Making client's life easier - if it stores the cache in ThreadLocal variable and needs any other cache-related
    // information (e.g. custom invalidator), it does not need to create another ThreadLocal for that.
    private Object clientContextInformation;

    public Object getClientContextInformation() {
        return clientContextInformation;
    }

    public void setClientContextInformation(Object clientContextInformation) {
        this.clientContextInformation = clientContextInformation;
    }

    protected Map<QK, QR> queries = new HashMap<>();

    public List<V> getQueryResult(Class<? extends ObjectType> type, ObjectQuery query, ObjectSearchStrategyType searchStrategy, ExpressionEvaluationContext params, PrismContext prismContext) {
        QK queryKey = createQueryKey(type, query, searchStrategy, params, prismContext);
        if (queryKey != null) {         // TODO BRUTAL HACK
            QR result = queries.get(queryKey);
            if (result != null) {
                return result.getResultingList();
            }
        }
        return null;
    }

    public <T extends ObjectType> void putQueryResult(Class<T> type, ObjectQuery query, ObjectSearchStrategyType searchStrategy,
                                                      ExpressionEvaluationContext params, List<V> resultList, List<RV> rawResultList,
                                                      PrismContext prismContext) {
        QK queryKey = createQueryKey(type, query, searchStrategy, params, prismContext);
        if (queryKey != null) {     // TODO BRUTAL HACK
            QR queryResult = createQueryResult(resultList, rawResultList);
            queries.put(queryKey, queryResult);
        }
    }

    abstract protected QK createQueryKey(Class<? extends ObjectType> type, ObjectQuery query, ObjectSearchStrategyType searchStrategy,
                                         ExpressionEvaluationContext params, PrismContext prismContext);

    protected abstract QR createQueryResult(List<V> resultList, List<RV> rawResultList);

    @Override
    public String description() {
        return "Q:"+queries.size();
    }
}
