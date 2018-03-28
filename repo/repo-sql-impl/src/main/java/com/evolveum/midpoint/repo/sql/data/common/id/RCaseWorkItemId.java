/*
 * Copyright (c) 2010-2017 Evolveum
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
import java.util.Objects;

/**
 * @author lazyman
 */
public class RCaseWorkItemId implements Serializable {

    private String ownerOid;         // case OID
    private Integer id;

    public RCaseWorkItemId() {
    }

    public RCaseWorkItemId(String ownerOid, Integer id) {
        this.ownerOid = ownerOid;
        this.id = id;
    }

    public String getOwnerOid() {
        return ownerOid;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RCaseWorkItemId))
            return false;
        RCaseWorkItemId that = (RCaseWorkItemId) o;
        return Objects.equals(ownerOid, that.ownerOid) &&
                Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerOid, id);
    }

    @Override
    public String toString() {
        return "RCertWorkItemId{" +
                ", ownerOid=" + ownerOid +
                ", id=" + id +
                '}';
    }
}
