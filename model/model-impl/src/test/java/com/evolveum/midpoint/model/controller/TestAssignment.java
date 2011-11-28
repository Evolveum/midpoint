/*
 * Copyright (c) 2011 Evolveum
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or CDDLv1.0.txt file in the source
 * code distribution. See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * 
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.controller;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.io.File;

import javax.xml.bind.JAXBElement;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.test.XmlAsserts;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.model.test.util.ModelTUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.namespace.MidPointNamespacePrefixMapper;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-model-unit-test.xml",
		"classpath:application-context-configuration-test-no-repo.xml",
		"classpath:application-context-task.xml" })
public class TestAssignment extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/assignment/simple");
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");
	private static final Trace LOGGER = TraceManager.getTrace(TestAssignment.class);
	
	@Autowired(required = true)
	private ModelController model;
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	
	@Autowired(required = true)
	private ProvisioningService provisioning;

	@BeforeMethod
	public void before() {
		Mockito.reset(repository, provisioning);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void simpleRoleAssignment() throws Exception {
		MidPointNamespacePrefixMapper.initialize();

		ModelTUtil.mockGetSystemConfiguration(repository, new File(
				TEST_FOLDER_COMMON,"system-configuration.xml"));
		final UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER, "user.xml")))
				.getValue();
		final RoleType role = ((JAXBElement<RoleType>) JAXBUtil.unmarshal(new File(TEST_FOLDER, "role.xml")))
				.getValue();
		final ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File( TEST_FOLDER_COMMON,
				"resource.xml"))).getValue();

		when(
				repository.getObject(eq(RoleType.class), eq(role.getOid()),
						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(role);
		when(
				provisioning.getObject(eq(ResourceType.class), eq(resource.getOid()),
						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
				resource);

		when(
				provisioning.addObject(any(AccountShadowType.class), any(ScriptsType.class),
						any(OperationResult.class))).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				AccountShadowType account = (AccountShadowType) invocation.getArguments()[0];
				LOGGER.info("Created account:\n{}", JAXBUtil.silentMarshalWrap(account));
				XmlAsserts.assertPatch(new File(TEST_FOLDER, "account-expected.xml"),
						JAXBUtil.marshalWrap(account));

				return "12345678-d34d-b33f-f00d-987987987989";
			}
		});

		when(repository.addObject(any(UserType.class), any(OperationResult.class))).thenAnswer(
				new Answer<String>() {
					@Override
					public String answer(InvocationOnMock invocation) throws Throwable {
						UserType user = (UserType) invocation.getArguments()[0];
						System.out.println("XXXXXXXXX");
						System.out.println(JAXBUtil.marshalWrap(user));
						XmlAsserts.assertPatch(new File(TEST_FOLDER, "user-expected.xml"),
								JAXBUtil.marshalWrap(user));

						return "12345678-d34d-b33f-f00d-987987987988";
					}
				});

		OperationResult result = new OperationResult("Simple Role Assignment");
		try {
			
			//WHEN
			model.addObject(user, result);
			
		} finally {
			LOGGER.debug(result.dump());
		}
	}

	/*
	 * Test disabled Oct 18 2011. No point in fixing it. It is no longer compatible with model implementation.
	 * And as model implementation will be changed soon it would be a wasted work. 
	 */
	@SuppressWarnings("unchecked")
	@Test(enabled=false)
	public void accountAssignment() throws Exception {
		try {
			MidPointNamespacePrefixMapper.initialize();
			
			ModelTUtil.mockGetSystemConfiguration(repository, new File(TEST_FOLDER_COMMON, 
					"system-configuration.xml"));
			final UserType user = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
					"user-account-assignment.xml"))).getValue();
	
			final ResourceType resource = ((JAXBElement<ResourceType>) JAXBUtil.unmarshal(new File(
					TEST_FOLDER_COMMON, "resource.xml"))).getValue();
	
			when(
					provisioning.getObject(eq(ResourceType.class), eq(resource.getOid()),
							any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
					resource);
			when(
					provisioning.addObject(any(AccountShadowType.class), any(ScriptsType.class),
							any(OperationResult.class))).thenAnswer(new Answer<String>() {
				@Override
				public String answer(InvocationOnMock invocation) throws Throwable {
					AccountShadowType account = (AccountShadowType) invocation.getArguments()[0];
					LOGGER.info("Created account:\n{}", JAXBUtil.silentMarshalWrap(account));
					XmlAsserts.assertPatch(new File(TEST_FOLDER, "account-expected.xml"),
							JAXBUtil.marshalWrap(account));
	
					return "12345678-d34d-b33f-f00d-987987987989";
				}
			});
			when(repository.addObject(any(UserType.class), any(OperationResult.class))).thenAnswer(
					new Answer<String>() {
						@Override
						public String answer(InvocationOnMock invocation) throws Throwable {
							UserType returnedUser = (UserType) invocation.getArguments()[0];
							
							final UserType userExpected = ((JAXBElement<UserType>) JAXBUtil.unmarshal(new File(TEST_FOLDER,
							"user-expected.xml"))).getValue();
							userExpected.getAssignment().clear();
							userExpected.getAssignment().add(user.getAssignment().get(0));
							
//							System.out.println("XXXXXXXXXXXX");
							System.out.println(JAXBUtil.marshalWrap(returnedUser));
							
							XmlAsserts.assertPatch(JAXBUtil.marshalWrap(userExpected),
									JAXBUtil.marshalWrap(returnedUser));
	
							return "12345678-d34d-b33f-f00d-987987987988";
						}
					});
	
			OperationResult result = new OperationResult("Account Assignment");
			try {
				model.addObject(user, result);
			} finally {
				LOGGER.debug(result.dump());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
