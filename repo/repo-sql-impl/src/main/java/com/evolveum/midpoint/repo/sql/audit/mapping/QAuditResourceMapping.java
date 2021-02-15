/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.mapping;

import static com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditItem.TABLE_NAME;

import com.evolveum.midpoint.repo.sql.audit.beans.MAuditResource;
import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditResource;
import com.evolveum.midpoint.repo.sqlbase.mapping.QueryTableMapping;

/**
 * Mapping for {@link QAuditResource}, no transformation supported.
 */
public class QAuditResourceMapping
        extends QueryTableMapping<String, QAuditResource, MAuditResource> {

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
}
