/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.caching;

import com.evolveum.midpoint.model.common.expression.evaluator.AbstractSearchExpressionEvaluator.ObjectFound;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.cache.AbstractThreadLocalCache;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for search expression-based evaluators.
 *
 * It needs to be customized in the following ways:
 *
 * - what is in the query key besides basic data - namely, what parts of {@link ExpressionEvaluationContext} should
 * be part of the key?
 * - should we store anything in addition to the resulting list of values? E.g. shadow kind in case of `associationTargetSearch`
 * that is used for invalidation?
 *
 * @param <V> type of cached result items
 * @param <O> type of raw values that we are searching for
 * @param <QK> query key
 * @param <QR> query result
 *
 * After refactoring, this class contains almost nothing ;) Consider removing it altogether.
 */
public abstract class AbstractSearchExpressionEvaluatorCache<
        V extends PrismValue,
        O extends ObjectType,
        QK extends QueryKey,
        QR extends QueryResult<V>>
        extends AbstractThreadLocalCache {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractSearchExpressionEvaluatorCache.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(AbstractSearchExpressionEvaluatorCache.class.getName() + ".content");

    // Making client's life easier - if it stores the cache in ThreadLocal variable and needs any other cache-related
    // information (e.g. custom invalidator), it does not need to create another ThreadLocal for that.
    private Object clientContextInformation;

    public Object getClientContextInformation() {
        return clientContextInformation;
    }

    public void setClientContextInformation(Object clientContextInformation) {
        this.clientContextInformation = clientContextInformation;
    }

    // We need thread-safety here e.g. because of size determination, see getSize() method.
    // Also probably because of MID-5355, although it's a bit unclear.
    Map<QK, QR> cachedSearches = new ConcurrentHashMap<>();

    public List<V> getSearchResult(
            Class<O> type,
            Collection<ObjectQuery> queries,
            ObjectSearchStrategyType searchStrategy,
            ExpressionEvaluationContext eeCtx) {
        QK key = createKey(type, queries, searchStrategy, eeCtx);
        QR result = cachedSearches.get(key);
        if (result != null) {
            return result.getResultingList();
        }
        return null;
    }

    public void putSearchResult(
            Class<O> type,
            Collection<ObjectQuery> queries,
            ObjectSearchStrategyType searchStrategy,
            ExpressionEvaluationContext params,
            Collection<? extends ObjectFound<O, V>> objectsFound) {
        cachedSearches.put(
                createKey(type, queries, searchStrategy, params),
                createQueryResult(objectsFound));
    }

    abstract protected @NotNull QK createKey(
            Class<O> type,
            Collection<ObjectQuery> queries,
            ObjectSearchStrategyType searchStrategy,
            ExpressionEvaluationContext eeCtx);

    protected abstract QR createQueryResult(Collection<? extends ObjectFound<O, V>> objectsFound);

    @Override
    public String description() {
        return "Q:"+ cachedSearches.size();
    }

    @Override
    protected int getSize() {
        return cachedSearches.size();
    }

    @Override
    protected void dumpContent(String threadName) {
        if (LOGGER_CONTENT.isInfoEnabled()) {
            cachedSearches.forEach((qk, qr) -> LOGGER.info("Cached search expression evaluation [{}] {}: {}", threadName, qk, qr));
        }
    }
}
