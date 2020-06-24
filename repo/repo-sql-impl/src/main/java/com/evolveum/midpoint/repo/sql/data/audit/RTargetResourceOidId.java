/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import java.io.Serializable;
import java.util.Objects;

public class RTargetResourceOidId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long recordId;
    private String resourceOid;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getResourceOid() {
        return resourceOid;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RTargetResourceOidId)) { return false; }

        RTargetResourceOidId that = (RTargetResourceOidId) o;
        return Objects.equals(recordId, that.recordId)
                && Objects.equals(resourceOid, that.resourceOid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId, resourceOid);
    }
}
