/*
 * Copyright (C) 2015-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad.multidomain;

import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.getRootSyncTokenRealValueRequired;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.PATH_CN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_CREDENTIALS_PASSWORD_VALUE;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;

import com.evolveum.midpoint.testing.conntest.AbstractLdapTest;
import com.evolveum.midpoint.testing.conntest.ad.AbstractAdLdapTest;
import com.evolveum.midpoint.testing.conntest.ad.AdTestMixin;

import org.apache.commons.lang3.StringUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.testing.conntest.UserLdapConnectionConfig;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Active Directory multidomain test abstract superclass.
 * This tests configuration of Active Directory forrest with two domains: parent domain and child domain.
 * Whole forrest is configured as a single resource.
 * <p>
 * This is a "live and conservative" conntest.
 * Which means that it runs on very realy and very live Active Directory forrest.
 * The AD servers are NOT cleaned and reset after/before test.
 * The test is carefully built not to destroy the environment.
 * The test cleans up all the objects that it creates.
 * In case that the test fails, there is a special cleanup procedure in test000Sanity.
 *
 * @author Radovan Semancik
 */
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractAdLdapMultidomainTest extends AbstractAdLdapTest
        implements AdTestMixin {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ad-ldap-multidomain");

    protected static final File ROLE_PIRATES_FILE = new File(TEST_DIR, "role-pirate.xml");
    protected static final String ROLE_PIRATES_OID = "5dd034e8-41d2-11e5-a123-001e8c717e5b";

    protected static final File ROLE_SUBMISSIVE_FILE = new File(TEST_DIR, "role-submissive.xml");
    protected static final String ROLE_SUBMISSIVE_OID = "0c0c81b2-d0a1-11e5-b51e-0309a826745e";

    protected static final File ROLE_META_ORG_FILE = new File(TEST_DIR, "role-meta-org.xml");
    protected static final String ROLE_META_ORG_OID = "f2ad0ace-45d7-11e5-af54-001e8c717e5b";

    protected static final File ROLE_META_ORG_GROUP_FILE = new File(TEST_DIR, "role-meta-org-group.xml");
    protected static final String ROLE_META_ORG_GROUP_OID = "c5d3294a-0d8e-11e7-bd9d-ff848c2e7e3f";

    protected static final String ACCOUNT_JACK_SAM_ACCOUNT_NAME = "jack";
    protected static final String ACCOUNT_JACK_FULL_NAME = "Jack Sparrow";
    protected static final String ACCOUNT_JACK_PASSWORD = "qwe.123";

    protected static final String ACCOUNT_HT_UID = "ht";
    protected static final String ACCOUNT_HT_CN = "Herman Toothrot";
    protected static final String ACCOUNT_HT_GIVENNAME = "Herman";
    protected static final String ACCOUNT_HT_SN = "Toothrot";
    protected static final String ACCOUNT_HT_SN_MODIFIED = "Torquemeda Marley";

    protected static final String ACCOUNT_HTM_UID = "htm";
    protected static final String ACCOUNT_HTM_CN = "Horatio Torquemada Marley";

    protected static final String USER_CPTBARBOSSA_FULL_NAME = "Captain Hector Barbossa";

    private static final String GROUP_PIRATES_NAME = "pirates";
    private static final String GROUP_MELEE_ISLAND_NAME = "Mêlée Island";
    private static final String GROUP_MELEE_ISLAND_ALT_NAME = "Alternative Mêlée Island";
    private static final String GROUP_MELEE_ISLAND_PIRATES_NAME = "Mêlée Island Pirates";
    private static final String GROUP_MELEE_ISLAND_PIRATES_DESCRIPTION = "swashbuckle and loot";

    private static final String ASSOCIATION_GROUP_NAME = "group";

    private static final String NS_EXTENSION = "http://whatever.com/my";
    private static final QName EXTENSION_SHOW_IN_ADVANCED_VIEW_ONLY_QNAME = new QName(NS_EXTENSION, "showInAdvancedViewOnly");

    protected static final File USER_SUBMAN_FILE = new File(TEST_DIR, "user-subman.xml");
    private static final String USER_SUBMAN_OID = "910ac45a-8bd6-11e6-9122-ef88d95095f0";
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
    private static final String USER_SUBMARINE_OID = "c4377f86-8be9-11e6-8ef5-c3c56ff64b09";
    private static final String USER_SUBMARINE_USERNAME = "submarine";
    private static final String USER_SUBMARINE_GIVEN_NAME = "Sub";
    private static final String USER_SUBMARINE_FAMILY_NAME = "Marine";
    private static final String USER_SUBMARINE_FULL_NAME = "Sub Marine";

    private static final String INTENT_GROUP = "group";
    private static final String INTENT_OU_TOP = "ou-top";

    private static final String USER_EMPTYHEAD_NAME = "emptyhead";

    private static final String PROXY_ADDRES_ADDR_UPCASE = "smpt:ADDR";
    private static final String PROXY_ADDRES_ADDR_LOWCASE = "smpt:addr";

    private static final String USER_GUYBRUSH_PASSWORD_123 = "wanna.be.a.123";
    private static final String USER_GUYBRUSH_PASSWORD_333 = "wanna.be.a.333";

    private static final String VERY_STRANGE_PARAMETER = "This iš a véry stándže p§räméteř!";

    private static final String SHADOW_GHOST_OID = "0c244f74-0169-11eb-a13f-77a028a1ab97";
//    protected static final File SHADOW_GHOST_FILE = new File(TEST_DIR, "shadow-ghost.xml");

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

    @Override
    protected String getResourceOid() {
        return "eced6d24-73e3-11e5-8457-93eff15a6b85";
    }

    @Override
    protected File getBaseDir() {
        return TEST_DIR;
    }

    @Override
    protected String getSyncTaskOid() {
        return "0f93d8d4-5fb4-11ea-8571-a3f090bf921f";
    }

    @Override
    protected String getLdapBindPassword() {
        return "qwe.123";
    }

    protected abstract String getLdapSubServerHost();

    protected abstract String getLdapSubSuffix();

    protected String getPeopleLdapSubSuffix() {
        return "CN=Users," + getLdapSubSuffix();
    }

    protected String getLdapSubBindDn() {
        return "CN=midpoint," + getPeopleLdapSubSuffix();
    }

    private QName getAssociationGroupQName() {
        return new QName(MidPointConstants.NS_RI, ASSOCIATION_GROUP_NAME);
    }

    @Override
    protected boolean allowDuplicateSearchResults() {
        return allowDuplicateSearchResults;
    }

    protected String getOrgsLdapSuffix() {
        return "OU=Org," + getLdapSuffix();
    }

    protected abstract String getAccountJackSid();

    protected abstract File getShadowGhostFile();

    private UserLdapConnectionConfig getSubLdapConnectionConfig() {
        UserLdapConnectionConfig config = new UserLdapConnectionConfig();
        config.setLdapHost(getLdapSubServerHost());
        config.setLdapPort(getLdapServerPort());
        config.setBindDn(getLdapSubBindDn());
        config.setBindPassword(getLdapBindPassword());
        config.setBaseContext(getLdapSubSuffix());
        return config;
    }

    protected abstract File getReconciliationTaskFile();

    protected abstract String getReconciliationTaskOid();

    protected String getLdapConnectorClassName() {
        return AD_CONNECTOR_TYPE;
    }

    protected abstract int getNumberOfAllAccounts();

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
        repoAddObjectFromFile(ROLE_END_USER_FILE, initResult);
        repoAddObjectFromFile(ROLE_PIRATES_FILE, initResult);
        repoAddObjectFromFile(ROLE_SUBMISSIVE_FILE, initResult);
        repoAddObjectFromFile(ROLE_META_ORG_FILE, initResult);
        repoAddObjectFromFile(ROLE_META_ORG_GROUP_FILE, initResult);

        assignRole(USER_BARBOSSA_OID, ROLE_END_USER_OID);

    }

    @Test
    public void test000Sanity() throws Exception {

        Properties p = System.getProperties();
        Enumeration keys = p.keys();
        while (keys.hasMoreElements()) {
            String key = (String) keys.nextElement();
            String value = (String) p.get(key);
            System.out.println("PROP: " + key + ": " + value);
        }

        assertLdapPassword(ACCOUNT_JACK_SAM_ACCOUNT_NAME, ACCOUNT_JACK_FULL_NAME, ACCOUNT_JACK_PASSWORD);
        cleanupDelete(toAccountDn(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME));
        cleanupDelete(toAccountDn(USER_CPTBARBOSSA_USERNAME, USER_CPTBARBOSSA_FULL_NAME));
        cleanupDelete(toAccountDn(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME));
        cleanupDelete(getSubLdapConnectionConfig(), toAccountSubDn(USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME));
        cleanupDelete(getSubLdapConnectionConfig(), toAccountSubDn(USER_SUBDOG_USERNAME, USER_SUBDOG_FULL_NAME));
        cleanupDelete(getSubLdapConnectionConfig(), toAccountSubDn(USER_SUBMARINE_USERNAME, USER_SUBMARINE_FULL_NAME));
        cleanupDelete(toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN));
        cleanupDelete(toAccountDn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN));
        cleanupDelete(toGroupDn(GROUP_MELEE_ISLAND_NAME));
        cleanupDelete(toGroupDn(GROUP_MELEE_ISLAND_ALT_NAME));
        cleanupDelete(toOrgGroupDn(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME));
        cleanupDelete(toOrgGroupDn(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_ALT_NAME));
        cleanupDelete(toOrgDn(GROUP_MELEE_ISLAND_NAME));
        cleanupDelete("ou=underMelee," + toOrgDn(GROUP_MELEE_ISLAND_ALT_NAME));
        cleanupDelete(toOrgDn(GROUP_MELEE_ISLAND_ALT_NAME));
    }

    @Test
    @Override
    public void test020Schema() throws Exception {
        accountDefinition = assertAdResourceSchema(resource, getAccountObjectClass());
        assertAdRefinedSchema(resource, getAccountObjectClass());
        if (hasExchange()) {
            assertExchangeSchema(resource, getAccountObjectClass());
        }

        var resourceSchema = ResourceSchemaFactory.getNativeSchemaRequired(resource);

        int expectedDefinitions = 4;
        if (hasExchange()) {
            expectedDefinitions = 5;
        }
        assertEquals("Unexpected number of definitions in schema (limited by generation constraints)",
                expectedDefinitions, resourceSchema.size());

        // Not checking the number of connector instances: the previous operation might have been "test partial configuration"
        // that leaves no cached connector instances.
    }

    @Test
    public void test100SearchJackBySamAccountName() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = createSamAccountNameQuery(ACCOUNT_JACK_SAM_ACCOUNT_NAME);
        display("SamAccountName query:\n" + query.debugDump());

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows =
                modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_JACK_SAM_ACCOUNT_NAME, ACCOUNT_JACK_FULL_NAME));
        assertSid(shadow, getAccountJackSid());
        assertObjectCategory(shadow, getObjectCategoryPerson());
        jackAccountOid = shadow.getOid();

