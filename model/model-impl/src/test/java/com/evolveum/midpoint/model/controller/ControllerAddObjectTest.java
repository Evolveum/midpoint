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
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.model.test.util.ModelTUtil;
import com.evolveum.midpoint.model.test.util.mock.ObjectTypeNameMatcher;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.ns._public.common.fault_1_wsdl.FaultMessage;

/**
 * 
 * @author lazyman
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml",
		"classpath:application-context-configuration-test-no-repo.xml",
		"classpath:application-context-task.xml", 
		"classpath:application-context-audit.xml"})
public class ControllerAddObjectTest extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/addObject");
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerAddObjectTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;
	@Autowired(required = true)
	private TaskManager taskManager;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	@BeforeMethod
	public void before() {
		Mockito.reset(provisioning, repository);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullObject() throws Exception {
		displayTestTile("nullObject");
		controller.addObject(null, taskManager.createTaskInstance(), new OperationResult("Test Operation"));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		displayTestTile("nullResult");
		controller.addObject(new UserType().asPrismObject(), taskManager.createTaskInstance(), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	@SuppressWarnings("unchecked")
	public void addUserWithoutName() throws Exception {
		displayTestTile("addUserWithoutName");
		final UserType expectedUser = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER,
				"add-user-without-name.xml"), UserType.class);

		OperationResult result = new OperationResult("Test Operation");
		try {
			controller.addObject(expectedUser.asPrismObject(), taskManager.createTaskInstance(), result);
		} finally {
			LOGGER.debug(result.dump());
		}
	}

	/**
	 * Testing add user with undefined user template
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void addUserCorrect() throws Exception {
		displayTestTile("addUserCorrect");
		
		// GIVEN
		Task task = taskManager.createTaskInstance();
		
		ModelTUtil.mockGetSystemConfiguration(repository, new File(TEST_FOLDER_COMMON, "system-configuration.xml"));

		final PrismObject<UserType> expectedUser = PrismTestUtil.parseObject(new File(TEST_FOLDER,
				"add-user-correct.xml"));
		final UserType expectedUserType = expectedUser.asObjectable();

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(
				repository.addObject(argThat(new ObjectTypeNameMatcher(expectedUserType.getName())),
						any(OperationResult.class))).thenAnswer(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				PrismObject<UserType> user = (PrismObject<UserType>) invocation.getArguments()[0];
				PrismAsserts.assertEquivalent("Unexpected argument to addObject", expectedUser, user);

				return oid;
			}
		});

		OperationResult result = new OperationResult("Test Operation");
		
		// WHEN
		String userOid = controller.addObject(expectedUser, task, result);
		
		// THEN
		display("addObject result",result.dump());

		verify(repository, times(1)).addObject(argThat(new ObjectTypeNameMatcher(expectedUserType.getName())),
				any(OperationResult.class));
		assertEquals(oid, userOid);
	}

	/**
	 * Testing add user with undefined user template. It must fail because user
	 * already exists (mocked).
	 */
	@Test(expectedExceptions = ObjectAlreadyExistsException.class)
	@SuppressWarnings("unchecked")
	public void addUserWithExistingOid() throws Exception {
		displayTestTile("addUserWithExistingOid");
		
		// GIVEN
		Task task = taskManager.createTaskInstance();
		
		ModelTUtil.mockGetSystemConfiguration(repository, new File(TEST_FOLDER_COMMON, "system-configuration.xml"));
		final UserType expectedUser = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER, "add-user-with-oid.xml"), UserType.class);
		
		when(repository.addObject(eq(expectedUser.asPrismObject()), any(OperationResult.class))).thenThrow(
				new ObjectAlreadyExistsException());

		OperationResult result = new OperationResult("Test Operation");
		try {
		
			// WHEN
			controller.addObject(expectedUser.asPrismObject(), task, result);
		
		} finally {
			LOGGER.debug(result.dump());

			verify(repository, times(1)).addObject(
					argThat(new ObjectTypeNameMatcher(expectedUser.getName())), any(OperationResult.class));
		}
	}

