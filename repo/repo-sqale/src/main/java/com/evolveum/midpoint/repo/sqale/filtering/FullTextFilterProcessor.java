/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.FullTextFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;

/**
 * Filter processor that resolves {@link FullTextFilter}.
 */
public class FullTextFilterProcessor implements FilterProcessor<FullTextFilter> {

    private final SqaleQueryContext<?, ?, ?> context;

    public FullTextFilterProcessor(SqaleQueryContext<?, ?, ?> context) {
        this.context = context;
    }

    @Override
    public Predicate process(FullTextFilter filter) throws QueryException {
        if (filter.getValues().size() != 1) {
            throw new QueryException("FullText filter currently supports only a single string");
        }
        String text = filter.getValues().iterator().next();
        String normalized = PrismContext.get().getDefaultPolyStringNormalizer().normalize(text);
        String[] words = StringUtils.split(normalized);
        if (words.length == 0) {
            return null; // no condition, matches everything
        }

        if (!(context.mapping() instanceof QObjectMapping)) {
            throw new QueryException("Fulltext currently supported only on objects");
        }

        Predicate predicate = null;
        for (String word : words) {
            // and() is null safe on both sides
            predicate = ExpressionUtils.and(predicate,
                    // We know it's object context, so we can risk the cast.
                    context.path(QObject.class).fullTextInfo.contains(word));
        }

        return predicate;
    }
}
