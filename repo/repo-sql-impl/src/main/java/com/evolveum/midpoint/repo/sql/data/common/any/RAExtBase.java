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

import com.evolveum.midpoint.repo.sql.data.common.type.RAssignmentExtensionType;

import java.util.Objects;

abstract public class RAExtBase<T> extends RAnyBase<T> implements RAExtValue<T> {

	private Boolean trans;

	//owner entity
	private RAssignmentExtension anyContainer;
	private String ownerOid;
	private Integer ownerId;

	private RAssignmentExtensionType extensionType;

	public String getOwnerOid() {
		if (ownerOid == null && anyContainer != null) {
			ownerOid = anyContainer.getOwnerOid();
		}
		return ownerOid;
	}

	public void setOwnerOid(String ownerOid) {
		this.ownerOid = ownerOid;
	}

	public Integer getOwnerId() {
		if (ownerId == null && anyContainer != null) {
			ownerId = anyContainer.getOwnerId();
		}
		return ownerId;
	}

	public void setOwnerId(Integer ownerId) {
		this.ownerId = ownerId;
	}

	@Override
	public Boolean isTransient() {
		return trans;
	}

	@Override
	public void setTransient(Boolean trans) {
		this.trans = trans;
	}

	@Override
	public RAssignmentExtension getAnyContainer() {
		return anyContainer;
	}

//	@Transient
//	public String getAssignmentOwnerOid() {
//		return anyContainer != null ? anyContainer.getAssignmentOwnerOid() : null;
//	}

//	@Transient
//	public Integer getAssignmentId() {
//		return anyContainer != null ? anyContainer.getAssignmentId() : null;
//	}

	@Override
	public void setAnyContainer(RAssignmentExtension anyContainer) {
		this.anyContainer = anyContainer;
	}

	@Override
	public RAssignmentExtensionType getExtensionType() {
		return extensionType;
	}

	@Override
	public void setExtensionType(RAssignmentExtensionType extensionType) {
		this.extensionType = extensionType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof RAExtBase))
			return false;
		if (!super.equals(o))
			return false;
		RAExtBase<?> raExtBase = (RAExtBase<?>) o;
		return /*Objects.equals(getAssignmentOwnerOid(), raExtBase.getAssignmentOwnerOid()) &&
				Objects.equals(getAssignmentId(), raExtBase.getAssignmentId()) && */
				getExtensionType() == raExtBase.getExtensionType();
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), /*getAssignmentOwnerOid(), getAssignmentId(), */ getExtensionType());
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" +
				"item id=" + getItemId() +
				", value=" + getValue() +
				'}';
	}

	//	public RObject getOwner() {
	//		return owner;
	//	}
	//
	//	public String getOwnerOid() {
	//		if (ownerOid == null && owner != null) {
	//			ownerOid = owner.getOid();
	//		}
	//		return ownerOid;
	//	}
	//
	//	public RObjectExtensionType getOwnerType() {
	//		return ownerType;
	//	}
	//
	//	public void setOwner(RObject owner) {
	//		this.owner = owner;
	//	}
	//
	//	public void setOwnerOid(String ownerOid) {
	//		this.ownerOid = ownerOid;
	//	}
	//
	//	public void setOwnerType(RObjectExtensionType ownerType) {
	//		this.ownerType = ownerType;
	//	}
	//
	//	@Override
	//	public boolean equals(Object o) {
	//		if (this == o)
	//			return true;
	//		if (!(o instanceof RAExtBase))
	//			return false;
	//		if (!super.equals(o))
	//			return false;
	//		RAExtBase roExtBase = (RAExtBase) o;
	//		return Objects.equals(getOwnerOid(), roExtBase.getOwnerOid()) &&
	//				getOwnerType() == roExtBase.getOwnerType() &&
	//				Objects.equals(getItem(), roExtBase.getItem());
	//	}
	//
	//	@Override
	//	public int hashCode() {
	//		return Objects.hash(super.hashCode(), getOwnerOid(), getOwnerType(), getItem());
	//	}
}
