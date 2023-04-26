/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.usertype.UserType;

public class PrefixedStringType implements UserType<String> {

    public static final String NAME = "PrefixedStringType";

    @Override
    public String assemble(Serializable cached, Object owner) throws HibernateException {
        return (String) cached;
    }

    @Override
    public int getSqlType() {
        return StandardBasicTypes.STRING.getSqlTypeCode();
    }

    @Override
    public Class<String> returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(String x, String y) throws HibernateException {
        return x != null ? x.equals(y) : y == null;
    }

    @Override
    public int hashCode(String x) throws HibernateException {
        if (x == null) {
            return 0;
        }
        return x.hashCode();
    }


    @Override
    public String nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        String value = rs.getString(position);
        if (isOracle(session) && value != null) {
            value = value.substring(1);
        }

        return value;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, String value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (isOracle(session) && value != null) {
            value = '\u0000' + (value);
        }
        st.setString(index, value);
    }

    private boolean isOracle(SharedSessionContractImplementor session) {
        //SessionFactoryImplementor factory = session.getFactory();
        //return Oracle10gDialect.class.isAssignableFrom(factory.getDialect().getClass());

        return false;
    }

    @Override
    public String deepCopy(String value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(String value) throws HibernateException {
        return value;
    }

    @Override
    public String replace(String original, String target, Object owner) throws HibernateException {
        return deepCopy(original);
    }
}
