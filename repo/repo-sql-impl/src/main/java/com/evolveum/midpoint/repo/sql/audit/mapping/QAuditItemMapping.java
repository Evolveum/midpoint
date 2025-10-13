/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sql.audit.mapping;

import static com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditItem.TABLE_NAME;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sql.audit.beans.MAuditItem;
import com.evolveum.midpoint.repo.sql.audit.querymodel.QAuditItem;
import com.evolveum.midpoint.repo.sqlbase.SqlRepoContext;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Mapping for {@link QAuditItem}, model type is non-containerable {@link ItemPathType}.
 */
public class QAuditItemMapping
        extends AuditTableMapping<ItemPathType, QAuditItem, MAuditItem> {

    public static final String DEFAULT_ALIAS_NAME = "ai";

    private static QAuditItemMapping instance;

    public static QAuditItemMapping init(@NotNull SqlRepoContext repositoryContext) {
        instance = new QAuditItemMapping(repositoryContext);
        return instance;
    }

    public static QAuditItemMapping get() {
        return Objects.requireNonNull(instance);
    }

    private QAuditItemMapping(@NotNull SqlRepoContext repositoryContext) {
        super(TABLE_NAME, DEFAULT_ALIAS_NAME,
                ItemPathType.class, QAuditItem.class, repositoryContext);
    }

    @Override
    protected QAuditItem newAliasInstance(String alias) {
        return new QAuditItem(alias);
    }
}
