/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.model.controller;

import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml" })
public class ControllerListAccountShadowOwnerTest extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/listObjects");
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerListAccountShadowOwnerTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;
	@Autowired(required = true)
	private TaskManager taskManager;

	@BeforeMethod
	public void before() {
		Mockito.reset(repository, provisioning);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullAccountOid() throws Exception {
		controller.findShadowOwner(null, null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void emptyAccountOid() throws Exception {
		controller.findShadowOwner("", null, null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		controller.findShadowOwner("1", null, null);
	}

	@Test
	public void accountWithoutOwner() throws FaultMessage, ObjectNotFoundException {
		final String accountOid = "1";
		when(repository.listAccountShadowOwner(eq(accountOid), any(OperationResult.class))).thenReturn(null);

		Task task = taskManager.createTaskInstance("accountWithoutOwner");
		try {
			final PrismObject<UserType> returned = controller.findShadowOwner("1", task, task.getResult());
			assertNull(returned);
		} finally {
			LOGGER.debug(task.getResult().dump());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void correctListAccountShadowOwner() throws FaultMessage, JAXBException, ObjectNotFoundException, SchemaException, FileNotFoundException {
		final String accountOid = "acc11111-76e0-48e2-86d6-3d4f02d3e1a2";
		UserType expected = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER,
				"list-account-shadow-owner.xml"), UserType.class);

		when(repository.listAccountShadowOwner(eq(accountOid), any(OperationResult.class))).thenReturn(
				expected.asPrismObject());
		Task task = taskManager.createTaskInstance("correctListAccountShadowOwner");
		try {
			final UserType returned = controller.findShadowOwner(accountOid, task, task.getResult()).asObjectable();
			assertNotNull(returned);
			assertEquals(expected, returned);
		} finally {
			LOGGER.debug(task.getResult().dump());
		}
	}
}
