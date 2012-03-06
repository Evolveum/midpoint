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

import org.hibernate.annotations.Cascade;

import javax.persistence.*;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/3/12
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class Assignment extends IdentifiableContainer {

    private String description;
    //reference
    private Reference reference;
    private O target;

    @Embedded
    public Reference getReference() {
        if (reference == null) {
            reference = new Reference();
        }
        return reference;
    }

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    public O getTarget() {
        return target;
    }

    public String getDescription() {
        return description;
    }

    public void setReference(Reference reference) {
        this.reference = reference;
    }

    public void setTarget(O target) {
        this.target = target;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "[" + description + "]";
    }
}
