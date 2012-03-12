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

package com.evolveum.midpoint.repo.sql.data.a1;

import javax.persistence.Embeddable;
import javax.xml.namespace.QName;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/12/12
 * Time: 9:00 PM
 * To change this template use File | Settings | File Templates.
 */
@Embeddable
public class DateValue extends Value {

    private Date value;

    public DateValue() {
    }

    public DateValue(Date value) {
        this(null, null, value);
    }

    public DateValue(QName name, QName type, Date value) {
        setName(name);
        setType(type);
        setValue(value);
    }

    public Date getValue() {
        return value;
    }

    public void setValue(Date value) {
        this.value = value;
    }
}
