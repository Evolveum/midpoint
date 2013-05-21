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

import com.evolveum.midpoint.repo.sql.type.QNameType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RObjectReferenceId implements Serializable {

    private String ownerOid;
    private Long ownerId;
    private String targetOid;
    private String relationNamespace;
    private String relationLocalPart;

    public RObjectReferenceId() {
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

    public String getTargetOid() {
        return targetOid;
    }

    public void setTargetOid(String targetOid) {
        this.targetOid = targetOid;
    }

    public String getRelationLocalPart() {
        if (relationLocalPart == null) {
            relationLocalPart = QNameType.EMPTY_QNAME_COLUMN_VALUE;
        }
        return relationLocalPart;
    }

    public void setRelationLocalPart(String relationLocalPart) {
        this.relationLocalPart = relationLocalPart;
    }

    public String getRelationNamespace() {
        if (relationNamespace == null) {
            relationNamespace = QNameType.EMPTY_QNAME_COLUMN_VALUE;
        }
        return relationNamespace;
    }

    public void setRelationNamespace(String relationNamespace) {
        this.relationNamespace = relationNamespace;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RObjectReferenceId that = (RObjectReferenceId) o;

        if (ownerId != null ? !ownerId.equals(that.ownerId) : that.ownerId != null) return false;
        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;
        if (targetOid != null ? !targetOid.equals(that.targetOid) : that.targetOid != null) return false;
        if (getRelationNamespace() != null ? !getRelationNamespace().equals(that.getRelationNamespace()) :
                that.getRelationNamespace() != null) return false;
        if (getRelationLocalPart() != null ? !getRelationLocalPart().equals(that.getRelationLocalPart()) :
                that.getRelationLocalPart() != null) return false;

        return true;
    }


    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (targetOid != null ? targetOid.hashCode() : 0);
        result = 31 * result + (getRelationNamespace() != null ? getRelationNamespace().hashCode() : 0);
        result = 31 * result + (getRelationLocalPart() != null ? getRelationLocalPart().hashCode() : 0);

        return result;
    }

    @Override
    public String toString() {
        return "RObjectReferenceId[" + ownerOid + "," + ownerId + "," + targetOid + ","
                + relationNamespace + "," + relationLocalPart + ']';
    }
}
