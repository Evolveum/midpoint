/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.role;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerSupport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class RoleSqlTransformer
        extends AbstractRoleSqlTransformer<RoleType, QRole, MRole> {

    public RoleSqlTransformer(
            SqlTransformerSupport transformerSupport, QRoleMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MRole toRowObjectWithoutFullObject(
            RoleType schemaObject, JdbcSession jdbcSession) {
        MRole row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.roleType = schemaObject.getRoleType();

        return row;
    }
}
