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
package com.evolveum.midpoint.provisioning.test.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.List;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.ProvisioningTestUtil;
import com.evolveum.midpoint.provisioning.ucf.impl.ConnectorFactoryIcfImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Almost the same as TestDummy but this is using a UUID as ICF UID.
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = "classpath:ctx-provisioning-test-main.xml")
@DirtiesContext
public class TestDummyUuidNonUniqueName extends TestDummyUuid {
	
	public static final String TEST_DIR = "src/test/resources/impl/dummy-uuid-nonunique-name/";
	public static final String RESOURCE_DUMMY_FILENAME = TEST_DIR + "resource-dummy.xml";
	
	public static final String ACCOUNT_FETTUCINI_NAME = "fettucini";
	public static final File ACCOUNT_FETTUCINI_ALFREDO_FILE = new File(TEST_DIR, "account-alfredo-fettucini.xml");
	public static final String ACCOUNT_FETTUCINI_ALFREDO_OID = "c0c010c0-d34d-b44f-f11d-444400009ffa";
	public static final String ACCOUNT_FETTUCINI_ALFREDO_FULLNAME = "Alfredo Fettucini";
	public static final File ACCOUNT_FETTUCINI_BILL_FILE = new File(TEST_DIR, "account-bill-fettucini.xml");
	public static final String ACCOUNT_FETTUCINI_BILL_OID = "c0c010c0-d34d-b44f-f11d-444400009ffb";
	public static final String ACCOUNT_FETTUCINI_BILL_FULLNAME = "Bill Fettucini";
	public static final String ACCOUNT_FETTUCINI_CARLO_FULLNAME = "Carlo Fettucini";

	@Override
	protected String getResourceDummyFilename() {
		return RESOURCE_DUMMY_FILENAME;
	}
	
	@Override
	protected boolean isNameUnique() {
		return false;
	}
	
	@Test
	public void test770AddAccountFettuciniAlfredo() throws Exception {
		final String TEST_NAME = "test770AddAccountFettuciniAlfredo";
		TestUtil.displayTestTile(TEST_NAME);
		addFettucini(TEST_NAME, ACCOUNT_FETTUCINI_ALFREDO_FILE, ACCOUNT_FETTUCINI_ALFREDO_OID, ACCOUNT_FETTUCINI_ALFREDO_FULLNAME);
		searchFettucini(1);
	}
	
	@Test
	public void test772AddAccountFettuciniBill() throws Exception {
		final String TEST_NAME = "test772AddAccountFettuciniBill";
		TestUtil.displayTestTile(TEST_NAME);
		addFettucini(TEST_NAME, ACCOUNT_FETTUCINI_BILL_FILE, ACCOUNT_FETTUCINI_BILL_OID, ACCOUNT_FETTUCINI_BILL_FULLNAME);
		searchFettucini(2);
	}
	
	/**
	 * Add directly on resource. Therefore provisioning must create the shadow during search.
	 */
	@Test
	public void test774AddAccountFettuciniCarlo() throws Exception {
		final String TEST_NAME = "test774AddAccountFettuciniCarlo";
		TestUtil.displayTestTile(TEST_NAME);
		dummyResourceCtl.addAccount(ACCOUNT_FETTUCINI_NAME, ACCOUNT_FETTUCINI_CARLO_FULLNAME);
		searchFettucini(3);
	}
	
	@Override
	@Test
	public void test600AddAccountAlreadyExist() throws Exception {
		// DO nothing. This test is meaningless in non-unique environment
	}

	private String addFettucini(final String TEST_NAME, File file, String oid, String expectedFullName) throws SchemaException, ObjectAlreadyExistsException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, IOException {
		// GIVEN
		Task task = taskManager.createTaskInstance(TestDummy.class.getName()
				+ "." + TEST_NAME);
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ "." + TEST_NAME);
		syncServiceMock.reset();

		PrismObject<ShadowType> account = prismContext.parseObject(file);
		account.checkConsistence();

		display("Adding shadow", account);

		// WHEN
		String addedObjectOid = provisioningService.addObject(account, null, null, task, result);

		// THEN
		result.computeStatus();
		display("add object result", result);
		TestUtil.assertSuccess("addObject has failed (result)", result);
		assertEquals(oid, addedObjectOid);

		account.checkConsistence();

		PrismObject<ShadowType> accountRepo = repositoryService.getObject(ShadowType.class, oid, null, result);
		display("Account repo", accountRepo);
		ShadowType accountTypeRepo = accountRepo.asObjectable();
		PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_FETTUCINI_NAME, accountTypeRepo.getName());
		assertEquals("Wrong kind (repo)", ShadowKindType.ACCOUNT, accountTypeRepo.getKind());
		assertAttribute(accountTypeRepo, ConnectorFactoryIcfImpl.ICFS_NAME, ACCOUNT_FETTUCINI_NAME);
		String icfUid = getIcfUid(accountRepo);
		
		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
				oid, null, task, result);
		display("Account provisioning", accountProvisioning);
		ShadowType accountTypeProvisioning = accountProvisioning.asObjectable();
		display("account from provisioning", accountTypeProvisioning);
		PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_FETTUCINI_NAME, accountTypeProvisioning.getName());
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, accountTypeProvisioning.getKind());
		assertAttribute(accountTypeProvisioning, ConnectorFactoryIcfImpl.ICFS_NAME, ACCOUNT_FETTUCINI_NAME);
		assertAttribute(accountTypeProvisioning, ConnectorFactoryIcfImpl.ICFS_UID, icfUid);

		// Check if the account was created in the dummy resource
		DummyAccount dummyAccount = getDummyAccountAssert(ACCOUNT_FETTUCINI_NAME, icfUid);
		assertNotNull("No dummy account", dummyAccount);
		assertEquals("Fullname is wrong", expectedFullName, dummyAccount.getAttributeValue("fullname"));

		// Check if the shadow is still in the repo (e.g. that the consistency or sync haven't removed it)
		PrismObject<ShadowType> shadowFromRepo = repositoryService.getObject(ShadowType.class,
				addedObjectOid, null, result);
		assertNotNull("Shadow was not created in the repository", shadowFromRepo);
		display("Repository shadow", shadowFromRepo.debugDump());

		ProvisioningTestUtil.checkRepoAccountShadow(shadowFromRepo);

		checkConsistency(accountProvisioning);
		assertSteadyResource();
		
		return icfUid;
	}
	
	private void searchFettucini(int expectedNumberOfFettucinis) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		OperationResult result = new OperationResult(TestDummy.class.getName()
				+ ".searchFettucini");
		
		ObjectFilter filter = AndFilter.createAnd(
					RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, resource), 
					EqualFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, prismContext, null,
							new QName(dummyResourceCtl.getNamespace(), "AccountObjectClass")),
					EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, getIcfNameDefinition().getName()), 
							getIcfNameDefinition(), new PrismPropertyValue(ACCOUNT_FETTUCINI_NAME)));
		ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		// WHEN
		List<PrismObject<ShadowType>> shadows = provisioningService.searchObjects(ShadowType.class, query, null, result);
		assertEquals("Wrong number of Fettucinis found", expectedNumberOfFettucinis, shadows.size());
	}

	private PrismPropertyDefinition<String> getIcfNameDefinition() {
		return new PrismPropertyDefinition<String>(ConnectorFactoryIcfImpl.ICFS_NAME, 
				DOMUtil.XSD_STRING, prismContext);
	}
		
}
