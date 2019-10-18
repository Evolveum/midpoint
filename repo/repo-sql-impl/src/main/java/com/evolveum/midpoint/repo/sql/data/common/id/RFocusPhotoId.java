/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.common.id;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RFocusPhotoId implements Serializable {

    private String ownerOid;

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RFocusPhotoId that = (RFocusPhotoId) o;

        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return ownerOid != null ? ownerOid.hashCode() : 0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RFocusPhotoId{");
        sb.append("ownerOid='").append(ownerOid).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
