/*
 * Copyright (c) 2010-2013 Evolveum
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

import org.hibernate.dialect.SQLServer2008Dialect;

import java.sql.Types;

/**
 * @author lazyman
 */
public class UnicodeSQLServer2008Dialect extends SQLServer2008Dialect {

    private static final int MAX_LENGTH = 8000;

    public UnicodeSQLServer2008Dialect() {
        registerColumnType(Types.CLOB, "nvarchar(MAX)");
        registerColumnType(Types.LONGVARCHAR, "nvarchar(MAX) collate database_default");
        registerColumnType(Types.VARCHAR, "nvarchar(MAX) collate database_default");
        registerColumnType(Types.VARCHAR, MAX_LENGTH, "nvarchar($l) collate database_default");
    }
}
