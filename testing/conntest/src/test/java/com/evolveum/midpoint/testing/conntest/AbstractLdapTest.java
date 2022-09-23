/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import static com.evolveum.midpoint.test.IntegrationTestTools.displayXml;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.QNAME_CN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.test.IntegrationTestTools.LDAP_CONNECTOR_TYPE;

import java.io.File;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.*;

import org.apache.directory.api.ldap.codec.api.DefaultConfigurableBinaryAttributeDetector;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.CursorLdapReferralException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.message.*;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.sync.tasks.recon.ReconciliationLauncher;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.ConnectorOperationalStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.Lsof;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.*;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-conntest-test-main.xml" })
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractLdapTest extends AbstractModelIntegrationTest {

    public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
    public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

    protected static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
    protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
    protected static final String USER_ADMINISTRATOR_USERNAME = "administrator";

    protected static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
    protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

    protected static final File ROLE_END_USER_FILE = new File(COMMON_DIR, "role-end-user.xml");
    protected static final String ROLE_END_USER_OID = "00000000-0000-0000-0000-000000000008";

    protected static final File USER_BARBOSSA_FILE = new File(COMMON_DIR, "user-barbossa.xml");
    protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
    protected static final String USER_BARBOSSA_USERNAME = "barbossa";
    protected static final String USER_BARBOSSA_FULL_NAME = "Hector Barbossa";
    protected static final String USER_BARBOSSA_PASSWORD = "deadjack.tellnotales123";
    protected static final String USER_BARBOSSA_PASSWORD_2 = "hereThereBeMonsters";
    protected static final String USER_BARBOSSA_PASSWORD_AD_1 = "There.Be.Mönsters.111"; // MID-5242
    protected static final String USER_BARBOSSA_PASSWORD_AD_2 = "Thére.Be.Mönšters.222"; // MID-5242
    protected static final String USER_BARBOSSA_PASSWORD_AD_3 = "There.Be.Monsters.333";

    // Barbossa after rename
    protected static final String USER_CPTBARBOSSA_USERNAME = "cptbarbossa";

    protected static final File USER_GUYBRUSH_FILE = new File(COMMON_DIR, "user-guybrush.xml");
    protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
    protected static final String USER_GUYBRUSH_USERNAME = "guybrush";
    protected static final String USER_GUYBRUSH_FULL_NAME = "Guybrush Threepwood";

    protected static final File USER_LECHUCK_FILE = new File(COMMON_DIR, "user-lechuck.xml");
    protected static final String USER_LECHUCK_OID = "0201583e-ffca-11e5-a949-affff1aa5a60";
    protected static final String USER_LECHUCK_USERNAME = "lechuck";
    protected static final String USER_LECHUCK_FULL_NAME = "LeChuck";

    protected static final String LDAP_INETORGPERSON_OBJECTCLASS = "inetOrgPerson";
    protected static final String LDAP_ATTRIBUTE_ROOM_NUMBER = "roomNumber";

    protected static final QName ASSOCIATION_GROUP_NAME = new QName(MidPointConstants.NS_RI, "group");

    @Autowired
    protected MatchingRuleRegistry matchingRuleRegistry;

    @Autowired
    protected ReconciliationLauncher reconciliationLauncher;

    protected ResourceType resourceType;
    protected PrismObject<ResourceType> resource;

    protected MatchingRule<String> dnMatchingRule;
    protected MatchingRule<String> ciMatchingRule;

    private static String stopCommand;

    /** Should be object type (a.k.a. refined) definition, if available. */
    protected ResourceObjectDefinition accountDefinition;

    protected DefaultConfigurableBinaryAttributeDetector binaryAttributeDetector = new DefaultConfigurableBinaryAttributeDetector();

    protected Lsof lsof;

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

    protected String getResourceNamespace() {
        return MidPointConstants.NS_RI;
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

    protected String getLdapBindDn() {
        return "CN=midpoint," + getPeopleLdapSuffix();
    }

    protected abstract String getLdapBindPassword();

    protected abstract int getSearchSizeLimit();

    protected String getLdapSuffix() {
        return "dc=example,dc=com";
    }

    protected String getPeopleLdapSuffix() {
        return "ou=People," + getLdapSuffix();
    }

    protected String getGroupsLdapSuffix() {
        return "ou=groups," + getLdapSuffix();
    }

    public String getPrimaryIdentifierAttributeName() {
        return "entryUUID";
    }

    public QName getPrimaryIdentifierAttributeQName() {
        return new QName(MidPointConstants.NS_RI, getPrimaryIdentifierAttributeName());
    }

    protected abstract String getLdapGroupObjectClass();

    protected abstract String getLdapGroupMemberAttribute();

    protected boolean needsGroupFakeMemberEntry() {
        return false;
    }

    protected boolean isUsingGroupShortcutAttribute() {
        return true;
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

    protected boolean isGroupMemberMandatory() {
        return true;
    }

    protected boolean isAssertOpenFiles() {
        return false;
    }

    protected QName getAssociationGroupName() {
        return new QName(MidPointConstants.NS_RI, "group");
    }

    protected String getLdapConnectorClassName() {
        return LDAP_CONNECTOR_TYPE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // System Configuration
        PrismObject<SystemConfigurationType> config;
        try {
            config = repoAddObjectFromFile(SYSTEM_CONFIGURATION_FILE, initResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
                    "looks like the previous test haven't cleaned it up", e);
        }

        modelService.postInit(initResult);

        // TODO what now? config is unused
        // to get profiling facilities (until better API is available)
//        LoggingConfigurationManager.configure(
//                ProfilingConfigurationManager.checkSystemProfilingConfiguration(config),
//                config.asObjectable().getVersion(), initResult);

        // administrator
        PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, initResult);
        repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
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

        ciMatchingRule = matchingRuleRegistry.getMatchingRule(PrismConstants.STRING_IGNORE_CASE_MATCHING_RULE_NAME, DOMUtil.XSD_STRING);
        dnMatchingRule = matchingRuleRegistry.getMatchingRule(PrismConstants.DISTINGUISHED_NAME_MATCHING_RULE_NAME, DOMUtil.XSD_STRING);

        logTrustManagers();

        if (isAssertOpenFiles()) {
            lsof = new Lsof(TestUtil.getPid());
        }
    }

    @Test
    public void test010PartialConfigurationResourceObject() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObject<ResourceType> resourceFromRepo = getObject(ResourceType.class, getResourceOid());
        ResourceType resource = new ResourceType()
                .name("newResource")
                .connectorRef(resourceFromRepo.asObjectable().getConnectorRef())
                .connectorConfiguration(resourceFromRepo.asObjectable().getConnectorConfiguration());

        OperationResult testResult = provisioningService.testPartialConfiguration(resource.asPrismObject(), task, result);

        display("Test partial configuration of resource result", testResult);
        TestUtil.assertSuccess("Test partial configuration of resource failed", testResult);

        if (isAssertOpenFiles()) {
            // Set lsof baseline only after the first connection.
            // We will have more reasonable number here.
            lsof.rememberBaseline();
            displayDumpable("lsof baseline", lsof);
        }

        displayXml("Resource after test connection", resource.asPrismObject());

        assertNull("Resource was saved to repo, during partial configuration test", findObjectByName(ResourceType.class, "newResource"));
    }

    @Test
    public void test011Connection() throws Exception {
        Task task = getTestTask();

        OperationResult testResult = provisioningService.testResource(getResourceOid(), task, task.getResult());

        display("Test connection result", testResult);
        TestUtil.assertSuccess("Test connection failed", testResult);

        if (isAssertOpenFiles()) {
            // Set lsof baseline only after the first connection.
            // We will have more reasonable number here.
            lsof.rememberBaseline();
            displayDumpable("lsof baseline", lsof);
        }

        resource = getObject(ResourceType.class, getResourceOid());
        displayXml("Resource after test connection", resource);
    }

    @Test
    public void test012ConnectionResourceObject() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObject<ResourceType> resourceFromRepo = getObject(ResourceType.class, getResourceOid());
        ResourceType resource = new ResourceType()
                .name("newResource")
                .connectorRef(resourceFromRepo.asObjectable().getConnectorRef())
                .connectorConfiguration(resourceFromRepo.asObjectable().getConnectorConfiguration())
                .schema(resourceFromRepo.asObjectable().getSchema());

        OperationResult testResult = provisioningService.testResource(resource.asPrismObject(), task, result);

        display("Test connection result", testResult);
        TestUtil.assertSuccess("Test connection failed", testResult);

        if (isAssertOpenFiles()) {
            // Set lsof baseline only after the first connection.
            // We will have more reasonable number here.
            lsof.rememberBaseline();
            displayDumpable("lsof baseline", lsof);
        }

        displayXml("Resource after test connection", resource.asPrismObject());

        assertNull("Resource was saved to repo, during partial configuration test", findObjectByName(ResourceType.class, "newResource"));
    }

    @Test
    public void test020Schema() throws Exception {
        ResourceSchema resourceSchema = ResourceSchemaFactory.getRawSchema(resource);
        displayDumpable("Raw resource schema", resourceSchema);

        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(resource);
        displayDumpable("Complete resource schema", refinedSchema);

        accountDefinition = refinedSchema.findDefinitionForObjectClassRequired(getAccountObjectClass());
        assertThat(accountDefinition).as("account definition").isInstanceOf(ResourceObjectTypeDefinition.class);
        displayDumpable("Account object class def", accountDefinition);

        ResourceAttributeDefinition<?> cnDef = accountDefinition.findAttributeDefinition("cn");
        PrismAsserts.assertDefinition(cnDef, QNAME_CN, DOMUtil.XSD_STRING, 1, 1);
        assertTrue("cn read", cnDef.canRead());
        assertTrue("cn modify", cnDef.canModify());
        assertTrue("cn add", cnDef.canAdd());

        ResourceAttributeDefinition<?> oDef = accountDefinition.findAttributeDefinition("o");
        PrismAsserts.assertDefinition(oDef, new QName(MidPointConstants.NS_RI, "o"), DOMUtil.XSD_STRING, 0, -1);
        assertTrue("o read", oDef.canRead());
        assertTrue("o modify", oDef.canModify());
        assertTrue("o add", oDef.canAdd());

        ResourceAttributeDefinition<?> createTimestampDef = accountDefinition.findAttributeDefinition(getCreateTimeStampAttributeName());
        PrismAsserts.assertDefinition(createTimestampDef, new QName(MidPointConstants.NS_RI, getCreateTimeStampAttributeName()),
                getTimestampXsdType(), 0, 1);
        assertTrue(getCreateTimeStampAttributeName() + " def read", createTimestampDef.canRead());
        assertFalse(getCreateTimeStampAttributeName() + " def read", createTimestampDef.canModify());
        assertFalse(getCreateTimeStampAttributeName() + " def read", createTimestampDef.canAdd());

        assertStableSystem();
    }

    protected String getCreateTimeStampAttributeName() { return "createTimestamp"; }

    protected QName getTimestampXsdType() {
        return DOMUtil.XSD_DATETIME;
    }

    @Test
    public void test030Capabilities() throws Exception {
        CapabilitiesType capabilities = resourceType.getCapabilities();
        display("Resource capabilities", capabilities);
        assertNotNull("Null capabilities", capabilities);

        CapabilityCollectionType nativeCapabilities = capabilities.getNative();
        assertNotNull("Null native capabilities", nativeCapabilities);
        assertFalse("Empty native capabilities", CapabilityUtil.isEmpty(nativeCapabilities));

        assertCapability(nativeCapabilities, DiscoverConfigurationCapabilityType.class);
        assertCapability(nativeCapabilities, ReadCapabilityType.class);
        assertCapability(nativeCapabilities, CreateCapabilityType.class);
        assertCapability(nativeCapabilities, UpdateCapabilityType.class);
        assertCapability(nativeCapabilities, DeleteCapabilityType.class);

        // TODO: assert password capability. Check password readability.

        ActivationCapabilityType activationCapabilityType = CapabilityUtil.getCapability(nativeCapabilities, ActivationCapabilityType.class);
        assertActivationCapability(activationCapabilityType);

        assertAdditionalCapabilities(nativeCapabilities);

        assertStableSystem();
    }

    protected void assertActivationCapability(ActivationCapabilityType activationCapabilityType) {
        // for subclasses
    }

    protected void assertAdditionalCapabilities(CapabilityCollectionType nativeCapabilities) {
        // for subclasses
    }

    protected <C extends CapabilityType> void assertCapability(CapabilityCollectionType capabilities, Class<C> capabilityClass) {
        C capability = CapabilityUtil.getCapability(capabilities, capabilityClass);
        assertNotNull("No " + capabilityClass.getSimpleName() + " capability", capability);
        assertTrue("Capability " + capabilityClass.getSimpleName() + " is disabled", CapabilityUtil.isCapabilityEnabled(capability));
    }

    protected <T> ObjectFilter createAttributeFilter(String attrName, T attrVal) {
        ResourceAttributeDefinition<?> ldapAttrDef = accountDefinition.findAttributeDefinition(attrName);
        return prismContext.queryFor(ShadowType.class)
                .itemWithDef(ldapAttrDef, ShadowType.F_ATTRIBUTES, ldapAttrDef.getItemName()).eq(attrVal)
                .buildFilter();
    }

    protected ObjectQuery createUidQuery(String uid) throws SchemaException {
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        ObjectQueryUtil.filterAnd(query.getFilter(), createAttributeFilter("uid", uid), prismContext);
        return query;
    }

    protected SearchResultList<PrismObject<ShadowType>> doSearch(ObjectQuery query, int expectedSize, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return doSearch(query, null, expectedSize, task, result);
    }

    protected SearchResultList<PrismObject<ShadowType>> doSearch(ObjectQuery query, GetOperationOptions rootOptions, int expectedSize, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        final List<PrismObject<ShadowType>> foundObjects = new ArrayList<>(expectedSize);
        ResultHandler<ShadowType> handler = (object, parentResult) -> {
            String name = object.asObjectable().getName().getOrig();
            for (PrismObject<ShadowType> foundShadow : foundObjects) {
                if (!allowDuplicateSearchResults() && foundShadow.asObjectable().getName().getOrig().equals(name)) {
                    AssertJUnit.fail("Duplicate name " + name);
                }
            }
            foundObjects.add(object);
            return true;
        };

        Collection<SelectorOptions<GetOperationOptions>> options = null;
        if (rootOptions != null) {
            options = SelectorOptions.createCollection(rootOptions);
        }

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        displayDumpable("Searching shadows, options=" + options + ", query", query);
        SearchResultMetadata searchResultMetadata = modelService.searchObjectsIterative(ShadowType.class, query, handler, options, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        if (expectedSize != foundObjects.size()) {
            if (foundObjects.size() < 10) {
                display("Found objects", foundObjects);
                AssertJUnit.fail("Unexpected number of accounts. Expected " + expectedSize + ", found " + foundObjects.size() + ": " + foundObjects);
            } else {
                AssertJUnit.fail("Unexpected number of accounts. Expected " + expectedSize + ", found " + foundObjects.size() + " (too many to display)");
            }
        }

        return new SearchResultList<>(foundObjects, searchResultMetadata);
    }

    protected Entry getLdapAccountByUid(String uid) throws LdapException, IOException, CursorException {
        return searchLdapAccount("(uid=" + uid + ")");
    }

    protected Entry getLdapAccountByCn(String cn) throws LdapException, IOException, CursorException {
        return getLdapAccountByCn(null, cn);
    }

    protected Entry getLdapAccountByCn(UserLdapConnectionConfig config, String cn) throws LdapException, IOException, CursorException {
        return searchLdapAccount(config, "(cn=" + cn + ")");
    }

    protected Entry searchLdapAccount(String filter) throws LdapException, IOException, CursorException {
        return searchLdapAccount(null, filter);
    }

    protected Entry searchLdapAccount(UserLdapConnectionConfig config, String filter) throws LdapException, IOException, CursorException {
        LdapNetworkConnection connection = ldapConnect(config);
        List<Entry> entries = ldapSearch(config, connection, filter);
        ldapDisconnect(connection);

        assertEquals("Unexpected number of entries for " + filter + ": " + entries, 1, entries.size());

        return entries.get(0);
    }

    protected Entry assertLdapAccount(String uid, String cn) throws LdapException, IOException, CursorException {
        Entry entry = getLdapAccountByUid(uid);
        assertAttribute(entry, "cn", cn);
        return entry;
    }

    protected Entry getLdapGroupByName(String name) throws LdapException, IOException, CursorException {
        LdapNetworkConnection connection = ldapConnect();
        List<Entry> entries = ldapSearch(connection, "(&(cn=" + name + ")(objectClass=" + getLdapGroupObjectClass() + "))");
        ldapDisconnect(connection);

        assertEquals("Unexpected number of entries for group cn=" + name + ": " + entries, 1, entries.size());

        return entries.get(0);
    }

    protected Entry assertLdapGroup(String cn) throws LdapException, IOException, CursorException {
        Entry entry = getLdapGroupByName(cn);
        assertAttribute(entry, "cn", cn);
        return entry;
    }

    protected void assertNoLdapGroup(String cn) throws LdapException, IOException, CursorException {
        LdapNetworkConnection connection = ldapConnect();
        List<Entry> entries = ldapSearch(connection, "(&(cn=" + cn + ")(objectClass=" + getLdapGroupObjectClass() + "))");
        ldapDisconnect(connection);
        assertEquals("Unexpected LDAP group " + cn + ": " + entries, 0, entries.size());
    }

    protected void assertAttribute(Entry entry, String attrName, String expectedValue) throws LdapInvalidAttributeValueException {
        String dn = entry.getDn().toString();
        Attribute ldapAttribute = entry.get(attrName);
        if (ldapAttribute == null) {
            if (expectedValue == null) {
                return;
            } else {
                AssertJUnit.fail("No attribute " + attrName + " in " + dn + ", expected: " + expectedValue);
            }
        } else {
            assertEquals("Wrong attribute " + attrName + " in " + dn, expectedValue, ldapAttribute.getString());
        }
    }

    protected void assertNoAttribute(Entry entry, String attrName) {
        String dn = entry.getDn().toString();
        Attribute ldapAttribute = entry.get(attrName);
        if (ldapAttribute != null) {
            AssertJUnit.fail("Unexpected attribute " + attrName + " in " + dn + ": " + ldapAttribute);
        }
    }

    protected void assertAttributeContains(Entry entry, String attrName, String expectedValue) throws SchemaException {
        assertAttributeContains(entry, attrName, expectedValue, null);
    }

    protected void assertAttributeContains(
            Entry entry, String attrName, String expectedValue, MatchingRule<String> matchingRule)
            throws SchemaException {
        String dn = entry.getDn().toString();
        Attribute ldapAttribute = entry.get(attrName);
        if (ldapAttribute == null) {
            if (expectedValue == null) {
                return;
            } else {
                AssertJUnit.fail("No attribute " + attrName + " in " + dn + ", expected: " + expectedValue);
            }
        } else {
            List<String> vals = new ArrayList<>();
            Iterator<Value> iterator = ldapAttribute.iterator();
            while (iterator.hasNext()) {
                Value value = iterator.next();
                if (matchingRule == null) {
                    if (expectedValue.equals(value.getString())) {
                        return;
                    }
                } else {
                    if (matchingRule.match(expectedValue, value.getString())) {
                        return;
                    }
                }
                vals.add(value.getString());
            }
            AssertJUnit.fail("Wrong attribute " + attrName + " in " + dn + " expected to contain value " + expectedValue + " but it has values " + vals);
        }
    }

    protected void assertAttributeNotContains(Entry entry, String attrName, String expectedValue) throws SchemaException {
        assertAttributeNotContains(entry, attrName, expectedValue, null);
    }

    protected void assertAttributeNotContains(Entry entry, String attrName, String expectedValue, MatchingRule<String> matchingRule) throws SchemaException {
        String dn = entry.getDn().toString();
        Attribute ldapAttribute = entry.get(attrName);
        if (ldapAttribute != null) {
            for (Value value : ldapAttribute) {
                if (matchingRule == null) {
                    if (expectedValue.equals(value.getString())) {
                        AssertJUnit.fail("Attribute " + attrName + " in " + dn + " contains value " + expectedValue + ", but it should not have it");
                    }
                } else {
                    if (matchingRule.match(expectedValue, value.getString())) {
                        AssertJUnit.fail("Attribute " + attrName + " in " + dn + " contains value " + expectedValue + ", but it should not have it");
                    }
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

    protected Entry getLdapEntry(LdapNetworkConnection connection, String dn) throws LdapException, CursorException {
        List<Entry> entries = ldapSearch(connection, dn, "(objectclass=*)", SearchScope.OBJECT, "*");
        if (entries.isEmpty()) {
            return null;
        }
        return entries.get(0);
    }

    protected void assertNoLdapAccount(String uid) throws LdapException, IOException, CursorException {
        LdapNetworkConnection connection = ldapConnect();
        List<Entry> entries = ldapSearch(connection, "(uid=" + uid + ")");
        ldapDisconnect(connection);

        assertEquals("Unexpected number of entries for uid=" + uid + ": " + entries, 0, entries.size());
    }

    protected void assertNoEntry(String dn) throws LdapException, IOException, CursorException {
        Entry entry = getLdapEntry(dn);
        assertNull("Expected no entry " + dn + ", but found " + entry, entry);
    }

    protected void assertLdapGroupMember(Entry accountEntry, String groupName) throws LdapException, IOException, CursorException, SchemaException {
        assertLdapGroupMember(accountEntry.getDn().toString(), groupName);
    }

    protected void assertLdapGroupMember(String accountEntryDn, String groupName) throws LdapException, IOException, CursorException, SchemaException {
        Entry groupEntry = getLdapGroupByName(groupName);
        assertAttributeContains(groupEntry, getLdapGroupMemberAttribute(), accountEntryDn, dnMatchingRule);
    }

    protected void assertLdapNoGroupMember(Entry accountEntry, String groupName) throws LdapException, IOException, CursorException, SchemaException {
        assertLdapNoGroupMember(accountEntry.getDn().toString(), groupName);
    }

    protected void assertLdapNoGroupMember(String accountEntryDn, String groupName) throws LdapException, IOException, CursorException, SchemaException {
        Entry groupEntry = getLdapGroupByName(groupName);
        assertAttributeNotContains(groupEntry, getLdapGroupMemberAttribute(), accountEntryDn, dnMatchingRule);
    }

    protected List<Entry> ldapSearch(LdapNetworkConnection connection, String filter) throws LdapException, CursorException {
        return ldapSearch(null, connection, filter);
    }

    protected List<Entry> ldapSearch(UserLdapConnectionConfig config, LdapNetworkConnection connection, String filter) throws LdapException, CursorException {
        String baseContext = getLdapSuffix();
        if (config != null && config.getBaseContext() != null) {
            baseContext = config.getBaseContext();
        }
        return ldapSearch(connection, baseContext, filter, SearchScope.SUBTREE, "*", "isMemberOf", "memberof", "isMemberOf", getPrimaryIdentifierAttributeName());
    }

    protected List<Entry> ldapSearch(LdapNetworkConnection connection, String baseDn, String filter, SearchScope scope, String... attributes) throws LdapException, CursorException {
        logger.trace("LDAP search base={}, filter={}, scope={}, attributes={}",
                baseDn, filter, scope, attributes);

        SearchRequest searchRequest = new SearchRequestImpl();
        searchRequest.setBase(new Dn(baseDn));
        searchRequest.setFilter(filter);
        searchRequest.setScope(scope);
        searchRequest.addAttributes(attributes);
        searchRequest.ignoreReferrals();

        List<Entry> entries = new ArrayList<>();
        try {
            SearchCursor searchCursor = connection.search(searchRequest);
            while (searchCursor.next()) {
                Response response = searchCursor.get();
                if (response instanceof SearchResultEntry) {
                    Entry entry = ((SearchResultEntry) response).getEntry();
                    entries.add(entry);
                }
            }
            searchCursor.close();
        } catch (IOException e) {
            throw new IllegalStateException("IO Error: " + e.getMessage(), e);
        } catch (CursorLdapReferralException e) {
            throw new IllegalStateException("Got referral to: " + e.getReferralInfo(), e);
        }
        return entries;
    }

    protected void assertLdapPassword(String uid, String password) throws LdapException, IOException, CursorException {
        Entry entry = getLdapAccountByUid(uid);
        assertLdapPassword(entry, password);
    }

    protected void assertLdapPassword(Entry entry, String password) throws LdapException, IOException, CursorException {
        assertLdapPassword(null, entry, password);
    }

    protected void assertLdapPassword(UserLdapConnectionConfig config, Entry entry, String password) throws LdapException, IOException, CursorException {
        LdapNetworkConnection conn = ldapConnect(config, entry.getDn().toString(), password);
        assertTrue("Not connected", conn.isConnected());
        assertTrue("Not authenticated", conn.isAuthenticated());
        // AD sometimes pretends to bind successfuly. Even though success is indicated, the bind in fact fails silently.
        // Therefore try to read my own entry.
        EntryCursor cursor = conn.search(entry.getDn(), "(objectclass=*)", SearchScope.OBJECT, "*");
        int foundEntries = 0;
        while (cursor.next()) {
            Entry entryFound = cursor.get();
            logger.trace("Search-after-auth found: {}", entryFound);
            foundEntries++;
        }
        cursor.close();
        logger.debug("Search-after-auth found {} entries", foundEntries);
        ldapDisconnect(conn);
        if (foundEntries != 1) {
            throw new SecurityException("Cannot read my own entry (" + entry.getDn() + ")");
        }
    }

    protected Entry addLdapAccount(String uid, String cn, String givenName, String sn)
            throws LdapException, IOException {
        Entry entry = createAccountEntry(uid, cn, givenName, sn);
        addLdapEntry(entry);
        return entry;
    }

    protected void addLdapEntry(Entry entry) throws LdapException, IOException {
        LdapNetworkConnection connection = ldapConnect();
        try {
            connection.add(entry);
            display("Added LDAP account:\n" + entry);
        } catch (Exception e) {
            display("Error adding entry:\n" + entry + "\nError: " + e.getMessage());
            ldapDisconnect(connection);
            throw e;
        }
        ldapDisconnect(connection);
    }

    protected Entry createAccountEntry(String uid, String cn, String givenName, String sn) throws LdapException {
        return new DefaultEntry(toAccountDn(uid),
                "objectclass", getLdapAccountObjectClass(),
                "uid", uid,
                "cn", cn,
                "givenName", givenName,
                "sn", sn);
    }

    protected Entry addLdapGroup(String cn, String description, String... memberDns)
            throws LdapException, IOException {
        LdapNetworkConnection connection = ldapConnect();
        Entry entry = createGroupEntry(cn, description, memberDns);
        logger.trace("Adding LDAP entry:\n{}", entry);
        connection.add(entry);
        display("Added LDAP group:" + entry);
        ldapDisconnect(connection);
        return entry;
    }

    protected Entry createGroupEntry(String cn, String description, String... memberDns)
            throws LdapException {
        Entry entry = new DefaultEntry(toGroupDn(cn),
                "objectclass", getLdapGroupObjectClass(),
                "cn", cn,
                "description", description);
        if (isGroupMemberMandatory() && memberDns != null && memberDns.length > 0) {
            entry.add(getLdapGroupMemberAttribute(), memberDns);
        }
        return entry;
    }

    protected void deleteLdapEntry(String dn) throws LdapException, IOException {
        LdapNetworkConnection connection = ldapConnect();
        connection.delete(dn);
        display("Deleted LDAP entry: " + dn);
        ldapDisconnect(connection);
    }

    /**
     * Silent delete. Used to clean up after previous test runs.
     * @return deleted entry or null
     */
    protected Entry cleanupDelete(String dn) throws LdapException, IOException, CursorException {
        return cleanupDelete(null, dn);
    }

    /**
     * Silent delete. Used to clean up after previous test runs.
     * @return deleted entry or null
     */
    protected Entry cleanupDelete(UserLdapConnectionConfig config, String dn) throws LdapException, IOException, CursorException {
        LdapNetworkConnection connection = ldapConnect(config);
        Entry entry = getLdapEntry(connection, dn);
        if (entry != null) {
            connection.delete(dn);
            display("Cleaning up LDAP entry: " + dn);
        }
        ldapDisconnect(connection);
        return entry;
    }

    protected String toAccountDn(String username, String fullName) {
        return toAccountDn(username);
    }

    protected String toAccountDn(String username) {
        return "uid=" + username + "," + getPeopleLdapSuffix();
    }

    protected Rdn toAccountRdn(String username, String fullName) {
        try {
            return new Rdn(new Ava("uid", username));
        } catch (LdapInvalidDnException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected String toGroupDn(String cn) {
        return "cn=" + cn + "," + getGroupsLdapSuffix();
    }

    protected String getAttributeAsString(Entry entry, String primaryIdentifierAttributeName) throws LdapInvalidAttributeValueException {
        if ("dn".equals(primaryIdentifierAttributeName)) {
            return entry.getDn().toString();
        } else {
            return entry.get(primaryIdentifierAttributeName).getString();
        }
    }

    protected LdapNetworkConnection ldapConnect() throws LdapException, IOException {
        return ldapConnect(getLdapBindDn(), getLdapBindPassword());
    }

    protected LdapNetworkConnection ldapConnect(String bindDn, String bindPassword) throws LdapException, IOException {
        UserLdapConnectionConfig config = new UserLdapConnectionConfig();
        config.setLdapHost(getLdapServerHost());
        config.setLdapPort(getLdapServerPort());
        config.setBindDn(bindDn);
        config.setBindPassword(bindPassword);

        return ldapConnect(config);
    }

    protected LdapNetworkConnection ldapConnect(UserLdapConnectionConfig config, String bindDn, String bindPassword) throws LdapException, IOException {
        if (config == null) {
            config = new UserLdapConnectionConfig();
            config.setLdapHost(getLdapServerHost());
            config.setLdapPort(getLdapServerPort());
        }
        config.setBindDn(bindDn);
        config.setBindPassword(bindPassword);

        return ldapConnect(config);
    }

    protected LdapNetworkConnection ldapConnect(UserLdapConnectionConfig config) throws LdapException, IOException {
        if (config == null) {
            config = new UserLdapConnectionConfig();
            config.setLdapHost(getLdapServerHost());
            config.setLdapPort(getLdapServerPort());
            config.setBindDn(getLdapBindDn());
            config.setBindPassword(getLdapBindPassword());
        }
        logger.trace("LDAP connect to {}:{} as {}",
                config.getLdapHost(), config.getLdapPort(), config.getBindDn());

        if (useSsl()) {
            config.setUseSsl(true);
            TrustManager trustManager = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] chain, String authType) {
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
            AssertJUnit.fail("Cannot connect to LDAP server " + config.getLdapHost() + ":" + config.getLdapPort());
        }
        logger.trace("LDAP connected to {}:{}, executing bind as {}",
                config.getLdapHost(), config.getLdapPort(), config.getBindDn());
        BindRequest bindRequest = new BindRequestImpl();
        bindRequest.setDn(new Dn(config.getBindDn()));
        bindRequest.setCredentials(config.getBindPassword());
        bindRequest.setSimple(true);
        BindResponse bindResponse = connection.bind(bindRequest);
        if (bindResponse.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS) {
            ldapDisconnect(connection);
            throw new SecurityException("Bind as " + config.getBindDn() + " failed: " + bindResponse.getLdapResult().getDiagnosticMessage() + " (" + bindResponse.getLdapResult().getResultCode() + ")");
        }
        logger.trace("LDAP connected to {}:{}, bound as {}",
                config.getLdapHost(), config.getLdapPort(), config.getBindDn());
        return connection;
    }

    protected void ldapDisconnect(LdapNetworkConnection connection) throws IOException {
        logger.trace("LDAP disconnect {}", connection);
        connection.close();
    }

    protected void assertAccountShadow(PrismObject<ShadowType> shadow, String dn) throws SchemaException, ConfigurationException {
        assertShadowCommon(shadow, null, dn, resourceType, getAccountObjectClass(), ciMatchingRule, false);
    }

    protected void assertGroupShadow(PrismObject<ShadowType> shadow, String dn) throws SchemaException, ConfigurationException {
        assertShadowCommon(shadow, null, dn, resourceType, getGroupObjectClass(), ciMatchingRule, false, true);
    }

    protected long roundTsDown(long ts) {
        return (ts / 1000 * 1000);
    }

    // TODO: for 10000 it produces 10001? I don't understand it (Virgo)
    protected long roundTsUp(long ts) {
        return (ts / 1000 * 1000) + 1;
    }

    protected void assertStableSystem() throws NumberFormatException, IOException, InterruptedException {
        if (isAssertOpenFiles()) {
            lsof.assertStable();
        }
    }

    protected void assertLdapConnectorReasonableInstances() throws NumberFormatException, IOException, InterruptedException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (isUsingGroupShortcutAttribute()) {
            assertLdapConnectorInstancesRange(1,2);
        } else {
            // Maybe two, maybe higher, adjust as needed
            assertLdapConnectorInstancesRange(1,2);
        }
    }

    protected void assertLdapConnectorInstancesRange(int expectedConnectorInstancesLow, int expectedConnectorInstancesHigh) throws NumberFormatException, IOException, InterruptedException, SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        if (runsInIdea()) {
            // IntelliJ IDEA affects management of connector instances in some way.
            // This makes the number of connector instances different when compared to a test executed from command-line.
            return;
        }
        Task task = createTask(AbstractLdapTest.class.getName() + ".assertLdapConnectorInstances");
        OperationResult result = task.getResult();
        List<ConnectorOperationalStatus> stats = provisioningService.getConnectorOperationalStatus(getResourceOid(), task, result);
        display("Resource connector stats", stats);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        ConnectorOperationalStatus stat = findLdapConnectorStat(stats);
        assertNotNull("No stat for LDAP connector", stat);

        int actualConnectorInstances = stat.getPoolStatusNumIdle() + stat.getPoolStatusNumActive();

        if (actualConnectorInstances < expectedConnectorInstancesLow) {
            fail("Number of LDAP connector instances too low, expected at least "+expectedConnectorInstancesLow+" instances, but was "+actualConnectorInstances);
        }
        if (actualConnectorInstances > expectedConnectorInstancesHigh) {
            fail("Number of LDAP connector instances too high, expected at most "+expectedConnectorInstancesHigh+" instances, but was "+actualConnectorInstances);
        }

        if (!isAssertOpenFiles()) {
            return;
        }
        if (actualConnectorInstances == 1) {
            assertStableSystem();
        } else {
            lsof.assertFdIncrease((actualConnectorInstances - 1) * getNumberOfFdsPerLdapConnectorInstance());
        }
    }

    private ConnectorOperationalStatus findLdapConnectorStat(List<ConnectorOperationalStatus> stats) {
        for (ConnectorOperationalStatus stat : stats) {
            if (stat.getConnectorClassName().equals(getLdapConnectorClassName())) {
                return stat;
            }
        }
        return null;
    }

    protected int getNumberOfFdsPerLdapConnectorInstance() {
        return 7;
    }

}
