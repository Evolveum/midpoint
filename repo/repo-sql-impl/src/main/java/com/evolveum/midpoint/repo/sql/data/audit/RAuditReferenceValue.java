/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.audit;

import static com.evolveum.midpoint.repo.sql.data.audit.RAuditReferenceValue.COLUMN_RECORD_ID;
import static com.evolveum.midpoint.repo.sql.data.audit.RAuditReferenceValue.TABLE_NAME;
import static com.evolveum.midpoint.repo.sql.pure.querymodel.QAuditRefValue.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import javax.persistence.*;

import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.InsertQueryBuilder;
import com.evolveum.midpoint.repo.sql.data.SingleSqlQuery;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.EntityState;
import com.evolveum.midpoint.repo.sql.util.RUtil;

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

    public static RAuditReferenceValue toRepo(RAuditEventRecord record, String name, AuditReferenceValue value) {
        RAuditReferenceValue rValue = new RAuditReferenceValue();
        rValue.setRecord(record);
        rValue.setName(name);
        if (value != null) {
            rValue.setOid(value.getOid());
            rValue.setType(RUtil.qnameToString(value.getType()));
            rValue.setTargetName(RPolyString.toRepo(value.getTargetName()));
        }
        return rValue;
    }

    public static SingleSqlQuery toRepo(Long recordId, String name, AuditReferenceValue value) {
        InsertQueryBuilder queryBuilder = new InsertQueryBuilder(TABLE_NAME);
        queryBuilder.addParameter(COLUMN_RECORD_ID, recordId);
        queryBuilder.addParameter(NAME_COLUMN_NAME, name);
        if (value != null) {
            queryBuilder.addParameter(OID_COLUMN_NAME, value.getOid());
            queryBuilder.addParameter(TYPE_COLUMN_NAME, RUtil.qnameToString(value.getType()));
            if (value.getTargetName() != null) {
                queryBuilder.addParameter(TARGET_NAME_ORIG_COLUMN_NAME, value.getTargetName().getOrig());
                queryBuilder.addParameter(TARGET_NAME_NORM_COLUMN_NAME, value.getTargetName().getNorm());
            } else {
                queryBuilder.addNullParameter(TARGET_NAME_ORIG);
                queryBuilder.addNullParameter(TARGET_NAME_NORM.getName());
            }
        } else {
            queryBuilder.addNullParameter(OID.getName());
            queryBuilder.addNullParameter(TYPE.getName());
            queryBuilder.addNullParameter(TARGET_NAME_ORIG.getName());
            queryBuilder.addNullParameter(TARGET_NAME_NORM.getName());
        }
        return queryBuilder.build();
    }

    public AuditReferenceValue fromRepo(PrismContext prismContext) {
        return new AuditReferenceValue(oid, RUtil.stringToQName(type), RPolyString.fromRepo(targetName, prismContext));
    }

    public static AuditReferenceValue fromRepo(ResultSet resultSet) throws SQLException {
        PolyString targetName = null;
        if (resultSet.getString(TARGET_NAME_ORIG.getName()) != null
                || resultSet.getString(TARGET_NAME_NORM.getName()) != null) {
            targetName = new PolyString(resultSet.getString(TARGET_NAME_ORIG.getName()),
                    resultSet.getString(TARGET_NAME_NORM.getName()));
        }
        return new AuditReferenceValue(resultSet.getString(OID.getName()),
                RUtil.stringToQName(resultSet.getString(TYPE.getName())), targetName);
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
