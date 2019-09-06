/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import java.io.Serializable;

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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RObjectDeltaOperationId that = (RObjectDeltaOperationId) o;

        if (checksum != null ? !checksum.equals(that.checksum) : that.checksum != null) return false;
        if (recordId != null ? !recordId.equals(that.recordId) : that.recordId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = recordId != null ? recordId.hashCode() : 0;
        result = 31 * result + (checksum != null ? checksum.hashCode() : 0);
        return result;
    }
}
