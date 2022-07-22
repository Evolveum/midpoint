/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import static com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditRefValue.TABLE_NAME;

import java.util.Collection;
import java.util.Objects;

import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordReferenceType;

/**
 * Mapping between {@link QAuditRefValue} and {@link AuditEventRecordReferenceType}.
 */
public class QAuditRefValueMapping
        extends SqaleTableMapping<AuditEventRecordReferenceType, QAuditRefValue, MAuditRefValue> {

    public static final String DEFAULT_ALIAS_NAME = "aref";

    private static QAuditRefValueMapping instance;

    public static QAuditRefValueMapping init(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QAuditRefValueMapping(repositoryContext);
        return instance;
    }

    public static QAuditRefValueMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAuditRefValueMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                AuditEventRecordReferenceType.class, QAuditRefValue.class, repositoryContext);
    }

    @Override
    protected QAuditRefValue newAliasInstance(String alias) {
        return new QAuditRefValue(alias);
    }

    @Override
    public AuditEventRecordReferenceType toSchemaObject(MAuditRefValue row) {
        throw new UnsupportedOperationException(); // implemented in service
    }

    @Override
    public AuditEventRecordReferenceType toSchemaObject(
            @NotNull Tuple row, @NotNull QAuditRefValue entityPath, @NotNull JdbcSession jdbcSession, Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {
        throw new UnsupportedOperationException(); // implemented in service
    }
}
