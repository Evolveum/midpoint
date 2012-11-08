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

import org.testng.annotations.Test;
import org.testng.AssertJUnit;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * 
 * @author lazyman
 * 
 */
public class SynchronizationSituationTest {

	@Test
	public void nullUser() {
		SynchronizationSituation situation = new SynchronizationSituation(null,
				SynchronizationSituationType.UNMATCHED);
		AssertJUnit.assertNotNull(situation);
		AssertJUnit.assertNull(situation.getUser());
		AssertJUnit.assertEquals(SynchronizationSituationType.UNMATCHED, situation.getSituation());
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullSituation() {
		new SynchronizationSituation(new UserType(), null);
	}

	@Test
	public void correct() {
		UserType user = new UserType();
		SynchronizationSituation situation = new SynchronizationSituation(user,
				SynchronizationSituationType.UNMATCHED);
		AssertJUnit.assertNotNull(situation);
		AssertJUnit.assertEquals(user, situation.getUser());
		AssertJUnit.assertEquals(SynchronizationSituationType.UNMATCHED, situation.getSituation());
	}
}
