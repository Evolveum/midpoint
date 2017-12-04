/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.dialect.PostgresPlusDialect;
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
