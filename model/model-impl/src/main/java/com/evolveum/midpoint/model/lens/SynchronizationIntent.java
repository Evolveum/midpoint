/**
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens;

import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;

/**
 * @author semancik
 *
 */
public enum SynchronizationIntent {
	
	/**
	 * New account that should be added (and linked)
	 */
	ADD,
	
	/**
	 * Existing account that should be deleted (and unlinked)
	 */
	DELETE,
	
	/**
	 * Existing account that is kept as it is (remains linked).
	 */
	KEEP,
	
	/**
	 * Existing account that should be unlinked (but NOT deleted)
	 */
	UNLINK,
	
	/**
	 * Existing account that belongs to the user and needs to be synchronized.
	 * This may include deleting, archiving or disabling the account.
	 */
	SYNCHRONIZE;

	public SynchronizationPolicyDecision toSynchronizationPolicyDecision() {
		if (this == ADD) {
			return SynchronizationPolicyDecision.ADD;
		}
		if (this == DELETE) {
			return SynchronizationPolicyDecision.DELETE;
		}
		if (this == KEEP) {
			return SynchronizationPolicyDecision.KEEP;
		}
		if (this == UNLINK) {
			return SynchronizationPolicyDecision.UNLINK;
		}
		if (this == SYNCHRONIZE) {
			return null;
		}
		throw new IllegalStateException("Unexpected value "+this);
	}

}
