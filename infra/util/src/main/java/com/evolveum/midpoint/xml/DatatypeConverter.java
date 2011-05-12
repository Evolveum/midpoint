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

package com.evolveum.midpoint.xml;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Sample Class Doc
 *
 * @author $author$
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class DatatypeConverter {

    public static final String code_id = "$Id$";

    static public Date parseDateTime(String value) {
        return (javax.xml.bind.DatatypeConverter.parseDateTime(value).getTime());
    }

    static public String printDateTime(Date value) {
        if (value == null) {
            return null;
        }
        Calendar cal = new GregorianCalendar();
        cal.setTime(value);
        return (javax.xml.bind.DatatypeConverter.printDateTime(cal));
    }

    static public Date parseDate(String value) {
        return (javax.xml.bind.DatatypeConverter.parseDate(value).getTime());
    }

    static public String printDate(Date value) {
        if (value == null) {
            return null;
        }
        Calendar cal = new GregorianCalendar();
        cal.setTime(value);
        return (javax.xml.bind.DatatypeConverter.printDate(cal));
    }
}
