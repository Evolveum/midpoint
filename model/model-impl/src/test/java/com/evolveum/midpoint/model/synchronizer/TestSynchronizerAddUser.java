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

import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;
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

import java.io.File;
import java.io.IOException;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/**
 * 
 * @author lazyman
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-configuration-test-no-repo.xml",
		"classpath:application-context-model-unit-test.xml", "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml" })
public class TestSynchronizerAddUser extends AbstractTestNGSpringContextTests {

	private static final File TEST_FOLDER = new File("./src/test/resources/controller/addUser");
	private static final File TEST_FOLDER_COMMON = new File("./src/test/resources/common");

	private static final Trace LOGGER = TraceManager.getTrace(TestSynchronizerAddUser.class);
	
	@Autowired(required = true)
	private UserSynchronizer userSynchronizer;
	
	@Autowired(required=true)
	private PrismContext prismContext;
	
	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	private RepositoryService repository;
	
	@Autowired(required = true)
	private ProvisioningService provisioning;
	
	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		DebugUtil.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@BeforeMethod
	public void before() {
		Mockito.reset(provisioning, repository);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void addUserWithSimpleTemplate() throws Exception {
		PrismObject<UserType> user = PrismTestUtil.parseObject(new File(TEST_FOLDER_COMMON, "user-drake.xml"));
		UserType userType = user.asObjectable();
		PrismObject<UserTemplateType> userTemplate = PrismTestUtil.parseObject(new File(TEST_FOLDER_COMMON, "user-template.xml"));
		UserTemplateType userTemplateType = userTemplate.asObjectable();
		PrismObject<ResourceType> resource = PrismTestUtil.parseObject(new File(TEST_FOLDER_COMMON, "resource-opendj.xml"));
		ResourceType resourceType = resource.asObjectable();

		final String userOid = "10000000-0000-0000-0000-000000000001";
		final String resourceOid = "10000000-0000-0000-0000-000000000003";
		final String accountOid = "10000000-0000-0000-0000-000000000004";

		when(
				provisioning.getObject(eq(ResourceType.class), eq(resourceOid),
						any(PropertyReferenceListType.class), any(OperationResult.class))).thenReturn(
				resourceType.asPrismObject());
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
		objectDelta.setObjectToAdd(user);
		
		syncContext.setUserOld(null);
		syncContext.setUserNew(user);
		syncContext.setUserPrimaryDelta(objectDelta);
		
		syncContext.setUserTemplate(userTemplateType);

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
