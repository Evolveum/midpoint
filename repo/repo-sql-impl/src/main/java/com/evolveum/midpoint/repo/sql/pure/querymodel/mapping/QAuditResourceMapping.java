/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.pure.querymodel.mapping;

import static com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditItem.TABLE_NAME;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.pure.SqlTransformer;
import com.evolveum.midpoint.repo.sql.pure.mapping.QueryModelMapping;
import com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditResource;
import com.evolveum.midpoint.repo.sql.pure.querymodel.beans.MAuditResource;

/**
 * Mapping for {@link QAuditResource}, no transformation supported.
 */
public class QAuditResourceMapping
        extends QueryModelMapping<String, QAuditResource, MAuditResource> {

    public static final String DEFAULT_ALIAS_NAME = "ares";

    public static final QAuditResourceMapping INSTANCE = new QAuditResourceMapping();

    private QAuditResourceMapping() {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                String.class, QAuditResource.class);
    }

    @Override
    protected QAuditResource newAliasInstance(String alias) {
        return new QAuditResource(alias);
    }

    @Override
    public SqlTransformer<String, MAuditResource> createTransformer(
            PrismContext prismContext) {
        throw new UnsupportedOperationException("handled by AuditEventRecordSqlTransformer");
    }
}
