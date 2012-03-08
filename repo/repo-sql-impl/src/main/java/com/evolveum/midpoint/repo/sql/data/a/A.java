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

package com.evolveum.midpoint.repo.sql.data.a;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/8/12
 * Time: 4:59 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class A implements Serializable {

    private String owner;
    private Long id;

    private R r;

//    private Set<E> eset;

    @MapsId("oid")
    @ManyToOne
    @JoinColumn(name = "owner", referencedColumnName = "oid")
    public R getR() {
        return r;
    }

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.ContainerIdGenerator")
    public Long getId() {
        return id;
    }

    @Id
    @Column(name="owner")
    public String getOwner() {
        return owner;
    }

//    @OneToMany(mappedBy = "a")
//    @Cascade({org.hibernate.annotations.CascadeType.ALL})
//    public Set<E> getEset() {
//        return eset;
//    }

    public void setR(R r) {
        this.r = r;
    }

//    public void setEset(Set<E> eset) {
//        this.eset = eset;
//    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOwner(String oid) {
        this.owner = oid;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, ToStringStyle.SIMPLE_STYLE);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        if (!(o instanceof A)) {
            return false;
        }

        A a = (A) o;
        return equals(owner, a.getOwner())
                && id != null && equals(id, a.getId());
    }

    private boolean equals(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }
}
