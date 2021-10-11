/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
/**
 * @author skublik
 */
public abstract class ConverterSqlAndJavaObject {

    public abstract <T> T convertToValue(ResultSet rs, String nameOfColumn, Class<T> javaClazz) throws SQLException;

    public abstract <T> T convertToValue(ResultSet rs, int index, Class<T> javaClazz) throws SQLException;

    public abstract Types getSqlType(Object value);

}
