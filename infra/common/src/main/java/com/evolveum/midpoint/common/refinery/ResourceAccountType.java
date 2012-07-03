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

import com.evolveum.midpoint.schema.constants.MidPointConstants;

/**
 * Aggregate bean containing resource OID, accountType and thombstone flag.
 * It uniquely identifies an account for a specific user regardless whether it has OID, does not have
 * OID yet, it exists of was deleted.
 * 
 * This is used mostly as a key in hashes and for searches.
 * 
 * @author Radovan Semancik
 */
public class ResourceAccountType {

	private String resourceOid;
	private String accountType;
	private boolean thombstone;
	
	public ResourceAccountType(String resourceOid, String accountType) {
		this(resourceOid, accountType, false);
	}
	
	public ResourceAccountType(String resourceOid, String accountType, boolean thombstone) {
		this.resourceOid = resourceOid;
		this.thombstone = thombstone;
		setAccountType(accountType);
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
		if (accountType == null) {
			this.accountType = MidPointConstants.DEFAULT_ACCOUNT_NAME;
		} else {
			this.accountType = accountType;
		}
	}

	/**
	 * Thumbstone flag is true: the account no longer exists. The data we have are the latest metadata we were able to get. 
	 */
	public boolean isThombstone() {
		return thombstone;
	}

	public void setThombstone(boolean thombstone) {
		this.thombstone = thombstone;
	}

	@Override
	public String toString() {
		return "RAT(" + resourceOid + ": " + accountType + ( thombstone ? ", THOMBSTONE" : "" ) + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accountType == null) ? 0 : accountType.hashCode());
		result = prime * result + ((resourceOid == null) ? 0 : resourceOid.hashCode());
		result = prime * result + (thombstone ? 1231 : 1237);
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
		if (thombstone != other.thombstone)
			return false;
		return true;
	}

	public boolean equivalent(Object obj) {
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
		} else if (!equalsAccountType(this.accountType, other.accountType))
			return false;
		if (resourceOid == null) {
			if (other.resourceOid != null)
				return false;
		} else if (!resourceOid.equals(other.resourceOid))
			return false;
		if (thombstone != other.thombstone)
			return false;
		return true;
	}
	
	public static boolean equalsAccountType(String a, String b) {
		if (isDefaultAccountType(a) && isDefaultAccountType(b)) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

	public static boolean isDefaultAccountType(String accountType) {
		if (accountType == null) {
			return true;
		}
		return (MidPointConstants.DEFAULT_ACCOUNT_NAME.equals(accountType));
	}
	
}
