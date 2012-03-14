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

import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/12/12
 * Time: 6:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "container")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Container implements Serializable {

    private String oid;
    private Long id;
    private long version;

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.ContainerIdGenerator")
    @Column(name = "id")
    public Long getId() {
        return id;
    }

    @Id
    @GeneratedValue(generator = "OidGenerator")
    @GenericGenerator(name = "OidGenerator", strategy = "com.evolveum.midpoint.repo.sql.OidGenerator")
    @Column(name="oid", nullable = false, updatable = false, length = 36)
    public String getOid() {
        return oid;
    }

    @Version
    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
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

        Container container = (Container) o;

//        if (id != null ? !id.equals(container.id) : container.id != null) return false;
//        if (oid != null ? !oid.equals(container.oid) : container.oid != null) return false;
//
//        return true;
        return false;
    }

    @Override
    public int hashCode() {
//        int result = oid != null ? oid.hashCode() : 0;
//        result = 31 * result + (id != null ? id.hashCode() : 0);
//        return result;
        return 31;
    }
}
