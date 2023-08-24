/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.multi;

import java.io.File;
import java.util.Collections;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.MidPointConstants;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.INTENT_DEFAULT;

/**
 * Test multiple accounts with the same resource+kind+intent.
 *
 * MID-3542
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMultiAccount extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/multi-account");

    // Green dummy resource is authoritative. This version supports multiaccounts.
    private static final File RESOURCE_DUMMY_MULTI_GREEN_FILE = new File(TEST_DIR, "resource-dummy-multi-green.xml");
    private static final String RESOURCE_DUMMY_MULTI_GREEN_OID = "128469e0-6759-11e9-8520-db9fa0f25495";
    private static final String RESOURCE_DUMMY_MULTI_GREEN_NAME = "multi-green";

    // Clever HR resource. It has one account for every work contract. One of these contracts is the primary one.
    private static final File RESOURCE_DUMMY_CLEVER_HR_FILE = new File(TEST_DIR, "resource-dummy-clever-hr.xml");
    private static final String RESOURCE_DUMMY_CLEVER_HR_OID = "4b20aab4-99d2-11ea-b0ae-bfae68238f94";
    private static final String RESOURCE_DUMMY_CLEVER_HR_NAME = "clever-hr";

    private static final String CLEVER_HR_ATTRIBUTE_FIRST_NAME = "firstName";
    private static final String CLEVER_HR_ATTRIBUTE_LAST_NAME = "lastName";
    private static final String CLEVER_HR_ATTRIBUTE_PERSONAL_NUMBER = "personalNumber";
    private static final String CLEVER_HR_ATTRIBUTE_PRIMARY = "primary";
    private static final String CLEVER_HR_ATTRIBUTE_LOCATION = "location";
    private static final String CLEVER_HR_ATTRIBUTE_OU = "ou";

    // Multi outbound dummy resource, target with multiaccounts.
    private static final File RESOURCE_DUMMY_MULTI_OUTBOUND_FILE = new File(TEST_DIR, "resource-dummy-multi-outbound.xml");
    private static final String RESOURCE_DUMMY_MULTI_OUTBOUND_OID = "d4da475e-8539-11ea-8343-dfdb4091c1dc";
    private static final String RESOURCE_DUMMY_MULTI_OUTBOUND_NAME = "multi-outbound";

    private static final File RESOURCE_DUMMY_MULTI_OUTBOUND_SIMPLE_FILE = new File(TEST_DIR, "resource-dummy-multi-outbound-simple.xml");
    private static final String RESOURCE_DUMMY_MULTI_OUTBOUND_SIMPLE_OID = "e08cb619-fe83-487a-83cc-10a50d19c947";
    private static final String RESOURCE_DUMMY_MULTI_OUTBOUND_SIMPLE_NAME = "multi-outbound-simple";

    private static final String USER_IDAHO_GIVEN_NAME = "Duncan";
    private static final String USER_IDAHO_FAMILY_NAME = "Idaho";
    private static final String USER_IDAHO_NAME = "idaho";

    private static final String ACCOUNT_PAUL_ATREIDES_ID = "001";

    private static final String ACCOUNT_PAUL_ATREIDES_USERNAME = "paul";
    private static final String ACCOUNT_PAUL_ATREIDES_FULL_NAME = "Paul Atreides";

    private static final String ACCOUNT_MUAD_DIB_USERNAME = "muaddib";
    private static final String ACCOUNT_MUAD_DIB_FULL_NAME = "Muad'Dib";

    private static final String ACCOUNT_DUKE_USERNAME = "duke";
    private static final String ACCOUNT_DUKE_FULL_NAME = "Duke Paul Atreides";
    private static final String ACCOUNT_DUKE_TITLE = "duke";

    private static final String ACCOUNT_MAHDI_USERNAME = "mahdi";
    private static final String ACCOUNT_MAHDI_FULL_NAME = "Mahdi Muad'Dib";
    private static final String ACCOUNT_MAHDI_TITLE = "mahdi";

    private static final String USER_ODRADE_USERNAME = "odrade";
    private static final String ACCOUNT_ODRADE_FIRST_NAME = "Darwi";
    private static final String ACCOUNT_ODRADE_LAST_NAME = "Odrade";
    private static final String ACCOUNT_ODRADE_PERSONAL_NUMBER = "54321";
    private static final String ACCOUNT_ODRADE_CONTRACT_NUMBER_APPRENTICE = "A007";
    private static final String ACCOUNT_ODRADE_CONTRACT_NUMBER_GUARDIAN = "G007";
    private static final String ACCOUNT_ODRADE_CONTRACT_NUMBER_MOTHER_SUPERIOR = "MS007";
    private static final String OU_MOTHER_SCHOOL = "Mother School";
    private static final String OU_MOTHER_SUPERIOR_OFFICE = "Mother Superior Office";
    private static final String OU_SECURITY = "Security";

    private static final String INTENT_ADMIN = "admin";
    private static final String INTENT_ENVOY = "envoy";

    private static final String PLANET_CALADAN = "Caladan";
    private static final String PLANET_KAITAIN = "Kaitain";
    private static final String PLANET_IX = "Ix";
    private static final String PLANET_GINAZ = "Ginaz";
    private static final String PLANET_WALLACH_IX = "Wallach IX";
    private static final String PLANET_CHAPTERHOUSE = "Chapterhouse";
    private static final String PLANET_ARRAKIS = "Arrakis";

    private static final String ACCOUNT_IDAHO_ENVOY_IX_USERNAME = "envoy-idaho-ix";

    private String accountPaulOid;
    private String accountMuaddibOid;
    private String accountDukeOid;
    private String userIdahoOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_MULTI_GREEN_NAME,
                RESOURCE_DUMMY_MULTI_GREEN_FILE, RESOURCE_DUMMY_MULTI_GREEN_OID, initTask, initResult);

        initDummyResource(RESOURCE_DUMMY_CLEVER_HR_NAME,
                RESOURCE_DUMMY_CLEVER_HR_FILE, RESOURCE_DUMMY_CLEVER_HR_OID,
                controller -> {
                    controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                            CLEVER_HR_ATTRIBUTE_FIRST_NAME, String.class, false, false);
                    controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                            CLEVER_HR_ATTRIBUTE_LAST_NAME, String.class, true, false);
                    controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                            CLEVER_HR_ATTRIBUTE_PERSONAL_NUMBER, String.class, true, false);
                    controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                            CLEVER_HR_ATTRIBUTE_PRIMARY, Boolean.class, false, false);
                    controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                            CLEVER_HR_ATTRIBUTE_LOCATION, String.class, false, false);
                    controller.addAttrDef(controller.getDummyResource().getAccountObjectClass(),
                            CLEVER_HR_ATTRIBUTE_OU, String.class, false, false);
                },
                initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_MULTI_OUTBOUND_NAME,
                RESOURCE_DUMMY_MULTI_OUTBOUND_FILE, RESOURCE_DUMMY_MULTI_OUTBOUND_OID, initTask, initResult);
        initDummyResourcePirate(RESOURCE_DUMMY_MULTI_OUTBOUND_SIMPLE_NAME,
                RESOURCE_DUMMY_MULTI_OUTBOUND_SIMPLE_FILE, RESOURCE_DUMMY_MULTI_OUTBOUND_SIMPLE_OID, initTask, initResult);
    }

    /**
     * Mostly just sanity. Make sure that "empty" import works that that the multigreen
     * resource configuration is sane.
     */
    @Test
    public void test010ImportAccountsFromDummyMultiGreen() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Preconditions
        assertUsers(getNumberOfUsers());

        // WHEN
        when();
        importMultiGreenAccounts(task, result);

        // THEN
        then();

        // No accounts on multigreen resource yet. No users should be created.
        assertUsers(getNumberOfUsers());
    }

    /**
     * Sanity/preparation. Import absolutely ordinary account.
     */
    @Test
    public void test020ImportPaulAtreides() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = new DummyAccount(ACCOUNT_PAUL_ATREIDES_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_PAUL_ATREIDES_FULL_NAME);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ACCOUNT_PAUL_ATREIDES_ID);
