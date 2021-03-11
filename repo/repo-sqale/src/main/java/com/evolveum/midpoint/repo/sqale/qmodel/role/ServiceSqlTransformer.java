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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

public class ServiceSqlTransformer
        extends AbstractRoleSqlTransformer<ServiceType, QService, MService> {

    public ServiceSqlTransformer(
            SqlTransformerSupport transformerSupport, QServiceMapping mapping) {
        super(transformerSupport, mapping);
    }

    @Override
    public @NotNull MService toRowObjectWithoutFullObject(
            ServiceType schemaObject, JdbcSession jdbcSession) {
        MService row = super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        row.displayOrder = schemaObject.getDisplayOrder();

        return row;
    }
}
