/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.repo.sql.data.InsertQueryBuilder;
import com.evolveum.midpoint.repo.sql.data.SingleSqlQuery;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.util.EntityState;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.evolveum.midpoint.repo.sql.data.audit.RAuditPropertyValue.COLUMN_RECORD_ID;
import static com.evolveum.midpoint.repo.sql.data.audit.RAuditPropertyValue.TABLE_NAME;

@Ignore
@Entity
@Table(name = TABLE_NAME, indexes = {
        @Index(name = "iAuditPropValRecordId", columnList = COLUMN_RECORD_ID)})
public class RAuditPropertyValue implements EntityState {

    public static final String TABLE_NAME = "m_audit_prop_value";
    public static final String COLUMN_RECORD_ID = "record_id";

    public static final String NAME_COLUMN_NAME = "name";
    public static final String VALUE_COLUMN_NAME = "value";

    private Boolean trans;

    private long id;
    private RAuditEventRecord record;
    private Long recordId;
    private String name;
    private String value;

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

    @Column(length = AuditService.MAX_PROPERTY_SIZE)
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static RAuditPropertyValue toRepo(RAuditEventRecord record, String name, String value) {
        RAuditPropertyValue property = new RAuditPropertyValue();
        property.setRecord(record);
        property.setName(name);
        property.setValue(value);
        return property;
    }

    public static SingleSqlQuery toRepo(Long recordId, String name, String value) {
        InsertQueryBuilder queryBuilder = new InsertQueryBuilder(TABLE_NAME);
        queryBuilder.addParameter(COLUMN_RECORD_ID, recordId);
        queryBuilder.addParameter(NAME_COLUMN_NAME, name);
        queryBuilder.addParameter(VALUE_COLUMN_NAME, value);
        return queryBuilder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RAuditPropertyValue))
            return false;
        RAuditPropertyValue that = (RAuditPropertyValue) o;
        return id == that.id &&
                Objects.equals(recordId, that.recordId) &&
                Objects.equals(name, that.name) &&
                Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, recordId, name);
    }

    @Override
    public String toString() {
        return "RAuditPropertyValue{" +
                "id=" + id +
                ", recordId=" + recordId +
                ", name='" + name + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

}
