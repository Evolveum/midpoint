/*
 * Copyright (C) 2015-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad.big;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.conntest.ad.AbstractAdLdapTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

/**
 * Active Directory big test abstract superclass.
 *
 *
 * @author Radovan Semancik
 */
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractAdLdapBigTest extends AbstractAdLdapTest {

    protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "ad-ldap-big");

    protected static final File ROLE_BIG_FILE = new File(TEST_DIR, "role-big.xml");
    protected static final String ROLE_BIG_NAME = "Big";
    protected static final String ROLE_BIG_OID = "e1728102-625a-11ec-838d-d74fbfaa5d44";

    protected static final String GROUP_BIG_NAME = ROLE_BIG_NAME;

    private static final String USER_GUYBRUSH_PASSWORD_123 = "wanna.be.a.123";

    private static final String ASSOCIATION_GROUP_NAME = "group";

    private static final String NS_EXTENSION = "http://whatever.com/my";

    private static final String INTENT_GROUP = "group";

// Real numbers
    private static final int NUM_POPULATED_USERS_PH1 = 600;
    private static final int NUM_POPULATED_USERS_PH2 = 1600;
    private static final int NUM_POPULATED_USERS_PH3 = 3200;
    private static final int NUM_POPULATED_USERS_PH4 = 5200;

