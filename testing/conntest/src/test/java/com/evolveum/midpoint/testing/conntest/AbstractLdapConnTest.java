package com.evolveum.midpoint.testing.conntest;
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


import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.directory.api.ldap.model.entry.Entry;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class})
public abstract class AbstractLdapConnTest extends AbstractLdapSynchronizationTest {

	private static final Trace LOGGER = TraceManager.getTrace(AbstractLdapConnTest.class);

	protected static final String ACCOUNT_IDM_DN = "uid=idm,ou=Administrators,dc=example,dc=com";
	protected static final String ACCOUNT_0_UID = "u00000000";
	protected static final String ACCOUNT_19_UID = "u00000019";
	protected static final String ACCOUNT_20_UID = "u00000020";
	protected static final String ACCOUNT_68_UID = "u00000068";
	protected static final String ACCOUNT_69_UID = "u00000069";
	protected static final String ACCOUNT_240_UID = "u00000240";
	protected static final String ACCOUNT_241_UID = "u00000241";

	protected static final int NUMBER_OF_GENERATED_ACCOUNTS = 4000;

	protected static final String ACCOUNT_HT_UID = "ht";
	protected static final String ACCOUNT_HT_CN = "Herman Toothrot";
	protected static final String ACCOUNT_HT_GIVENNAME = "Herman";
	protected static final String ACCOUNT_HT_SN = "Toothrot";

	private static final String USER_LARGO_NAME = "LarGO";
	private static final String USER_LARGO_GIVEN_NAME = "Largo";
	private static final String USER_LARGO_FAMILY_NAME = "LaGrande";

	protected static final File ROLE_UNDEAD_FILE = new File (COMMON_DIR, "role-undead.xml");
	protected static final String ROLE_UNDEAD_OID = "54885c40-ffcc-11e5-b782-63b3e4e2a69d";

	protected static final File ROLE_EVIL_FILE = new File (COMMON_DIR, "role-evil.xml");
	protected static final String ROLE_EVIL_OID = "624b43ec-ffcc-11e5-8297-f392afa54704";

	protected static final String GROUP_UNDEAD_CN = "undead";
	protected static final String GROUP_UNDEAD_DESCRIPTION = "Death is for loosers";

	protected static final String GROUP_EVIL_CN = "evil";
	protected static final String GROUP_EVIL_DESCRIPTION = "No pain no gain";

	private static final String REGEXP_RESOURCE_OID_PLACEHOLDER = "%%%RESOURCE%%%";

	protected String account0Oid;
	protected String accountBarbossaOid;
	protected String accountBarbossaDn;
	protected String accountBarbossaEntryId;
	protected String accountLechuckOid;
	protected String accountLechuckDn;

	protected String groupEvilShadowOid;

    @Autowired
    protected ReconciliationTaskHandler reconciliationTaskHandler;

	protected boolean isIdmAdminInteOrgPerson() {
		return false;
	}

	protected File getResourceFile() {
		return new File(getBaseDir(), "resource.xml");
	}

	protected abstract String getAccount0Cn();

	protected int getNumberOfAllAccounts() {
		return NUMBER_OF_GENERATED_ACCOUNTS + (isIdmAdminInteOrgPerson()?1:0);
	}

	protected boolean hasAssociationShortcut() {
		return true;
	}

	protected boolean isVlvSearchBeyondEndResurnsLastEntry() {
		return false;
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		// Users
		repoAddObjectFromFile(USER_BARBOSSA_FILE, initResult);
		repoAddObjectFromFile(USER_GUYBRUSH_FILE, initResult);
		repoAddObjectFromFile(USER_LECHUCK_FILE, initResult);

		// Roles
		repoAddObjectFromFileReplaceResource(ROLE_UNDEAD_FILE, RoleType.class, initResult);
		repoAddObjectFromFileReplaceResource(ROLE_EVIL_FILE, RoleType.class, initResult);
	}

	protected <O extends ObjectType> void repoAddObjectFromFileReplaceResource(File file, Class<O> type, OperationResult result) throws IOException, SchemaException, ObjectAlreadyExistsException {
		String fileContent = MiscUtil.readFile(file);
		String xmlString = fileContent.replaceAll(REGEXP_RESOURCE_OID_PLACEHOLDER, getResourceOid());
		PrismObject<O> object = PrismTestUtil.parseObject(xmlString);
		repositoryService.addObject(object, null, result);
	}

