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

import com.evolveum.midpoint.repo.sql.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.Identifiable;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.hibernate.annotations.Columns;
import org.w3c.dom.Element;

import javax.persistence.*;
import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class RValue implements Identifiable {

    private long id;
    private QName name;
    private QName type;

    @Id
    @GeneratedValue
    @Override
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Columns(columns = {
            @Column(name = "nameNamespaceURI"),
            @Column(name = "nameLocalPart")
    })
    public QName getType() {
        return type;
    }

    @Columns(columns = {
            @Column(name = "typeNamespaceURI"),
            @Column(name = "typeLocalPart")
    })
    public QName getName() {
        return name;
    }

    public void setType(QName type) {
        this.type = type;
    }

    public void setName(QName name) {
        this.name = name;
    }

    public abstract Object toObject() throws DtoTranslationException;

    public abstract void insertValueFromElement(Element element) throws SchemaException;
}