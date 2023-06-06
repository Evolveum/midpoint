/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.audit;

import static com.evolveum.midpoint.repo.sql.data.audit.RTargetResourceOid.COLUMN_RECORD_ID;

import java.util.Objects;
import jakarta.persistence.*;

import org.hibernate.annotations.ForeignKey;

import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.EntityState;

@Ignore
@Entity
@IdClass(RTargetResourceOidId.class)
@Table(name = RTargetResourceOid.TABLE_NAME, indexes = {
        @Index(name = "iAuditResourceOid", columnList = "resourceOid"),
        @Index(name = "iAuditResourceOidRecordId", columnList = COLUMN_RECORD_ID) })
public class RTargetResourceOid implements EntityState {

    public static final String TABLE_NAME = "m_audit_resource";
    public static final String COLUMN_RECORD_ID = "record_id";

    public static final String RESOURCE_OID_COLUMN_NAME = "resourceOid";

    private Boolean trans;

    private RAuditEventRecord record;
    private Long recordId;
    private String resourceOid;

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    @ForeignKey(name = "none")
    @MapsId("record")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = COLUMN_RECORD_ID, referencedColumnName = "id")
    })
    public RAuditEventRecord getRecord() {
        return record;
    }

    @Id
    @Column(name = COLUMN_RECORD_ID)
    public Long getRecordId() {
        if (recordId == null && record != null) {
            recordId = record.getId();
        }
        return recordId;
    }

    @Id
    @Column(name = "resourceOid")
    public String getResourceOid() {
        return resourceOid;
    }

    public void setRecord(RAuditEventRecord record) {
        if (record.getId() != 0) {
            this.recordId = record.getId();
        }
        this.record = record;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RTargetResourceOid)) { return false; }

        RTargetResourceOid that = (RTargetResourceOid) o;
        return Objects.equals(recordId, that.recordId)
                && Objects.equals(resourceOid, that.resourceOid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId, resourceOid);
    }
}
