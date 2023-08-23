/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.ldap;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.perf.OperationPerformanceInformation;
import com.evolveum.midpoint.repo.api.perf.PerformanceInformation;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Performance tests for accounts with large number of role assignments and group associations; using assignmentTargetSearch,
 * associationFromLink and similar mappings.
 * <p>
 * MID-5341
 */
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestLdapAssociationPerformance extends AbstractLdapTest {

    public static final File TEST_DIR = new File(LDAP_TEST_DIR, "assoc-perf");

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");
    private static final File SYSTEM_CONFIGURATION_NO_ROLE_CACHE_FILE = new File(TEST_DIR, "system-configuration-no-role-cache.xml");

    private static final File RESOURCE_OPENDJ_FILE = new File(TEST_DIR, "resource-opendj.xml");
    private static final String RESOURCE_OPENDJ_OID = "aeff994e-381a-4fb3-af3b-f0f5dcdc9653";

    private static final File ROLE_LDAP_FILE = new File(TEST_DIR, "role-ldap.xml");
    private static final File ROLE_META_FILE = new File(TEST_DIR, "role-meta.xml");
    private static final String ROLE_META_OID = "d723af35-857f-4931-adac-07cc66c4c235";
    private static final File USER_TEMPLATE_FILE = new File(TEST_DIR, "user-template.xml");
    private static final String USER_TEMPLATE_OID = "f86c9851-5724-4c6a-a7e8-59e25a6d4fd1";

    private static final File ROLE_TEST_FILE = new File(TEST_DIR, "role-test.xml");
    private static final String ROLE_TEST_OID = "f2ba978d-ef70-436b-83e6-2ca53bdc8cf2";

    private static final File USER_TEST_FILE = new File(TEST_DIR, "user-test.xml");
    private static final String USER_TEST_OID = "4206f8c9-1ef8-4b17-9ca7-52a4b1439e95";

    private static final File TASK_RECOMPUTE_1_FILE = new File(TEST_DIR, "task-recompute-1.xml");
    private static final String TASK_RECOMPUTE_1_OID = "e3a446c5-07ef-4cbd-9bc9-d37fa5a10d70";
    private static final File TASK_RECOMPUTE_4_FILE = new File(TEST_DIR, "task-recompute-4.xml");
    private static final String TASK_RECOMPUTE_4_OID = "580a9fc0-c902-479e-a061-5d9e0f4e294a";
    private static final File TASK_RECOMPUTE_NO_CACHE_FILE = new File(TEST_DIR, "task-recompute-no-role-and-shadow-cache.xml");
    private static final String TASK_RECOMPUTE_NO_CACHE_OID = "aadb61f6-5bd2-4802-a44d-02f7911eb270";
    private static final File TASK_RECOMPUTE_MULTINODE_FILE = new File(TEST_DIR, "task-recompute-multinode.xml");
    private static final String TASK_RECOMPUTE_MULTINODE_OID = "c20f9aa1-6cf3-4ba7-a187-af9b1e69a5d7";
    private static final File TASK_RECOMPUTE_MULTINODE_MULTITHREADED_FILE = new File(TEST_DIR, "task-recompute-multinode-multithreaded.xml");
    private static final String TASK_RECOMPUTE_MULTINODE_MULTITHREADED_OID = "1952f893-ac76-49c5-a2b1-e65af4d28c63";

    private static final int NUMBER_OF_GENERATED_USERS = 20;
    private static final String GENERATED_USER_NAME_FORMAT = "user%06d";
    private static final String GENERATED_USER_FULL_NAME_FORMAT = "Random J. U%06d";
    private static final String GENERATED_USER_GIVEN_NAME = "Random";
    private static final String GENERATED_USER_FAMILY_NAME_FORMAT = "U%06d";

    private static final int NUMBER_OF_GENERATED_ROLES = 100;
    private static final String GENERATED_ROLE_NAME_FORMAT = "role-%06d";
    private static final String GENERATED_ROLE_OID_FORMAT = "22222222-0000-ffff-1000-000000%06d";

    private static final int RECOMPUTE_TASK_WAIT_TIMEOUT = 120000;

    private static final String SUMMARY_LINE_FORMAT = "%50s: %5d ms (%4d ms/user, %7.2f ms/user/role)\n";
    private static final String REPO_LINE_FORMAT = "%6d (%8.1f/%s)\n";

    private final Map<String, Long> durations = new LinkedHashMap<>();

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected void importSystemTasks(OperationResult initResult) {
        // we don't want these
    }

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServerRI();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    @Override
    protected int getNumberOfRoles() {
        return super.getNumberOfRoles() + 2;            // superuser + ldap, meta
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Resources
        PrismObject<ResourceType> resourceOpenDj = importAndGetObjectFromFile(ResourceType.class, RESOURCE_OPENDJ_FILE, RESOURCE_OPENDJ_OID, initTask, initResult);
        openDJController.setResource(resourceOpenDj);

        importObjectFromFile(ROLE_LDAP_FILE);
        importObjectFromFile(ROLE_META_FILE);
        importObjectFromFile(USER_TEMPLATE_FILE);
        setDefaultUserTemplate(USER_TEMPLATE_OID);

//        InternalMonitor.setTrace(InternalOperationClasses.CONNECTOR_OPERATIONS, true);
    }

    @Override
    protected String getLdapResourceOid() {
        return RESOURCE_OPENDJ_OID;
    }

    @Test
    public void test000ClonePerformance() throws SchemaException {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        long start = System.currentTimeMillis();

        final int COUNT = 1000;
        for (int i = 0; i < COUNT; i++) {
            CloneUtil.clone(systemConfiguration.asObjectable().getGlobalPolicyRule());
        }
        long duration = System.currentTimeMillis() - start;
        System.out.println("Time per clone = " + duration * 1000.0 / COUNT + " us");

    }

    @Test
    public void test010Sanity() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        addObject(ROLE_TEST_FILE, task, result);
        addObject(USER_TEST_FILE, task, result);

        // THEN
        then();

        dumpLdap();
        openDJController.assertUniqueMember("cn=role-test,ou=groups,dc=example,dc=com", "uid=user-test,ou=people,dc=example,dc=com");
        assertLdapConnectorInstances(1);

        deleteObject(UserType.class, USER_TEST_OID);
        deleteObject(RoleType.class, ROLE_TEST_OID);
    }

    @Test
    public void test020GenerateRoles() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        resetPerformanceCollectors();

        long startMillis = System.currentTimeMillis();

        IntegrationTestTools.setSilentConsole(true);
        generateObjects(RoleType.class, NUMBER_OF_GENERATED_ROLES, GENERATED_ROLE_NAME_FORMAT, GENERATED_ROLE_OID_FORMAT,
                (role, i) ->
                        role.beginAssignment()
                                .targetRef(ROLE_META_OID, RoleType.COMPLEX_TYPE),
                role -> addObject(role, task, result),
                result);
        IntegrationTestTools.setSilentConsole(false);

        // THEN
        then();

        long endMillis = System.currentTimeMillis();
        recordDuration((endMillis - startMillis));

        PerformanceInformation performanceInformation = getRepoPerformanceMonitor()
                .getThreadLocalPerformanceInformation();
        dumpRepoSnapshot(performanceInformation, "role", NUMBER_OF_GENERATED_ROLES);
        dumpGlobalCachePerformanceData();

        result.computeStatus();
        assertSuccess(result);

        assertRoles(getNumberOfRoles() + NUMBER_OF_GENERATED_ROLES);

        //dumpLdap();
        assertLdapConnectorInstances(1);
    }

    private void resetPerformanceCollectors() {
        getRepoPerformanceMonitor().startThreadLocalPerformanceInformationCollection();
        resetGlobalCachePerformanceCollector();
    }

    @Test
    public void test100AddUsers() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        resetPerformanceCollectors();

        long startMillis = System.currentTimeMillis();

        IntegrationTestTools.setSilentConsole(true);
        generateObjects(UserType.class, NUMBER_OF_GENERATED_USERS, GENERATED_USER_NAME_FORMAT, null,
                (user, i) -> {
                    user
                            .fullName(String.format(GENERATED_USER_FULL_NAME_FORMAT, i))
                            .givenName(GENERATED_USER_GIVEN_NAME)
                            .familyName(String.format(GENERATED_USER_FAMILY_NAME_FORMAT, i));
                    PrismProperty<Object> memberOf;
                    try {
                        memberOf = user.asPrismObject().createExtension().getValue()
                                .findOrCreateProperty(new ItemName("memberOf"));
                        for (int roleIndex = 0; roleIndex < NUMBER_OF_GENERATED_ROLES; roleIndex++) {
                            memberOf.addRealValue(String.format(GENERATED_ROLE_NAME_FORMAT, roleIndex));
                        }
                    } catch (SchemaException e) {
                        throw new AssertionError(e.getMessage(), e);
                    }
                },
                user -> addObject(user, task, result),
                result);
        IntegrationTestTools.setSilentConsole(false);

        // THEN
        then();

        long endMillis = System.currentTimeMillis();
        recordDuration((endMillis - startMillis));

        PerformanceInformation performanceInformation = getRepoPerformanceMonitor().getThreadLocalPerformanceInformation();
        dumpRepoSnapshotPerUser(performanceInformation);
        dumpGlobalCachePerformanceData();

        result.computeStatus();
        assertSuccess(result);
        //dumpLdap();
        assertLdapConnectorInstances(1);
    }

    private void dumpRepoSnapshotPerUser(PerformanceInformation performanceInformation) {
        dumpRepoSnapshot(performanceInformation, "user", NUMBER_OF_GENERATED_USERS);
    }

    private void dumpRepoSnapshot(
            PerformanceInformation performanceInformation, String unit, int unitCount) {
        String label = "SQL operations for " + getTestNameShort();
        displayDumpable(label + " (" + NUMBER_OF_GENERATED_USERS + " users, " + NUMBER_OF_GENERATED_ROLES
                + " roles) (got from global monitor)", performanceInformation);

        // per unit:
        Map<String, OperationPerformanceInformation> counters = performanceInformation.getAllData();
        ArrayList<String> kinds = new ArrayList<>(counters.keySet());
        kinds.sort(String::compareToIgnoreCase);
        int max = kinds.stream().mapToInt(String::length).max().orElse(0);
        StringBuilder sb = new StringBuilder();
        kinds.forEach(kind -> sb.append(
                String.format("%" + (max + 2) + "s: " + REPO_LINE_FORMAT,
                        kind, counters.get(kind).getInvocationCount(),
                        (double) counters.get(kind).getInvocationCount() / unitCount, unit)));
        displayValue(label + " (" + NUMBER_OF_GENERATED_USERS + " users, " + NUMBER_OF_GENERATED_ROLES + " roles) - per " + unit, sb.toString());
    }

    @Test
    public void test110RecomputeUsers() throws Exception {
        rememberConnectorResourceCounters();

        // WHEN
        when();

        getRepoPerformanceMonitor().clearGlobalPerformanceInformation();
        resetGlobalCachePerformanceCollector();

        addTask(TASK_RECOMPUTE_1_FILE);

        waitForTaskFinish(TASK_RECOMPUTE_1_OID, RECOMPUTE_TASK_WAIT_TIMEOUT);

        // THEN
        then();

        recordDuration(getRunDurationMillis(TASK_RECOMPUTE_1_OID));
        PerformanceInformation performanceInformation = getRepoPerformanceMonitor().getGlobalPerformanceInformation();
        dumpRepoSnapshotPerUser(performanceInformation);
        dumpGlobalCachePerformanceData();

        OperationStatsType statistics = getTaskTreeOperationStatistics(TASK_RECOMPUTE_1_OID);
        displayOperationStatistics(statistics);
        assertNotNull(statistics);

        assertUsers(getNumberOfUsers() + NUMBER_OF_GENERATED_USERS);

        assertLdapAccounts(getNumberOfLdapAccounts() + NUMBER_OF_GENERATED_USERS);
        assertLdapConnectorInstances(1);

        assertSteadyResource();
        //assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);

        // TODO
        //assertEquals("Wrong success count", NUMBER_OF_GENERATED_USERS, TaskOperationStatsUtil.getItemsProcessedWithSuccess(statistics));
    }

    @Test
    public void test120RecomputeUsersNoRoleAndShadowCache() throws Exception {
        rememberConnectorResourceCounters();

        // WHEN
        when();

        getRepoPerformanceMonitor().clearGlobalPerformanceInformation();
        resetGlobalCachePerformanceCollector();

        addTask(TASK_RECOMPUTE_NO_CACHE_FILE);

        waitForTaskFinish(TASK_RECOMPUTE_NO_CACHE_OID, RECOMPUTE_TASK_WAIT_TIMEOUT);

        // THEN
        then();

        recordDuration(getRunDurationMillis(TASK_RECOMPUTE_NO_CACHE_OID));

        PerformanceInformation performanceInformation = getRepoPerformanceMonitor().getGlobalPerformanceInformation();
        dumpRepoSnapshotPerUser(performanceInformation);
        dumpGlobalCachePerformanceData();

        OperationStatsType statistics = getTaskTreeOperationStatistics(TASK_RECOMPUTE_NO_CACHE_OID);
        displayOperationStatistics(statistics);
        assertNotNull(statistics);

        assertUsers(getNumberOfUsers() + NUMBER_OF_GENERATED_USERS);

        assertLdapAccounts(getNumberOfLdapAccounts() + NUMBER_OF_GENERATED_USERS);
        assertLdapConnectorInstances(1);

        assertSteadyResource();
        //assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);

        // TODO
        //assertEquals("Wrong success count", NUMBER_OF_GENERATED_USERS, TaskOperationStatsUtil.getItemsProcessedWithSuccess(statistics));
    }

    @Test
    public void test130RecomputeUsersMultinode() throws Exception {
        rememberConnectorResourceCounters();

        // WHEN
        when();

        getRepoPerformanceMonitor().clearGlobalPerformanceInformation();
        resetGlobalCachePerformanceCollector();

        addTask(TASK_RECOMPUTE_MULTINODE_FILE);

        IntegrationTestTools.setSilentConsole(true);
        waitForNextRootActivityCompletion(TASK_RECOMPUTE_MULTINODE_OID, RECOMPUTE_TASK_WAIT_TIMEOUT);
        IntegrationTestTools.setSilentConsole(false);

        // THEN
        then();

        recordDuration(getTreeRunDurationMillis(TASK_RECOMPUTE_MULTINODE_OID));

        PerformanceInformation performanceInformation = getRepoPerformanceMonitor().getGlobalPerformanceInformation();
        dumpRepoSnapshotPerUser(performanceInformation);
        dumpGlobalCachePerformanceData();

        OperationStatsType statistics = getTaskTreeOperationStatistics(TASK_RECOMPUTE_MULTINODE_OID);
        displayOperationStatistics(statistics);
        assertNotNull(statistics);

        assertUsers(getNumberOfUsers() + NUMBER_OF_GENERATED_USERS);

        assertLdapAccounts(getNumberOfLdapAccounts() + NUMBER_OF_GENERATED_USERS);
        //assertLdapConnectorInstances(1);

        assertSteadyResource();
        //assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);

        //dumpTaskTree(TASK_RECOMPUTE_MULTINODE_OID, result);

        // TODO
        //assertEquals("Wrong success count", NUMBER_OF_GENERATED_USERS, TaskOperationStatsUtil.getItemsProcessedWithSuccess(statistics));
    }

    @Test
    public void test140RecomputeUsersMultinodeMultithreaded() throws Exception {
        rememberConnectorResourceCounters();

        // WHEN
        when();

        getRepoPerformanceMonitor().clearGlobalPerformanceInformation();
        resetGlobalCachePerformanceCollector();

        addTask(TASK_RECOMPUTE_MULTINODE_MULTITHREADED_FILE);

        IntegrationTestTools.setSilentConsole(true);
        waitForNextRootActivityCompletion(TASK_RECOMPUTE_MULTINODE_MULTITHREADED_OID, RECOMPUTE_TASK_WAIT_TIMEOUT);
        IntegrationTestTools.setSilentConsole(false);

        // THEN
        then();

        recordDuration(getTreeRunDurationMillis(TASK_RECOMPUTE_MULTINODE_MULTITHREADED_OID));

        // todo retrieve this information from finished task
        PerformanceInformation performanceInformation = getRepoPerformanceMonitor().getGlobalPerformanceInformation();
        dumpRepoSnapshotPerUser(performanceInformation);
        dumpGlobalCachePerformanceData();

        OperationStatsType statistics = getTaskTreeOperationStatistics(TASK_RECOMPUTE_MULTINODE_MULTITHREADED_OID);
        displayOperationStatistics(statistics);
        assertNotNull(statistics);

        assertUsers(getNumberOfUsers() + NUMBER_OF_GENERATED_USERS);

        assertLdapAccounts(getNumberOfLdapAccounts() + NUMBER_OF_GENERATED_USERS);
        //assertLdapConnectorInstances(1);

        assertSteadyResource();
        //assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);

        //dumpTaskTree(TASK_RECOMPUTE_MULTINODE_OID, result);

        // TODO
        //assertEquals("Wrong success count", NUMBER_OF_GENERATED_USERS, TaskOperationStatsUtil.getItemsProcessedWithSuccess(statistics));
    }

    @Test
    public void test200RecomputeUsersNoDefaultRoleCache() throws Exception {
        rememberConnectorResourceCounters();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();

        PrismObject<SystemConfigurationType> newConfiguration = parseObject(SYSTEM_CONFIGURATION_NO_ROLE_CACHE_FILE);
        repositoryService.addObject(newConfiguration, RepoAddOptions.createOverwrite(), result);

        getRepoPerformanceMonitor().clearGlobalPerformanceInformation();
        resetGlobalCachePerformanceCollector();

        addTask(TASK_RECOMPUTE_4_FILE);
        waitForTaskFinish(TASK_RECOMPUTE_4_OID, RECOMPUTE_TASK_WAIT_TIMEOUT);

        // THEN
        then();

        recordDuration(getRunDurationMillis(TASK_RECOMPUTE_4_OID));

        // todo retrieve this information from finished task
        PerformanceInformation performanceInformation = getRepoPerformanceMonitor().getGlobalPerformanceInformation();
        dumpRepoSnapshotPerUser(performanceInformation);
        dumpGlobalCachePerformanceData();

        OperationStatsType statistics = getTaskTreeOperationStatistics(TASK_RECOMPUTE_4_OID);
        displayOperationStatistics(statistics);
        assertNotNull(statistics);

        assertUsers(getNumberOfUsers() + NUMBER_OF_GENERATED_USERS);

        assertLdapAccounts(getNumberOfLdapAccounts() + NUMBER_OF_GENERATED_USERS);
//        assertLdapConnectorInstances(1);

        assertSteadyResource();
        //assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_MODIFICATION_COUNT, 0);

        // TODO
        //assertEquals("Wrong success count", NUMBER_OF_GENERATED_USERS, TaskOperationStatsUtil.getItemsProcessedWithSuccess(statistics));
    }

    @Test
    public void test900Summarize() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Long> entry : durations.entrySet()) {
            sb.append(summary(entry.getKey(), entry.getValue()));
        }
        displayValue("Summary (" + NUMBER_OF_GENERATED_USERS + " users, " + NUMBER_OF_GENERATED_ROLES + " roles)", sb.toString());

        // THEN
        then();

        // TODO: more thresholds

    }

    private void rememberConnectorResourceCounters() {
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
        rememberCounter(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
        rememberCounter(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_MODIFICATION_COUNT);
    }

    private void assertSteadyResource() {
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_CONFIGURATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
        assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT, 0);
    }

    @SuppressWarnings("UnusedReturnValue")
    private long recordDuration(long duration) {
        durations.put(getTestNameShort(), duration);
        return duration;
    }

    private Object summary(String label, long duration) {
        return String.format(SUMMARY_LINE_FORMAT, label, duration, duration / NUMBER_OF_GENERATED_USERS, (double) duration / (NUMBER_OF_GENERATED_USERS * NUMBER_OF_GENERATED_ROLES));
    }
}