//	@Test
//	@SuppressWarnings("unchecked")
//	public void addUserAndCreateDefaultAccount() throws Exception {
//		displayTestTile("addUserAndCreateDefaultAccount");
//		
//		// GIVEN
//		Task task = taskManager.createTaskInstance();
//		
//		ModelTUtil.mockGetSystemConfiguration(repository, new File(TEST_FOLDER_COMMON,
//				"system-configuration-with-template.xml"));
//
//		final String resourceOid = "10000000-0000-0000-0000-000000000003";
//		ResourceType resource = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER_COMMON, "resource.xml"));
//		when(
//				provisioning.getObject(eq(ResourceType.class), eq(resourceOid),
//						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
//				resource.asPrismObject());
//
//		final String accountOid = "10000000-0000-0000-0000-000000000004";
//		when(
//				provisioning.addObject(any(PrismObject.class), any(ScriptsType.class),
//						any(OperationResult.class))).thenAnswer(new Answer<String>() {
//			@Override
//			public String answer(InvocationOnMock invocation) throws Throwable {
//				AccountShadowType account = (AccountShadowType) invocation.getArguments()[0];
//				display("repository.addObject() account", account.asPrismObject());
//// Assert fails due to a prefix mismatch: false negative
////				XmlAsserts.assertPatch(new File(TEST_FOLDER, "expected-account.xml"),
////						marshalledAccount);
//				return accountOid;
//			}
//		});
//
//		final String userOid = "abababab-abab-abab-abab-000000000001";
//		when(repository.addObject(any(PrismObject.class), any(OperationResult.class))).thenAnswer(
//				new Answer<String>() {
//					@Override
//					public String answer(InvocationOnMock invocation) throws Throwable {
//						UserType user = (UserType) invocation.getArguments()[0];
//						PrismAsserts.assertEquals(new File(TEST_FOLDER, "expected-add-user-default-accounts.xml"), user);
//						return userOid;
//					}
//				});
//		
//		doAnswer(new Answer<Object>() {
//			@Override
//			public Object answer(InvocationOnMock invocation) throws Throwable {
//				ObjectModificationType mod = (ObjectModificationType) invocation.getArguments()[1];
//				String marshalledMod = JAXBUtil.marshalWrap(mod);
//				display("repository.modifyObject() ",marshalledMod);
//				XmlAsserts.assertPatch(
//						new File(TEST_FOLDER, "expected-modify-user-default-accounts.xml"),
//						marshalledMod);
//				return null;
//			}
//		}).when(repository).modifyObject(any(Class.class), any(ObjectModificationType.class),
//				any(OperationResult.class));
//
//		OperationResult result = new OperationResult("Test Operation");
//		final UserType addedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
//				"add-user-default-accounts.xml"))).getValue();
//		
//		// WHEN
//		String returnedOid = controller.addObject(addedUser, task, result);
//		
//		// THEN
//		display("addObject operation result",result.dump());
//
//		verify(provisioning, atLeast(1)).getObject(eq(ResourceType.class), eq(resourceOid),
//				any(PropertyReferenceListType.class), any(OperationResult.class));
//		verify(repository, times(1)).addObject(argThat(new ObjectTypeNameMatcher(addedUser.getName())),
//				any(OperationResult.class));
//		assertEquals(userOid, returnedOid);
//	}

	@Test
	@SuppressWarnings("unchecked")
	public void addResourceCorrect() throws JAXBException, FaultMessage, ObjectAlreadyExistsException,
			SchemaException, CommunicationException, ObjectNotFoundException, ExpressionEvaluationException, FileNotFoundException, ConfigurationException {
		displayTestTile("addResourceCorrect");
		
		
		Task task = taskManager.createTaskInstance();
		
		final PrismObject<ResourceType> expectedResource = PrismTestUtil.parseObject(new File(
				TEST_FOLDER, "add-resource-correct.xml"));
		final ResourceType expectedResourceType = expectedResource.asObjectable();

		final String oid = "abababab-abab-abab-abab-000000000002";
		when(
				provisioning.addObject(argThat(new ObjectTypeNameMatcher(expectedResourceType.getName())),
						any(ScriptsType.class), any(OperationResult.class))).thenAnswer(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				PrismObject<ResourceType> resource = (PrismObject<ResourceType>) invocation.getArguments()[0];
				PrismAsserts.assertEquivalent("Wrong argument to addObject", expectedResource, resource);

				return oid;
			}
		});

		OperationResult result = new OperationResult("Test Operation");
		try {
			String resourceOid = controller.addObject(expectedResource, task, result);
			assertEquals(oid, resourceOid);
		} finally {
			LOGGER.debug(result.dump());

			verify(provisioning, times(1)).addObject(
					argThat(new ObjectTypeNameMatcher(expectedResourceType.getName())), any(ScriptsType.class),
					any(OperationResult.class));
		}
	}
}
