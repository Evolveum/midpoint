/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.query.ObjectPaging;

/**
 * @author Pavol
 */ // Temporary hack. Represents special paging object that means
// "give me objects with OID greater than specified one, sorted by OID ascending".
//
// TODO: replace by using cookie that is part of the standard ObjectPaging
// (but think out all consequences, e.g. conflicts with the other use of the cookie)
public class ObjectPagingAfterOid extends ObjectPaging {
    private String oidGreaterThan;

    public String getOidGreaterThan() {
        return oidGreaterThan;
    }

    public void setOidGreaterThan(String oidGreaterThan) {
        this.oidGreaterThan = oidGreaterThan;
    }

    @Override
    public String toString() {
        return super.toString() + ", after OID: " + oidGreaterThan;
    }

    @Override
    public ObjectPagingAfterOid clone() {
        ObjectPagingAfterOid clone = new ObjectPagingAfterOid();
        copyTo(clone);
        return clone;
    }

    protected void copyTo(ObjectPagingAfterOid clone) {
        super.copyTo(clone);
        clone.oidGreaterThan = this.oidGreaterThan;
    }

	@Override
    public boolean equals(Object o, boolean exact) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o, exact))
			return false;

		ObjectPagingAfterOid that = (ObjectPagingAfterOid) o;

		return oidGreaterThan != null ? oidGreaterThan.equals(that.oidGreaterThan) : that.oidGreaterThan == null;

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + (oidGreaterThan != null ? oidGreaterThan.hashCode() : 0);
		return result;
	}
}
