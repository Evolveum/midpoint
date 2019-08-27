/**
 * Copyright (c) 2019 Evolveum
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
package com.evolveum.midpoint.prism;

import java.io.Serializable;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class SchemaMigration implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private final QName elementQName;
	private final String version;
	private final SchemaMigrationOperation operation;
	
	public SchemaMigration(QName elementQName, String version, SchemaMigrationOperation operation) {
		super();
		this.elementQName = elementQName;
		this.version = version;
		this.operation = operation;
	}

	public QName getElementQName() {
		return elementQName;
	}

	public String getVersion() {
		return version;
	}

	public SchemaMigrationOperation getOperation() {
		return operation;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elementQName == null) ? 0 : elementQName.hashCode());
		result = prime * result + ((operation == null) ? 0 : operation.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		SchemaMigration other = (SchemaMigration) obj;
		if (elementQName == null) {
			if (other.elementQName != null) {
				return false;
			}
		} else if (!elementQName.equals(other.elementQName)) {
			return false;
		}
		if (operation != other.operation) {
			return false;
		}
		if (version == null) {
			if (other.version != null) {
				return false;
			}
		} else if (!version.equals(other.version)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "SchemaMigration(element=" + elementQName + ", version=" + version + ", operation="
				+ operation + ")";
	}

}
