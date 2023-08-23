/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.story;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

/**
 * Test for various livesync scenarios and corner cases.
 *
 * @author semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLiveSyncMadness extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "livesync-madness");

    // target
    protected static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
    protected static final String RESOURCE_DUMMY_OID = "8126d404-0ae3-11ea-a1e8-0bdaaafeca7d";

    // HR = source
    protected static final File RESOURCE_DUMMY_HR_FILE = new File(TEST_DIR, "resource-dummy-hr.xml");
    protected static final String RESOURCE_DUMMY_HR_ID = "HR";
    protected static final String RESOURCE_DUMMY_HR_OID = "3cf5686a-0ae1-11ea-8319-47b746c1e7b1";
    protected static final String RESOURCE_DUMMY_HR_NAMESPACE = MidPointConstants.NS_RI;

    protected static final File TASK_LIVE_SYNC_DUMMY_HR_FILE = new File(TEST_DIR, "task-dumy-hr-livesync.xml");
    protected static final String TASK_LIVE_SYNC_DUMMY_HR_OID = "01d7cd7c-0ae1-11ea-825f-c3d1d4cff3b0";

    public static final File ROLE_BASIC_FILE = new File(TEST_DIR, "role-basic.xml");
    public static final String ROLE_BASIC_OID = "f822d958-0ae9-11ea-aae1-27914635df2c";

    public static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
    public static final String OBJECT_TEMPLATE_USER_OID = "817b3764-0ae9-11ea-b5cd-5bbc7109f250";

    private static final String ACCOUNT_HERMAN_USERNAME = "ht";
    private static final String ACCOUNT_HERMAN_FIST_NAME = "Herman";
    private static final String ACCOUNT_HERMAN_LAST_NAME = "Toothrot";
    private static final String ACCOUNT_HT_FIST_NAME = "Horatio Torquemada";
    private static final String ACCOUNT_HT_LAST_NAME = "Marley";

    private static final String USER_JACK_FULL_NAME_CAPTAIN = "Captain Jack Sparrow";

    protected static DummyResource dummyResourceHr;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Resources
        initDummyResource(RESOURCE_DUMMY_HR_ID, RESOURCE_DUMMY_HR_FILE, RESOURCE_DUMMY_HR_OID, c -> {
                DummyObjectClass dummyAdAccountObjectClass = c.getDummyResource().getAccountObjectClass();
                c.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME,
                        String.class, false, false);
                c.addAttrDef(dummyAdAccountObjectClass, DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME,
                        String.class, false, false);
                // We want CREATE_OR_UPDATE delta for MID-5922.
                // But SMART sync style may be interesting for other test cases. Maybe add more tests with smart sync style later?
                c.getDummyResource().setSyncStyle(DummySyncStyle.DUMB);
            } , initTask, initResult);
        dummyResourceHr = getDummyResource(RESOURCE_DUMMY_HR_ID);

        initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);

        // Role
        importObjectFromFile(ROLE_BASIC_FILE, initResult);

        // Object Templates
        importObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
        setDefaultUserTemplate(OBJECT_TEMPLATE_USER_OID);

        // Tasks
        importObjectFromFile(TASK_LIVE_SYNC_DUMMY_HR_FILE, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultHr = modelService.testResource(RESOURCE_DUMMY_HR_OID, task, task.getResult());
        TestUtil.assertSuccess(testResultHr);

        OperationResult testResultOpenDj = modelService.testResource(RESOURCE_DUMMY_OID, task, task.getResult());
        TestUtil.assertSuccess(testResultOpenDj);

        waitForTaskStart(TASK_TRIGGER_SCANNER_OID);
        waitForTaskStart(TASK_VALIDITY_SCANNER_OID);
        waitForTaskStart(TASK_LIVE_SYNC_DUMMY_HR_OID);
    }

    @Test
    public void test100AddHrAccountHerman() throws Exception {
        dummyAuditService.clear();

        DummyAccount newAccount = new DummyAccount(ACCOUNT_HERMAN_USERNAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_HERMAN_FIST_NAME);
        newAccount.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_HERMAN_LAST_NAME);
        dummyResourceHr.addAccount(newAccount);

        // WHEN
        when();
        restartTask(TASK_LIVE_SYNC_DUMMY_HR_OID);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID);

        // THEN
        then();
        assertUserAfterByUsername(ACCOUNT_HERMAN_USERNAME)
                .assertFullName(ACCOUNT_HERMAN_FIST_NAME + " " + ACCOUNT_HERMAN_LAST_NAME)
                .assertTitle("Mr. " + ACCOUNT_HERMAN_LAST_NAME)
                .links()
                    .assertLiveLinks(2)
                    .by()
                        .resourceOid(RESOURCE_DUMMY_HR_OID)
                    .assertCount(1)
                    .by()
                        .resourceOid(RESOURCE_DUMMY_OID)
                    .find()
                        .target()
                            .assertName(ACCOUNT_HERMAN_USERNAME);

        assertDummyAccountByUsername(null, ACCOUNT_HERMAN_USERNAME)
                .assertFullName(ACCOUNT_HERMAN_FIST_NAME + " " + ACCOUNT_HERMAN_LAST_NAME)
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Mr. " + ACCOUNT_HERMAN_LAST_NAME + " " + ACCOUNT_HERMAN_FIST_NAME);

        displayDumpable("Audit", dummyAuditService);
    }

    @Test
    public void test110RenameHrAccountHerman() throws Exception {
        dummyAuditService.clear();

        DummyAccount account = dummyResourceHr.getAccountByUsername(ACCOUNT_HERMAN_USERNAME);
        account.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_FIRST_NAME, ACCOUNT_HT_FIST_NAME);
        account.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_HR_LAST_NAME, ACCOUNT_HT_LAST_NAME);

        // WHEN
        when();
        restartTask(TASK_LIVE_SYNC_DUMMY_HR_OID);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID);

        // THEN
        then();
        assertUserAfterByUsername(ACCOUNT_HERMAN_USERNAME)
                .assertFullName(ACCOUNT_HT_FIST_NAME + " " + ACCOUNT_HT_LAST_NAME)
                .assertTitle("Mr. " + ACCOUNT_HT_LAST_NAME)
                .links()
                    .assertLiveLinks(2)
                    .by()
                        .resourceOid(RESOURCE_DUMMY_HR_OID)
                    .assertCount(1)
                    .by()
                        .resourceOid(RESOURCE_DUMMY_OID)
                    .find()
                        .target()
                            .assertName(ACCOUNT_HERMAN_USERNAME);

        assertDummyAccountByUsername(null, ACCOUNT_HERMAN_USERNAME)
                .assertFullName(ACCOUNT_HT_FIST_NAME + " " + ACCOUNT_HT_LAST_NAME)
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Mr. " + ACCOUNT_HT_LAST_NAME + " " + ACCOUNT_HT_FIST_NAME);

        displayDumpable("Audit", dummyAuditService);
    }

    /**
     * Initiate livesync with empty delta. See if anything breaks.
     * MID-5922
     */
    @Test
    public void test112HrAccountHermanEmptyDelta() throws Exception {
        dummyAuditService.clear();

        dummyResourceHr.recordEmptyDeltaForAccountByUsername(ACCOUNT_HERMAN_USERNAME, DummyDeltaType.MODIFY);

        // WHEN
        when();
        restartTask(TASK_LIVE_SYNC_DUMMY_HR_OID);
        waitForTaskNextRunAssertSuccess(TASK_LIVE_SYNC_DUMMY_HR_OID);

        // THEN
        then();
        assertUserAfterByUsername(ACCOUNT_HERMAN_USERNAME)
                .assertFullName(ACCOUNT_HT_FIST_NAME + " " + ACCOUNT_HT_LAST_NAME)
                .assertTitle("Mr. " + ACCOUNT_HT_LAST_NAME)
                .links()
                    .assertLiveLinks(2)
                    .by()
                        .resourceOid(RESOURCE_DUMMY_HR_OID)
                    .assertCount(1)
                    .by()
                        .resourceOid(RESOURCE_DUMMY_OID)
                    .find()
                        .target()
                            .assertName(ACCOUNT_HERMAN_USERNAME);

        assertDummyAccountByUsername(null, ACCOUNT_HERMAN_USERNAME)
                .assertFullName(ACCOUNT_HT_FIST_NAME + " " + ACCOUNT_HT_LAST_NAME)
                .assertAttribute(DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_TITLE_NAME, "Mr. " + ACCOUNT_HT_LAST_NAME + " " + ACCOUNT_HT_FIST_NAME);

        displayDumpable("Audit", dummyAuditService);
        dummyAuditService.assertNoRecord();
    }
}
