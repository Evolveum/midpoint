/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.repo.sqale.qmodel.role;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.jetbrains.annotations.NotNull;

/**
 * Mapping between {@link QPolicy} and {@link PolicyType}.
 */
public class QPolicyMapping
        extends QAbstractRoleMapping<PolicyType, QPolicy, MPolicy> {

    public static final String DEFAULT_ALIAS_NAME = "p";

    public static QPolicyMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QPolicyMapping(repositoryContext);
    }

    private QPolicyMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QRole.TABLE_NAME, DEFAULT_ALIAS_NAME,
                PolicyType.class, QPolicy.class, repositoryContext);
    }

    @Override
    protected QPolicy newAliasInstance(String alias) {
        return new QPolicy(alias);
    }

    @Override
    public MPolicy newRowObject() {
        return new MPolicy();
    }

    @Override
    public @NotNull MPolicy toRowObjectWithoutFullObject(
            PolicyType schemaObject, JdbcSession jdbcSession) {
        MPolicy row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        return row;
    }
}
