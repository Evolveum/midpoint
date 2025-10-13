/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.audit.mapping;

import static com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditItem.TABLE_NAME;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sql.audit.beans.MAuditResource;
import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditResource;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;

/**
 * Mapping for {@link QAuditResource}.
 */
public class QAuditResourceMapping
        extends AuditTableMapping<String, QAuditResource, MAuditResource> {

    public static final String DEFAULT_ALIAS_NAME = "ares";

    private static QAuditResourceMapping instance;

    public static QAuditResourceMapping init(@NotNull SqlRepoContext repositoryContext) {
        instance = new QAuditResourceMapping(repositoryContext);
        return instance;
    }

    public static QAuditResourceMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAuditResourceMapping(@NotNull SqlRepoContext repositoryContext) {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                String.class, QAuditResource.class, repositoryContext);
    }

    @Override
    protected QAuditResource newAliasInstance(String alias) {
        return new QAuditResource(alias);
    }
}
