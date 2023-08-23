/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.async;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

/**
 *  Tests model.notifyChange using real AMQP messages.
 *
 *  Currently uses caching. And plain messages (no transformation).
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAsyncUpdateUcf extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "async/ucf");

    private static final File RESOURCE_GROUPER_FILE = new File(TEST_DIR, "resource-grouper-ucf-internal.xml");
    protected static final String RESOURCE_GROUPER_ID = "Grouper";
    private static final String RESOURCE_GROUPER_OID = "bbb9900a-b53d-4453-b60b-908725e3950e";

    private static final String BANDERSON_USERNAME = "banderson";
    private static final String JLEWIS685_USERNAME = "jlewis685";
    private static final String KWHITE_USERNAME = "kwhite";
    private static final String ALUMNI_NAME = "ref:alumni";
    private static final String STAFF_NAME = "ref:staff";

    public static final String GROUPER_USER_INTENT = "subject";
    public static final String GROUPER_GROUP_INTENT = "group";

    private static final TestObject<TaskType> TASK_ASYNC_UPDATE = TestObject.file(TEST_DIR, "task-async-update.xml", "02c3f13d-e3e4-40c8-8a39-50013859f0f6");

    private static final File CHANGE_100 = new File(TEST_DIR, "change-100-banderson-add.xml");
    private static final File CHANGE_110 = new File(TEST_DIR, "change-110-alumni-add.xml");
    private static final File CHANGE_110_A = new File(TEST_DIR, "change-110a-staff-add.xml");
    private static final File CHANGE_120 = new File(TEST_DIR, "change-120-kwhite-identifiers-only.xml");
    private static final File CHANGE_200 = new File(TEST_DIR, "change-200-banderson-add-alumni-full-shadow.xml");
    private static final File CHANGE_210 = new File(TEST_DIR, "change-210-banderson-add-staff.xml");
    private static final File CHANGE_220 = new File(TEST_DIR, "change-220-jlewis685-add-alumni.xml");
    private static final File CHANGE_230 = new File(TEST_DIR, "change-230-jlewis685-identifiers-only.xml");
    private static final File CHANGE_300 = new File(TEST_DIR, "change-300-banderson-delete.xml");
    private static final File CHANGE_310 = new File(TEST_DIR, "change-310-staff-delete.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        importAndGetObjectFromFile(ResourceType.class, RESOURCE_GROUPER_FILE, RESOURCE_GROUPER_OID, initTask, initResult);

        addObject(TASK_ASYNC_UPDATE, initTask, initResult);
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultGrouper = modelService.testResource(RESOURCE_GROUPER_OID, task, task.getResult());
        TestUtil.assertSuccess(testResultGrouper);
    }

    /**
     * Shadow ADD delta for banderson.
     */
    @Test
    public void test100AddAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(CHANGE_100).parseRealValue(UcfChangeType.class));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .displayWithProjections()
                .links()
                    .singleAny()
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
    //                        .assertIntent(GROUPER_USER_INTENT)
                            .assertResource(RESOURCE_GROUPER_OID)
                            .end()
                        .end()
                    .end()
                .assertOrganizationalUnits();
    }

    /**
     * Shadow ADD deltas for ref:alumni and ref:staff.
     */
    @Test
    public void test110AddAlumniAndStaff() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(CHANGE_110).parseRealValue(UcfChangeType.class));
        MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(CHANGE_110_A).parseRealValue(UcfChangeType.class));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertOrgByName(ALUMNI_NAME, "after")
                .displayWithProjections()
                .links()
                    .singleAny()
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ENTITLEMENT)
//                        .assertIntent(GROUPER_GROUP_INTENT)
                        .assertResource(RESOURCE_GROUPER_OID);

        assertOrgByName(STAFF_NAME, "after")
                .displayWithProjections()
                .links()
                    .singleAny()
                    .resolveTarget()
                        .display()
                        .assertKind(ShadowKindType.ENTITLEMENT)
