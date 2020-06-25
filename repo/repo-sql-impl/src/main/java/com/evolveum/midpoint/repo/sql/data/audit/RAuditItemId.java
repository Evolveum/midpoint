/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import java.io.Serializable;
import java.util.Objects;

public class RAuditItemId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long recordId;
    private String changedItemPath;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getChangedItemPath() {
        return changedItemPath;
    }

    public void setChangedItemPath(String changedItemPath) {
        this.changedItemPath = changedItemPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        RAuditItemId that = (RAuditItemId) o;
        return Objects.equals(recordId, that.recordId)
                && Objects.equals(changedItemPath, that.changedItemPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId, changedItemPath);
    }
}
