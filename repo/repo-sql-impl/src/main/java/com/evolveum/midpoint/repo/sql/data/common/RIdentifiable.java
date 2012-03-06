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

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

/**
 * @author lazyman
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class RIdentifiable {

    private RObjectType owner;
    private Long containerId;

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.ContainerIdGenerator")
    @Column(nullable = true, updatable = true)
    public Long getContainerId() {
        return containerId;
    }

    @Id
    @MapsId("oid")
    @ManyToOne
    public RObjectType getOwner() {
        return owner;
    }

    public void setContainerId(Long containerId) {
        this.containerId = containerId;
    }

    public void setOwner(RObjectType owner) {
        this.owner = owner;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = hashCode(result, owner);
        result = hashCode(result, containerId);

        return result;
    }

    private int hashCode(int subResult, Object object) {
        final int prime = 31;

        return prime * subResult + (object == null ? 0 : object.hashCode());
    }
}
