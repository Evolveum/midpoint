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

import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RAClobId implements Serializable {

    private String ownerOid;
    private Short ownerId;
    private RObjectType ownerType;
    private String checksum;
    private String name;
    private String type;

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public Short getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Short ownerId) {
        this.ownerId = ownerId;
    }

    public RObjectType getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(RObjectType ownerType) {
        this.ownerType = ownerType;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAClobId raClobId = (RAClobId) o;

        if (checksum != null ? !checksum.equals(raClobId.checksum) : raClobId.checksum != null) return false;
        if (name != null ? !name.equals(raClobId.name) : raClobId.name != null) return false;
        if (ownerId != null ? !ownerId.equals(raClobId.ownerId) : raClobId.ownerId != null) return false;
        if (ownerOid != null ? !ownerOid.equals(raClobId.ownerOid) : raClobId.ownerOid != null) return false;
        if (ownerType != raClobId.ownerType) return false;
        if (type != null ? !type.equals(raClobId.type) : raClobId.type != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (ownerId != null ? ownerId.hashCode() : 0);
        result = 31 * result + (ownerType != null ? ownerType.hashCode() : 0);
        result = 31 * result + (checksum != null ? checksum.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RAClobId{" +
                "ownerOid='" + ownerOid + '\'' +
                ", ownerId=" + ownerId +
                ", ownerType=" + ownerType +
                ", checksum='" + checksum + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