//        assertConnectorOperationIncrement(2);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * MID-3730
     */
    @Test
    public void test101SearchJackByDn() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String jackDn = toAccountDn(ACCOUNT_JACK_SAM_ACCOUNT_NAME, ACCOUNT_JACK_FULL_NAME);
        ObjectQuery query = createAccountShadowQueryByAttribute("dn", jackDn, resource);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, jackDn);
        assertSid(shadow, getAccountJackSid());
        assertObjectCategory(shadow, getObjectCategoryPerson());

//        assertConnectorOperationIncrement(2);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Search for non-existent DN should return no results. It should NOT
     * throw an error.
     * <p>
     * MID-3730
     */
    @Test
    public void test102SearchNotExistByDn() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String dn = toAccountDn("idonoexist", "I am a Fiction");
        ObjectQuery query = createAccountShadowQueryByAttribute("dn", dn, resource);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertEquals("Unexpected search result: " + shadows, 0, shadows.size());

//        assertConnectorOperationIncrement(2);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test105SearchPiratesByCn() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getGroupObjectClass());
        ObjectQueryUtil.filterAnd(query.getFilter(), createAttributeFilter("cn", GROUP_PIRATES_NAME));

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        groupPiratesOid = shadow.getOid();
        assertObjectCategory(shadow, getObjectCategoryGroup());