//        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, TITLE_DUKE);
        getDummyResource(RESOURCE_DUMMY_MULTI_GREEN_NAME).addAccount(account);

        // Preconditions
        assertUsers(getNumberOfUsers());

        // WHEN
        when();
        importMultiGreenAccounts(task, result);

        // THEN
        then();

        // @formatter:off
        accountPaulOid = assertUserAfterByUsername(ACCOUNT_PAUL_ATREIDES_USERNAME)
                .displayWithProjections()
                .assertFullName(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
                .assertEmployeeNumber(ACCOUNT_PAUL_ATREIDES_ID)
                .assertOrganizationalUnits(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
                .singleLink()
                .resolveTarget()
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertIntent(INTENT_DEFAULT)
                    .assertTagIsOid()
                    .getOid();
        // @formatter:on

        assertUsers(getNumberOfUsers() + 1);
    }

    /**
     * Import another account that correlates to Paul. This has the same resource+kind+intent.
     */
    @Test
    public void test100ImportMuadDib() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = new DummyAccount(ACCOUNT_MUAD_DIB_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_MUAD_DIB_FULL_NAME);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ACCOUNT_PAUL_ATREIDES_ID);
        getDummyResource(RESOURCE_DUMMY_MULTI_GREEN_NAME).addAccount(account);

        // Preconditions
        assertUsers(getNumberOfUsers() + 1);

        // WHEN
        when();
        importMultiGreenAccounts(task, result);

        // THEN
        then();

        // @formatter:off
        accountMuaddibOid = assertUserAfterByUsername(ACCOUNT_PAUL_ATREIDES_USERNAME)
                .displayWithProjections()
                .assertFullName(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
                .assertEmployeeNumber(ACCOUNT_PAUL_ATREIDES_ID)
                .assertOrganizationalUnits(ACCOUNT_PAUL_ATREIDES_FULL_NAME, ACCOUNT_MUAD_DIB_FULL_NAME)
                .links()
                    .assertLiveLinks(2)
                    .link(accountPaulOid)
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .assertTag(accountPaulOid)
                            .end()
                        .end()
                    .by().notTags(accountPaulOid).find()
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .assertTagIsOid()
                            .getOid();
        // @formatter:on

        assertUsers(getNumberOfUsers() + 1);
    }

    @Test
    public void test102ReconcileUserPaul() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        String userPaulOid = findUserByUsername(ACCOUNT_PAUL_ATREIDES_USERNAME).getOid();

        // WHEN
        when();
        reconcileUser(userPaulOid, task, result);

        // THEN
        then();

        // @formatter:off
        accountMuaddibOid = assertUserAfter(userPaulOid)
                .displayWithProjections()
                .assertFullName(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
                .assertEmployeeNumber(ACCOUNT_PAUL_ATREIDES_ID)
                // TODO
                .assertOrganizationalUnits(ACCOUNT_PAUL_ATREIDES_FULL_NAME, ACCOUNT_MUAD_DIB_FULL_NAME)
                .links()
                    .assertLiveLinks(2)
                    .link(accountPaulOid)
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .assertTag(accountPaulOid)
                            .end()
                        .end()
                    .by().notTags(accountPaulOid).find()
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .assertTagIsOid()
                            .getOid();
        // @formatter:on

        assertUsers(getNumberOfUsers() + 1);
    }

    /**
     * Import another account that correlates to Paul. This has the same resource+kind+intent.
     * But this is an admin account (title=duke). Therefore it will have different intent.
     * And there is a custom tag expression.
     */
    @Test
    public void test200ImportDuke() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = new DummyAccount(ACCOUNT_DUKE_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_DUKE_FULL_NAME);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ACCOUNT_PAUL_ATREIDES_ID);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ACCOUNT_DUKE_TITLE);
        getDummyResource(RESOURCE_DUMMY_MULTI_GREEN_NAME).addAccount(account);

        // Preconditions
        assertUsers(getNumberOfUsers() + 1);

        // WHEN
        when();
        importMultiGreenAccounts(task, result);

        // THEN
        then();

        // @formatter:off
        accountDukeOid = assertUserAfterByUsername(ACCOUNT_PAUL_ATREIDES_USERNAME)
                .displayWithProjections()
                .assertFullName(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
                .assertEmployeeNumber(ACCOUNT_PAUL_ATREIDES_ID)
                // TODO
//            .assertOrganizationalUnits(ACCOUNT_PAUL_ATREIDES_FULL_NAME, ACCOUNT_MUAD_DIB_FULL_NAME)
                .links()
                    .assertLiveLinks(3)
                    .link(accountPaulOid)
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .assertTag(accountPaulOid)
                        .end()
                    .end()
                    .link(accountMuaddibOid)
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .assertTag(accountMuaddibOid)
                        .end()
                    .end()
                    .by().notTags(accountPaulOid, accountMuaddibOid).find()
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_ADMIN)
                            .assertTag(ACCOUNT_DUKE_TITLE)
                            .getOid();
        // @formatter:on

        assertUsers(getNumberOfUsers() + 1);
    }

    /**
     * Import yet another admin account that correlates to Paul.
     */
    @Test
    public void test210ImportMahdi() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = new DummyAccount(ACCOUNT_MAHDI_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_MAHDI_FULL_NAME);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ACCOUNT_PAUL_ATREIDES_ID);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, ACCOUNT_MAHDI_TITLE);
        getDummyResource(RESOURCE_DUMMY_MULTI_GREEN_NAME).addAccount(account);

        // Preconditions
        assertUsers(getNumberOfUsers() + 1);

        // WHEN
        when();
        importMultiGreenAccounts(task, result);

        // THEN
        then();

        // @formatter:off
        assertUserAfterByUsername(ACCOUNT_PAUL_ATREIDES_USERNAME)
                .displayWithProjections()
                .assertFullName(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
                .assertEmployeeNumber(ACCOUNT_PAUL_ATREIDES_ID)
                // TODO
//            .assertOrganizationalUnits(ACCOUNT_PAUL_ATREIDES_FULL_NAME, ACCOUNT_MUAD_DIB_FULL_NAME)
                .links()
                    .assertLiveLinks(4)
                    .link(accountPaulOid)
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .assertTag(accountPaulOid)
                            .end()
                        .end()
                    .link(accountMuaddibOid)
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .assertTag(accountMuaddibOid)
                            .end()
                        .end()
                    .link(accountDukeOid)
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_ADMIN)
                            .assertTag(ACCOUNT_DUKE_TITLE)
                            .end()
                        .end()
                    .by().notTags(accountPaulOid, accountMuaddibOid, ACCOUNT_DUKE_TITLE).find()
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_ADMIN)
                            .assertTag(ACCOUNT_MAHDI_TITLE)
                            .getOid();
        // @formatter:on

        assertUsers(getNumberOfUsers() + 1);
    }

    /**
     * Create Duncan Idaho user, assign default account on outbound resource, make sure that the usual use case works.
     * MID-6242
     */
    @Test
    public void test300CreateIdaho() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> useBefore = createUser(USER_IDAHO_NAME, USER_IDAHO_GIVEN_NAME, USER_IDAHO_FAMILY_NAME, true);
        addObject(useBefore);
        userIdahoOid = useBefore.getOid();

        // Preconditions
        assertUsers(getNumberOfUsers() + 2);

        // WHEN
        when();
        assignAccountToUser(userIdahoOid, RESOURCE_DUMMY_MULTI_OUTBOUND_OID, INTENT_DEFAULT, task, result);

        // THEN
        then();
        assertSuccess(result);

        // @formatter:off
        assertUserAfter(userIdahoOid)
                .singleLink()
                    .target()
                        .assertResource(RESOURCE_DUMMY_MULTI_OUTBOUND_OID)
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(INTENT_DEFAULT);
                        //.assertTagIsOid();
        // @formatter:on

        assertUsers(getNumberOfUsers() + 2);
    }

    /**
     * Add Caladan and Kaitain organizations to Duncan Idaho.
     * Appropriate Envoy accounts should be created.
     *
     * MID-6242
     */
    @Test
    public void test310IdahoEnvoyCaladanKaitain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ORGANIZATION).add(createPolyString(PLANET_CALADAN), createPolyString(PLANET_KAITAIN))
                        .asObjectDelta(userIdahoOid),
                null, task , result);


        // Preconditions
        assertUsers(getNumberOfUsers() + 2);

        // WHEN
        when();
        assignAccountToUser(userIdahoOid, RESOURCE_DUMMY_MULTI_OUTBOUND_OID, INTENT_ENVOY, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertEnvoyAccounts(userIdahoOid, USER_IDAHO_NAME, PLANET_CALADAN, PLANET_KAITAIN);

        assertUsers(getNumberOfUsers() + 2);
    }

    /**
     * Remove Kaitain from Idaho's organization. His Kaitain envoy account should be deleted.
     *
     * MID-6242
     */
    @Test
    public void test320IdahoEnvoyRemoveKaitain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Preconditions
        assertUsers(getNumberOfUsers() + 2);

        // WHEN
        when();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ORGANIZATION).delete(createPolyString(PLANET_KAITAIN))
                        .asObjectDelta(userIdahoOid),
                null, task , result);

        // THEN
        then();
        assertSuccess(result);

        assertEnvoyAccounts(userIdahoOid, USER_IDAHO_NAME, PLANET_CALADAN);

        assertUsers(getNumberOfUsers() + 2);
    }

    /**
     * Add Ix and Ginaz to Idaho's organization. New envoy accounts should be created.
     *
     * MID-6242
     */
    @Test
    public void test330IdahoEnvoyAddIxGinaz() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Preconditions
        assertUsers(getNumberOfUsers() + 2);

        // WHEN
        when();
        executeChanges(
                deltaFor(UserType.class)
                        .item(UserType.F_ORGANIZATION).add(createPolyString(PLANET_IX), createPolyString(PLANET_GINAZ))
                        .asObjectDelta(userIdahoOid),
                null, task , result);

        // THEN
        then();
        assertSuccess(result);

        assertEnvoyAccounts(userIdahoOid, USER_IDAHO_NAME, PLANET_CALADAN, PLANET_IX, PLANET_GINAZ);

        assertUsers(getNumberOfUsers() + 2);
        displayDumpable("Outbound dummy resource", getDummyResource(RESOURCE_DUMMY_MULTI_OUTBOUND_NAME));
    }

    /**
     * Unassign Envoy account from Idaho. All resource accounts should be deleted.
     *
     * MID-6242
     */
    @Test
    public void test340IdahoUnassignOutboundMultiaccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Preconditions
        assertUsers(getNumberOfUsers() + 2);

        // WHEN
        when();
        unassignAccountFromUser(userIdahoOid, RESOURCE_DUMMY_MULTI_OUTBOUND_OID, INTENT_ENVOY, task, result);

        // THEN
        then();
        assertSuccess(result);

        assertEnvoyAccounts(userIdahoOid, USER_IDAHO_NAME /* no values */);

        assertUsers(getNumberOfUsers() + 2);
    }

    /**
     * Assign Envoy account back to Idaho. Resource accounts corresponding to his organizations should be re-created.
     * Moreover, envoy-idaho-ix is created "from the side", so it will be in fact discovered. See MID-6796.
     */
    @Test
    public void test350IdahoAssignOutboundMultiaccount() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = new DummyAccount(ACCOUNT_IDAHO_ENVOY_IX_USERNAME);
        account.setEnabled(true);
        getDummyResource(RESOURCE_DUMMY_MULTI_OUTBOUND_NAME).addAccount(account);

        // Preconditions
        assertUsers(getNumberOfUsers() + 2);

        // WHEN
        when();
        assignAccountToUser(userIdahoOid, RESOURCE_DUMMY_MULTI_OUTBOUND_OID, INTENT_ENVOY, task, result);

        // THEN
        then();
        assertSuccess(result, 5); // There is discovery related fatal error deep inside

        // Ix is lowercased, because it was tagged during discovery so the tag is derived from icfs:name (envoy-idaho-ix).
        assertEnvoyAccounts(userIdahoOid, USER_IDAHO_NAME, PLANET_CALADAN, PLANET_IX.toLowerCase(), PLANET_GINAZ);

        assertUsers(getNumberOfUsers() + 2);
    }

    /**
     * Mostly just sanity. Make sure that "empty" import works and that the clever HR
     * resource configuration is sane.
     *
     * MID-6080
     */
    @Test
    public void test400ImportAccountsFromCleverHr() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // Preconditions
        assertUsers(getNumberOfUsers() + 2);

        // WHEN
        when();
        importCleverHrAccounts(task, result);

        // THEN
        then();

        // No accounts on HR resource yet. No users should be created.
        assertUsers(getNumberOfUsers() + 2);
    }

    /**
     * Import the first account. Nothing special here yet.
     */
    @Test
    public void test410ImportOdradeApprentice() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = new DummyAccount(ACCOUNT_ODRADE_CONTRACT_NUMBER_APPRENTICE);
        account.setEnabled(true);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_FIRST_NAME, ACCOUNT_ODRADE_FIRST_NAME);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_LAST_NAME, ACCOUNT_ODRADE_LAST_NAME);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_PERSONAL_NUMBER, ACCOUNT_ODRADE_PERSONAL_NUMBER);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_LOCATION, PLANET_WALLACH_IX);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_PRIMARY, Collections.singleton(Boolean.TRUE));
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_OU, OU_MOTHER_SCHOOL);
        getDummyResource(RESOURCE_DUMMY_CLEVER_HR_NAME).addAccount(account);

        // Preconditions
        assertUsers(getNumberOfUsers() + 2);

        // WHEN
        when();
        importCleverHrAccounts(task, result);

        // THEN
        then();

        assertUserAfterByUsername(USER_ODRADE_USERNAME)
                .displayWithProjections()
                .assertGivenName(ACCOUNT_ODRADE_FIRST_NAME)
                .assertFamilyName(ACCOUNT_ODRADE_LAST_NAME)
                .assertEmployeeNumber(ACCOUNT_ODRADE_CONTRACT_NUMBER_APPRENTICE)
                .assertLocality(PLANET_WALLACH_IX)
                .assertOrganizationalUnits(OU_MOTHER_SCHOOL)
                .singleLink()
                    .resolveTarget()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(INTENT_DEFAULT)
                        .assertTag(ACCOUNT_ODRADE_CONTRACT_NUMBER_APPRENTICE);

        assertUsers(getNumberOfUsers() + 3);

    }

    /**
     * Import the second account. It should correlate to Odrade user as well.
     * However, this contract is NOT primary. Therefore employee number and location should NOT be changed.
     *
     * MID-6080
     */
    @Test
    public void test420ImportOdradeGuardian() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        DummyAccount account = new DummyAccount(ACCOUNT_ODRADE_CONTRACT_NUMBER_GUARDIAN);
        account.setEnabled(true);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_FIRST_NAME, ACCOUNT_ODRADE_FIRST_NAME);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_LAST_NAME, ACCOUNT_ODRADE_LAST_NAME);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_PERSONAL_NUMBER, ACCOUNT_ODRADE_PERSONAL_NUMBER);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_LOCATION, PLANET_ARRAKIS);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_PRIMARY, Collections.singleton(Boolean.FALSE));
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_OU, OU_SECURITY);
        getDummyResource(RESOURCE_DUMMY_CLEVER_HR_NAME).addAccount(account);

        // Preconditions
        assertUsers(getNumberOfUsers() + 3);

        // WHEN
        when();
        importCleverHrAccounts(task, result);

        // THEN
        then();

        assertUserAfterByUsername(USER_ODRADE_USERNAME)
                .displayWithProjections()
                .assertGivenName(ACCOUNT_ODRADE_FIRST_NAME)
                .assertFamilyName(ACCOUNT_ODRADE_LAST_NAME)
                .assertLocality(PLANET_WALLACH_IX)
                .assertEmployeeNumber(ACCOUNT_ODRADE_CONTRACT_NUMBER_APPRENTICE)
                .assertOrganizationalUnits(OU_MOTHER_SCHOOL, OU_SECURITY)
                .links()
                    .assertLiveLinks(2)
                    .by()
                        .tag(ACCOUNT_ODRADE_CONTRACT_NUMBER_APPRENTICE)
                    .find()
                        .resolveTarget()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .end()
                        .end()
                    .by()
                        .tag(ACCOUNT_ODRADE_CONTRACT_NUMBER_GUARDIAN)
                    .find()
                        .resolveTarget()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .end()
                        .end();

        assertUsers(getNumberOfUsers() + 3);
    }

    /**
     * Promote Odrade to Mother Superior. The primary contract is changed in this case.
     * User object should reflect data from the new primary contract.
     */
    @Test
    public void test430ImportOdradeMotherSuperior() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        getDummyResource(RESOURCE_DUMMY_CLEVER_HR_NAME)
                .getAccountByUsername(ACCOUNT_ODRADE_CONTRACT_NUMBER_APPRENTICE)
                    .replaceAttributeValue(CLEVER_HR_ATTRIBUTE_PRIMARY, Boolean.FALSE);

        DummyAccount account = new DummyAccount(ACCOUNT_ODRADE_CONTRACT_NUMBER_MOTHER_SUPERIOR);
        account.setEnabled(true);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_FIRST_NAME, ACCOUNT_ODRADE_FIRST_NAME);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_LAST_NAME, ACCOUNT_ODRADE_LAST_NAME);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_PERSONAL_NUMBER, ACCOUNT_ODRADE_PERSONAL_NUMBER);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_LOCATION, PLANET_CHAPTERHOUSE);
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_PRIMARY, Collections.singleton(Boolean.TRUE));
        account.addAttributeValues(CLEVER_HR_ATTRIBUTE_OU, OU_MOTHER_SUPERIOR_OFFICE);
        getDummyResource(RESOURCE_DUMMY_CLEVER_HR_NAME).addAccount(account);

        // Preconditions
        assertUsers(getNumberOfUsers() + 3);

        // WHEN
        when();
        importCleverHrAccounts(task, result);

        // THEN
        then();

        assertUserAfterByUsername(USER_ODRADE_USERNAME)
                .displayWithProjections()
                .assertGivenName(ACCOUNT_ODRADE_FIRST_NAME)
                .assertFamilyName(ACCOUNT_ODRADE_LAST_NAME)
                .assertLocality(PLANET_CHAPTERHOUSE)
                .assertEmployeeNumber(ACCOUNT_ODRADE_CONTRACT_NUMBER_MOTHER_SUPERIOR)
                .assertOrganizationalUnits(OU_MOTHER_SUPERIOR_OFFICE, OU_MOTHER_SCHOOL, OU_SECURITY)
                .links()
                    .assertLiveLinks(3)
                    .by()
                        .tag(ACCOUNT_ODRADE_CONTRACT_NUMBER_APPRENTICE)
                    .find()
                        .resolveTarget()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .end()
                        .end()
                    .by()
                        .tag(ACCOUNT_ODRADE_CONTRACT_NUMBER_GUARDIAN)
                    .find()
                        .resolveTarget()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .end()
                        .end()
                .by()
                        .tag(ACCOUNT_ODRADE_CONTRACT_NUMBER_MOTHER_SUPERIOR)
                    .find()
                        .resolveTarget()
                            .assertKind(ShadowKindType.ACCOUNT)
                            .assertIntent(INTENT_DEFAULT)
                            .end()
                        .end();

        assertUsers(getNumberOfUsers() + 3);
    }

    /**
     * MID-6899
     */
    @Test
    public void test500MultiSimple() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        UserType user = new UserType(prismContext)
                .name("test500")
                .organization("org1");
        addObject(user, task, result);

        when();

        assignAccountToUser(user.getOid(), RESOURCE_DUMMY_MULTI_OUTBOUND_SIMPLE_OID, INTENT_DEFAULT, task, result);

        then();

        assertUserAfter(user.getOid())
                .links()
                    .singleLive()
                        .resolveTarget()
                            .display()
                            .assertName("test500-org1");
    }

    @SuppressWarnings("SameParameterValue")
    private void assertEnvoyAccounts(String userOid, String username, String... planets) throws Exception {
        UserAsserter<Void> asserter = assertUserAfter(userOid)
            .displayWithProjections()
            .links()
                .assertLiveLinks(planets.length + 1)
                .by()
                    .resourceOid(RESOURCE_DUMMY_MULTI_OUTBOUND_OID)
                    .intent(INTENT_DEFAULT)
                .find()
                    .target()
                        .assertResource(RESOURCE_DUMMY_MULTI_OUTBOUND_OID)
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(INTENT_DEFAULT)
                    .end()
                .end()
            .end();

        asserter
            .links()
                .by()
                    .resourceOid(RESOURCE_DUMMY_MULTI_OUTBOUND_OID)
                    .intent(INTENT_ENVOY)
                    .assertCount(planets.length);

        for (String planet : planets) {
            asserter
                .links()
                    .by()
                        .resourceOid(RESOURCE_DUMMY_MULTI_OUTBOUND_OID)
                        .intent(INTENT_ENVOY)
                        .tag(planet)
                    .find()
                        .target()
                            .assertName(getEnvoy(username,planet));

                assertDummyAccountByUsername(RESOURCE_DUMMY_MULTI_OUTBOUND_NAME, getEnvoy(username,planet))
                    .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, planet);
            }
    }

    private void importMultiGreenAccounts(Task task, OperationResult result) throws Exception {
        getDummyResourceController(RESOURCE_DUMMY_MULTI_GREEN_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_MULTI_GREEN_OID, new QName(MidPointConstants.NS_RI, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), task, result);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        waitForTaskFinish(task, 40000);
    }

    private void importCleverHrAccounts(Task task, OperationResult result) throws Exception {
        getDummyResourceController(RESOURCE_DUMMY_CLEVER_HR_NAME);
        modelService.importFromResource(RESOURCE_DUMMY_CLEVER_HR_OID, new QName(MidPointConstants.NS_RI, SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), task, result);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        waitForTaskFinish(task, 40000);
    }

    private String getEnvoy(String username, String planet) {
        return "envoy-" + username + "-" + planet.toLowerCase();
    }
}
