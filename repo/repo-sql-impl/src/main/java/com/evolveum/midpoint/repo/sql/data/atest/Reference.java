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

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.*;
import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/3/12
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class Reference implements Serializable {

    private O owner;
//    private Long id;
    private Assignment assignment;
    private QName type;
    private O target;
    
//    private String owner_container_id;
//    private String container_id;
//
//    @Column(name = "containerOwnerOid", insertable = false, updatable = false)
//    public String getContainer_id() {
//        return container_id;
//    }
//
//    @Column(name = "containerId", insertable = false, updatable = false)
//    public String getOwner_container_id() {
//        return owner_container_id;
//    }

    @Id
    @MapsId("oid")
    @ManyToOne
    public O getOwner() {
        return owner;
    }

//        @MapsId(value = "assignment.id")
////    @Index(name = "iId")
//    @Column(name = "id", insertable = false, updatable = false)
//    public Long getId() {
//        return id;
//    }

    @OneToOne(optional = true, fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "owner_oid"),
            @PrimaryKeyJoinColumn(name = "id")}
    )
//    @JoinColumns({
//            @JoinColumn(name = "containerOwnerOid"),
//            @JoinColumn(name = "containerId")
//            })
    public Assignment getAssignment() {
        return assignment;
    }

    @Id
    @ManyToOne
    public O getTarget() {
        return target;
    }

    @Columns(columns = {
            @Column(name = "namespaceURI"),
            @Column(name = "localPart")
    })
    public QName getType() {
        return type;
    }

    public void setTarget(O target) {
        this.target = target;
    }

    public void setType(QName type) {
        this.type = type;
    }

    public void setOwner(O owner) {
        this.owner = owner;
    }

    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }

//    public void setId(Long id) {
//        this.id = id;
//    }


//    public void setContainer_id(String container_id) {
//        this.container_id = container_id;
//    }
//
//    public void setOwner_container_id(String owner_container_id) {
//        this.owner_container_id = owner_container_id;
//    }
}
