/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import java.io.Serializable;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicUpdate;

import com.evolveum.midpoint.repo.sql.data.common.id.RFocusPhotoId;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.RUtil;

@Ignore
@IdClass(RFocusPhotoId.class)
@Entity
@Table(name = "m_focus_photo")
@DynamicUpdate
public class RFocusPhoto implements Serializable, EntityState {

    private Boolean trans;

    private RFocus owner;
    private String ownerOid;

    private byte[] photo;

    @MapsId("ownerOid")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    @JoinColumn(name = "owner_oid", referencedColumnName = "oid", foreignKey = @ForeignKey(name = "fk_focus_photo"))
    public RFocus getOwner() {
        return owner;
    }

    @Id
    @Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    @Lob
    public byte[] getPhoto() {
        return photo;
    }

    @Transient
    public Boolean isTransient() {
        return trans;
    }

    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    public void setOwner(RFocus owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (o == null || getClass() != o.getClass()) {return false;}

        RFocusPhoto photo = (RFocusPhoto) o;

        return getOwnerOid() != null ? getOwnerOid().equals(photo.getOwnerOid()) : photo.getOwnerOid() == null;
    }

    @Override
    public int hashCode() {
        return getOwnerOid() != null ? getOwnerOid().hashCode() : 0;
    }
}
