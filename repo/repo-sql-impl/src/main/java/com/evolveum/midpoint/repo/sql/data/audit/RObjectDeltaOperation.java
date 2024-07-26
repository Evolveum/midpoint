/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.audit;

import static com.evolveum.midpoint.repo.sql.data.audit.RObjectDeltaOperation.COLUMN_RECORD_ID;

import java.util.Objects;
import jakarta.persistence.*;

import org.hibernate.annotations.ForeignKey;

import com.evolveum.midpoint.repo.sql.data.common.ROperationResultFull;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RChangeType;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.RUtil;

import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcType;

/**
 * @author lazyman
 */
@Ignore
@Entity
@IdClass(RObjectDeltaOperationId.class)
@Table(name = RObjectDeltaOperation.TABLE_NAME, indexes = {
        @Index(name = "iAuditDeltaRecordId", columnList = COLUMN_RECORD_ID) })
public class RObjectDeltaOperation implements ROperationResultFull, EntityState {

    private static final long serialVersionUID = -1065600513263271161L;

    public static final String TABLE_NAME = "m_audit_delta";
    public static final String COLUMN_RECORD_ID = "record_id";

    private static final String CHECKSUM_COLUMN_NAME = "checksum";
    private static final String DELTA_COLUMN_NAME = "delta";
    private static final String DELTA_OID_COLUMN_NAME = "deltaOid";
    private static final String DELTA_TYPE_COLUMN_NAME = "deltaType";
    private static final String FULL_RESULT_COLUMN_NAME = "fullResult";
    private static final String OBJECT_NAME_NORM_COLUMN_NAME = "objectName_norm";
    private static final String OBJECT_NAME_ORIG_COLUMN_NAME = "objectName_orig";
    private static final String RESOURCE_NAME_NORM_COLUMN_NAME = "resourceName_norm";
    private static final String RESOURCE_NAME_ORIG_COLUMN_NAME = "resourceName_orig";
    private static final String RESOURCE_OID_COLUMN_NAME = "resourceOid";
    private static final String STATUS_COLUMN_NAME = "status";

    private Boolean trans;

    private RAuditEventRecord record;
    private Long recordId;

    //delta
    private byte[] delta;
    private String checksum;
    private String deltaOid;
    private RChangeType deltaType;

    //operation result
    private ROperationResultStatus status;
    private byte[] fullResult;

    // additional info from ObjectDeltaOperationType
    private RPolyString objectName;
    private String resourceOid;
    private RPolyString resourceName;

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

    /**
     * This method is used for content comparing when querying database (we don't want to compare clob values).
     *
     * @return md5 hash of {@link RObjectDeltaOperation#delta} and {@link RObjectDeltaOperation#fullResult}
     */
    @Id
    @Column(length = 32, name = "checksum")
    public String getChecksum() {
        if (checksum == null) {
            recomputeChecksum();
        }

        return checksum;
    }

    @Lob
    public byte[] getDelta() {
        return delta;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public RChangeType getDeltaType() {
        return deltaType;
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    public String getDeltaOid() {
        return deltaOid;
    }

    @Lob
    public byte[] getFullResult() {
        return fullResult;
    }

    @JdbcType(IntegerJdbcType.class)
    @Enumerated(EnumType.ORDINAL)
    public ROperationResultStatus getStatus() {
        return status;
    }

    @Embedded
    public RPolyString getObjectName() {
        return objectName;
    }

    @Column(length = RUtil.COLUMN_LENGTH_OID)
    public String getResourceOid() {
        return resourceOid;
    }

    @Embedded
    public RPolyString getResourceName() {
        return resourceName;
    }

    @Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
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

    public void setChecksum(@SuppressWarnings("unused") String checksum) {
        //checksum is always computed from delta and result, this setter is only to satisfy hibernate
    }

    public void setDelta(byte[] delta) {
        this.delta = delta;

        recomputeChecksum();
    }

    public void setStatus(ROperationResultStatus status) {
        this.status = status;
    }

    public void setFullResult(byte[] fullResult) {
        this.fullResult = fullResult;

        recomputeChecksum();
    }

    public void setDeltaType(RChangeType deltaType) {
        this.deltaType = deltaType;
    }

    public void setDeltaOid(String deltaOid) {
        this.deltaOid = deltaOid;
    }

    public void setObjectName(RPolyString objectName) {
        this.objectName = objectName;
    }

    public void setResourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public void setResourceName(RPolyString resourceName) {
        this.resourceName = resourceName;
    }

    @Transient
    private void recomputeChecksum() {
        checksum = RUtil.computeChecksum(delta, fullResult);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RObjectDeltaOperation)) { return false; }

        RObjectDeltaOperation that = (RObjectDeltaOperation) o;
        return Objects.equals(recordId, that.recordId)
                && Objects.equals(checksum, that.checksum);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recordId, checksum);
    }
}
