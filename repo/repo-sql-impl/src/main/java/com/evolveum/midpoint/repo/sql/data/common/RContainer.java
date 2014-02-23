/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import java.io.Serializable;

/**
 * @author lazyman
 */
@Entity
@IdClass(RContainerId.class)
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class RContainer implements Serializable {

    private String oid;
    private Short id;

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    public Short getId() {
        return id;
    }

    @Id
    @GeneratedValue(generator = "ContainerOidGenerator")
    @GenericGenerator(name = "ContainerOidGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerOidGenerator")
    @Column(name = "oid", nullable = false, updatable = false, length = RUtil.COLUMN_LENGTH_OID)
    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public void setId(Short id) {
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
