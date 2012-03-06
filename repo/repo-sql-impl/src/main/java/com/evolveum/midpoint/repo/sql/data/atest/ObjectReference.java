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

package com.evolveum.midpoint.repo.sql.data.atest;

import javax.persistence.*;
import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/6/12
 * Time: 10:57 AM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class ObjectReference implements Serializable {

    private O owner;
    private Reference reference;
    private O target;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    public O getTarget() {
        return target;
    }

    @Id
    @MapsId("oid")
    @ManyToOne(fetch = FetchType.LAZY)
    public O getOwner() {
        return owner;
    }

    @Embedded
    public Reference getReference() {
        if (reference == null) {
            reference = new Reference();
        }
        return reference;
    }

    public void setOwner(O owner) {
        this.owner = owner;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    public void setTarget(O target) {
        this.target = target;
    }

    @Transient
    public QName getType() {
        return getReference().getType();
    }

    @Transient
    public void setType(QName type) {
        getReference().setType(type);
    }
}
