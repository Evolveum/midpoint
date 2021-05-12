/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.audit.mapping;

import static com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditItem.TABLE_NAME;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sql.audit.beans.MAuditRefValue;
import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditRefValue;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordReferenceType;

/**
 * Mapping between {@link QAuditRefValue} and {@link AuditEventRecordReferenceType}.
 */
public class QAuditRefValueMapping
        extends AuditTableMapping<AuditEventRecordReferenceType, QAuditRefValue, MAuditRefValue> {

    public static final String DEFAULT_ALIAS_NAME = "aref";

    private static QAuditRefValueMapping instance;

    public static QAuditRefValueMapping init(@NotNull SqlRepoContext repositoryContext) {
        instance = new QAuditRefValueMapping(repositoryContext);
        return instance;
    }

    public static QAuditRefValueMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAuditRefValueMapping(@NotNull SqlRepoContext repositoryContext) {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                AuditEventRecordReferenceType.class, QAuditRefValue.class, repositoryContext);
    }

    @Override
    protected QAuditRefValue newAliasInstance(String alias) {
        return new QAuditRefValue(alias);
    }
}
