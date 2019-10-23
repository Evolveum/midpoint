/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import java.io.Serializable;

public class RTargetResourceOidId implements Serializable{

    private static final long serialVersionUID = 1L;
    private Long recordId;
    private String resourceOid;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getresourceOid() {
        return resourceOid;
    }

    public void setresourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RTargetResourceOidId that = (RTargetResourceOidId) o;

        if (recordId != null ? !recordId.equals(that.recordId) : that.recordId != null) return false;
        if (resourceOid != null ? !resourceOid.equals(that.resourceOid) : that.resourceOid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = recordId != null ? recordId.hashCode() : 0;
        result = 31 * result + (resourceOid != null ? resourceOid.hashCode() : 0);
        return result;
    }

}
