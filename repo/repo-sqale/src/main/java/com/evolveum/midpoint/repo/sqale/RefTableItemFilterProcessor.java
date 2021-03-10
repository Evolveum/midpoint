/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.mapping.item.ItemSqlMapper;

/**
 * Filter processor for reference item paths resolved via {@link QReference} tables.
 * This just joins the reference table and then delegates to {@link RefItemIntFilterProcessor}.
 */
public class RefTableItemFilterProcessor
        extends ItemFilterProcessor<RefFilter> {

    /** Returns the mapper function creating the ref-filter processor from query context. */
    public static ItemSqlMapper mapper(QReferenceMapping qReferenceMapping) {
        return new ItemSqlMapper(ctx -> new RefTableItemFilterProcessor(ctx, qReferenceMapping));
    }

    private final QReferenceMapping qReferenceMapping;

    public RefTableItemFilterProcessor(
            SqlQueryContext<?, ?, ?> context, QReferenceMapping qReferenceMapping) {
        super(context);
        this.qReferenceMapping = qReferenceMapping;
    }

    @Override
    public Predicate process(RefFilter filter) {
        // the cast is NOT redundant really (IDEA thinks so), it's needed for "o" in lambda
        @SuppressWarnings({ "RedundantCast", "unchecked" })
        SqlQueryContext<?, QReference, MReference> refContext =
                ((SqlQueryContext<?, QObject<?>, ?>) context)
                        .leftJoin(qReferenceMapping, (o, r) -> o.oid.eq(r.ownerOid));
        QReference ref = refContext.path();

        return new RefItemIntFilterProcessor(context, ref.targetOid, ref.targetType, ref.relationId)
                .process(filter);
    }
}
