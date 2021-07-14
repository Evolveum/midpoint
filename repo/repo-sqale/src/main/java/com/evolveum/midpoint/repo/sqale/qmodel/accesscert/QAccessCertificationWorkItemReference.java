/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import java.sql.Types;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;

/**
 * Querydsl query type for case work item reference tables.
 */
public class QAccessCertificationWorkItemReference extends QReference<MAccessCertificationWorkItemReference, MAccessCertificationWorkItem> {

    private static final long serialVersionUID = -4323954643404516391L;

    public static final ColumnMetadata ACCESS_CERT_CASE_CID =
            ColumnMetadata.named("accessCertCaseCid").ofType(Types.BIGINT).notNull();

    public static final ColumnMetadata ACCESS_CERT_WORK_ITEM_CID =
            ColumnMetadata.named("accessCertWorkItemCid").ofType(Types.BIGINT).notNull();

    public final NumberPath<Long> accessCertCaseCid = createLong("accessCertCaseCid", ACCESS_CERT_CASE_CID);

    public final NumberPath<Long> accessCertWorkItemCid = createLong("accessCertWorkItemCid", ACCESS_CERT_WORK_ITEM_CID);

    public final PrimaryKey<MAccessCertificationWorkItemReference> pk =
            createPrimaryKey(ownerOid, accessCertWorkItemCid, referenceType, relationId, targetOid);

    public QAccessCertificationWorkItemReference(String variable, String tableName) {
        this(variable, DEFAULT_SCHEMA_NAME, tableName);
    }

    public QAccessCertificationWorkItemReference(String variable, String schema, String table) {
        super(MAccessCertificationWorkItemReference.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(MAccessCertificationWorkItem ownerRow) {
        return ownerOid.eq(ownerRow.ownerOid)
                .and(accessCertWorkItemCid.eq(ownerRow.cid));
    }
}
