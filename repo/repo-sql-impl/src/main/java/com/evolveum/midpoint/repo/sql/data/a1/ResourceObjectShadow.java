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

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Columns;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import javax.xml.namespace.QName;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/12/12
 * Time: 10:07 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "resource_shadow")
@ForeignKey(name = "fk_resource_object_shadow")
public class ResourceObjectShadow extends O {

    private QName objectClass;
    private AnyContainer attributes;

    @Columns(columns = {
            @Column(name = "class_namespace"),
            @Column(name = "class_localPart")
    })
    public QName getObjectClass() {
        return objectClass;
    }

    @OneToOne(optional = true, mappedBy = "owner")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public AnyContainer getAttributes() {
        return attributes;
    }

    public void setAttributes(AnyContainer attributes) {
        this.attributes = attributes;
        if (attributes != null) {
            attributes.setOwnerType(RContainerType.RESOURCE_OBJECT_SHADOW);
        }
    }

    public void setObjectClass(QName objectClass) {
        this.objectClass = objectClass;
    }
}
