/**
 * Copyright (c) 2015-2016 Evolveum
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
package com.evolveum.midpoint.testing.conntest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.testing.conntest.AdUtils.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
@Listeners({com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class})
public abstract class AbstractAdLdapTest extends AbstractLdapSynchronizationTest {
	
	protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ad-ldap");
	
	protected static final File ROLE_PIRATES_FILE = new File(TEST_DIR, "role-pirate.xml");
	protected static final String ROLE_PIRATES_OID = "5dd034e8-41d2-11e5-a123-001e8c717e5b";
	
	protected static final File ROLE_META_ORG_FILE = new File(TEST_DIR, "role-meta-org.xml");
	protected static final String ROLE_META_ORG_OID = "f2ad0ace-45d7-11e5-af54-001e8c717e5b";
	
	
	
	protected static final String ACCOUNT_JACK_SAM_ACCOUNT_NAME = "jack";
	protected static final String ACCOUNT_JACK_FULL_NAME = "Jack Sparrow";
	protected static final String ACCOUNT_JACK_PASSWORD = "qwe.123";
	
	protected static final String USER_CPTBARBOSSA_FULL_NAME = "Captain Hector Barbossa";
	
	private static final String GROUP_PIRATES_NAME = "pirates";
	private static final String GROUP_MELEE_ISLAND_NAME = "Mêlée Island";
	
	protected static final int NUMBER_OF_ACCOUNTS = 25;
	private static final String ASSOCIATION_GROUP_NAME = "group";

	private static final String NS_EXTENSION = "http://whatever.com/my";
	private static final QName EXTENSION_SHOW_IN_ADVANCED_VIEW_ONLY_QNAME = new QName(NS_EXTENSION, "showInAdvancedViewOnly");
	
	private boolean allowDuplicateSearchResults = false;
	
	protected String jackAccountOid;
	protected String groupPiratesOid;
	protected long jackLockoutTimestamp;
	protected String accountBarbossaOid;
	protected String orgMeleeIslandOid;
	protected String groupMeleeOid;
	
	@Override
	public String getStartSystemCommand() {
		return null;
	}

	@Override
	public String getStopSystemCommand() {
		return null;
	}

	@Override
	protected File getBaseDir() {
		return TEST_DIR;
	}

	@Override
	protected String getSyncTaskOid() {
		return "cd1e0ff2-0099-11e5-9e22-001e8c717e5b";
	}
	
	@Override
	protected boolean useSsl() {
		return true;
	}

	@Override
	protected String getLdapSuffix() {
		return "DC=win,DC=evolveum,DC=com";
	}

	@Override
	protected String getLdapBindDn() {
		return "CN=midpoint admin1,CN=Users,DC=win,DC=evolveum,DC=com";
	}

	@Override
	protected String getLdapBindPassword() {
		return "mAZadlo911";
	}

	@Override
	protected int getSearchSizeLimit() {
		return -1;
	}
	
	@Override
	public String getPrimaryIdentifierAttributeName() {
		return "objectGUID";
	}
	
	@Override
	protected String getPeopleLdapSuffix() {
		return "CN=Users,"+getLdapSuffix();
	}
	
	@Override
	protected String getGroupsLdapSuffix() {
		return "CN=Users,"+getLdapSuffix();
	}

	@Override
	protected String getLdapAccountObjectClass() {
		return "user";
	}

	@Override
	protected String getLdapGroupObjectClass() {
		return "group";
	}

	@Override
	protected String getLdapGroupMemberAttribute() {
		return "member";
	}
	
	private QName getAssociationGroupQName() {
		return new QName(MidPointConstants.NS_RI, ASSOCIATION_GROUP_NAME);
	}
	
	@Override
	protected boolean allowDuplicateSearchResults() {
		return allowDuplicateSearchResults;
	}
	
	@Override
	protected File getSyncTaskInetOrgPersonFile() {
		return new File(getBaseDir(), "task-sync-user.xml");
	}
	
	@Override
	protected boolean isGroupMemberMandatory() {
		return false;
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		binaryAttributeDetector.addBinaryAttribute(ATTRIBUTE_OBJECT_GUID_NAME);
		binaryAttributeDetector.addBinaryAttribute(ATTRIBUTE_UNICODE_PWD_NAME);
		
		// Users
		repoAddObjectFromFile(USER_BARBOSSA_FILE, initResult);
		repoAddObjectFromFile(USER_GUYBRUSH_FILE, initResult);
		
		// Roles
		repoAddObjectFromFile(ROLE_PIRATES_FILE, initResult);
		repoAddObjectFromFile(ROLE_META_ORG_FILE, initResult);
		
	}
	
	@Test
	@Override
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);
        super.test000Sanity();
        
		assertLdapPassword(ACCOUNT_JACK_SAM_ACCOUNT_NAME, ACCOUNT_JACK_FULL_NAME, ACCOUNT_JACK_PASSWORD);
		cleanupDelete(toAccountDn(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME));
		cleanupDelete(toAccountDn(USER_CPTBARBOSSA_USERNAME, USER_CPTBARBOSSA_FULL_NAME));
		cleanupDelete(toAccountDn(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME));
		cleanupDelete(toGroupDn(GROUP_MELEE_ISLAND_NAME));
		cleanupDelete(toGroupDn(GROUP_FOOLS_CN));
	}

	@Test
	@Override
    public void test020Schema() throws Exception {
		final String TEST_NAME = "test020Schema";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        accountObjectClassDefinition = AdUtils.assertAdResourceSchema(resource, getAccountObjectClass(), prismContext);
        
        assertLdapConnectorInstances(1);
	}
	
	// test050 in subclasses
	
	@Test
    public void test100SeachJackBySamAccountName() throws Exception {
		final String TEST_NAME = "test100SeachJackBySamAccountName";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = createSamAccountNameQuery(ACCOUNT_JACK_SAM_ACCOUNT_NAME);
        
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        
        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_JACK_SAM_ACCOUNT_NAME, ACCOUNT_JACK_FULL_NAME));
        jackAccountOid = shadow.getOid();
        
        assertConnectorOperationIncrement(2);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
        assertLdapConnectorInstances(1);
	}
	
	@Test
    public void test105SeachPiratesByCn() throws Exception {
		final String TEST_NAME = "test105SeachPiratesByCn";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getGroupObjectClass(), prismContext);
		ObjectQueryUtil.filterAnd(query.getFilter(), createAttributeFilter("cn", GROUP_PIRATES_NAME));
        
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        
        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        groupPiratesOid = shadow.getOid();
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
        assertLdapConnectorInstances(1);
	}
	
	@Test
    public void test110GetJack() throws Exception {
		final String TEST_NAME = "test110GetJack";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, jackAccountOid, null, task, result);
        
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);		
        display("Shadow", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_JACK_SAM_ACCOUNT_NAME, ACCOUNT_JACK_FULL_NAME));
        jackAccountOid = shadow.getOid();
        
        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupQName(), groupPiratesOid);
        
        assertAttribute(shadow, "dn", "CN=Jack Sparrow,CN=Users,DC=win,DC=evolveum,DC=com");
        assertAttribute(shadow, "cn", ACCOUNT_JACK_FULL_NAME);
        assertAttribute(shadow, "sn", "Sparrow");
        assertAttribute(shadow, "info", "The best pirate the world has ever seen");
        assertAttribute(shadow, "sAMAccountName", ACCOUNT_JACK_SAM_ACCOUNT_NAME);
        assertAttribute(shadow, "lastLogon", 0L);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        assertLdapConnectorInstances(1);
	}
	
	
	/**
	 * No paging. It should return all accounts.
	 */
	@Test
    public void test150SeachAllAccounts() throws Exception {
		final String TEST_NAME = "test150SeachAllAccounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
        
        // WHEN
        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 
        		NUMBER_OF_ACCOUNTS, task, result);
        
        // TODO: why 11? should be 1
        assertConnectorOperationIncrement(12);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
        assertLdapConnectorInstances(2);
    }
	
	/**
	 * Blocksize is 5, so this is in one block.
	 */
	@Test
    public void test152SeachFirst2Accounts() throws Exception {
		final String TEST_NAME = "test152SeachFirst2Accounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setMaxSize(2);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 2, task, result);
                
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
        assertLdapConnectorInstances(2);
    }
	
	/**
	 * Blocksize is 5, so this gets more than two blocks.
	 */
	@Test
    public void test154SeachFirst11Accounts() throws Exception {
		final String TEST_NAME = "test154SeachFirst11Accounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setMaxSize(11);
		query.setPaging(paging);
		
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 11, task, result);
                
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
        assertLdapConnectorInstances(2);
    }
	
	@Test
    public void test162SeachFirst2AccountsOffset0() throws Exception {
		final String TEST_NAME = "test162SeachFirst2AccountsOffset0";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setOffset(0);
        paging.setMaxSize(2);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 2, task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
        assertLdapConnectorInstances(2);
    }
	
	/**
	 * Blocksize is 5, so this is in one block.
	 * There is offset, so VLV should be used.
	 * No explicit sorting.
	 */
	@Test
    public void test172Search2AccountsOffset1() throws Exception {
		final String TEST_NAME = "test172Search2AccountsOffset1";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createPaging(1, 2);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 2, task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
        assertLdapConnectorInstances(2);
    }
	
	/**
	 * Blocksize is 5, so this gets more than two blocks.
	 * There is offset, so VLV should be used.
	 * No explicit sorting.
	 */
	@Test
    public void test174SeachFirst11AccountsOffset2() throws Exception {
		final String TEST_NAME = "test174SeachFirst11AccountsOffset2";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createPaging(2, 11);
		query.setPaging(paging);
        
		allowDuplicateSearchResults = true;
		
		// WHEN
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 11, task, result);
        
		// THEN
		allowDuplicateSearchResults = false;
		
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
        assertLdapConnectorInstances(2);
    }
	
	/**
	 * Blocksize is 5, so this is in one block.
	 * There is offset, so VLV should be used.
	 * Explicit sorting.
	 */
	@Test
    public void test182Search2AccountsOffset1SortCn() throws Exception {
		final String TEST_NAME = "test182Search2AccountsOffset1SortCn";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createPaging(1, 2);
        paging.setOrdering(getAttributePath(resource, "cn"), OrderDirection.ASCENDING);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> shadows = doSearch(TEST_NAME, query, 2, task, result);
        
        assertAccountShadow(shadows.get(0), "CN=Adalbert Meduza,OU=evolveum,DC=win,DC=evolveum,DC=com");
        assertAccountShadow(shadows.get(1), "CN=Adalbert Meduza1,OU=evolveum,DC=win,DC=evolveum,DC=com");
        
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorInstances(2);
    }
		
	@Test
    public void test200AssignAccountBarbossa() throws Exception {
		final String TEST_NAME = "test200AssignAccountBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);
        
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

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", null);
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountBarbossaOid = shadow.getOid();
        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        String accountBarbossaIcfUid = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in "+shadow, accountBarbossaIcfUid);
        
        assertEquals("Wrong ICFS UID", 
        		AdUtils.formatGuidToDashedNotation(MiscUtil.binaryToHex(entry.get(getPrimaryIdentifierAttributeName()).getBytes())),
        		accountBarbossaIcfUid);
        
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD);
        
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        ResourceAttribute<Long> createTimestampAttribute = ShadowUtil.getAttribute(shadow, new QName(MidPointConstants.NS_RI, "createTimeStamp"));
        assertNotNull("No createTimestamp in "+shadow, createTimestampAttribute);
        Long createTimestamp = createTimestampAttribute.getRealValue();
        // LDAP server may be on a different host. Allow for some clock offset.
        TestUtil.assertBetween("Wrong createTimestamp in "+shadow, roundTsDown(tsStart)-120000, roundTsUp(tsEnd)+120000, createTimestamp);
        
        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test210ModifyAccountBarbossaTitle() throws Exception {
		final String TEST_NAME = "test210ModifyAccountBarbossaTitle";
        TestUtil.displayTestTile(this, TEST_NAME);

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
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
        
        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test212ModifyAccountBarbossaShowInAdvancedViewOnlyTrue() throws Exception {
		final String TEST_NAME = "test212ModifyAccountBarbossaShowInAdvancedViewOnlyTrue";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<ShadowType> delta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountBarbossaOid, prismContext);
        QName attrQName = new QName(MidPointConstants.NS_RI, "showInAdvancedViewOnly");
        ResourceAttributeDefinition<String> attrDef = accountObjectClassDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<Boolean> attrDelta = PropertyDelta.createModificationReplaceProperty(
        		new ItemPath(ShadowType.F_ATTRIBUTES, attrQName), attrDef, Boolean.TRUE);
        delta.addModification(attrDelta);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "showInAdvancedViewOnly", "TRUE");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
        
        assertLdapConnectorInstances(2);
	}
	
	/**
	 * Modify USER, test boolean value mapping.
	 */
	@Test
    public void test213ModifyUserBarbossaShowInAdvancedViewOnlyFalse() throws Exception {
		final String TEST_NAME = "test213ModifyUserBarbossaShowInAdvancedViewOnlyFalse";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        
        ObjectDelta<ShadowType> delta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountBarbossaOid, prismContext);
        QName attrQName = new QName(MidPointConstants.NS_RI, "showInAdvancedViewOnly");
        ResourceAttributeDefinition<String> attrDef = accountObjectClassDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<Boolean> attrDelta = PropertyDelta.createModificationReplaceProperty(
        		new ItemPath(ShadowType.F_ATTRIBUTES, attrQName), attrDef, Boolean.TRUE);
        delta.addModification(attrDelta);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, 
        		new ItemPath(UserType.F_EXTENSION,  EXTENSION_SHOW_IN_ADVANCED_VIEW_ONLY_QNAME), 
        		task, result, Boolean.FALSE);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "showInAdvancedViewOnly", "FALSE");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
        
        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test220ModifyUserBarbossaPassword() throws Exception {
		final String TEST_NAME = "test220ModifyUserBarbossaPassword";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue("here.There.Be.Monsters");
        
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
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, "here.There.Be.Monsters");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
        
        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test230DisableUserBarbossa() throws Exception {
		final String TEST_NAME = "test230DisableUserBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, 
        		new ItemPath(UserType.F_ACTIVATION,  ActivationType.F_ADMINISTRATIVE_STATUS), 
        		task, result, ActivationStatusType.DISABLED);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertLdapConnectorInstances(2);
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertAdministrativeStatus(user, ActivationStatusType.DISABLED);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "514");
                
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAccountDisabled(shadow);
        
        try {
        	assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, "here.There.Be.Monsters");
        	AssertJUnit.fail("Password authentication works, but it should fail");
        } catch (SecurityException e) {
        	// this is expected
        }
        
        assertLdapConnectorInstances(2);
	}

	@Test
    public void test239EnableUserBarbossa() throws Exception {
		final String TEST_NAME = "test239EnableUserBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, 
        		new ItemPath(UserType.F_ACTIVATION,  ActivationType.F_ADMINISTRATIVE_STATUS), 
        		task, result, ActivationStatusType.ENABLED);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertAdministrativeStatus(user, ActivationStatusType.ENABLED);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAccountEnabled(shadow);
        
        assertLdapConnectorInstances(2);
	}

	/**
	 * This should create account with a group. And disabled.
	 */
	@Test
    public void test250AssignGuybrushPirates() throws Exception {
		final String TEST_NAME = "test250AssignGuybrushPirates";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                
        modifyUserReplace(USER_GUYBRUSH_OID, 
        		new ItemPath(UserType.F_ACTIVATION,  ActivationType.F_ADMINISTRATIVE_STATUS), 
        		task, result, ActivationStatusType.DISABLED);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_PIRATES_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        display("Entry", entry);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "514");
        
        assertLdapGroupMember(entry, GROUP_PIRATES_NAME);
        
        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        assertAdministrativeStatus(user, ActivationStatusType.DISABLED);
        String shadowOid = getSingleLinkOid(user);
        
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupQName(), groupPiratesOid);
        assertAccountDisabled(shadow);
        
        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test255ModifyUserGuybrushPassword() throws Exception {
		final String TEST_NAME = "test255ModifyUserGuybrushPassword";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue("wanna.be.a.123");
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, 
        		new ItemPath(UserType.F_CREDENTIALS,  CredentialsType.F_PASSWORD, PasswordType.F_VALUE), 
        		task, result, userPasswordPs);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "514");

        try {
        	assertLdapPassword(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, "wanna.be.a.123");
        	AssertJUnit.fail("Password authentication works, but it should fail");
        } catch (SecurityException e) {
        	// this is expected, account is disabled
        }
        
        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test260EnableGyubrush() throws Exception {
		final String TEST_NAME = "test260EnableGyubrush";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, 
        		new ItemPath(UserType.F_ACTIVATION,  ActivationType.F_ADMINISTRATIVE_STATUS), 
        		task, result, ActivationStatusType.ENABLED);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        assertAdministrativeStatus(user, ActivationStatusType.ENABLED);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAccountEnabled(shadow);
        
        assertLdapPassword(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, "wanna.be.a.123");
        
        assertLdapConnectorInstances(2);
	}

	@Test
    public void test300AssignBarbossaPirates() throws Exception {
		final String TEST_NAME = "test300AssignBarbossaPirates";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignRole(USER_BARBOSSA_OID, ROLE_PIRATES_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        display("Entry", entry);
        assertAttribute(entry, "title", "Captain");
        
        assertLdapGroupMember(entry, GROUP_PIRATES_NAME);
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
        
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupQName(), groupPiratesOid);
        
        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test390ModifyUserBarbossaRename() throws Exception {
		final String TEST_NAME = "test390ModifyUserBarbossaRename";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_BARBOSSA_OID, UserType.F_NAME, 
        		PrismTestUtil.createPolyString(USER_CPTBARBOSSA_USERNAME));
        objectDelta.addModificationReplaceProperty(UserType.F_FULL_NAME,
        		PrismTestUtil.createPolyString(USER_CPTBARBOSSA_FULL_NAME));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_CPTBARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        display("Shadow after rename (model)", shadow);
        
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        display("Shadow after rename (repo)", repoShadow);
        
        assertNoLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        
        assertLdapConnectorInstances(2);
	}
	
	// TODO: create account with a group membership
	
	
	@Test
    public void test395UnAssignBarbossaPirates() throws Exception {
		final String TEST_NAME = "test395UnAssignBarbossaPirates";
        TestUtil.displayTestTile(this, TEST_NAME);

        // TODO: do this on another account. There is a bad interference with rename.
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignRole(USER_BARBOSSA_OID, ROLE_PIRATES_OID, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_CPTBARBOSSA_FULL_NAME);
        display("Entry", entry);
        assertAttribute(entry, "title", "Captain");
        
        assertLdapNoGroupMember(entry, GROUP_PIRATES_NAME);
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
        
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        IntegrationTestTools.assertNoAssociation(shadow, getAssociationGroupQName(), groupPiratesOid);
        
        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test399UnAssignAccountBarbossa() throws Exception {
		final String TEST_NAME = "test399UnAssignAccountBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

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

        assertNoLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertNoLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_CPTBARBOSSA_FULL_NAME);
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertNoLinkedAccount(user);
        
        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test500AddOrgMeleeIsland() throws Exception {
		final String TEST_NAME = "test500AddOrgMeleeIsland";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<OrgType> org = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(OrgType.class).instantiate();
        OrgType orgType = org.asObjectable();
        orgType.setName(new PolyStringType(GROUP_MELEE_ISLAND_NAME));
        AssignmentType metaroleAssignment = new AssignmentType();
        ObjectReferenceType metaroleRef = new ObjectReferenceType();
        metaroleRef.setOid(ROLE_META_ORG_OID);
        metaroleRef.setType(RoleType.COMPLEX_TYPE);
		metaroleAssignment.setTargetRef(metaroleRef);
		orgType.getAssignment().add(metaroleAssignment);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(org, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        orgMeleeIslandOid = org.getOid();
        Entry entry = assertLdapGroup(GROUP_MELEE_ISLAND_NAME);
        
        org = getObject(OrgType.class, orgMeleeIslandOid);
        groupMeleeOid = getSingleLinkOid(org);
        PrismObject<ShadowType> shadow = getShadowModel(groupMeleeOid);
        display("Shadow (model)", shadow);
        
        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test510AssignGuybrushMeleeIsland() throws Exception {
		final String TEST_NAME = "test510AssignGuybrushMeleeIsland";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignOrg(USER_GUYBRUSH_OID, orgMeleeIslandOid, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        
        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        
        assertLdapGroupMember(entry, GROUP_MELEE_ISLAND_NAME);

        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupQName(), groupMeleeOid);
        
        assertLdapConnectorInstances(2);
	}
	
	@Override
	protected void doAdditionalRenameModifications(LdapNetworkConnection connection) throws LdapException {
		Modification mod = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, 
				ATTRIBUTE_SAM_ACCOUNT_NAME_NAME, ACCOUNT_HTM_UID);
		connection.modify(toAccountDn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN), mod);
		display("Modified "+toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN)+" " + ATTRIBUTE_SAM_ACCOUNT_NAME_NAME + 
				" -> "+ACCOUNT_HTM_UID+": "+mod);
	}
	
	@Override
	protected String getAccountHtmCnAfterRename() {
		return ACCOUNT_HTM_CN;
	}
	
	@Override
	protected void assertAccountShadow(PrismObject<ShadowType> shadow, String dn) throws SchemaException {
		super.assertAccountShadow(shadow, dn);
		ResourceAttribute<String> primaryIdAttr = ShadowUtil.getAttribute(shadow, getPrimaryIdentifierAttributeQName());
		assertNotNull("No primary identifier ("+getPrimaryIdentifierAttributeQName()+" in "+shadow, primaryIdAttr);
		String primaryId = primaryIdAttr.getRealValue();
		assertTrue("Unexpected chars in primary ID: '"+primaryId+"'", primaryId.matches("[a-z0-9\\-]+"));
	}

	@Override
	protected Entry assertLdapAccount(String samAccountName, String cn) throws LdapException, IOException, CursorException {
		Entry entry = searchLdapAccount("(cn="+cn+")");
		assertAttribute(entry, "cn", cn);
		assertAttribute(entry, ATTRIBUTE_SAM_ACCOUNT_NAME_NAME, samAccountName);
		return entry;
	}

	@Override
	protected void assertNoLdapAccount(String uid) throws LdapException, IOException, CursorException {
		throw new UnsupportedOperationException("Boom! Cannot do this here. This is bloody AD! We need full name!");
	}
	
	protected void assertNoLdapAccount(String uid, String cn) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		List<Entry> entriesCn = ldapSearch(connection, "(cn="+cn+")");
		List<Entry> entriesSamAccountName = ldapSearch(connection, "(sAMAccountName="+uid+")");
		ldapDisconnect(connection);

		assertEquals("Unexpected number of entries for cn="+cn+": "+entriesCn, 0, entriesCn.size());
		assertEquals("Unexpected number of entries for sAMAccountName="+uid+": "+entriesSamAccountName, 0, entriesSamAccountName.size());
	}

	@Override
	protected String toAccountDn(String username) {
		throw new UnsupportedOperationException("Boom! Cannot do this here. This is bloody AD! We need full name!");
	}
	
	@Override
	protected String toAccountDn(String username, String fullName) {
		return "CN="+fullName+","+getPeopleLdapSuffix();
	}
	
	@Override
	protected Rdn toAccountRdn(String username, String fullName) {
		try {
			return new Rdn(new Ava("CN", fullName));
		} catch (LdapInvalidDnException e) {
			throw new IllegalStateException(e.getMessage(),e);
		}
	}

	protected void assertLdapPassword(String uid, String fullName, String password) throws LdapException, IOException, CursorException {
		Entry entry = getLdapAccountByCn(fullName);
		assertLdapPassword(entry, password);
	}

	protected void assertLdapPassword(String uid, String password) throws LdapException, IOException, CursorException {
		throw new UnsupportedOperationException("Boom! Cannot do this here. This is bloody AD! We need full name!");
	}

	protected ObjectQuery createSamAccountNameQuery(String samAccountName) throws SchemaException {
		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
		ObjectQueryUtil.filterAnd(query.getFilter(), createAttributeFilter(ATTRIBUTE_SAM_ACCOUNT_NAME_NAME, samAccountName));
		return query;
	}
	
	@Override
	protected Entry createAccountEntry(String uid, String cn, String givenName, String sn) throws LdapException {
		byte[] password = encodePassword("Secret.123");
		Entry entry = new DefaultEntry(toAccountDn(uid, cn),
				"objectclass", getLdapAccountObjectClass(),
				ATTRIBUTE_SAM_ACCOUNT_NAME_NAME, uid,
				"cn", cn,
				"givenName", givenName,
				"sn", sn,
				ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512",
				ATTRIBUTE_UNICODE_PWD_NAME, password);
		return entry;
	}
	
	private byte[] encodePassword(String password) {
		String quotedPassword = "\"" + password + "\"";
		try {
			return quotedPassword.getBytes("UTF-16LE");
		} catch (UnsupportedEncodingException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	@Override
	protected void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd) throws ObjectNotFoundException,
			SchemaException {
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName()+".assertSyncToken");
		Task task = taskManager.getTask(syncTaskOid, result);
		PrismProperty<String> syncTokenProperty = task.getExtensionProperty(SchemaConstants.SYNC_TOKEN);
		assertNotNull("No sync token", syncTokenProperty);
		assertNotNull("No sync token value", syncTokenProperty.getRealValue());
		assertNotNull("Empty sync token value", StringUtils.isBlank(syncTokenProperty.getRealValue()));
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}

	public <T> void assertAttribute(PrismObject<ShadowType> shadow, String attrName, T... expectedValues) {
		assertAttribute(shadow, new QName(getResourceNamespace(), attrName), expectedValues);
	}
	
	public <T> void assertAttribute(PrismObject<ShadowType> shadow, QName attrQname, T... expectedValues) {
		List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
		PrismAsserts.assertSets("attribute "+attrQname+" in " + shadow, actualValues, expectedValues);
	}
	
	protected abstract void assertAccountDisabled(PrismObject<ShadowType> shadow);
	
	protected abstract void assertAccountEnabled(PrismObject<ShadowType> shadow);
}
