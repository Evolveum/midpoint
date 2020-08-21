/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querymodel.mapping;

import static com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditItem.TABLE_NAME;

import com.querydsl.sql.Configuration;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.pure.SqlTransformer;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditDelta;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectDeltaOperationType;

/**
 * Mapping between {@link QAuditDelta} and {@link ObjectDeltaOperationType}.
 */
public class QAuditDeltaMapping
        extends QueryModelMapping<ObjectDeltaOperationType, QAuditDelta, MAuditDelta> {

    public static final String DEFAULT_ALIAS_NAME = "ad";

    public static final QAuditDeltaMapping INSTANCE = new QAuditDeltaMapping();

    private QAuditDeltaMapping() {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                ObjectDeltaOperationType.class, QAuditDelta.class);
    }

    @Override
    protected QAuditDelta newAliasInstance(String alias) {
        return new QAuditDelta(alias);
    }

    @Override
    public SqlTransformer<ObjectDeltaOperationType, QAuditDelta, MAuditDelta> createTransformer(
            PrismContext prismContext, Configuration querydslConfiguration) {
        return new AuditDeltaSqlTransformer(prismContext, this, querydslConfiguration);
    }
}