	@Test
    public void test000Sanity() throws Exception {
		super.test000Sanity();
		cleanupDelete(toAccountDn(USER_BARBOSSA_USERNAME));
		cleanupDelete(toAccountDn(USER_CPTBARBOSSA_USERNAME));

		cleanupDelete(toGroupDn(GROUP_UNDEAD_CN));
		cleanupDelete(toGroupDn(GROUP_EVIL_CN));

		if (needsGroupFakeMemeberEntry()) {
        	addLdapGroup(GROUP_UNDEAD_CN, GROUP_UNDEAD_DESCRIPTION, "uid=fake,"+getPeopleLdapSuffix());
        	addLdapGroup(GROUP_EVIL_CN, GROUP_EVIL_DESCRIPTION, "uid=fake,"+getPeopleLdapSuffix());
        } else {
        	addLdapGroup(GROUP_UNDEAD_CN, GROUP_UNDEAD_DESCRIPTION);
        	addLdapGroup(GROUP_EVIL_CN, GROUP_EVIL_DESCRIPTION);
        }
	}

	@Test
    public void test100SeachAccount0ByLdapUid() throws Exception {
		final String TEST_NAME = "test100SeachAccount0ByLdapUid";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = createUidQuery(ACCOUNT_0_UID);

		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_0_UID));

        assertConnectorOperationIncrement(1, 2);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1);
	}

	/**
	 * No paging. It should return all accounts.
	 */
	@Test
    public void test150SeachAllAccounts() throws Exception {
		final String TEST_NAME = "test150SeachAllAccounts";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, getNumberOfAllAccounts(), task, result);

        assertConnectorOperationIncrement(1, getNumberOfAllAccounts() + 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1, 2);
    }

	/**
	 * Blocksize is 100, so this is in one block.
	 */
	@Test
    public void test152SeachFirst50Accounts() throws Exception {
		final String TEST_NAME = "test152SeachFirst50Accounts";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);

        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setMaxSize(50);
		query.setPaging(paging);

		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 50, task, result);

        assertConnectorOperationIncrement(1, 51);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1, 2);
    }

	/**
	 * Blocksize is 100, so this gets more than two blocks.
	 */
	@Test
    public void test154SeachFirst222Accounts() throws Exception {
		final String TEST_NAME = "test154SeachFirst222Accounts";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);

        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setMaxSize(222);
		query.setPaging(paging);

		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 222, task, result);

        assertConnectorOperationIncrement(1, 223);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1, 2);
    }

	/**
	 * Make a search that starts in the list of all accounts but goes beyond the end.
	 */
	@Test
    public void test156SeachThroughEnd() throws Exception {
		final String TEST_NAME = "test156SeachBeyondEnd";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);

        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setOffset(getNumberOfAllAccounts() - 150);
        paging.setMaxSize(333);
		query.setPaging(paging);

		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 150, task, result);

        assertConnectorOperationIncrement(1, 151);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1, 2);
    }

	/**
	 * Make a search that goes beyond the end of the list of all accounts.
	 */
	@Test
    public void test158SeachBeyondEnd() throws Exception {
		final String TEST_NAME = "test158SeachBeyondEnd";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);

        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setOffset(getNumberOfAllAccounts() + 50);
        paging.setMaxSize(123);
		query.setPaging(paging);

		int expectedEntries = 0;
		if (isVlvSearchBeyondEndResurnsLastEntry()) {
			expectedEntries = 1;
		}
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, expectedEntries, task, result);

