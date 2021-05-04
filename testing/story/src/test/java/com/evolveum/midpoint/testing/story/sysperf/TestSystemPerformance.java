/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story.sysperf;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.test.util.MidPointTestConstants.TARGET_DIR_PATH;
import static com.evolveum.midpoint.tools.testng.TestMonitor.PERF_REPORT_PREFIX_PROPERTY_NAME;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.statistics.StatisticsUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.tools.testng.PerformanceTestMixin;
import com.evolveum.midpoint.tools.testng.TestReportSection;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests the overall system performance. Can be parameterized using Java properties.
 *
 * Examples:
 *
 *   -Dsources.resources=2 -Dtargets.resources=3 -Droles.business.count=10 -Droles.technical.count=20 -Droles.assignments.count=4 -Droles.inducements.count=5
 */
@SuppressWarnings("BusyWait")
@ContextConfiguration(locations = { "classpath:ctx-story-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSystemPerformance extends AbstractStoryTest implements PerformanceTestMixin {

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "system-perf");
    static final File TARGET_DIR = new File(TARGET_DIR_PATH);

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    static final SchemaConfiguration SCHEMA_CONFIGURATION;
    static final SourcesConfiguration SOURCES_CONFIGURATION;
    static final TargetsConfiguration TARGETS_CONFIGURATION;
    static final RolesConfiguration ROLES_CONFIGURATION;
    static final ImportConfiguration IMPORTS_CONFIGURATION;
    static final ReconciliationConfiguration RECONCILIATIONS_CONFIGURATION;
    static final RecomputationConfiguration RECOMPUTATION_CONFIGURATION;

    static final OtherParameters OTHER_PARAMETERS;

    private static final List<DummyTestResource> RESOURCE_SOURCE_LIST;
    private static final List<DummyTestResource> RESOURCE_TARGET_LIST;

    private static final TestResource<ArchetypeType> ARCHETYPE_BASIC_USER =
            new TestResource<>(TEST_DIR, "archetype-basic-user.xml", "463e21c5-9959-48e9-bc2a-5356eafb0589");

    static final TestResource<RoleType> ROLE_TARGETS = new TestResource<>(TARGET_DIR, "generated-role-targets.xml", "3b65aad7-7d6b-412e-bfc7-2cee44d22c32");
    private static final TestResource<RoleType> TEMPLATE_USER = new TestResource<>(TEST_DIR, "template-user.xml", "0c77fde5-4ad5-49ce-8ee9-14f330660d8e");

    private static final List<TestResource<RoleType>> BUSINESS_ROLE_LIST;
    private static final List<TestResource<RoleType>> TECHNICAL_ROLE_LIST;
    private static final List<TestResource<TaskType>> TASK_IMPORT_LIST;
    private static final List<TestResource<TaskType>> TASK_RECONCILIATION_LIST;
    private static final TestResource<TaskType> TASK_RECOMPUTE;

    static final long START = System.currentTimeMillis();

    private static final String REPORT_FILE_PREFIX = TARGET_DIR_PATH + "/" + START + "-report";
    private static final String REPORT_SECTION_SUMMARY_NAME = "summary";
    private static final String REPORT_SECTION_TASK_EXECUTION_NAME = "taskExecution";
    private static final String REPORT_SECTION_TASK_EXECUTION_DENORMALIZED_NAME = "taskExecutionDenormalized";

    private TestReportSection taskExecutionReportSection;
    private TestReportSection taskExecutionDenormalizedReportSection;

    private final ProgressOutputFile progressOutputFile = new ProgressOutputFile();
    private final SummaryOutputFile summaryOutputFile = new SummaryOutputFile();
    private final DetailsOutputFile detailsOutputFile = new DetailsOutputFile();

    private final List<String> summaryReportHeader = new ArrayList<>();
    private final List<Object> summaryReportDataRow = new ArrayList<>();

    private long lastProgress;

    static {
        SCHEMA_CONFIGURATION = SchemaConfiguration.setup();
        SOURCES_CONFIGURATION = SourcesConfiguration.setup();
        TARGETS_CONFIGURATION = TargetsConfiguration.setup();
        ROLES_CONFIGURATION = RolesConfiguration.setup();
        IMPORTS_CONFIGURATION = ImportConfiguration.setup();
        RECONCILIATIONS_CONFIGURATION = ReconciliationConfiguration.setup();
        RECOMPUTATION_CONFIGURATION = RecomputationConfiguration.setup();

        OTHER_PARAMETERS = OtherParameters.setup();

        RESOURCE_SOURCE_LIST = SOURCES_CONFIGURATION.getGeneratedResources();
        RESOURCE_TARGET_LIST = TARGETS_CONFIGURATION.getGeneratedResources();
        BUSINESS_ROLE_LIST = ROLES_CONFIGURATION.getGeneratedBusinessRoles();
        TECHNICAL_ROLE_LIST = ROLES_CONFIGURATION.getGeneratedTechnicalRoles();
        TASK_IMPORT_LIST = IMPORTS_CONFIGURATION.getGeneratedTasks();
        TASK_RECONCILIATION_LIST = RECONCILIATIONS_CONFIGURATION.getGeneratedTasks();
        TASK_RECOMPUTE = RECOMPUTATION_CONFIGURATION.getGeneratedTask();

        System.setProperty(PERF_REPORT_PREFIX_PROPERTY_NAME, REPORT_FILE_PREFIX);
    }

    public TestSystemPerformance() throws IOException {
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DummyAuditService.getInstance().setEnabled(false);

        InternalsConfig.turnOffAllChecks();

        new SourceInitializer(this, RESOURCE_SOURCE_LIST, initTask)
                .run(initResult);

        new TargetInitializer(this, RESOURCE_TARGET_LIST, initTask)
                .run(initResult);

        repoAdd(ARCHETYPE_BASIC_USER, initResult);
        repoAdd(ROLE_TARGETS, initResult);
        repoAdd(TEMPLATE_USER, initResult);

        for (TestResource<?> resource : TECHNICAL_ROLE_LIST) {
            addObject(resource, initTask, initResult); // creates resource objects
        }

        for (TestResource<?> resource : BUSINESS_ROLE_LIST) {
            repoAdd(resource, initResult);
        }

        createSummaryReportData();
    }

    private void createSummaryReportData() {

        summaryReportHeader.clear();
        summaryReportHeader.addAll(
                Arrays.asList(
                        "label",
                        "sources", "accounts", "singleValuedInboundMappings", "multiValuedInboundMappings", "attributeValues",
                        "targets", "singleValuedOutboundMappings", "multiValuedOutboundMappings",
                        "businessRoles", "technicalRoles", "assignmentsMin", "assignmentsMax", "inducementsMin", "inducementsMax",
                        "schemaSingleValuedProperties", "schemaMultiValuedProperties", "schemaIndexedPercentage",
                        "importTaskThreads",
                        "reconciliationTaskThreads",
                        "recomputationTaskThreads"));

        summaryReportDataRow.clear();
        summaryReportDataRow.addAll(
                Arrays.asList(
                        OTHER_PARAMETERS.label,

                        SOURCES_CONFIGURATION.getNumberOfResources(),
                        SOURCES_CONFIGURATION.getNumberOfAccounts(),
                        SOURCES_CONFIGURATION.getSingleValuedMappings(),
                        SOURCES_CONFIGURATION.getMultiValuedMappings(),
                        SOURCES_CONFIGURATION.getAttributeValues(),

                        TARGETS_CONFIGURATION.getNumberOfResources(),
                        TARGETS_CONFIGURATION.getSingleValuedMappings(),
                        TARGETS_CONFIGURATION.getMultiValuedMappings(),

                        ROLES_CONFIGURATION.getNumberOfBusinessRoles(),
                        ROLES_CONFIGURATION.getNumberOfTechnicalRoles(),
                        ROLES_CONFIGURATION.getNumberOfAssignmentsMin(),
                        ROLES_CONFIGURATION.getNumberOfAssignmentsMax(),
                        ROLES_CONFIGURATION.getNumberOfInducementsMin(),
                        ROLES_CONFIGURATION.getNumberOfInducementsMax(),

                        SCHEMA_CONFIGURATION.getSingleValuedProperties(),
                        SCHEMA_CONFIGURATION.getMultiValuedProperties(),
                        SCHEMA_CONFIGURATION.getIndexedPercentage(),

                        IMPORTS_CONFIGURATION.getThreads(),
                        RECONCILIATIONS_CONFIGURATION.getThreads(),
                        RECOMPUTATION_CONFIGURATION.getThreads()));
    }

    @Override
    protected boolean isAvoidLoggingChange() {
        return false; // we want logging from our system config
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
    public void test000LogStart() {

        testMonitor().addReportSection(REPORT_SECTION_SUMMARY_NAME)
                .withColumns(summaryReportHeader.toArray(new String[0]))
                .addRow(summaryReportDataRow.toArray());

        List<String> taskExecutionHeader = Arrays.asList("task", "time", "timePerAccount");
        taskExecutionReportSection = testMonitor().addReportSection(REPORT_SECTION_TASK_EXECUTION_NAME)
                .withColumns(taskExecutionHeader.toArray(new String[0]));

        taskExecutionDenormalizedReportSection = testMonitor().addReportSection(REPORT_SECTION_TASK_EXECUTION_DENORMALIZED_NAME)
                .withColumns(ListUtils.union(summaryReportHeader, taskExecutionHeader).toArray(new String[0]));

        logger.info("********** STARTED **********\n");
        logger.info("Extension schema: {}", SCHEMA_CONFIGURATION);
        logger.info("Sources: {}", SOURCES_CONFIGURATION);
        logger.info("Targets: {}", TARGETS_CONFIGURATION);
        logger.info("Roles: {}", ROLES_CONFIGURATION);
        logger.info("Import: {}", IMPORTS_CONFIGURATION);
        logger.info("Reconciliation: {}", RECONCILIATIONS_CONFIGURATION);
        logger.info("Recomputation: {}", RECOMPUTATION_CONFIGURATION);
        logger.info("Progress file: {}", ProgressOutputFile.FILE);

        summaryOutputFile.logStart();
    }

    @Test
    public void test001DumpExtensionSchema() {
        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismContainerDefinition<?> userExtDef = userDef.getExtensionDefinition();
        System.out.println(userExtDef.debugDump());
    }

    @Test
    public void test100Import() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        int usersBefore = repositoryService.countObjects(UserType.class, null, null, result);

        String label = "initial-run-of-";

        for (int taskIndex = 0; taskIndex < TASK_IMPORT_LIST.size(); taskIndex++) {
            String importName = "import #" + taskIndex;

            when(importName);

            TestResource<TaskType> taskImport = TASK_IMPORT_LIST.get(taskIndex);

            lastProgress = 0;
            addTask(taskImport.file);
            waitForTaskFinish(taskImport.oid, false, 0, OTHER_PARAMETERS.taskTimeout, false, 0,
                    builder -> builder.taskConsumer(task1 -> recordProgress(label, task1)));

            then(importName);

            // Note: after first import the number of users should be constant
            int usersAfter = repositoryService.countObjects(UserType.class, null, null, result);
            assertThat(usersAfter - usersBefore)
                    .as("users after " + importName)
                    .isEqualTo(SOURCES_CONFIGURATION.getNumberOfAccounts());

            PrismObject<TaskType> taskAfter = assertTask(taskImport.oid, "after")
                    .display()
                    .getObject();

            logTaskFinish(taskAfter, label);
        }

        String accountName = SourceInitializer.getAccountName(0);

        DummyAccount account = RESOURCE_SOURCE_LIST.get(0).controller.getDummyResource().getAccountByUsername(accountName);
        Set<String> roles = account.getAttributeValues(SourcesConfiguration.A_ROLE, String.class);
        displayValue("Roles for " + accountName, roles);

        Set<String> memberships = RESOURCE_TARGET_LIST.stream()
                .flatMap(r -> emptyIfNull(getMemberships(accountName, r)).stream())
                .collect(Collectors.toSet());
        displayValue("Memberships for " + accountName, memberships);

        Set<String> technicalRoles = memberships.stream()
                .map(this::getTechnicalRoleName)
                .collect(Collectors.toSet());
        displayValue("Technical roles for " + accountName, technicalRoles);

        PrismObject<UserType> user = assertUserAfterByUsername(accountName)
                .assertAssignments(roles.size() + 1) // 1. archetype
                .assertLinks(SOURCES_CONFIGURATION.getNumberOfResources() + TARGETS_CONFIGURATION.getNumberOfResources())
                .extension()
                    .assertSize(SOURCES_CONFIGURATION.getSingleValuedMappings() + SOURCES_CONFIGURATION.getMultiValuedMappings())
                    .property(ItemPath.create(getSingleValuedPropertyName(0)))
                        .assertSize(1)
                        .end()
                    .property(ItemPath.create(getMultiValuedPropertyName(0)))
                        .assertSize(SOURCES_CONFIGURATION.getAttributeValues() * SOURCES_CONFIGURATION.getNumberOfResources())
                        .end()
                    .end()
                .getObject();

        // temporarily disabled
//        if (TARGETS_CONFIGURATION.getNumberOfResources() > 0) {
//            assertThat(user.asObjectable().getRoleMembershipRef().size())
//                    .as("# of role membership refs")
//                    .isEqualTo(roles.size() + technicalRoles.size() + 2); // 1. archetype, 2. role-targets)
//        }
    }

    private String getTechnicalRoleName(String membership) {
        Matcher matcher = Pattern.compile("business-[0-9]+-(technical-[0-9]+)").matcher(membership);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private Set<String> getMemberships(String accountName, DummyTestResource r) {
        try {
            return r.controller.getDummyResource()
                    .getAccountByUsername(accountName)
                    .getAttributeValues(TargetsConfiguration.A_MEMBERSHIP, String.class);
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }

    @Test
    public void test110ImportAgain() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        int usersBefore = repositoryService.countObjects(UserType.class, null, null, result);

        for (int retry = 0; retry < IMPORTS_CONFIGURATION.getNoOpRuns(); retry++) {

            String label = String.format("no-op-run-%d-of-", retry + 1);

            for (int taskIndex = 0; taskIndex < TASK_IMPORT_LIST.size(); taskIndex++) {
                String importName = String.format("re-import #%d of resource #%d", retry+1, taskIndex);

                when(importName);

                TestResource<TaskType> taskImport = TASK_IMPORT_LIST.get(taskIndex);

                lastProgress = 0;
                restartTask(taskImport.oid, result);
                Thread.sleep(500);

                waitForTaskFinish(taskImport.oid, false, 0, OTHER_PARAMETERS.taskTimeout, false, 0,
                        builder -> builder.taskConsumer(task1 -> recordProgress(label, task1)));

                then(importName);

                int usersAfter = repositoryService.countObjects(UserType.class, null, null, result);
                assertThat(usersAfter - usersBefore).as("users created").isEqualTo(0);

                PrismObject<TaskType> taskAfter = assertTask(taskImport.oid, "after")
                        .display()
                        .getObject();

                logTaskFinish(taskAfter, label);
            }
        }
    }

    @Test
    public void test120Reconciliation() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        for (int run = 0; run < RECONCILIATIONS_CONFIGURATION.getRuns(); run++) {
            String label = String.format("run-%d-of-", run + 1);

            for (int taskIndex = 0; taskIndex < TASK_RECONCILIATION_LIST.size(); taskIndex++) {
                String importName = String.format("reconciliation #%d of resource #%d", run+1, taskIndex);

                when(importName);

                TestResource<TaskType> reconTask = TASK_RECONCILIATION_LIST.get(taskIndex);

                lastProgress = 0;
                if (run == 0) {
                    addTask(reconTask.file);
                } else {
                    restartTask(reconTask.oid, result);
                    Thread.sleep(500);
                }

                waitForTaskFinish(reconTask.oid, false, 0, OTHER_PARAMETERS.taskTimeout, false, 0,
                        builder -> builder.taskConsumer(task1 -> recordProgress(label, task1)));

                then(importName);

                PrismObject<TaskType> taskAfter = assertTask(reconTask.oid, "after")
                        .display()
                        .getObject();

                logTaskFinish(taskAfter, label);
            }
        }
    }

    @Test
    public void test130RecomputeUsers() throws Exception {
        given();

        when();

        lastProgress = 0;
        addTask(TASK_RECOMPUTE.file);
        waitForTaskFinish(TASK_RECOMPUTE.oid, false, 0, OTHER_PARAMETERS.taskTimeout, false, 0,
                builder -> builder.taskConsumer(task1 -> recordProgress("", task1)));

        then();

        PrismObject<TaskType> taskAfter = assertTask(TASK_RECOMPUTE.oid, "after")
                .display()
                .getObject();

        logTaskFinish(taskAfter, "");
    }

    private long getExecutionTime(PrismObject<TaskType> taskAfter) {
        long start = XmlTypeConverter.toMillis(taskAfter.asObjectable().getLastRunStartTimestamp());
        long end = XmlTypeConverter.toMillis(taskAfter.asObjectable().getLastRunFinishTimestamp());
        return end - start;
    }

    private String getSingleValuedPropertyName(int i) {
        return String.format("p-single-%04d", i);
    }

    private String getMultiValuedPropertyName(int i) {
        return String.format("p-multi-%04d", i);
    }

    private void logTaskFinish(PrismObject<TaskType> taskAfter, String label) {
        String desc = label + taskAfter.getName().getOrig();

        long executionTime = getExecutionTime(taskAfter);
        double timePerAccount = (double) executionTime / (double) SOURCES_CONFIGURATION.getNumberOfAccounts();

        logger.info("********** FINISHED: {} **********\n", desc);
        logger.info(String.format("Task execution time: %,d ms", executionTime));
        logger.info(String.format("Time per account: %,.1f ms", timePerAccount));
        logger.info(StatisticsUtil.format(taskAfter.asObjectable().getOperationStats()));

        summaryOutputFile.logTaskFinish(desc, executionTime, timePerAccount);
        detailsOutputFile.logTaskFinish(desc, taskAfter.asObjectable());

        List<Object> dataRow = Arrays.asList(desc, executionTime, timePerAccount);
        taskExecutionReportSection
                .addRow(dataRow.toArray());
        taskExecutionDenormalizedReportSection
                .addRow(ListUtils.union(summaryReportDataRow, dataRow).toArray());
    }

    private void recordProgress(String label, Task task) {
        Long start = task.getLastRunStartTimestamp();
        long progress = task.getProgress();

        if (start == null || progress == lastProgress) {
            return;
        }
        lastProgress = progress;

        progressOutputFile.recordProgress(label, task);

        OperationStatsType stats = task.getStoredOperationStats();
        logger.info("\n{}", StatisticsUtil.format(stats));
    }
}
