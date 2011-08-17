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
package com.evolveum.midpoint.model.controller;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectModificationType;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml" })
public class ControllerModifyObjectTest extends AbstractTestNGSpringContextTests {

	private static final Trace LOGGER = TraceManager.getTrace(ControllerModifyObjectTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;

	@BeforeMethod
	public void before() {
		Mockito.reset(repository, provisioning);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullChange() throws Exception {
		controller.modifyObject(null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullChangeOid() throws Exception {
		controller.modifyObject(new ObjectModificationType(), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void emptyChangeOid() throws Exception {
		ObjectModificationType change = new ObjectModificationType();
		change.setOid("");
		controller.modifyObject(change, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		ObjectModificationType change = new ObjectModificationType();
		change.setOid("1");
		controller.modifyObject(change, null);
	}
}
