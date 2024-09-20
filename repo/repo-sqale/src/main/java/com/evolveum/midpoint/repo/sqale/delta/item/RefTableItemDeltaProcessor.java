/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.delta.item;

import java.util.Collection;
import java.util.UUID;

import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.delta.ItemDeltaValueProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReferenceMapping;
import com.evolveum.midpoint.repo.sqale.update.SqaleUpdateContext;
import com.evolveum.midpoint.repo.sqlbase.querydsl.FlexibleRelationalPathBase;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.Nullable;

/**
 * Delta value processor for multi-value references stored in separate tables.
 *
 * @param <Q> type of entity path for the reference table
 * @param <OQ> query type of the reference owner
 * @param <OR> row type of the reference owner (related to {@link OQ})
 */
public class RefTableItemDeltaProcessor<Q extends QReference<?, OR>, OQ extends FlexibleRelationalPathBase<OR>, OR>
        extends ItemDeltaValueProcessor<ObjectReferenceType> {

    protected final SqaleUpdateContext<?, OQ, OR> context;
    private final QReferenceMapping<Q, ?, OQ, OR> refTableMapping;

    public RefTableItemDeltaProcessor(
            SqaleUpdateContext<?, OQ, OR> context,
            QReferenceMapping<Q, ?, OQ, OR> refTableMapping) {
        super(context);
        this.context = context;
        this.refTableMapping = refTableMapping;
    }

    @Override
    public void addValues(Collection<ObjectReferenceType> values)
            throws SchemaException {
        for (ObjectReferenceType ref : values) {
           addRealValue(ref);
        }
    }

    @Override
    protected void addRealValue(ObjectReferenceType ref) throws SchemaException {
        ref = SqaleUtils.referenceWithTypeFixed(ref);
        context.insertOwnedRow(refTableMapping, ref);
    }

    @Override
    public void deleteValues(Collection<ObjectReferenceType> values) {
        for (ObjectReferenceType ref : values) {
            deleteRealValue(ref);
        }
    }

    @Override
    protected void deleteRealValue(ObjectReferenceType ref) {
        Q r = refTableMapping.defaultAlias();
        Integer relId = context.repositoryContext().searchCachedRelationId(ref.getRelation());
        context.jdbcSession().newDelete(r)
                .where(r.isOwnedBy(context.row())
                        .and(r.targetOid.eq(UUID.fromString(ref.getOid())))
                        .and(r.relationId.eq(relId)))
                .execute();
    }

    @Override
    public @Nullable ObjectReferenceType convertRealValue(Object realValue) {
        if (realValue instanceof ObjectReferenceType ort) {
            return ort;
        }
        if (realValue instanceof Referencable realRef) {
            var prismRef = realRef.asReferenceValue().clone();
            var ret = new ObjectReferenceType();
            ret.setupReferenceValue(prismRef);
            return ret;
        }
        throw new UnsupportedOperationException("Unknown reference type");
    }

    @Override
    public void delete() {
        Q r = refTableMapping.defaultAlias();
        context.jdbcSession().newDelete(r)
                .where(r.isOwnedBy(context.row()))
                .execute();
    }
}
