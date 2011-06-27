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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml" })
public class ControllerAddUserTest {

	private static final Trace LOGGER = TraceManager.getTrace(ControllerAddUserTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;

	@Test(expected = IllegalArgumentException.class)
	public void addNullUser() throws Exception {
		controller.addUser(null, null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addUserWithNullTemplate() throws Exception {
		controller.addUser(new UserType(), null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void addUserWithNullResult() throws Exception {
		controller.addUser(null, null, null);
	}

	@Ignore //TODO: not finished yet
	@Test(expected = ObjectNotFoundException.class)
	public void addUserWithSimpleTemplate() throws Exception {
		UserType user = null;
		UserTemplateType userTemplate = null;

		OperationResult result = new OperationResult("Add User With Template");
		try {
			controller.addUser(user, userTemplate, result);
		} finally {
			LOGGER.info(result.debugDump());
		}
	}
}
