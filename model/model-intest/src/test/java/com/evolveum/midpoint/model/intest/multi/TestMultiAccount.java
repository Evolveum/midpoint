/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.multi;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;

import java.io.File;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.model.intest.sync.AbstractSynchronizationStoryTest;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Test multiple accounts with the same resource+kind+intent.
 *
 * MID-3542
 *
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestMultiAccount extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/multi-account");

    // Green dummy resource is authoritative. This version supports multiaccounts.
    protected static final File RESOURCE_DUMMY_MULTI_GREEN_FILE = new File(TEST_DIR, "resource-dummy-multi-green.xml");
    protected static final String RESOURCE_DUMMY_MULTI_GREEN_OID = "128469e0-6759-11e9-8520-db9fa0f25495";
    protected static final String RESOURCE_DUMMY_MULTI_GREEN_NAME = "multi-green";
    protected static final String RESOURCE_DUMMY_MULTI_GREEN_NAMESPACE = MidPointConstants.NS_RI;

    protected static final String ACCOUNT_PAUL_ATREIDES_ID = "001";

    protected static final String ACCOUNT_PAUL_ATREIDES_USERNAME = "paul";
    protected static final String ACCOUNT_PAUL_ATREIDES_FULL_NAME = "Paul Atreides";

    protected static final String ACCOUNT_MUAD_DIB_USERNAME = "muaddib";
    protected static final String ACCOUNT_MUAD_DIB_FULL_NAME = "Muad'Dib";

    protected static final String ACCOUNT_DUKE_USERNAME = "duke";
    protected static final String ACCOUNT_DUKE_FULL_NAME = "Duke Paul Atreides";
    protected static final String ACCOUNT_DUKE_TITLE = "duke";

    protected static final String ACCOUNT_MAHDI_USERNAME = "mahdi";
    protected static final String ACCOUNT_MAHDI_FULL_NAME = "Mahdi Muad'Dib";
    protected static final String ACCOUNT_MAHDI_TITLE = "mahdi";

    private static final String INTENT_ADMIN = "admin";


    private String accountPaulOid;
    private String accountMuaddibOid;
    private String accountDukeOid;

    private String accountMahdiOid;

    private String userPaulOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initDummyResourcePirate(RESOURCE_DUMMY_MULTI_GREEN_NAME,
                RESOURCE_DUMMY_MULTI_GREEN_FILE, RESOURCE_DUMMY_MULTI_GREEN_OID, initTask, initResult);
    }

    /**
     * Mostly just sanity. Make sure that "empty" import works that that the multigreen
     * resource configuration is sane.
     */
    @Test
    public void test010ImportAccountsFromDummyMultiGreen() throws Exception {
        final String TEST_NAME = "test010ImportAccountsFromDummyMultiGreen";

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // Preconditions
        assertUsers(getNumberOfUsers());

        // WHEN
        displayWhen(TEST_NAME);
        importMultiGreenAccounts(task, result);

        // THEN
        displayThen(TEST_NAME);

        // No accounts on multigreen resource yet. No users should be created.
        assertUsers(getNumberOfUsers());
    }

    /**
     * Sanity/preparation. Import absolutely ordinary account.
     */
    @Test
    public void test020ImportPaulAtreides() throws Exception {
        final String TEST_NAME = "test020ImportPaulAtreides";

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
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
        displayWhen(TEST_NAME);
        importMultiGreenAccounts(task, result);

        // THEN
        displayThen(TEST_NAME);

        accountPaulOid = assertUserAfterByUsername(ACCOUNT_PAUL_ATREIDES_USERNAME)
            .displayWithProjections()
            .assertFullName(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
            .assertEmployeeNumber(ACCOUNT_PAUL_ATREIDES_ID)
            .assertOrganizationalUnits(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
            .singleLink()
                .resolveTarget()
                    .assertKind(ShadowKindType.ACCOUNT)
                    .assertIntent(SchemaConstants.INTENT_DEFAULT)
                    .assertTagIsOid()
                    .getOid();

        assertUsers(getNumberOfUsers() + 1);

    }

    /**
     * Import another account that correlates to Paul. This has the same resource+kind+intent.
     */
    @Test
    public void test100ImportMuadDib() throws Exception {
        final String TEST_NAME = "test100ImportMuadDib";

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        DummyAccount account = new DummyAccount(ACCOUNT_MUAD_DIB_USERNAME);
        account.setEnabled(true);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_NAME, ACCOUNT_MUAD_DIB_FULL_NAME);
        account.addAttributeValues(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_SHIP_NAME, ACCOUNT_PAUL_ATREIDES_ID);
        getDummyResource(RESOURCE_DUMMY_MULTI_GREEN_NAME).addAccount(account);

        // Preconditions
        assertUsers(getNumberOfUsers() + 1);

        // WHEN
        displayWhen(TEST_NAME);
        importMultiGreenAccounts(task, result);

        // THEN
        displayThen(TEST_NAME);

        accountMuaddibOid = assertUserAfterByUsername(ACCOUNT_PAUL_ATREIDES_USERNAME)
            .displayWithProjections()
            .assertFullName(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
            .assertEmployeeNumber(ACCOUNT_PAUL_ATREIDES_ID)
            .assertOrganizationalUnits(ACCOUNT_PAUL_ATREIDES_FULL_NAME, ACCOUNT_MUAD_DIB_FULL_NAME)
            .links()
                .assertLinks(2)
                .link(accountPaulOid)
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(SchemaConstants.INTENT_DEFAULT)
                        .assertTag(accountPaulOid)
                        .end()
                    .end()
                .by()
                    .notTags(accountPaulOid)
                .find()
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(SchemaConstants.INTENT_DEFAULT)
                        .assertTagIsOid()
                        .getOid();


        assertUsers(getNumberOfUsers() + 1);

    }

    @Test
    public void test102ReconcileUserPaul() throws Exception {
        final String TEST_NAME = "test102ReconcileUserPaul";

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        userPaulOid = findUserByUsername(ACCOUNT_PAUL_ATREIDES_USERNAME).getOid();

        // WHEN
        displayWhen(TEST_NAME);
        reconcileUser(userPaulOid, task, result);

        // THEN
        displayThen(TEST_NAME);

        accountMuaddibOid = assertUserAfter(userPaulOid)
            .displayWithProjections()
            .assertFullName(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
            .assertEmployeeNumber(ACCOUNT_PAUL_ATREIDES_ID)
            // TODO
            .assertOrganizationalUnits(ACCOUNT_PAUL_ATREIDES_FULL_NAME, ACCOUNT_MUAD_DIB_FULL_NAME)
            .links()
                .assertLinks(2)
                .link(accountPaulOid)
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(SchemaConstants.INTENT_DEFAULT)
                        .assertTag(accountPaulOid)
                        .end()
                    .end()
                .by()
                    .notTags(accountPaulOid)
                .find()
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(SchemaConstants.INTENT_DEFAULT)
                        .assertTagIsOid()
                        .getOid();

        assertUsers(getNumberOfUsers() + 1);

    }

    /**
     * Import another account that correlates to Paul. This has the same resource+kind+intent.
     * But this is an admin account (title=duke). Therefore it will have different intent.
     * And there is a custom tag expression.
     */
    @Test
    public void test200ImportDuke() throws Exception {
        final String TEST_NAME = "test200ImportDuke";

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
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
        displayWhen(TEST_NAME);
        importMultiGreenAccounts(task, result);

        // THEN
        displayThen(TEST_NAME);

        accountDukeOid = assertUserAfterByUsername(ACCOUNT_PAUL_ATREIDES_USERNAME)
            .displayWithProjections()
            .assertFullName(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
            .assertEmployeeNumber(ACCOUNT_PAUL_ATREIDES_ID)
            // TODO
//            .assertOrganizationalUnits(ACCOUNT_PAUL_ATREIDES_FULL_NAME, ACCOUNT_MUAD_DIB_FULL_NAME)
            .links()
                .assertLinks(3)
                .link(accountPaulOid)
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(SchemaConstants.INTENT_DEFAULT)
                        .assertTag(accountPaulOid)
                        .end()
                    .end()
                .link(accountMuaddibOid)
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(SchemaConstants.INTENT_DEFAULT)
                        .assertTag(accountMuaddibOid)
                        .end()
                    .end()
                .by()
                    .notTags(accountPaulOid, accountMuaddibOid)
                .find()
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(INTENT_ADMIN)
                        .assertTag(ACCOUNT_DUKE_TITLE)
                        .getOid();


        assertUsers(getNumberOfUsers() + 1);

    }

    /**
     * Import yet another admin account that correlates to Paul.
     */
    @Test
    public void test210ImportMahdi() throws Exception {
        final String TEST_NAME = "test210ImportMahdi";

        // GIVEN
        Task task = createTask(AbstractSynchronizationStoryTest.class.getName() + "." + TEST_NAME);
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
        displayWhen(TEST_NAME);
        importMultiGreenAccounts(task, result);

        // THEN
        displayThen(TEST_NAME);

        accountMahdiOid = assertUserAfterByUsername(ACCOUNT_PAUL_ATREIDES_USERNAME)
            .displayWithProjections()
            .assertFullName(ACCOUNT_PAUL_ATREIDES_FULL_NAME)
            .assertEmployeeNumber(ACCOUNT_PAUL_ATREIDES_ID)
            // TODO
//            .assertOrganizationalUnits(ACCOUNT_PAUL_ATREIDES_FULL_NAME, ACCOUNT_MUAD_DIB_FULL_NAME)
            .links()
                .assertLinks(4)
                .link(accountPaulOid)
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(SchemaConstants.INTENT_DEFAULT)
                        .assertTag(accountPaulOid)
                        .end()
                    .end()
                .link(accountMuaddibOid)
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(SchemaConstants.INTENT_DEFAULT)
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
                .by()
                    .notTags(accountPaulOid, accountMuaddibOid, ACCOUNT_DUKE_TITLE)
                .find()
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ACCOUNT)
                        .assertIntent(INTENT_ADMIN)
                        .assertTag(ACCOUNT_MAHDI_TITLE)
                        .getOid();


        assertUsers(getNumberOfUsers() + 1);

    }

    private void importMultiGreenAccounts(Task task, OperationResult result) throws Exception {
        modelService.importFromResource(RESOURCE_DUMMY_MULTI_GREEN_OID, new QName(getDummyResourceController(RESOURCE_DUMMY_MULTI_GREEN_NAME).getNamespace(), SchemaConstants.ACCOUNT_OBJECT_CLASS_LOCAL_NAME), task, result);
        OperationResult subresult = result.getLastSubresult();
        TestUtil.assertInProgress("importAccountsFromResource result", subresult);
        waitForTaskFinish(task, true, 40000);
    }
}
