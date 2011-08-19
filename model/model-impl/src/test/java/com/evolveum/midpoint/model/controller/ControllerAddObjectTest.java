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
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.common.test.XmlAsserts;
import com.evolveum.midpoint.model.test.util.ModelTUtil;
import com.evolveum.midpoint.model.test.util.mock.ObjectTypeNameMatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.exception.CommunicationException;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
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
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml" })
public class ControllerAddObjectTest extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/addObject");
	private static final Trace LOGGER = TraceManager.getTrace(ControllerAddObjectTest.class);
	@Autowired(required = true)
	private ModelController controller;
	@Autowired(required = true)
	private RepositoryService repository;
	@Autowired(required = true)
	private ProvisioningService provisioning;

	@BeforeMethod
	public void before() {
		Mockito.reset(provisioning, repository);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullObject() throws Exception {
		controller.addObject(null, new OperationResult("Test Operation"));
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void nullResult() throws Exception {
		controller.addObject(new UserType(), null);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	@SuppressWarnings("unchecked")
	public void addUserWithoutName() throws Exception {
		final UserType expectedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"add-user-without-name.xml"))).getValue();

		OperationResult result = new OperationResult("Test Operation");
		try {
			controller.addObject(expectedUser, result);
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
		ModelTUtil.mockGetSystemConfiguration(repository, new File(TEST_FOLDER, "system-configuration.xml"));

		final UserType expectedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"add-user-correct.xml"))).getValue();

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(
				repository.addObject(argThat(new ObjectTypeNameMatcher(expectedUser.getName())),
						any(OperationResult.class))).thenAnswer(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				UserType user = (UserType) invocation.getArguments()[0];
				XmlAsserts.assertPatch(new File(TEST_FOLDER, "add-user-correct.xml"),
						JAXBUtil.marshalWrap(user));

				return oid;
			}
		});

		OperationResult result = new OperationResult("Test Operation");
		String userOid = controller.addObject(expectedUser, result);
		LOGGER.debug(result.dump());

		verify(repository, times(1)).addObject(argThat(new ObjectTypeNameMatcher(expectedUser.getName())),
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
		ModelTUtil.mockGetSystemConfiguration(repository, new File(TEST_FOLDER, "system-configuration.xml"));

		final UserType expectedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"add-user-with-oid.xml"))).getValue();
		when(repository.addObject(eq(expectedUser), any(OperationResult.class))).thenThrow(
				new ObjectAlreadyExistsException());

		OperationResult result = new OperationResult("Test Operation");
		try {
			controller.addObject(expectedUser, result);
		} finally {
			LOGGER.debug(result.dump());

			verify(repository, times(1)).addObject(
					argThat(new ObjectTypeNameMatcher(expectedUser.getName())), any(OperationResult.class));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void addUserAndCreateDefaultAccount() throws Exception {
		ModelTUtil.mockGetSystemConfiguration(repository, new File(TEST_FOLDER,
				"system-configuration-with-template.xml"));

		final String resourceOid = "10000000-0000-0000-0000-000000000003";
		ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"resource.xml"))).getValue();
		when(
				provisioning.getObject(eq(ResourceType.class), eq(resourceOid),
						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
				resource);

		final String accountOid = "10000000-0000-0000-0000-000000000004";
		when(
				provisioning.addObject(any(AccountShadowType.class), any(ScriptsType.class),
						any(OperationResult.class))).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				AccountShadowType account = (AccountShadowType) invocation.getArguments()[0];
				XmlAsserts.assertPatch(new File(TEST_FOLDER, "expected-account.xml"),
						JAXBUtil.marshalWrap(account));

				return accountOid;
			}
		});

		final String oid = "abababab-abab-abab-abab-000000000001";
		when(repository.addObject(any(UserType.class), any(OperationResult.class))).thenAnswer(
				new Answer<String>() {
					@Override
					public String answer(InvocationOnMock invocation) throws Throwable {
						UserType user = (UserType) invocation.getArguments()[0];
						XmlAsserts.assertPatch(
								new File(TEST_FOLDER, "expected-add-user-default-accounts.xml"),
								JAXBUtil.marshalWrap(user));

						return oid;
					}
				});

		OperationResult result = new OperationResult("Test Operation");
		final UserType addedUser = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
				"add-user-default-accounts.xml"))).getValue();
		String userOid = controller.addObject(addedUser, result);
		LOGGER.debug(result.dump());

		verify(provisioning, atLeast(1)).getObject(eq(ResourceType.class), eq(resourceOid),
				any(PropertyReferenceListType.class), any(OperationResult.class));
		verify(repository, times(1)).addObject(argThat(new ObjectTypeNameMatcher(addedUser.getName())),
				any(OperationResult.class));
		assertEquals(oid, userOid);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void addResourceCorrect() throws JAXBException, FaultMessage, ObjectAlreadyExistsException,
			SchemaException, CommunicationException, ObjectNotFoundException {
		final ResourceType expectedResource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
				TEST_FOLDER, "add-resource-correct.xml"))).getValue();

		final String oid = "abababab-abab-abab-abab-000000000002";
		when(
				provisioning.addObject(argThat(new ObjectTypeNameMatcher(expectedResource.getName())),
						any(ScriptsType.class), any(OperationResult.class))).thenAnswer(new Answer<String>() {

			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				ResourceType resource = (ResourceType) invocation.getArguments()[0];

				XmlAsserts.assertPatch(new File(TEST_FOLDER, "add-resource-correct.xml"),
						JAXBUtil.marshalWrap(resource));

				return oid;
			}
		});

		OperationResult result = new OperationResult("Test Operation");
		try {
			String resourceOid = controller.addObject(expectedResource, result);
			assertEquals(oid, resourceOid);
		} finally {
			LOGGER.debug(result.dump());

			verify(provisioning, times(1)).addObject(
					argThat(new ObjectTypeNameMatcher(expectedResource.getName())), any(ScriptsType.class),
					any(OperationResult.class));
		}
	}
}
