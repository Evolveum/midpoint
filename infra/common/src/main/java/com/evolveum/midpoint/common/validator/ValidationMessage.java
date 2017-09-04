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

package com.evolveum.midpoint.common.validator;

/**
 *
 * @author semancik
 */
public class ValidationMessage {

	public enum Type {
		WARNING, ERROR
	};

	public Type type;
	public String oid;
	private String name;
	public String message;
	public String property;

	public ValidationMessage() {
	}

	public ValidationMessage(Type type, String message) {
		this(type, message, null, null);
	}

	public ValidationMessage(Type type, String message, String oid, String name) {
		this(type, message, oid, name, null);
	}

	public ValidationMessage(Type type, String message, String oid, String name, String property) {
		this.type = type;
		this.message = message;
		this.oid = oid;
		this.name = name;
		this.property = property;
	}

	/**
	 * Get the value of type
	 *
	 * @return the value of type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Set the value of type
	 *
	 * @param type
	 *            new value of type
	 */
	public void setType(Type type) {
		this.type = type;
	}

	/**
	 * Get the value of message
	 *
	 * @return the value of message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Set the value of message
	 *
	 * @param message
	 *            new value of message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Get the value of oid
	 *
	 * @return the value of oid
	 */
	public String getOid() {
		return oid;
	}

	/**
	 * Set the value of oid
	 *
	 * @param oid
	 *            new value of oid
	 */
	public void setOid(String oid) {
		this.oid = oid;
	}

	/**
	 * Get the value of property
	 *
	 * @return the value of property
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * Set the value of property
	 *
	 * @param property
	 *            new value of property
	 */
	public void setProperty(String property) {
		this.property = property;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (Type.ERROR.equals(getType())) {
			sb.append("ERROR: ");
		} else if (Type.WARNING.equals(getType())) {
			sb.append("WARNING: ");
		}
		sb.append(message);
		if (getOid() != null || getProperty() != null) {
			sb.append(" (");
			if (getOid() != null) {
				sb.append("OID: ");
				sb.append(getOid());
			}
			if (getProperty() != null) {
				sb.append(", property: ");
				sb.append(getProperty());
			}
			sb.append(")");
		}

		return sb.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ValidationMessage other = (ValidationMessage) obj;
		if (this.type != other.type) {
			return false;
		}
		if ((this.message == null) ? (other.message != null) : !this.message.equals(other.message)) {
			return false;
		}
		if ((this.oid == null) ? (other.oid != null) : !this.oid.equals(other.oid)) {
			return false;
		}
		if ((this.property == null) ? (other.property != null) : !this.property.equals(other.property)) {
			return false;
		}
		if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int hash = 7;
		hash = 71 * hash + this.type.hashCode();
		hash = 71 * hash + (this.message != null ? this.message.hashCode() : 0);
		hash = 71 * hash + (this.oid != null ? this.oid.hashCode() : 0);
		hash = 71 * hash + (this.property != null ? this.property.hashCode() : 0);
		hash = 71 * hash + (this.name != null ? this.name.hashCode() : 0);

		return hash;
	}

}