//                        .assertIntent(GROUPER_GROUP_INTENT)
                        .assertResource(RESOURCE_GROUPER_OID);
    }

    /**
     * Identifiers-only message for kwhite.
     */
    @Test
    public void test120AddWhite() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(CHANGE_120).parseRealValue(UcfChangeType.class));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(KWHITE_USERNAME)
                .displayWithProjections()
                .links()
                    .singleAny()
                        .resolveTarget()
                            .display()
                            .assertKind(ShadowKindType.ACCOUNT)
                //                        .assertIntent(GROUPER_USER_INTENT)
                            .assertResource(RESOURCE_GROUPER_OID)
                        .end()
                    .end()
                .end()
                .assertOrganizationalUnits();
    }

    /**
     * Adding ref:alumni membership for banderson "the old way" (i.e. by providing full current shadow).
     */
    @Test
    public void test200AddAlumniForAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(CHANGE_200).parseRealValue(UcfChangeType.class));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .displayWithProjections()
                .links()
                    .singleAny()
                        .resolveTarget()
                            .assertKind(ShadowKindType.ACCOUNT)
//                            .assertIntent(GROUPER_USER_INTENT)
                            .assertResource(RESOURCE_GROUPER_OID)
                            .display("shadow after")
                            .end()
                        .end()
                    .end()
                .assertOrganizationalUnits(ALUMNI_NAME);
    }

    /**
     * Adding ref:staff membership for banderson using delta.
     */
    @Test
    public void test210AddStaffForAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(CHANGE_210).parseRealValue(UcfChangeType.class));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .displayWithProjections()
                .links()
                    .singleAny()
                        .resolveTarget()
                            .display("shadow after")
                            .assertKind(ShadowKindType.ACCOUNT)
//                            .assertIntent(GROUPER_USER_INTENT)
                            .assertResource(RESOURCE_GROUPER_OID)
                            .end()
                        .end()
                    .end()
                .assertOrganizationalUnits(ALUMNI_NAME, STAFF_NAME);
    }

    /**
     * Adding ref:alumni membership for jlewis685 "the new way" (i.e. by a delta). But this is the first occurrence of jlewis685!
     */
    @Test
    public void test220AddAlumniForLewis() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(CHANGE_220).parseRealValue(UcfChangeType.class));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(JLEWIS685_USERNAME)
                .displayWithProjections()
                .links()
                    .singleAny()
                        .resolveTarget()
                            .display("shadow after")
                            .assertKind(ShadowKindType.ACCOUNT)
//                            .assertIntent(GROUPER_USER_INTENT)
                            .assertResource(RESOURCE_GROUPER_OID)
                        .end()
                    .end()
                .end()
                .assertOrganizationalUnits(ALUMNI_NAME);
    }

    /**
     * Mentions jlewis again (notification-only change). Should be idempotent.
     */
    @Test
    public void test230MentionLewis() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(CHANGE_230).parseRealValue(UcfChangeType.class));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(JLEWIS685_USERNAME)
                .displayWithProjections()
                .links()
                    .singleAny()
                        .resolveTarget()
                            .display("shadow after")
                            .assertKind(ShadowKindType.ACCOUNT)
//                            .assertIntent(GROUPER_USER_INTENT)
                            .assertResource(RESOURCE_GROUPER_OID)
                        .end()
                    .end()
                .end()
                .assertOrganizationalUnits(ALUMNI_NAME);
    }

    /**
     * Deleting banderson.
     */
    @Test
    public void test300DeleteAnderson() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(CHANGE_300).parseRealValue(UcfChangeType.class));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertUserAfterByUsername(BANDERSON_USERNAME)
                .displayWithProjections()
                //.assertOrganizationalUnits(ALUMNI_NAME)
                .links()
                    .assertNoLiveLinks();
    }

    /**
     * Deleting etc:staff.
     */
    @Test
    public void test310DeleteStaff() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // GIVEN

        assertObjectByName(OrgType.class, STAFF_NAME, task, result);

        MockAsyncUpdateSource.INSTANCE.reset();
        MockAsyncUpdateSource.INSTANCE.prepareMessage(prismContext.parserFor(CHANGE_310).parseRealValue(UcfChangeType.class));

        // WHEN

        rerunTask(TASK_ASYNC_UPDATE.oid, result);

        // THEN

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoObjectByName(OrgType.class, STAFF_NAME, task, result);
    }
}
