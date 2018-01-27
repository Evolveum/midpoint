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

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import org.hibernate.annotations.ForeignKey;

import javax.persistence.*;
import java.util.Objects;

abstract public class ROExtBase extends RAnyBase implements ROExtValue {

	//owner entity
	private RObject owner;
	private String ownerOid;
	private RObjectExtensionType ownerType;

	public RObject getOwner() {
		return owner;
	}

	public String getOwnerOid() {
		if (ownerOid == null && owner != null) {
			ownerOid = owner.getOid();
		}
		return ownerOid;
	}

	public RObjectExtensionType getOwnerType() {
		return ownerType;
	}

	public void setOwner(RObject owner) {
		this.owner = owner;
	}

	public void setOwnerOid(String ownerOid) {
		this.ownerOid = ownerOid;
	}

	public void setOwnerType(RObjectExtensionType ownerType) {
		this.ownerType = ownerType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ROExtBase))
			return false;
		if (!super.equals(o))
			return false;
		ROExtBase roExtBase = (ROExtBase) o;
		return Objects.equals(getOwnerOid(), roExtBase.getOwnerOid()) &&
				getOwnerType() == roExtBase.getOwnerType() &&
				Objects.equals(getItem(), roExtBase.getItem());
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), getOwnerOid(), getOwnerType(), getItem());
	}
}
