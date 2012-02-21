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
package com.evolveum.midpoint.model.synchronizer;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;

import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
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

import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.controller.ModelController;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.PrismAsserts;
import com.evolveum.midpoint.test.util.PrismTestUtil;
import com.evolveum.midpoint.test.util.XmlAsserts;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * 
 * @author lazyman
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-configuration-test-no-repo.xml",
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml" })
public class TestSynchronizerAddUser extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/addUser");
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");

	private static final Trace LOGGER = TraceManager.getTrace(TestSynchronizerAddUser.class);
	
	@Autowired(required = true)
	private UserSynchronizer userSynchronizer;
	
	@Autowired(required=true)
	private SchemaRegistry schemaRegistry;
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	
	@Autowired(required = true)
	private ProvisioningService provisioning;

	@BeforeMethod
	public void before() {
		Mockito.reset(provisioning, repository);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void addUserWithSimpleTemplate() throws Exception {
		UserType userType = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER_COMMON, "user-drake.xml"));
		UserTemplateType userTemplate = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER_COMMON, "user-template.xml"));
		ResourceType resource = PrismTestUtil.unmarshalObject(new File(TEST_FOLDER_COMMON, "resource.xml"));

		final String userOid = "10000000-0000-0000-0000-000000000001";
		final String resourceOid = "10000000-0000-0000-0000-000000000003";
		final String accountOid = "10000000-0000-0000-0000-000000000004";

		when(
				provisioning.getObject(eq(ResourceType.class), eq(resourceOid),
						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
				resource.asPrismObject());
		when(
				provisioning.addObject(any(PrismObject.class), any(ScriptsType.class),
						any(OperationResult.class))).thenAnswer(new Answer<String>() {
			@Override
			public String answer(InvocationOnMock invocation) throws Throwable {
				AccountShadowType account = (AccountShadowType) invocation.getArguments()[0];
				PrismAsserts.assertEquals(new File(TEST_FOLDER, "expected-account.xml"), account);

				return accountOid;
			}
		});
		when(repository.addObject(any(PrismObject.class), any(OperationResult.class))).thenAnswer(
				new Answer<String>() {
					@Override
					public String answer(InvocationOnMock invocation) throws Throwable {
						UserType user = (UserType) invocation.getArguments()[0];
						PrismAsserts.assertEquals(new File(TEST_FOLDER, "expected-user.xml"), user);
						return userOid;
					}
				});

		OperationResult result = new OperationResult("Add User With Template");
		
		SyncContext syncContext = new SyncContext(PrismTestUtil.getPrismContext());

		ObjectDelta<UserType> objectDelta = new ObjectDelta<UserType>(UserType.class, ChangeType.ADD);
		PrismObject<UserType> user = userType.asPrismObject();
		objectDelta.setObjectToAdd(user);
		
		syncContext.setUserOld(null);
		syncContext.setUserNew((PrismObject<UserType>) user);
		syncContext.setUserPrimaryDelta((ObjectDelta<UserType>) objectDelta);
		
		syncContext.setUserTemplate(userTemplate);

		try {
			LOGGER.info("provisioning: " + provisioning.getClass());
			LOGGER.info("repo" + repository.getClass());
						
			// WHEN
			userSynchronizer.synchronizeUser(syncContext, result);
			
		} finally {
			LOGGER.info(result.dump());
		}
		
		// THEN
		
		display("Context after sync",syncContext);
		
		// TODO
	}
}
