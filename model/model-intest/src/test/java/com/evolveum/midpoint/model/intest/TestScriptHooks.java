/*
 * Copyright (c) 2013-2017 Evolveum
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

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import com.evolveum.midpoint.model.intest.util.StaticHookRecorder;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

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

	private static final File ORG_TOP_FILE = new File(TEST_DIR, "org-top.xml");
	private static final String ORG_TOP_OID = "80808080-8888-6666-0000-100000000001";

	private static final File GENERIC_BLACK_PEARL_FILE = new File(TEST_DIR, "generic-blackpearl.xml");
	private static final String GENERIC_BLACK_PEARL_OID = "54195419-5419-5419-5419-000000000001";

	private static final File SYSTEM_CONFIGURATION_HOOKS_FILE = new File(TEST_DIR, "system-configuration-hooks.xml");

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
		importObjectFromFile(ORG_TOP_FILE);
	}

	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_HOOKS_FILE;
	}



	@Test
    public void test100JackAssignHookAccount() throws Exception {
		final String TEST_NAME = "test100JackAssignHookAccount";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestScriptHooks.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        dummyAuditService.clear();
        StaticHookRecorder.reset();

		// WHEN
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_HOOK_OID, null, task, result);

		// THEN
		assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Shadow (repo)", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyHookType);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Shadow (model)", accountModel);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyHookType);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_HOOK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        display("StaticHookRecorder", StaticHookRecorder.dump());
        StaticHookRecorder.assertInvocationCount("org", 1);
        StaticHookRecorder.assertInvocationCount("foo", 5);
        StaticHookRecorder.assertInvocationCount("bar", 5);
        StaticHookRecorder.assertInvocationCount("bar-user", 1);

        // Check audit
        display("Audit", dummyAuditService);
        dummyAuditService.assertRecords(2);
        dummyAuditService.assertSimpleRecordSanity();
        dummyAuditService.assertAnyRequestDeltas();
        dummyAuditService.assertExecutionDeltas(3);
        dummyAuditService.assertHasDelta(ChangeType.MODIFY, UserType.class);
        dummyAuditService.assertHasDelta(ChangeType.ADD, ShadowType.class);
        dummyAuditService.assertTarget(USER_JACK_OID);
        dummyAuditService.assertExecutionSuccess();
	}

	@Test
    public void test110JackAddOrganization() throws Exception {
		final String TEST_NAME = "test110JackAddOrganization";
        displayTestTitle(TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestScriptHooks.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        dummyAuditService.clear();
        StaticHookRecorder.reset();

		// WHEN
        modifyUserAdd(USER_JACK_OID, UserType.F_ORGANIZATION, task, result, createPolyString("Pirate Brethren"));

		// THEN
		assertSuccess(result);

		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("User after change execution", userJack);
		assertUserJack(userJack);
        String accountOid = getSingleLinkOid(userJack);

		// Check shadow
        PrismObject<ShadowType> accountShadow = repositoryService.getObject(ShadowType.class, accountOid, null, result);
        display("Shadow (repo)", accountShadow);
        assertAccountShadowRepo(accountShadow, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyHookType);

        // Check account
        PrismObject<ShadowType> accountModel = modelService.getObject(ShadowType.class, accountOid, null, task, result);
        display("Shadow (model)", accountModel);
        assertAccountShadowModel(accountModel, accountOid, ACCOUNT_JACK_DUMMY_USERNAME, resourceDummyHookType);

        // Check account in dummy resource
        assertDummyAccount(RESOURCE_DUMMY_HOOK_NAME, ACCOUNT_JACK_DUMMY_USERNAME, "Jack Sparrow", true);

        PrismObject<OrgType> brethrenOrg = searchObjectByName(OrgType.class, "Pirate Brethren", task, result);
        assertNotNull("Brethren org was not created", brethrenOrg);
        display("Brethren org", brethrenOrg);

        assertAssignedOrg(userJack, brethrenOrg);

        display("StaticHookRecorder", StaticHookRecorder.dump());
        StaticHookRecorder.assertInvocationCount("org", 1);
        StaticHookRecorder.assertInvocationCount("foo", 10);
        StaticHookRecorder.assertInvocationCount("bar", 10);
        StaticHookRecorder.assertInvocationCount("bar-user", 1);


        // TODO
//        // Check audit
//        display("Audit", dummyAuditService);
//        dummyAuditService.assertRecords(4);
//        dummyAuditService.assertAnyRequestDeltas();
//        dummyAuditService.assertExecutionDeltas(1,1);
//        dummyAuditService.asserHasDelta(1,ChangeType.ADD, OrgType.class);
//        dummyAuditService.assertExecutionDeltas(3,1);
//        dummyAuditService.asserHasDelta(3,ChangeType.MODIFY, UserType.class);
//        dummyAuditService.assertExecutionSuccess();
	}

}