//        assertConnectorOperationIncrement(1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test110GetJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
        long startOfTestMsTimestamp = getWin32Filetime(System.currentTimeMillis());

        // WHEN
        when();
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, jackAccountOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Shadow", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_JACK_SAM_ACCOUNT_NAME, ACCOUNT_JACK_FULL_NAME));
        jackAccountOid = shadow.getOid();

        IntegrationTestTools.assertAssociationObjectRef(shadow, getAssociationGroupQName(), groupPiratesOid);

        assertAttribute(shadow, "dn", "CN=Jack Sparrow," + getPeopleLdapSuffix());
        assertAttribute(shadow, "cn", ACCOUNT_JACK_FULL_NAME);
        assertAttribute(shadow, "sn", "Sparrow");
        assertAttribute(shadow, "description", "The best pirate the world has ever seen");
        assertAttribute(shadow, "sAMAccountName", ACCOUNT_JACK_SAM_ACCOUNT_NAME);
        assertObjectCategory(shadow, getObjectCategoryPerson());
        List<Long> lastLogonValues = ShadowUtil.getAttributeValues(shadow, new QName(getResourceNamespace(), "lastLogon"));
        assertEquals("Wrong number of lastLong values: " + lastLogonValues, 1, lastLogonValues.size());
        if (lastLogonValues.get(0) > startOfTestMsTimestamp) {
            fail("Wrong lastLogon, expected it to be less than " + startOfTestMsTimestamp + ", but was " + lastLogonValues.get(0));
        }

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * No paging. It should return all accounts.
     */
    @Test
    public void test150SearchAllAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query,
                getNumberOfAllAccounts(), task, result);

        if (isVagueTest()) {
            rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        } else {
            // This seems to vary quite wildly from system to system. Maybe TODO investigate the reasons later?
            assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 8,10);
        }
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * This is in one block.
     */
    @Test
    public void test152SearchFirst2Accounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging();
        paging.setMaxSize(2);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 2, task, result);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test154SearchFirst5Accounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging();
        paging.setMaxSize(5);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 5, task, result);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test162SearchFirst2AccountsOffset0() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(getResourceOid(), ShadowKindType.ACCOUNT, "default");

        ObjectPaging paging = prismContext.queryFactory().createPaging();
        paging.setOffset(0);
        paging.setMaxSize(2);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 2, task, result);

        // increment = 2 because there are two searches:
        // 1. one for baseContext (as defined for account/default)
        // 2. one for objects
        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 2);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * There is offset, so VLV should be used.
     * No explicit sorting.
     */
    @Test
    public void test172Search2AccountsOffset1() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging(1, 2);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 2, task, result);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * There is offset, so VLV should be used.
     * No explicit sorting.
     */
    @Test
    public void test174SearchFirst5AccountsOffset2() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging(2, 5);
        query.setPaging(paging);

        allowDuplicateSearchResults = true;

        // WHEN
        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 5, task, result);

        // THEN
        allowDuplicateSearchResults = false;

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * There is offset, so VLV should be used.
     * Explicit sorting.
     */
    @Test
    public void test182Search2AccountsOffset1SortCn() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging(1, 2);
        paging.setOrdering(PATH_CN, OrderDirection.ASCENDING);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> shadows = doSearch(query, 2, task, result);

        assertAccountShadow(shadows.get(0), getExpected182FirstShadow());
//        assertAccountShadow(shadows.get(0), "CN=Adolf Supperior,CN=Users,DC=ad,DC=evolveum,DC=com");
//        assertAccountShadow(shadows.get(1), "CN=DiscoverySearchMailbox {D919BA05-46A6-415f-80AD-7E09334BB852},CN=Users,DC=ad,DC=evolveum,DC=com");

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    protected String getExpected182FirstShadow() {
        return "CN=Admin," + getPeopleLdapSuffix();
    }

    /**
     * Try to get account that does not exist.
     * The goal is to check that our error handling is at least a bit sane.
     * We need to create a fake shadow for this, otherwise midPoint won't try the AD operation at all.
     */
    @Test
    public void test190GetGhost() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        importObjectFromFile(getShadowGhostFile());

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);
        long startOfTestMsTimestamp = getWin32Filetime(System.currentTimeMillis());

        // WHEN
        when();
        modelService.getObject(ShadowType.class, SHADOW_GHOST_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertRepoShadow(SHADOW_GHOST_OID)
                .assertDead();

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test200AssignAccountBarbossa() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        assignAccountToUser(USER_BARBOSSA_OID, getResourceOid(), null, task, result);

        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", null);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountBarbossaOid = shadow.getOid();
        Collection<ShadowSimpleAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        String accountBarbossaIcfUid = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in " + shadow, accountBarbossaIcfUid);

        assertEquals("Wrong ICFS UID",
                formatGuidToDashedNotation(MiscUtil.bytesToHex(entry.get(getPrimaryIdentifierAttributeName()).getBytes())),
                accountBarbossaIcfUid);

        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD);

        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        assertAttribute(entry, ATTRIBUTE_OBJECT_CATEGORY_NAME, getObjectCategoryPerson());

        // MID-4624
        ShadowSimpleAttribute<XMLGregorianCalendar> createTimestampAttribute = ShadowUtil.getSimpleAttribute(shadow, new QName(MidPointConstants.NS_RI, "createTimeStamp"));
        assertNotNull("No createTimestamp in " + shadow, createTimestampAttribute);
        XMLGregorianCalendar createTimestamp = createTimestampAttribute.getRealValue();
        long createTimestampMillis = XmlTypeConverter.toMillis(createTimestamp);
        // LDAP server may be on a different host. Allow for some clock offset.
        TestUtil.assertBetween("Wrong createTimestamp in " + shadow, roundTsDown(tsStart) - 120000, roundTsUp(tsEnd) + 120000, createTimestampMillis);

        assertObjectCategory(shadow, getObjectCategoryPerson());

//        assertLdapConnectorInstances(2);
    }

    @Test
    public void test210ModifyAccountBarbossaTitle() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, ATTRIBUTE_TITLE_NAME);
        ShadowSimpleAttributeDefinition<?> attrDef = accountDefinition.findSimpleAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "Captain");
        delta.addModification(attrDelta);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, ATTRIBUTE_TITLE_NAME, "Captain");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        assertAttribute(entry, ATTRIBUTE_OBJECT_CATEGORY_NAME, getObjectCategoryPerson());
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

