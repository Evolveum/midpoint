/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author lazyman
 */
public class RObjectDeltaOperationId implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long recordId;
    private String checksum;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RObjectDeltaOperationId)) { return false; }

        RObjectDeltaOperationId that = (RObjectDeltaOperationId) o;
        return Objects.equals(recordId, that.recordId)
                && Objects.equals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId, checksum);
    }
}
