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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityPerformanceInformation;
import com.evolveum.midpoint.test.util.TestReportUtil;

import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.commons.collections4.ListUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.task.TaskOperationStatsUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.testing.story.AbstractStoryTest;
import com.evolveum.midpoint.tools.testng.PerformanceTestClassMixin;
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
public class TestSystemPerformance extends AbstractStoryTest implements PerformanceTestClassMixin {

    private static final Trace LOGGER = TraceManager.getTrace(TestSystemPerformance.class);

    public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "system-perf");
    static final File TARGET_DIR = new File(TARGET_DIR_PATH);

    private static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/test/system-perf";
    private static final ItemName EXT_MEMBER_OF = new ItemName(NS_EXT, "memberOf");

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

    private static final TestObject<ArchetypeType> ARCHETYPE_BASIC_USER =
            TestObject.file(TEST_DIR, "archetype-basic-user.xml", "463e21c5-9959-48e9-bc2a-5356eafb0589");

    private static final TestObject<RoleType> METAROLE_TECHNICAL =
            TestObject.file(TEST_DIR, "metarole-technical.xml", "7c359aa0-d798-4781-a58b-d6336cb9b1ee");

    static final TestObject<RoleType> ROLE_TARGETS = TestObject.file(TARGET_DIR, "generated-role-targets.xml", "3b65aad7-7d6b-412e-bfc7-2cee44d22c32");
    private static final TestObject<RoleType> TEMPLATE_USER = TestObject.file(TEST_DIR, "template-user.xml", "0c77fde5-4ad5-49ce-8ee9-14f330660d8e");

    private static final List<TestObject<RoleType>> BUSINESS_ROLE_LIST;
    private static final List<TestObject<RoleType>> TECHNICAL_ROLE_LIST;
    private static final List<TestObject<TaskType>> TASK_IMPORT_LIST;
    private static final List<TestObject<TaskType>> TASK_RECONCILIATION_LIST;
    private static final TestObject<TaskType> TASK_RECOMPUTE;

    static final long START = System.currentTimeMillis();

    private static final String REPORT_SECTION_SUMMARY_NAME = "summary";
    private static final String REPORT_SECTION_TASK_EXECUTION_NAME = "taskExecution";
    private static final String REPORT_SECTION_TASK_EXECUTION_DENORMALIZED_NAME = "taskExecutionDenormalized";

    private TestReportSection taskExecutionReportSection;
    private TestReportSection taskExecutionDenormalizedReportSection;

    private final ProgressOutputFile progressOutputFile = new ProgressOutputFile();
    private final SummaryOutputFile summaryOutputFile = new SummaryOutputFile();
    private final DetailsOutputFile detailsOutputFile = new DetailsOutputFile();
    private final TaskDumper taskDumper = new TaskDumper();

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

        System.setProperty(PERF_REPORT_PREFIX_PROPERTY_NAME, createReportFilePrefix());
    }

    private static String createReportFilePrefix() {
        return TARGET_DIR_PATH + "/" + START + "-" + OTHER_PARAMETERS.label + "-report";
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
        if (ROLES_CONFIGURATION.isMemberOfComputation()) {
            repoAdd(METAROLE_TECHNICAL, initResult);
        }

        for (TestObject<?> resource : TECHNICAL_ROLE_LIST) {
            addObject(resource, initTask, initResult); // creates resource objects
        }

        for (TestObject<?> resource : BUSINESS_ROLE_LIST) {
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
                        "memberOfComputation",
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
                        ROLES_CONFIGURATION.isMemberOfComputation(),

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

            TestObject<TaskType> taskImport = TASK_IMPORT_LIST.get(taskIndex);

            lastProgress = 0;
            addTask(taskImport, result);
            waitForTaskFinish(taskImport.oid, 0, OTHER_PARAMETERS.taskTimeout, false, 0,
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

            logTaskFinish(taskAfter, label, result);
            taskDumper.dumpTask(taskAfter, getTestNameShort());
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

        boolean memberOf = ROLES_CONFIGURATION.isMemberOfComputation();
        PrismObject<UserType> user = assertUserAfterByUsername(accountName)
                .assertAssignments(roles.size() + 1) // 1. archetype
                .assertLinks(SOURCES_CONFIGURATION.getNumberOfResources() + TARGETS_CONFIGURATION.getNumberOfResources(), 0)
                .extension()
                    .assertSize(SOURCES_CONFIGURATION.getSingleValuedMappings() +
                            SOURCES_CONFIGURATION.getMultiValuedMappings() +
                            (memberOf ? 1 : 0))
                    .property(getSingleValuedPropertyQName(0))
                        .assertSize(1)
                        .end()
                    .property(getMultiValuedPropertyQName(0))
                        .assertSize(SOURCES_CONFIGURATION.getAttributeValues() * SOURCES_CONFIGURATION.getNumberOfResources())
                        .end()
                    .end()
                .getObject();

        if (memberOf) {
            Collection<String> memberOfValue = ObjectTypeUtil.getExtensionPropertyValues(user.asObjectable(), EXT_MEMBER_OF);
            displayValue("memberOf", memberOfValue);
            assertThat(memberOfValue).as("memberOf").hasSize(memberships.size());
        }

        LOGGER.info("user:\n{}", prismContext.xmlSerializer().serialize(user));

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

                TestObject<TaskType> taskImport = TASK_IMPORT_LIST.get(taskIndex);

                lastProgress = 0;
                restartTask(taskImport.oid, result);
                Thread.sleep(500);

                waitForTaskFinish(taskImport.oid, 0, OTHER_PARAMETERS.taskTimeout, false, 0,
                        builder -> builder.taskConsumer(task1 -> recordProgress(label, task1)));

                then(importName);

                int usersAfter = repositoryService.countObjects(UserType.class, null, null, result);
                assertThat(usersAfter - usersBefore).as("users created").isEqualTo(0);

                PrismObject<TaskType> taskAfter = assertTask(taskImport.oid, "after")
                        .display()
                        .getObject();

                logTaskFinish(taskAfter, label, result);
                taskDumper.dumpTask(taskAfter, getTestNameShort());
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

                TestObject<TaskType> reconTask = TASK_RECONCILIATION_LIST.get(taskIndex);

                lastProgress = 0;
                if (run == 0) {
                    addTask(reconTask, result);
                } else {
                    restartTask(reconTask.oid, result);
                    Thread.sleep(500);
                }

                waitForTaskFinish(reconTask.oid, 0, OTHER_PARAMETERS.taskTimeout, false, 0,
                        builder -> builder.taskConsumer(task1 -> recordProgress(label, task1)));

                then(importName);

                PrismObject<TaskType> taskAfter = assertTask(reconTask.oid, "after")
                        .display()
                        .getObject();

                logTaskFinish(taskAfter, label, result);
                taskDumper.dumpTask(taskAfter, getTestNameShort());
            }
        }
    }

    @Test
    public void test130RecomputeUsers() throws Exception {
        given();

        Task task = getTestTask();
        OperationResult result = task.getResult();

        when();

        lastProgress = 0;
        addTask(TASK_RECOMPUTE, result);
        waitForTaskFinish(TASK_RECOMPUTE.oid, 0, OTHER_PARAMETERS.taskTimeout, false, 0,
                builder -> builder.taskConsumer(task1 -> recordProgress("", task1)));

        then();

        PrismObject<TaskType> taskAfter = assertTask(TASK_RECOMPUTE.oid, "after")
                .display()
                .getObject();

        logTaskFinish(taskAfter, "", result);
        taskDumper.dumpTask(taskAfter, getTestNameShort());
    }

    @Test
    public void test999Finish() {
        logFinish();
    }

    private long getExecutionTime(PrismObject<TaskType> taskAfter) {
        long start = XmlTypeConverter.toMillis(taskAfter.asObjectable().getLastRunStartTimestamp());
        long end = XmlTypeConverter.toMillis(taskAfter.asObjectable().getLastRunFinishTimestamp());
        return end - start;
    }

    @SuppressWarnings("SameParameterValue")
    private ItemName getSingleValuedPropertyQName(int i) {
        return new ItemName(NS_EXT, getSingleValuedPropertyName(i));
    }

    private String getSingleValuedPropertyName(int i) {
        return String.format("p-single-%04d", i);
    }

    @SuppressWarnings("SameParameterValue")
    private ItemName getMultiValuedPropertyQName(int i) {
        return new ItemName(NS_EXT, getMultiValuedPropertyName(i));
    }

    @SuppressWarnings("SameParameterValue")
    private String getMultiValuedPropertyName(int i) {
        return String.format("p-multi-%04d", i);
    }

    private void logTaskFinish(PrismObject<TaskType> taskAfter, String label, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        String desc = label + taskAfter.getName().getOrig();

        TreeNode<ActivityPerformanceInformation> performanceInformation =
                activityManager.getPerformanceInformation(taskAfter.getOid(), result);
        long executionTime = getExecutionTime(taskAfter);
        int executionTimeSeconds = (int) (executionTime / 1000);
        int numberOfAccounts = SOURCES_CONFIGURATION.getNumberOfAccounts();
        double timePerAccount = (double) executionTime / (double) numberOfAccounts;

        logger.info("********** FINISHED: {} **********\n", desc);
        logger.info(String.format("Task execution time: %,d ms", executionTime));
        logger.info(String.format("Time per account: %,.1f ms", timePerAccount));
        logger.info(TaskOperationStatsUtil.format(taskAfter.asObjectable().getOperationStats()));
        logger.info(performanceInformation.debugDump());

        summaryOutputFile.logTaskFinish(desc, executionTime, timePerAccount);
        detailsOutputFile.logTaskFinish(desc, taskAfter.asObjectable(), performanceInformation);

        List<Object> dataRow = Arrays.asList(desc, executionTime, timePerAccount);
        taskExecutionReportSection
                .addRow(dataRow.toArray());
        taskExecutionDenormalizedReportSection
                .addRow(ListUtils.union(summaryReportDataRow, dataRow).toArray());

        TestReportUtil.reportTaskOperationPerformance(
                testMonitor(), desc, taskAfter.asObjectable(), numberOfAccounts, executionTimeSeconds);
        TestReportUtil.reportTaskComponentPerformance(
                testMonitor(), desc, taskAfter.asObjectable(), numberOfAccounts);
        TestReportUtil.reportTaskRepositoryPerformance(
                testMonitor(), desc, taskAfter.asObjectable(), numberOfAccounts, executionTimeSeconds);
        TestReportUtil.reportTaskCachesPerformance(testMonitor(), desc, taskAfter.asObjectable());
        TestReportUtil.reportTaskProvisioningStatistics(testMonitor(), desc, taskAfter.asObjectable());
    }

    private void logFinish() {
        summaryOutputFile.logFinish();
    }

    private void recordProgress(String label, Task task) {
        Long start = task.getLastRunStartTimestamp();
        long progress = task.getLegacyProgress();

        if (start == null || progress == lastProgress) {
            return;
        }
        lastProgress = progress;

        progressOutputFile.recordProgress(label, task);

        OperationStatsType stats = task.getStoredOperationStatsOrClone();
        logger.info("\n{}", TaskOperationStatsUtil.format(stats));

        // TODO remove fake result + remove getting the task!
        OperationResult tempResult = new OperationResult("temp");
        TreeNode<ActivityPerformanceInformation> performanceInformation;
        try {
            performanceInformation = activityManager.getPerformanceInformation(task.getOid(), tempResult);
        } catch (CommonException e) {
            throw new SystemException(e);
        }
        displayDumpable("performance: " + label + task.getName(), performanceInformation);
    }
}
