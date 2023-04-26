/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.audit;

import static com.evolveum.midpoint.repo.sql.data.audit.RAuditReferenceValue.COLUMN_RECORD_ID;
import static com.evolveum.midpoint.repo.sql.data.audit.RAuditReferenceValue.TABLE_NAME;

import java.util.Objects;
import jakarta.persistence.*;

import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.EntityState;

@Ignore
@Entity
@Table(name = TABLE_NAME, indexes = {
        @Index(name = "iAuditRefValRecordId", columnList = COLUMN_RECORD_ID) })
public class RAuditReferenceValue implements EntityState {

    public static final String TABLE_NAME = "m_audit_ref_value";
    public static final String COLUMN_RECORD_ID = "record_id";

    public static final String NAME_COLUMN_NAME = "name";
    private static final String OID_COLUMN_NAME = "oid";
    private static final String TARGET_NAME_NORM_COLUMN_NAME = "targetName_norm";
    private static final String TARGET_NAME_ORIG_COLUMN_NAME = "targetName_orig";
    private static final String TYPE_COLUMN_NAME = "type";

    private Boolean trans;

    private long id;
    private RAuditEventRecord record;
    private Long recordId;
    private String name;
    private String oid;
    private String type;
    private RPolyString targetName;

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    //@ForeignKey(name = "none")
    @MapsId("record")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = COLUMN_RECORD_ID, referencedColumnName = "id")
    })
    public RAuditEventRecord getRecord() {
        return record;
    }

    @Column(name = COLUMN_RECORD_ID)
    public Long getRecordId() {
        if (recordId == null && record != null) {
            recordId = record.getId();
        }
        return recordId;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(length = 36)
    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public RPolyString getTargetName() {
        return targetName;
    }

    public void setTargetName(RPolyString targetName) {
        this.targetName = targetName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RAuditReferenceValue)) { return false; }
        RAuditReferenceValue that = (RAuditReferenceValue) o;
        return id == that.id &&
                Objects.equals(recordId, that.recordId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(oid, that.oid) &&
                Objects.equals(type, that.type) &&
                Objects.equals(targetName, that.targetName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, recordId, name, oid);
    }

    @Override
    public String toString() {
        return "RAuditReferenceValue{" +
                "id=" + id +
                ", recordId=" + recordId +
                ", name='" + name + '\'' +
                ", oid='" + oid + '\'' +
                ", type='" + type + '\'' +
                ", targetName='" + targetName + '\'' +
                '}';
    }
}
