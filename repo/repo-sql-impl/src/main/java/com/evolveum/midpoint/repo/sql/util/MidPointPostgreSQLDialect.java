/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;

import java.sql.Types;

/**
 * @author lazyman
 */
public class MidPointPostgreSQLDialect extends PostgreSQL95Dialect {

    public MidPointPostgreSQLDialect() {

    }

    @Override
    protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.registerColumnTypes(typeContributions, serviceRegistry);
        var blobBytea = new DdlTypeImpl(SqlTypes.BLOB, "bytea", this);
        typeContributions.getTypeConfiguration().getDdlTypeRegistry().addDescriptor(blobBytea);

    }

    @Override
    protected String columnType(int sqlTypeCode) {
        if (SqlTypes.BLOB == sqlTypeCode) {
            return "bytea";
        }
        return super.columnType(sqlTypeCode);
    }

    /*
    @Override
    public SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
        if (Types.BLOB == sqlCode) {
            return LongVarbinaryTypeDescriptor.INSTANCE;
        }

        return super.getSqlTypeDescriptorOverride(sqlCode);
    }*/
}
