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

import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;

/**
 * 
 * @author lazyman
 * 
 */
public class AccountShadowTypeComparator extends Equals<AccountShadowType> {

	@Override
	public boolean areEqual(AccountShadowType o1, AccountShadowType o2) {
		if (!new ResourceObjectShadowTypeComparator().areEqual(o1, o2)) {
			return false;
		}

		return areEqual(o1.getActivation(), o2.getActivation(), new ActivationTypeComparator())
				&& areEqual(o1.getCredentials(), o2.getCredentials(), new CredentialsTypeComparator());
	}
}