// Smaller numbers to test the test ... to make it run quicker during test code development
//    private static final int NUM_POPULATED_USERS_PH1 = 6;
//    private static final int NUM_POPULATED_USERS_PH2 = 16;
//    private static final int NUM_POPULATED_USERS_PH3 = 32;
//    private static final int NUM_POPULATED_USERS_PH4 = 52;

    private static final int NUM_OTHER_USERS = 2;

    private static final QName GROUP_MEMBER_QNAME = new QName(MidPointConstants.NS_RI, ATTRIBUTE_MEMBER_NAME);
    private static final UniformItemPath PATH_GROUP_MEMBER = UniformItemPath.create(ShadowType.F_ATTRIBUTES, GROUP_MEMBER_QNAME);

    private boolean allowDuplicateSearchResults = false;

    private String groupBigShadowOid;

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

    private QName getAssociationGroupQName() {
        return new QName(MidPointConstants.NS_RI, ASSOCIATION_GROUP_NAME);
    }

    @Override
    protected boolean allowDuplicateSearchResults() {
        return allowDuplicateSearchResults;
    }

    protected abstract int getNumberOfAllAccounts();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        binaryAttributeDetector.addBinaryAttribute(ATTRIBUTE_OBJECT_GUID_NAME);
        binaryAttributeDetector.addBinaryAttribute(ATTRIBUTE_UNICODE_PWD_NAME);

        // Users
        repoAddObjectFromFile(USER_GUYBRUSH_FILE, initResult);

        // Roles
        repoAddObjectFromFile(ROLE_END_USER_FILE, initResult);

    }

    @Test
    public void test000Sanity() throws Exception {

        cleanupDelete(toAccountDn(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME));
        cleanupDeleteGeneratedAccounts();
        cleanupDelete(toGroupDn(GROUP_BIG_NAME));
    }

    private void cleanupDeleteGeneratedAccounts() throws CursorException, IOException, LdapException {
        int i = 0;
        // We are NOT using the contants (e.g. NUM_POPULATED_USERS_PH4) here by purpose.
        // We want to clean up entries left behind by a test that had the constants set differently than this test.
        while (true) {
            String username = generateUserName(i);
            String familyName = generateFamilyName(i);
            String fullName = "Dummy " + familyName;
            String dn = toAccountDn(username, fullName);
            display("Try cleanup gen entry "+dn);
            Entry entry = cleanupDelete(dn);
            if (entry == null) {
                break;
            }
            i++;
        }
    }

    @Test
    public void test100AddRoleBig() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        addObject(ROLE_BIG_FILE, task, result);

        // THEN
        then();
        assertSuccess(result);

        String dn = toGroupDn(ROLE_BIG_NAME);
        Entry entry = getLdapEntry(dn);
        display("Role Big: " + entry);
        assertNotNull("No entry " + dn, entry);
        assertAttribute(entry, "cn", GROUP_BIG_NAME);

        PrismObject<RoleType> roleAfter = getRole(ROLE_BIG_OID);
        groupBigShadowOid = getSingleLinkOid(roleAfter);
    }

    @Test
    public void test115SearchBigByCn() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getGroupObjectClass());
        ObjectQueryUtil.filterAnd(query.getFilter(), createAttributeFilter("cn", GROUP_BIG_NAME));

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        groupBigShadowOid = shadow.getOid();
        assertObjectCategory(shadow, getObjectCategoryGroup());

        assertLdapConnectorReasonableInstances();
    }


    @Test
    public void test110AssignGuybrushBig() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        assertNoLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);

        // WHEN
        when();
        assignRole(USER_GUYBRUSH_OID, ROLE_BIG_OID, task, result);

        // THEN
        then();
        assertSuccess(result);

        Entry entry = assertLdapAccount(USER_GUYBRUSH_USERNAME, USER_GUYBRUSH_FULL_NAME);
        displayValue("Entry", entry);

        assertLdapGroupMember(entry, GROUP_BIG_NAME);

        PrismObject<UserType> user = getUser(USER_GUYBRUSH_OID);
        String shadowOid = getSingleLinkOid(user);

        PrismObject<ShadowType> shadow = getObject(ShadowType.class, shadowOid);
        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupQName(), groupBigShadowOid);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Fetch group Big from AD using default settings.
     * This should NOT return list of members, as it has fetchStrategy=minimal.
     *
     * TODO
     */
    @Test
    public void test120GetBigDefault() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        PrismObject<ShadowType> shadow = getShadowModel(groupBigShadowOid);

        // THEN
        then();
        display("Group Big fetched shadow", shadow);
        // MID-7556
        //assertNoAttribute(shadow.asObjectable(), GROUP_MEMBER_QNAME);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Test that fetch of very small group works.
     * Mostly just for sanity.
     */
    @Test
    public void test122GetBigFetchPh0() throws Exception {
        // GIVEN
        testGetBigFetch(1);
    }

    @Test
    public void test200PopulateUsersPh1() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        populateUsers(0, NUM_POPULATED_USERS_PH1);

        // THEN
        then();
        assertUsers(NUM_POPULATED_USERS_PH1 + NUM_OTHER_USERS);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Normal fetch, under the magic 1500 limit.
     * Expecting no problems so far.
     */
    @Test
    public void test202GetBigFetchPh1() throws Exception {
        // GIVEN
        testGetBigFetch(NUM_POPULATED_USERS_PH1 + 1);
    }

    @Test
    public void test210PopulateUsersPh2() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        populateUsers(NUM_POPULATED_USERS_PH1, NUM_POPULATED_USERS_PH2);

        // THEN
        then();
        assertUsers(NUM_POPULATED_USERS_PH2 + NUM_OTHER_USERS);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Range fetch, over the magic 1500 limit.
     * One additional range search is required.
     */
    @Test
    public void test212GetBigFetchPh2() throws Exception {
        // GIVEN
        testGetBigFetch(NUM_POPULATED_USERS_PH2 + 1);
    }

    @Test
    public void test220PopulateUsersPh3() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        populateUsers(NUM_POPULATED_USERS_PH2, NUM_POPULATED_USERS_PH3);

        // THEN
        then();
        assertUsers(NUM_POPULATED_USERS_PH3 + NUM_OTHER_USERS);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Range fetch, twice over the magic 1500 limit.
     * Two additional range searches is required.
     */
    @Test
    public void test222GetBigFetchPh3() throws Exception {
        // GIVEN
        testGetBigFetch(NUM_POPULATED_USERS_PH3 + 1);
    }

    @Test
    public void test230PopulateUsersPh4() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        populateUsers(NUM_POPULATED_USERS_PH3, NUM_POPULATED_USERS_PH4);

        // THEN
        then();
        assertUsers(NUM_POPULATED_USERS_PH4 + NUM_OTHER_USERS);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test232GetBigFetchPh4() throws Exception {
        // GIVEN
        testGetBigFetch(NUM_POPULATED_USERS_PH4 + 1);
    }

    @Test
    public void test900DeleteUsers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        for(int i = 0; i < NUM_POPULATED_USERS_PH4; i++) {
            String username = generateUserName(i);
            PrismObject<UserType> user = findUserByUsername(username);
            deleteObject(UserType.class, user.getOid(), task, result);
        }

    }

    /**
     * Fetch group Big from AD, explicitly requesting member list.
     */
    private void testGetBigFetch(int expectedNumberOfMembers) throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> opts = SelectorOptions.createCollection(GetOperationOptions.createRetrieve(), PATH_GROUP_MEMBER);

        // WHEN
        when();
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, groupBigShadowOid, opts, task, result);

        // THEN
        then();
        display("Group Big fetched shadow", shadow);
        List<String> memberValues = ShadowUtil.getAttributeValues(shadow, GROUP_MEMBER_QNAME);
        display("Member #: "+memberValues.size());
        assertThat(memberValues.size()).as("Unexpected size of group member attribute").isEqualTo(expectedNumberOfMembers);

        assertLdapConnectorReasonableInstances();
    }

    private void populateUsers(int iFrom, int iTo) throws Exception {
        for(int i = iFrom; i < iTo; i++) {
            String username = generateUserName(i);
            String familyName = generateFamilyName(i);
            PrismObject<UserType> user = prismContext.createObject(UserType.class);
            user.asObjectable()
                    .name(username)
                    .givenName("Dummy")
                    .familyName(familyName)
                    .fullName("Dummy " + familyName)
                    .assignment(new AssignmentType(prismContext)
                            .targetRef(ROLE_BIG_OID, RoleType.COMPLEX_TYPE))
                    .credentials(new CredentialsType(prismContext)
                            .password(new PasswordType(prismContext)
                                    .value(new ProtectedStringType()
                                            .clearValue("dummy.dummius.321"))));
            display("Adding user", user);
            addObject(user);
        }

    }

    private String generateUserName(int i) {
        return String.format("dummy%05d", i);
    }

    private String generateFamilyName(int i) {
        return String.format("D%05d", i);
    }

}
