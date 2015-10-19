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
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.directory.api.ldap.codec.api.BinaryAttributeDetector;
import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.CursorLdapReferralException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.ModifyDnRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyDnResponse;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Ava;
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
import com.evolveum.midpoint.schema.processor.ResourceSchema;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractLdapTest extends AbstractModelIntegrationTest {
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractLdapTest.class);
	
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
	protected static final String USER_BARBOSSA_PASSWORD = "deadjack.tellnotales123";
	
	// Barbossa after rename
	protected static final String USER_CPTBARBOSSA_USERNAME = "cptbarbossa";
	
	protected static final File USER_GUYBRUSH_FILE = new File (COMMON_DIR, "user-guybrush.xml");
	protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
	protected static final String USER_GUYBRUSH_USERNAME = "guybrush";
	protected static final String USER_GUYBRUSH_FULL_NAME = "Guybrush Threepwood";
			
	private static final String LDAP_INETORGPERSON_OBJECTCLASS = "inetOrgPerson";
		
	@Autowired(required = true)
	protected MatchingRuleRegistry matchingRuleRegistry;
	
    @Autowired
    protected ReconciliationTaskHandler reconciliationTaskHandler;
	
	protected ResourceType resourceType;
	protected PrismObject<ResourceType> resource;
	
	protected MatchingRule<String> ciMatchingRule;
	
	private static String stopCommand;
    
    protected ObjectClassComplexTypeDefinition accountObjectClassDefinition;
    
    protected DefaultConfigurableBinaryAttributeDetector binaryAttributeDetector = new DefaultConfigurableBinaryAttributeDetector();
	
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
		return new QName(MidPointConstants.NS_RI, getLdapAccountObjectClass());
	}
	
	protected String getLdapAccountObjectClass() {
		return LDAP_INETORGPERSON_OBJECTCLASS;
	}
	
	protected QName getGroupObjectClass() {
		return new QName(MidPointConstants.NS_RI, getLdapGroupObjectClass());
	}
	
	protected abstract String getLdapServerHost();
	
	protected abstract int getLdapServerPort();
	
	protected boolean useSsl() {
		return false;
	}
	
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
		return "ou=groups,"+getLdapSuffix();
	}
	
	public String getPrimaryIdentifierAttributeName() {
    	return "entryUUID";
    }
	
	public QName getPrimaryIdentifierAttributeQName() {
    	return new QName(MidPointConstants.NS_RI,getPrimaryIdentifierAttributeName());
    }
	
	protected abstract String getLdapGroupObjectClass();
	
	protected abstract String getLdapGroupMemberAttribute();
	
	protected boolean needsGroupFakeMemeberEntry() {
		return false;
	}
	
	protected String getScriptDirectoryName() {
		return "/opt/Bamboo/local/conntest";
	}
	
	protected boolean isImportResourceAtInit() {
		return true;
	}
	
	protected boolean allowDuplicateSearchResults() {
		return false;
	}
	
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
				
		// Roles
		
		// Resources
		if (isImportResourceAtInit()) {
			resource = importAndGetObjectFromFile(ResourceType.class, getResourceFile(), getResourceOid(), initTask, initResult);
			resourceType = resource.asObjectable();
		}
		
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
		
		OperationResult	testResult = provisioningService.testResource(getResourceOid());
		
		display("Test connection result",testResult);
		TestUtil.assertSuccess("Test connection failed",testResult);
	}
	
	@Test
    public void test020Schema() throws Exception {
		final String TEST_NAME = "test020Schema";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        ResourceSchema resourceSchema = RefinedResourceSchema.getResourceSchema(resource, prismContext);
        display("Resource schema", resourceSchema);
        
        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
        display("Refined schema", refinedSchema);
        accountObjectClassDefinition = refinedSchema.findObjectClassDefinition(getAccountObjectClass());
        assertNotNull("No definition for object class "+getAccountObjectClass(), accountObjectClassDefinition);
        display("Account object class def", accountObjectClassDefinition);
        
        ResourceAttributeDefinition<String> cnDef = accountObjectClassDefinition.findAttributeDefinition("cn");
        PrismAsserts.assertDefinition(cnDef, new QName(MidPointConstants.NS_RI, "cn"), DOMUtil.XSD_STRING, 1, -1);
        assertTrue("cn read", cnDef.canRead());
        assertTrue("cn modify", cnDef.canModify());
        assertTrue("cn add", cnDef.canAdd());
        
        ResourceAttributeDefinition<String> oDef = accountObjectClassDefinition.findAttributeDefinition("o");
        PrismAsserts.assertDefinition(oDef, new QName(MidPointConstants.NS_RI, "o"), DOMUtil.XSD_STRING, 0, -1);
        assertTrue("o read", oDef.canRead());
        assertTrue("o modify", oDef.canModify());
        assertTrue("o add", oDef.canAdd());
        
        ResourceAttributeDefinition<Long> createTimestampDef = accountObjectClassDefinition.findAttributeDefinition("createTimestamp");
        PrismAsserts.assertDefinition(createTimestampDef, new QName(MidPointConstants.NS_RI, "createTimestamp"),
        		DOMUtil.XSD_LONG, 0, 1);
        assertTrue("createTimestampDef read", createTimestampDef.canRead());
        assertFalse("createTimestampDef read", createTimestampDef.canModify());
        assertFalse("createTimestampDef read", createTimestampDef.canAdd());
        
	}
	
	protected <T> ObjectFilter createAttributeFilter(String attrName, T attrVal) throws SchemaException {
		ResourceAttributeDefinition ldapUidAttrDef = accountObjectClassDefinition.findAttributeDefinition(attrName);
        return EqualFilter.createEqual(
        		new ItemPath(ShadowType.F_ATTRIBUTES, ldapUidAttrDef.getName()), ldapUidAttrDef, attrVal);
	}
	
	protected ObjectQuery createUidQuery(String uid) throws SchemaException {
		ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
		ObjectQueryUtil.filterAnd(query.getFilter(), createAttributeFilter("uid", uid));
		return query;
	}
	
	protected SearchResultList<PrismObject<ShadowType>> doSearch(final String TEST_NAME, ObjectQuery query, int expectedSize, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		return doSearch(TEST_NAME, query, null, expectedSize, task, result);
	}
	
	protected SearchResultList<PrismObject<ShadowType>> doSearch(final String TEST_NAME, ObjectQuery query, GetOperationOptions rootOptions, int expectedSize, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		final List<PrismObject<ShadowType>> foundObjects = new ArrayList<PrismObject<ShadowType>>(expectedSize);
        ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
			@Override
			public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
//				LOGGER.trace("Found {}", object);
				String name = object.asObjectable().getName().getOrig();
				for(PrismObject<ShadowType> foundShadow: foundObjects) {
					if (!allowDuplicateSearchResults() && foundShadow.asObjectable().getName().getOrig().equals(name)) {
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
		
	protected Entry getLdapAccountByUid(String uid) throws LdapException, IOException, CursorException {
		return searchLdapAccount("(uid="+uid+")");
	}
	
	protected Entry getLdapAccountByCn(String cn) throws LdapException, IOException, CursorException {
		return searchLdapAccount("(cn="+cn+")");
	}
	
	protected Entry searchLdapAccount(String filter) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		List<Entry> entries = ldapSearch(connection, filter);
		ldapDisconnect(connection);

		assertEquals("Unexpected number of entries for "+filter+": "+entries, 1, entries.size());
		Entry entry = entries.get(0);

		return entry;
	}
	
	protected Entry assertLdapAccount(String uid, String cn) throws LdapException, IOException, CursorException {
		Entry entry = getLdapAccountByUid(uid);
		assertAttribute(entry, "cn", cn);
		return entry;
	}
	
	protected Entry getLdapGroupByName(String name) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		List<Entry> entries = ldapSearch(connection, "(&(cn="+name+")(objectClass="+getLdapGroupObjectClass()+"))");
		ldapDisconnect(connection);

		assertEquals("Unexpected number of entries for group cn="+name+": "+entries, 1, entries.size());
		Entry entry = entries.get(0);

		return entry;
	}
	
	protected Entry assertLdapGroup(String cn) throws LdapException, IOException, CursorException {
		Entry entry = getLdapGroupByName(cn);
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
	
	protected void assertAttributeContains(Entry entry, String attrName, String expectedValue) throws LdapInvalidAttributeValueException {
		String dn = entry.getDn().toString();
		Attribute ldapAttribute = entry.get(attrName);
		if (ldapAttribute == null) {
			if (expectedValue == null) {
				return;
			} else {
				AssertJUnit.fail("No attribute "+attrName+" in "+dn+", expected: "+expectedValue);
			}
		} else {
			List<String> vals = new ArrayList<>();
			Iterator<Value<?>> iterator = ldapAttribute.iterator();
			while (iterator.hasNext()) {
				Value<?> value = iterator.next();
				if (expectedValue.equals(value.getString())) {
					return;
				}
				vals.add(value.getString());
			}
			AssertJUnit.fail("Wrong attribute "+attrName+" in "+dn+" expected to contain value " + expectedValue + " but it has values " + vals);
		}
	}
	
	protected void assertAttributeNotContains(Entry entry, String attrName, String expectedValue) throws LdapInvalidAttributeValueException {
		String dn = entry.getDn().toString();
		Attribute ldapAttribute = entry.get(attrName);
		if (ldapAttribute == null) {
			return;
		} else {
			Iterator<Value<?>> iterator = ldapAttribute.iterator();
			while (iterator.hasNext()) {
				Value<?> value = iterator.next();
				if (expectedValue.equals(value.getString())) {
					AssertJUnit.fail("Attribute "+attrName+" in "+dn+" contains value " + expectedValue + ", but it should not have it");
				}
			}
		}
	}
	
	protected Entry getLdapEntry(String dn) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		Entry entry = getLdapEntry(connection, dn);
		ldapDisconnect(connection);
		return entry;
	}
	
	protected Entry getLdapEntry(LdapNetworkConnection connection, String dn) throws LdapException, IOException, CursorException {
		List<Entry> entries = ldapSearch(connection, dn, "(objectclass=*)", SearchScope.OBJECT, "*");
		if (entries.isEmpty()) {
			return null;
		}
		return entries.get(0);
	}
	
	protected void assertNoLdapAccount(String uid) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		List<Entry> entries = ldapSearch(connection, "(uid="+uid+")");
		ldapDisconnect(connection);

		assertEquals("Unexpected number of entries for uid="+uid+": "+entries, 0, entries.size());
	}
	
	protected void assertLdapGroupMember(Entry accountEntry, String groupName) throws LdapException, IOException, CursorException {
		Entry groupEntry = getLdapGroupByName(groupName);
		assertAttributeContains(groupEntry, getLdapGroupMemberAttribute(), accountEntry.getDn().toString());
	}
	
	protected void assertLdapNoGroupMember(Entry accountEntry, String groupName) throws LdapException, IOException, CursorException {
		Entry groupEntry = getLdapGroupByName(groupName);
		assertAttributeNotContains(groupEntry, getLdapGroupMemberAttribute(), accountEntry.getDn().toString());
	}
	
	protected List<Entry> ldapSearch(LdapNetworkConnection connection, String filter) throws LdapException, CursorException {
		return ldapSearch(connection, getLdapSuffix(), filter, SearchScope.SUBTREE, "*", getPrimaryIdentifierAttributeName());
	}
	
	protected List<Entry> ldapSearch(LdapNetworkConnection connection, String baseDn, String filter, SearchScope scope, String... attributes) throws LdapException, CursorException {
		LOGGER.trace("LDAP search base={}, filter={}, scope={}, attributes={}",
				new Object[]{baseDn, filter, scope, attributes});
		
		SearchRequest searchRequest = new SearchRequestImpl();
		searchRequest.setBase(new Dn(baseDn));
		searchRequest.setFilter(filter);
		searchRequest.setScope(scope);
		searchRequest.addAttributes(attributes);
		searchRequest.ignoreReferrals();
		
		List<Entry> entries = new ArrayList<Entry>();
		try {
			SearchCursor searchCursor = connection.search(searchRequest);
			while (searchCursor.next()) {
				Response response = searchCursor.get();
				if (response instanceof SearchResultEntry) {
					Entry entry = ((SearchResultEntry)response).getEntry();
					entries.add(entry);
				}
			}
		} catch (CursorLdapReferralException e) {
			throw new IllegalStateException("Got referral to: "+e.getReferralInfo(), e);
		}
		return entries;
	}
	
	protected void assertLdapPassword(String uid, String password) throws LdapException, IOException, CursorException {
		Entry entry = getLdapAccountByUid(uid);
		assertLdapPassword(entry, password);
	}
	
	protected void assertLdapPassword(Entry entry, String password) throws LdapException, IOException, CursorException {
		LdapNetworkConnection conn = ldapConnect(entry.getDn().toString(), password);
		assertTrue("Not connected", conn.isConnected());
		assertTrue("Not authenticated", conn.isAuthenticated());
	}

	protected Entry addLdapAccount(String uid, String cn, String givenName, String sn) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		Entry entry = createAccountEntry(uid, cn, givenName, sn);
		try {
			connection.add(entry);
			display("Added LDAP account:\n"+entry);
		} catch (Exception e) {
			display("Error adding entry:\n"+entry+"\nError: "+e.getMessage());
			ldapDisconnect(connection);
			throw e;
		}
		ldapDisconnect(connection);
		return entry;
	}

	protected Entry createAccountEntry(String uid, String cn, String givenName, String sn) throws LdapException {
		Entry entry = new DefaultEntry(toAccountDn(uid),
				"objectclass", getLdapAccountObjectClass(),
				"uid", uid,
				"cn", cn,
				"givenName", givenName,
				"sn", sn);
		return entry;
	}
	
	protected Entry addLdapGroup(String cn, String description, String... memberDns) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		Entry entry = createGroupEntry(cn, description, memberDns);
		connection.add(entry);
		display("Added LDAP group:"+entry);
		ldapDisconnect(connection);
		return entry;
	}

	protected Entry createGroupEntry(String cn, String description, String... memberDns) throws LdapException {
		Entry entry = new DefaultEntry(toGroupDn(cn),
				"objectclass", getLdapGroupObjectClass(),
				"cn", cn,
				"description", description);
		if (memberDns != null && memberDns.length > 0) {
			entry.add(getLdapGroupMemberAttribute(), memberDns);
		}
		return entry;
	}

	protected void deleteLdapEntry(String dn) throws LdapException, IOException {
		LdapNetworkConnection connection = ldapConnect();
		connection.delete(dn);
		display("Deleted LDAP entry: "+dn);
		ldapDisconnect(connection);
	}
	
	/**
	 * Silent delete. Used to clean up after previous test runs.
	 */
	protected void cleanupDelete(String dn) throws LdapException, IOException, CursorException {
		LdapNetworkConnection connection = ldapConnect();
		Entry entry = getLdapEntry(connection, dn);
		if (entry != null) {
			connection.delete(dn);
			display("Cleaning up LDAP entry: "+dn);
		}
		ldapDisconnect(connection);
	}
	
	protected String toAccountDn(String username, String fullName) {
		return toAccountDn(username);
	}
	
	protected String toAccountDn(String username) {
		return "uid="+username+","+getPeopleLdapSuffix();
	}
	
	protected Rdn toAccountRdn(String username, String fullName) {
		try {
			return new Rdn(new Ava("uid", username));
		} catch (LdapInvalidDnException e) {
			throw new IllegalStateException(e.getMessage(),e);
		}
	}
	
	protected String toGroupDn(String cn) {
		return "cn="+cn+","+getGroupsLdapSuffix();
	}
	
	protected String getAttributeAsString(Entry entry, String primaryIdentifierAttributeName) throws LdapInvalidAttributeValueException {
		if ("dn".equals(primaryIdentifierAttributeName)) {
			return entry.getDn().toString();
		} else {
			return entry.get(primaryIdentifierAttributeName).getString();
		}
	}
	
	protected LdapNetworkConnection ldapConnect() throws LdapException {
		return ldapConnect(getLdapBindDn(), getLdapBindPassword());
	}
	
	protected LdapNetworkConnection ldapConnect(String bindDn, String bindPassword) throws LdapException {
		LOGGER.trace("LDAP connect to {}:{} as {}",
				getLdapServerHost(), getLdapServerPort(), bindDn);
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost(getLdapServerHost());
		config.setLdapPort(getLdapServerPort());
		
		if (useSsl()) {
			config.setUseSsl(true);
			TrustManager trustManager = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
					
				}
				public void checkServerTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
				}
				public X509Certificate[] getAcceptedIssuers() {
					return new X509Certificate[0];
				}
			};
			config.setTrustManagers(trustManager);
		}
		
		config.setBinaryAttributeDetector(binaryAttributeDetector);
		
		LdapNetworkConnection connection = new LdapNetworkConnection(config);
		boolean connected = connection.connect();
		if (!connected) {
			AssertJUnit.fail("Cannot connect to LDAP server "+getLdapServerHost()+":"+getLdapServerPort());
		}
		LOGGER.trace("LDAP connected to {}:{}, executing bind as {}",
				getLdapServerHost(), getLdapServerPort(), bindDn);
		BindRequest bindRequest = new BindRequestImpl();
		bindRequest.setDn(new Dn(bindDn));
		bindRequest.setCredentials(bindPassword);
		bindRequest.setSimple(true);
		BindResponse bindResponse = connection.bind(bindRequest);
		if (bindResponse.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS) {
			throw new SecurityException("Bind as "+bindDn+" failed: "+bindResponse.getLdapResult().getDiagnosticMessage()+" ("+bindResponse.getLdapResult().getResultCode()+")");
		}
		LOGGER.trace("LDAP connected to {}:{}, bound as {}",
				getLdapServerHost(), getLdapServerPort(), bindDn);
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
