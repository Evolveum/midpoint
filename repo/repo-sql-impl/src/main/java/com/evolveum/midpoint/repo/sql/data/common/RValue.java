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

import org.hibernate.annotations.Columns;

import javax.persistence.*;
import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
@MappedSuperclass
public abstract class RValue<T> {

    private boolean dynamic;
    private QName name;
    private QName type;
    private RValueType valueType;

    @Columns(columns = {
            @Column(name = "name_namespace"),
            @Column(name = "name_localPart")
    })
    public QName getName() {
        return name;
    }

    @Columns(columns = {
            @Column(name = "type_namespace"),
            @Column(name = "type_localPart")
    })
    public QName getType() {
        return type;
    }

    @Enumerated(EnumType.ORDINAL)
    public RValueType getValueType() {
        return valueType;
    }

    public void setValueType(RValueType valueType) {
        this.valueType = valueType;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public void setType(QName type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RValue rValue = (RValue) o;

        if (dynamic != rValue.dynamic) return false;
        if (name != null ? !name.equals(rValue.name) : rValue.name != null) return false;
        if (type != null ? !type.equals(rValue.type) : rValue.type != null) return false;
        if (valueType != rValue.valueType) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (dynamic ? 1 : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (valueType != null ? valueType.hashCode() : 0);
        return result;
    }

    @Transient
    public abstract T getValue();

    /**
     * @return true if this property has dynamic definition
     */
    @Column(name = "dynamicDef")
    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean dynamic) {
        this.dynamic = dynamic;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append("[n=");
        builder.append(name);
        builder.append(",t=");
        builder.append(type);
        builder.append(",d=");
        builder.append(dynamic);
        builder.append(",type=");
        builder.append(valueType);
        builder.append("]");

        return builder.toString();
    }
}