//        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test212ModifyAccountBarbossaShowInAdvancedViewOnlyTrue() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, "showInAdvancedViewOnly");
        ShadowSimpleAttributeDefinition<?> attrDef = accountDefinition.findSimpleAttributeDefinition(attrQName);
        PropertyDelta<Boolean> attrDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, Boolean.TRUE);
        delta.addModification(attrDelta);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "showInAdvancedViewOnly", "TRUE");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        assertAttribute(entry, ATTRIBUTE_OBJECT_CATEGORY_NAME, getObjectCategoryPerson());
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Modify USER, test boolean value mapping.
     */
    @Test
    public void test213ModifyUserBarbossaShowInAdvancedViewOnlyFalse() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, "showInAdvancedViewOnly");
        ShadowSimpleAttributeDefinition<?> attrDef = accountDefinition.findSimpleAttributeDefinition(attrQName);
        PropertyDelta<Boolean> attrDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, Boolean.TRUE);
        delta.addModification(attrDelta);

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID, ItemPath.create(UserType.F_EXTENSION, EXTENSION_SHOW_IN_ADVANCED_VIEW_ONLY_QNAME),
                task, result, Boolean.FALSE);

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "showInAdvancedViewOnly", "FALSE");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        assertAttribute(entry, ATTRIBUTE_OBJECT_CATEGORY_NAME, getObjectCategoryPerson());
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Just normal modification of proxyAddress, directly on the account.
     * As proxyAddress is multivalue, this is ADD and not REPLACE. This is what GUI would do.
     * No previous value for proxyAddress is set in the AD account.
     * MID-5330
     */
    @Test
    public void test214ModifyAccountBarbossaProxyAddressesSimple() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, ATTRIBUTE_PROXY_ADDRESSES_NAME);
        ShadowSimpleAttributeDefinition<?> attrDef = accountDefinition.findSimpleAttributeDefinition(attrQName);
        assertNotNull("No definition for attribute " + attrQName, attrDef);
        PropertyDelta<String> attrDelta = prismContext.deltaFactory().property().createModificationAddProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, PROXY_ADDRES_ADDR_UPCASE);
        delta.addModification(attrDelta);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, ATTRIBUTE_PROXY_ADDRESSES_NAME, PROXY_ADDRES_ADDR_UPCASE);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        assertAttribute(entry, ATTRIBUTE_OBJECT_CATEGORY_NAME, getObjectCategoryPerson());
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

//        assertLdapConnectorInstances(2);
    }

    /**
     * MID-4385
     */
    @Test
    public void test216ModifyAccountBarbossaUserParameters() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, ATTRIBUTE_USER_PARAMETERS_NAME);
        ShadowSimpleAttributeDefinition<?> attrDef = accountDefinition.findSimpleAttributeDefinition(attrQName);
        assertNotNull("No definition for attribute " + attrQName, attrDef);
        PropertyDelta<String> attrDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, VERY_STRANGE_PARAMETER);
        delta.addModification(attrDelta);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, ATTRIBUTE_USER_PARAMETERS_NAME, VERY_STRANGE_PARAMETER);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        assertAttribute(entry, ATTRIBUTE_OBJECT_CATEGORY_NAME, getObjectCategoryPerson());
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertModelShadow(shadowOid)
                .attributes()
                .simpleAttribute(ATTRIBUTE_USER_PARAMETERS_NAME)
                .assertRealValues(VERY_STRANGE_PARAMETER);

//        assertLdapConnectorInstances(2);
    }

    @Test
    public void test218ModifyAccountBarbossaTelephoneNumberGood() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, ATTRIBUTE_TELEPHONE_NUMBER);
        ShadowSimpleAttributeDefinition<?> attrDef = accountDefinition.findSimpleAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "+421901123456");
        delta.addModification(attrDelta);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, ATTRIBUTE_TELEPHONE_NUMBER, "+421901123456");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        assertAttribute(entry, ATTRIBUTE_OBJECT_CATEGORY_NAME, getObjectCategoryPerson());
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

