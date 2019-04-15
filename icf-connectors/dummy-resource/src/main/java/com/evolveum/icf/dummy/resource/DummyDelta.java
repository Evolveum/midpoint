/*
 * Copyright (c) 2010-2019 Evolveum
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
package com.evolveum.icf.dummy.resource;

import java.util.Collection;

/**
 * @author semancik
 *
 */
public class DummyDelta {

	private int syncToken;
	private Class<? extends DummyObject> objectClass;
	private String objectId;
	private String objectName;
	private DummyDeltaType type;
	private String attributeName;
	private Collection<Object> valuesAdded = null;
	private Collection<Object> valuesDeleted = null;
	private Collection<Object> valuesReplaced = null;

	DummyDelta(int syncToken, Class<? extends DummyObject> objectClass, String objectId, String objectName, DummyDeltaType type) {
		this.syncToken = syncToken;
		this.objectClass = objectClass;
		this.objectId = objectId;
		this.objectName = objectName;
		this.type = type;
	}

	public int getSyncToken() {
		return syncToken;
	}

	public void setSyncToken(int syncToken) {
		this.syncToken = syncToken;
	}

	public Class<? extends DummyObject> getObjectClass() {
		return objectClass;
	}

	public void setObjectClass(Class<? extends DummyObject> objectClass) {
		this.objectClass = objectClass;
	}

	public String getObjectId() {
		return objectId;
	}

	public void setObjectId(String accountId) {
		this.objectId = accountId;
	}

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public DummyDeltaType getType() {
		return type;
	}

	public void setType(DummyDeltaType type) {
		this.type = type;
	}
	
	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	public Collection<Object> getValuesAdded() {
		return valuesAdded;
	}

	public void setValuesAdded(Collection<Object> valuesAdded) {
		this.valuesAdded = valuesAdded;
	}

	public Collection<Object> getValuesDeleted() {
		return valuesDeleted;
	}

	public void setValuesDeleted(Collection<Object> valuesDeleted) {
		this.valuesDeleted = valuesDeleted;
	}

	public Collection<Object> getValuesReplaced() {
		return valuesReplaced;
	}

	public void setValuesReplaced(Collection<Object> valuesReplaced) {
		this.valuesReplaced = valuesReplaced;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("DummyDelta(");
		dumpToString(sb);
		sb.append(")");
		return sb.toString();
	}
	
	public void dump(StringBuilder sb) {
		dumpToString(sb);
		sb.append(", A=").append(attributeName).append(": ");
		if (valuesAdded != null) {
			sb.append(" +").append(valuesAdded);
		}
		if (valuesDeleted != null) {
			sb.append(" -").append(valuesDeleted);
		}
		if (valuesReplaced != null) {
			sb.append(" =").append(valuesReplaced);
		}
	}
	
	private void dumpToString(StringBuilder sb) {
		sb.append("T=").append(syncToken);
		sb.append(", c=").append(objectClass.getSimpleName());
		sb.append(", id=").append(objectId);
		sb.append(", name=").append(objectName);
		sb.append(", t=").append(type);
	}
}
