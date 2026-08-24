/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.filtering;

import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.StringExpression;
import com.querydsl.sql.SQLQuery;

import com.evolveum.midpoint.prism.query.FullTextFilter;
import com.evolveum.midpoint.repo.sqale.SqaleQueryContext;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecord;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditEventRecordMapping;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditPayload;
import com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditPayloadMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.QueryException;
import com.evolveum.midpoint.repo.sqlbase.filtering.FilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.QuerydslUtils;
import com.evolveum.midpoint.schema.util.FullTextSearchUtil;

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
        String[] words = FullTextSearchUtil.normalizeWords(filter.getValues().iterator().next());
        if (words.length == 0) {
            return null; // no condition, matches everything
        }

        if (context.mapping() instanceof QObjectMapping) {
            // We know it's object context, so we can risk the cast.
            return predicateForWords(context.path(QObject.class).fullTextInfo, words);
        } else if (context.mapping() instanceof QAuditEventRecordMapping) {
            return auditPayloadExistsPredicate(words);
        }

        throw new QueryException("FullText filter is not supported for this type");
    }

    /**
     * Creates a predicate matching audit records whose payload contains all full-text search words.
     *
     * The payload is correlated with the current audit record by record ID and timestamp.
     * All words must match the same payload row.
     */
    private Predicate auditPayloadExistsPredicate(String[] words) {
        QAuditEventRecord audit = context.path(QAuditEventRecord.class);
        QAuditPayload payload = QAuditPayloadMapping.get().newAlias("apft");

        Predicate predicate = payload.recordId.eq(audit.id)
                .and(payload.timestamp.eq(audit.timestamp));
        predicate = ExpressionUtils.and(predicate, predicateForWords(payload.searchableText, words));

        return new SQLQuery<>()
                .select(QuerydslUtils.EXPRESSION_ONE)
                .from(payload)
                .where(predicate)
                .exists();
    }

    private Predicate predicateForWords(StringExpression path, String[] words) {
        Predicate predicate = null;
        for (String word : words) {
            // and() is null safe on both sides
            predicate = ExpressionUtils.and(predicate, path.contains(word));
        }
        return predicate;
    }
}
