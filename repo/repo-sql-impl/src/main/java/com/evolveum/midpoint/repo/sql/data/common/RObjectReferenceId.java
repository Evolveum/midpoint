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

package com.evolveum.midpoint.repo.sql.data.common;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RObjectReferenceId implements Serializable {

    private String ownerOid;
    private Long ownerId;
    private String targetOid;
    private Long targetId;

    public RObjectReferenceId() {
    }

    public RObjectReferenceId(Long ownerId, String ownerOid, Long targetId, String targetOid) {
        this.ownerId = ownerId;
        this.ownerOid = ownerOid;
        this.targetId = targetId;
        this.targetOid = targetOid;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getTargetOid() {
        return targetOid;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RObjectReferenceId that = (RObjectReferenceId) o;

        if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;
        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;
        if (targetId != null ? !targetId.equals(that.targetId) : that.targetId != null) return false;
        if (targetOid != null ? !targetOid.equals(that.targetOid) : that.targetOid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (targetOid != null ? targetOid.hashCode() : 0);
        result = 31 * result + (targetId != null ? targetId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RObjectReferenceId[" + ownerOid + "," + ownerId + "," + targetOid + "," + targetId + ']';
    }
}
