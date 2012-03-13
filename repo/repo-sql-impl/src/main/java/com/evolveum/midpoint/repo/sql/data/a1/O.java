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

import org.hibernate.annotations.*;

import javax.persistence.*;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/12/12
 * Time: 6:54 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "object")
@ForeignKey(name = "fk_container")
public abstract class O extends Container {

    private String name;
    private String description;
    private AnyContainer extension;

//    private String extOid;
//    private Long extId;
//    private RContainerType extType;

    @OneToOne(optional = true, mappedBy = "owner")
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
//    @PrimaryKeyJoinColumns({
//            @PrimaryKeyJoinColumn(name = "oid", referencedColumnName = "owner_oid"),
//            @PrimaryKeyJoinColumn(name = "id", referencedColumnName = "owner_id"),
//            @PrimaryKeyJoinColumn(name="extType", referencedColumnName = "ownerType")
//    })
    public AnyContainer getExtension() {
        return extension;
    }

//    @Column(name = "anyId")
//    public Long getExtId() {
//        return extId;
//    }
//
//    @Column(name = "anyOid", length = 36)
//    public String getExtOid() {
//        return extOid;
//    }
//
//    @Column(name = "anyType")
//    @Enumerated(EnumType.ORDINAL)
//    public RContainerType getExtType() {
//        return extType;
//    }
//
//    public void setExtId(Long extId) {
//        this.extId = extId;
//    }
//
//    public void setExtOid(String extOid) {
//        this.extOid = extOid;
//    }
//
//    public void setExtType(RContainerType extType) {
//        this.extType = extType;
//    }

    public String getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setExtension(AnyContainer extension) {
        this.extension = extension;
        if (extension != null) {
            extension.setOwnerType(RContainerType.OBJECT);
        }
    }
}
