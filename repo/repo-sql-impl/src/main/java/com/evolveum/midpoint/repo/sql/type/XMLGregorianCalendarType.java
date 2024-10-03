/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.type;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.usertype.UserType;

/**
 * @author lazyman
 */
public class XMLGregorianCalendarType implements UserType<XMLGregorianCalendar> {

    public static final String NAME = "XMLGregorianCalendarType";

    private final static DatatypeFactory DATATYPE_FACTORY;

    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException dce) {
            throw new IllegalStateException("Exception while obtaining Datatype Factory instance", dce);
        }
    }

    @Override
    public XMLGregorianCalendar assemble(Serializable cached, Object owner) throws HibernateException {
        if (cached == null) {
            return null;
        }
        if (cached instanceof XMLGregorianCalendar) {
            return (XMLGregorianCalendar) ((XMLGregorianCalendar) cached).clone();
        }
        long date = (Long) cached;

        return asXMLGregorianCalendar(new Date(date));
    }

    @Override
    public int getSqlType() {
        return SqlTypes.TIMESTAMP;
    }

    @Override
    public int getDefaultSqlPrecision(Dialect dialect, JdbcType jdbcType) {
        return dialect.getDefaultTimestampPrecision();
    }

    @Override
    public Class<XMLGregorianCalendar> returnedClass() {
        return XMLGregorianCalendar.class;
    }

    @Override
    public boolean equals(XMLGregorianCalendar x, XMLGregorianCalendar y) throws HibernateException {
        return x == null ? y == null : x.equals(y);
    }

    @Override
    public int hashCode(XMLGregorianCalendar x) throws HibernateException {
        if (x == null) {
            return 0;
        }

        return x.hashCode();
    }

    @Override
    public XMLGregorianCalendar nullSafeGet(ResultSet rs, int index, SharedSessionContractImplementor session, Object owner)
            throws HibernateException, SQLException {
        Date date = rs.getTimestamp(index);
        if (date == null) {
            return null;
        }
        return asXMLGregorianCalendar(date);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, XMLGregorianCalendar value, int index, SharedSessionContractImplementor session)
            throws HibernateException, SQLException {
        XMLGregorianCalendar calendar = value;
        Date date = null;
        if (calendar != null) {
            date = asDate(calendar);
        }
        var relValue = date != null ? new Timestamp(date.getTime()) : null;
        st.setTimestamp(index, relValue);
    }

    @Override
    public XMLGregorianCalendar deepCopy(XMLGregorianCalendar value) throws HibernateException {
        if (value == null) {
            return null;
        }

        XMLGregorianCalendar calendar = value;
        return asXMLGregorianCalendar(asDate(calendar));
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(XMLGregorianCalendar value) throws HibernateException {
        if (value == null) {
            return null;
        }
        return (Serializable) value.clone();
        //XMLGregorianCalendar calendar = value;
        //return asDate(calendar).getTime();
    }

    @Override
    public XMLGregorianCalendar replace(XMLGregorianCalendar original, XMLGregorianCalendar target, Object owner) throws HibernateException {
        return deepCopy(original);
    }

    /**
     * Converts a java.util.Date into an instance of XMLGregorianCalendar
     *
     * @param date Instance of java.util.Date or a null reference
     * @return XMLGregorianCalendar instance whose value is based upon the
     * value in the date parameter. If the date parameter is null then
     * this method will simply return null.
     */
    public static XMLGregorianCalendar asXMLGregorianCalendar(java.util.Date date) {
        if (date == null) {
            return null;
        } else {
            GregorianCalendar gc = new GregorianCalendar();
            gc.setTimeInMillis(date.getTime());
            return DATATYPE_FACTORY.newXMLGregorianCalendar(gc);
        }
    }

    /**
     * Converts an XMLGregorianCalendar to an instance of java.util.Date
     *
     * @param xgc Instance of XMLGregorianCalendar or a null reference
     * @return java.util.Date instance whose value is based upon the
     * value in the xgc parameter. If the xgc parameter is null then
     * this method will simply return null.
     */
    public static java.util.Date asDate(XMLGregorianCalendar xgc) {
        if (xgc == null) {
            return null;
        } else {
            return xgc.toGregorianCalendar().getTime();
        }
    }

}
