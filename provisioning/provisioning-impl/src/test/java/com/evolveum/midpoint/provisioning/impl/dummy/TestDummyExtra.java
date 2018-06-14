/*
 * Copyright (c) 2015-2018 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Collection;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.midpoint.common.refinery.RefinedAssociationDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationProvisioningScriptsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CredentialsCapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PasswordCapabilityType;

/**
 * Almost the same as TestDummy but with some extra things, such as:
 * * readable password
 * * account-account assciations
 *
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext
public class TestDummyExtra extends TestDummy {

	public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-extra");
	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

	private static final String DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME = "mate";

	protected static final QName ASSOCIATION_CREW_NAME = new QName(RESOURCE_DUMMY_NS, "crew");

	@Override
	protected File getResourceDummyFile() {
		return RESOURCE_DUMMY_FILE;
	}

	@Override
	protected void extraDummyResourceInit() throws Exception {
		DummyObjectClass accountObjectClass = dummyResource.getAccountObjectClass();
		dummyResourceCtl.addAttrDef(accountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME, String.class, false, true);
	}

	@Override
	protected int getExpectedRefinedSchemaDefinitions() {
		return super.getExpectedRefinedSchemaDefinitions() + 1;
	}

	@Override
	protected void assertSchemaSanity(ResourceSchema resourceSchema, ResourceType resourceType) throws Exception {
		// schema is extended, displayOrders are changed
		dummyResourceCtl.assertDummyResourceSchemaSanityExtended(resourceSchema, resourceType, false, 19);

		RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
		RefinedObjectClassDefinition accountRDef = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);

		Collection<RefinedAssociationDefinition> associationDefinitions = accountRDef.getAssociationDefinitions();
		assertEquals("Wrong number of association defs", 3, associationDefinitions.size());
		RefinedAssociationDefinition crewAssociationDef = accountRDef.findAssociationDefinition(ASSOCIATION_CREW_NAME);
		assertNotNull("No definitin for crew assocation", crewAssociationDef);
	}

	@Override
	protected void assertNativeCredentialsCapability(CredentialsCapabilityType capCred) {
		PasswordCapabilityType passwordCapabilityType = capCred.getPassword();
		assertNotNull("password native capability not present", passwordCapabilityType);
		Boolean readable = passwordCapabilityType.isReadable();
		assertNotNull("No 'readable' inducation in password capability", readable);
		assertTrue("Password not 'readable' in password capability", readable);
	}

	@Test
	public void test400AddAccountElizabeth() throws Exception {
		final String TEST_NAME = "test400AddAccountElizabeth";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		PrismObject<ShadowType> account = prismContext.parseObject(ACCOUNT_ELIZABETH_FILE);
		account.checkConsistence();

		display("Adding shadow", account);

		XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();

		// WHEN
		displayWhen(TEST_NAME);
		String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();

		assertEquals(ACCOUNT_ELIZABETH_OID, addedObjectOid);

		account.checkConsistence();

		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_ELIZABETH_OID, null, task, result);

		XMLGregorianCalendar tsAfterRead = clock.currentTimeXMLGregorianCalendar();

		display("Account will from provisioning", accountProvisioning);

		DummyAccount dummyAccount = getDummyAccountAssert(ACCOUNT_ELIZABETH_USERNAME, ACCOUNT_ELIZABETH_USERNAME);
		assertNotNull("No dummy account", dummyAccount);
		assertTrue("The account is not enabled", dummyAccount.isEnabled());

		checkConsistency(accountProvisioning);
		assertSteadyResource();
	}

	/**
	 * MID-2668
	 */
	@Test
	public void test410AssociateCrewWillElizabeth() throws Exception {
		final String TEST_NAME = "test410AssociateCrewWillElizabeth";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = IntegrationTestTools.createEntitleDelta(ACCOUNT_WILL_OID,
				ASSOCIATION_CREW_NAME, ACCOUNT_ELIZABETH_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		syncServiceMock.assertNotifySuccessOnly();
		delta.checkConsistence();

		DummyAccount dummyAccountWill = getDummyAccountAssert(ACCOUNT_WILL_USERNAME, ACCOUNT_WILL_USERNAME);
		display("Dummy account will", dummyAccountWill);
		assertNotNull("No dummy account will", dummyAccountWill);
		assertTrue("The account will is not enabled", dummyAccountWill.isEnabled());
		assertDummyAttributeValues(dummyAccountWill, DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME, ACCOUNT_ELIZABETH_USERNAME);

		PrismObject<ShadowType> accountWillProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		display("Account will from provisioning", accountWillProvisioning);
		assertAssociation(accountWillProvisioning, ASSOCIATION_CREW_NAME, ACCOUNT_ELIZABETH_OID);

		assertSteadyResource();
	}

	/**
	 * MID-2668
	 */
	@Test
	public void test419DisassociateCrewWillElizabeth() throws Exception {
		final String TEST_NAME = "test419DisassociateCrewWillElizabeth";
		TestUtil.displayTestTitle(TEST_NAME);

		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();

		syncServiceMock.reset();

		ObjectDelta<ShadowType> delta = IntegrationTestTools.createDetitleDelta(ACCOUNT_WILL_OID,
				ASSOCIATION_CREW_NAME, ACCOUNT_ELIZABETH_OID, prismContext);
		display("ObjectDelta", delta);
		delta.checkConsistence();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		provisioningService.modifyObject(ShadowType.class, delta.getOid(), delta.getModifications(),
				new OperationProvisioningScriptsType(), null, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		display("modifyObject result", result);
		TestUtil.assertSuccess(result);

		syncServiceMock.assertNotifySuccessOnly();
		delta.checkConsistence();

		DummyAccount dummyAccountWill = getDummyAccountAssert(ACCOUNT_WILL_USERNAME, ACCOUNT_WILL_USERNAME);
		display("Dummy account will", dummyAccountWill);
		assertNotNull("No dummy account will", dummyAccountWill);
		assertTrue("The account will is not enabled", dummyAccountWill.isEnabled());
		assertNoDummyAttribute(dummyAccountWill, DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME);

		PrismObject<ShadowType> accountWillProvisioning = provisioningService.getObject(ShadowType.class,
				ACCOUNT_WILL_OID, null, task, result);
		display("Account will from provisioning", accountWillProvisioning);
		assertNoAssociation(accountWillProvisioning, ASSOCIATION_CREW_NAME, ACCOUNT_ELIZABETH_OID);

		assertSteadyResource();
	}

	// TODO: disassociate

	@Test
	public void test499DeleteAccountElizabeth() throws Exception {
		final String TEST_NAME = "test499DeleteAccountElizabeth";
		displayTestTitle(TEST_NAME);
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
		syncServiceMock.reset();

		// WHEN
		displayWhen(TEST_NAME);
		provisioningService.deleteObject(ShadowType.class, ACCOUNT_ELIZABETH_OID, null, null, task, result);

		// THEN
		displayThen(TEST_NAME);
		assertSuccess(result);

		syncServiceMock.assertNotifySuccessOnly();
		assertNoRepoObject(ShadowType.class, ACCOUNT_ELIZABETH_OID);

		assertNoDummyAccount(ACCOUNT_ELIZABETH_USERNAME, ACCOUNT_ELIZABETH_USERNAME);

		assertSteadyResource();
	}

	@Override
	protected void checkAccountWill(PrismObject<ShadowType> shadow, OperationResult result, XMLGregorianCalendar startTs, XMLGregorianCalendar endTs) throws SchemaException, EncryptionException {
		super.checkAccountWill(shadow, result, startTs, endTs);
		assertPassword(shadow.asObjectable(), accountWillCurrentPassword);
	}

}
