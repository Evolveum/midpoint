package com.evolveum.midpoint.testing.conntest;
/*
 * Copyright (c) 2010-2015 Evolveum
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.ModifyDnRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyDnResponse;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractLdapConnTest extends AbstractModelIntegrationTest {
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractLdapConnTest.class);
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ldap");
	
	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();
	
	protected static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	protected static final String USER_ADMINISTRATOR_USERNAME = "administrator";
	
	protected static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";
		
	protected static final File USER_BARBOSSA_FILE = new File(COMMON_DIR, "user-barbossa.xml");
	protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
	protected static final String USER_BARBOSSA_USERNAME = "barbossa";
	protected static final String USER_BARBOSSA_FULL_NAME = "Hector Barbossa";
	
	protected static final File USER_GUYBRUSH_FILE = new File (COMMON_DIR, "user-guybrush.xml");
	protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
	protected static final String USER_GUYBRUSH_USERNAME = "guybrush";
	protected static final String USER_GUYBRUSH_FULL_NAME = "Guybrush Threepwood";
	
	private static final String USER_LECHUCK_NAME = "lechuck";
	private static final String ACCOUNT_LECHUCK_NAME = "lechuck";
	private static final String ACCOUNT_CHARLES_NAME = "charles";
	
	private static final String LDAP_ACCOUNT_OBJECTCLASS = "inetOrgPerson";
	
	private static final String LDAP_GROUP_PIRATES_DN = "cn=Pirates,ou=groups,dc=example,dc=com";

	private static final String ATTRIBUTE_ENTRY_UUID = "entryUuid";
	
	protected static final String ACCOUNT_IDM_DN = "uid=idm,ou=Administrators,dc=example,dc=com";
	protected static final String ACCOUNT_0_UID = "u00000000";
	protected static final String ACCOUNT_18_UID = "u00000018";
	protected static final String ACCOUNT_19_UID = "u00000019";
	protected static final String ACCOUNT_67_UID = "u00000067";
	protected static final String ACCOUNT_68_UID = "u00000068";
	protected static final String ACCOUNT_239_UID = "u00000239";
	protected static final String ACCOUNT_240_UID = "u00000240";

	protected static final int NUMBER_OF_GENERATED_ACCOUNTS = 4000;

	protected static final String ACCOUNT_HT_UID = "ht";
	protected static final String ACCOUNT_HT_CN = "Herman Toothrot";
	protected static final String ACCOUNT_HT_GIVENNAME = "Herman";
	protected static final String ACCOUNT_HT_SN = "Toothrot";

	private static final String GROUP_MONKEYS_CN = "monkeys";
	private static final String GROUP_MONKEYS_DESCRIPTION = "Monkeys of Monkey Island";
	
	@Autowired(required = true)
	protected MatchingRuleRegistry matchingRuleRegistry;
	
	protected ResourceType resourceType;
	protected PrismObject<ResourceType> resource;
	
	protected MatchingRule<String> ciMatchingRule;
	
	private static String stopCommand;
	
	protected ObjectClassComplexTypeDefinition accountObjectClassDefinition;
	protected String account0Oid;
	protected String accountBarbossaOid;

    @Autowired
    protected ReconciliationTaskHandler reconciliationTaskHandler;
	
    @Override
    protected void startResources() throws Exception {
    	super.startResources();
    	
    	String command = getStartSystemCommand();
    	if (command != null) {
    		TestUtil.execSystemCommand(command);
    	}
    	stopCommand = getStopSystemCommand();
    }

    public abstract String getStartSystemCommand();
    
    public abstract String getStopSystemCommand();
    
	protected abstract void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd) throws ObjectNotFoundException, SchemaException;
    
	protected boolean isIdmAdminInteOrgPerson() {
		return false;
	}

	@AfterClass
    public static void stopResources() throws Exception {
        //end profiling
        ProfilingDataManager.getInstance().printMapAfterTest();
        ProfilingDataManager.getInstance().stopProfilingAfterTest();
        
    	if (stopCommand != null) {
    		TestUtil.execSystemCommand(stopCommand);
    	}
    }
    
	protected abstract String getResourceOid();

	protected abstract File getBaseDir();
	
	protected File getResourceFile() {
		return new File(getBaseDir(), "resource.xml");
	}
	
	protected File getSyncTaskFile() {
		return new File(getBaseDir(), "task-sync.xml");
	}
	
	protected File getSyncTaskInetOrgPersonFile() {
		return new File(getBaseDir(), "task-sync-inetorgperson.xml");
	}
	
	protected abstract String getSyncTaskOid();
	
	protected QName getAccountObjectClass() {
		return new QName(MidPointConstants.NS_RI, "inetOrgPerson");
	}

	
	protected abstract String getLdapServerHost();
	
	protected abstract int getLdapServerPort();
	
	protected abstract String getLdapBindDn();
	
	protected abstract String getLdapBindPassword();
	
	protected abstract int getSearchSizeLimit();

	protected String getLdapSuffix() {
		return "dc=example,dc=com";
	}
	
	protected String getPeopleLdapSuffix() {
		return "ou=people,"+getLdapSuffix();
	}

	protected String getGroupsLdapSuffix() {
		return "ou=people,"+getLdapSuffix();
	}
	
	protected abstract String getLdapGroupObjectClass();
	
	protected abstract String getLdapGroupMemberAttribute();
	
	protected String getScriptDirectoryName() {
		return "/opt/Bamboo/local/conntest";
	}
	
	protected abstract String getAccount0Cn();

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		// System Configuration
        PrismObject<SystemConfigurationType> config;
		try {
			config = repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}
		
		modelService.postInit(initResult);

        // to get profiling facilities (until better API is available)
//        LoggingConfigurationManager.configure(
//                ProfilingConfigurationManager.checkSystemProfilingConfiguration(config),
//                config.asObjectable().getVersion(), initResult);

        // administrator
		PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, UserType.class, initResult);
		repoAddObjectFromFile(ROLE_SUPERUSER_FILE, RoleType.class, initResult);
		login(userAdministrator);
		
		// Users
		repoAddObjectFromFile(USER_BARBOSSA_FILE, UserType.class, initResult);
		repoAddObjectFromFile(USER_GUYBRUSH_FILE, UserType.class, initResult);
		
		// Roles
		
		// Resources
		resource = importAndGetObjectFromFile(ResourceType.class, getResourceFile(), getResourceOid(), initTask, initResult);
		resourceType = resource.asObjectable();
		
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        //initProfiling - start
        ProfilingDataManager profilingManager = ProfilingDataManager.getInstance();

        Map<ProfilingDataManager.Subsystem, Boolean> subsystems = new HashMap<>();
        subsystems.put(ProfilingDataManager.Subsystem.MODEL, true);
        subsystems.put(ProfilingDataManager.Subsystem.REPOSITORY, true);
        profilingManager.configureProfilingDataManagerForTest(subsystems, true);

        profilingManager.appendProfilingToTest();
        //initProfiling - end
        
        ciMatchingRule = matchingRuleRegistry.getMatchingRule(StringIgnoreCaseMatchingRule.NAME, DOMUtil.XSD_STRING);
	}
	
	@Test
	public void test010Connection() throws Exception {
		final String TEST_NAME = "test010Connection";
		TestUtil.displayTestTile(TEST_NAME);
		
		OperationResult result = new OperationResult(this.getClass().getName()+"."+TEST_NAME);
		
		OperationResult	operationResult = provisioningService.testResource(getResourceOid());
		
		display("Test connection result",operationResult);
		TestUtil.assertSuccess("Test connection failed",operationResult);
	}
	
	@Test
    public void test020Schema() throws Exception {
		final String TEST_NAME = "test020Schema";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
        accountObjectClassDefinition = refinedSchema.findObjectClassDefinition(getAccountObjectClass());
        assertNotNull("No definition for object class "+getAccountObjectClass(), accountObjectClassDefinition);
        
        ResourceAttributeDefinition<String> cnDef = accountObjectClassDefinition.findAttributeDefinition("cn");
        PrismAsserts.assertDefinition(cnDef, new QName(MidPointConstants.NS_RI, "cn"), DOMUtil.XSD_STRING, 1, -1);
        assertTrue("createTimestampDef read", cnDef.canRead());
        assertTrue("createTimestampDef read", cnDef.canModify());
        assertTrue("createTimestampDef read", cnDef.canAdd());
        
        ResourceAttributeDefinition<Long> createTimestampDef = accountObjectClassDefinition.findAttributeDefinition("createTimestamp");
        PrismAsserts.assertDefinition(createTimestampDef, new QName(MidPointConstants.NS_RI, "createTimestamp"),
        		DOMUtil.XSD_LONG, 0, 1);
        assertTrue("createTimestampDef read", createTimestampDef.canRead());
        assertFalse("createTimestampDef read", createTimestampDef.canModify());
        assertFalse("createTimestampDef read", createTimestampDef.canAdd());
        
	}
	
	@Test
    public void test100SeachAccount0ByLdapUid() throws Exception {
		final String TEST_NAME = "test100SeachAccount0ByLdapUid";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ResourceAttributeDefinition ldapUidAttrDef = accountObjectClassDefinition.findAttributeDefinition("uid");
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        ObjectFilter additionalFilter = EqualFilter.createEqual(
        		new ItemPath(ShadowType.F_ATTRIBUTES, ldapUidAttrDef.getName()), ldapUidAttrDef, ACCOUNT_0_UID);
		ObjectQueryUtil.filterAnd(query.getFilter(), additionalFilter);
        
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        
        PrismObject<ShadowType> shadow = shadows.get(0);
        assertAccountShadow(shadow, toDn(ACCOUNT_0_UID));
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
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
        
        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, NUMBER_OF_GENERATED_ACCOUNTS + (isIdmAdminInteOrgPerson()?1:0), task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
    }
	
	/**
	 * Blocksize is 100, so this is in one block.
	 */
	@Test
    public void test152SeachFirst50Accounts() throws Exception {
		final String TEST_NAME = "test152SeachFirst50Accounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setMaxSize(50);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 50, task, result);
                
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
    }
	
	/**
	 * Blocksize is 100, so this gets more than two blocks.
	 */
	@Test
    public void test154SeachFirst222Accounts() throws Exception {
		final String TEST_NAME = "test154SeachFirst222Accounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setMaxSize(222);
		query.setPaging(paging);
		
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 222, task, result);
                
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
    }
	
	
	@Test
    public void test162SeachFirst50AccountsOffset0() throws Exception {
		final String TEST_NAME = "test152SeachFirst50Accounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setOffset(0);
        paging.setMaxSize(50);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 50, task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
    }
	
	/**
	 * Blocksize is 100, so this is in one block.
	 * There is offset, so VLV should be used.
	 * No explicit sorting.
	 */
	@Test
    public void test172Search50AccountsOffset20() throws Exception {
		final String TEST_NAME = "test172Search50AccountsOffset20";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createPaging(20, 50);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 50, task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
    }

	/**
	 * Blocksize is 100, so this gets more than two blocks.
	 * There is offset, so VLV should be used.
	 * No explicit sorting.
	 */
	@Test
    public void test174SeachFirst222AccountsOffset20() throws Exception {
		final String TEST_NAME = "test174SeachFirst222AccountsOffset20";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createPaging(20, 222);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 222, task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

    }
	
	/**
	 * Blocksize is 100, so this is in one block.
	 * There is offset, so VLV should be used.
	 * Explicit sorting.
	 */
	@Test
    public void test182Search50AccountsOffset20SortUid() throws Exception {
		final String TEST_NAME = "test182Seac50AccountsOffset20SortUid";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createPaging(20, 50);
        paging.setOrderBy(getAttributeQName(resource, "uid"));
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> shadows = doSearch(TEST_NAME, query, 50, task, result);
        
        assertAccountShadow(shadows.get(0), toDn(isIdmAdminInteOrgPerson()?ACCOUNT_18_UID:ACCOUNT_19_UID));
        assertAccountShadow(shadows.get(49), toDn(isIdmAdminInteOrgPerson()?ACCOUNT_67_UID:ACCOUNT_68_UID));
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

    }

	/**
	 * Blocksize is 100, so this gets more than two blocks.
	 * There is offset, so VLV should be used.
	 * No explicit sorting.
	 */
	@Test
    public void test184SearchFirst222AccountsOffset20SortUid() throws Exception {
		final String TEST_NAME = "test184SeachFirst222AccountsOffset20SortUid";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createPaging(20, 222);
        paging.setOrderBy(getAttributeQName(resource, "uid"));
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> shadows = doSearch(TEST_NAME, query, 222, task, result);
        
        assertAccountShadow(shadows.get(0), toDn(isIdmAdminInteOrgPerson()?ACCOUNT_18_UID:ACCOUNT_19_UID));
        assertAccountShadow(shadows.get(221), toDn(isIdmAdminInteOrgPerson()?ACCOUNT_239_UID:ACCOUNT_240_UID));
                
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
    }
	
	/**
	 * No paging. Allow incomplete results. This should violate sizelimit, but some results should
	 * be returned anyway.
	 */
	@Test
    public void test190SeachAllAccountsSizelimit() throws Exception {
		final String TEST_NAME = "test190SeachAllAccountsSizelimit";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        query.setAllowPartialResults(true);
        
		SearchResultList<PrismObject<ShadowType>> resultList = doSearch(TEST_NAME, query, getSearchSizeLimit(), task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = resultList.getMetadata();
        assertNotNull("No search metadata", metadata);
        assertTrue("Partial results not indicated", metadata.isPartialResults());
    }
	
	// TODO: scoped search
	
	private SearchResultList<PrismObject<ShadowType>> doSearch(final String TEST_NAME, ObjectQuery query, int expectedSize, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		return doSearch(TEST_NAME, query, null, expectedSize, task, result);
	}
	
	private SearchResultList<PrismObject<ShadowType>> doSearch(final String TEST_NAME, ObjectQuery query, GetOperationOptions rootOptions, int expectedSize, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		final List<PrismObject<ShadowType>> foundObjects = new ArrayList<PrismObject<ShadowType>>(expectedSize);
        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
//				LOGGER.trace("Found {}", object);
				String name = object.asObjectable().getName().getOrig();
				for(PrismObject<ShadowType> foundShadow: foundObjects) {
					if (foundShadow.asObjectable().getName().getOrig().equals(name)) {
						AssertJUnit.fail("Duplicate name "+name);
					}
				}
				foundObjects.add(object);
				return true;
			}
		};
		
		Collection<SelectorOptions<GetOperationOptions>> options = null;
		if (rootOptions != null) {
			options = SelectorOptions.createCollection(rootOptions);
		}
		
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
		
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultMetadata searchResultMetadata = modelService.searchObjectsIterative(ShadowType.class, query, handler, options, task, result);
		
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		assertEquals("Unexpected number of accounts", expectedSize, foundObjects.size());
		
		SearchResultList<PrismObject<ShadowType>> resultList = new SearchResultList<>(foundObjects, searchResultMetadata);
		
		return resultList;
	}

	
    // TODO: count shadows
	
	@Test
    public void test200AssignAccountToBarbossa() throws Exception {
		final String TEST_NAME = "test200AssignAccountToBarbossa";
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
        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(shadow);
        String accountBarbossaIcfUid = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in "+shadow, accountBarbossaIcfUid);
        
        assertEquals("Wrong ICFS UID", entry.get(ATTRIBUTE_ENTRY_UUID).getString(), accountBarbossaIcfUid);
        
        ResourceAttribute<Long> createTimestampAttribute = ShadowUtil.getAttribute(shadow, new QName(MidPointConstants.NS_RI, "createTimestamp"));
        assertNotNull("No createTimestamp in "+shadow, createTimestampAttribute);
        Long createTimestamp = createTimestampAttribute.getRealValue();
        TestUtil.assertBetween("Wrong createTimestamp in "+shadow, roundTsDown(tsStart), roundTsUp(tsEnd), createTimestamp);
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
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
	}
	
	@Test
    public void test290ModifyUserBarbossaRename() throws Exception {
		final String TEST_NAME = "test290ModifyUserBarbossaRename";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString("cptbarbossa"));
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount("cptbarbossa", USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
	}
	
	@Test
    public void test299UnAssignAccountBarbossa() throws Exception {
		final String TEST_NAME = "test299UnAssignAccountBarbossa";
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

        assertNoLdapAccount(USER_BARBOSSA_USERNAME);
        assertNoLdapAccount("cptbarbossa");
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertNoLinkedAccount(user);
	}

	// TODO: sync tests
	
	@Test
    public void test800ImportSyncTask() throws Exception {
		final String TEST_NAME = "test800ImportSyncTask";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(getSyncTaskFile(), task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        waitForTaskNextRun(getSyncTaskOid(), true);
        
        long tsEnd = System.currentTimeMillis();
        
        assertStepSyncToken(getSyncTaskOid(), 0, tsStart, tsEnd);
	}
	
	@Test
    public void test801SyncAddAccountHt() throws Exception {
		final String TEST_NAME = "test801SyncAddAccountHt";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addLdapAccount(ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);
        waitForTaskNextRun(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        displayUsers();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user "+ACCOUNT_HT_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

        assertStepSyncToken(getSyncTaskOid(), 1, tsStart, tsEnd);
	}
	
	@Test
    public void test802ModifyAccountHt() throws Exception {
		final String TEST_NAME = "test802ModifyAccountHt";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        LdapNetworkConnection connection = ldapConnect();
        Modification modCn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "cn", "Horatio Torquemeda Marley");
        connection.modify(toDn(ACCOUNT_HT_UID), modCn);
		ldapDisconnect(connection);

		waitForTaskNextRun(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user "+ACCOUNT_HT_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, "Horatio Torquemeda Marley", ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

        assertStepSyncToken(getSyncTaskOid(), 2, tsStart, tsEnd);

	}
	
	@Test
    public void test810SyncAddGroupMonkeys() throws Exception {
		final String TEST_NAME = "test810SyncAddGroupMonkeys";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addLdapGroup(GROUP_MONKEYS_CN, GROUP_MONKEYS_DESCRIPTION, "uid=fake,"+getPeopleLdapSuffix());
        waitForTaskNextRun(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<RoleType> role = findObjectByName(RoleType.class, GROUP_MONKEYS_CN);
        display("Role", role);
        assertNotNull("no role "+GROUP_MONKEYS_CN, role);
        PrismAsserts.assertPropertyValue(role, RoleType.F_DESCRIPTION, GROUP_MONKEYS_DESCRIPTION);
        assertNotNull("No role "+GROUP_MONKEYS_CN+" created", role);

        assertStepSyncToken(getSyncTaskOid(), 3, tsStart, tsEnd);
	}
	
	@Test
    public void test817RenameAccount() throws Exception {
		final String TEST_NAME = "test817RenameAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        LdapNetworkConnection connection = ldapConnect();
        
        ModifyDnRequest modDnRequest = new ModifyDnRequestImpl();
        modDnRequest.setName(new Dn(toDn(ACCOUNT_HT_UID)));
        modDnRequest.setNewRdn(new Rdn("uid=htm"));
        modDnRequest.setDeleteOldRdn(true);
		ModifyDnResponse modDnResponse = connection.modifyDn(modDnRequest);
        
		display("Modified "+toDn(ACCOUNT_HT_UID)+" -> uid=htm: "+modDnResponse);
		
		ldapDisconnect(connection);

		waitForTaskNextRun(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername("htm");
        assertNotNull("No user "+"htm"+" created", user);
        assertUser(user, user.getOid(), "htm", "Horatio Torquemeda Marley", ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);
        assertNull("User "+ACCOUNT_HT_UID+" still exist", findUserByUsername(ACCOUNT_HT_UID));

        assertStepSyncToken(getSyncTaskOid(), 4, tsStart, tsEnd);

	}
	
	@Test
    public void test818DeleteAccountHtm() throws Exception {
		final String TEST_NAME = "test818DeleteAccountHtm";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        deleteLdapEntry(toDn("htm"));

		waitForTaskNextRun(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        assertNull("User "+"htm"+" still exist", findUserByUsername("htm"));
        assertNull("User "+ACCOUNT_HT_UID+" still exist", findUserByUsername(ACCOUNT_HT_UID));

        assertStepSyncToken(getSyncTaskOid(), 5, tsStart, tsEnd);
	}
	
	
	// TODO: sync with "ALL" object class
	
	@Test
    public void test819DeleteSyncTask() throws Exception {
		final String TEST_NAME = "test819DeleteSyncTask";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        deleteObject(TaskType.class, getSyncTaskOid(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoObject(TaskType.class, getSyncTaskOid(), task, result);
	}
	
	@Test
    public void test820ImportSyncTaskInetOrgPerson() throws Exception {
		final String TEST_NAME = "test820ImportSyncTaskInetOrgPerson";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(getSyncTaskInetOrgPersonFile(), task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        waitForTaskNextRun(getSyncTaskOid(), true);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<TaskType> syncTask = getTask(getSyncTaskOid());
        display("Sync task after start", syncTask);
        
        assertStepSyncToken(getSyncTaskOid(), 4, tsStart, tsEnd);
	}

	@Test
    public void test821SyncAddAccountHt() throws Exception {
		final String TEST_NAME = "test821SyncAddAccountHt";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addLdapAccount(ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);
        waitForTaskNextRun(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        displayUsers();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user "+ACCOUNT_HT_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

        assertStepSyncToken(getSyncTaskOid(), 5, tsStart, tsEnd);
	}
	
	@Test
    public void test822ModifyAccountHt() throws Exception {
		final String TEST_NAME = "test822ModifyAccountHt";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        LdapNetworkConnection connection = ldapConnect();
        Modification modCn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "cn", "Horatio Torquemeda Marley");
        connection.modify(toDn(ACCOUNT_HT_UID), modCn);
		ldapDisconnect(connection);

		waitForTaskNextRun(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user "+ACCOUNT_HT_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, "Horatio Torquemeda Marley", ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

        assertStepSyncToken(getSyncTaskOid(), 6, tsStart, tsEnd);

	}
	
	@Test
    public void test837RenameAccount() throws Exception {
		final String TEST_NAME = "test837RenameAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        LdapNetworkConnection connection = ldapConnect();
        
        ModifyDnRequest modDnRequest = new ModifyDnRequestImpl();
        modDnRequest.setName(new Dn(toDn(ACCOUNT_HT_UID)));
        modDnRequest.setNewRdn(new Rdn("uid=htm"));
        modDnRequest.setDeleteOldRdn(true);
		ModifyDnResponse modDnResponse = connection.modifyDn(modDnRequest);
        
		display("Modified "+toDn(ACCOUNT_HT_UID)+" -> uid=htm: "+modDnResponse);
		
		ldapDisconnect(connection);

		waitForTaskNextRun(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername("htm");
        assertNotNull("No user "+"htm"+" created", user);
        assertUser(user, user.getOid(), "htm", "Horatio Torquemeda Marley", ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);
        assertNull("User "+ACCOUNT_HT_UID+" still exist", findUserByUsername(ACCOUNT_HT_UID));

        assertStepSyncToken(getSyncTaskOid(), 7, tsStart, tsEnd);

	}
	
	// TODO: create object of a different object class. See that it is ignored by sync.
	
	@Test
    public void test838DeleteAccountHtm() throws Exception {
		final String TEST_NAME = "test838DeleteAccountHtm";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        deleteLdapEntry(toDn("htm"));

		waitForTaskNextRun(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        assertNull("User "+"htm"+" still exist", findUserByUsername("htm"));
        assertNull("User "+ACCOUNT_HT_UID+" still exist", findUserByUsername(ACCOUNT_HT_UID));

        assertStepSyncToken(getSyncTaskOid(), 8, tsStart, tsEnd);
	}

	@Test
    public void test839DeleteSyncTask() throws Exception {
		final String TEST_NAME = "test839DeleteSyncTask";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        deleteObject(TaskType.class, getSyncTaskOid(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoObject(TaskType.class, getSyncTaskOid(), task, result);
	}
	
	protected Entry getLdapAccountByUid(String uid) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		List<Entry> entries = ldapSearch(connection, "(uid="+uid+")");
		ldapDisconnect(connection);

		assertEquals("Unexpected number of entries for uid="+uid+": "+entries, 1, entries.size());
		Entry entry = entries.get(0);

		return entry;
	}
	
	protected Entry assertLdapAccount(String uid, String cn) throws LdapException, IOException, CursorException {
		Entry entry = getLdapAccountByUid(uid);
		assertAttribute(entry, "cn", cn);
		return entry;
	}
	
	protected void assertAttribute(Entry entry, String attrName, String expectedValue) throws LdapInvalidAttributeValueException {
		String dn = entry.getDn().toString();
		Attribute ldapAttribute = entry.get(attrName);
		if (ldapAttribute == null) {
			if (expectedValue == null) {
				return;
			} else {
				AssertJUnit.fail("No attribute "+attrName+" in "+dn+", expected: "+expectedValue);
			}
		} else {
			assertEquals("Wrong attribute "+attrName+" in "+dn, expectedValue, ldapAttribute.getString());
		}
	}
	
	protected void assertNoLdapAccount(String uid) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		List<Entry> entries = ldapSearch(connection, "(uid="+uid+")");
		ldapDisconnect(connection);

		assertEquals("Unexpected number of entries for uid="+uid+": "+entries, 0, entries.size());
	}
	
	protected List<Entry> ldapSearch(LdapNetworkConnection connection, String filter) throws LdapException, CursorException {
		return ldapSearch(connection, getLdapSuffix(), filter, SearchScope.SUBTREE, "*", ATTRIBUTE_ENTRY_UUID);
	}
	
	protected List<Entry> ldapSearch(LdapNetworkConnection connection, String baseDn, String filter, SearchScope scope, String... attributes) throws LdapException, CursorException {
		List<Entry> entries = new ArrayList<Entry>();
		EntryCursor entryCursor = connection.search( baseDn, filter, scope, attributes );
		Entry entry = null;
		while (entryCursor.next()) {
			entries.add(entryCursor.get());
		}
		return entries;
	}

	protected Entry addLdapAccount(String uid, String cn, String givenName, String sn) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		Entry entry = createAccountEntry(uid, cn, givenName, sn);
		connection.add(entry);
		display("Added LDAP account:"+entry);
		ldapDisconnect(connection);
		return entry;
	}

	protected Entry createAccountEntry(String uid, String cn, String givenName, String sn) throws LdapException {
		Entry entry = new DefaultEntry(toDn(uid),
				"objectclass", LDAP_ACCOUNT_OBJECTCLASS,
				"uid", uid,
				"cn", cn,
				"givenName", givenName,
				"sn", sn);
		return entry;
	}
	
	protected Entry addLdapGroup(String cn, String description, String memberDn) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		Entry entry = createGroupEntry(cn, description, memberDn);
		connection.add(entry);
		display("Added LDAP group:"+entry);
		ldapDisconnect(connection);
		return entry;
	}

	protected Entry createGroupEntry(String cn, String description, String memberDn) throws LdapException {
		Entry entry = new DefaultEntry(toGroupDn(cn),
				"objectclass", getLdapGroupObjectClass(),
				"cn", cn,
				"description", description,
				getLdapGroupMemberAttribute(), memberDn);
		return entry;
	}

	protected void deleteLdapEntry(String dn) throws LdapException, IOException {
		LdapNetworkConnection connection = ldapConnect();
		connection.delete(dn);
		display("Deleted LDAP entry: "+dn);
		ldapDisconnect(connection);
	}
	
	protected String toDn(String username) {
		return "uid="+username+","+getPeopleLdapSuffix();
	}
	
	protected String toGroupDn(String cn) {
		return "cn="+cn+","+getGroupsLdapSuffix();
	}
	
	protected LdapNetworkConnection ldapConnect() throws LdapException {
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(getLdapServerHost());
		config.setLdapPort(getLdapServerPort());
		LdapNetworkConnection connection = new LdapNetworkConnection(config);
		boolean connected = connection.connect();
		if (!connected) {
			AssertJUnit.fail("Cannot connect to LDAP server "+getLdapServerHost()+":"+getLdapServerPort());
		}
		BindRequest bindRequest = new BindRequestImpl();
		bindRequest.setDn(new Dn(getLdapBindDn()));
		bindRequest.setCredentials(getLdapBindPassword());
		BindResponse bindResponse = connection.bind(bindRequest);
		return connection;
	}

	protected void ldapDisconnect(LdapNetworkConnection connection) throws IOException {
		connection.close();
	}
	
	protected void assertAccountShadow(PrismObject<ShadowType> shadow, String dn) throws SchemaException {
		assertShadowCommon(shadow, null, dn, resourceType, getAccountObjectClass(), ciMatchingRule);
	}

	protected long roundTsDown(long ts) {
		return (((long)(ts/1000))*1000);
	}
	
	protected long roundTsUp(long ts) {
		return (((long)(ts/1000))*1000)+1;
	}
}
