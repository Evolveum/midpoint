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

import java.io.Serializable;
import java.util.Objects;

/**
 * @author mederly
 */
public class ROExtBaseId implements Serializable {

	protected String ownerOid;
	protected RObjectExtensionType ownerType;
	protected Integer itemId;

	public String getOwnerOid() {
		return ownerOid;
	}

	public void setOwnerOid(String ownerOid) {
		this.ownerOid = ownerOid;
	}

	public RObjectExtensionType getOwnerType() {
		return ownerType;
	}

	public void setOwnerType(RObjectExtensionType ownerType) {
		this.ownerType = ownerType;
	}

	public Integer getItemId() {
		return itemId;
	}

	public void setItemId(Integer itemId) {
		this.itemId = itemId;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ROExtBaseId))
			return false;
		ROExtBaseId that = (ROExtBaseId) o;
		return itemId.equals(that.itemId) &&
				Objects.equals(ownerOid, that.ownerOid) &&
				ownerType == that.ownerType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(ownerOid, ownerType, itemId);
	}
}
