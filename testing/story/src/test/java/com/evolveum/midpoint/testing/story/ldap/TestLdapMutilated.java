/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.ldap;

import static org.testng.AssertJUnit.*;

import java.io.File;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * LDAP Tests with LDAP content that is completely mutilated. It is all wrong.
 * Wrong capitalization in objectClass attributes, spaces in DNs and so on.
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapMutilated extends AbstractLdapTest {

    public static final File TEST_DIR = new File(LDAP_TEST_DIR, "mutilated");

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";
    private static final String RESOURCE_OPENDJ_NAMESPACE = MidPointConstants.NS_RI;

    protected static final String OPENDJ_ACCOUNTS_SUFFIX = "ou=accounts,dc=example,dc=com";

    private String accountJackOid;

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServerRI();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // All capital ACCOUNTS and a space in DN
        openDJController.addEntry("dn: ou=ACCOUNTS, dc=example,dc=com\n" +
                // Capitalization here does not match the DN
                "ou: accounts\n" +
                "objectclass: top\n" +
                // lowecase "u" does not match the schema
                "objectclass: organizationalunit");

        // Resources
        PrismObject<ResourceType> resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        openDJController.setResource(resourceOpenDj);

        DebugUtil.setDetailedDebugDump(false);
    }

    @Override
    protected String getLdapResourceOid() {
        return RESOURCE_OPENDJ_OID;
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task);
        TestUtil.assertSuccess(testResultOpenDj);

        dumpLdap();
    }

    /**
     * Make sure there is no shadow for ou=people,dc=example,dc=com.
     * In fact, there should be no shadow at all.
     * MID-5544
     */
    @Test
    public void test010Shadows() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceQuery(RESOURCE_OPENDJ_OID, prismContext);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);

        // THEN
        then();
        assertSuccess(result);

        display("Found shadows", shadows);
        assertEquals("Unexpected number of shadows", 0, shadows.size());
    }

    /**
     * Simple test, more like a sanity test that everything works OK.
     */
    @Test
    public void test100AssignAccountOpenDjSimple() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_OPENDJ_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountJackOid = assertUserAfter(USER_JACK_OID)
                .singleLink()
                .getOid();

        assertModelShadow(accountJackOid)
                .display();

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME);
    }

    /**
     * Make sure there is no shadow for ou=people,dc=example,dc=com.
     * In fact, there should be no shadow at all.
     * MID-5544
     */
    @Test
    public void test105Shadows() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceQuery(RESOURCE_OPENDJ_OID, prismContext);
        displayDumpable("Query", query);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);

        // THEN
        then();
        assertSuccess(result);

        display("Found shadows", shadows);
        assertEquals("Unexpected number of shadows", 1, shadows.size());
    }

    @Test
    public void test109UnassignAccountOpenDjSimple() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignAccountFromUser(USER_JACK_OID, RESOURCE_OPENDJ_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .links()
                .assertNoLiveLinks();

        assertNoShadow(accountJackOid);

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertNull("Unexpected LDAP entry for jack", accountEntry);
    }

    /**
     * Make sure there is no shadow for ou=accounts,dc=example,dc=com.
     * We haven't searched for accounts yet.
     * MID-5544
     */
    @Test
    public void test300Shadows() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceQuery(RESOURCE_OPENDJ_OID, prismContext);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);

        // THEN
        then();
        assertSuccess(result);

        display("Found shadows", shadows);
        assertEquals("Unexpected number of shadows", 0, shadows.size());
    }

    /**
     * Normal search, all accounts in ou=accounts
     * MID-5485
     */
    @Test
    public void test310SearchLdapAccounts() throws Exception {
        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID,
                ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT, prismContext);

        // WHEN
        when();
        searchObjectsIterative(ShadowType.class, query, o -> display("Found object", o), 0);

    }

    /**
     * MID-5544
     */
    @Test
    public void test312Shadows() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceQuery(RESOURCE_OPENDJ_OID, prismContext);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);

        // THEN
        then();
        assertSuccess(result);

        display("Found shadows", shadows);
        // 1 shadow for ou=accounts
        assertEquals("Unexpected number of shadows", 1, shadows.size());
        PrismObject<ShadowType> peopleShadow = null;
        for (PrismObject<ShadowType> shadow : shadows) {
            if (StringUtils.equalsIgnoreCase(shadow.getName().getOrig(), OPENDJ_ACCOUNTS_SUFFIX)) {
                peopleShadow = shadow;
            }
        }
        assertNotNull("No ou=accounts shadow", peopleShadow);
        assertShadow(peopleShadow, "ou=accounts")
                .display()
                .assertObjectClass(new QName(MidPointConstants.NS_RI, "organizationalUnit"))
                .assertKind(ShadowKindType.UNKNOWN);
    }

    /**
     * Simple test, more like a sanity test that everything works OK.
     */
    @Test
    public void test320AssignAccountOpenDj() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_OPENDJ_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountJackOid = assertUserAfter(USER_JACK_OID)
                .singleLink()
                .getOid();

        assertModelShadow(accountJackOid)
                .display();

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME);
    }

    /**
     * Normal search, all accounts in ou=accounts
     * MID-5544
     */
    @Test
    public void test322SearchLdapAccounts() throws Exception {
        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID,
                ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT, prismContext);

        // WHEN
        when();
        searchObjectsIterative(ShadowType.class, query, o -> display("Found object", o), 1);
    }
}
