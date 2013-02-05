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

package com.evolveum.midpoint.repo.sql.type;

import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.StringType;
import org.hibernate.usertype.UserType;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author lazyman
 */
public class QNameType implements UserType {

    public static final String EMPTY_QNAME_COLUMN_VALUE = " ";

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public int[] sqlTypes() {
        return new int[]{
                StringType.INSTANCE.sqlType(), StringType.INSTANCE.sqlType()
        };
    }

    @Override
    public Class returnedClass() {
        return QName.class;
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
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws
            HibernateException, SQLException {

        String namespaceURI = StringType.INSTANCE.nullSafeGet(rs, names[0], session);
        String localPart = StringType.INSTANCE.nullSafeGet(rs, names[1], session);

        return recreateQName(namespaceURI, localPart);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws
            HibernateException, SQLException {
        QName qname = optimizeQName((QName) value);
        String namespaceURI = qname != null ? qname.getNamespaceURI() : null;
        String localPart = qname != null ? qname.getLocalPart() : null;

        StringType.INSTANCE.nullSafeSet(st, namespaceURI, index, session);
        StringType.INSTANCE.nullSafeSet(st, localPart, index + 1, session);
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        if (value == null) {
            return null;
        }
        QName qname = (QName) value;
        return new QName(qname.getNamespaceURI(), qname.getLocalPart(), qname.getPrefix());
    }

    @Override
    public boolean isMutable() {
        return true;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (QName) value;
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return deepCopy(original);
    }

    public static QName optimizeQName(QName qname) {
        return qname;

//        //todo optimize later
//        if (qname == null) {
//            return null;
//        }
//
//        return optimizeQName(qname.getNamespaceURI(), qname.getLocalPart());
    }

//    private static QName optimizeQName(String namespaceURI, String localPart) {
//        if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(namespaceURI)) {
//            namespaceURI = null;
//        }
//
//        if (StringUtils.isEmpty(localPart)) {
//            return null;
//        }
//
//        if (namespaceURI == null) {
//            return new QName(localPart);
//        }
//
//        return new QName(namespaceURI, localPart);
//    }

    private static QName recreateQName(String namespaceURI, String localPart) {
//        if (namespaceURI == null) {
//            namespaceURI = XMLConstants.W3C_XML_SCHEMA_NS_URI;
//        }
//
//        if (StringUtils.isEmpty(localPart)) {
//            return null;
//        }
        if (localPart == null) {
            return null;
        }

        return new QName(namespaceURI, localPart);
    }

    public static QName assembleQName(String namespaceURI, String localPart) {
        if (StringUtils.isBlank(localPart)) {
            return null;
        }

        String namespace = EMPTY_QNAME_COLUMN_VALUE.equals(namespaceURI) ? null : namespaceURI;

        return new QName(namespace, localPart);
    }

    public static String[] disassembleQName(QName qname) {
        String[] value = {EMPTY_QNAME_COLUMN_VALUE, EMPTY_QNAME_COLUMN_VALUE};
        if (qname == null) {
            return value;
        }

        value[0] = qname.getNamespaceURI() == null ? EMPTY_QNAME_COLUMN_VALUE : qname.getNamespaceURI();
        value[1] = qname.getLocalPart() == null ? EMPTY_QNAME_COLUMN_VALUE : qname.getLocalPart();

        return value;
    }
}