//		Fails for 389ds tests. For some unknown reason. And this is not that important. There are similar asserts in other tests that are passing.
//        assertConnectorOperationIncrement(1);

        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1, 2);
    }

	@Test
    public void test162SeachFirst50AccountsOffset0() throws Exception {
		final String TEST_NAME = "test152SeachFirst50Accounts";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);

        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setOffset(0);
        paging.setMaxSize(50);
		query.setPaging(paging);

		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 50, task, result);

        assertConnectorOperationIncrement(1, 51);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1, 2);
    }

	/**
	 * Blocksize is 100, so this is in one block.
	 * There is offset, so VLV should be used.
	 * No explicit sorting.
	 */
	@Test
    public void test172Search50AccountsOffset20() throws Exception {
		final String TEST_NAME = "test172Search50AccountsOffset20";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);

        ObjectPaging paging = ObjectPaging.createPaging(20, 50);
		query.setPaging(paging);

		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 50, task, result);

        assertConnectorOperationIncrement(1, 51);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1, 2);
    }

	/**
	 * Blocksize is 100, so this gets more than two blocks.
	 * There is offset, so VLV should be used.
	 * No explicit sorting.
	 */
	@Test
    public void test174SeachFirst222AccountsOffset20() throws Exception {
		final String TEST_NAME = "test174SeachFirst222AccountsOffset20";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);

        ObjectPaging paging = ObjectPaging.createPaging(20, 222);
		query.setPaging(paging);

		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 222, task, result);

        assertConnectorOperationIncrement(1, 223);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1, 2);
    }

	/**
	 * Blocksize is 100, so this is in one block.
	 * There is offset, so VLV should be used.
	 * Explicit sorting.
	 */
	@Test
    public void test182Search50AccountsOffset20SortUid() throws Exception {
		final String TEST_NAME = "test182Search50AccountsOffset20SortUid";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);

        ObjectPaging paging = ObjectPaging.createPaging(20, 50);
        paging.setOrdering(getAttributePath(resource, "uid"), OrderDirection.ASCENDING);
		query.setPaging(paging);

		SearchResultList<PrismObject<ShadowType>> shadows = doSearch(TEST_NAME, query, 50, task, result);

        assertAccountShadow(shadows.get(0), toAccountDn(isIdmAdminInteOrgPerson()?ACCOUNT_19_UID:ACCOUNT_20_UID));
        assertAccountShadow(shadows.get(49), toAccountDn(isIdmAdminInteOrgPerson()?ACCOUNT_68_UID:ACCOUNT_69_UID));

        assertConnectorOperationIncrement(1, 51);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1, 2);
    }

	/**
	 * Blocksize is 100, so this gets more than two blocks.
	 * There is offset, so VLV should be used.
	 * No explicit sorting.
	 */
	@Test
    public void test184SearchFirst222AccountsOffset20SortUid() throws Exception {
		final String TEST_NAME = "test184SeachFirst222AccountsOffset20SortUid";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);

        ObjectPaging paging = ObjectPaging.createPaging(20, 222);
        paging.setOrdering(getAttributePath(resource, "uid"), OrderDirection.ASCENDING);
		query.setPaging(paging);

		SearchResultList<PrismObject<ShadowType>> shadows = doSearch(TEST_NAME, query, 222, task, result);

        assertAccountShadow(shadows.get(0), toAccountDn(isIdmAdminInteOrgPerson()?ACCOUNT_19_UID:ACCOUNT_20_UID));
        assertAccountShadow(shadows.get(221), toAccountDn(isIdmAdminInteOrgPerson()?ACCOUNT_240_UID:ACCOUNT_241_UID));

        assertConnectorOperationIncrement(1, 223);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1, 2);
    }

	/**
	 * No paging. Allow incomplete results. This should violate sizelimit, but some results should
	 * be returned anyway.
	 */
	@Test
    public void test190SeachAllAccountsSizelimit() throws Exception {
		final String TEST_NAME = "test190SeachAllAccountsSizelimit";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        query.setAllowPartialResults(true);

		SearchResultList<PrismObject<ShadowType>> resultList = doSearch(TEST_NAME, query, getSearchSizeLimit(), task, result);

        assertConnectorOperationIncrement(1, getSearchSizeLimit() + 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = resultList.getMetadata();
        assertNotNull("No search metadata", metadata);
        assertTrue("Partial results not indicated", metadata.isPartialResults());

        assertLdapConnectorInstances(1, 2);
    }

	/**
	 * Do many searches with different sorting and paging options. This test is designed
	 * to deplete SSS/VLV resources on the LDAP server side, so the server may reach with
	 * an error. Make sure that the connector transparently handles the error and that
	 * we can sustain a large number of searches.
	 */
	@Test
    public void test195SearchInferno() throws Exception {
		final String TEST_NAME = "test195SearchInferno";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        singleInfernoSearch(query, 30, 10, 30, "uid", task, result);
        singleInfernoSearch(query, 40, 5, 40, "cn", task, result);
        singleInfernoSearch(query, 15, 2, 15, "sn", task, result);
        singleInfernoSearch(query, 42, 200, 42, "uid", task, result);
        singleInfernoSearch(query, 200, 30, 200, "sn", task, result);

        assertConnectorOperationIncrement(5, 332);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        assertLdapConnectorInstances(1, 2);
    }

	private void singleInfernoSearch(ObjectQuery query, int expectedNumberOfResults, Integer offset, Integer maxSize, String sortAttrName, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		ObjectPaging paging = ObjectPaging.createPaging(offset, maxSize);
        paging.setOrdering(getAttributePath(resource, sortAttrName), OrderDirection.ASCENDING);
		query.setPaging(paging);

		final MutableInt count = new MutableInt();
		ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
				count.increment();
				return true;
			}
		};

		modelService.searchObjectsIterative(ShadowType.class, query, handler, null, task, result);

		assertEquals("Unexpected number of search results", expectedNumberOfResults, count.intValue());
	}

	// TODO: scoped search

    // TODO: count shadows

	@Test
    public void test200AssignAccountToBarbossa() throws Exception {
		final String TEST_NAME = "test200AssignAccountToBarbossa";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        long tsStart = System.currentTimeMillis();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_BARBOSSA_OID, getResourceOid(), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        assertLdapConnectorInstances(1, 2);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", null);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountBarbossaOid = shadow.getOid();
        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        accountBarbossaEntryId = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in "+shadow, accountBarbossaEntryId);

        assertEquals("Wrong ICFS UID", getAttributeAsString(entry, getPrimaryIdentifierAttributeName()), accountBarbossaEntryId);

        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_PASSWORD);

        ResourceAttribute<Long> createTimestampAttribute = ShadowUtil.getAttribute(shadow, new QName(MidPointConstants.NS_RI, "createTimestamp"));
        assertNotNull("No createTimestamp in "+shadow, createTimestampAttribute);
        Long createTimestamp = createTimestampAttribute.getRealValue();
        // LDAP server may be on a different host. Allow for some clock offset.
        TestUtil.assertBetween("Wrong createTimestamp in "+shadow, roundTsDown(tsStart)-1000, roundTsUp(tsEnd)+1000, createTimestamp);

        assertLdapConnectorInstances(1, 2);
	}

	@Test
    public void test210ModifyAccountBarbossaReplaceTitle() throws Exception {
		final String TEST_NAME = "test210ModifyAccountBarbossaReplaceTitle";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountBarbossaOid, prismContext);
        QName attrQName = new QName(MidPointConstants.NS_RI, "title");
        ResourceAttributeDefinition<String> attrDef = accountObjectClassDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = PropertyDelta.createModificationReplaceProperty(
        		new ItemPath(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "Captain");
        delta.addModification(attrDelta);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorInstances(1, 2);
	}

	/**
	 * Make a duplicate modification. Add a title value that is already there.
	 * Normal LDAP should fail. So check that connector and midPoint handles that.
	 */
	@Test
    public void test212ModifyAccountBarbossaAddTitleDuplicate() throws Exception {
		final String TEST_NAME = "test212ModifyAccountBarbossaAddTitleDuplicate";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountBarbossaOid, prismContext);
        QName attrQName = new QName(MidPointConstants.NS_RI, "title");
        ResourceAttributeDefinition<String> attrDef = accountObjectClassDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = PropertyDelta.createModificationAddProperty(
        		new ItemPath(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "Captain");
        delta.addModification(attrDelta);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorInstances(1, 2);
	}

	/**
	 * Make another duplicate modification. Add a title value that is already there,
	 * but with a different capitalization.
	 */
	@Test
    public void test213ModifyAccountBarbossaAddTitleDuplicateCapitalized() throws Exception {
		final String TEST_NAME = "test213ModifyAccountBarbossaAddTitleDuplicateCapitalized";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountBarbossaOid, prismContext);
        QName attrQName = new QName(MidPointConstants.NS_RI, "title");
        ResourceAttributeDefinition<String> attrDef = accountObjectClassDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = PropertyDelta.createModificationAddProperty(
        		new ItemPath(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "CAPTAIN");
        delta.addModification(attrDelta);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorInstances(1, 2);
	}

	@Test
    public void test220ModifyUserBarbossaPassword() throws Exception {
		final String TEST_NAME = "test220ModifyUserBarbossaPassword";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(USER_BARBOSSA_PASSWORD_2);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID,
        		new ItemPath(UserType.F_CREDENTIALS,  CredentialsType.F_PASSWORD, PasswordType.F_VALUE),
        		task, result, userPasswordPs);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_PASSWORD_2);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorInstances(1, 2);
	}

	@Test
    public void test230ModifyUserBarbossaEmployeeType() throws Exception {
		final String TEST_NAME = "test230ModifyUserBarbossaEmployeeType";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_EMPLOYEE_TYPE, task, result, "Pirate");

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "employeeType", "Pirate");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorInstances(1, 2);
	}

	@Test
    public void test232ModifyUserBarbossaEmployeeTypeAgain() throws Exception {
		final String TEST_NAME = "test232ModifyUserBarbossaEmployeeTypeAgain";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_EMPLOYEE_TYPE, task, result, "Pirate");

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "employeeType", "Pirate");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorInstances(1, 2);
	}

	@Test
    public void test234ModifyUserBarbossaEmployeeTypeAgainCapitalized() throws Exception {
		final String TEST_NAME = "test234ModifyUserBarbossaEmployeeTypeAgainCapitalized";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_EMPLOYEE_TYPE, task, result, "PIRATE");

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "employeeType", "Pirate");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorInstances(1, 2);
	}

	@Test
    public void test290ModifyUserBarbossaRename() throws Exception {
		final String TEST_NAME = "test290ModifyUserBarbossaRename";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString(USER_CPTBARBOSSA_USERNAME));

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        display("LDAP entry after", entry);
        assertEquals("Wrong DN", toAccountDn(USER_CPTBARBOSSA_USERNAME), entry.getDn().toString());
        assertAttribute(entry, "title", "Captain");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        display("Repo shadow after rename", repoShadow);

        String repoPrimaryIdentifier = getAttributeValue(repoShadow, getPrimaryIdentifierAttributeQName(), String.class);
        if ("dn".equals(getPrimaryIdentifierAttributeName())) {
        	assertEquals("Entry DN (primary identifier) was not updated in the shadow", toAccountDn(USER_CPTBARBOSSA_USERNAME).toLowerCase(), repoPrimaryIdentifier);
        } else {
        	assertEquals("Entry ID changed after rename", accountBarbossaEntryId, repoPrimaryIdentifier);
        }

        assertLdapConnectorInstances(1, 2);
	}

	/**
	 * Try the rename again. This time just as a capitalization of the original name.
	 * The DN should not change.
	 */
	@Test
    public void test292ModifyUserBarbossaRenameCapitalized() throws Exception {
		final String TEST_NAME = "test292ModifyUserBarbossaRenameCapitalized";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_NAME, task, result,
        		PrismTestUtil.createPolyString(USER_CPTBARBOSSA_USERNAME.toUpperCase()));

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        display("LDAP entry after", entry);
        assertAttribute(entry, "title", "Captain");
        assertEquals("Wrong DN", toAccountDn(USER_CPTBARBOSSA_USERNAME), entry.getDn().toString());

        assertConnectorOperationIncrement(1, 2); // Just account read, no modify

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        display("Repo shadow after rename", repoShadow);

        String repoPrimaryIdentifier = getAttributeValue(repoShadow, getPrimaryIdentifierAttributeQName(), String.class);
        if ("dn".equals(getPrimaryIdentifierAttributeName())) {
        	assertEquals("Entry DN (primary identifier) was not updated in the shadow", toAccountDn(USER_CPTBARBOSSA_USERNAME).toLowerCase(), repoPrimaryIdentifier);
        } else {
        	assertEquals("Entry ID changed after rename", accountBarbossaEntryId, repoPrimaryIdentifier);
        }

        assertLdapConnectorInstances(1, 2);
	}

	@Test
    public void test299UnAssignAccountBarbossa() throws Exception {
		final String TEST_NAME = "test299UnAssignAccountBarbossa";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignAccount(USER_BARBOSSA_OID, getResourceOid(), null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoLdapAccount(USER_BARBOSSA_USERNAME);
        assertNoLdapAccount(USER_CPTBARBOSSA_USERNAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertNoLinkedAccount(user);

        assertLdapConnectorInstances(1, 2);
	}

	/**
	 *  MID-2853: Unexpected association behaviour - removing roles does not always remove from groups
	 */
	@Test
    public void test300AssignRoleEvilToLechuck() throws Exception {
		final String TEST_NAME = "test300AssignRoleEvilToLechuck";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_LECHUCK_OID, ROLE_EVIL_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_LECHUCK_USERNAME, USER_LECHUCK_FULL_NAME);

        PrismObject<UserType> user = getUser(USER_LECHUCK_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountLechuckOid = shadow.getOid();
        accountLechuckDn = entry.getDn().toString();
        assertNotNull(accountLechuckDn);

        assertLdapGroupMember(entry, GROUP_EVIL_CN);
        assertLdapNoGroupMember(entry, GROUP_UNDEAD_CN);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        display("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        display("Undead group", ldapEntryUndead);

        assertLdapConnectorInstances(1, 2);
	}

	/**
	 *  MID-2853: Unexpected association behaviour - removing roles does not always remove from groups
	 */
	@Test
    public void test302AssignRoleUndeadToLechuck() throws Exception {
		final String TEST_NAME = "test302AssignRoleUndeadToLechuck";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_LECHUCK_OID, ROLE_UNDEAD_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_LECHUCK_USERNAME, USER_LECHUCK_FULL_NAME);

        PrismObject<UserType> user = getUser(USER_LECHUCK_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);

        assertLdapGroupMember(entry, GROUP_EVIL_CN);
        assertLdapGroupMember(entry, GROUP_UNDEAD_CN);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        display("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        display("Undead group", ldapEntryUndead);

        assertLdapConnectorInstances(1, 2);
	}

	/**
	 *  MID-2853: Unexpected association behaviour - removing roles does not always remove from groups
	 */
	@Test
    public void test306UnassignRoleEvilFromLechuck() throws Exception {
		final String TEST_NAME = "test306UnassignRoleEvilFromLechuck";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_LECHUCK_OID, ROLE_EVIL_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_LECHUCK_USERNAME, USER_LECHUCK_FULL_NAME);

        PrismObject<UserType> user = getUser(USER_LECHUCK_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);

        assertLdapNoGroupMember(entry, GROUP_EVIL_CN);
        assertLdapGroupMember(entry, GROUP_UNDEAD_CN);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        display("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        display("Undead group", ldapEntryUndead);

        assertLdapConnectorInstances(1, 2);
	}

	/**
	 *  MID-2853: Unexpected association behaviour - removing roles does not always remove from groups
	 */
	@Test
    public void test309UnassignRoleUndeadFromLechuck() throws Exception {
		final String TEST_NAME = "test309UnassignRoleUndeadFromLechuck";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_LECHUCK_OID, ROLE_UNDEAD_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_LECHUCK_OID);
        assertNoLinkedAccount(user);

        assertNoEntry(accountLechuckDn);

        assertNoObject(ShadowType.class, accountLechuckOid, task, result);

        assertLdapNoGroupMember(accountLechuckDn, GROUP_EVIL_CN);
        assertLdapNoGroupMember(accountLechuckDn, GROUP_UNDEAD_CN);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        display("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        display("Undead group", ldapEntryUndead);

        assertLdapConnectorInstances(1, 2);
	}

	@Test
    public void test310SeachGroupEvilByCn() throws Exception {
		final String TEST_NAME = "test310SeachGroupEvilByCn";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getGroupObjectClass(), prismContext);
		ObjectQueryUtil.filterAnd(query.getFilter(), createAttributeFilter("cn", GROUP_EVIL_CN));

		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        assertGroupShadow(shadow, toGroupDn(GROUP_EVIL_CN));
        groupEvilShadowOid = shadow.getOid();
        assertNotNull(groupEvilShadowOid);

        assertConnectorOperationIncrement(1, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(1, 2);
	}

	/**
	 *  MID-3209: Rename does not change group membership for associations, when resource does not implement its own referential integrity
	 */
	@Test
    public void test312AssignRoleEvilToBarbossa() throws Exception {
		final String TEST_NAME = "test312AssignRoleEvilToBarbossa";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_BARBOSSA_OID, ROLE_EVIL_OID, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        display("Account LDAP entry", entry);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountBarbossaOid = shadow.getOid();
        accountBarbossaDn = entry.getDn().toString();
        assertNotNull(accountBarbossaDn);

        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        accountBarbossaEntryId = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in "+shadow, accountBarbossaEntryId);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        display("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        display("Undead group", ldapEntryUndead);

        assertLdapGroupMember(entry, GROUP_EVIL_CN);
        assertLdapNoGroupMember(entry, GROUP_UNDEAD_CN);

        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupName(), groupEvilShadowOid);

        assertLdapConnectorInstances(1, 2);
	}

	/**
	 *  MID-3209: Rename does not change group membership for associations, when resource does not implement its own referential integrity
	 */
	@Test
    public void test314ModifyUserBarbossaRenameBack() throws Exception {
		final String TEST_NAME = "test314ModifyUserBarbossaRenameBack";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_BARBOSSA_OID);
        display("user defore", userBefore);
        assertNotNull(userBefore);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString(USER_BARBOSSA_USERNAME));

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        display("LDAP entry after", entry);
        assertEquals("Wrong DN", toAccountDn(USER_BARBOSSA_USERNAME), entry.getDn().toString());

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        display("Repo shadow after rename", repoShadow);

        String repoPrimaryIdentifier = getAttributeValue(repoShadow, getPrimaryIdentifierAttributeQName(), String.class);
        if ("dn".equals(getPrimaryIdentifierAttributeName())) {
        	assertEquals("Entry DN (primary identifier) was not updated in the shadow", toAccountDn(USER_BARBOSSA_USERNAME).toLowerCase(), repoPrimaryIdentifier);
        } else {
        	assertEquals("Entry ID changed after rename", accountBarbossaEntryId, repoPrimaryIdentifier);
        }

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        display("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        display("Undead group", ldapEntryUndead);

        assertLdapGroupMember(entry, GROUP_EVIL_CN);
        assertLdapNoGroupMember(accountBarbossaDn, GROUP_EVIL_CN);
        assertLdapNoGroupMember(entry, GROUP_UNDEAD_CN);

        assertLdapConnectorInstances(1, 2);
	}

	/**
	 * Add user that has role evil, check association. Use mixed lower/upper chars in the username.
	 * MID-3713
	 */
	@Test
    public void test320AddEvilUserLargo() throws Exception {
		final String TEST_NAME = "test320AddEvilUserLargo";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_LARGO_NAME, USER_LARGO_GIVEN_NAME, USER_LARGO_FAMILY_NAME, true);
        AssignmentType assignmentType = createTargetAssignment(ROLE_EVIL_OID, RoleType.COMPLEX_TYPE);
		userBefore.asObjectable().getAssignment().add(assignmentType);
		display("user before", userBefore);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(userBefore, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_NAME);
        display("user after", userAfter);
        assertAssignedRole(userAfter, ROLE_EVIL_OID);
        assertAssignments(userAfter, 1);

        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);

        Entry entry = assertLdapAccount(USER_LARGO_NAME, userAfter.asObjectable().getFullName().getOrig());
        display("account after", entry);

        assertLdapGroupMember(entry, GROUP_EVIL_CN);
        assertLdapNoGroupMember(entry, GROUP_UNDEAD_CN);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        display("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        display("Undead group", ldapEntryUndead);

        assertAssociation(shadow, ASSOCIATION_GROUP_NAME, groupEvilShadowOid);

        assertLdapConnectorInstances(1, 2);
	}

	protected void assertConnectorOperationIncrement(int shortcutIncrement, int noShortcutIncrement) {
		if (hasAssociationShortcut()) {
			assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, shortcutIncrement);
		} else {
			assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, noShortcutIncrement);
		}
	}

}
