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

import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Entity
@IdClass(RContainerId.class)
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class RContainer implements Serializable {

    private String oid;
    private Long id;

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    public Long getId() {
        return id;
    }

    @Id
    @GeneratedValue(generator = "ContainerOidGenerator")
    @GenericGenerator(name = "ContainerOidGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerOidGenerator")
    @Column(name = "oid", nullable = false, updatable = false, length = 36)
    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RContainer container = (RContainer) o;

        if (id != null ? !id.equals(container.id) : container.id != null) return false;
        if (oid != null ? !oid.equals(container.oid) : container.oid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return "RContainer{" + oid + ", " + id + "}";
    }
}
