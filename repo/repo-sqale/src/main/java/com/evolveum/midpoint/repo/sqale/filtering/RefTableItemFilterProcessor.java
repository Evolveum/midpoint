/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.filtering;

import com.querydsl.core.types.Predicate;

import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.ItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

/**
 * Filter processor for reference item paths resolved via {@link QReference} tables.
 * This just joins the reference table and then delegates to {@link RefItemFilterProcessor}.
 */
public class RefTableItemFilterProcessor<Q extends QReference<R>, R extends MReference,
        OQ extends FlexibleRelationalPathBase<OR>, OR>
        extends ItemFilterProcessor<RefFilter> {

    private final SqlQueryContext<?, OQ, OR> context;
    private final QReferenceMapping<Q, R, OQ, OR> referenceMapping;

    public RefTableItemFilterProcessor(
            SqlQueryContext<?, OQ, OR> context, QReferenceMapping<Q, R, OQ, OR> referenceMapping) {
        super(context);
        this.context = context;
        this.referenceMapping = referenceMapping;
    }

    @Override
    public Predicate process(RefFilter filter) {
        SqlQueryContext<?, Q, R> refContext =
                context.leftJoin(referenceMapping, referenceMapping.joinOnPredicate());
        QReference<?> ref = refContext.path();

        return new RefItemFilterProcessor(context, ref.targetOid, ref.targetType, ref.relationId)
                .process(filter);
    }
}
