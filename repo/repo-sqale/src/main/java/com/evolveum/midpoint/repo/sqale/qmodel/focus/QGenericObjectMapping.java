/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.focus;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericObjectType;

/**
 * Mapping between {@link QGenericObject} and {@link GenericObjectType}.
 */
public class QGenericObjectMapping
        extends QFocusMapping<GenericObjectType, QGenericObject, MFocus> {

    public static final String DEFAULT_ALIAS_NAME = "go";

    public static QGenericObjectMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QGenericObjectMapping(repositoryContext);
    }

    private QGenericObjectMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QGenericObject.TABLE_NAME, DEFAULT_ALIAS_NAME,
                GenericObjectType.class, QGenericObject.class, repositoryContext);
    }

    @Override
    protected QGenericObject newAliasInstance(String alias) {
        return new QGenericObject(alias);
    }

    @Override
    public MFocus newRowObject() {
        return new MFocus();
    }

    @Override
    public @NotNull MFocus toRowObjectWithoutFullObject(
            GenericObjectType genericObject, JdbcSession jdbcSession) {
        MFocus row = super.toRowObjectWithoutFullObject(genericObject, jdbcSession);

        return row;
    }
}
