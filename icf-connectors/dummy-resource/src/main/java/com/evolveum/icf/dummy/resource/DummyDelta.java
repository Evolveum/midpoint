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
package com.evolveum.icf.dummy.resource;

/**
 * @author semancik
 *
 */
public class DummyDelta {
	
	private int syncToken;
	private String accountId;
	private DummyDeltaType type;
	
	DummyDelta(int syncToken, String accountId, DummyDeltaType type) {
		this.syncToken = syncToken;
		this.accountId = accountId;
		this.type = type;
	}

	public int getSyncToken() {
		return syncToken;
	}
	
	public void setSyncToken(int syncToken) {
		this.syncToken = syncToken;
	}
	
	public String getAccountId() {
		return accountId;
	}
	
	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public DummyDeltaType getType() {
		return type;
	}

	public void setType(DummyDeltaType type) {
		this.type = type;
	}

	@Override
	public String toString() {
		return "DummyDelta(T=" + syncToken + ", id=" + accountId + ", t=" + type + ")";
	}
}
