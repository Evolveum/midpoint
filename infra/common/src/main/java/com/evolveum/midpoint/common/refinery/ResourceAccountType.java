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
package com.evolveum.midpoint.common.refinery;

/**
 * @author semancik
 *
 */
public class ResourceAccountType {

	private String resourceOid;
	private String accountType;
	
	public ResourceAccountType(String resourceOid, String accountType) {
		this.resourceOid = resourceOid;
		this.accountType = accountType;
	}
	
	public String getResourceOid() {
		return resourceOid;
	}
	public void setResourceOid(String resourceOid) {
		this.resourceOid = resourceOid;
	}
	public String getAccountType() {
		return accountType;
	}
	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	@Override
	public String toString() {
		return "RAT(" + resourceOid + ": " + accountType + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accountType == null) ? 0 : accountType.hashCode());
		result = prime * result + ((resourceOid == null) ? 0 : resourceOid.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceAccountType other = (ResourceAccountType) obj;
		if (accountType == null) {
			if (other.accountType != null)
				return false;
		} else if (!accountType.equals(other.accountType))
			return false;
		if (resourceOid == null) {
			if (other.resourceOid != null)
				return false;
		} else if (!resourceOid.equals(other.resourceOid))
			return false;
		return true;
	}
	
}
