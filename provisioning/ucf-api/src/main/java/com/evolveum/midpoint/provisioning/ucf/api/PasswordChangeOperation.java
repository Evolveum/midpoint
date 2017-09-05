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
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
public class PasswordChangeOperation extends Operation {

	private ProtectedStringType newPassword;
	private ProtectedStringType oldPassword;

	public PasswordChangeOperation(ProtectedStringType newPassword) {
		super();
		this.newPassword = newPassword;
	}

	public PasswordChangeOperation(ProtectedStringType newPassword, ProtectedStringType oldPassword) {
		super();
		this.newPassword = newPassword;
		this.oldPassword = oldPassword;
	}

	public ProtectedStringType getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(ProtectedStringType newPassword) {
		this.newPassword = newPassword;
	}

	public ProtectedStringType getOldPassword() {
		return oldPassword;
	}

	public void setOldPassword(ProtectedStringType oldPassword) {
		this.oldPassword = oldPassword;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((newPassword == null) ? 0 : newPassword.hashCode());
		result = prime * result + ((oldPassword == null) ? 0 : oldPassword.hashCode());
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
		PasswordChangeOperation other = (PasswordChangeOperation) obj;
		if (newPassword == null) {
			if (other.newPassword != null) {
				return false;
			}
		} else if (!newPassword.equals(other.newPassword)) {
			return false;
		}
		if (oldPassword == null) {
			if (other.oldPassword != null) {
				return false;
			}
		} else if (!oldPassword.equals(other.oldPassword)) {
			return false;
		}
		return true;
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		SchemaDebugUtil.indentDebugDump(sb, indent);
		sb.append("Password change: new password ");
		appendPasswordDescription(sb,newPassword);
		sb.append("; old password ");
		appendPasswordDescription(sb,oldPassword);
		return sb.toString();
	}

	private void appendPasswordDescription(StringBuilder sb, ProtectedStringType passwd) {
		if (passwd != null) {
			sb.append("present");
			if (passwd.getClearValue() != null) {
				sb.append(" in clear");
				if (passwd.getClearValue().isEmpty()) {
					sb.append(" and empty");
				}
			}
			if (passwd.getEncryptedDataType() != null) {
				sb.append(" encrypted");
			}
		} else {
			sb.append("null");
		}
	}

}
