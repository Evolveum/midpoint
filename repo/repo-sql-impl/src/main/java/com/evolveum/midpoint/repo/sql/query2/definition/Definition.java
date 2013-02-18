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
 * Portions Copyrighted 2013 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql.query2.definition;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class Definition {

    //jaxb
    private QName jaxbName;
    private QName jaxbType;
    //jpa
    private String jpaName;
    private Class jpaType;

    public Definition(QName jaxbName, QName jaxbType, String jpaName, Class jpaType) {
        this.jaxbName = jaxbName;
        this.jaxbType = jaxbType;
        this.jpaName = jpaName;
        this.jpaType = jpaType;
    }

    public QName getJaxbName() {
        return jaxbName;
    }

    public QName getJaxbType() {
        return jaxbType;
    }

    public String getJpaName() {
        return jpaName;
    }

    public Class getJpaType() {
        return jpaType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append('[');
        builder.append("jaxbName=").append(jaxbName);
        builder.append(", jaxbType=").append(jaxbType);
        builder.append(", jpaName=").append(jpaName);
        builder.append(", jpaType=").append((jpaType != null ? jpaType.getName() : ""));
        builder.append(']');

        return builder.toString();
    }
}
