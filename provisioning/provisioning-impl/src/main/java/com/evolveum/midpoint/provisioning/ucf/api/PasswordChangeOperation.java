/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ProtectedStringType;

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
			if (passwd.getEncryptedData()!=null) {
				sb.append(" encrypted");
			}
		} else {
			sb.append("null");
		}
	}
	
}