//        assertLdapConnectorReasonableInstances();
    }

    /**
     * We are trying to set a telephone number that is too long (more than 64 chars).
     * AD will not accept that.
     * Make sure the operation fails in a graceful way (e.g. no endless retry loops).
     */
    @Test
    public void test219ModifyAccountBarbossaTelephoneNumberLong() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, ATTRIBUTE_TELEPHONE_NUMBER);
        ShadowSimpleAttributeDefinition<?> attrDef = accountDefinition.findSimpleAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "+4219011234567890123456789012345678901234567890123456789012345678901234567890");
        delta.addModification(attrDelta);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertPartialError(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        // We expect unchanged telephone number here
        assertAttribute(entry, ATTRIBUTE_TELEPHONE_NUMBER, "+421901123456");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        assertAttribute(entry, ATTRIBUTE_OBJECT_CATEGORY_NAME, getObjectCategoryPerson());
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

//        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test220ModifyUserBarbossaPasswordSelfServicePassword1() throws Exception {
        testModifyUserBarbossaPasswordSelfServiceSuccess(
                USER_BARBOSSA_PASSWORD, USER_BARBOSSA_PASSWORD_AD_1);
    }

    /**
     * Try to set the same password again. If this is "admin mode" (no runAs capability)
     * the such change should be successful. In "selfservice mode" (runAs capability - in subclass)
     * this change should fail.
     */
    @Test
    public void test222ModifyUserBarbossaPasswordSelfServicePassword1Again() throws Exception {
        testModifyUserBarbossaPasswordSelfServiceSuccess(
                USER_BARBOSSA_PASSWORD_AD_1, USER_BARBOSSA_PASSWORD_AD_1);
    }

    /**
     * Change to different password. This should go well for both admin and self-service.
     * MID-5242
     */
    @Test
    public void test224ModifyUserBarbossaPasswordSelfServicePassword2() throws Exception {
        testModifyUserBarbossaPasswordSelfServiceSuccess(
                USER_BARBOSSA_PASSWORD_AD_1, USER_BARBOSSA_PASSWORD_AD_2);
    }

    /**
     * Change password back to the first password. This password was used before.
     * In admin mode this should go well. Admin can set password to anything.
     * But in self-service mode (in subclass) this should fail due to password history check.
     */
    @Test
    public void test226ModifyUserBarbossaPasswordSelfServicePassword1AgainAgain() throws Exception {
        testModifyUserBarbossaPasswordSelfServiceSuccess(
                USER_BARBOSSA_PASSWORD_AD_2, USER_BARBOSSA_PASSWORD_AD_1);
    }

    protected void testModifyUserBarbossaPasswordSelfServiceSuccess(String oldPassword, String newPassword) throws Exception {
        // GIVEN
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, oldPassword);

        login(USER_BARBOSSA_USERNAME);

        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_SELF_SERVICE_URI);
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createOldNewPasswordDelta(USER_BARBOSSA_OID,
                oldPassword, newPassword);

        // WHEN
        when();
        executeChanges(objectDelta, null, task, result);

        // THEN
        then();
        login(USER_ADMINISTRATOR_USERNAME);
        assertSuccess(result);

        assertBarbossaEnabled(newPassword);

        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, newPassword);

        assertLdapConnectorReasonableInstances();
    }

    protected void testModifyUserBarbossaPasswordSelfServiceFailure(
            String oldPassword) throws Exception {
        // GIVEN
        login(USER_BARBOSSA_USERNAME);

        Task task = getTestTask();
        task.setChannel(SchemaConstants.CHANNEL_SELF_SERVICE_URI);
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createOldNewPasswordDelta(USER_BARBOSSA_OID,
                oldPassword, AbstractLdapTest.USER_BARBOSSA_PASSWORD_AD_1);

        // WHEN
        when();
        executeChanges(objectDelta, null, task, result);

        // THEN
        then();
        login(USER_ADMINISTRATOR_USERNAME);
        assertPartialError(result);

        assertBarbossaEnabled(AbstractLdapTest.USER_BARBOSSA_PASSWORD_AD_1);
        assertUserAfter(USER_BARBOSSA_OID)
                .assertPassword(AbstractLdapTest.USER_BARBOSSA_PASSWORD_AD_1);

        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, oldPassword);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test230DisableUserBarbossa() throws Exception {
        // precondition
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, USER_BARBOSSA_PASSWORD_AD_1);

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID,
                SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                task, result, ActivationStatusType.DISABLED);

        // THEN
        then();
        assertSuccess(result);

        assertBarbossaDisabled();
    }

    /**
     * MID-4041
     */
    @Test
    public void test232ReconcileBarbossa() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reconcileUser(USER_BARBOSSA_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertBarbossaDisabled();
    }

    /**
     * MID-4041
     */
    @Test
    public void test236EnableUserBarbossa() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID,
                SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                task, result, ActivationStatusType.ENABLED);

        // THEN
        then();
        assertSuccess(result);

        assertBarbossaEnabled(USER_BARBOSSA_PASSWORD_AD_1);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * MID-4041
     */
    @Test
    public void test237ReconcileBarbossa() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        reconcileUser(USER_BARBOSSA_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertBarbossaEnabled(USER_BARBOSSA_PASSWORD_AD_1);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * MID-4041
     */
    @Test
    public void test238DisableUserBarbossaRawAndReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_BARBOSSA_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                executeOptions().raw(), task, result, ActivationStatusType.DISABLED);

        // WHEN
        when();
        reconcileUser(USER_BARBOSSA_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertBarbossaDisabled();

        assertLdapConnectorReasonableInstances();
    }

    /**
     * MID-4041
     */
    @Test
    public void test239EnableUserBarbossaRawAndReconcile() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_BARBOSSA_OID, SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                executeOptions().raw(), task, result, ActivationStatusType.ENABLED);

        // WHEN
        when();
        reconcileUser(USER_BARBOSSA_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertBarbossaEnabled(USER_BARBOSSA_PASSWORD_AD_1);

        assertLdapConnectorReasonableInstances();
    }

    protected PrismObject<UserType> assertBarbossaEnabled(String ldapPassword) throws Exception {
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertAdministrativeStatus(user, ActivationStatusType.ENABLED);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
        if (hasExchange()) {
            assertAttribute(entry, ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME, "FALSE");
        }

        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAccountEnabled(shadow);

        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME, ldapPassword);

        return user;
    }

    private void assertBarbossaDisabled() throws Exception {
//        assertLdapConnectorInstances(2);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertAdministrativeStatus(user, ActivationStatusType.DISABLED);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        displayValue("disabled Barbossa entry", entry);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "514");

        if (hasExchange()) {
            assertAttribute(entry, ATTRIBUTE_MS_EXCH_HIDE_FROM_ADDRESS_LISTS_NAME, "TRUE");
        }

        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAccountDisabled(shadow);

        try {
            assertLdapPassword(null, entry, AbstractLdapTest.USER_BARBOSSA_PASSWORD_AD_1);
            AssertJUnit.fail("Password authentication works, but it should fail");
        } catch (SecurityException e) {
            // this is expected
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * This should create account with a group. And the account should be disabled.
     */
    @Test
    public void test250AssignGuybrushPirates() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

//        ProtectedStringType userPasswordPs = new ProtectedStringType();
//        userPasswordPs.setClearValue(USER_GUYBRUSH_PASSWORD_333);
//        modifyUserReplace(USER_GUYBRUSH_OID, PATH_CREDENTIALS_PASSWORD_VALUE, task, result, userPasswordPs);
        modifyUserReplace(USER_GUYBRUSH_OID,
                SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                task, result, ActivationStatusType.DISABLED);

        // WHEN
        when();
        assignRole(USER_GUYBRUSH_OID, ROLE_PIRATES_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        displayValue("Entry", entry);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "514");

        assertLdapGroupMember(entry, GROUP_PIRATES_NAME);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        assertAdministrativeStatus(user, ActivationStatusType.DISABLED);
        String shadowOid = getSingleLinkOid(user);

        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        IntegrationTestTools.assertAssociationObjectRef(shadow, getAssociationGroupQName(), groupPiratesOid);
        assertAccountDisabled(shadow);

//        try {
//            assertLdapPassword(null, entry, USER_GUYBRUSH_PASSWORD_333);
//            AssertJUnit.fail("Password authentication works, but it should fail");
//        } catch (SecurityException e) {
//            // this is expected, account is disabled
//        }

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test255ModifyUserGuybrushPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(USER_GUYBRUSH_PASSWORD_123);

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, PATH_CREDENTIALS_PASSWORD_VALUE, task, result, userPasswordPs);

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        displayValue("Guybrush entry after", entry);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "514");

        try {
            assertLdapPassword(null, entry, USER_GUYBRUSH_PASSWORD_123);
            AssertJUnit.fail("Password authentication works, but it should fail");
        } catch (SecurityException e) {
            // this is expected, account is disabled
        }

