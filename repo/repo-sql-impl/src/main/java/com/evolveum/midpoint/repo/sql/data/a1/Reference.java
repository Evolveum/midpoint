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

import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/12/12
 * Time: 7:07 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
public class Reference implements Serializable {

    private Container owner;
    private Container target;

    private String ownerOid;
    private Long ownerId;
    private String targetOid;
    private Long targetId;

    @ForeignKey(name = "fk_reference_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "owner_oid", referencedColumnName = "ownerOid"),
            @PrimaryKeyJoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public Container getOwner() {
        return owner;
    }

    @ForeignKey(name = "fk_reference_target")
    @MapsId("target")
    @ManyToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "target_oid", referencedColumnName = "ownerOid"),
            @PrimaryKeyJoinColumn(name = "target_id", referencedColumnName = "id")
    })
    public Container getTarget() {
        return target;
    }

    @Id
    @Column(name = "owner_id")
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Id
    @Column(name = "owner_oid", length = 36)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Id
    @Column(name = "target_id")
    public Long getTargetId() {
        if (targetId == null && target != null) {
            targetId = target.getId();
        }
        return targetId;
    }

    @Id
    @Column(name = "target_oid", length = 36)
    public String getTargetOid() {
        if (targetOid == null && target != null) {
            targetOid = target.getOid();
        }
        return targetOid;
    }

    public void setTarget(Container target) {
        this.target = target;
    }

    public void setOwner(Container owner) {
        this.owner = owner;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    //todo hash and equals
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Reference reference = (Reference) o;

//        if (ownerId != null ? !ownerId.equals(reference.ownerId) : reference.ownerId != null) return false;
//        if (ownerOid != null ? !ownerOid.equals(reference.ownerOid) : reference.ownerOid != null) return false;
//        if (targetId != null ? !targetId.equals(reference.targetId) : reference.targetId != null) return false;
//        if (targetOid != null ? !targetOid.equals(reference.targetOid) : reference.targetOid != null) return false;
//
//        return true;
        return false;
    }

    @Override
    public int hashCode() {
//        int result = ownerOid != null ? ownerOid.hashCode() : 0;
//        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
//        result = 31 * result + (targetOid != null ? targetOid.hashCode() : 0);
//        result = 31 * result + (targetId != null ? targetId.hashCode() : 0);
//        return result;
        return 31;
    }
}
