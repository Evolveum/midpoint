/*
 * Copyright (c) 2015-2016 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import javax.xml.namespace.QName;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.testng.AssertJUnit;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 */
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractEDirTest extends AbstractLdapTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "edir");

    protected static final File ROLE_PIRATES_FILE = new File(TEST_DIR, "role-pirate.xml");
    protected static final String ROLE_PIRATES_OID = "5dd034e8-41d2-11e5-a123-001e8c717e5b";

    protected static final File ROLE_META_ORG_FILE = new File(TEST_DIR, "role-meta-org.xml");
    protected static final String ROLE_META_ORG_OID = "f2ad0ace-45d7-11e5-af54-001e8c717e5b";

    public static final String ATTRIBUTE_LOCKOUT_LOCKED_NAME = "lockedByIntruder";
    public static final String ATTRIBUTE_LOCKOUT_RESET_TIME_NAME = "loginIntruderResetTime";
    public static final String ATTRIBUTE_GROUP_MEMBERSHIP_NAME = "groupMembership";
    public static final String ATTRIBUTE_EQUIVALENT_TO_ME_NAME = "equivalentToMe";
    public static final String ATTRIBUTE_SECURITY_EQUALS_NAME = "securityEquals";

    protected static final String ACCOUNT_JACK_UID = "jack";
    protected static final String ACCOUNT_JACK_PASSWORD = "qwe123";

    private static final String GROUP_PIRATES_NAME = "pirates";
    private static final String GROUP_MELEE_ISLAND_NAME = "Mêlée Island";
    private static final String GROUP_MELA_NOVA_NAME = "Mela Nova";

    protected static final int NUMBER_OF_ACCOUNTS = 4;
    protected static final int LOCKOUT_EXPIRATION_SECONDS = 65;
    private static final String ASSOCIATION_GROUP_NAME = "group";

    protected String jackAccountOid;
    protected String groupPiratesOid;
    protected long jackLockoutTimestamp;
    private String accountBarbossaOid;
    private String orgMeleeIslandOid;
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
        return null;
    }

    @Override
    protected boolean useSsl() {
        return true;
    }

    @Override
    protected String getLdapSuffix() {
        return "o=example";
    }

    @Override
    protected String getLdapBindDn() {
        return "cn=admin,o=example";
    }

    @Override
    protected String getLdapBindPassword() {
        return "secret";
    }

    @Override
    protected int getSearchSizeLimit() {
        return -1;
    }

    @Override
    public String getPrimaryIdentifierAttributeName() {
        return "GUID";
    }

    @Override
    protected String getLdapGroupObjectClass() {
        return "groupOfNames";
    }

    @Override
    protected String getLdapGroupMemberAttribute() {
        return "member";
    }

    private QName getAssociationGroupQName() {
        return new QName(MidPointConstants.NS_RI, ASSOCIATION_GROUP_NAME);
    }

    protected String getOrgGroupsLdapSuffix() {
        return "ou=orggroups," + getLdapSuffix();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        binaryAttributeDetector.addBinaryAttribute("GUID");

        // Users
        repoAddObjectFromFile(USER_BARBOSSA_FILE, initResult);
        repoAddObjectFromFile(USER_GUYBRUSH_FILE, initResult);

        // Roles
        repoAddObjectFromFile(ROLE_PIRATES_FILE, initResult);
        repoAddObjectFromFile(ROLE_META_ORG_FILE, initResult);

    }

    @Test
    public void test000Sanity() throws Exception {
        assertLdapPassword(ACCOUNT_JACK_UID, ACCOUNT_JACK_PASSWORD);
        assertEDirGroupMember(ACCOUNT_JACK_UID, GROUP_PIRATES_NAME);
        cleanupDelete(toAccountDn(USER_BARBOSSA_USERNAME));
        cleanupDelete(toAccountDn(USER_CPTBARBOSSA_USERNAME));
        cleanupDelete(toAccountDn(USER_GUYBRUSH_USERNAME));
        cleanupDelete(toOrgGroupDn(GROUP_MELEE_ISLAND_NAME));
        cleanupDelete(toOrgGroupDn(GROUP_MELA_NOVA_NAME));
    }

    @Test
    public void test050Capabilities() {
        CapabilityCollectionType nativeCapabilitiesCollection = ResourceTypeUtil.getNativeCapabilitiesCollection(resourceType);
        display("Native capabilities", nativeCapabilitiesCollection);

        assertTrue("No native activation capability", ResourceTypeUtil.hasResourceNativeActivationCapability(resourceType));
        assertTrue("No native activation status capability", ResourceTypeUtil.hasResourceNativeActivationStatusCapability(resourceType));
        assertTrue("No native lockout capability", ResourceTypeUtil.hasResourceNativeActivationLockoutCapability(resourceType));
        assertTrue("No native credentials capability", ResourceTypeUtil.isCredentialsCapabilityEnabled(resourceType, null));     // todo ok?
    }

    @Test
    public void test100SearchJackByLdapUid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = createUidQuery(ACCOUNT_JACK_UID);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_JACK_UID));
        assertShadowLockout(shadow, LockoutStatusType.NORMAL);
        jackAccountOid = shadow.getOid();
        assertNotNull("Null OID in " + shadow, jackAccountOid);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 2);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }
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
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        groupPiratesOid = shadow.getOid();

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 2);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }
    }

    @Test
    public void test110GetJack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = createUidQuery(ACCOUNT_JACK_UID);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, jackAccountOid, null, task, result);

        // THEN
        result.computeStatus();
        TestUtil.assertSuccess(result);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_JACK_UID));
        assertShadowLockout(shadow, LockoutStatusType.NORMAL);
        assertPasswordAllowChange(shadow, null);
        jackAccountOid = shadow.getOid();

        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupQName(), groupPiratesOid);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
    }

    @Test
    public void test120JackLockout() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);

        jackLockoutTimestamp = System.currentTimeMillis();

        ObjectQuery query = createUidQuery(ACCOUNT_JACK_UID);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_JACK_UID));
        assertShadowLockout(shadow, LockoutStatusType.LOCKED);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }
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

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, NUMBER_OF_ACCOUNTS, task, result);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }
    }

    @Test
    public void test190SearchLockedAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassFilterPrefix(getResourceOid(), getAccountObjectClass())
                .and().item(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS).eq(LockoutStatusType.LOCKED)
                .build();

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 1, task, result);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        PrismObject<ShadowType> shadow = searchResultList.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_JACK_UID));
        assertShadowLockout(shadow, LockoutStatusType.LOCKED);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }
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
        assertNotNull("No identifier in " + shadow, accountBarbossaIcfUid);

        assertEquals("Wrong ICFS UID", MiscUtil.bytesToHex(entry.get(getPrimaryIdentifierAttributeName()).getBytes()), accountBarbossaIcfUid);

        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_PASSWORD);
        assertPasswordAllowChange(shadow, null);

        ResourceAttribute<Long> createTimestampAttribute = ShadowUtil.getAttribute(shadow, new QName(MidPointConstants.NS_RI, "createTimestamp"));
        assertNotNull("No createTimestamp in " + shadow, createTimestampAttribute);
        Long createTimestamp = createTimestampAttribute.getRealValue();
        // LDAP server may be on a different host. Allow for some clock offset.
        TestUtil.assertBetween("Wrong createTimestamp in " + shadow, roundTsDown(tsStart) - 1000, roundTsUp(tsEnd) + 1000, createTimestamp);
    }

    @Test
    public void test210ModifyAccountBarbossaTitle() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, "title");
        //noinspection unchecked
        ResourceAttributeDefinition<String> attrDef =
                (ResourceAttributeDefinition<String>) accountDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "Captain");
        delta.addModification(attrDelta);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
    }

    @Test
    public void test220ModifyUserBarbossaPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue("hereThereBeMonsters");

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID, PATH_CREDENTIALS_PASSWORD_VALUE, task, result, userPasswordPs);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");
        assertLdapPassword(USER_BARBOSSA_USERNAME, "hereThereBeMonsters");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
    }

    @Test
    public void test230DisableBarbossa() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID, PATH_ACTIVATION_ADMINISTRATIVE_STATUS, task, result, ActivationStatusType.DISABLED);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertAdministrativeStatus(user, ActivationStatusType.DISABLED);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "loginDisabled", "TRUE");

        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAdministrativeStatus(shadow, ActivationStatusType.DISABLED);
    }

    @Test
    public void test239EnableBarbossa() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID, PATH_ACTIVATION_ADMINISTRATIVE_STATUS, task, result, ActivationStatusType.ENABLED);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertAdministrativeStatus(user, ActivationStatusType.ENABLED);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "loginDisabled", "FALSE");

        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAdministrativeStatus(shadow, ActivationStatusType.ENABLED);
    }

    /**
     * passwordAllowChange is a boolean attribute
     */
    @Test
    public void test240ModifyAccountBarbossaPasswordAllowChangeFalse() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, "passwordAllowChange");
        //noinspection unchecked
        ResourceAttributeDefinition<Boolean> attrDef =
                (ResourceAttributeDefinition<Boolean>) accountDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<Boolean> attrDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, Boolean.FALSE);
        delta.addModification(attrDelta);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "passwordAllowChange", "FALSE");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        display("Shadow after", shadow);
        assertPasswordAllowChange(shadow, false);

    }

    /**
     * This should create account with a group. And disabled.
     */
    @Test
    public void test250AssignGuybrushPirates() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        modifyUserReplace(USER_GUYBRUSH_OID, PATH_ACTIVATION_ADMINISTRATIVE_STATUS, task, result, ActivationStatusType.DISABLED);

        // WHEN
        when();
        assignRole(USER_GUYBRUSH_OID, ROLE_PIRATES_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        displayValue("Entry", entry);
        assertAttribute(entry, "loginDisabled", "TRUE");

        assertEDirGroupMember(entry, GROUP_PIRATES_NAME);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        assertAdministrativeStatus(user, ActivationStatusType.DISABLED);
        String shadowOid = getSingleLinkOid(user);

        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupQName(), groupPiratesOid);
        assertAdministrativeStatus(shadow, ActivationStatusType.DISABLED);
    }

    @Test
    public void test260EnableGyubrush() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_GUYBRUSH_OID, PATH_ACTIVATION_ADMINISTRATIVE_STATUS, task, result, ActivationStatusType.ENABLED);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        assertAdministrativeStatus(user, ActivationStatusType.ENABLED);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        assertAttribute(entry, "loginDisabled", "FALSE");

        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        assertAdministrativeStatus(shadow, ActivationStatusType.ENABLED);
    }

    // TODO: search for disabled accounts

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

        Entry groupEntry = assertEDirGroupMember(entry, GROUP_PIRATES_NAME);
        displayValue("Group entry", groupEntry);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupQName(), groupPiratesOid);
    }

    @Test
    public void test390ModifyUserBarbossaRename() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        renameObject(UserType.class, USER_BARBOSSA_OID, USER_CPTBARBOSSA_USERNAME, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        display("Shadow after rename (model)", shadow);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        display("Shadow after rename (repo)", repoShadow);

        assertNoLdapAccount(USER_BARBOSSA_USERNAME);
    }

    // TODO: create account with a group membership

    @Test
    public void test500AddOrgMeleeIsland() throws Exception {
        // GIVEN
        Task task = getTestTask();
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
        when();
        addObject(org, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        orgMeleeIslandOid = org.getOid();
        Entry entry = assertLdapGroup(GROUP_MELEE_ISLAND_NAME);

        org = getObject(OrgType.class, orgMeleeIslandOid);
        groupMeleeOid = getSingleLinkOid(org);
        PrismObject<ShadowType> shadow = getShadowModel(groupMeleeOid);
        display("Shadow (model)", shadow);
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

        assertEDirGroupMember(entry, GROUP_MELEE_ISLAND_NAME);

        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupQName(), groupMeleeOid);
    }

    @Test
    public void test520RenameOrgMeleeIsland() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        renameObject(OrgType.class, orgMeleeIslandOid, GROUP_MELA_NOVA_NAME, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapGroup(GROUP_MELA_NOVA_NAME);

        PrismObject<OrgType> org = getObject(OrgType.class, orgMeleeIslandOid);
        String groupMeleeOidAfter = getSingleLinkOid(org);
        PrismObject<ShadowType> shadow = getShadowModel(groupMeleeOidAfter);
        display("Shadow (model)", shadow);
    }

    // Wait until the lockout of Jack expires, check status
    @Test
    public void test800JackLockoutExpires() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        long now = System.currentTimeMillis();
        long lockoutExpires = jackLockoutTimestamp + LOCKOUT_EXPIRATION_SECONDS * 1000;
        if (now < lockoutExpires) {
            display("Sleeping for " + (lockoutExpires - now) + "ms (waiting for lockout expiration)");
            Thread.sleep(lockoutExpires - now);
        }
        now = System.currentTimeMillis();
        display("Time is now " + now);

        ObjectQuery query = createUidQuery(ACCOUNT_JACK_UID);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_JACK_UID));
        assertShadowLockout(shadow, LockoutStatusType.NORMAL);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }
    }

    @Test
    public void test810SearchLockedAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassFilterPrefix(getResourceOid(), getAccountObjectClass())
                .and().item(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS).eq(LockoutStatusType.LOCKED)
                .build();
        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 0, task, result);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);
    }

    @Test
    public void test820JackLockoutAndUnlock() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);

        jackLockoutTimestamp = System.currentTimeMillis();

        ObjectQuery query = createUidQuery(ACCOUNT_JACK_UID);

        // precondition
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());
        PrismObject<ShadowType> shadowLocked = shadows.get(0);
        display("Locked shadow", shadowLocked);
        assertAccountShadow(shadowLocked, toAccountDn(ACCOUNT_JACK_UID));
        assertShadowLockout(shadowLocked, LockoutStatusType.LOCKED);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        modifyObjectReplaceProperty(ShadowType.class, shadowLocked.getOid(), PATH_ACTIVATION_LOCKOUT_STATUS, task, result,
                LockoutStatusType.NORMAL);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        PrismObject<ShadowType> shadowAfter = getObject(ShadowType.class, shadowLocked.getOid());
        display("Shadow after", shadowAfter);
        assertAccountShadow(shadowAfter, toAccountDn(ACCOUNT_JACK_UID));
        assertShadowLockout(shadowAfter, LockoutStatusType.NORMAL);

        assertLdapPassword(ACCOUNT_JACK_UID, ACCOUNT_JACK_PASSWORD);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }
    }

    // Let's do this at the very end.
    // We need to wait after rename, otherwise the delete fail with:
    // NDS error: previous move in progress (-637)
    // So ... let's give some time to eDirectory to sort the things out

    @Test
    public void test890UnAssignBarbossaPirates() throws Exception {
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

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        displayValue("Entry", entry);
        assertAttribute(entry, "title", "Captain");

        assertEDirNoGroupMember(entry, GROUP_PIRATES_NAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        IntegrationTestTools.assertNoAssociation(shadow, getAssociationGroupQName(), groupPiratesOid);

    }

    @Test
    public void test899UnAssignAccountBarbossa() throws Exception {
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

        assertNoLdapAccount(USER_BARBOSSA_USERNAME);
        assertNoLdapAccount(USER_CPTBARBOSSA_USERNAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertNoLinkedAccount(user);
    }

    // TODO: lock out jack again, explicitly reset the lock, see that he can login

    @Override
    protected void assertAccountShadow(PrismObject<ShadowType> shadow, String dn) throws SchemaException, ConfigurationException {
        super.assertAccountShadow(shadow, dn);
        ResourceAttribute<String> primaryIdAttr = ShadowUtil.getAttribute(shadow, getPrimaryIdentifierAttributeQName());
        assertNotNull("No primary identifier (" + getPrimaryIdentifierAttributeQName() + " in " + shadow, primaryIdAttr);
        String primaryId = primaryIdAttr.getRealValue();
        assertTrue("Unexpected chars in primary ID: '" + primaryId + "'", primaryId.matches("[a-z0-9]+"));
    }

    protected void assertPasswordAllowChange(PrismObject<ShadowType> shadow, Boolean expected) throws SchemaException {
        Boolean passwordAllowChange = ShadowUtil.getAttributeValue(shadow, new QName(MidPointConstants.NS_RI, "passwordAllowChange"));
        assertEquals("Wrong passwordAllowChange in " + shadow, expected, passwordAllowChange);
    }

    private void makeBadLoginAttempt(String uid) throws LdapException, IOException {
        try {
            LdapNetworkConnection conn = ldapConnect(toAccountDn(uid), "thisIsAwRoNgPASSW0RD");
            if (conn.isAuthenticated()) {
                AssertJUnit.fail("Bad authentication went good for " + uid);
            }
        } catch (SecurityException e) {
            // this is expected
        }
    }

    private void assertEDirGroupMember(String accountUid, String groupName) throws LdapException, IOException, CursorException, SchemaException {
        Entry accountEntry = getLdapAccountByUid(accountUid);
        assertEDirGroupMember(accountEntry, groupName);
    }

    private Entry assertEDirGroupMember(Entry accountEntry, String groupName) throws LdapException, IOException, CursorException, SchemaException {
        Entry groupEntry = getLdapGroupByName(groupName);
        assertAttributeContains(groupEntry, getLdapGroupMemberAttribute(), accountEntry.getDn().toString());
        assertAttributeContains(groupEntry, ATTRIBUTE_EQUIVALENT_TO_ME_NAME, accountEntry.getDn().toString());
        assertAttributeContains(accountEntry, ATTRIBUTE_GROUP_MEMBERSHIP_NAME, groupEntry.getDn().toString());
        assertAttributeContains(accountEntry, ATTRIBUTE_SECURITY_EQUALS_NAME, groupEntry.getDn().toString());
        return groupEntry;
    }

    private void assertEDirNoGroupMember(Entry accountEntry, String groupName) throws LdapException, IOException, CursorException, SchemaException {
        Entry groupEntry = getLdapGroupByName(groupName);
        assertAttributeNotContains(groupEntry, getLdapGroupMemberAttribute(), accountEntry.getDn().toString());
        assertAttributeNotContains(groupEntry, ATTRIBUTE_EQUIVALENT_TO_ME_NAME, accountEntry.getDn().toString());
        assertAttributeNotContains(accountEntry, ATTRIBUTE_GROUP_MEMBERSHIP_NAME, groupEntry.getDn().toString());
        assertAttributeNotContains(accountEntry, ATTRIBUTE_SECURITY_EQUALS_NAME, groupEntry.getDn().toString());
    }

    protected String toOrgGroupDn(String cn) {
        return "cn=" + cn + "," + getOrgGroupsLdapSuffix();
    }
}
