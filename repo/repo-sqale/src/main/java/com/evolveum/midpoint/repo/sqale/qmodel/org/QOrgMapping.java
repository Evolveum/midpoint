/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.org;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType.F_DISPLAY_ORDER;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType.F_TENANT;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.qmodel.role.QAbstractRoleMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Mapping between {@link QOrg} and {@link OrgType}.
 */
public class QOrgMapping
        extends QAbstractRoleMapping<OrgType, QOrg, MOrg> {

    public static final String DEFAULT_ALIAS_NAME = "org";

    private static QOrgMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QOrgMapping initOrgMapping(@NotNull SqaleRepoContext repositoryContext) {
        instance = new QOrgMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QOrgMapping getOrgMapping() {
        return Objects.requireNonNull(instance);
    }

    private QOrgMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QOrg.TABLE_NAME, DEFAULT_ALIAS_NAME,
                OrgType.class, QOrg.class, repositoryContext);

        addItemMapping(F_DISPLAY_ORDER, integerMapper(q -> q.displayOrder));
        addItemMapping(F_TENANT, booleanMapper(q -> q.tenant));
    }

    @Override
    protected QOrg newAliasInstance(String alias) {
        return new QOrg(alias);
    }

    @Override
    public MOrg newRowObject() {
        return new MOrg();
    }

    @Override
    public @NotNull MOrg toRowObjectWithoutFullObject(
            OrgType schemaObject, JdbcSession jdbcSession) {
        MOrg row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.displayOrder = schemaObject.getDisplayOrder();
        row.tenant = schemaObject.isTenant();

        return row;
    }
}
