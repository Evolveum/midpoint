/*
 * Copyright (c) 2010-2013 Evolveum
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

	@Override
	public String toString() {
		return "DummyDelta(T=" + syncToken + ", c="+objectClass.getSimpleName()+", id=" + objectId + ", name="+objectName+", t=" + type + ")";
	}
}
