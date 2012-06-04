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
package com.evolveum.midpoint.model.sync;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.xml.ns._public.common.common_2.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public class SynchronizationSituation {

	private UserType user;
	private SynchronizationSituationType situation;

	SynchronizationSituation(UserType user, SynchronizationSituationType situation) {
		Validate.notNull(situation, "Synchronization situation must not be null.");
		this.user = user;
		this.situation = situation;
	}

	public UserType getUser() {
		return user;
	}

	public SynchronizationSituationType getSituation() {
		return situation;
	}
}
