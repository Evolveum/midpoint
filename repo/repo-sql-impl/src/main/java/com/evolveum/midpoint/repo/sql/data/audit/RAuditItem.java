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

import javax.persistence.*;

import com.evolveum.midpoint.repo.sql.util.EntityState;
import org.hibernate.annotations.ForeignKey;

@Entity
@IdClass(RAuditItemId.class)
@Table(name = RAuditItem.TABLE_NAME, indexes = {
		@Index(name = "iChangedItemPath", columnList = "changedItemPath")})
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
    @Column(name = "changedItemPath", length=900)
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAuditItem that = (RAuditItem) o;

        if (changedItemPath != null ? !changedItemPath.equals(that.changedItemPath) : that.changedItemPath != null) return false;
//        if (record != null ? !record.equals(that.record) : that.record != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result1 = changedItemPath != null ? changedItemPath.hashCode() : 0;
//        result1 = 31 * result1 + (record != null ? record.hashCode() : 0);
        return result1;
    }


}
