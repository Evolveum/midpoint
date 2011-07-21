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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;

/**
 * 
 * @author lazyman
 * 
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml" })
public class ControllerListAccountShadowOwnerTest {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/listObjects");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerListAccountShadowOwnerTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;

	@Before
	public void before() {
		Mockito.reset(repository, provisioning);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullAccountOid() throws Exception {
		controller.listAccountShadowOwner(null, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyAccountOid() throws Exception {
		controller.listAccountShadowOwner("", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		controller.listAccountShadowOwner("1", null);
	}

	@Test
	public void accountWithoutOwner() throws FaultMessage, ObjectNotFoundException {
		final String accountOid = "1";
		when(repository.listAccountShadowOwner(eq(accountOid), any(OperationResult.class))).thenReturn(null);

		OperationResult result = new OperationResult("accountWithoutOwner");
		try {
			final UserType returned = controller.listAccountShadowOwner("1", result);
			assertNull(returned);
		} finally {
			LOGGER.debug(result.dump());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void correctListAccountShadowOwner() throws FaultMessage, JAXBException, ObjectNotFoundException {
		final String accountOid = "acc11111-76e0-48e2-86d6-3d4f02d3e1a2";
		UserType expected = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"list-account-shadow-owner.xml"))).getValue();

		when(repository.listAccountShadowOwner(eq(accountOid), any(OperationResult.class))).thenReturn(
				expected);
		OperationResult result = new OperationResult("correctListAccountShadowOwner");
		try {
			final UserType returned = (UserType) controller.listAccountShadowOwner(accountOid, result);
			assertNotNull(returned);
			assertEquals(expected, returned);
		} finally {
			LOGGER.debug(result.dump());
		}
	}
}
