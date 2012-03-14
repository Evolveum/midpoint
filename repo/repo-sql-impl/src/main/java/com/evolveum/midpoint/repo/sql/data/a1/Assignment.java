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

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;

/**
 * Created by IntelliJ IDEA.
 * User: lazyman
 * Date: 3/12/12
 * Time: 6:55 PM
 * To change this template use File | Settings | File Templates.
 */
@Entity
@Table(name = "assignment")
@ForeignKey(name = "fk_assignment")
public class Assignment extends Container implements Ownable {

    //owner
    private O owner;
    private String ownerOid;
    private Long ownerId;
    //extension
    private AnyContainer extension;
    //assignment fields
    private String accountConstruction;
    private Reference targetRef;

    @ForeignKey(name = "fk_assignment_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "oid", referencedColumnName = "oid"),
            @JoinColumn(name = "owner_id", referencedColumnName = "id")
    })
    public O getOwner() {
        return owner;
    }

    @OneToOne(optional = true, mappedBy = "owner")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Reference getTargetRef() {
        return targetRef;
    }

    @Column(name = "owner_id", nullable = false)
    public Long getOwnerId() {
        if (ownerId == null && owner != null) {
            ownerId = owner.getId();
        }
        return ownerId;
    }

    @Column(name = "owner_oid", length = 36, nullable = false)
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @ManyToOne(optional = true)
    @ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    @PrimaryKeyJoinColumns({
            @PrimaryKeyJoinColumn(name = "extOid", referencedColumnName = "owner_oid"),
            @PrimaryKeyJoinColumn(name = "extId", referencedColumnName = "owner_id"),
            @PrimaryKeyJoinColumn(name = "extType", referencedColumnName = "ownerType")
    })
    public AnyContainer getExtension() {
        return extension;
    }

    public String getAccountConstruction() {
        return accountConstruction;
    }

    public void setExtension(AnyContainer extension) {
        this.extension = extension;
        if (extension != null) {
            extension.setOwnerType(RContainerType.ASSIGNMENT);
        }
    }

    public void setAccountConstruction(String accountConstruction) {
        this.accountConstruction = accountConstruction;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setTargetRef(Reference targetRef) {
        this.targetRef = targetRef;
    }

    public void setOwner(O owner) {
        this.owner = owner;
    }

    @Transient
    @Override
    public Container getContainerOwner() {
        return getOwner();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Assignment that = (Assignment) o;

        if (accountConstruction != null ? !accountConstruction.equals(that.accountConstruction) : that.accountConstruction != null)
            return false;
//        if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;
//        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;
        if (targetRef != null ? !targetRef.equals(that.targetRef) : that.targetRef != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (targetRef != null ? targetRef.hashCode() : 0);
//        result = 31 * result + (ownerOid != null ? ownerOid.hashCode() : 0);
//        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (accountConstruction != null ? accountConstruction.hashCode() : 0);
        return result;
    }
}
