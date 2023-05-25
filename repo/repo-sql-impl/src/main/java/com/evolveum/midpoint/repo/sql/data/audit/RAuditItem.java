/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import static com.evolveum.midpoint.repo.sql.data.audit.RAuditItem.COLUMN_RECORD_ID;

import java.util.Objects;
import jakarta.persistence.*;

import org.hibernate.annotations.ForeignKey;

import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.EntityState;

@Ignore
@Entity
@IdClass(RAuditItemId.class)
@Table(name = RAuditItem.TABLE_NAME, indexes = {
        @Index(name = "iChangedItemPath", columnList = "changedItemPath"),
        @Index(name = "iAuditItemRecordId", columnList = COLUMN_RECORD_ID) })
public class RAuditItem implements EntityState {

    public static final String TABLE_NAME = "m_audit_item";
    public static final String COLUMN_RECORD_ID = "record_id";

    private Boolean trans;

    private RAuditEventRecord record;
    private Long recordId;
    private String changedItemPath;

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
    @Column(name = "changedItemPath")
    public String getChangedItemPath() {
        return changedItemPath;
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

    public void setChangedItemPath(String changedItemPath) {
        this.changedItemPath = changedItemPath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }

        RAuditItem that = (RAuditItem) o;

        return Objects.equals(recordId, that.recordId)
                && Objects.equals(changedItemPath, that.changedItemPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId, changedItemPath);
    }
}
