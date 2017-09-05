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
package com.evolveum.midpoint.model.impl.controller;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.evolveum.midpoint.util.exception.NoFocusNameSchemaException;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.impl.ModelCrudService;
import com.evolveum.midpoint.model.impl.util.ModelTUtil;
import com.evolveum.midpoint.model.impl.util.ObjectTypeNameMatcher;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningOperationOptions;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_3.FaultMessage;

/**
 *
 * @author lazyman
 *
 */
@ContextConfiguration(locations = { "classpath:ctx-model-test-no-repo.xml"})
public class ControllerAddObjectTest extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/addObject");
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerAddObjectTest.class);
	@Autowired(required = true)
	private ModelCrudService controller;
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;
	@Autowired(required = true)
	private TaskManager taskManager;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@BeforeMethod
	public void before() {
		Mockito.reset(provisioning, repository);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullObject() throws Exception {
		TestUtil.displayTestTitle("nullObject");
		controller.addObject(null, null, taskManager.createTaskInstance(), new OperationResult("Test Operation"));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		TestUtil.displayTestTitle("nullResult");
		controller.addObject(new UserType().asPrismObject(), null, taskManager.createTaskInstance(), null);
	}

	@Test(expectedExceptions = NoFocusNameSchemaException.class)
	@SuppressWarnings("unchecked")
	public void addUserWithoutName() throws Exception {
		TestUtil.displayTestTitle("addUserWithoutName");
		final UserType expectedUser = (UserType) PrismTestUtil.parseObject(new File(TEST_FOLDER,
                "add-user-without-name.xml")).asObjectable();

		OperationResult result = new OperationResult("Test Operation");
		try {
			controller.addObject(expectedUser.asPrismObject(), null, taskManager.createTaskInstance(), result);
		} finally {
			LOGGER.debug(result.debugDump());
		}
	}

	/**
	 * Testing add user with undefined user template
	 */
//	@Test
	@SuppressWarnings("unchecked")
	public void addUserCorrect() throws Exception {
		TestUtil.displayTestTitle("addUserCorrect");

		// GIVEN
		Task task = taskManager.createTaskInstance();

		ModelTUtil.mockGetSystemConfiguration(repository, new File(TEST_FOLDER_COMMON, "system-configuration.xml"));

		final PrismObject<UserType> expectedUser = PrismTestUtil.parseObject(new File(TEST_FOLDER,
				"add-user-correct.xml"));
		final UserType expectedUserType = expectedUser.asObjectable();

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(
				repository.addObject(argThat(new ObjectTypeNameMatcher(expectedUserType.getName())),
						any(RepoAddOptions.class), any(OperationResult.class))).thenAnswer(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				PrismObject<UserType> user = (PrismObject<UserType>) invocation.getArguments()[0];
				IntegrationTestTools.display("Got user", user);
				PrismAsserts.assertEquivalent("Unexpected argument to addObject", expectedUser, user);

				return oid;
			}
		});

		OperationResult result = new OperationResult("Test Operation");

		// WHEN
		String userOid = controller.addObject(expectedUser, null, task, result);

		// THEN
		display("addObject result",result.debugDump());

		verify(repository, times(1)).addObject(argThat(new ObjectTypeNameMatcher(expectedUserType.getName())),
				any(RepoAddOptions.class), any(OperationResult.class));
		assertEquals(oid, userOid);
	}

//	@Test
	@SuppressWarnings("unchecked")
	public void addResourceCorrect() throws JAXBException, FaultMessage, ObjectAlreadyExistsException,
            SchemaException, CommunicationException, ObjectNotFoundException, ExpressionEvaluationException,
            IOException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		TestUtil.displayTestTitle("addResourceCorrect");


		Task task = taskManager.createTaskInstance();

		final PrismObject<ResourceType> expectedResource = PrismTestUtil.parseObject(new File(
				TEST_FOLDER, "add-resource-correct.xml"));
		final ResourceType expectedResourceType = expectedResource.asObjectable();
		AssertJUnit.assertNotNull("resource to add must not be null", expectedResource);

		final String oid = "abababab-abab-abab-abab-000000000002";
		when(
				provisioning.addObject(argThat(new ObjectTypeNameMatcher(expectedResourceType.getName())),
						any(OperationProvisioningScriptsType.class), any(ProvisioningOperationOptions.class), any(Task.class), any(OperationResult.class))).thenAnswer(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				PrismObject<ResourceType> resource = (PrismObject<ResourceType>) invocation.getArguments()[0];
				PrismAsserts.assertEquivalent("Wrong argument to addObject", expectedResource, resource);

				return oid;
			}
		});

		OperationResult result = new OperationResult("Test Operation");
		try {
			String resourceOid = controller.addObject(expectedResource, null, task, result);
			assertEquals(oid, resourceOid);
		} finally {
			LOGGER.debug(result.debugDump());

			verify(provisioning, times(1)).addObject(
					argThat(new ObjectTypeNameMatcher(expectedResourceType.getName())), any(OperationProvisioningScriptsType.class),
					any(ProvisioningOperationOptions.class), any(Task.class), any(OperationResult.class));
		}
	}
}
