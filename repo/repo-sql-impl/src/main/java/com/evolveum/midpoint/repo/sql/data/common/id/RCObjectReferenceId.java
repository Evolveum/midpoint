/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.repo.sql.data.common.id;

import com.evolveum.midpoint.repo.sql.data.common.other.RCReferenceOwner;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RCObjectReferenceId implements Serializable {

    private String ownerOid;
    private Integer ownerId;
    private String targetOid;
    private String relation;
    private RCReferenceOwner referenceType;

    public RCObjectReferenceId() {
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public Integer getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Integer ownerId) {
        this.ownerId = ownerId;
    }

    public String getTargetOid() {
        return targetOid;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public RCReferenceOwner getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(RCReferenceOwner referenceType) {
        this.referenceType = referenceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RCObjectReferenceId that = (RCObjectReferenceId) o;

        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;
        if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;
        if (targetOid != null ? !targetOid.equals(that.targetOid) : that.targetOid != null) return false;
        if (relation != null ? !relation.equals(that.relation) : that.relation != null) return false;
        if (referenceType != null ? !referenceType.equals(that.referenceType) : that.referenceType != null)
            return false;

        return true;
    }


    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (targetOid != null ? targetOid.hashCode() : 0);
        result = 31 * result + (relation != null ? relation.hashCode() : 0);
        result = 31 * result + (referenceType != null ? referenceType.hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        return "RObjectReferenceId[" + ownerOid + "," + ownerId + ","
                + targetOid + "," + relation + "," + referenceType + ']';
    }
}
