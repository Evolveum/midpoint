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
package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.provisioning.impl.ProvisioningTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
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

	public static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-uuid-nonunique-name");
	public static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

	public static final String ACCOUNT_FETTUCINI_NAME = "fettucini";
	public static final File ACCOUNT_FETTUCINI_ALFREDO_FILE = new File(TEST_DIR, "account-alfredo-fettucini.xml");
	public static final String ACCOUNT_FETTUCINI_ALFREDO_OID = "c0c010c0-d34d-b44f-f11d-444400009ffa";
	public static final String ACCOUNT_FETTUCINI_ALFREDO_FULLNAME = "Alfredo Fettucini";
	public static final File ACCOUNT_FETTUCINI_BILL_FILE = new File(TEST_DIR, "account-bill-fettucini.xml");
	public static final String ACCOUNT_FETTUCINI_BILL_OID = "c0c010c0-d34d-b44f-f11d-444400009ffb";
	public static final String ACCOUNT_FETTUCINI_BILL_FULLNAME = "Bill Fettucini";
	public static final String ACCOUNT_FETTUCINI_CARLO_FULLNAME = "Carlo Fettucini";

	@Override
	protected File getResourceDummyFile() {
		return RESOURCE_DUMMY_FILE;
	}

	@Override
	protected boolean isNameUnique() {
		return false;
	}

	@Test
	public void test770AddAccountFettuciniAlfredo() throws Exception {
		final String TEST_NAME = "test770AddAccountFettuciniAlfredo";
		TestUtil.displayTestTitle(TEST_NAME);
		addFettucini(TEST_NAME, ACCOUNT_FETTUCINI_ALFREDO_FILE, ACCOUNT_FETTUCINI_ALFREDO_OID, ACCOUNT_FETTUCINI_ALFREDO_FULLNAME);
		searchFettucini(1);
	}

	@Test
	public void test772AddAccountFettuciniBill() throws Exception {
		final String TEST_NAME = "test772AddAccountFettuciniBill";
		TestUtil.displayTestTitle(TEST_NAME);
		addFettucini(TEST_NAME, ACCOUNT_FETTUCINI_BILL_FILE, ACCOUNT_FETTUCINI_BILL_OID, ACCOUNT_FETTUCINI_BILL_FULLNAME);
		searchFettucini(2);
	}

	/**
	 * Add directly on resource. Therefore provisioning must create the shadow during search.
	 */
	@Test
	public void test774AddAccountFettuciniCarlo() throws Exception {
		final String TEST_NAME = "test774AddAccountFettuciniCarlo";
		TestUtil.displayTestTitle(TEST_NAME);
		dummyResourceCtl.addAccount(ACCOUNT_FETTUCINI_NAME, ACCOUNT_FETTUCINI_CARLO_FULLNAME);
		searchFettucini(3);
	}

	@Override
	@Test
	public void test600AddAccountAlreadyExist() throws Exception {
		// DO nothing. This test is meaningless in non-unique environment
	}

	private String addFettucini(final String TEST_NAME, File file, String oid, String expectedFullName) throws SchemaException, ObjectAlreadyExistsException, CommunicationException, ObjectNotFoundException, ConfigurationException, SecurityViolationException, IOException, SchemaViolationException, ConflictException, ExpressionEvaluationException {
		// GIVEN
		Task task = createTask(TEST_NAME);
		OperationResult result = task.getResult();
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
		assertAttribute(accountRepo, SchemaConstants.ICFS_NAME, ACCOUNT_FETTUCINI_NAME);
		String icfUid = getIcfUid(accountRepo);

		syncServiceMock.assertNotifySuccessOnly();

		PrismObject<ShadowType> accountProvisioning = provisioningService.getObject(ShadowType.class,
				oid, null, task, result);
		display("Account provisioning", accountProvisioning);
		ShadowType accountTypeProvisioning = accountProvisioning.asObjectable();
		display("account from provisioning", accountTypeProvisioning);
		PrismAsserts.assertEqualsPolyString("Name not equal", ACCOUNT_FETTUCINI_NAME, accountTypeProvisioning.getName());
		assertEquals("Wrong kind (provisioning)", ShadowKindType.ACCOUNT, accountTypeProvisioning.getKind());
		assertAttribute(accountProvisioning, SchemaConstants.ICFS_NAME, ACCOUNT_FETTUCINI_NAME);
		assertAttribute(accountProvisioning, SchemaConstants.ICFS_UID, icfUid);

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

	private void searchFettucini(int expectedNumberOfFettucinis) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Task task = createTask(TestDummy.class.getName() + ".searchFettucini");
		OperationResult result = task.getResult();
		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
				.item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
				.and().item(ShadowType.F_OBJECT_CLASS).eq(new QName(dummyResourceCtl.getNamespace(), "AccountObjectClass"))
				.and().itemWithDef(getIcfNameDefinition(), ShadowType.F_ATTRIBUTES, getIcfNameDefinition().getName()).eq(ACCOUNT_FETTUCINI_NAME)
				.build();

		// WHEN
		List<PrismObject<ShadowType>> shadows = provisioningService.searchObjects(ShadowType.class, query, null, task, result);
		assertEquals("Wrong number of Fettucinis found", expectedNumberOfFettucinis, shadows.size());
	}

	private PrismPropertyDefinition<String> getIcfNameDefinition() {
		return new PrismPropertyDefinitionImpl<>(SchemaConstants.ICFS_NAME,
				DOMUtil.XSD_STRING, prismContext);
	}

}
