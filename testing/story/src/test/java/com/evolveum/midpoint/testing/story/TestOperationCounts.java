/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.RepoOpAsserter;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * Checks for repository (and maybe others) operations counts using some basic scenarios.
 * Used to optimize things specified in MID-5539 and related issues.
 *
 * The scenario is quite simplistic: a couple of roles giving some resource accounts;
 * user creation and modification.
 *
 * We will gradually add features as needed.
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml","classpath:ctx-interceptor.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestOperationCounts extends AbstractStoryTest {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "counts");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final File OBJECT_TEMPLATE_FILE = new File(TEST_DIR, "template-user.xml");
    private static final String OBJECT_TEMPLATE_OID = "e84d7b5a-4634-4b75-a17c-df0b8b49b593";

    private static final String RESOURCE_DUMMY_ONE_NAME = "one";
    private static final File RESOURCE_DUMMY_ONE_FILE = new File(TEST_DIR, "resource-dummy-one.xml");
    private static final String RESOURCE_DUMMY_ONE_OID = "3955c91d-e567-4c50-9c2a-09e9dcd246a5";

    private static final String RESOURCE_DUMMY_TWO_NAME = "two";
    private static final File RESOURCE_DUMMY_TWO_FILE = new File(TEST_DIR, "resource-dummy-two.xml");
    private static final String RESOURCE_DUMMY_TWO_OID = "be7cdd4c-9af3-4f75-9d81-a2cfc7e477b5";

    private static final String RESOURCE_DUMMY_THREE_NAME = "three";
    private static final File RESOURCE_DUMMY_THREE_FILE = new File(TEST_DIR, "resource-dummy-three.xml");
    private static final String RESOURCE_DUMMY_THREE_OID = "f0c6cff1-30f6-46ca-b5a1-88c2dc47862c";

    private static final File ROLE_ONE_FILE = new File(TEST_DIR, "role-one.xml");

    private static final File ROLE_TWO_FILE = new File(TEST_DIR, "role-two.xml");

    private static final File ROLE_THREE_FILE = new File(TEST_DIR, "role-three.xml");

    private static final File USER_ALICE_FILE = new File(TEST_DIR, "user-alice.xml");
    private static final String USER_ALICE_OID = "84851118-7579-46aa-a1c7-5b22eec1e443";

    private static final File USER_BOB_FILE = new File(TEST_DIR, "user-bob.xml");
    private static final String USER_BOB_OID = "92a724e3-9441-4727-9c68-a23bcba024a9";

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        InternalsConfig.turnOffAllChecks();

        // Object Templates
        importObjectFromFile(OBJECT_TEMPLATE_FILE, initResult);
        setDefaultUserTemplate(OBJECT_TEMPLATE_OID);

        initDummyResourcePirate(RESOURCE_DUMMY_ONE_NAME, RESOURCE_DUMMY_ONE_FILE, RESOURCE_DUMMY_ONE_OID, initTask, initResult);
        initDummyResourcePirate(RESOURCE_DUMMY_TWO_NAME, RESOURCE_DUMMY_TWO_FILE, RESOURCE_DUMMY_TWO_OID, initTask, initResult);
        initDummyResourcePirate(RESOURCE_DUMMY_THREE_NAME, RESOURCE_DUMMY_THREE_FILE, RESOURCE_DUMMY_THREE_OID, initTask, initResult);

        importObjectFromFile(ROLE_ONE_FILE, initResult);
        importObjectFromFile(ROLE_TWO_FILE, initResult);
        importObjectFromFile(ROLE_THREE_FILE, initResult);

        notificationManager.setDisabled(false);             // in order to test MID-5557
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false;           // we want logging from our system config
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected void importSystemTasks(OperationResult initResult) {
        // nothing here
    }

    @Test
    public void test000Sanity() throws Exception {
        Task task = getTestTask();

        OperationResult testResultOne = modelService.testResource(RESOURCE_DUMMY_ONE_OID, task, task.getResult());
        TestUtil.assertSuccess(testResultOne);

        SystemConfigurationType systemConfiguration = getSystemConfiguration();
        assertNotNull("No system configuration", systemConfiguration);
        display("System config", systemConfiguration);
    }

    @Test
    public void test100AddAlice() throws Exception {
        Task task = createTracedTask();
        OperationResult result = task.getResult();

        // GIVEN

        // WHEN
        when();

        resetThreadLocalPerformanceData();
        addObject(USER_ALICE_FILE, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        RepoOpAsserter repoOpAsserter = createRepoOpAsserter().display();
        dumpThreadLocalCachePerformanceData();

        assertUser(USER_ALICE_OID, "alice")
                .assertFullName("Alice White")
                .assertLiveLinks(3)
                .projectionOnResource(RESOURCE_DUMMY_ONE_OID)
                    .end()
                .projectionOnResource(RESOURCE_DUMMY_TWO_OID)
                    .end()
                .projectionOnResource(RESOURCE_DUMMY_THREE_OID)
                    .end();

        repoOpAsserter
                .assertOp("addObject.UserType", 1)
                .assertOp("addObject.ShadowType", 3)
                .assertOp("addObject", 4)
                .assertOp("audit.AuditEventRecord", 2)
                .assertOp("getObject.RoleType", 3)
                .assertOp("getObject.ShadowType", 6)               // todo lower this
                .assertOp("getObject.UserType", 2)                  // todo lower this
                .assertOp("getVersion.ObjectType", 0)
                .assertOp("listAccountShadowOwner.UserType", 0)
                .assertOp("modifyObject.ShadowType", 3)             // todo
                .assertOp("modifyObject.UserType", 3);              // todo
    }

    // Some objects should be cached now
    @Test
    public void test110AddBob() throws Exception {
        Task task = createTracedTask();
        OperationResult result = task.getResult();

        // GIVEN

        // WHEN
        when();

        resetThreadLocalPerformanceData();
        addObject(USER_BOB_FILE, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        RepoOpAsserter repoOpAsserter = createRepoOpAsserter().display();
        dumpThreadLocalCachePerformanceData();

        assertUser(USER_BOB_OID, "bob")
                .assertFullName("Bob Black")
                .assertLiveLinks(3)
                .projectionOnResource(RESOURCE_DUMMY_ONE_OID)
                    .end()
                .projectionOnResource(RESOURCE_DUMMY_TWO_OID)
                    .end()
                .projectionOnResource(RESOURCE_DUMMY_THREE_OID)
                    .end();

        repoOpAsserter
                .assertOp("addObject.UserType", 1)
                .assertOp("addObject.ShadowType", 3)
                .assertOp("audit.AuditEventRecord", 2)
                .assertOp("getObject.ShadowType", 6)               // todo lower this
                .assertOp("getObject.UserType", 2)                  // todo lower this
                .assertOp("getVersion.ObjectType", 0)
                .assertOp("listAccountShadowOwner.UserType", 0)
                .assertOp("modifyObject.ShadowType", 3)             // todo
                .assertOp("modifyObject.UserType", 3);              // todo
    }

    @Test
    public void test120ModifyBob() throws Exception {
        Task task = createTracedTask();
        OperationResult result = task.getResult();

        // GIVEN

        ObjectDelta<UserType> delta = prismContext.deltaFor(UserType.class)
                .item(UserType.F_FAMILY_NAME).replace(PolyString.fromOrig("Brown"))
                .asObjectDelta(USER_BOB_OID);

        // WHEN
        when();

        resetThreadLocalPerformanceData();
        executeChanges(delta, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        RepoOpAsserter repoOpAsserter = createRepoOpAsserter().display();
        dumpThreadLocalCachePerformanceData();

        assertUser(USER_BOB_OID, "bob")
                .assertFullName("Bob Brown")
                .assertLiveLinks(3);

        repoOpAsserter
                .assertOp("addObject", 0)
                .assertOp("audit.AuditEventRecord", 2)
                .assertOp("getObject.ShadowType", 0)                // this is 0 because of the global cache!
                .assertOp("getObject.UserType", 2)                  // todo lower this
                .assertOp("getVersion.ObjectType", 0)
                .assertOp("listAccountShadowOwner.UserType", 0)
                .assertOp("modifyObject.ShadowType", 0)
                .assertOp("modifyObject.UserType", 1);              // todo
    }
}
