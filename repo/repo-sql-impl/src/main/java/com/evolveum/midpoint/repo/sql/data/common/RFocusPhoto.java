/*
 * Copyright (c) 2010-2014 Evolveum
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

import com.evolveum.midpoint.repo.sql.data.common.id.RFocusPhotoId;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Arrays;

/**
 * @author lazyman
 */
@Ignore
@IdClass(RFocusPhotoId.class)
@Entity
@Table(name = "m_focus_photo")
public class RFocusPhoto implements Serializable, EntityState {

    private Boolean trans;

    private RFocus owner;
    private String ownerOid;

    private byte[] photo;

    @Id
    @ForeignKey(name = "fk_focus_photo")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RFocusPhoto photo = (RFocusPhoto) o;

        return getOwnerOid() != null ? getOwnerOid().equals(photo.getOwnerOid()) : photo.getOwnerOid() == null;
    }

    @Override
    public int hashCode() {
        return getOwnerOid() != null ? getOwnerOid().hashCode() : 0;
    }
}
