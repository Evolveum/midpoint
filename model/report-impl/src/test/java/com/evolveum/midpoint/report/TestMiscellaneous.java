/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.report;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.model.test.CommonInitialObjects.*;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.report.api.ReportManager;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.TrustDescriptorSetter;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.TestReport;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Tests e.g. {@link ReportManager#runReport(PrismObject, PrismContainer, Task, OperationResult)} method - besides others.
 */
@ContextConfiguration(locations = { "classpath:ctx-report-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestMiscellaneous extends EmptyReportIntegrationTest {

    private static final File TEST_DIR = new File(TEST_RESOURCES_DIR, "misc");

    private static final TestReport REPORT_EXPORT_USERS = TestReport.file(
            TEST_DIR, "report-export-users.xml", "63665d73-9829-4064-a8ec-0a04c554ebcc");
    private static final TestReport REPORT_IMPORT_USERS = TestReport.file(
            TEST_DIR, "report-import-users.xml", "54319c28-cc40-4005-bac3-a5a9dbabd91b");
    private static final TestReport REPORT_INDIRECT_ASSIGNMENTS_BUILT_IN = TestReport.classPath(
            REPORTS, "200-report-indirect-assignments.xml", "00000000-0000-0000-0000-000000000200");

    private static final String IGA_OWNER_OID = "00000000-0000-0000-0000-000000012798";
    private static final String IGA_LEGACY_OWNER_OID = "00000000-0000-0000-0000-000000012796";
    private static final String IGA_NO_CHANNEL_OWNER_OID = "00000000-0000-0000-0000-000000012797";
    private static final String IGA_BUSINESS_ROLE_OID = "00000000-0000-0000-0001-000000012798";
    private static final String IGA_TARGET_ROLE_OID = "00000000-0000-0000-0002-000000012798";

    private static final List<String> IGA_DATA_COLUMNS =
            List.of("user", "allPath", "parent", "activation", "validTo", "since", "createChannel");
    private static final List<String> CERTIFICATION_WORK_ITEM_COLUMNS = List.of(
            "iteration", "stageNumber", "originalAssigneeRef", "deadline", "assigneeRef", "escalationLevel",
            "performerRef", "outcome", "comment", "outputChangeTimestamp", "closeTimestamp");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initTestObjects(initTask, initResult,
                REPORT_EXPORT_USERS,
                REPORT_IMPORT_USERS);
    }

    /**
     * Checking that {@link ReportManager#runReport(PrismObject, PrismContainer, Task, OperationResult)} correctly
     * applies the archetypes, e.g. their mappings.
     *
     * MID-8364
     */
    @Test
    public void test100RunExport() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        when("report export is run");
        reportManager.runReport(REPORT_EXPORT_USERS.get(), null, task, result);

        then("the task has archetype mappings applied");
        var taskOid = Objects.requireNonNull(result.findTaskOid(), "no task OID");
        waitForTaskCloseOrSuspend(taskOid);
        assertTask(taskOid, "after")
                .display()
                .assertArchetypeRefs(2)
                .assertHasArchetype(ARCHETYPE_TASK_REPORT_EXPORT_CLASSIC.oid)
                .assertHasArchetype(ARCHETYPE_TASK_REPORT.oid)
                .asTask()
                .assertClosed()
                .assertSuccess();
    }

    /**
     * As {@link #test100RunExport()} but for import method {@link ReportManager#importReport(PrismObject, PrismObject,
     * Task, OperationResult)}.
     *
     * MID-8364
     */
    @Test
    public void test110RunImport() throws Exception {
        var task = getTestTask();
        var result = task.getResult();

        given("user data file");
        ReportDataType data = new ReportDataType()
                .name("users-to-import")
                .fileFormat(FileFormatTypeType.CSV)
                .filePath(new File(TEST_DIR, "users-to-import.csv").getAbsolutePath());
        addObject(data, task, result);

        when("report import is run");
        reportManager.importReport(
                REPORT_IMPORT_USERS.get(),
                data.asPrismObject(),
                task, result);

        then("the task has archetype mappings applied");
        var taskOid = Objects.requireNonNull(result.findTaskOid(), "no task OID");
        waitForTaskCloseOrSuspend(taskOid);
        assertTask(taskOid, "after")
                .display()
                .assertArchetypeRefs(2)
                .assertHasArchetype(ARCHETYPE_TASK_REPORT_IMPORT_CLASSIC.oid)
                .assertHasArchetype(ARCHETYPE_TASK_REPORT.oid)
                .asTask()
                .assertClosed()
                .assertSuccess();

        and("user is imported");
        assertUserAfterByUsername("jim")
                .assertFullName("Jim Hacker");
    }

    @Override
    protected FileFormatConfigurationType getFileFormatConfiguration() {
        return null; // unused
    }

    /**
     * MID-10659
     *
     * Chained subreports variables, different code path via {@link ReportManager#evaluateSubreportParameters} used in GUI
     */
    @Test
    public void testSubreportsVariables() {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_OBJECT, new AuditEventRecordType(), AuditEventRecordType.class);

        reportManager.evaluateSubreportParameters(REPORT_SUBREPORT_AUDIT.get(),variables, task, result);

        assertSuccess(result);
    }

    /** MID-12798: IGA columns accept both export scalars and preview collections, evaluating each path independently. */
    @Test
    public void testIgaDataColumnExpressions() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        addObject(new RoleType().oid(IGA_BUSINESS_ROLE_OID).name("iga-business-role"), task, result);
        addObject(new RoleType().oid(IGA_TARGET_ROLE_OID).name("iga-target-role"), task, result);
        addObject(new UserType()
                .oid(IGA_OWNER_OID)
                .name("iga-owner")
                .assignment(igaAssignment(101L, IGA_TARGET_ROLE_OID))
                .assignment(igaAssignment(102L, IGA_BUSINESS_ROLE_OID, SchemaConstants.CHANNEL_REST_URI)), task, result);
        repositoryService.addObject(new UserType()
                .oid(IGA_LEGACY_OWNER_OID)
                .name("iga-legacy-owner")
                .assignment(igaLegacyAssignment(103L, IGA_TARGET_ROLE_OID))
                .assignment(igaEmptyModernMetadataWithLegacyChannelAssignment(105L, IGA_TARGET_ROLE_OID))
                .assignment(igaModernAndLegacyChannelAssignment(106L, IGA_TARGET_ROLE_OID))
                .assignment(igaMultipleModernMetadataAssignment(107L, IGA_TARGET_ROLE_OID))
                .asPrismObject(), null, result);
        repositoryService.addObject(new UserType()
                .oid(IGA_NO_CHANNEL_OWNER_OID)
                .name("iga-no-channel-owner")
                .assignment(igaAssignmentWithoutChannel(104L, IGA_TARGET_ROLE_OID))
                .asPrismObject(), null, result);

        ValueMetadataType direct = igaMetadata(101L, IGA_TARGET_ROLE_OID);
        ValueMetadataType indirect = igaMetadata(102L, IGA_BUSINESS_ROLE_OID, IGA_TARGET_ROLE_OID);
        ValueMetadataType legacy = igaMetadata(IGA_LEGACY_OWNER_OID, 103L, IGA_TARGET_ROLE_OID);
        ValueMetadataType noChannel = igaMetadata(IGA_NO_CHANNEL_OWNER_OID, 104L, IGA_TARGET_ROLE_OID);
        ValueMetadataType emptyModernWithLegacy = igaMetadata(IGA_LEGACY_OWNER_OID, 105L, IGA_TARGET_ROLE_OID);
        ValueMetadataType modernAndLegacy = igaMetadata(IGA_LEGACY_OWNER_OID, 106L, IGA_TARGET_ROLE_OID);
        ValueMetadataType multipleModern = igaMetadata(IGA_LEGACY_OWNER_OID, 107L, IGA_TARGET_ROLE_OID);

        Map<String, List<Object>> scalar = evaluateColumns(
                REPORT_INDIRECT_ASSIGNMENTS_BUILT_IN, IGA_DATA_COLUMNS, "data", direct, ValueMetadataType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> indirectScalar = evaluateColumns(
                REPORT_INDIRECT_ASSIGNMENTS_BUILT_IN, IGA_DATA_COLUMNS, "data", indirect, ValueMetadataType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> singleton = evaluateColumns(
                REPORT_INDIRECT_ASSIGNMENTS_BUILT_IN, IGA_DATA_COLUMNS, "data", List.of(direct.asPrismContainerValue()),
                ValueMetadataType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> multiple = evaluateColumns(
                REPORT_INDIRECT_ASSIGNMENTS_BUILT_IN, IGA_DATA_COLUMNS, "data",
                List.of(direct.asPrismContainerValue(), indirect.asPrismContainerValue()),
                ValueMetadataType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> empty = evaluateColumns(
                REPORT_INDIRECT_ASSIGNMENTS_BUILT_IN, IGA_DATA_COLUMNS, "data", List.of(), ValueMetadataType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> legacyScalar = evaluateColumns(
                REPORT_INDIRECT_ASSIGNMENTS_BUILT_IN, IGA_DATA_COLUMNS, "data", legacy, ValueMetadataType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> noChannelScalar = evaluateColumns(
                REPORT_INDIRECT_ASSIGNMENTS_BUILT_IN, IGA_DATA_COLUMNS, "data", noChannel, ValueMetadataType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> emptyModernWithLegacyScalar = evaluateColumns(
                REPORT_INDIRECT_ASSIGNMENTS_BUILT_IN, IGA_DATA_COLUMNS, "data", emptyModernWithLegacy,
                ValueMetadataType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> modernAndLegacyScalar = evaluateColumns(
                REPORT_INDIRECT_ASSIGNMENTS_BUILT_IN, IGA_DATA_COLUMNS, "data", modernAndLegacy,
                ValueMetadataType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> multipleModernScalar = evaluateColumns(
                REPORT_INDIRECT_ASSIGNMENTS_BUILT_IN, IGA_DATA_COLUMNS, "data", multipleModern,
                ValueMetadataType.COMPLEX_TYPE, task, result);

        assertThat(singleton).isEqualTo(scalar);
        assertThat(scalar.get("allPath")).containsExactly("iga-target-role");
        assertThat(scalar.get("parent")).containsExactly("Direct");
        assertThat(scalar.get("createChannel")).containsExactly("User");
        assertThat(indirectScalar.get("allPath")).containsExactly("iga-business-role -> iga-target-role");
        assertThat(indirectScalar.get("parent")).containsExactly("iga-business-role");
        assertThat(indirectScalar.get("createChannel")).containsExactly("REST");
        assertThat(multiple.get("allPath"))
                .containsExactlyInAnyOrder("iga-target-role", "iga-business-role -> iga-target-role");
        assertThat(multiple.get("parent")).containsExactlyInAnyOrder("Direct", "iga-business-role");
        assertThat(multiple.get("createChannel")).containsExactlyInAnyOrder("User", "REST");
        assertThat(empty.get("user")).containsExactly("Unknown owner");
        assertThat(empty.get("allPath")).containsExactly("?");
        assertThat(empty.get("parent")).containsExactly("?");
        assertThat(empty.get("createChannel")).isEmpty();
        assertThat(legacyScalar.get("createChannel")).containsExactly("User");
        assertThat(noChannelScalar.get("createChannel")).isEmpty();
        assertThat(emptyModernWithLegacyScalar.get("createChannel")).containsExactly("REST");
        assertThat(modernAndLegacyScalar.get("createChannel")).containsExactly("User");
        assertThat(multipleModernScalar.get("createChannel")).containsExactly("User");

        assertSuccess(result);
    }

    /** MID-12798: Certification preview consistently uses the first work item. */
    @Test
    public void testCertificationWorkItemColumnExpressions() {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        AccessCertificationWorkItemType first = certificationWorkItem(
                1, 11, "first", SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT);
        AccessCertificationWorkItemType second = certificationWorkItem(
                2, 22, "second", SchemaConstants.MODEL_CERTIFICATION_OUTCOME_REVOKE);

        Map<String, List<Object>> scalar = evaluateColumns(
                REPORT_CERTIFICATION_WORK_ITEMS, CERTIFICATION_WORK_ITEM_COLUMNS,
                "workItems", first, AccessCertificationWorkItemType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> singleton = evaluateColumns(
                REPORT_CERTIFICATION_WORK_ITEMS, CERTIFICATION_WORK_ITEM_COLUMNS,
                "workItems", List.of(first.asPrismContainerValue()),
                AccessCertificationWorkItemType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> multiple = evaluateColumns(
                REPORT_CERTIFICATION_WORK_ITEMS, CERTIFICATION_WORK_ITEM_COLUMNS,
                "workItems", List.of(first.asPrismContainerValue(), second.asPrismContainerValue()),
                AccessCertificationWorkItemType.COMPLEX_TYPE, task, result);
        Map<String, List<Object>> empty = evaluateColumns(
                REPORT_CERTIFICATION_WORK_ITEMS, CERTIFICATION_WORK_ITEM_COLUMNS,
                "workItems", List.of(), AccessCertificationWorkItemType.COMPLEX_TYPE, task, result);

        assertThat(singleton).isEqualTo(scalar);
        assertThat(multiple).isEqualTo(scalar);
        assertThat(multiple.get("escalationLevel")).extracting(String::valueOf).containsExactly("1");
        assertThat(multiple.get("comment")).containsExactly("first-comment");
        assertThat(empty.values()).allSatisfy(values -> assertThat(values).isEmpty());
        assertSuccess(result);
    }

    private AssignmentType igaAssignment(long id, String targetOid) throws SchemaException {
        return igaAssignment(id, targetOid, SchemaConstants.CHANNEL_USER_URI);
    }

    private AssignmentType igaAssignment(long id, String targetOid, String channel) throws SchemaException {
        AssignmentType assignment = baseIgaAssignment(id, targetOid);
        assignment.asPrismContainerValue().setValueMetadata(new ValueMetadataType()
                .storage(new StorageMetadataType().createChannel(channel)));
        return assignment;
    }

    private AssignmentType igaAssignmentWithoutChannel(long id, String targetOid) throws SchemaException {
        AssignmentType assignment = baseIgaAssignment(id, targetOid);
        assignment.asPrismContainerValue().setValueMetadata(new ValueMetadataType()
                .storage(new StorageMetadataType().createTimestamp("2026-01-01T00:00:00Z")));
        return assignment;
    }

    private AssignmentType igaLegacyAssignment(long id, String targetOid) {
        return baseIgaAssignment(id, targetOid)
                .metadata(new MetadataType().createChannel(SchemaConstants.CHANNEL_USER_URI));
    }

    private AssignmentType igaEmptyModernMetadataWithLegacyChannelAssignment(long id, String targetOid) throws SchemaException {
        AssignmentType assignment = baseIgaAssignment(id, targetOid)
                .metadata(new MetadataType().createChannel(SchemaConstants.CHANNEL_REST_URI));
        addIgaValueMetadata(assignment, null);
        return assignment;
    }

    private AssignmentType igaModernAndLegacyChannelAssignment(long id, String targetOid) throws SchemaException {
        AssignmentType assignment = baseIgaAssignment(id, targetOid)
                .metadata(new MetadataType().createChannel(SchemaConstants.CHANNEL_REST_URI));
        addIgaValueMetadata(assignment, SchemaConstants.CHANNEL_USER_URI);
        return assignment;
    }

    private AssignmentType igaMultipleModernMetadataAssignment(long id, String targetOid) throws SchemaException {
        AssignmentType assignment = baseIgaAssignment(id, targetOid)
                .metadata(new MetadataType().createChannel(SchemaConstants.CHANNEL_REST_URI));
        addIgaValueMetadata(assignment, null);
        addIgaValueMetadata(assignment, SchemaConstants.CHANNEL_USER_URI);
        return assignment;
    }

    private void addIgaValueMetadata(AssignmentType assignment, String createChannel) throws SchemaException {
        StorageMetadataType storage = new StorageMetadataType().createTimestamp("2026-01-01T00:00:00Z");
        if (createChannel != null) {
            storage.createChannel(createChannel);
        }
        assignment.asPrismContainerValue().getValueMetadata().addMetadataValue(
                new ValueMetadataType().storage(storage).asPrismContainerValue());
    }

    private AssignmentType baseIgaAssignment(long id, String targetOid) {
        return new AssignmentType()
                .id(id)
                .targetRef(targetOid, RoleType.COMPLEX_TYPE)
                .activation(new ActivationType()
                        .effectiveStatus(ActivationStatusType.ENABLED)
                        .validTo("2030-01-01T00:00:00Z"));
    }

    private ValueMetadataType igaMetadata(long assignmentId, String... pathTargetOids) {
        return igaMetadata(IGA_OWNER_OID, assignmentId, pathTargetOids);
    }

    private ValueMetadataType igaMetadata(String ownerOid, long assignmentId, String... pathTargetOids) {
        AssignmentPathMetadataType path = new AssignmentPathMetadataType().sourceRef(ownerOid, UserType.COMPLEX_TYPE);
        for (int i = 0; i < pathTargetOids.length; i++) {
            path.beginSegment()
                    .segmentOrder(i + 1)
                    .assignmentId(i == 0 ? assignmentId : null)
                    .targetRef(pathTargetOids[i], RoleType.COMPLEX_TYPE);
        }
        return new ValueMetadataType()
                .provenance(new ProvenanceMetadataType().assignmentPath(path))
                .storage(new StorageMetadataType().createTimestamp("2026-01-01T00:00:00Z"));
    }

    private AccessCertificationWorkItemType certificationWorkItem(int iteration, int stage, String marker, String outcome) {
        return new AccessCertificationWorkItemType()
                .iteration(iteration)
                .stageNumber(stage)
                .originalAssigneeRef(namedUserReference(marker + "-original"))
                .deadline("2026-01-0" + iteration + "T10:00:00Z")
                .assigneeRef(namedUserReference(marker + "-assignee-1"))
                .assigneeRef(namedUserReference(marker + "-assignee-2"))
                .escalationLevel(new WorkItemEscalationLevelType().number(iteration))
                .performerRef(namedUserReference(marker + "-performer"))
                .output(new AbstractWorkItemOutputType().outcome(outcome).comment(marker + "-comment"))
                .outputChangeTimestamp("2026-02-0" + iteration + "T10:00:00Z")
                .closeTimestamp("2026-03-0" + iteration + "T10:00:00Z");
    }

    private ObjectReferenceType namedUserReference(String name) {
        return new ObjectReferenceType()
                .oid(name)
                .type(UserType.COMPLEX_TYPE)
                .targetName(createPolyStringType(name));
    }

    private Map<String, List<Object>> evaluateColumns(TestReport report, List<String> columnNames, String variableName, Object variableValue,
            QName variableType, Task task, OperationResult result) {
        PrismObject<ReportType> reportObject = report.get();
        TrustDescriptorSetter.setDescriptors(reportObject.asObjectable(), IntegrationTestTools.trustedForTests());
        VariablesMap variables = new VariablesMap();
        if (variableValue instanceof Collection<?> collection) {
            @SuppressWarnings("unchecked")
            Collection<? extends PrismValue> prismValues = (Collection<? extends PrismValue>) collection;
            variables.put(variableName, TypedValue.of(prismValues, variableType));
        } else {
            variables.put(variableName, TypedValue.of(variableValue, Object.class));
        }
        return columnNames.stream()
                .collect(Collectors.toMap(
                        name -> name,
                        name -> evaluateColumn(reportObject, name, variables, task, result),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private List<Object> evaluateColumn(PrismObject<ReportType> report, String columnName, VariablesMap variables, Task task, OperationResult result) {
        GuiObjectColumnType column = report.asObjectable().getObjectCollection().getView().getColumn().stream()
                .filter(candidate -> columnName.equals(candidate.getName()))
                .findFirst()
                .orElseThrow();
        try {
            Collection<? extends PrismValue> values = (Collection<? extends PrismValue>) reportManager.evaluateScript(
                    report, column.getExport().getExpression(), variables, "column '" + columnName + "'", task, result);
            return values.stream()
                    .map(PrismValue::getRealValue)
                    .toList();
        } catch (Exception e) {
            throw new AssertionError("Couldn't evaluate column '" + columnName + "'", e);
        }
    }
}
