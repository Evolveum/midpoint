/*
 * Copyright (c) 2018 Evolveum
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
package com.evolveum.midpoint.testing.story;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestServiceAccounts extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "service-accounts");
	
	protected static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
	protected static final String RESOURCE_DUMMY_OID = "0069ac14-8377-11e8-b404-5b5a1a8af0db";
	private static final String RESOURCE_DUMMY_NS = MidPointConstants.NS_RI;

	private static final String ACCOUNT_RUM_STORAGE_DUMMY_USERNAME = "rum-storage";
	private static final String ACCOUNT_RUM_STORAGE_DUMMY_FULLNAME = "Rum Storage Application";

	private static final File TASK_LIVE_SYNC_DUMMY_FILE = new File(TEST_DIR, "task-dumy-livesync.xml");
	private static final String TASK_LIVE_SYNC_DUMMY_OID = "474eb3ac-837e-11e8-8cf8-6bd4fe328f30";
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
		getDummyResource().setSyncStyle(DummySyncStyle.SMART);
	}

	@Test
	public void test050StartSyncTask() throws Exception {
		final String TEST_NAME = "test050StartSyncTask";
		displayTestTitle(TEST_NAME);
		
		assertUsers(getNumberOfUsers());
		assertServices(0);
		
		// WHEN
        displayWhen(TEST_NAME);
        
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_FILE);

		// THEN
		displayThen(TEST_NAME);
		
		waitForTaskStart(TASK_LIVE_SYNC_DUMMY_OID, true);
		
		assertServices(0);
		assertUsers(getNumberOfUsers());
	}
	
	@Test
	public void test100AddServiceAccountSync() throws Exception {
		final String TEST_NAME = "test100AddServiceAccountSync";
		displayTestTitle(TEST_NAME);
		
		// Preconditions
		assertServices(0);

        DummyAccount account = new DummyAccount(ACCOUNT_RUM_STORAGE_DUMMY_USERNAME);
		account.setEnabled(true);
		account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, 
				ACCOUNT_RUM_STORAGE_DUMMY_FULLNAME);

		// WHEN
        displayWhen(TEST_NAME);

		getDummyResource().addAccount(account);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_OID, true);
		
		// THEN
		displayThen(TEST_NAME);
		
		assertServices(1);
		PrismObject<ServiceType> serviceRumAfter = findServiceByUsername(ACCOUNT_RUM_STORAGE_DUMMY_USERNAME);
		display("Service rum after", serviceRumAfter);
		assertNotNull("No rum service", serviceRumAfter);
		PrismAsserts.assertPropertyValue(serviceRumAfter, ServiceType.F_NAME, createPolyString(ACCOUNT_RUM_STORAGE_DUMMY_USERNAME));
		PrismAsserts.assertPropertyValue(serviceRumAfter, ServiceType.F_DESCRIPTION, ACCOUNT_RUM_STORAGE_DUMMY_FULLNAME);
	}
	
	// TODO: account modifications, check that the changes are synced to service
	
	/**
	 * MID-4522
	 */
	@Test
	public void test109DeleteServiceAccountSync() throws Exception {
		final String TEST_NAME = "test109DeleteServiceAccountSync";
		displayTestTitle(TEST_NAME);
		
		// Preconditions
		assertServices(1);

		// WHEN
        displayWhen(TEST_NAME);

		getDummyResource().deleteAccountByName(ACCOUNT_RUM_STORAGE_DUMMY_USERNAME);

        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_OID, true);
		
		// THEN
		displayThen(TEST_NAME);
		
		assertServices(0);
		PrismObject<ServiceType> serviceRumAfter = findServiceByUsername(ACCOUNT_RUM_STORAGE_DUMMY_USERNAME);
		display("Service rum after", serviceRumAfter);
		assertNull("Unexpected rum service", serviceRumAfter);
	}
	
}
