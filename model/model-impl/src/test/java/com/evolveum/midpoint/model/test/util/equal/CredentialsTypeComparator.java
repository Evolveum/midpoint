/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.test.util.equal;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.CredentialsType;

/**
 * 
 * @author lazyman
 * 
 */
public class CredentialsTypeComparator extends Equals<CredentialsType> {

	private static final Trace LOGGER = TraceManager.getTrace(CredentialsTypeComparator.class);
	
	@Override
	public boolean areEqual(CredentialsType o1, CredentialsType o2) {
		// TODO: finish this comparator !!!
		LOGGER.warn("IMPLEMENT CredentialsTypeComparator. It's not comparing now.");
		return true;
	}
}
