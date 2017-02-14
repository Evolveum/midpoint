/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.data.audit;

import com.evolveum.midpoint.audit.api.AuditReferenceValue;
import com.evolveum.midpoint.repo.sql.util.RUtil;

import javax.persistence.*;
import java.util.Objects;

import static com.evolveum.midpoint.repo.sql.data.audit.RAuditReferenceValue.COLUMN_RECORD_ID;
import static com.evolveum.midpoint.repo.sql.data.audit.RAuditReferenceValue.TABLE_NAME;

@Entity
@Table(name = TABLE_NAME, indexes = {
		@Index(name = "iRecordId", columnList = COLUMN_RECORD_ID)})
public class RAuditReferenceValue {

	public static final String TABLE_NAME = "m_audit_ref_value";
	public static final String COLUMN_RECORD_ID = "record_id";

	private long id;
    private RAuditEventRecord record;
    private Long recordId;
    private String key;
    private String oid;
    private String type;
    private String targetName;

	@Id
	@GeneratedValue
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

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

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

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public static RAuditReferenceValue toRepo(RAuditEventRecord record, String key, AuditReferenceValue value) {
    	RAuditReferenceValue rValue = new RAuditReferenceValue();
    	rValue.setRecord(record);
    	rValue.setKey(key);
    	if (value != null) {
			rValue.setOid(value.getOid());
			rValue.setType(RUtil.qnameToString(value.getType()));
			rValue.setTargetName(value.getTargetName());
		}
    	return rValue;
    }

	public AuditReferenceValue fromRepo() {
		return new AuditReferenceValue(oid, RUtil.stringToQName(type), targetName);
	}


	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof RAuditReferenceValue))
			return false;
		RAuditReferenceValue that = (RAuditReferenceValue) o;
		return id == that.id &&
				Objects.equals(recordId, that.recordId) &&
				Objects.equals(key, that.key) &&
				Objects.equals(oid, that.oid) &&
				Objects.equals(type, that.type) &&
				Objects.equals(targetName, that.targetName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, recordId, key, oid);
	}

	@Override
	public String toString() {
		return "RAuditReferenceValue{" +
				"id=" + id +
				", recordId=" + recordId +
				", key='" + key + '\'' +
				", oid='" + oid + '\'' +
				", type='" + type + '\'' +
				", targetName='" + targetName + '\'' +
				'}';
	}

}