//        assertLdapConnectorInstances(2);
    }

    @Test
    public void test260EnableGyubrush() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID,
                SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                task, result, ActivationStatusType.ENABLED);

        // THEN
        then();
        assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        assertAdministrativeStatus(user, ActivationStatusType.ENABLED);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");

        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAccountEnabled(shadow);

        assertLdapPassword(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME, "wanna.be.a.123");

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Try to create an account without password. This should end up with an error.
     * A reasonable error.
     * MID-4046
     */
    @Test
    public void test270AssignAccountToEmptyhead() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_EMPTYHEAD_NAME, USER_EMPTYHEAD_NAME, true);
        display("User before", userBefore);
        addObject(userBefore);
        String userEmptyheadOid = userBefore.getOid();

        // WHEN
        when();
        assignAccountToUser(userEmptyheadOid, getResourceOid(), null, task, result);

        // THEN
        then();
        assertPartialError(result);

        assertMessageContains(result.getMessage(), "does not meet the length, complexity, or history requirement");

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Just make random test connection between the tests. Make sure that the
     * test does not break anything. If it does the next tests will simply fail.
     */
    @Test
    public void test295TestConnection() throws Exception {
        // GIVEN
        Task task = getTestTask();

        // WHEN
        when();
        OperationResult testResult = modelService.testResource(getResourceOid(), task, task.getResult());

        // THEN
        then();
        display("Test connection result", testResult);
        TestUtil.assertSuccess("Test connection result", testResult);

        // Test is disposing connector facade and the entire connector pool.
        // Therefore we are back at 1 instance.
        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test300AssignBarbossaPirates() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_BARBOSSA_OID, ROLE_PIRATES_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        displayValue("Entry", entry);
        assertAttribute(entry, "title", "Captain");

        assertLdapGroupMember(entry, GROUP_PIRATES_NAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        IntegrationTestTools.assertAssociationObjectRef(shadow, getAssociationGroupQName(), groupPiratesOid);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test390ModifyUserBarbossaRename() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_BARBOSSA_OID, UserType.F_NAME,
                PrismTestUtil.createPolyString(USER_CPTBARBOSSA_USERNAME));
        objectDelta.addModificationReplaceProperty(UserType.F_FULL_NAME,
                PrismTestUtil.createPolyString(USER_CPTBARBOSSA_FULL_NAME));
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);

        // WHEN
        when();
        modelService.executeChanges(deltas, null, task, result);

        // THEN
        then();
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
        // TODO: do this on another account. There is a bad interference with rename.

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(USER_BARBOSSA_OID, ROLE_PIRATES_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_CPTBARBOSSA_FULL_NAME);
        displayValue("Entry", entry);
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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignAccountFromUser(USER_BARBOSSA_OID, getResourceOid(), null, task, result);

        // THEN
        then();
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
        // GIVEN
        Task task = getTestTask();
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
        when();
        addObject(org, task, result);

        // THEN
        then();
        assertSuccess(result);

        orgMeleeIslandOid = org.getOid();
        assertLdapGroup(GROUP_MELEE_ISLAND_NAME);
        assertLdapOrg(GROUP_MELEE_ISLAND_NAME);

        org = getObject(OrgType.class, orgMeleeIslandOid);
        groupMeleeIslandOid = getLinkRefOid(org, getResourceOid(), ShadowKindType.ENTITLEMENT, INTENT_GROUP);
        ouMeleeIslandOid = getLinkRefOid(org, getResourceOid(), ShadowKindType.GENERIC, INTENT_OU_TOP);
        assertLiveLinks(org, 2);

        PrismObject<ShadowType> shadowGroup = getShadowModel(groupMeleeIslandOid);
        display("Shadow: group (model)", shadowGroup);

        PrismObject<ShadowType> shadowOu = getShadowModel(ouMeleeIslandOid);
        display("Shadow: ou (model)", shadowOu);

//        assertLdapConnectorInstances(2);
    }

    @Test
    public void test510AssignGuybrushMeleeIsland() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignOrg(USER_GUYBRUSH_OID, orgMeleeIslandOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);

        assertLdapGroupMember(entry, GROUP_MELEE_ISLAND_NAME);

        IntegrationTestTools.assertAssociationObjectRef(shadow, getAssociationGroupQName(), groupMeleeIslandOid);

//        assertLdapConnectorInstances(2);
    }

    /**
     * Create role under the Melee Island org. This creates group in the orgstruct.
     */
    @Test
    public void test515AddOrgGroupMeleeIslandPirates() throws Exception {
        // GIVEN
        Task task = getTestTask();
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
        when();
        addObject(role, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        roleMeleeIslandPiratesOid = role.getOid();
        // TODO: assert LDAP object

        assertLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME);

        PrismObject<RoleType> roleAfter = getObject(RoleType.class, roleMeleeIslandPiratesOid);
        display("Role after", roleAfter);
        groupMeleeIslandPiratesOid = getSingleLinkOid(roleAfter);
        PrismObject<ShadowType> shadow = getShadowModel(groupMeleeIslandPiratesOid);
        display("Shadow (model)", shadow);
    }

    /**
     * Rename org unit. MidPoint should rename OU and ordinary group.
     * AD will rename the group in the orgstruct automatically. We need to
     * make sure that we can still access that group.
     */
    @Test
    public void test520RenameMeleeIsland() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        renameObject(OrgType.class, orgMeleeIslandOid, GROUP_MELEE_ISLAND_ALT_NAME, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<OrgType> orgAfter = getObject(OrgType.class, orgMeleeIslandOid);
        groupMeleeIslandOid = getLinkRefOid(orgAfter, getResourceOid(), ShadowKindType.ENTITLEMENT, INTENT_GROUP);
        ouMeleeIslandOid = getLinkRefOid(orgAfter, getResourceOid(), ShadowKindType.GENERIC, INTENT_OU_TOP);
        assertLiveLinks(orgAfter, 2);

        PrismObject<ShadowType> shadowGroup = getShadowModel(groupMeleeIslandOid);
        display("Shadow: group (model)", shadowGroup);

        PrismObject<ShadowType> shadowOu = getShadowModel(ouMeleeIslandOid);
        display("Shadow: ou (model)", shadowOu);

        assertLdapGroup(GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapGroup(GROUP_MELEE_ISLAND_NAME);

        assertLdapOrg(GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapOrg(GROUP_MELEE_ISLAND_NAME);

        assertLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME);

        Entry entryGuybrush = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        String shadowAccountOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadowAccount = getShadowModel(shadowAccountOid);
        display("Shadow: account (model)", shadowAccount);

        assertLdapGroupMember(entryGuybrush, GROUP_MELEE_ISLAND_ALT_NAME);

        IntegrationTestTools.assertAssociationObjectRef(shadowAccount, getAssociationGroupQName(), groupMeleeIslandOid);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyObjectReplaceProperty(ShadowType.class, groupMeleeIslandPiratesOid,
                ItemPath.create(ShadowType.F_ATTRIBUTES, new QName(MidPointConstants.NS_RI, "description")),
                task, result, GROUP_MELEE_ISLAND_PIRATES_DESCRIPTION);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entryOrgGroup = assertLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_ALT_NAME);
        assertAttribute(entryOrgGroup, "description", GROUP_MELEE_ISLAND_PIRATES_DESCRIPTION);

        assertNoLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME);

