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
 * @author mederly
 */
public class RCaseWorkItemReferenceId implements Serializable {

    private String ownerOwnerOid;
    private Integer ownerId;
    private String targetOid;
    private String relation;

    public RCaseWorkItemReferenceId() {
    }

    public String getOwnerOwnerOid() {
        return ownerOwnerOid;
    }

    public void setOwnerOwnerOid(String ownerOwnerOid) {
        this.ownerOwnerOid = ownerOwnerOid;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RCaseWorkItemReferenceId))
            return false;
        RCaseWorkItemReferenceId that = (RCaseWorkItemReferenceId) o;
        return Objects.equals(ownerOwnerOid, that.ownerOwnerOid) &&
                Objects.equals(ownerId, that.ownerId) &&
                Objects.equals(targetOid, that.targetOid) &&
                Objects.equals(relation, that.relation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerOwnerOid, ownerId, targetOid, relation);
    }

    @Override
    public String toString() {
        return "RCertWorkItemReferenceId{" +
                "ownerOwnerOid=" + ownerOwnerOid +
                ", ownerId=" + ownerId +
                ", targetOid='" + targetOid + '\'' +
                ", relation='" + relation + '\'' +
                '}';
    }
}
