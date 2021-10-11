/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import javax.persistence.*;

import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.repo.sql.data.InsertQueryBuilder;
import com.evolveum.midpoint.repo.sql.data.SingleSqlQuery;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.RUtil;

import org.hibernate.annotations.ForeignKey;

import static com.evolveum.midpoint.repo.sql.data.audit.RTargetResourceOid.COLUMN_RECORD_ID;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Ignore
@Entity
@IdClass(RTargetResourceOidId.class)
@Table(name = RTargetResourceOid.TABLE_NAME, indexes = {
        @Index(name = "iAuditResourceOid", columnList = "resourceOid"),
        @Index(name = "iAuditResourceOidRecordId", columnList = COLUMN_RECORD_ID)})
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

    public void setresourceOid(String resourceOid) {
        this.resourceOid = resourceOid;
    }

    public static RTargetResourceOid toRepo(RAuditEventRecord record, String resourceOid) {
        RTargetResourceOid resourceOidObject = new RTargetResourceOid();
        resourceOidObject.setRecord(record);
        resourceOidObject.setresourceOid(resourceOid);
        return resourceOidObject;

    }

    public static SingleSqlQuery toRepo(Long recordId, String resourceOid) {
        InsertQueryBuilder queryBuilder = new InsertQueryBuilder(TABLE_NAME);
        queryBuilder.addParameter(COLUMN_RECORD_ID, recordId, true);
        queryBuilder.addParameter(RESOURCE_OID_COLUMN_NAME, resourceOid, true);
        return queryBuilder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RTargetResourceOid that = (RTargetResourceOid) o;

        if (resourceOid != null ? !resourceOid.equals(that.resourceOid) : that.resourceOid != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = resourceOid != null ? resourceOid.hashCode() : 0;
        return result;
    }

}