//        assertLdapConnectorInstances(2);
    }

    @Test
    public void test524GetMeleeIslandPirates() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, groupMeleeIslandPiratesOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        display("Shadow after", shadow);
        assertNotNull(shadow);

        assertLdapGroup(GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapGroup(GROUP_MELEE_ISLAND_NAME);
        assertLdapOrg(GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapOrg(GROUP_MELEE_ISLAND_NAME);
        Entry entryOrgGroup = assertLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_ALT_NAME);
        displayValue("Melee org", entryOrgGroup);
        assertNoLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME);
    }

    @Test
    public void test595DeleteOrgGroupMeleeIslandPirates() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        deleteObject(RoleType.class, roleMeleeIslandPiratesOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNoLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapOrgGroup(GROUP_MELEE_ISLAND_PIRATES_NAME, GROUP_MELEE_ISLAND_NAME);

        assertNoObject(ShadowType.class, groupMeleeIslandPiratesOid);

//        assertLdapConnectorInstances(2);
    }

    /**
     * We create "underMelee" org that gets into the way of the delete.
     * Melee cannot be deleted in an ordinary way. "tree delete" control must
     * be used. This is configured in the connector config.
     * <p>
     * MID-5935
     */
    @Test
    public void test599DeleteOrgMeleeIsland() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        createUnderMeleeEntry();

        // WHEN
        when();
        deleteObject(OrgType.class, orgMeleeIslandOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNoLdapGroup(GROUP_MELEE_ISLAND_NAME);
        assertNoLdapGroup(GROUP_MELEE_ISLAND_ALT_NAME);
        assertNoLdapOrg(GROUP_MELEE_ISLAND_NAME);
        assertNoLdapOrg(GROUP_MELEE_ISLAND_ALT_NAME);

        assertNoObject(ShadowType.class, groupMeleeIslandOid);
        assertNoObject(ShadowType.class, ouMeleeIslandOid);

//        assertLdapConnectorInstances(2);
    }

    protected void createUnderMeleeEntry() throws LdapException, IOException {
        // This OU just gets into the way of the delete.
        Entry entry = new DefaultEntry("ou=underMelee," + toOrgDn(GROUP_MELEE_ISLAND_ALT_NAME),
                "objectclass", "organizationalUnit",
                "ou", "underMelee");
        displayValue("underMelee org", entry);
        addLdapEntry(entry);
    }

    @Test
    public void test600AssignAccountSubman() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        assignRole(USER_SUBMAN_OID, ROLE_SUBMISSIVE_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        Entry entry = assertLdapSubAccount(USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME);
        displayValue("Sub entry", entry);
        assertAttribute(entry, "title", null);

        PrismObject<UserType> userAfter = getUser(USER_SUBMAN_OID);
        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountSubmanOid = shadow.getOid();
        Collection<ShadowSimpleAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        assertNotNull("Unexpected situation, no identifiers found", identifiers);
        String accountBarbossaIcfUid = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in " + shadow, accountBarbossaIcfUid);

        assertEquals("Wrong ICFS UID",
                formatGuidToDashedNotation(MiscUtil.bytesToHex(entry.get(getPrimaryIdentifierAttributeName()).getBytes())),
                accountBarbossaIcfUid);

        assertLdapPassword(getSubLdapConnectionConfig(), USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME, USER_SUBMAN_PASSWORD);

        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");

        // MID-4624
        ShadowSimpleAttribute<XMLGregorianCalendar> createTimestampAttribute = ShadowUtil.getSimpleAttribute(shadow, new QName(MidPointConstants.NS_RI, "createTimeStamp"));
        assertNotNull("No createTimestamp in " + shadow, createTimestampAttribute);
        XMLGregorianCalendar createTimestamp = createTimestampAttribute.getRealValue();
        long createTimestampMillis = XmlTypeConverter.toMillis(createTimestamp);
        // LDAP server may be on a different host. Allow for some clock offset.
        TestUtil.assertBetween("Wrong createTimestamp in " + shadow, roundTsDown(tsStart) - 120000, roundTsUp(tsEnd) + 120000, createTimestampMillis);

//        assertLdapConnectorInstances(2);
    }

    @Test
    public void test610ModifyUserSubmanTitle() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_SUBMAN_OID, UserType.F_TITLE, task, result,
                PolyString.fromOrig("Underdog"));

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertLdapSubAccount(USER_SUBMAN_USERNAME, USER_SUBMAN_FULL_NAME);
        displayValue("Sub entry", entry);
        assertAttribute(entry, "title", "Underdog");
        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");

        PrismObject<UserType> user = getUser(USER_SUBMAN_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountSubmanOid, shadowOid);

