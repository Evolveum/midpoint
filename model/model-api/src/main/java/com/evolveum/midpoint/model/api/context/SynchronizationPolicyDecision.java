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
package com.evolveum.midpoint.model.api.context;

/**
 * Describes what the policy "decides" about a specific account.
 * 
 * @author Radovan Semancik
 *
 */
public enum SynchronizationPolicyDecision {
	
	/**
	 * New account that is going to be added (and linked)
	 */
	ADD,
	
	/**
	 * Existing account that is going to be deleted (and unlinked)
	 */
	DELETE,
	
	/**
	 * Existing account that is kept as it is (remains linked).
	 * Note: there still may be attribute or entitlement changes.
	 */
	KEEP,
	
	/**
	 * Existing account that is going to be unlinked (but NOT deleted)
	 */
	UNLINK,
	
	/**
	 * The account is not usable. E.g. because the associated shadow does
	 * not exist any more, resource does not exists any more, etc.
	 * Such account link will be removed.
	 */
	BROKEN;

}
