/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType.F_ROLE_TYPE;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Mapping between {@link QRole} and {@link RoleType}.
 */
public class QRoleMapping
        extends QAbstractRoleMapping<RoleType, QRole, MRole> {

    public static final String DEFAULT_ALIAS_NAME = "r";

    public static final QRoleMapping INSTANCE = new QRoleMapping();

    private QRoleMapping() {
        super(QRole.TABLE_NAME, DEFAULT_ALIAS_NAME,
                RoleType.class, QRole.class);

        addItemMapping(F_ROLE_TYPE, stringMapper(q -> q.roleType));
    }

    @Override
    protected QRole newAliasInstance(String alias) {
        return new QRole(alias);
    }

    @Override
    public MRole newRowObject() {
        return new MRole();
    }

    @Override
    public @NotNull MRole toRowObjectWithoutFullObject(
            RoleType schemaObject, JdbcSession jdbcSession) {
        MRole row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.roleType = schemaObject.getRoleType();

        return row;
    }
}
