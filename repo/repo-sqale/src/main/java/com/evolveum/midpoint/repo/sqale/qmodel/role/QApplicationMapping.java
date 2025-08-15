/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApplicationType;

import org.jetbrains.annotations.NotNull;

/**
 * Mapping between {@link QApplication} and {@link ApplicationType}.
 */
public class QApplicationMapping
        extends QAbstractRoleMapping<ApplicationType, QApplication, MApplication> {

    public static final String DEFAULT_ALIAS_NAME = "app";

    public static QApplicationMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QApplicationMapping(repositoryContext);
    }

    private QApplicationMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QRole.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ApplicationType.class, QApplication.class, repositoryContext);
    }

    @Override
    protected QApplication newAliasInstance(String alias) {
        return new QApplication(alias);
    }

    @Override
    public MApplication newRowObject() {
        return new MApplication();
    }

    @Override
    public @NotNull MApplication toRowObjectWithoutFullObject(
            ApplicationType schemaObject, JdbcSession jdbcSession) {
        return super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);
    }
}
