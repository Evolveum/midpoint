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

public class RTargetResourceOidId implements Serializable{

	private static final long serialVersionUID = 1L;
	private Long recordId;
	private String resourceOid;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public String getresourceOid() {
		return resourceOid;
	}

    public void setresourceOid(String resourceOid) {
		this.resourceOid = resourceOid;
	}


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RTargetResourceOidId that = (RTargetResourceOidId) o;

        if (recordId != null ? !recordId.equals(that.recordId) : that.recordId != null) return false;
        if (resourceOid != null ? !resourceOid.equals(that.resourceOid) : that.resourceOid != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = recordId != null ? recordId.hashCode() : 0;
        result = 31 * result + (resourceOid != null ? resourceOid.hashCode() : 0);
        return result;
    }

}
