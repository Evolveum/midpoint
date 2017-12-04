package com.evolveum.midpoint.repo.sql.type;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author lazyman
 */
public class PrefixedStringType implements UserType {

    public static final String NAME = "PrefixedStringType";

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public int[] sqlTypes() {
        return new int[]{StringType.INSTANCE.sqlType()};
    }

    @Override
    public Class returnedClass() {
        return String.class;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return x != null ? x.equals(y) : y == null;
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        if (x == null) {
            return 0;
        }
        return x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        String value = StringType.INSTANCE.nullSafeGet(rs, names[0], session);
        if (isOracle(session) && value != null) {
            value = value.substring(1);
        }

        return value;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        if (isOracle(session) && value != null) {
            value = '\u0000' + ((String) value);
        }

        StringType.INSTANCE.nullSafeSet(st, value, index, session);
    }

    private boolean isOracle(SharedSessionContractImplementor session) {
        SessionFactoryImplementor factory = session.getFactory();
        //System.out.println("]]] " + Oracle10gDialect.class.isAssignableFrom(factory.getDialect().getClass()));
        return Oracle10gDialect.class.isAssignableFrom(factory.getDialect().getClass());
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (String) value;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return deepCopy(original);
    }
}
