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
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
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
	
	private static final String LDAP_GROUP_PIRATES_DN = "cn=Pirates,ou=groups,dc=example,dc=com";

	private static final String ATTRIBUTE_ENTRY_UUID = "entryUuid";
	
	protected static final String ACCOUNT_0_UID = "u00000000";

	private static final int NUMBER_OF_GENERTED_ACCOUNTS = 4000;
	
	protected ResourceType resourceType;
	protected PrismObject<ResourceType> resource;
	
	private static String stopCommand;
	
	protected String account0Oid;

    @Autowired
    private ReconciliationTaskHandler reconciliationTaskHandler;
	
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

	protected abstract File getResourceFile();
	
	protected QName getAccountObjectClass() {
		return new QName(MidPointConstants.NS_RI, "AccountObjectClass");
	}

	
	protected abstract String getLdapServerHost();
	
	protected abstract int getLdapServerPort();
	
	protected abstract String getLdapBindDn();
	
	protected abstract String getLdapBindPassword();

	protected String getLdapSuffix() {
		return "dc=example,dc=com";
	}
	
	protected String getPeopleLdapSuffix() {
		return "ou=people,"+getLdapSuffix();
	}

	protected String getGroupsLdapSuffix() {
		return "ou=people,"+getLdapSuffix();
	}
	
	protected String getScriptDirectoryName() {
		return "/opt/Bamboo/local/conntest";
	}
	
	protected abstract String getAccount0Cn();

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		modelService.postInit(initResult);
		
		// System Configuration
        PrismObject<SystemConfigurationType> config;
		try {
			config = repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, SystemConfigurationType.class, initResult);
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}

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
	
	// TODO: search 

	@Test
    public void test100SeachAccount0ByLdapUid() throws Exception {
		final String TEST_NAME = "test100SeachAccount0ByLdapUid";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
        ObjectClassComplexTypeDefinition objectClassDefinition = refinedSchema.findObjectClassDefinition(getAccountObjectClass());
        ResourceAttributeDefinition ldapUidAttrDef = objectClassDefinition.findAttributeDefinition("uid");
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndAccountQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        ObjectFilter additionalFilter = EqualFilter.createEqual(
        		new ItemPath(ShadowType.F_ATTRIBUTES, ldapUidAttrDef.getName()), ldapUidAttrDef, ACCOUNT_0_UID);
		ObjectQueryUtil.filterAnd(query.getFilter(), additionalFilter);
                
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        
        // TODO: check shadow
	}
	
	@Test
    public void test150SeachAllAccounts() throws Exception {
		final String TEST_NAME = "test150SeachAllAccounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndAccountQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        final MutableInt count = new MutableInt(0);
        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
				LOGGER.trace("Found {}", object);
				count.increment();
				return true;
			}
		};
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.searchObjectsIterative(ShadowType.class, query, handler, null, task, result);
        
        assertEquals("Unexpected number of accounts", NUMBER_OF_GENERTED_ACCOUNTS + 1, count.getValue());
        
        // TODO: count shadows
	}
	
	@Test
    public void test200AssignAccountToBarbossa() throws Exception {
		final String TEST_NAME = "test200AssignAccountToBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_BARBOSSA_OID, getResourceOid(), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(shadow);
        String icfsUid = (String) identifiers.iterator().next().getRealValue();
        
        assertEquals("Wrong ICFS UID", entry.get(ATTRIBUTE_ENTRY_UUID).getString(), icfsUid);
	}
	
	// TODO: modify the account
	
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
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertNoLinkedAccount(user);
	}

	// TODO: sync tests

//	private Entry createEntry(String uid, String name) throws IOException {
//		StringBuilder sb = new StringBuilder();
//		String dn = "uid="+uid+","+getPeopleLdapSuffix();
//		sb.append("dn: ").append(dn).append("\n");
//		sb.append("objectClass: inetOrgPerson\n");
//		sb.append("uid: ").append(uid).append("\n");
//		sb.append("cn: ").append(name).append("\n");
//		sb.append("sn: ").append(name).append("\n");
//	}
	
	protected Entry assertLdapAccount(String uid, String cn) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		List<Entry> entries = ldapSearch(connection, "(uid="+uid+")");
		ldapDisconnect(connection);

		assertEquals("Unexpected number of entries for uid="+uid+": "+entries, 1, entries.size());
		Entry entry = entries.get(0);

		String dn = entry.getDn().toString();
		assertEquals("Wrong cn in "+dn, cn, entry.get("cn").getString());
		return entry;
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


	protected String toDn(String username) {
		return "uid="+username+","+getPeopleLdapSuffix();
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
}
