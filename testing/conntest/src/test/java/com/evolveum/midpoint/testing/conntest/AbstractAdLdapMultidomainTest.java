/**
 * Copyright (c) 2015-2017 Evolveum
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

import static org.testng.AssertJUnit.assertNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
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

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.util.GeneralizedTime;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
@Listeners({com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class})
public abstract class AbstractAdLdapMultidomainTest extends AbstractLdapTest {
	
	protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ad-ldap-multidomain");
	
	protected static final File ROLE_PIRATES_FILE = new File(TEST_DIR, "role-pirate.xml");
	protected static final String ROLE_PIRATES_OID = "5dd034e8-41d2-11e5-a123-001e8c717e5b";
	
	protected static final File ROLE_SUBMISSIVE_FILE = new File(TEST_DIR, "role-submissive.xml");
	protected static final String ROLE_SUBMISSIVE_OID = "0c0c81b2-d0a1-11e5-b51e-0309a826745e";
	
	protected static final File ROLE_META_ORG_FILE = new File(TEST_DIR, "role-meta-org.xml");
	protected static final String ROLE_META_ORG_OID = "f2ad0ace-45d7-11e5-af54-001e8c717e5b";
	
	protected static final File ROLE_META_ORG_GROUP_FILE = new File(TEST_DIR, "role-meta-org-group.xml");
	protected static final String ROLE_META_ORG_GROUP_OID = "c5d3294a-0d8e-11e7-bd9d-ff848c2e7e3f";
	
	public static final String ATTRIBUTE_OBJECT_GUID_NAME = "objectGUID";
	public static final String ATTRIBUTE_SAM_ACCOUNT_NAME_NAME = "sAMAccountName";
	public static final String ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME = "userAccountControl";
	public static final QName ATTRIBUTE_USER_ACCOUNT_CONTROL_QNAME = new QName(MidPointConstants.NS_RI, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME);
	public static final String ATTRIBUTE_UNICODE_PWD_NAME = "unicodePwd";
	public static final String ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME = "msExchHideFromAddressLists";
	
	protected static final String ACCOUNT_JACK_SAM_ACCOUNT_NAME = "jack";
	protected static final String ACCOUNT_JACK_FULL_NAME = "Jack Sparrow";
	protected static final String ACCOUNT_JACK_PASSWORD = "qwe.123";
	
	protected static final String USER_CPTBARBOSSA_FULL_NAME = "Captain Hector Barbossa";
	
	private static final String GROUP_PIRATES_NAME = "pirates";
	private static final String GROUP_MELEE_ISLAND_NAME = "Mêlée Island";
	private static final String GROUP_MELEE_ISLAND_ALT_NAME = "Alternative Mêlée Island";
	private static final String GROUP_MELEE_ISLAND_PIRATES_NAME = "Mêlée Island Pirates";
	private static final String GROUP_MELEE_ISLAND_PIRATES_DESCRIPTION = "swashbuckle and loot";
	
	protected static final int NUMBER_OF_ACCOUNTS = 17;
	private static final String ASSOCIATION_GROUP_NAME = "group";

	private static final String NS_EXTENSION = "http://whatever.com/my";
	private static final QName EXTENSION_SHOW_IN_ADVANCED_VIEW_ONLY_QNAME = new QName(NS_EXTENSION, "showInAdvancedViewOnly");

	protected static final File USER_SUBMAN_FILE = new File(TEST_DIR, "user-subman.xml");
	private static final String USER_SUBMAN_OID ="910ac45a-8bd6-11e6-9122-ef88d95095f0";
	private static final String USER_SUBMAN_USERNAME = "subman";
	private static final String USER_SUBMAN_GIVEN_NAME = "Sub";
	private static final String USER_SUBMAN_FAMILY_NAME = "Man";
	private static final String USER_SUBMAN_FULL_NAME = "Sub Man";
	private static final String USER_SUBMAN_PASSWORD = "sub.123";

	private static final String USER_SUBDOG_USERNAME = "subdog";
	private static final String USER_SUBDOG_GIVEN_NAME = "Sub";
	private static final String USER_SUBDOG_FAMILY_NAME = "Dog";
	private static final String USER_SUBDOG_FULL_NAME = "Sub Dog";
	
	protected static final File USER_SUBMARINE_FILE = new File(TEST_DIR, "user-submarine.xml");
	private static final String USER_SUBMARINE_OID ="c4377f86-8be9-11e6-8ef5-c3c56ff64b09";
	private static final String USER_SUBMARINE_USERNAME = "submarine";
	private static final String USER_SUBMARINE_GIVEN_NAME = "Sub";
	private static final String USER_SUBMARINE_FAMILY_NAME = "Marine";
	private static final String USER_SUBMARINE_FULL_NAME = "Sub Marine";

	private static final String INTENT_GROUP = "group";
	private static final String INTENT_OU_TOP = "ou-top";

	private static final String USER_EMPTYHEAD_NAME = "emptyhead";
	
	private boolean allowDuplicateSearchResults = false;
	
	protected String jackAccountOid;
	protected String groupPiratesOid;
	protected long jackLockoutTimestamp;
	protected String accountBarbossaOid;
	protected String orgMeleeIslandOid;
	protected String groupMeleeIslandOid;
	protected String ouMeleeIslandOid;
	protected String roleMeleeIslandPiratesOid;
	protected String groupMeleeIslandPiratesOid;

	private String accountSubmanOid;

	private String accountSubmarineOid;
	
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
		return "DC=ad,DC=evolveum,DC=com";
	}

	@Override
	protected String getLdapBindDn() {
		return "CN=midpoint,CN=Users,DC=ad,DC=evolveum,DC=com";
	}

	@Override
	protected String getLdapBindPassword() {
		return "qwe.123";
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
	
	protected String getLdapSubSuffix() {
		return "DC=sub,DC=ad,DC=evolveum,DC=com";
	}

	protected String getPeopleLdapSubSuffix() {
		return "CN=Users,"+getLdapSubSuffix();
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
	protected boolean isGroupMemberMandatory() {
		return false;
	}
	
	protected String getOrgsLdapSuffix() {
		return "OU=Org,"+getLdapSuffix();
	}
	
	private UserLdapConnectionConfig getSubLdapConnectionConfig() {
		UserLdapConnectionConfig config = new UserLdapConnectionConfig();
		config.setLdapHost("hydra.ad.evolveum.com");
		config.setLdapPort(getLdapServerPort());
		config.setBindDn("CN=midpoint,CN=Users,DC=sub,DC=ad,DC=evolveum,DC=com");
		config.setBindPassword(getLdapBindPassword());
		config.setBaseContext(getLdapSubSuffix());
		return config;
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		binaryAttributeDetector.addBinaryAttribute(ATTRIBUTE_OBJECT_GUID_NAME);
		binaryAttributeDetector.addBinaryAttribute(ATTRIBUTE_UNICODE_PWD_NAME);
		
		// Users
		repoAddObjectFromFile(USER_BARBOSSA_FILE, initResult);
		repoAddObjectFromFile(USER_GUYBRUSH_FILE, initResult);
		repoAddObjectFromFile(USER_SUBMAN_FILE, initResult);
		repoAddObjectFromFile(USER_SUBMARINE_FILE, initResult);
		
		// Roles
		repoAddObjectFromFile(ROLE_PIRATES_FILE, initResult);
		repoAddObjectFromFile(ROLE_SUBMISSIVE_FILE, initResult);
		repoAddObjectFromFile(ROLE_META_ORG_FILE, initResult);
		repoAddObjectFromFile(ROLE_META_ORG_GROUP_FILE, initResult);
		
	}
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTile(TEST_NAME);
        
		assertLdapPassword(ACCOUNT_JACK_SAM_ACCOUNT_NAME, ACCOUNT_JACK_FULL_NAME, ACCOUNT_JACK_PASSWORD);
		cleanupDelete(toAccountDn(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME));
		cleanupDelete(toAccountDn(USER_CPTBARBOSSA_USERNAME, USER_CPTBARBOSSA_FULL_NAME));
		cleanupDelete(toAccountDn(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME));
		cleanupDelete(getSubLdapConnectionConfig(), toAccountSubDn(USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME));
		cleanupDelete(getSubLdapConnectionConfig(), toAccountSubDn(USER_SUBDOG_USERNAME, USER_SUBDOG_FULL_NAME));
		cleanupDelete(getSubLdapConnectionConfig(), toAccountSubDn(USER_SUBMARINE_USERNAME, USER_SUBMARINE_FULL_NAME));
		cleanupDelete(toGroupDn(GROUP_MELEE_ISLAND_NAME));
		cleanupDelete(toGroupDn(GROUP_MELEE_ISLAND_ALT_NAME));
		cleanupDelete(toOrgGroupDn(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME));
		cleanupDelete(toOrgGroupDn(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_ALT_NAME));
		cleanupDelete(toOrgDn(GROUP_MELEE_ISLAND_NAME));
		cleanupDelete(toOrgDn(GROUP_MELEE_ISLAND_ALT_NAME));
	}

	@Test
	@Override
    public void test020Schema() throws Exception {
		final String TEST_NAME = "test020Schema";
        displayTestTile(TEST_NAME);
        
//        IntegrationTestTools.displayXml("Resource XML", resource);
        accountObjectClassDefinition = AdUtils.assertAdResourceSchema(resource, getAccountObjectClass(), prismContext);
        AdUtils.assertAdRefinedSchema(resource, getAccountObjectClass(), prismContext);
        AdUtils.assertExchangeSchema(resource, getAccountObjectClass(), prismContext);
        
        ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
        assertEquals("Unexpected number of schema definitions (limited by generation constraints)", 5, resourceSchema.getDefinitions().size());
        
        assertLdapConnectorInstances(1);
	}
	
	@Test
    public void test100SeachJackBySamAccountName() throws Exception {
		final String TEST_NAME = "test100SeachJackBySamAccountName";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = createSamAccountNameQuery(ACCOUNT_JACK_SAM_ACCOUNT_NAME);
        
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
		
        // WHEN
        displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        
        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_JACK_SAM_ACCOUNT_NAME, ACCOUNT_JACK_FULL_NAME));
        jackAccountOid = shadow.getOid();
        
//        assertConnectorOperationIncrement(2);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
        assertLdapConnectorInstances(1);
	}
	
	/**
	 * MID-3730
	 */
	@Test
    public void test101SeachJackByDn() throws Exception {
		final String TEST_NAME = "test101SeachJackByDn";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        String jackDn = toAccountDn(ACCOUNT_JACK_SAM_ACCOUNT_NAME, ACCOUNT_JACK_FULL_NAME);
        ObjectQuery query = createAccountShadowQueryByAttribute("dn", jackDn, resource);
        
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
		
        // WHEN
        displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        
        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, jackDn);
        
