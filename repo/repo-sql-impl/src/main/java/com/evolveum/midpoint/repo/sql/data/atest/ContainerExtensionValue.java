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

import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/7/12
 * Time: 2:11 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
//@IdClass(TestId.class)
public class ContainerExtensionValue implements Serializable {

//    private O owner;
    private Assignment assignment;

    private Long id;
    private String ownerOid;

    @Id
    @Column(name = "id", insertable = true, updatable = false)
    public Long getId() {
        return id;
    }

    @Id
    @Column(name = "owner", insertable = true, updatable = false)
    public String getOwnerOid() {
        return ownerOid;
    }

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "owner_oid")
//    public O getOwner() {
//        return owner;
//    }

//    @Id
    @MapsId
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "id", insertable = false, updatable = false, nullable = false),
            @JoinColumn(name = "owner", insertable = false, updatable = false, nullable = false)
    })
    public Assignment getAssignment() {
        return assignment;
    }

    public void setId(Long id) {
        this.id = id;
    }

//    public void setOwner(O owner) {
//        this.owner = owner;
//    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }
}
