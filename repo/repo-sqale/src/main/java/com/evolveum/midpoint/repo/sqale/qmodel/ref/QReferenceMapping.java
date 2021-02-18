/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.ref;

import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.uuidMapper;

import com.evolveum.midpoint.repo.sqale.UriItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.SqaleModelMapping;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Mapping between {@link QReference} (and its subclasses) and {@link ObjectReferenceType}.
 * The mapping is the same for all subtypes, see different `INSTANCE_*` constants below.
 */
public class QReferenceMapping
        extends SqaleModelMapping<ObjectReferenceType, QReference, MReference> {

    // see also subtype specific alias names defined for instances below
    public static final String DEFAULT_ALIAS_NAME = "ref";

    public static final QReferenceMapping INSTANCE =
            new QReferenceMapping(QReference.TABLE_NAME, DEFAULT_ALIAS_NAME);
    public static final QReferenceMapping INSTANCE_ARCHETYPE =
            new QReferenceMapping("m_ref_archetype", "refa");
    // TODO the rest

    private QReferenceMapping(String tableName, String defaultAliasName) {
        super(tableName, defaultAliasName,
                ObjectReferenceType.class, QReference.class);

        // TODO owner and reference type is not possible to query, probably OK
        addItemMapping(ObjectReferenceType.F_RELATION,
                UriItemFilterProcessor.mapper(path(q -> q.relationId)));
        addItemMapping(ObjectReferenceType.F_OID, uuidMapper(path(q -> q.targetOid)));
    }

    @Override
    protected QReference newAliasInstance(String alias) {
        return new QReference(alias);
    }

//    @Override TODO
//    public TriggerSqlTransformer createTransformer(
//            SqlTransformerContext transformerContext) {
//        return new TriggerSqlTransformer(transformerContext, this);
//    }

    @Override
    public MReference newRowObject() {
        return new MReference();
    }
}
