/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Almost the same as TestDummy but this is using a caseIgnore resource version.
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyCaseIgnore extends TestDummy {

	public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-case-ignore");
	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
	private MatchingRule<String> uidMatchingRule;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		uidMatchingRule = matchingRuleRegistry.getMatchingRule(StringIgnoreCaseMatchingRule.NAME, DOMUtil.XSD_STRING);
	}

	@Override
	protected File getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILE;
	}

	@Override
	protected String getWillRepoIcfName() {
		return "will";
	}

	@Override
	protected String getMurrayRepoIcfName() {
		return StringUtils.lowerCase(super.getMurrayRepoIcfName());
	}

	@Override
	protected String getBlackbeardRepoIcfName() {
		return StringUtils.lowerCase(super.getBlackbeardRepoIcfName());
	}

	@Override
	protected String getDrakeRepoIcfName() {
		return StringUtils.lowerCase(super.getDrakeRepoIcfName());
	}

	@Override
	protected MatchingRule<String> getUidMatchingRule() {
		return uidMatchingRule;
	}

	@Override
	protected boolean isAvoidDuplicateValues() {
		return true;
	}

	@Test
	public void test175SearchUidCase() throws Exception {
		final String TEST_NAME = "test175SearchUidCase";
		TestUtil.displayTestTitle(TEST_NAME);
		testSeachIterativeSingleAttrFilter(TEST_NAME,
				SchemaConstants.ICFS_UID, "wIlL", null, true,
				transformNameFromResource("Will"));
	}

	@Test
	public void test176SearchUidCaseNoFetch() throws Exception {
		final String TEST_NAME = "test176SearchUidCaseNoFetch";
		TestUtil.displayTestTitle(TEST_NAME);
		testSeachIterativeSingleAttrFilter(TEST_NAME,
				SchemaConstants.ICFS_UID, "wIlL", GetOperationOptions.createNoFetch(), false,
				transformNameFromResource("Will"));
	}

	/**
	 * Add will to the group pirates. But he is already there.
	 */
	@Test
	public void test280EntitleAccountWillPiratesAlreadyThere() throws Exception {
		final String TEST_NAME = "test280EntitleAccountWillPiratesAlreadyThere";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		DummyGroup groupPirates = getDummyGroup(GROUP_PIRATES_NAME, piratesIcfUid);
		groupPirates.addMember(getWillRepoIcfName());

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				GROUP_PIRATES_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertMember(group, getWillRepoIcfName());

		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow after", shadow);
		assertEntitlementGroup(shadow, GROUP_PIRATES_OID);

		assertSteadyResource();
	}

	@Test
	public void test282DetitleAccountWillPirates() throws Exception {
		final String TEST_NAME = "test282DetitleAccountWillPirates";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = IntegrationTestTools.createDetitleDelta(ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				GROUP_PIRATES_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertNoMember(group, getWillRepoIcfName());

		// Make sure that account is still there and it has the privilege
		DummyAccount dummyAccount = getDummyAccountAssert(ACCOUNT_WILL_USERNAME, willIcfUid);
		assertNotNull("Account will is gone!", dummyAccount);

		syncServiceMock.assertNotifySuccessOnly();
		assertSteadyResource();
	}

	/**
	 * Add will to the group pirates. But he is already there - and the capitalization is wrong.
	 */
	@Test
	public void test285EntitleAccountWillPiratesAlreadyThereCaseIgnore() throws Exception {
		final String TEST_NAME = "test285EntitleAccountWillPiratesAlreadyThereCaseIgnore";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		DummyGroup groupPirates = getDummyGroup(GROUP_PIRATES_NAME, piratesIcfUid);
		groupPirates.addMember(getWillRepoIcfName().toUpperCase());

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				GROUP_PIRATES_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		IntegrationTestTools.assertGroupMember(group, getWillRepoIcfName(),true);

		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> shadow = provisioningService.getObject(ShadowType.class, ACCOUNT_WILL_OID, null, task, result);
		display("Shadow after", shadow);
		assertEntitlementGroup(shadow, GROUP_PIRATES_OID);

		assertSteadyResource();
	}

	@Test
	public void test289DetitleAccountWillPirates() throws Exception {
		final String TEST_NAME = "test289DetitleAccountWillPirates";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = IntegrationTestTools.createDetitleDelta(ACCOUNT_WILL_OID,
				dummyResourceCtl.getAttributeQName(DummyResourceContoller.DUMMY_ENTITLEMENT_GROUP_NAME),
				GROUP_PIRATES_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		delta.checkConsistence();
		DummyGroup group = getDummyGroupAssert(GROUP_PIRATES_NAME, piratesIcfUid);
		assertNoMember(group, getWillRepoIcfName());
		assertNoMember(group, getWillRepoIcfName().toUpperCase());
		assertNoMember(group, getWillRepoIcfName().toLowerCase());

		// Make sure that account is still there and it has the privilege
		DummyAccount dummyAccount = getDummyAccountAssert(ACCOUNT_WILL_USERNAME, willIcfUid);
		assertNotNull("Account will is gone!", dummyAccount);

		syncServiceMock.assertNotifySuccessOnly();
		assertSteadyResource();
	}

	@Test
	public void test511AddProtectedAccountCaseIgnore() throws Exception {
		final String TEST_NAME = "test511AddProtectedAccountCaseIgnore";
		TestUtil.displayTestTitle(TEST_NAME);
		// GIVEN
		testAddProtectedAccount(TEST_NAME, "xaxa");
		testAddProtectedAccount(TEST_NAME, "somebody-ADM");
		testAddProtectedAccount(TEST_NAME, "everybody-AdM");
	}
}
