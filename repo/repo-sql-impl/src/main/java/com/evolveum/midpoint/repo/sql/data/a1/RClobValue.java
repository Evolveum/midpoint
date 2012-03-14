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

import org.hibernate.annotations.Type;

import javax.persistence.Embeddable;
import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
@Embeddable
public class RClobValue extends Value {

    private String value;

    public RClobValue() {
    }

    public RClobValue(String value) {
        this(null, null, value);
    }

    public RClobValue(QName name, QName type, String value) {
        setName(name);
        setType(type);
        setValue(value);
    }

    @Type(type = "org.hibernate.type.MaterializedClobType")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
