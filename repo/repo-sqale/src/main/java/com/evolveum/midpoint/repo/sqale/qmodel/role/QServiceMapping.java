/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType.F_DISPLAY_ORDER;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

/**
 * Mapping between {@link QService} and {@link ServiceType}.
 */
public class QServiceMapping
        extends QAbstractRoleMapping<ServiceType, QService, MService> {

    public static final String DEFAULT_ALIAS_NAME = "svc";

    public static QServiceMapping init(@NotNull SqaleRepoContext repositoryContext) {
        return new QServiceMapping(repositoryContext);
    }

    private QServiceMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QService.TABLE_NAME, DEFAULT_ALIAS_NAME,
                ServiceType.class, QService.class, repositoryContext);

        addItemMapping(F_DISPLAY_ORDER, integerMapper(q -> q.displayOrder));
    }

    @Override
    protected QService newAliasInstance(String alias) {
        return new QService(alias);
    }

    @Override
    public MService newRowObject() {
        return new MService();
    }

    @Override
    public @NotNull MService toRowObjectWithoutFullObject(
            ServiceType schemaObject, JdbcSession jdbcSession) {
        MService row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.displayOrder = schemaObject.getDisplayOrder();

        return row;
    }
}
