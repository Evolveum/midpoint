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
package com.evolveum.midpoint.schema;

import java.util.Collection;

import com.evolveum.midpoint.util.MiscUtil;

/**
 * @author semancik
 *
 */
public enum ObjectOperationOption {
	
	/**
	 * Resolve the object reference. This only makes sense with a (path-based) selector.
	 */
	RESOLVE,
	
	/**
	 * No not fetch any information from external sources, e.g. do not fetch account data from resource,
	 * do not fetch resource schema, etc.
	 * Such operation returns only the data stored in midPoint repository.
	 */
	NO_FETCH,
	
	/**
	 * Force the operation even if it would otherwise fail due to external failure. E.g. attempt to delete an account
	 * that no longer exists on resource may fail without a FORCE option. If FORCE option is used then the operation is
	 * finished even if the account does not exist (e.g. at least shadow is removed from midPoint repository).
	 */
	FORCE,
	
	/**
	 * Avoid any smart processing of the data except for schema application. Do not synchronize the data, do not apply
	 * any expressions, etc.
	 */
	RAW,
	
	/**
	 * Encrypt any cleartext data on write, decrypt any encrypted data on read. Applies only to the encrypted
	 * data formats (ProtectedString, ProtectedByteArray).
	 */
	CRYPT;
	
	// TODO:
	// SYNC option: always perform synchronous operation. If it would go to async or delayed then throw an error
	// (e.g. if approvals are started, async provisioning, queueing the operation because resource is offline, etc.)
	
	public static boolean hasOption(Collection<ObjectOperationOption> options, ObjectOperationOption option) {
		if (options == null) {
			return false;
		}
		for (ObjectOperationOption myOption: options) {
			if (myOption.equals(option)) {
				return true;
			}
		}
		return false;
	}

	public static Collection<ObjectOperationOption> createCollection(ObjectOperationOption... options) {
		return MiscUtil.createCollection(options);
	}

}
