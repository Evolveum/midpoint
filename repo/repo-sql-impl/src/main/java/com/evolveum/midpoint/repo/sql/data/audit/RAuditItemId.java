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

import java.io.Serializable;

public class RAuditItemId implements Serializable{

	private static final long serialVersionUID = 1L;
	private Long recordId;
	private String changedItemPath;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getChangedItemPath() {
		return changedItemPath;
	}

    public void setChangedItemPath(String changedItemPath) {
		this.changedItemPath = changedItemPath;
	}


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RAuditItemId that = (RAuditItemId) o;

        if (recordId != null ? !recordId.equals(that.recordId) : that.recordId != null) return false;
        if (changedItemPath != null ? !changedItemPath.equals(that.changedItemPath) : that.changedItemPath != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = recordId != null ? recordId.hashCode() : 0;
        result = 31 * result + (changedItemPath != null ? changedItemPath.hashCode() : 0);
        return result;
    }

}
