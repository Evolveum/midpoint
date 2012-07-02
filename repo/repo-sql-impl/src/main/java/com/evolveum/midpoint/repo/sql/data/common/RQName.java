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

package com.evolveum.midpoint.repo.sql.data.common;

import org.apache.commons.lang.Validate;

import javax.persistence.Embeddable;
import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Embeddable entity which represents {@link QName}, it's almost the same as QNameType, but
 * it has simpler usage when embedding twice in entity (e.g. twice in {@link REmbeddedReference} in
 * {@link RTask}).
 *
 * @author lazyman
 */
@Embeddable
public class RQName implements Serializable {

    private String namespace;
    private String localPart;

    public RQName() {
    }

    public RQName(QName qname) {
        Validate.notNull(qname, "QName must not be null.");
        this.namespace = qname.getNamespaceURI();
        this.localPart = qname.getLocalPart();
    }

    public String getLocalPart() {
        return localPart;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setLocalPart(String localPart) {
        this.localPart = localPart;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RQName rqName = (RQName) o;

        if (localPart != null ? !localPart.equals(rqName.localPart) : rqName.localPart != null) return false;
        if (namespace != null ? !namespace.equals(rqName.namespace) : rqName.namespace != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = namespace != null ? namespace.hashCode() : 0;
        result = 31 * result + (localPart != null ? localPart.hashCode() : 0);
        return result;
    }

    public QName toQName() {
        return new QName(getNamespace(), getLocalPart());
    }
}
