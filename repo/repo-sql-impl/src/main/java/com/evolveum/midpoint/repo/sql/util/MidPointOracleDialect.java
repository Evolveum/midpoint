/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.util;

import org.hibernate.dialect.Oracle10gDialect;

import java.sql.Types;

/**
 * @author lazyman
 */
public class MidPointOracleDialect extends Oracle10gDialect {

    @Override
    protected void registerLargeObjectTypeMappings() {
        super.registerLargeObjectTypeMappings();

        registerColumnType(Types.LONGVARCHAR, "clob");
    }

//    protected void registerLargeObjectTypeMappings() {
//        registerColumnType( Types.BINARY, 2000, "raw($l)" );
//        registerColumnType( Types.BINARY, "long raw" );
//
//        registerColumnType( Types.VARBINARY, 2000, "raw($l)" );
//        registerColumnType( Types.VARBINARY, "long raw" );
//
//        registerColumnType( Types.BLOB, "blob" );
//        registerColumnType( Types.CLOB, "clob" );
//
//        registerColumnType(Types.LONGVARCHAR, 4000, "varchar2($l char)");
//        registerColumnType( Types.LONGVARCHAR, "clob" );
//        registerColumnType( Types.LONGVARBINARY, "long raw" );
//    }
//
//    protected void registerCharacterTypeMappings() {
//        registerColumnType(Types.CHAR, "char(1 char)");
//        registerColumnType(Types.VARCHAR, 4000, "varchar2($l char)");
//        registerColumnType(Types.VARCHAR, "clob");
//    }
}
