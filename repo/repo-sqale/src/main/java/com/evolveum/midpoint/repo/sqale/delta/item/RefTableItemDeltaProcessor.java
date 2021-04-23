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
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.MReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.ReferenceSqlTransformer;

public class RefTableItemDeltaProcessor extends ItemDeltaValueProcessor<Referencable> {

    private final QObjectReferenceMapping refTableMapping;

    public RefTableItemDeltaProcessor(
            SqaleUpdateContext<?, ?, ?> context,
            QObjectReferenceMapping refTableMapping) {
        super(context);
        this.refTableMapping = refTableMapping;
    }

    @Override
    public void addValues(Collection<Referencable> values) {
        MObject ownerRow = context.row();
        ReferenceSqlTransformer<QObjectReference, MReference> transformer =
                refTableMapping.createTransformer(context.transformerSupport());

        // It looks like the insert belongs to context, but there is no common insert contract.
        // Each transformer has different types and needs. What? No, I don't want to introduce
        // owner row type as another parametrized type on the transformer, thank you.
        values.forEach(ref -> transformer.insert(ref, ownerRow, context.jdbcSession()));
    }

    @Override
    public void deleteValues(Collection<Referencable> values) {
        QObjectReference r = refTableMapping.defaultAlias();
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
        QObjectReference r = refTableMapping.defaultAlias();
        context.jdbcSession().newDelete(r)
                .where(r.ownerOid.eq(context.objectOid()))
                .execute();
    }
}
