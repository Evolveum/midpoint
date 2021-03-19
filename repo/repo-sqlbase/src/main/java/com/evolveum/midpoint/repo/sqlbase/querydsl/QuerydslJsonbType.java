/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.querydsl.sql.types.AbstractType;
import org.jetbrains.annotations.Nullable;
import org.postgresql.util.PGobject;

import com.evolveum.midpoint.util.exception.SystemException;

/**
 * String to JSONB converter for Querydsl.
 */
public class QuerydslJsonbType extends AbstractType<Jsonb> {

    public QuerydslJsonbType() {
        super(Types.OTHER);
    }

    @Override
    public Class<Jsonb> getReturnedClass() {
        return Jsonb.class;
    }

    @Nullable
    @Override
    public Jsonb getValue(ResultSet rs, int startIndex) throws SQLException {
        Object object = rs.getObject(startIndex);
        if (object == null) {
            return null;
        }

        if (object instanceof PGobject) {
            PGobject pgObject = (PGobject) object;
            if ("jsonb".equals(pgObject.getType())) {
                return new Jsonb(object.toString());
            }
        }
        throw new SystemException("Expected value for JSONB column, returned " + object.getClass());
    }

    @Override
    public void setValue(PreparedStatement st, int startIndex, Jsonb value) throws SQLException {
        PGobject jsonObject = new PGobject();
        jsonObject.setType("json");
        jsonObject.setValue(value.value);
        st.setObject(startIndex, jsonObject);
    }
}
