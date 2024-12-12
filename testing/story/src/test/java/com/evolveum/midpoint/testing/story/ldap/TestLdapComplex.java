/*
 * Copyright (c) 2016-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.ldap;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Complex LDAP tests:
 *
 * Testing PolyString all the way to LDAP connector. The PolyString data should be translated
 * to LDAP "language tag" attributes (attribute options).
 * MID-5210
 *
 * search scope limited to "one" (MID-5485)
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapComplex extends AbstractLdapTest {

    public static final File TEST_DIR = new File(LDAP_TEST_DIR, "complex");

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    private static final String RESOURCE_OPENDJ_OID = "10000000-0000-0000-0000-000000000003";

    private static final File ORG_PROJECT_TOP_FILE = new File(TEST_DIR, "org-project-top.xml");
    private static final String ORG_PROJECT_TOP_OID = "4547657c-e9bc-11e9-87d6-ef7cd13d2828";

    private static final File ORG_FUNCTIONAL_TOP_FILE = new File(TEST_DIR, "org-functional-top.xml");
    private static final String ORG_FUNCTIONAL_TOP_OID = "44dff496-e9bc-11e9-8c17-4fc5d5f4d2cf";

    private static final String[] JACK_FULL_NAME_LANG_EN_SK = {
            "en", "Jack Sparrow",
            "sk", "Džek Sperou"
    };

    private static final String[] JACK_FULL_NAME_LANG_EN_SK_RU_HR = {
            "en", "Jack Sparrow",
            "sk", "Džek Sperou",
            "ru", "Джек Воробей",
            "hr", "Ðek Sperou"
    };

    private static final String[] JACK_FULL_NAME_LANG_CZ_HR = {
            "cz", "Džek Sperou",
            "hr", "Ðek Sperou"
    };

    protected static final String USER_JACK_FULL_NAME_CAPTAIN = "Captain Jack Sparrow";

    private static final String[] JACK_FULL_NAME_LANG_CAPTAIN_EN_CZ_SK = {
            "en", "Captain Jack Sparrow",
            "cz", "Kapitán Džek Sperou",
            "sk", "Kapitán Džek Sperou"
    };

    private static final String TITLE_CAPTAIN = "captain";
    private static final String[] TITLE_EN_SK_RU = {
            "en", "captain",
            "sk", "kapitán",
            "ru", "капитан"
    };
    private static final String[] TITLE_HR = {
            "hr", "kapetan"
    };
    private static final String[] TITLE_EN_SK_RU_HR = {
            "en", "captain",
            "sk", "kapitán",
            "ru", "капитан",
            "hr", "kapetan"
    };
    private static final String[] TITLE_RU = {
            "ru", "капитан"
    };
    private static final String[] TITLE_EN_SK_HR = {
            "en", "captain",
            "sk", "kapitán",
            "hr", "kapetan"
    };

    private static final String USER_JACK_BLAHBLAH = "BlahBlahBlah!";

    private static final String INTENT_LDAP_PROJECT_GROUP = "ldapProjectGroup";
    private static final String INTENT_LDAP_ORG_GROUP = "ldapOrgGroup";

    private static final String ASSOCIATION_LDAP_PROJECT_GROUP = "ldapProjectGroup";
    private static final String ASSOCIATION_LDAP_ORG_GROUP = "ldapOrgGroup";

    private static final String PROJECT_KEELHAUL_NAME = "Keelhaul";
    private static final String PROJECT_WALK_THE_PLANK_NAME = "Walk the Plank";
    private static final String ORG_RUM_DEPARTMENT_NAME = "Rum department";

    private String accountJackOid;
    private String projectKeelhaulOid;
    private String projectWalkThePlankOid;
    private String orgRumDepartmentOid;
    private String groupKeelhaulOid;
    private String groupWalkThePlankOid;
    private String groupRumDepartmentOid;

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

        // Resources
        PrismObject<ResourceType> resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        openDJController.setResource(resourceOpenDj);

        openDJController.addEntry("dn: ou=orgStruct,dc=example,dc=com\n" +
                "objectClass: organizationalUnit\n" +
                "ou: orgStruct");

        importObjectFromFile(ORG_PROJECT_TOP_FILE);
        importObjectFromFile(ORG_FUNCTIONAL_TOP_FILE);

        DebugUtil.setDetailedDebugDump(false);
    }

    @Override
    protected String getLdapResourceOid() {
        return RESOURCE_OPENDJ_OID;
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_OPENDJ_OID, task, task.getResult());
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

        ObjectQuery query = ObjectQueryUtil.createResourceQuery(RESOURCE_OPENDJ_OID);

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
     * Simple test, more like a sanity test that everything works OK with simple polystrings (no lang yet).
     */
    @Test
    public void test050AssignAccountOpenDjSimple() throws Exception {
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
        assertDescription(accountEntry, USER_JACK_FULL_NAME /* no langs here (yet) */);
        assertTitle(accountEntry, null /* no langs here (yet) */);
    }

    /**
     * Make sure there is no shadow for ou=people,dc=example,dc=com.
     * In fact, there should be no shadow at all.
     * MID-5544
     */
    @Test
    public void test055Shadows() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceQuery(RESOURCE_OPENDJ_OID);
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
    public void test059UnassignAccountOpenDjSimple() throws Exception {
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
     * Things are getting interesting here. We set up Jack's full name with
     * a small set of 'lang' values.
     * No provisioning yet. Just to make sure midPoint core works.
     */
    @Test
    public void test100ModifyJackFullNameLang() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PolyString newFullName = new PolyString(USER_JACK_FULL_NAME);
        newFullName.setLang(MiscUtil.paramsToMap(JACK_FULL_NAME_LANG_EN_SK));

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, newFullName);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .fullName()
                .display()
                .assertOrig(USER_JACK_FULL_NAME)
                .assertLangs(JACK_FULL_NAME_LANG_EN_SK)
                .end()
                .links()
                .assertNoLiveLinks();

    }

    /**
     * Assign LDAP account to jack. Jack's fullName is full of langs,
     * those should be translated to description;lang-* LDAP attributes.
     * MID-5210
     */
    @Test
    public void test110AssignAccountOpenDjLang() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_OPENDJ_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountJackOid = assertUserAfter(USER_JACK_OID)
                .fullName()
                .assertOrig(USER_JACK_FULL_NAME)
                .assertLangs(JACK_FULL_NAME_LANG_EN_SK)
                .end()
                .singleLink()
                .getOid();

        assertModelShadow(accountJackOid);

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME);
        assertDescription(accountEntry, USER_JACK_FULL_NAME, JACK_FULL_NAME_LANG_EN_SK);
        assertTitle(accountEntry, null /* no langs here (yet) */);
    }

    /**
     * Adding more langs to Jack's fullName. This should update all
     * LDAP language tags properly.
     */
    @Test
    public void test112ModifyJackFullNameLangEnSkRuHr() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PolyString newFullName = new PolyString(USER_JACK_FULL_NAME);
        newFullName.setLang(MiscUtil.paramsToMap(JACK_FULL_NAME_LANG_EN_SK_RU_HR));

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, newFullName);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .fullName()
                .display()
                .assertOrig(USER_JACK_FULL_NAME)
                .assertLangs(JACK_FULL_NAME_LANG_EN_SK_RU_HR)
                .end()
                .singleLink()
                .assertOid(accountJackOid);

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME);
        assertDescription(accountEntry, USER_JACK_FULL_NAME, JACK_FULL_NAME_LANG_EN_SK_RU_HR);
        assertTitle(accountEntry, null /* no langs here (yet) */);
    }

    /**
     * Modifying langs in Jack's fullName again. Some are removed, some are new.
     * This should update all LDAP language tags properly.
     */
    @Test
    public void test114ModifyJackFullNameLangCzHr() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PolyString newFullName = new PolyString(USER_JACK_FULL_NAME);
        newFullName.setLang(MiscUtil.paramsToMap(JACK_FULL_NAME_LANG_CZ_HR));

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, newFullName);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .fullName()
                .display()
                .assertOrig(USER_JACK_FULL_NAME)
                .assertLangs(JACK_FULL_NAME_LANG_CZ_HR)
                .end()
                .singleLink()
                .assertOid(accountJackOid);

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME);
        assertDescription(accountEntry, USER_JACK_FULL_NAME, JACK_FULL_NAME_LANG_CZ_HR);
        assertTitle(accountEntry, null /* no langs here (yet) */);
    }

    /**
     * Modifying Jack's full name to include proper "Captain" title.
     * The orig is also changed this time.
     */
    @Test
    public void test116ModifyJackFullNameLangCaptain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PolyString newFullName = new PolyString(USER_JACK_FULL_NAME_CAPTAIN);
        newFullName.setLang(MiscUtil.paramsToMap(JACK_FULL_NAME_LANG_CAPTAIN_EN_CZ_SK));

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, newFullName);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .fullName()
                .display()
                .assertOrig(USER_JACK_FULL_NAME_CAPTAIN)
                .assertLangs(JACK_FULL_NAME_LANG_CAPTAIN_EN_CZ_SK)
                .end()
                .singleLink()
                .assertOid(accountJackOid);

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME_CAPTAIN);
        assertDescription(accountEntry, USER_JACK_FULL_NAME_CAPTAIN, JACK_FULL_NAME_LANG_CAPTAIN_EN_CZ_SK);
        assertTitle(accountEntry, null /* no langs here (yet) */);
    }

    /**
     * Back to simple polystring. No langs.
     */
    @Test
    public void test118ModifyJackFullNameCaptain() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PolyString newFullName = new PolyString(USER_JACK_FULL_NAME_CAPTAIN);

        // WHEN
        when();
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, newFullName);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .fullName()
                .display()
                .assertOrig(USER_JACK_FULL_NAME_CAPTAIN)
                .assertNoLangs()
                .end()
                .singleLink()
                .assertOid(accountJackOid);

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME_CAPTAIN);
        assertDescription(accountEntry, USER_JACK_FULL_NAME_CAPTAIN /* no langs */);
        assertTitle(accountEntry, null /* no langs here (yet) */);
    }

    @Test
    public void test119UnassignAccountOpenDjLang() throws Exception {
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
     * Things are getting even more interesting here. We set up Jack's title map
     * (in the extension), so it can be source of confu...errr..fun later on.
     * No provisioning yet. Just to make sure midPoint core works.
     */
    @Test
    public void test120ModifyJackTitleMap() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<PrismContainerValue<?>> cvals = createTitleMapValues(TITLE_EN_SK_RU);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(PATH_EXTENSION_TITLE_MAP)
                .replace(cvals)
                .asObjectDelta(USER_JACK_OID);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
                .extension()
                .container(TITLE_MAP_QNAME)
                .assertSize(TITLE_EN_SK_RU.length / 2)
                .end()
                .end()
                .links()
                .assertNoLiveLinks();
    }

    /**
     * Assign LDAP account to jack.
     * There is titleMap in extension that is mapped to LDAP title attribute.
     * There should be a nice polystring in LDAP title.
     * MID-5264
     */
    @Test
    public void test130AssignAccountOpenDjTitleMap() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_OPENDJ_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountJackOid = assertUserAfter(USER_JACK_OID)
                .fullName()
                .assertOrig(USER_JACK_FULL_NAME_CAPTAIN)
                .assertNoLangs()
                .end()
                .extension()
                .container(TITLE_MAP_QNAME)
                .assertSize(TITLE_EN_SK_RU.length / 2)
                .end()
                .end()
                .singleLink()
                .getOid();

        assertModelShadow(accountJackOid);

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME_CAPTAIN);
        assertDescription(accountEntry, USER_JACK_FULL_NAME_CAPTAIN /* no langs */);
        assertTitle(accountEntry, TITLE_CAPTAIN, TITLE_EN_SK_RU);
    }

    /**
     * Add some container values.
     * MID-5264
     */
    @Test
    public void test132AssignAccountOpenDjTitleMapAdd() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<PrismContainerValue<?>> cvals = createTitleMapValues(TITLE_HR);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(PATH_EXTENSION_TITLE_MAP)
                .add(cvals)
                .asObjectDelta(USER_JACK_OID);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountJackOid = assertUserAfter(USER_JACK_OID)
                .fullName()
                .assertOrig(USER_JACK_FULL_NAME_CAPTAIN)
                .assertNoLangs()
                .end()
                .extension()
                .container(TITLE_MAP_QNAME)
                .assertSize(TITLE_EN_SK_RU_HR.length / 2)
                .end()
                .end()
                .singleLink()
                .getOid();

        assertModelShadow(accountJackOid);

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME_CAPTAIN);
        assertDescription(accountEntry, USER_JACK_FULL_NAME_CAPTAIN /* no langs */);
        assertTitle(accountEntry, TITLE_CAPTAIN, TITLE_EN_SK_RU_HR);
    }

    /**
     * Delete some container values.
     * MID-5264
     */
    @Test
    public void test134AssignAccountOpenDjTitleMapDelete() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<PrismContainerValue<?>> cvals = createTitleMapValues(TITLE_RU);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(PATH_EXTENSION_TITLE_MAP)
                .delete(cvals)
                .asObjectDelta(USER_JACK_OID);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountJackOid = assertUserAfter(USER_JACK_OID)
                .fullName()
                .assertOrig(USER_JACK_FULL_NAME_CAPTAIN)
                .assertNoLangs()
                .end()
                .extension()
                .container(TITLE_MAP_QNAME)
                .assertSize(TITLE_EN_SK_HR.length / 2)
                .end()
                .end()
                .singleLink()
                .getOid();

        assertModelShadow(accountJackOid);

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME_CAPTAIN);
        assertDescription(accountEntry, USER_JACK_FULL_NAME_CAPTAIN /* no langs */);
        assertTitle(accountEntry, TITLE_CAPTAIN, TITLE_EN_SK_HR);
    }

    /**
     * Add some container values.
     * MID-5264
     */
    @Test
    public void test138AssignAccountOpenDjTitleMapReplace() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<PrismContainerValue<?>> cvals = createTitleMapValues(TITLE_EN_SK_RU);

        ObjectDelta<UserType> delta = deltaFor(UserType.class)
                .item(PATH_EXTENSION_TITLE_MAP)
                .replace(cvals)
                .asObjectDelta(USER_JACK_OID);

        // WHEN
        when();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountJackOid = assertUserAfter(USER_JACK_OID)
                .fullName()
                .assertOrig(USER_JACK_FULL_NAME_CAPTAIN)
                .assertNoLangs()
                .end()
                .extension()
                .container(TITLE_MAP_QNAME)
                .assertSize(TITLE_EN_SK_RU.length / 2)
                .end()
                .end()
                .singleLink()
                .getOid();

        assertModelShadow(accountJackOid);

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME_CAPTAIN);
        assertDescription(accountEntry, USER_JACK_FULL_NAME_CAPTAIN /* no langs */);
        assertTitle(accountEntry, TITLE_CAPTAIN, TITLE_EN_SK_RU);
    }

    @Test
    public void test139UnassignAccountOpenDjTitleMap() throws Exception {
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
     * Mostly just preparation for next tests. Just make sure there is
     * (pretty ordinary) LDAP account for jack.
     */
    @Test
    public void test150AssignAccountOpenDj() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignAccountToUser(USER_JACK_OID, RESOURCE_OPENDJ_OID, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        accountJackOid = assertUserAfter(USER_JACK_OID)
                .fullName()
                .assertOrig(USER_JACK_FULL_NAME_CAPTAIN)
                .assertNoLangs()
                .end()
                .extension()
                .container(TITLE_MAP_QNAME)
                .assertSize(TITLE_EN_SK_RU.length / 2)
                .end()
                .end()
                .singleLink()
                .getOid();

        assertModelShadow(accountJackOid);

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Jack LDAP entry", accountEntry);
        assertCn(accountEntry, USER_JACK_FULL_NAME_CAPTAIN);
        assertDescription(accountEntry, USER_JACK_FULL_NAME_CAPTAIN /* no langs */);
        assertTitle(accountEntry, TITLE_CAPTAIN, TITLE_EN_SK_RU);
    }

    /**
     * Attribute description has two values in LDAP. This is all wrong, because
     * description is a polystring attribute and we do not support multivalue there.
     * But if connector dies on reading this, there is no way how midPoint can figure
     * out what is going on and no way how to fix it. Therefore there is a special mode
     * to allow reduction of multivalues to singlevalue.
     * MID-5275
     *
     * NOTE: The INCOMPLETE flag is currently (temporarily?) not set up, see MID-10168.
     */
    @Test
    public void test152JackMultivalueDescriptionGet() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Let's ruing Jack's description in LDAP.

        Entry accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        assertNotNull("No jack account?", accountEntry);
        openDJController.modifyAdd(accountEntry.getDN().toString(), LDAP_ATTRIBUTE_DESCRIPTION, USER_JACK_BLAHBLAH);

        accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Ruined LDAP entry", accountEntry);

        // precondition
        OpenDJController.assertAttribute(accountEntry, LDAP_ATTRIBUTE_DESCRIPTION,
                USER_JACK_FULL_NAME_CAPTAIN, USER_JACK_BLAHBLAH);

        String accountJackOid = assertUserBefore(USER_JACK_OID)
                .singleLink()
                .getOid();

        // WHEN
        when();
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, accountJackOid, null, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        PolyString descriptionShadowAttribute =
                assertShadow(shadow, "Jack's shadow after read")
                        .attributes()
                            .simpleAttribute(LDAP_ATTRIBUTE_DESCRIPTION)
                                //.assertIncomplete() // see MID-10168
                                .singleValue()
                                    .getRealValueRequired(PolyString.class);
        // @formatter:on

        assertTrue("Unexpected value of description attribute from shadow: " + descriptionShadowAttribute,
                USER_JACK_FULL_NAME_CAPTAIN.equals(descriptionShadowAttribute.getOrig())
                        || USER_JACK_BLAHBLAH.equals(descriptionShadowAttribute.getOrig()));

        accountEntry = getLdapEntryByUid(USER_JACK_USERNAME);
        display("Ruined LDAP entry after", accountEntry);
        OpenDJController.assertAttribute(accountEntry, LDAP_ATTRIBUTE_DESCRIPTION,
                USER_JACK_FULL_NAME_CAPTAIN, USER_JACK_BLAHBLAH);

    }

    /**
     * Make sure there is no shadow for ou=people,dc=example,dc=com.
     * We haven't searched for accounts yet.
     * Therefore there should be just one shadow for jack's account.
     * MID-5544
     */
    @Test
    public void test300Shadows() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceQuery(RESOURCE_OPENDJ_OID);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);

        // THEN
        then();
        assertSuccess(result);

        display("Found shadows", shadows);
        assertEquals("Unexpected number of shadows", 1, shadows.size());
    }

    /**
     * Normal search, all accounts in ou=people, nothing special.
     * MID-5485
     */
    @Test
    public void test310SearchLdapAccounts() throws Exception {
        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);

        // WHEN
        when();
        searchObjectsIterative(ShadowType.class, query, o -> display("Found object", o), 4);
    }

    /**
     * MID-5544
     */
    @Test
    public void test312Shadows() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceQuery(RESOURCE_OPENDJ_OID);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);

        // THEN
        then();
        assertSuccess(result);

        display("Found shadows", shadows);
        // 4 account shadows, 1 shadow for ou=people, 1 shadow for ou=groups,dc=example,dc=com
        assertEquals("Unexpected number of shadows", 6, shadows.size());
        PrismObject<ShadowType> peopleShadow = null;
        for (PrismObject<ShadowType> shadow : shadows) {
            if (StringUtils.equalsIgnoreCase(shadow.getName().getOrig(), OPENDJ_PEOPLE_SUFFIX)) {
                peopleShadow = shadow;
            }
        }
        assertNotNull("No ou=people shadow", peopleShadow);
        assertShadow(peopleShadow, "ou=people shadow")
                .display()
                .assertObjectClass(new QName(MidPointConstants.NS_RI, "organizationalUnit"))
                .assertKind(ShadowKindType.UNKNOWN);
    }

    /**
     * Create an account in sub-ou. If scope is still set to "sub" ten this account will be found.
     * But the scope is overridden in the resource to "one". Therefore this account should be ignored.
     * MID-5485
     */
    @Test
    public void test320SearchLdapAccountsBelow() throws Exception {
        openDJController.addEntry("dn: ou=below,ou=People,dc=example,dc=com\n" +
                "ou: below\n" +
                "objectclass: top\n" +
                "objectclass: organizationalUnit");

        openDJController.addEntry("dn: uid=richard,ou=below,ou=People,dc=example,dc=com\n" +
                "uid: richard\n" +
                "cn: Richard Mayhew\n" +
                "sn: Mayhew\n" +
                "givenname: Richard\n" +
                "objectclass: top\n" +
                "objectclass: person\n" +
                "objectclass: organizationalPerson\n" +
                "objectclass: inetOrgPerson");

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);

        // WHEN
        when();
        searchObjectsIterative(ShadowType.class, query, o -> display("Found object", o), 4);

    }

    /**
     * Normal search, of groups, nothing special. More-or-less configuration sanity check.
     * MID-5790
     */
    @Test
    public void test400SearchLdapProjectGroups() throws Exception {
        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, INTENT_LDAP_PROJECT_GROUP);

        // WHEN
        when();
        // Group "pirates" already exists
        searchObjectsIterative(ShadowType.class, query, o -> display("Found object", o), 1);
    }

    /**
     * Normal search, of groups, nothing special. More-or-less configuration sanity check.
     * MID-5790
     */
    @Test
    public void test401SearchLdapOrgGroups() throws Exception {
        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, INTENT_LDAP_ORG_GROUP);

        // WHEN
        when();
        searchObjectsIterative(ShadowType.class, query, o -> display("Found object", o), 0);
    }

    /**
     * Create a project org. LDAP group should be created.
     */
    @Test
    public void test410CreateProjectKeelhaul() throws Exception {
        PrismObject<OrgType> projectKeelhaul = createObject(OrgType.class, PROJECT_KEELHAUL_NAME);
        projectKeelhaul.asObjectable()
                .beginAssignment()
                .targetRef(ORG_PROJECT_TOP_OID, OrgType.COMPLEX_TYPE);

        // WHEN
        when();
        addObject(projectKeelhaul);

        // THEN
        then();
        PrismObject<OrgType> orgKeelhaul = findObjectByName(OrgType.class, PROJECT_KEELHAUL_NAME);
        projectKeelhaulOid = orgKeelhaul.getOid();
        groupKeelhaulOid = assertOrg(orgKeelhaul, "after")
                .links()
                .singleAny()
                .getOid();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, INTENT_LDAP_PROJECT_GROUP);
        searchObjectsIterative(ShadowType.class, query, o -> display("Found object", o), 2);
    }

    /**
     * Create a project org. LDAP group should be created.
     */
    @Test
    public void test412CreateProjectWalkThePlank() throws Exception {
        PrismObject<OrgType> projectKeelhaul = createObject(OrgType.class, PROJECT_WALK_THE_PLANK_NAME);
        projectKeelhaul.asObjectable()
                .beginAssignment()
                .targetRef(ORG_PROJECT_TOP_OID, OrgType.COMPLEX_TYPE);

        // WHEN
        when();
        addObject(projectKeelhaul);

        // THEN
        then();
        PrismObject<OrgType> orgWalkThePlank = findObjectByName(OrgType.class, PROJECT_WALK_THE_PLANK_NAME);
        projectWalkThePlankOid = orgWalkThePlank.getOid();
        groupWalkThePlankOid = assertOrg(orgWalkThePlank, "after")
                .links()
                .singleAny()
                .getOid();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, INTENT_LDAP_PROJECT_GROUP);
        searchObjectsIterative(ShadowType.class, query, o -> display("Found object", o), 3);

        query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, INTENT_LDAP_ORG_GROUP);
        searchObjectsIterative(ShadowType.class, query, o -> display("Found object", o), 0);
    }

    /**
     * Create a project org. LDAP group should be created.
     */
    @Test
    public void test415CreateOrgRumDepartment() throws Exception {
        PrismObject<OrgType> orgBefore = createObject(OrgType.class, ORG_RUM_DEPARTMENT_NAME);
        orgBefore.asObjectable()
                .beginAssignment()
                .targetRef(ORG_FUNCTIONAL_TOP_OID, OrgType.COMPLEX_TYPE);

        // WHEN
        when();
        addObject(orgBefore);

        // THEN
        then();
        PrismObject<OrgType> orgAfter = findObjectByName(OrgType.class, ORG_RUM_DEPARTMENT_NAME);
        orgRumDepartmentOid = orgAfter.getOid();
        assertNotNull("Null org oid", orgRumDepartmentOid);
        groupRumDepartmentOid = assertOrg(orgAfter, "after")
                .links()
                .singleAny()
                .getOid();

        ObjectQuery query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, INTENT_LDAP_PROJECT_GROUP);
        searchObjectsIterative(ShadowType.class, query, o -> display("Found object", o), 3);

        query = ObjectQueryUtil.createResourceAndKindIntent(RESOURCE_OPENDJ_OID, ShadowKindType.ENTITLEMENT, INTENT_LDAP_ORG_GROUP);
        searchObjectsIterative(ShadowType.class, query, o -> display("Found object", o), 1);
    }

    /**
     * Assign Jack to project. Group association should appear.
     * MID-5790
     */
    @Test
    public void test420AssignJackToKeelhaul() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountJackOid = assertUserBefore(USER_JACK_OID)
                .singleLink()
                .target()
                .assertResource(RESOURCE_OPENDJ_OID)
                .end()
                .getOid();

        // WHEN
        when();
        assignOrg(USER_JACK_OID, projectKeelhaulOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        openDJController.assertUniqueMembers("cn=" + PROJECT_KEELHAUL_NAME + ",ou=groups,dc=example,dc=com", "uid=" + USER_JACK_USERNAME + ",ou=people,dc=example,dc=com");
        openDJController.assertUniqueMembers("cn=" + PROJECT_WALK_THE_PLANK_NAME + ",ou=groups,dc=example,dc=com" /* no value */);
        openDJController.assertUniqueMembers("cn=" + ORG_RUM_DEPARTMENT_NAME + ",ou=orgStruct,dc=example,dc=com" /* no value */);

        assertModelShadow(accountJackOid)
                .associations()
                .association(ASSOCIATION_LDAP_PROJECT_GROUP)
                .assertShadowOids(groupKeelhaulOid)
                .end()
                .association(ASSOCIATION_LDAP_ORG_GROUP)
                // MID-5790
                .assertNone();
    }

    /**
     * Assign Jack to another project. Another group association should appear.
     * MID-5790
     */
    @Test
    public void test422AssignJackToWalkThePlank() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountJackOid = assertUserBefore(USER_JACK_OID)
                .singleLink()
                .target()
                .assertResource(RESOURCE_OPENDJ_OID)
                .end()
                .getOid();

        // WHEN
        when();
        assignOrg(USER_JACK_OID, projectWalkThePlankOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        openDJController.assertUniqueMembers("cn=" + PROJECT_KEELHAUL_NAME + ",ou=groups,dc=example,dc=com", "uid=" + USER_JACK_USERNAME + ",ou=people,dc=example,dc=com");
        openDJController.assertUniqueMembers("cn=" + PROJECT_WALK_THE_PLANK_NAME + ",ou=groups,dc=example,dc=com", "uid=" + USER_JACK_USERNAME + ",ou=people,dc=example,dc=com");
        openDJController.assertUniqueMembers("cn=" + ORG_RUM_DEPARTMENT_NAME + ",ou=orgStruct,dc=example,dc=com" /* no value */);

        assertModelShadow(accountJackOid)
                .associations()
                .association(ASSOCIATION_LDAP_PROJECT_GROUP)
                .assertShadowOids(groupKeelhaulOid, groupWalkThePlankOid)
                .end()
                .association(ASSOCIATION_LDAP_ORG_GROUP)
                // MID-5790
                .assertNone();
    }

    /**
     * Same routine, but with org.
     * MID-5790
     */
    @Test
    public void test424AssignJackToRumDepartment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String accountJackOid = assertUserBefore(USER_JACK_OID)
                .singleLink()
                .target()
                .assertResource(RESOURCE_OPENDJ_OID)
                .end()
                .getOid();

        // WHEN
        when();
        assignOrg(USER_JACK_OID, orgRumDepartmentOid, task, result);

        // THEN
        then();
        assertSuccess(result);

        openDJController.assertUniqueMembers("cn=" + PROJECT_KEELHAUL_NAME + ",ou=groups,dc=example,dc=com", "uid=" + USER_JACK_USERNAME + ",ou=people,dc=example,dc=com");
        openDJController.assertUniqueMembers("cn=" + PROJECT_WALK_THE_PLANK_NAME + ",ou=groups,dc=example,dc=com", "uid=" + USER_JACK_USERNAME + ",ou=people,dc=example,dc=com");
        openDJController.assertUniqueMembers("cn=" + ORG_RUM_DEPARTMENT_NAME + ",ou=orgStruct,dc=example,dc=com", "uid=" + USER_JACK_USERNAME + ",ou=people,dc=example,dc=com");

        assertModelShadow(accountJackOid)
                .associations()
                .association(ASSOCIATION_LDAP_PROJECT_GROUP)
                .assertShadowOids(groupKeelhaulOid, groupWalkThePlankOid)
                .end()
                .association(ASSOCIATION_LDAP_ORG_GROUP)
                // MID-5790
                .assertShadowOids(groupRumDepartmentOid);
    }

    private List<PrismContainerValue<?>> createTitleMapValues(String... params) throws SchemaException {
        List<PrismContainerValue<?>> cvals = new ArrayList<>();
        for (int i = 0; i < params.length; i += 2) {
            PrismContainerValue<?> cval = prismContext.itemFactory().createContainerValue();

            PrismProperty<String> keyProp = prismContext.itemFactory().createProperty(TITLE_MAP_KEY_QNAME);
            keyProp.setRealValue(params[i]);
            cval.add(keyProp);

            PrismProperty<String> valueProp = prismContext.itemFactory().createProperty(TITLE_MAP_VALUE_QNAME);
            valueProp.setRealValue(params[i + 1]);
            cval.add(valueProp);

            cvals.add(cval);
        }
        return cvals;
    }

    private void assertDescription(Entry entry, String expectedOrigValue, String... params) {
        OpenDJController.assertAttributeLang(entry, LDAP_ATTRIBUTE_DESCRIPTION, expectedOrigValue, params);
    }

    private void assertTitle(Entry entry, String expectedOrigValue, String... params) {
        OpenDJController.assertAttributeLang(entry, "title", expectedOrigValue, params);
    }

}
