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

package com.evolveum.midpoint.repo.sql.data.a0;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/3/12
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class Assignment implements Serializable {

    private String ownerOid;

    private O owner;
    private Long id;

    @Id
    @Column(name = "owner_oid", length = 36, insertable = true, updatable = false, unique = false)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @MapsId("oid")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn(name = "owner_oid", referencedColumnName = "oid")
    public O getOwner() {
        return owner;
    }

    @Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.ContainerIdGenerator")
    @Column(name = "id")
    public Long getId() {
        return id;
    }

    public void setOwner(O owner) {
        this.owner = owner;
    }

    public void setId(Long id) {
        this.id = id;
    }


    private String description;
    //reference
    private Reference reference;
    private O target;
    //extension
    private Set<ExtensionValue> extensions;

    @OneToMany(mappedBy = "object")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<ExtensionValue> getExtensions() {
        return extensions;
    }

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

    public void setExtensions(Set<ExtensionValue> extensions) {
        this.extensions = extensions;
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