//        assertConnectorOperationIncrement(2);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
        assertLdapConnectorInstances(1);
	}
	
	/**
	 * Search for non-existent DN should return no results. It should NOT
	 * throw an error.
	 * 
	 * MID-3730
	 */
	@Test
    public void test102SeachNotExistByDn() throws Exception {
		final String TEST_NAME = "test102SeachNotExistByDn";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        String dn = toAccountDn("idonoexist", "I am a Fiction");
        ObjectQuery query = createAccountShadowQueryByAttribute("dn", dn, resource);
        
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
		
        // WHEN
        displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
		// THEN
		displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
        assertEquals("Unexpected search result: "+shadows, 0, shadows.size());
                
//        assertConnectorOperationIncrement(2);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
        
        assertLdapConnectorInstances(1);
	}
	
	@Test
    public void test105SeachPiratesByCn() throws Exception {
		final String TEST_NAME = "test105SeachPiratesByCn";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getGroupObjectClass(), prismContext);
		ObjectQueryUtil.filterAnd(query.getFilter(), createAttributeFilter("cn", GROUP_PIRATES_NAME));
        
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
		
        // WHEN
        displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        
        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        groupPiratesOid = shadow.getOid();
        
//        assertConnectorOperationIncrement(1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
        assertLdapConnectorInstances(1);
	}
	
	@Test
    public void test110GetJack() throws Exception {
		final String TEST_NAME = "test110GetJack";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
		
        // WHEN
        displayWhen(TEST_NAME);
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, jackAccountOid, null, task, result);
        
		// THEN
        displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);		
        display("Shadow", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_JACK_SAM_ACCOUNT_NAME, ACCOUNT_JACK_FULL_NAME));
        jackAccountOid = shadow.getOid();
        
        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupQName(), groupPiratesOid);
        
        assertAttribute(shadow, "dn", "CN=Jack Sparrow,CN=Users,DC=ad,DC=evolveum,DC=com");
        assertAttribute(shadow, "cn", ACCOUNT_JACK_FULL_NAME);
        assertAttribute(shadow, "sn", "Sparrow");
        assertAttribute(shadow, "description", "The best pirate the world has ever seen");
        assertAttribute(shadow, "sAMAccountName", ACCOUNT_JACK_SAM_ACCOUNT_NAME);
        assertAttribute(shadow, "lastLogon", 0L);
        
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
        
        assertLdapConnectorInstances(1);
	}
	
	
	/**
	 * No paging. It should return all accounts.
	 */
	@Test
    public void test150SeachAllAccounts() throws Exception {
		final String TEST_NAME = "test150SeachAllAccounts";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
        
        // WHEN
        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 
        		NUMBER_OF_ACCOUNTS, task, result);
        
        // TODO: why 11? should be 1
