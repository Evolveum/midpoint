/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.audit.qmodel;

import static com.evolveum.midpoint.repo.sqale.audit.qmodel.QAuditPayload.TABLE_NAME;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;

import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.mapping.SqaleTableMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordPayloadType;

/**
 * Mapping between {@link QAuditPayload} and {@link AuditEventRecordPayloadType}.
 */
public class QAuditPayloadMapping
        extends SqaleTableMapping<AuditEventRecordPayloadType, QAuditPayload, MAuditPayload> {

    public static final String DEFAULT_ALIAS_NAME = "ap";

    private static QAuditPayloadMapping instance;

    public static QAuditPayloadMapping init(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QAuditPayloadMapping(repositoryContext);
        return instance;
    }

    public static QAuditPayloadMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAuditPayloadMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                AuditEventRecordPayloadType.class, QAuditPayload.class, repositoryContext);
    }

    @Override
    protected QAuditPayload newAliasInstance(String alias) {
        return new QAuditPayload(alias);
    }

    @Override
    public AuditEventRecordPayloadType toSchemaObject(MAuditPayload row) {
        return new AuditEventRecordPayloadType()
                .name(row.name)
                .contentType(row.contentType)
                .content(contentToString(row.content));
    }

    public byte[] contentToBytes(String content) {
        return content != null ? MiscUtil.stringToBytes(content) : null;
    }

    private String contentToString(byte[] content) {
        return content != null ? new String(content, StandardCharsets.UTF_8) : null;
    }

    @Override
    public AuditEventRecordPayloadType toSchemaObject(
            @NotNull Tuple row, @NotNull QAuditPayload entityPath, @NotNull JdbcSession jdbcSession,
            Collection<SelectorOptions<GetOperationOptions>> options)
            throws SchemaException {
        throw new UnsupportedOperationException(); // implemented through MAuditEventRecord batching
    }
}
