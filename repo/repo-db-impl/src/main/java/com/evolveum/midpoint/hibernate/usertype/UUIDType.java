/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.hibernate.usertype;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class UUIDType implements UserType {

    public static final String code_id = "$Id$";
    private static final String CAST_EXCEPTION_TEXT = " cannot be cast to a java.util.UUID.";

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#assemble(java.io.Serializable,
     * java.lang.Object)
     */
    public Object assemble(Serializable cached, Object owner) throws HibernateException {

        if (!String.class.isAssignableFrom(cached.getClass())) {
            return null;
        }

        return UUID.fromString((String) cached);
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#deepCopy(java.lang.Object)
     */
    public Object deepCopy(Object value) throws HibernateException {

        if (!UUID.class.isAssignableFrom(value.getClass())) {
            throw new HibernateException(value.getClass().toString() + CAST_EXCEPTION_TEXT);
        }

        UUID other = (UUID) value;

        return UUID.fromString(other.toString());
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#disassemble(java.lang.Object)
     */
    public Serializable disassemble(Object value) throws HibernateException {

        System.out.println(value.toString());
        return value.toString();
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#equals(java.lang.Object,
     * java.lang.Object)
     */
    public boolean equals(Object x, Object y) throws HibernateException {
        if (x == y) {
            return true;
        }

        if ((null == x)&&(null != y)) {
            return false;
        }
        if ((null == y)&&(null != x)) {
            return false;
        }

        if (!UUID.class.isAssignableFrom(x.getClass())) {
            throw new HibernateException(x.getClass().toString() + CAST_EXCEPTION_TEXT);
        } else if (!UUID.class.isAssignableFrom(y.getClass())) {

            throw new HibernateException(y.getClass().toString() + CAST_EXCEPTION_TEXT);
        }

        UUID a = (UUID) x;
        UUID b = (UUID) y;

        return a.equals(b);
    }



    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#hashCode(java.lang.Object)
     */
    public int hashCode(Object x) throws HibernateException {
        if (!UUID.class.isAssignableFrom(x.getClass())) {
            throw new HibernateException(x.getClass().toString() + CAST_EXCEPTION_TEXT);
        }

        return x.hashCode();
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#isMutable()
     */
    public boolean isMutable() {

        return false;
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#nullSafeGet(java.sql.ResultSet,
     * java.lang.String[], java.lang.Object)
     */
    public Object nullSafeGet(ResultSet rs, String[] names, Object owner) throws HibernateException,
            SQLException {

        String value = rs.getString(names[0]);
        if (value == null) {
            return null;
        } else {
            return UUID.fromString(value);
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.hibernate.usertype.UserType#nullSafeSet(java.sql.PreparedStatement,
     * java.lang.Object, int)
     */
    public void nullSafeSet(PreparedStatement st, Object value, int index)
            throws HibernateException, SQLException {

        if (value == null) {
            st.setNull(index, Types.VARCHAR);
            return;
        }

        if (!UUID.class.isAssignableFrom(value.getClass())) {
            throw new HibernateException(value.getClass().toString() + CAST_EXCEPTION_TEXT);
        }

        st.setString(index, value.toString());
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#replace(java.lang.Object,
     * java.lang.Object, java.lang.Object)
     */
    public Object replace(Object original, Object target, Object owner) throws HibernateException {

        if (!UUID.class.isAssignableFrom(original.getClass())) {
            throw new HibernateException(original.getClass().toString() + CAST_EXCEPTION_TEXT);
        }

        return UUID.fromString(original.toString());
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#returnedClass()
     */
    @SuppressWarnings("unchecked")
    public Class returnedClass() {

        return UUID.class;
    }

    /*
     * (non-Javadoc)
     * @see org.hibernate.usertype.UserType#sqlTypes()
     */
    public int[] sqlTypes() {

        return new int[]{Types.CHAR};
    }
}
