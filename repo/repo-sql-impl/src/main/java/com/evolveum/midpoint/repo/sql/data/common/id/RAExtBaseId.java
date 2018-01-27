/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql.data.common.id;

import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;

import java.util.Objects;

/**
 * @author mederly
 */
public class RAExtBaseId {

	protected String ownerOid;
	protected Integer ownerId;
	protected long itemId;

	public String getOwnerOid() {
		return ownerOid;
	}

	public void setOwnerOid(String ownerOid) {
		this.ownerOid = ownerOid;
	}

	public Integer getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Integer ownerId) {
		this.ownerId = ownerId;
	}

	public long getItemId() {
		return itemId;
	}

	public void setItemId(long itemId) {
		this.itemId = itemId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof RAExtBaseId))
			return false;
		RAExtBaseId that = (RAExtBaseId) o;
		return itemId == that.itemId &&
				Objects.equals(ownerOid, that.ownerOid) &&
				Objects.equals(ownerId, that.ownerId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(ownerOid, ownerId, itemId);
	}
}