//        assertConnectorOperationIncrement(11);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
//        assertLdapConnectorInstances(2);
    }
	
	/**
	 * This is in one block.
	 */
	@Test
    public void test152SeachFirst2Accounts() throws Exception {
		final String TEST_NAME = "test152SeachFirst2Accounts";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setMaxSize(2);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 2, task, result);
                
//        assertConnectorOperationIncrement(1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
//        assertLdapConnectorInstances(2);
    }
	
//	/**
//	 * Blocksize is 5, so this gets more than two blocks.
//	 */
//	@Test
//    public void test154SeachFirst11Accounts() throws Exception {
//		final String TEST_NAME = "test154SeachFirst11Accounts";
//        displayTestTile(TEST_NAME);
//        
//        // GIVEN
//        Task task = createTask(TEST_NAME);
//        OperationResult result = task.getResult();
//        
//        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
//        
//        ObjectPaging paging = ObjectPaging.createEmptyPaging();
//        paging.setMaxSize(11);
//		query.setPaging(paging);
//		
//		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 11, task, result);
//                
//        assertConnectorOperationIncrement(1);
//        assertConnectorSimulatedPagingSearchIncrement(0);
//        
//        SearchResultMetadata metadata = searchResultList.getMetadata();
//        if (metadata != null) {
//        	assertFalse(metadata.isPartialResults());
//        }
//        
//        assertLdapConnectorInstances(2);
//    }
//	
//	@Test
//    public void test162SeachFirst2AccountsOffset0() throws Exception {
//		final String TEST_NAME = "test162SeachFirst2AccountsOffset0";
//        displayTestTile(TEST_NAME);
//        
//        // GIVEN
//        Task task = createTask(TEST_NAME);
//        OperationResult result = task.getResult();
//        
//        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
//        
//        ObjectPaging paging = ObjectPaging.createEmptyPaging();
//        paging.setOffset(0);
//        paging.setMaxSize(2);
//		query.setPaging(paging);
//        
//		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 2, task, result);
//        
//        assertConnectorOperationIncrement(1);
//        assertConnectorSimulatedPagingSearchIncrement(0);
//        
//        SearchResultMetadata metadata = searchResultList.getMetadata();
//        if (metadata != null) {
//        	assertFalse(metadata.isPartialResults());
//        }
//        
//        assertLdapConnectorInstances(2);
//    }
//	
//	/**
//	 * Blocksize is 5, so this is in one block.
//	 * There is offset, so VLV should be used.
//	 * No explicit sorting.
//	 */
//	@Test
//    public void test172Search2AccountsOffset1() throws Exception {
//		final String TEST_NAME = "test172Search2AccountsOffset1";
//        displayTestTile(TEST_NAME);
//        
//        // GIVEN
//        Task task = createTask(TEST_NAME);
//        OperationResult result = task.getResult();
//        
//        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
//        
//        ObjectPaging paging = ObjectPaging.createPaging(1, 2);
//		query.setPaging(paging);
//        
//		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 2, task, result);
//        
//        assertConnectorOperationIncrement(1);
//        assertConnectorSimulatedPagingSearchIncrement(0);
//        
//        SearchResultMetadata metadata = searchResultList.getMetadata();
//        if (metadata != null) {
//        	assertFalse(metadata.isPartialResults());
//        }
//        
//        assertLdapConnectorInstances(2);
//    }
//	
//	/**
//	 * Blocksize is 5, so this gets more than two blocks.
//	 * There is offset, so VLV should be used.
//	 * No explicit sorting.
//	 */
//	@Test
//    public void test174SeachFirst11AccountsOffset2() throws Exception {
//		final String TEST_NAME = "test174SeachFirst11AccountsOffset2";
//        displayTestTile(TEST_NAME);
//        
//        // GIVEN
//        Task task = createTask(TEST_NAME);
//        OperationResult result = task.getResult();
//        
//        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
//        
//        ObjectPaging paging = ObjectPaging.createPaging(2, 11);
//		query.setPaging(paging);
//        
//		allowDuplicateSearchResults = true;
//		
//		// WHEN
//		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 11, task, result);
//        
//		// THEN
//		allowDuplicateSearchResults = false;
//		
//        assertConnectorOperationIncrement(1);
//        assertConnectorSimulatedPagingSearchIncrement(0);
//        
//        SearchResultMetadata metadata = searchResultList.getMetadata();
//        if (metadata != null) {
//        	assertFalse(metadata.isPartialResults());
//        }
//        
//        assertLdapConnectorInstances(2);
//    }
//	
//	/**
//	 * Blocksize is 5, so this is in one block.
//	 * There is offset, so VLV should be used.
//	 * Explicit sorting.
//	 */
//	@Test
//    public void test182Search2AccountsOffset1SortCn() throws Exception {
//		final String TEST_NAME = "test182Search2AccountsOffset1SortCn";
//        displayTestTile(TEST_NAME);
//        
//        // GIVEN
//        Task task = createTask(TEST_NAME);
//        OperationResult result = task.getResult();
//        
//        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
//        
//        ObjectPaging paging = ObjectPaging.createPaging(1, 2);
//        paging.setOrdering(getAttributePath(resource, "cn"), OrderDirection.ASCENDING);
//		query.setPaging(paging);
//        
//		SearchResultList<PrismObject<ShadowType>> shadows = doSearch(TEST_NAME, query, 2, task, result);
//        
//        assertAccountShadow(shadows.get(0), "CN=Adalbert Meduza,OU=evolveum,DC=win,DC=evolveum,DC=com");
//        assertAccountShadow(shadows.get(1), "CN=Adalbert Meduza1,OU=evolveum,DC=win,DC=evolveum,DC=com");
//        
//        assertConnectorOperationIncrement(1);
//        assertConnectorSimulatedPagingSearchIncrement(0);
//        
//        SearchResultMetadata metadata = shadows.getMetadata();
//        if (metadata != null) {
//        	assertFalse(metadata.isPartialResults());
//        }
//
//        assertLdapConnectorInstances(2);
//    }
		
	@Test
    public void test200AssignAccountBarbossa() throws Exception {
		final String TEST_NAME = "test200AssignAccountBarbossa";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignAccount(USER_BARBOSSA_OID, getResourceOid(), null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
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
        
        ResourceAttribute<String> createTimestampAttribute = ShadowUtil.getAttribute(shadow, new QName(MidPointConstants.NS_RI, "createTimeStamp"));
        assertNotNull("No createTimestamp in "+shadow, createTimestampAttribute);
        String createTimestamp = createTimestampAttribute.getRealValue();
        GeneralizedTime createTimestampGt = new GeneralizedTime(createTimestamp);
		long createTimestampMillis = createTimestampGt.getCalendar().getTimeInMillis();
        // LDAP server may be on a different host. Allow for some clock offset.
        TestUtil.assertBetween("Wrong createTimestamp in "+shadow, roundTsDown(tsStart)-120000, roundTsUp(tsEnd)+120000, createTimestampMillis);
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test210ModifyAccountBarbossaTitle() throws Exception {
		final String TEST_NAME = "test210ModifyAccountBarbossaTitle";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<ShadowType> delta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountBarbossaOid, prismContext);
        QName attrQName = new QName(MidPointConstants.NS_RI, "title");
        ResourceAttributeDefinition<String> attrDef = accountObjectClassDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = PropertyDelta.createModificationReplaceProperty(
        		new ItemPath(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "Captain");
        delta.addModification(attrDelta);
        
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test212ModifyAccountBarbossaShowInAdvancedViewOnlyTrue() throws Exception {
		final String TEST_NAME = "test212ModifyAccountBarbossaShowInAdvancedViewOnlyTrue";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<ShadowType> delta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountBarbossaOid, prismContext);
        QName attrQName = new QName(MidPointConstants.NS_RI, "showInAdvancedViewOnly");
        ResourceAttributeDefinition<String> attrDef = accountObjectClassDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<Boolean> attrDelta = PropertyDelta.createModificationReplaceProperty(
        		new ItemPath(ShadowType.F_ATTRIBUTES, attrQName), attrDef, Boolean.TRUE);
        delta.addModification(attrDelta);
        
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

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
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        
        ObjectDelta<ShadowType> delta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountBarbossaOid, prismContext);
        QName attrQName = new QName(MidPointConstants.NS_RI, "showInAdvancedViewOnly");
        ResourceAttributeDefinition<String> attrDef = accountObjectClassDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<Boolean> attrDelta = PropertyDelta.createModificationReplaceProperty(
        		new ItemPath(ShadowType.F_ATTRIBUTES, attrQName), attrDef, Boolean.TRUE);
        delta.addModification(attrDelta);
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, 
        		new ItemPath(UserType.F_EXTENSION,  EXTENSION_SHOW_IN_ADVANCED_VIEW_ONLY_QNAME), 
        		task, result, Boolean.FALSE);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

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
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue("here.There.Be.Monsters");
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, 
        		new ItemPath(UserType.F_CREDENTIALS,  CredentialsType.F_PASSWORD, PasswordType.F_VALUE), 
        		task, result, userPasswordPs);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertBarbossaEnabled();
        
        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test230DisableUserBarbossa() throws Exception {
		final String TEST_NAME = "test230DisableUserBarbossa";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, 
        		SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
        		task, result, ActivationStatusType.DISABLED);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        assertBarbossaDisabled();
	}
	
	/**
	 * MID-4041
	 */
	@Test
    public void test232ReconcileBarbossa() throws Exception {
		final String TEST_NAME = "test232ReconcileBarbossa";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_BARBOSSA_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        assertBarbossaDisabled();
	}

	/**
	 * MID-4041
	 */
	@Test
    public void test236EnableUserBarbossa() throws Exception {
		final String TEST_NAME = "test236EnableUserBarbossa";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, 
        		SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
        		task, result, ActivationStatusType.ENABLED);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        assertBarbossaEnabled();
        
        assertLdapConnectorInstances(2);
	}
	
	/**
	 * MID-4041
	 */
	@Test
    public void test237ReconcileBarbossa() throws Exception {
		final String TEST_NAME = "test237ReconcileBarbossa";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_BARBOSSA_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        assertBarbossaEnabled();
        
        assertLdapConnectorInstances(2);
	}
	
	/**
	 * MID-4041
	 */
	@Test
    public void test238DisableUserBarbossaRawAndReconcile() throws Exception {
		final String TEST_NAME = "test238DisableUserBarbossaRawAndReconcile";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        modifyUserReplace(USER_BARBOSSA_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
        		ModelExecuteOptions.createRaw(), task, result, ActivationStatusType.DISABLED);
        
        // WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_BARBOSSA_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        assertBarbossaDisabled();
        
        assertLdapConnectorInstances(2);
	}
	
	/**
	 * MID-4041
	 */
	@Test
    public void test239EnableUserBarbossaRawAndReconcile() throws Exception {
		final String TEST_NAME = "test239EnableUserBarbossaRawAndReconcile";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        modifyUserReplace(USER_BARBOSSA_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
        		ModelExecuteOptions.createRaw(), task, result, ActivationStatusType.ENABLED);
        
        // WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_BARBOSSA_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        assertBarbossaEnabled();
        
        assertLdapConnectorInstances(2);
	}
	
	private PrismObject<UserType> assertBarbossaEnabled() throws Exception {
		PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertAdministrativeStatus(user, ActivationStatusType.ENABLED);
        
		Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        assertAttribute(entry, ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME, "FALSE");        
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, "here.There.Be.Monsters");
        
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAccountEnabled(shadow);
        
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
        
        return user;
	}
	
	private void assertBarbossaDisabled() throws Exception {
		assertLdapConnectorInstances(2);
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertAdministrativeStatus(user, ActivationStatusType.DISABLED);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        display("disabled Barbossa entry", entry);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "514");
        
        assertAttribute(entry, ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME, "TRUE");
                
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

	/**
	 * This should create account with a group. And disabled.
	 */
	@Test
    public void test250AssignGuybrushPirates() throws Exception {
		final String TEST_NAME = "test250AssignGuybrushPirates";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        modifyUserReplace(USER_GUYBRUSH_OID, 
        		SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
        		task, result, ActivationStatusType.DISABLED);
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_GUYBRUSH_OID, ROLE_PIRATES_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

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
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue("wanna.be.a.123");
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, 
        		new ItemPath(UserType.F_CREDENTIALS,  CredentialsType.F_PASSWORD, PasswordType.F_VALUE), 
        		task, result, userPasswordPs);
        
        // THEN
        displayThen(TEST_NAME);
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
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test260EnableGyubrush() throws Exception {
		final String TEST_NAME = "test260EnableGyubrush";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_GUYBRUSH_OID, 
        		SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
        		task, result, ActivationStatusType.ENABLED);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
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
	
	/**
	 * Try to create an account without password. This should end up with an error.
	 * A reasonable error.
	 * MID-4046
	 */
	@Test
    public void test270AssignAccountToEmptyhead() throws Exception {
		final String TEST_NAME = "test270AssignAccountToEmptyhead";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = createUser(USER_EMPTYHEAD_NAME, USER_EMPTYHEAD_NAME, true);
        display("User before", userBefore);
        addObject(userBefore);
        String userEmptyheadOid = userBefore.getOid();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignAccount(userEmptyheadOid, getResourceOid(), null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertPartialError(result);
        
        assertMessageContains(result.getMessage(), "does not meet the length, complexity, or history requirement");
        
        assertLdapConnectorInstances(2);
	}
	
	/**
	 * Just make random test connection between the tests. Make sure that the
	 * test does not break anything. If it does the next tests will simply fail.
	 */
	@Test
    public void test295TestConnection() throws Exception {
		final String TEST_NAME = "test295TestConnection";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        OperationResult testResult = modelService.testResource(getResourceOid(), task);
        
        // THEN
        displayThen(TEST_NAME);
        display("Test connection result", testResult);
        TestUtil.assertSuccess("Test connection result", testResult);
                
        assertLdapConnectorInstances(2);
	}

	@Test
    public void test300AssignBarbossaPirates() throws Exception {
		final String TEST_NAME = "test300AssignBarbossaPirates";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_BARBOSSA_OID, ROLE_PIRATES_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
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
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_BARBOSSA_OID, UserType.F_NAME, 
        		PrismTestUtil.createPolyString(USER_CPTBARBOSSA_USERNAME));
        objectDelta.addModificationReplaceProperty(UserType.F_FULL_NAME,
        		PrismTestUtil.createPolyString(USER_CPTBARBOSSA_FULL_NAME));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		
        
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
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
        
//        assertLdapConnectorInstances(2);
	}
	
	// TODO: create account with a group membership
	
	
	@Test
    public void test395UnAssignBarbossaPirates() throws Exception {
		final String TEST_NAME = "test395UnAssignBarbossaPirates";
        displayTestTile(TEST_NAME);

        // TODO: do this on another account. There is a bad interference with rename.
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_BARBOSSA_OID, ROLE_PIRATES_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
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
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test399UnAssignAccountBarbossa() throws Exception {
		final String TEST_NAME = "test399UnAssignAccountBarbossa";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignAccount(USER_BARBOSSA_OID, getResourceOid(), null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertNoLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_CPTBARBOSSA_FULL_NAME);
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertNoLinkedAccount(user);
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test500AddOrgMeleeIsland() throws Exception {
		final String TEST_NAME = "test500AddOrgMeleeIsland";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<OrgType> org = instantiateObject(OrgType.class);
        OrgType orgType = org.asObjectable();
        orgType.setName(new PolyStringType(GROUP_MELEE_ISLAND_NAME));
        AssignmentType metaroleAssignment = new AssignmentType();
        ObjectReferenceType metaroleRef = new ObjectReferenceType();
        metaroleRef.setOid(ROLE_META_ORG_OID);
        metaroleRef.setType(RoleType.COMPLEX_TYPE);
		metaroleAssignment.setTargetRef(metaroleRef);
		orgType.getAssignment().add(metaroleAssignment);
        
        // WHEN
        displayWhen(TEST_NAME);
        addObject(org, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        orgMeleeIslandOid = org.getOid();
        Entry entryGroup = assertLdapGroup(GROUP_MELEE_ISLAND_NAME);
        Entry entryOu = assertLdapOrg(GROUP_MELEE_ISLAND_NAME);
        
        org = getObject(OrgType.class, orgMeleeIslandOid);
        groupMeleeIslandOid = getLinkRefOid(org, getResourceOid(), ShadowKindType.ENTITLEMENT, INTENT_GROUP);
        ouMeleeIslandOid = getLinkRefOid(org, getResourceOid(), ShadowKindType.GENERIC, INTENT_OU_TOP);
        assertLinks(org, 2);
        
        PrismObject<ShadowType> shadowGroup = getShadowModel(groupMeleeIslandOid);
        display("Shadow: group (model)", shadowGroup);
        
        PrismObject<ShadowType> shadowOu = getShadowModel(ouMeleeIslandOid);
        display("Shadow: ou (model)", shadowOu);
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test510AssignGuybrushMeleeIsland() throws Exception {
		final String TEST_NAME = "test510AssignGuybrushMeleeIsland";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignOrg(USER_GUYBRUSH_OID, orgMeleeIslandOid, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        
        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        
        assertLdapGroupMember(entry, GROUP_MELEE_ISLAND_NAME);

        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupQName(), groupMeleeIslandOid);
        
//        assertLdapConnectorInstances(2);
	}
	
	/**
	 * Create role under the Melee Island org. This creates group in the orgstruct.
	 */
	@Test
    public void test515AddOrgGroupMeleeIslandPirates() throws Exception {
		final String TEST_NAME = "test515AddOrgGroupMeleeIslandPirates";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<RoleType> role = instantiateObject(RoleType.class);
        RoleType roleType = role.asObjectable();
        roleType.setName(new PolyStringType(GROUP_MELEE_ISLAND_PIRATES_NAME));
        
        AssignmentType metaroleAssignment = new AssignmentType();
        ObjectReferenceType metaroleRef = new ObjectReferenceType();
        metaroleRef.setOid(ROLE_META_ORG_GROUP_OID);
        metaroleRef.setType(RoleType.COMPLEX_TYPE);
		metaroleAssignment.setTargetRef(metaroleRef);
		roleType.getAssignment().add(metaroleAssignment);
		
		AssignmentType orgAssignment = new AssignmentType();
        ObjectReferenceType orgRef = new ObjectReferenceType();
        orgRef.setOid(orgMeleeIslandOid);
        orgRef.setType(OrgType.COMPLEX_TYPE);
		orgAssignment.setTargetRef(orgRef);
		roleType.getAssignment().add(orgAssignment);
        
        // WHEN
        displayWhen(TEST_NAME);
        addObject(role, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        roleMeleeIslandPiratesOid = role.getOid();
        // TODO: assert LDAP object
        
        Entry entryOrgGroup = assertLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME);
        
        PrismObject<RoleType> roleAfter = getObject(RoleType.class, roleMeleeIslandPiratesOid);
        display("Role after", roleAfter);
        groupMeleeIslandPiratesOid = getSingleLinkOid(roleAfter);
        PrismObject<ShadowType> shadow = getShadowModel(groupMeleeIslandPiratesOid);
        display("Shadow (model)", shadow);
        
//        assertLdapConnectorInstances(2);
	}
	
	/**
	 * Rename org unit. MidPoint should rename OU and ordinary group.
	 * AD will rename the group in the orgstruct automatically. We need to
	 * make sure that we can still access that group.
	 */
	@Test
    public void test520RenameMeleeIsland() throws Exception {
		final String TEST_NAME = "test520RenameMeleeIsland";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        renameObject(OrgType.class, orgMeleeIslandOid, GROUP_MELEE_ISLAND_ALT_NAME, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<OrgType> orgAfter = getObject(OrgType.class, orgMeleeIslandOid);
        groupMeleeIslandOid = getLinkRefOid(orgAfter, getResourceOid(), ShadowKindType.ENTITLEMENT, INTENT_GROUP);
        ouMeleeIslandOid = getLinkRefOid(orgAfter, getResourceOid(), ShadowKindType.GENERIC, INTENT_OU_TOP);
        assertLinks(orgAfter, 2);
        
        PrismObject<ShadowType> shadowGroup = getShadowModel(groupMeleeIslandOid);
        display("Shadow: group (model)", shadowGroup);
        
        PrismObject<ShadowType> shadowOu = getShadowModel(ouMeleeIslandOid);
        display("Shadow: ou (model)", shadowOu);
        
        Entry groupEntry = assertLdapGroup(GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapGroup(GROUP_MELEE_ISLAND_NAME);
        
        Entry entryOu = assertLdapOrg(GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapOrg(GROUP_MELEE_ISLAND_NAME);
        
        Entry entryOrgGroup = assertLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME);

        Entry entryGuybrush = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        
        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        String shadowAccountOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadowAccount = getShadowModel(shadowAccountOid);
        display("Shadow: account (model)", shadowAccount);
        
        assertLdapGroupMember(entryGuybrush, GROUP_MELEE_ISLAND_ALT_NAME);

        IntegrationTestTools.assertAssociation(shadowAccount, getAssociationGroupQName(), groupMeleeIslandOid);
        
//        assertLdapConnectorInstances(2);
	}
	
	/**
	 * AD renamed the pirate groups by itself. MidPoint does not know about it.
	 * The GUID that is stored in the shadow is still OK. But the DN is now out
	 * of date. Try to update the group. Make sure it works.
	 * It is expected that the GUI will be used as a primary identifier.
	 * Note: just reading the group will NOT work. MidPoint is too smart
	 * for that. It will transparently fix the situation. 
	 */
	@Test
    public void test522ModifyMeleeIslandPirates() throws Exception {
		final String TEST_NAME = "test522GetMeleeIslandPirates";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyObjectReplaceProperty(ShadowType.class, groupMeleeIslandPiratesOid, 
        		new ItemPath(ShadowType.F_ATTRIBUTES, new QName(MidPointConstants.NS_RI, "description")),
        		task, result,
        		GROUP_MELEE_ISLAND_PIRATES_DESCRIPTION);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        Entry entryOrgGroup = assertLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_ALT_NAME);
        assertAttribute(entryOrgGroup, "description", GROUP_MELEE_ISLAND_PIRATES_DESCRIPTION);
        
        assertNoLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME);
        
//        assertLdapConnectorInstances(2);
	}

	@Test
    public void test524GetMeleeIslandPirates() throws Exception {
		final String TEST_NAME = "test524GetMeleeIslandPirates";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, groupMeleeIslandPiratesOid, null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        display("Shadow after", shadow);
        assertNotNull(shadow);
        
        Entry groupEntry = assertLdapGroup(GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapGroup(GROUP_MELEE_ISLAND_NAME);
        Entry entryOu = assertLdapOrg(GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapOrg(GROUP_MELEE_ISLAND_NAME);
        Entry entryOrgGroup = assertLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME);
        
//        assertLdapConnectorInstances(2);
	}

	@Test
    public void test595DeleteOrgGroupMeleeIslandPirates() throws Exception {
		final String TEST_NAME = "test595DeleteOrgGroupMeleeIslandPirates";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        deleteObject(RoleType.class, roleMeleeIslandPiratesOid, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME);
        
        assertNoObject(ShadowType.class, groupMeleeIslandPiratesOid);
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test599DeleteOrgMeleeIsland() throws Exception {
		final String TEST_NAME = "test599DeleteOrgMeleeIsland";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        deleteObject(OrgType.class, orgMeleeIslandOid, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoLdapGroup(GROUP_MELEE_ISLAND_NAME);
        assertNoLdapGroup(GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapOrg(GROUP_MELEE_ISLAND_NAME);
        assertNoLdapOrg(GROUP_MELEE_ISLAND_ALT_NAME);
        
        assertNoObject(ShadowType.class, groupMeleeIslandOid);
        assertNoObject(ShadowType.class, ouMeleeIslandOid);
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test600AssignAccountSubman() throws Exception {
		final String TEST_NAME = "test600AssignAccountSubman";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_SUBMAN_OID, ROLE_SUBMISSIVE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();

        Entry entry = assertLdapSubAccount(USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME);
        display("Sub entry", entry);
        assertAttribute(entry, "title", null);
        
        PrismObject<UserType> userAfter = getUser(USER_SUBMAN_OID);
        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountSubmanOid = shadow.getOid();
        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        String accountBarbossaIcfUid = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in "+shadow, accountBarbossaIcfUid);
        
        assertEquals("Wrong ICFS UID", 
        		AdUtils.formatGuidToDashedNotation(MiscUtil.binaryToHex(entry.get(getPrimaryIdentifierAttributeName()).getBytes())),
        		accountBarbossaIcfUid);
        
        assertLdapPassword(getSubLdapConnectionConfig(), USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME, USER_SUBMAN_PASSWORD);
        
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        ResourceAttribute<String> createTimestampAttribute = ShadowUtil.getAttribute(shadow, new QName(MidPointConstants.NS_RI, "createTimeStamp"));
        assertNotNull("No createTimestamp in "+shadow, createTimestampAttribute);
        String createTimestamp = createTimestampAttribute.getRealValue();
        GeneralizedTime createTimestampGt = new GeneralizedTime(createTimestamp);
		long createTimestampMillis = createTimestampGt.getCalendar().getTimeInMillis();
        // LDAP server may be on a different host. Allow for some clock offset.
        TestUtil.assertBetween("Wrong createTimestamp in "+shadow, roundTsDown(tsStart)-120000, roundTsUp(tsEnd)+120000, createTimestampMillis);
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test610ModifyUserSubmanTitle() throws Exception {
		final String TEST_NAME = "test610ModifyUserSubmanTitle";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_SUBMAN_OID, UserType.F_TITLE, task, result, 
        		PrismTestUtil.createPolyString("Underdog"));
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        Entry entry = assertLdapSubAccount(USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME);
        display("Sub entry", entry);
        assertAttribute(entry, "title", "Underdog");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        PrismObject<UserType> user = getUser(USER_SUBMAN_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountSubmanOid, shadowOid);
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test620ModifyUserSubmanPassword() throws Exception {
		final String TEST_NAME = "test620ModifyUserSubmanPassword";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue("SuB.321");
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_SUBMAN_OID, 
        		new ItemPath(UserType.F_CREDENTIALS,  CredentialsType.F_PASSWORD, PasswordType.F_VALUE), 
        		task, result, userPasswordPs);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapSubAccount(USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME);
        assertAttribute(entry, "title", "Underdog");
        assertLdapPassword(getSubLdapConnectionConfig(), USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME, "SuB.321");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        PrismObject<UserType> user = getUser(USER_SUBMAN_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountSubmanOid, shadowOid);
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test630DisableUserSubman() throws Exception {
		final String TEST_NAME = "test630DisableUserSubman";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_SUBMAN_OID, 
        		SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
        		task, result, ActivationStatusType.DISABLED);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
//        assertLdapConnectorInstances(2);
        
        PrismObject<UserType> user = getUser(USER_SUBMAN_OID);
        assertAdministrativeStatus(user, ActivationStatusType.DISABLED);

        Entry entry = assertLdapSubAccount(USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "514");
                
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAccountDisabled(shadow);
        
        try {
        	assertLdapPassword(getSubLdapConnectionConfig(), USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME, "SuB.321");
        	AssertJUnit.fail("Password authentication works, but it should fail");
        } catch (SecurityException e) {
        	// this is expected
        }
        
//        assertLdapConnectorInstances(2);
	}

	@Test
    public void test639EnableUserSubman() throws Exception {
		final String TEST_NAME = "test639EnableUserBarbossa";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_SUBMAN_OID, 
        		SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS, 
        		task, result, ActivationStatusType.ENABLED);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        PrismObject<UserType> user = getUser(USER_SUBMAN_OID);
        assertAdministrativeStatus(user, ActivationStatusType.ENABLED);

        Entry entry = assertLdapSubAccount(USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAccountEnabled(shadow);
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test690ModifyUserSubmanRename() throws Exception {
		final String TEST_NAME = "test690ModifyUserSubmanRename";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_SUBMAN_OID, UserType.F_NAME, 
        		PrismTestUtil.createPolyString(USER_SUBDOG_USERNAME));
        objectDelta.addModificationReplaceProperty(UserType.F_FULL_NAME,
        		PrismTestUtil.createPolyString(USER_SUBDOG_FULL_NAME));
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		
        
        // WHEN
        displayWhen(TEST_NAME);
        modelService.executeChanges(deltas, null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapSubAccount(USER_SUBDOG_USERNAME, USER_SUBDOG_FULL_NAME);
        assertAttribute(entry, "title", "Underdog");
        
        PrismObject<UserType> user = getUser(USER_SUBMAN_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountSubmanOid, shadowOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        display("Shadow after rename (model)", shadow);
        
        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        display("Shadow after rename (repo)", repoShadow);
        
        assertNoLdapSubAccount(USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME);
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test699UnAssignAccountSubdog() throws Exception {
		final String TEST_NAME = "test699UnAssignAccountSubdog";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_SUBMAN_OID, ROLE_SUBMISSIVE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoLdapSubAccount(USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME);
        assertNoLdapSubAccount(USER_SUBDOG_USERNAME, USER_SUBDOG_FULL_NAME);
        
        PrismObject<UserType> user = getUser(USER_SUBMAN_OID);
        assertNoLinkedAccount(user);
        
//        assertLdapConnectorInstances(2);
	}
	
	/**
	 * Create account and modify it in a very quick succession.
	 * This test is designed to check if we can live with a long
	 * global catalog update delay.
	 * MID-2926
	 */
	@Test
    public void test700AssignAccountSubmarineAndModify() throws Exception {
		final String TEST_NAME = "test700AssignAccountSubmarineAndModify";
        displayTestTile(TEST_NAME);
        
        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_SUBMARINE_OID, ROLE_SUBMISSIVE_OID, task, result);
        
        modifyUserReplace(USER_SUBMARINE_OID, UserType.F_TITLE, task, result, 
        		PrismTestUtil.createPolyString("Underseadog"));
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();

        Entry entry = assertLdapSubAccount(USER_SUBMARINE_USERNAME, USER_SUBMARINE_FULL_NAME);
        display("Sub entry", entry);
        assertAttribute(entry, "title", "Underseadog");
        
        PrismObject<UserType> userAfter = getUser(USER_SUBMARINE_OID);
        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountSubmarineOid = shadow.getOid();
        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        String accountIcfUid = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in "+shadow, accountIcfUid);
        
        assertEquals("Wrong ICFS UID", 
        		AdUtils.formatGuidToDashedNotation(MiscUtil.binaryToHex(entry.get(getPrimaryIdentifierAttributeName()).getBytes())),
        		accountIcfUid);
        
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        
//        assertLdapConnectorInstances(2);
	}
	
	@Test
    public void test809UnAssignAccountSubmarine() throws Exception {
		final String TEST_NAME = "test809UnAssignAccountSubmarine";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_SUBMARINE_OID, ROLE_SUBMISSIVE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoLdapSubAccount(USER_SUBMARINE_USERNAME, USER_SUBMARINE_FULL_NAME);
        
        PrismObject<UserType> user = getUser(USER_SUBMARINE_OID);
        assertNoLinkedAccount(user);
        
//        assertLdapConnectorInstances(2);
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
	
	protected Entry assertLdapSubAccount(String samAccountName, String cn) throws LdapException, IOException, CursorException {
		Entry entry = searchLdapAccount(getSubLdapConnectionConfig(), "(cn="+cn+")");
		assertAttribute(entry, "cn", cn);
		assertAttribute(entry, ATTRIBUTE_SAM_ACCOUNT_NAME_NAME, samAccountName);
		return entry;
	}

	@Override
	protected void assertNoLdapAccount(String uid) throws LdapException, IOException, CursorException {
		throw new UnsupportedOperationException("Boom! Cannot do this here. This is bloody AD! We need full name!");
	}
	
	protected void assertNoLdapAccount(String uid, String cn) throws LdapException, IOException, CursorException {
		assertNoLdapAccount(null, uid, cn);
	}
	
	protected void assertNoLdapSubAccount(String uid, String cn) throws LdapException, IOException, CursorException {
		assertNoLdapAccount(getSubLdapConnectionConfig(), uid, cn);
	}
	
	protected void assertNoLdapAccount(UserLdapConnectionConfig config, String uid, String cn) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect(config);
		List<Entry> entriesCn = ldapSearch(config, connection, "(cn="+cn+")");
		List<Entry> entriesSamAccountName = ldapSearch(config, connection, "(sAMAccountName="+uid+")");
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
		return ("CN="+fullName+","+getPeopleLdapSuffix());
	}
	
	protected String toAccountSubDn(String username, String fullName) {
		return ("CN="+fullName+","+getPeopleLdapSubSuffix());
	}
	
	@Override
	protected Rdn toAccountRdn(String username, String fullName) {
		try {
			return new Rdn(new Ava("CN", fullName));
		} catch (LdapInvalidDnException e) {
			throw new IllegalStateException(e.getMessage(),e);
		}
	}
	
	protected String toOrgDn(String cn) {
		return "ou="+cn+","+getOrgsLdapSuffix();
	}
	
	protected String toOrgGroupDn(String groupCn, String orgName) {
		return "cn="+groupCn+","+toOrgDn(orgName);
	}

	protected Entry assertLdapOrg(String orgName) throws LdapException, IOException, CursorException {
		String dn = toOrgDn(orgName);
		Entry entry = getLdapEntry(dn);
		assertNotNull("No entry "+dn, entry);
		assertAttribute(entry, "ou", orgName);
		return entry;
	}
	
	protected Entry assertNoLdapOrg(String orgName) throws LdapException, IOException, CursorException {
		String dn = toOrgDn(orgName);
		Entry entry = getLdapEntry(dn);
		assertNull("Unexpected org entry "+entry, entry);
		return entry;
	}
	
	protected Entry assertLdapOrgGroup(String groupCn, String orgName) throws LdapException, IOException, CursorException {
		String dn = toOrgGroupDn(groupCn, orgName);
		Entry entry = getLdapEntry(dn);
		assertNotNull("No entry "+dn, entry);
		assertAttribute(entry, "cn", groupCn);
		return entry;
	}
	
	protected Entry assertNoLdapOrgGroup(String groupCn, String orgName) throws LdapException, IOException, CursorException {
		String dn = toOrgGroupDn(groupCn, orgName);
		Entry entry = getLdapEntry(dn);
		assertNull("Unexpected org group entry "+entry, entry);
		return entry;
	}
	
	protected void assertLdapPassword(String uid, String fullName, String password) throws LdapException, IOException, CursorException {
		assertLdapPassword(null, uid, fullName, password);
	}
	
	protected void assertLdapPassword(UserLdapConnectionConfig config, String uid, String fullName, String password) throws LdapException, IOException, CursorException {
		Entry entry = getLdapAccountByCn(config, fullName);
		assertLdapPassword(config, entry, password);
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
