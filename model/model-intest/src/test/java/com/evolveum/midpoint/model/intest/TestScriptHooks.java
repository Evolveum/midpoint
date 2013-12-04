/*
 * Copyright (c) 2013 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.prism.xml.ns._public.types_2.PolyStringType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.model.intest.sync.AbstractSynchronizationStoryTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TimeIntervalStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestScriptHooks extends AbstractInitializedModelIntegrationTest {

	private static final File TEST_DIR = new File("src/test/resources/scripthooks");
	
	protected static final File RESOURCE_DUMMY_HOOK_FILE = new File(TEST_DIR, "resource-dummy-hook.xml");
	protected static final String RESOURCE_DUMMY_HOOK_OID = "10000000-0000-0000-0000-000004444001";
	protected static final String RESOURCE_DUMMY_HOOK_NAME = "hook";
	protected static final String RESOURCE_DUMMY_HOOK_NAMESPACE = MidPointConstants.NS_RI;

	private static final File GENERIC_BLACK_PEARL_FILE = new File(TEST_DIR, "generic-blackpearl.xml");
	private static final String GENERIC_BLACK_PEARL_OID = "54195419-5419-5419-5419-000000000001";
		
	protected DummyResource dummyResourceHook;
	protected DummyResourceContoller dummyResourceCtlHook;
	protected ResourceType resourceDummyHookType;
	protected PrismObject<ResourceType> resourceDummyHook;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		
		dummyResourceCtlHook = DummyResourceContoller.create(RESOURCE_DUMMY_HOOK_NAME, resourceDummyHook);
		dummyResourceCtlHook.extendSchemaPirate();
		dummyResourceHook = dummyResourceCtlHook.getDummyResource();
		resourceDummyHook = importAndGetObjectFromFile(ResourceType.class, RESOURCE_DUMMY_HOOK_FILE, RESOURCE_DUMMY_HOOK_OID, initTask, initResult); 
		resourceDummyHookType = resourceDummyHook.asObjectable();
		dummyResourceCtlHook.setResource(resourceDummyHook);
		
		importObjectFromFile(GENERIC_BLACK_PEARL_FILE);
	}

	@Test
    public void test100JackAssignHookAccount() throws Exception {
		final String TEST_NAME = "test100JackAssignHookAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestScriptHooks.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);        
        dummyAuditService.clear();
                
		// WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_HOOK_OID, null, task, result);
		
		// THEN
		result.computeStatus();
        TestUtil.assertSuccess(result);
        
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleUserAccountRef(userJack);
        
		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Shadow (repo)", accountShadow);
        assertShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyHookType);
        
        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Shadow (model)", accountModel);
        assertShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyHookType);
        
        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_HOOK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);
        
        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.asserHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.asserHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();
	}
			
}