//        assertLdapConnectorInstances(2);
    }

    @Test
    public void test620ModifyUserSubmanPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue("SuB.321");

        // WHEN
        when();
        modifyUserReplace(USER_SUBMAN_OID, PATH_CREDENTIALS_PASSWORD_VALUE, task, result, userPasswordPs);

        // THEN
        then();
        assertSuccess(result);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_SUBMAN_OID,
                SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                task, result, ActivationStatusType.DISABLED);

        // THEN
        then();
        assertSuccess(result);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_SUBMAN_OID,
                SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS,
                task, result, ActivationStatusType.ENABLED);

        // THEN
        then();
        assertSuccess(result);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<UserType> objectDelta = createModifyUserReplaceDelta(USER_SUBMAN_OID, UserType.F_NAME,
                PolyString.fromOrig(USER_SUBDOG_USERNAME));
        objectDelta.addModificationReplaceProperty(UserType.F_FULL_NAME,
                PolyString.fromOrig(USER_SUBDOG_FULL_NAME));

        // WHEN
        when();
        executeChanges(objectDelta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(USER_SUBMAN_OID, ROLE_SUBMISSIVE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

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
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_SUBMARINE_OID, ROLE_SUBMISSIVE_OID, task, result);

        modifyUserReplace(USER_SUBMARINE_OID, UserType.F_TITLE, task, result,
                PrismTestUtil.createPolyString("Underseadog"));

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertLdapSubAccount(USER_SUBMARINE_USERNAME, USER_SUBMARINE_FULL_NAME);
        displayValue("Sub entry", entry);
        assertAttribute(entry, "title", "Underseadog");

        PrismObject<UserType> userAfter = getUser(USER_SUBMARINE_OID);
        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        assertThat(shadow.getOid()).isNotBlank();
        Collection<ShadowSimpleAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        String accountIcfUid = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in " + shadow, accountIcfUid);

        assertEquals("Wrong ICFS UID",
                formatGuidToDashedNotation(MiscUtil.bytesToHex(entry.get(getPrimaryIdentifierAttributeName()).getBytes())),
                accountIcfUid);

        assertAttribute(entry, ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512");
    }

    @Test
    public void test809UnAssignAccountSubmarine() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(USER_SUBMARINE_OID, ROLE_SUBMISSIVE_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertNoLdapSubAccount(USER_SUBMARINE_USERNAME, USER_SUBMARINE_FULL_NAME);

        PrismObject<UserType> user = getUser(USER_SUBMARINE_OID);
        assertNoLinkedAccount(user);
    }

    @Test
    public void test850ReconcileAccounts() throws Exception {
        // GIVEN
        assertUsers(6);

        // WHEN
        when();
        addTask(getReconciliationTaskFile());

        waitForTaskFinish(getReconciliationTaskOid());

        // THEN
        then();

        assertUsers(getNumberOfAllAccounts() + 1); // all accounts + administrator

        // TODO: better asserts

//        assertLdapConnectorInstances(2);
    }

    @Test
    public void test900ImportSyncTask() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        getSyncTask().init(AbstractAdLdapMultidomainTest.this, task, result);

        // THEN
        then();
        assertSuccess(result);

        getSyncTask().rerun(result);

        long tsEnd = System.currentTimeMillis();

        assertStepSyncToken(getSyncTask().oid, 0, tsStart, tsEnd);
    }

    @Test
    public void test901SyncAddAccountHt() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        addLdapAccount(ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);
        getSyncTask().rerun(result);

        // THEN
        then();
        assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        displayUsers();

        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user " + ACCOUNT_HT_UID + " created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

        assertStepSyncToken(getSyncTask().oid, 1, tsStart, tsEnd);
    }

    protected void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd)
            throws ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult("assertStepSyncToken");
        Task task = taskManager.getTaskPlain(syncTaskOid, result);
        String tokenRealValue =
                (String) getRootSyncTokenRealValueRequired(task.getRawTaskObjectClonedIfNecessary().asObjectable());
        assertThat(StringUtils.isBlank(tokenRealValue))
                .as("Empty sync token value")
                .isFalse();
        assertSuccess(result);
    }

    protected Entry assertLdapSubAccount(String samAccountName, String cn) throws LdapException, IOException, CursorException {
        Entry entry = searchLdapAccount(getSubLdapConnectionConfig(), "(cn=" + cn + ")");
        assertAttribute(entry, "cn", cn);
        assertAttribute(entry, ATTRIBUTE_SAM_ACCOUNT_NAME_NAME, samAccountName);
        return entry;
    }

    protected void assertNoLdapSubAccount(String uid, String cn) throws LdapException, IOException, CursorException {
        assertNoLdapAccount(getSubLdapConnectionConfig(), uid, cn);
    }

    protected String toAccountSubDn(String username, String fullName) {
        return ("CN=" + fullName + "," + getPeopleLdapSubSuffix());
    }

    protected String toOrgDn(String cn) {
        return "ou=" + cn + "," + getOrgsLdapSuffix();
    }

    protected String toOrgGroupDn(String groupCn, String orgName) {
        return "cn=" + groupCn + "," + toOrgDn(orgName);
    }

    protected Entry assertLdapOrg(String orgName) throws LdapException, IOException, CursorException {
        String dn = toOrgDn(orgName);
        Entry entry = getLdapEntry(dn);
        assertNotNull("No entry " + dn, entry);
        assertAttribute(entry, "ou", orgName);
        return entry;
    }

    protected Entry assertNoLdapOrg(String orgName) throws LdapException, IOException, CursorException {
        String dn = toOrgDn(orgName);
        Entry entry = getLdapEntry(dn);
        assertNull("Unexpected org entry " + entry, entry);
        return entry;
    }

    protected Entry assertLdapOrgGroup(String groupCn, String orgName) throws LdapException, IOException, CursorException {
        String dn = toOrgGroupDn(groupCn, orgName);
        Entry entry = getLdapEntry(dn);
        assertNotNull("No entry " + dn, entry);
        assertAttribute(entry, "cn", groupCn);
        return entry;
    }

    protected Entry assertNoLdapOrgGroup(String groupCn, String orgName) throws LdapException, IOException, CursorException {
        String dn = toOrgGroupDn(groupCn, orgName);
        Entry entry = getLdapEntry(dn);
        assertNull("Unexpected org group entry " + entry, entry);
        return entry;
    }

    protected abstract void assertAccountDisabled(PrismObject<ShadowType> shadow);

    protected abstract void assertAccountEnabled(PrismObject<ShadowType> shadow);

}
