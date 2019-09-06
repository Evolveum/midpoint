/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.type.descriptor.sql.LongVarbinaryTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

import java.sql.Types;

/**
 * @author lazyman
 */
public class MidPointPostgreSQLDialect extends PostgreSQL95Dialect {

    public MidPointPostgreSQLDialect() {
        registerColumnType(Types.BLOB, "bytea");
    }

    @Override
    public SqlTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
        if (Types.BLOB == sqlCode) {
            return LongVarbinaryTypeDescriptor.INSTANCE;
        }

        return super.getSqlTypeDescriptorOverride(sqlCode);
    }
}
