/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import static com.evolveum.midpoint.repo.sql.data.audit.RAuditItem.COLUMN_RECORD_ID;

import java.util.Objects;
import javax.persistence.*;

import org.hibernate.annotations.ForeignKey;

import com.evolveum.midpoint.repo.sql.data.InsertQueryBuilder;
import com.evolveum.midpoint.repo.sql.data.SingleSqlQuery;
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

    private static final String CHANGE_ITEM_PATH_COLUMN_NAME = "changedItemPath";

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

    public static RAuditItem toRepo(RAuditEventRecord record, String itemPath) {
        RAuditItem itemChanged = new RAuditItem();
        itemChanged.setRecord(record);
        itemChanged.setChangedItemPath(itemPath);
        return itemChanged;

    }

    public static SingleSqlQuery toRepo(Long recordId, String itemPath) {
        InsertQueryBuilder queryBuilder = new InsertQueryBuilder(TABLE_NAME);
        queryBuilder.addParameter(CHANGE_ITEM_PATH_COLUMN_NAME, itemPath, true);
        queryBuilder.addParameter(COLUMN_RECORD_ID, recordId, true);
        return queryBuilder.build();

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
