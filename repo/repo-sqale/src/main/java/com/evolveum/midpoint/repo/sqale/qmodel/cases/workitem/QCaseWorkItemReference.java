/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.cases.workitem;

import java.sql.Types;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.PrimaryKey;

import com.evolveum.midpoint.repo.sqale.qmodel.ref.QReference;

/**
 * Querydsl query type for case work item reference tables.
 */
public class QCaseWorkItemReference extends QReference<MCaseWorkItemReference, MCaseWorkItem> {

    private static final long serialVersionUID = -2234353197695534782L;

    public static final ColumnMetadata WORK_ITEM_CID =
            ColumnMetadata.named("workItemCid").ofType(Types.BIGINT).notNull();

    public final NumberPath<Long> workItemCid = createLong("workItemCid", WORK_ITEM_CID);

    public final PrimaryKey<MCaseWorkItemReference> pk =
            createPrimaryKey(ownerOid, workItemCid, referenceType, relationId, targetOid);

    public QCaseWorkItemReference(String variable, String tableName) {
        this(variable, DEFAULT_SCHEMA_NAME, tableName);
    }

    public QCaseWorkItemReference(String variable, String schema, String table) {
        super(MCaseWorkItemReference.class, variable, schema, table);
    }

    @Override
    public BooleanExpression isOwnedBy(MCaseWorkItem ownerRow) {
        return ownerOid.eq(ownerRow.ownerOid)
                .and(workItemCid.eq(ownerRow.cid));
    }
}
