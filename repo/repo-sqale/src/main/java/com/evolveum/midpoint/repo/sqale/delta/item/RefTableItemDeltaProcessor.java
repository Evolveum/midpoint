/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.Collection;
import java.util.UUID;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.sqale.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.ReferenceSqlTransformer;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;

public class RefTableItemDeltaProcessor<Q extends QReference<?>, OQ extends FlexibleRelationalPathBase<OR>, OR>
        extends ItemDeltaValueProcessor<Referencable> {

    private final QReferenceMapping<Q, ?, OQ, OR> refTableMapping;

    public RefTableItemDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> context, // TODO OR as last here as well
            QReferenceMapping<Q, ?, OQ, OR> refTableMapping) {
        super(context);
        this.refTableMapping = refTableMapping;
    }

    @Override
    public void addValues(Collection<Referencable> values) {
        OR ownerRow = (OR) context.row(); // TODO cleanup when context has generic superclass
        ReferenceSqlTransformer<?, ?, OR> transformer =
                refTableMapping.createTransformer(context.transformerSupport());

        // It looks like the insert belongs to context, but there is no common insert contract.
        // Each transformer has different types and needs. What? No, I don't want to introduce
        // owner row type as another parametrized type on the transformer, thank you.
        values.forEach(ref -> transformer.insert(ref, ownerRow, context.jdbcSession()));
    }

    @Override
    public void deleteValues(Collection<Referencable> values) {
        Q r = refTableMapping.defaultAlias();
        for (Referencable ref : values) {
            Integer relId = context.transformerSupport().searchCachedRelationId(ref.getRelation());
            context.jdbcSession().newDelete(r)
                    .where(r.ownerOid.eq(context.objectOid())
                            .and(r.targetOid.eq(UUID.fromString(ref.getOid())))
                            .and(r.relationId.eq(relId)))
                    .execute();
        }
    }

    @Override
    public void delete() {
        Q r = refTableMapping.defaultAlias();
        context.jdbcSession().newDelete(r)
                .where(r.ownerOid.eq(context.objectOid()))
                .execute();
    }
}
