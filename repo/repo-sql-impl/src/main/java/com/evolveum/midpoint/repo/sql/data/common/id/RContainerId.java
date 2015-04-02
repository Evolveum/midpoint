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

import java.io.Serializable;

/**
 * @author lazyman
 */
public class RContainerId implements Serializable {

    private String ownerOid;
    private Integer id;

    public RContainerId() {
    }

    public RContainerId(String oid) {
        this(0, oid);
    }

    public RContainerId(Integer id, String oid) {
        this.id = id;
        this.ownerOid = oid;
    }

    public Integer getId() {
        return id;
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setOwnerOid(String oid) {
        this.ownerOid = oid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RContainerId that = (RContainerId) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (ownerOid != null ? !ownerOid.equals(that.ownerOid) : that.ownerOid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ownerOid != null ? ownerOid.hashCode() : 0;
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "RContainerId{" + ownerOid + ", " + id + "}";
    }
}
