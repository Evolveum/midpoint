/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.LongVarbinaryJdbcType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;

import java.sql.Types;

/**
 * @author lazyman
 */
public class MidPointPostgreSQLDialect extends PostgreSQLDialect {

    public MidPointPostgreSQLDialect() {
        super( DatabaseVersion.make( 9, 5 ) );
    }

    @Override
    public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
        super.registerColumnTypes(typeContributions, serviceRegistry);
        var blobBytea = new DdlTypeImpl(SqlTypes.BLOB, "bytea", this);
        typeContributions.getTypeConfiguration().getDdlTypeRegistry().addDescriptor(blobBytea);
        typeContributions.getTypeConfiguration().getJdbcTypeRegistry().addDescriptor(Types.BLOB, LongVarbinaryJdbcType.INSTANCE);
    }

    @Override
    protected String columnType(int sqlTypeCode) {
        if (SqlTypes.BLOB == sqlTypeCode) {
            return "bytea";
        }
        return super.columnType(sqlTypeCode);
    }

}
