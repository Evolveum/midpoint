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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * 
 * @author lazyman
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml" })
public class ActionManagerImplTest {

	@Autowired(required = true)
	private ActionManager<? extends Action> manager;

	@Test(expected = IllegalArgumentException.class)
	public void setActionMappingNull() {
		manager.setActionMapping(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getActionInstanceNullUri() {
		manager.getActionInstance(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void getctionInstanceEmptyUri() {
		manager.getActionInstance("");
	}

	@Test
	public void getActionInstanceNullParameters() {
		Action filter = manager
				.getActionInstance("http://midpoint.evolveum.com/xml/ns/public/model/action-1#addUser");
		assertNotNull(filter);
	}

	@Test
	public void getActionInstanceNotExistingFilter() {
		Action filter = manager.getActionInstance("notExistingUri");
		assertNull(filter);
	}
}
