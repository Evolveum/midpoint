/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
import static com.evolveum.midpoint.util.MiscUtil.asXMLGregorianCalendar;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import java.util.function.Function;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocus;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.filtering.item.PolyStringItemFilterProcessor;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

public class SqaleRepoSearchObjectTest extends SqaleRepoBaseTest {

    private final static String CASE_WI1_ASSIGNEE1_OID = "8cd0ddb2-c84d-11eb-a008-9321c31dd5ce";
    private final static String CASE_WI1_ASSIGNEE2_OID = "f29a15f0-c84d-11eb-a080-67fcdff6f07d";
    private final static String NONEXIST_OID = "f00f00f0-c84d-11eb-867d-234771aa36c5";

    // org structure
    private String org1Oid; // one root
    private String org11Oid;
    private String org111Oid;
    private String org112Oid;
    private String org12Oid;
    private String org2Oid; // second root
    private String org21Oid;
    private String orgXOid; // under two orgs

    private String user1Oid; // user without org
    private String user2Oid; // different user, this one is in org
    private String user3Oid; // another user in org
    private String user4Oid; // another user in org
    private String task1Oid; // task has more attribute type variability
    private String task2Oid; // task has more attribute type variability
    private String shadow1Oid; // ditto
    private String service1Oid; // object with integer attribute
    private String case1Oid; // Closed case, two work items

    // other info used in queries
    private final String creatorOid = UUID.randomUUID().toString();
    private final String modifierOid = UUID.randomUUID().toString();
    private final QName relation1 = QName.valueOf("{https://random.org/ns}rel-1");
    private final QName relation2 = QName.valueOf("{https://random.org/ns}rel-2");

    private ItemDefinition<?> shadowAttributeDefinition;

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

        // org structure
        org1Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-1").asPrismObject(),
                null, result);
        org11Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-1-1")
                        .parentOrgRef(org1Oid, OrgType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        org111Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-1-1-1")
                        .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE, relation2)
                        .subtype("newWorkers")
                        .asPrismObject(),
                null, result);
        org112Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-1-1-2")
                        .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE, relation1)
                        .subtype("secret")
                        .asPrismObject(),
                null, result);
        org12Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-1-2")
                        .parentOrgRef(org1Oid, OrgType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        org2Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-2").asPrismObject(),
                null, result);
        org21Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-2-1")
                        .costCenter("5")
                        .parentOrgRef(org2Oid, OrgType.COMPLEX_TYPE)
                        .policySituation("situationC")
                        .asPrismObject(),
                null, result);
        orgXOid = repositoryService.addObject(
                new OrgType(prismContext).name("org-X")
                        .parentOrgRef(org12Oid, OrgType.COMPLEX_TYPE)
                        .parentOrgRef(org21Oid, OrgType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER)
                        .asPrismObject(),
                null, result);

        // other objects
        UserType user1 = new UserType(prismContext).name("user-1")
                .metadata(new MetadataType()
                        .creatorRef(creatorOid, UserType.COMPLEX_TYPE, relation1)
                        .createChannel("create-channel")
                        .createTimestamp(asXMLGregorianCalendar(1L))
                        .modifierRef(modifierOid, UserType.COMPLEX_TYPE, relation2)
                        .modifyChannel("modify-channel")
                        .modifyTimestamp(asXMLGregorianCalendar(2L)))
                .subtype("workerA")
                .subtype("workerC")
                .policySituation("situationA")
                .policySituation("situationC")
                .activation(new ActivationType(prismContext)
                        .validFrom("2021-07-04T00:00:00Z")
                        .validTo("2022-07-04T00:00:00Z"))
                .assignment(new AssignmentType(prismContext)
                        .lifecycleState("assignment1-1")
                        .orgRef(org1Oid, OrgType.COMPLEX_TYPE, relation1)
                        .activation(new ActivationType(prismContext)
                                .validFrom("2021-03-01T00:00:00Z")
                                .validTo("2022-07-04T00:00:00Z")))
                .assignment(new AssignmentType(prismContext)
                        .lifecycleState("assignment1-2")
                        .order(1))
                .extension(new ExtensionType(prismContext));
        ExtensionType user1Extension = user1.getExtension();
        addExtensionValue(user1Extension, "string", "string-value");
        addExtensionValue(user1Extension, "int", 1);
        addExtensionValue(user1Extension, "long", 2L);
        addExtensionValue(user1Extension, "short", 3);
        addExtensionValue(user1Extension, "decimal",
                new BigDecimal("12345678901234567890.12345678901234567890"));
        addExtensionValue(user1Extension, "double", Double.MAX_VALUE);
        addExtensionValue(user1Extension, "float", Float.MAX_VALUE);
        addExtensionValue(user1Extension, "boolean", true);
        addExtensionValue(user1Extension, "enum", BeforeAfterType.AFTER);
        addExtensionValue(user1Extension, "dateTime", // 2021-09-30 before noon
                asXMLGregorianCalendar(Instant.ofEpochMilli(1633_000_000_000L)));
        addExtensionValue(user1Extension, "poly", PolyString.fromOrig("poly-value"));
        addExtensionValue(user1Extension, "ref", ref(org21Oid, OrgType.COMPLEX_TYPE, relation1));
        addExtensionValue(user1Extension, "string-mv", "string-value1", "string-value2");
        addExtensionValue(user1Extension, "enum-mv", // nonsense semantics, sorry about it
                OperationResultStatusType.WARNING, OperationResultStatusType.SUCCESS);
        addExtensionValue(user1Extension, "int-mv", 47, 31);
        addExtensionValue(user1Extension, "ref-mv",
                ref(org1Oid, null, relation2), // type is nullable if provided in schema
                ref(org2Oid, OrgType.COMPLEX_TYPE)); // default relation
        addExtensionValue(user1Extension, "string-ni", "not-indexed-item");

        ExtensionType user1AssignmentExtension = new ExtensionType(prismContext);
        user1.assignment(new AssignmentType(prismContext)
                .lifecycleState("assignment1-3-ext")
                .extension(user1AssignmentExtension));
        addExtensionValue(user1AssignmentExtension, "integer", 47);
        user1Oid = repositoryService.addObject(user1.asPrismObject(), null, result);

        UserType user2 = new UserType(prismContext).name("user-2")
                .parentOrgRef(orgXOid, OrgType.COMPLEX_TYPE)
                .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE, relation1)
                .subtype("workerA")
                .activation(new ActivationType(prismContext)
                        .validFrom("2021-03-01T00:00:00Z")
                        .validTo("2022-07-04T00:00:00Z"))
                .extension(new ExtensionType(prismContext));
        ExtensionType user2Extension = user2.getExtension();
        addExtensionValue(user2Extension, "string", "other-value...");
        addExtensionValue(user2Extension, "dateTime", // 2021-10-01 ~15PM
                asXMLGregorianCalendar(Instant.ofEpochMilli(1633_100_000_000L)));
        addExtensionValue(user2Extension, "int", 2);
        addExtensionValue(user2Extension, "double", Double.MIN_VALUE); // positive, close to zero
        addExtensionValue(user2Extension, "float", 0);
        addExtensionValue(user2Extension, "ref", ref(orgXOid, OrgType.COMPLEX_TYPE));
        addExtensionValue(user2Extension, "string-mv", "string-value2", "string-value3");
        addExtensionValue(user2Extension, "enum-mv",
                OperationResultStatusType.UNKNOWN, OperationResultStatusType.SUCCESS);
        addExtensionValue(user2Extension, "poly-mv",
                PolyString.fromOrig("poly-value1"), PolyString.fromOrig("poly-value2"));
        user2Oid = repositoryService.addObject(user2.asPrismObject(), null, result);

        UserType user3 = new UserType(prismContext).name("user-3")
                .costCenter("50")
                .parentOrgRef(orgXOid, OrgType.COMPLEX_TYPE)
                .parentOrgRef(org21Oid, OrgType.COMPLEX_TYPE, relation1)
                .policySituation("situationA")
                .assignment(new AssignmentType(prismContext)
                        .activation(new ActivationType(prismContext)
                                .validFrom("2021-01-01T00:00:00Z")))
                .assignment(new AssignmentType(prismContext)
                        .activation(new ActivationType(prismContext)
                                .validTo("2022-01-01T00:00:00Z")))
                .extension(new ExtensionType(prismContext));
        ExtensionType user3Extension = user3.getExtension();
        addExtensionValue(user3Extension, "int", 3);
        addExtensionValue(user3Extension, "dateTime", // 2021-10-02 ~19PM
                asXMLGregorianCalendar(Instant.ofEpochMilli(1633_200_000_000L)));
        user3Oid = repositoryService.addObject(user3.asPrismObject(), null, result);

        user4Oid = repositoryService.addObject(
                new UserType(prismContext).name("user-4")
                        .costCenter("51")
                        .parentOrgRef(org111Oid, OrgType.COMPLEX_TYPE)
                        .subtype("workerB")
                        .policySituation("situationB")
                        .asPrismObject(),
                null, result);
        task1Oid = repositoryService.addObject(
                new TaskType(prismContext).name("task-1")
                        .executionStatus(TaskExecutionStateType.RUNNABLE)
                        .asPrismObject(),
                null, result);
        task2Oid = repositoryService.addObject(
                new TaskType(prismContext).name("task-2")
                        .executionStatus(TaskExecutionStateType.CLOSED)
                        .asPrismObject(),
                null, result);

        ShadowType shadow1 = new ShadowType(prismContext).name("shadow-1")
                .pendingOperation(new PendingOperationType().attemptNumber(1))
                .pendingOperation(new PendingOperationType().attemptNumber(2))
                .extension(new ExtensionType(prismContext));
        addExtensionValue(shadow1.getExtension(), "string", "string-value");
        ItemName shadowAttributeName = new ItemName("http://example.com/p", "string-mv");
        ShadowAttributesHelper attributesHelper = new ShadowAttributesHelper(shadow1)
                .set(shadowAttributeName, DOMUtil.XSD_STRING, "string-value1", "string-value2");
        shadowAttributeDefinition = attributesHelper.getDefinition(shadowAttributeName);
        shadow1Oid = repositoryService.addObject(shadow1.asPrismObject(), null, result);
        // another shadow just to be check we don't select it accidentally
        repositoryService.addObject(
                new ShadowType(prismContext).name("shadow-2").asPrismObject(), null, result);

        service1Oid = repositoryService.addObject(
                new ServiceType(prismContext).name("service-1")
                        // TODO integer attribute
                        .asPrismObject(),
                null, result);
        case1Oid = repositoryService.addObject(
                new CaseType(prismContext).name("case-1")
                        .state("closed")
                        .closeTimestamp(asXMLGregorianCalendar(321L))
                        .workItem(new CaseWorkItemType(prismContext)
                                .id(41L)
                                .createTimestamp(asXMLGregorianCalendar(10000L))
                                .closeTimestamp(asXMLGregorianCalendar(10100L))
                                .deadline(asXMLGregorianCalendar(10200L))
                                .originalAssigneeRef(user3Oid, UserType.COMPLEX_TYPE)
                                .performerRef(user3Oid, UserType.COMPLEX_TYPE)
                                .stageNumber(1)
                                .assigneeRef(CASE_WI1_ASSIGNEE1_OID, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                                .assigneeRef(CASE_WI1_ASSIGNEE2_OID, UserType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)
                                .output(new AbstractWorkItemOutputType(prismContext).outcome("OUTCOME one")))
                        .workItem(new CaseWorkItemType(prismContext)
                                .id(42L)
                                .createTimestamp(asXMLGregorianCalendar(20000L))
                                .closeTimestamp(asXMLGregorianCalendar(20100L))
                                .deadline(asXMLGregorianCalendar(20200L))
                                .originalAssigneeRef(user1Oid, UserType.COMPLEX_TYPE)
                                .performerRef(user1Oid, UserType.COMPLEX_TYPE)
                                .stageNumber(2)
                                .output(new AbstractWorkItemOutputType(prismContext).outcome("OUTCOME two")))
                        .asPrismObject(),
                null, result);

        assertThatOperationResult(result).isSuccess();
    }

    // region simple filters
    @Test
    public void test100SearchUserByName() throws Exception {
        when("searching all objects with query without any conditions and paging");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .build(),
                operationResult);

        then("all objects are returned");
        assertThat(result).hasSize((int) count(QObject.CLASS));

        and("operation result is success");
        assertThatOperationResult(operationResult).isSuccess();
        OperationResult subresult = operationResult.getLastSubresult();
        assertThat(subresult).isNotNull();
        assertThat(subresult.getOperation()).isEqualTo("SqaleRepositoryService.searchObjects");
        assertThat(subresult.getStatus()).isEqualTo(OperationResultStatus.SUCCESS);
    }

    @Test
    public void test110SearchUserByName() throws Exception {
        when("searching user with query without any conditions and paging");
        OperationResult operationResult = createOperationResult();
        SearchResultList<UserType> result = searchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_NAME).eq(PolyString.fromOrig("user-1"))
                        .build(),
                operationResult);

        then("user with the matching name is returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOid()).isEqualTo(user1Oid);
    }

    @Test
    public void test120SearchObjectsBySubtype() throws Exception {
        searchObjectTest("having subtype equal to value", ObjectType.class,
                f -> f.item(ObjectType.F_SUBTYPE).eq("workerA"),
                user1Oid, user2Oid);
    }

    @Test
    public void test121SearchObjectsBySubtypeWithMultipleValues() throws Exception {
        searchObjectTest("having any subtype equal to any of the provided values", ObjectType.class,
                f -> f.item(ObjectType.F_SUBTYPE).eq("workerA", "workerB"),
                user1Oid, user2Oid, user4Oid);
    }

    @Test
    public void test122SearchObjectsHavingTwoSubtypeValuesUsingAnd() throws Exception {
        searchObjectTest("having multiple subtype values equal to provided values",
                ObjectType.class,
                f -> f.item(ObjectType.F_SUBTYPE).eq("workerA")
                        .and().item(ObjectType.F_SUBTYPE).eq("workerC"),
                user1Oid);
    }

    @Test
    public void test123SearchOrgsHavingTwoSubtypeValuesUsingAndButNoneMatches() throws Exception {
        searchObjectTest("having multiple subtype values equal to provided values (none matches)",
                ObjectType.class,
                f -> f.item(ObjectType.F_SUBTYPE).eq("workerA")
                        .and().item(ObjectType.F_SUBTYPE).eq("workerB"));
    }

    @Test
    public void test125SearchObjectsBySubtypeContainsIsNotSupported() {
        given("query for subtype containing (=substring) value");
        OperationResult operationResult = createOperationResult();
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(ObjectType.F_SUBTYPE).contains("worker")
                .build();

        expect("repository throws exception because it is not supported");
        assertThatThrownBy(() -> searchObjects(ObjectType.class, query, operationResult))
                .isInstanceOf(SystemException.class)
                .hasMessageStartingWith("Can't translate filter");
    }

    @Test
    public void test130SearchObjectsByPolicySituation() throws Exception {
        when("searching objects with policy situation equal to value");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(UserType.class)
                        .item(ObjectType.F_POLICY_SITUATION).eq("situationC")
                        .build(),
                operationResult);

        then("only objects having the specified policy situation are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result)
                .hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(user1Oid, org21Oid);
    }

    @Test
    public void test131SearchOrgsByPolicySituation() throws Exception {
        when("searching orgs with policy situation equal to value");
        OperationResult operationResult = createOperationResult();
        SearchResultList<OrgType> result = searchObjects(OrgType.class,
                prismContext.queryFor(OrgType.class)
                        .item(ObjectType.F_POLICY_SITUATION).eq("situationC")
                        .build(),
                operationResult);

        then("only orgs having the specified policy situation are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result)
                .hasSize(1)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org21Oid);
    }

    @Test
    public void test132SearchObjectsByPolicySituationWithMultipleValues() throws Exception {
        when("searching objects with any policy situation equal to any of the provided values");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(UserType.class)
                        .item(ObjectType.F_POLICY_SITUATION).eq("situationA", "situationB")
                        .build(),
                operationResult);

        then("objects with any of the policy situations are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result)
                .hasSize(3)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(user1Oid, user3Oid, user4Oid);
    }

    @Test
    public void test135SearchObjectsByPolicySituationContainsIsNotSupported() {
        given("query for policy situation containing (=substring) value");
        OperationResult operationResult = createOperationResult();
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(ObjectType.F_POLICY_SITUATION).contains("worker")
                .build();

        expect("repository throws exception because it is not supported");
        assertThatThrownBy(() -> searchObjects(ObjectType.class, query, operationResult))
                .isInstanceOf(SystemException.class)
                .hasMessageStartingWith("Can't translate filter");
    }

    @Test
    public void test140SearchTaskByEnumValue() throws Exception {
        when("searching task with execution status equal to one value");
        OperationResult operationResult = createOperationResult();
        SearchResultList<TaskType> result = searchObjects(TaskType.class,
                prismContext.queryFor(TaskType.class)
                        .item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStateType.RUNNABLE)
                        .build(),
                operationResult);

        then("tasks with the execution status with the provided value are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result)
                .hasSize(1)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(task1Oid);
    }

    @Test
    public void test141SearchTaskByEnumWithMultipleValues() throws Exception {
        when("searching task with execution status equal to any of provided value");
        OperationResult operationResult = createOperationResult();
        SearchResultList<TaskType> result = searchObjects(TaskType.class,
                prismContext.queryFor(TaskType.class)
                        .item(TaskType.F_EXECUTION_STATUS).eq(
                        TaskExecutionStateType.RUNNABLE, TaskExecutionStateType.CLOSED)
                        .build(),
                operationResult);

        then("tasks with execution status equal to any of the provided values are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result)
                .hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(task1Oid, task2Oid);
    }

    @Test
    public void test145SearchObjectsByEnumValueContainsIsNotSupported() {
        given("query for task's execution status containing (=substring) value");
        OperationResult operationResult = createOperationResult();
        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_EXECUTION_STATUS).contains(TaskExecutionStateType.RUNNABLE)
                .build();

        expect("repository throws exception because it is not supported");
        assertThatThrownBy(() -> searchObjects(TaskType.class, query, operationResult))
                .isInstanceOf(SystemException.class)
                .hasMessageStartingWith("Can't translate filter");
    }

    @Test
    public void test146SearchTaskByEnumValueProvidedAsStringIsNotSupported() {
        given("query for enum equality using string value");
        OperationResult operationResult = createOperationResult();
        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_EXECUTION_STATUS).eq("RUNNABLE")
                .build();

        expect("repository throws exception because it is not supported, enum must be used");
        assertThatThrownBy(() -> searchObjects(TaskType.class, query, operationResult))
                .isInstanceOf(SystemException.class);
    }

    /**
     * Searching by outcome in output in workitem in a case.
     * This is a special-case column.
     */
    @Test
    public void test180SearchCaseWorkItemByOutcome() throws Exception {
        searchCaseWorkItemByOutcome("OUTCOME one", case1Oid);
        searchCaseWorkItemByOutcome("OUTCOME two", case1Oid);
        searchCaseWorkItemByOutcome("OUTCOME nonexist");
    }

    private void searchCaseWorkItemByOutcome(String wiOutcome, String... expectedCaseOids) throws Exception {
        when("searching case with query for workitem/output/outcome " + wiOutcome);
        OperationResult operationResult = createOperationResult();
        SearchResultList<CaseType> result = searchObjects(CaseType.class,
                prismContext.queryFor(CaseType.class)
                        .item(CaseType.F_WORK_ITEM, CaseWorkItemType.F_OUTPUT,
                                AbstractWorkItemOutputType.F_OUTCOME).eq(wiOutcome)
                        .build(),
                operationResult);

        then("case with the matching workitem outcome is returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(expectedCaseOids);
    }

    /**
     * Searching by assigneeRef in workitem in a case.
     * The reference is multi-valued, it has a special table.
     */
    @Test
    public void test182SearchCaseWorkItemByAssignee() throws Exception {
        searchCaseWorkItemByAssignee(CASE_WI1_ASSIGNEE1_OID, case1Oid);
        searchCaseWorkItemByAssignee(CASE_WI1_ASSIGNEE2_OID, case1Oid);
        searchCaseWorkItemByAssignee(NONEXIST_OID);
    }

    private void searchCaseWorkItemByAssignee(String assigneeOid, String... expectedCaseOids) throws Exception {
        when("searching case with query for workitem/assigneeRef OID " + assigneeOid);
        OperationResult operationResult = createOperationResult();
        SearchResultList<CaseType> result = searchObjects(CaseType.class,
                prismContext.queryFor(CaseType.class)
                        .item(CaseType.F_WORK_ITEM, CaseWorkItemType.F_ASSIGNEE_REF).ref(assigneeOid)
                        .build(),
                operationResult);

        then("case with the matching workitem assigneeRef is returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(expectedCaseOids);
    }

    // TODO: search cert workitems
    // TODO: setup: cert workitems in two campaigns that have the same value, make sure campaign and case are properly reflected in the query.

    // endregion

    // region org filter
    @Test
    public void test200QueryForRootOrganizations() throws SchemaException {
        when("searching orgs with is-root filter");
        OperationResult operationResult = createOperationResult();
        SearchResultList<OrgType> result = searchObjects(OrgType.class,
                prismContext.queryFor(OrgType.class)
                        .isRoot()
                        .build(),
                operationResult);

        then("only organizations without any parents are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org1Oid, org2Oid);
    }

    @Test
    public void test201QueryForRootOrganizationsWithWrongType() throws SchemaException {
        // Currently this is "undefined", this does not work in old repo, in new repo it
        // checks parent-org refs (not closure). Prism does not complain either.
        // First we should fix it on Prism level first, then add type check to OrgFilterProcessor.
        when("searching user with is-root filter");
        OperationResult operationResult = createOperationResult();
        SearchResultList<UserType> result = searchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .isRoot()
                        .build(),
                operationResult);

        then("only users without any organizations are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(1)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(user1Oid);
    }

    @Test
    public void test210QueryForDirectChildrenOrgs() throws SchemaException {
        when("searching orgs just under another org");
        OperationResult operationResult = createOperationResult();
        SearchResultList<OrgType> result = searchObjects(OrgType.class,
                prismContext.queryFor(OrgType.class)
                        .isDirectChildOf(org1Oid)
                        .build(),
                operationResult);

        then("only orgs with direct parent-org ref to another org are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org11Oid, org12Oid);
    }

    @Test
    public void test211QueryForDirectChildrenOfAnyType() throws SchemaException {
        when("searching objects just under an org");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .isDirectChildOf(org11Oid)
                        .build(),
                operationResult);

        then("only objects (of any type) with direct parent-org ref to another org are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(3)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org111Oid, org112Oid, user2Oid);
    }

    @Test
    public void test212QueryForDirectChildrenOfAnyTypeWithRelation() throws SchemaException {
        when("searching objects just under an org with specific relation");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .isDirectChildOf(ref(org11Oid, OrgType.COMPLEX_TYPE, relation1))
                        .build(),
                operationResult);

        then("only objects with direct parent-org ref with specified relation are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org112Oid, user2Oid);
    }

    @Test
    public void test215QueryForChildrenOfAnyType() throws SchemaException {
        when("searching objects anywhere under an org");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .isChildOf(org2Oid)
                        .build(),
                operationResult);

        then("all objects under the specified organization are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(4)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org21Oid, orgXOid, user2Oid, user3Oid);
    }

    @Test
    public void test216QueryForChildrenOfAnyTypeWithRelation() throws SchemaException {
        when("searching objects anywhere under an org with specific relation");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .isChildOf(ref(org2Oid, OrgType.COMPLEX_TYPE, relation1))
                        .build(),
                operationResult);

        then("all objects under the specified organization with specified relation are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(1)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(user3Oid);
        // user-2 has another parent link with relation1, but not under org-2
    }

    @Test
    public void test220QueryForParents() throws SchemaException {
        when("searching parents of an org");
        OperationResult operationResult = createOperationResult();
        SearchResultList<OrgType> result = searchObjects(OrgType.class,
                prismContext.queryFor(OrgType.class)
                        .isParentOf(org112Oid)
                        .build(),
                operationResult);

        then("all ancestors of the specified organization are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org11Oid, org1Oid);
    }

    @Test
    public void test221QueryForParentsWithOrgSupertype() throws SchemaException {
        when("searching parents of an org using more abstract type");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .isParentOf(org112Oid)
                        .build(),
                operationResult);

        then("returns orgs but as supertype  instances");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org11Oid, org1Oid);
    }

    @Test
    public void test222QueryForParentsUsingOtherType() throws SchemaException {
        when("searching parents of an org using unrelated type");
        OperationResult operationResult = createOperationResult();
        SearchResultList<UserType> result = searchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .isParentOf(org112Oid)
                        .build(),
                operationResult);

        then("returns nothing (officially the behaviour is undefined)");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).isEmpty();
    }

    @Test
    public void test223QueryForParentIgnoresRelation() throws SchemaException {
        when("searching parents of an org with specified relation");
        OperationResult operationResult = createOperationResult();
        SearchResultList<OrgType> result = searchObjects(OrgType.class,
                prismContext.queryFor(OrgType.class)
                        .isParentOf(ref(org112Oid, OrgType.COMPLEX_TYPE, relation2))
                        .build(),
                operationResult);

        then("all ancestors are returned, ignoring provided relation");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org11Oid, org1Oid);
    }

    @Test
    public void test230QueryForChildrenOfAnyTypeWithAnotherCondition() throws SchemaException {
        when("searching objects anywhere under an org");
        OperationResult operationResult = createOperationResult();
        SearchResultList<FocusType> result = searchObjects(FocusType.class,
                prismContext.queryFor(FocusType.class)
                        .isChildOf(org2Oid)
                        .and().item(FocusType.F_COST_CENTER).startsWith("5")
                        .build(),
                operationResult);

        then("all objects under the specified organization matching other conditions are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org21Oid, user3Oid);
    }
    // endregion

    // region other filters: inOid, type, exists
    @Test
    public void test300QueryForObjectsWithInOid() throws SchemaException {
        searchObjectTest("having OID in the list", ObjectType.class,
                f -> f.id(user1Oid, task1Oid, org2Oid),
                user1Oid, task1Oid, org2Oid);
    }

    @Test
    public void test301QueryForFocusWithInOid() throws SchemaException {
        searchObjectTest("having OID in the list", FocusType.class,
                f -> f.id(user1Oid, task1Oid, org2Oid), // task will not match, it's not a focus
                user1Oid, org2Oid);
    }

    @Test
    public void test310QueryWithEmptyTypeFilter() throws SchemaException {
        searchObjectTest("matching the type filter without inner filter", ObjectType.class,
                f -> f.type(TaskType.class),
                task1Oid, task2Oid);
    }

    @Test
    public void test311QueryWithTypeFilterWithConditions() throws SchemaException {
        searchObjectTest("matching the type filter with inner filter", ObjectType.class,
                f -> f.type(FocusType.class)
                        .block()
                        .id(user1Oid, task1Oid, org2Oid) // task will not match, it's not a focus
                        .or()
                        .item(FocusType.F_COST_CENTER).eq("5")
                        .endBlock(),
                user1Oid, org2Oid, org21Oid);
    }

    @Test
    public void test312QueryWithTwoTypeFiltersInOr() throws SchemaException {
        searchObjectTest("matching two OR-ed type filters", ObjectType.class,
                f -> f.type(TaskType.class)
                        .id(task1Oid)
                        .or()
                        .type(OrgType.class)
                        .item(FocusType.F_COST_CENTER).eq("5"),
                task1Oid, org21Oid);
    }

    @Test
    public void test313QueryWithTwoTypeFiltersInOrOneTypeMatchingTheQuery() throws SchemaException {
        searchObjectTest("matching two OR-ed type filters, one matching the query type",
                ObjectType.class,
                f -> f.block()
                        .type(FocusType.class)
                        .item(ObjectType.F_SUBTYPE).eq("workerA")
                        .endBlock()
                        .or()
                        .type(OrgType.class)
                        .item(FocusType.F_COST_CENTER).eq("5"),
                user1Oid, user2Oid, org21Oid);
    }

    @Test
    public void test314QueryWithTypeFiltersMatchingTheQuery() throws SchemaException {
        searchObjectTest("matching the type filter matching the query type", TaskType.class,
                f -> f.type(TaskType.class),
                task1Oid, task2Oid);
    }

    @Test
    public void test315QueryWithTypeFilterForSupertype() throws SchemaException {
        // works fine, task is also an object, nothing bad happens
        searchObjectTest("matching the type filter of supertype", TaskType.class,
                f -> f.type(ObjectType.class),
                task1Oid, task2Oid);
    }

    @Test
    public void test316QueryWithTypeFilterForUnrelatedType() throws SchemaException {
        // "works" but for obvious reasons finds nothing, there is no common OID between the two
        searchObjectTest("matching the type filter of unrelated type", TaskType.class,
                f -> f.type(FocusType.class));
    }

    @Test
    public void test320QueryWithExistsFilter() throws SchemaException {
        searchUsersTest("matching the exists filter for assignment",
                f -> f.exists(UserType.F_ASSIGNMENT)
                        .item(AssignmentType.F_LIFECYCLE_STATE).eq("assignment1-1"),
                user1Oid);
    }

    @Test
    public void test322AndFilterVersusExistsAndFilter() throws SchemaException {
        searchUsersTest("matching the AND filter for assignment (across multiple assignments)",
                f -> f.item(UserType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM)
                        .lt(createXMLGregorianCalendar("2021-02-01T00:00:00Z"))
                        .and()
                        .item(UserType.F_ASSIGNMENT, AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO)
                        .gt(createXMLGregorianCalendar("2021-12-01T00:00:00Z")),
                user3Oid); // matches the user with two assignments

        searchUsersTest("matching the AND inside EXISTS filter for assignment",
                f -> f.exists(UserType.F_ASSIGNMENT)
                        .block() // block necessary, otherwise the second item goes from User
                        .item(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_FROM)
                        .lt(createXMLGregorianCalendar("2021-02-01T00:00:00Z"))
                        .and()
                        .item(AssignmentType.F_ACTIVATION, ActivationType.F_VALID_TO)
                        .gt(createXMLGregorianCalendar("2021-12-01T00:00:00Z"))
                        .endBlock());
        // but no user with one assignment matching both conditions exists
    }

    @Test
    public void test325ExistsFilterWithSizeColumn() throws SchemaException {
        searchObjectTest("having pending operations", ShadowType.class,
                f -> f.exists(ShadowType.F_PENDING_OPERATION),
                shadow1Oid);
    }


/* TODO EXISTS tests
1. @Count property => pendingOperationCount > 0; see: ClassDefinitionParser#parseMethod() + getJaxbName()
EXISTS(pendingOperation, null)

2. multi-value container stored in table:
EXISTS(operationExecution, AND(REF: taskRef, PRV(oid=task-oid-2, targetType=null); EQUAL: status, PPV(OperationResultStatusType:SUCCESS)))

3. container query (AccessCertificationWorkItemType) with EXISTS to the parent container (AccessCertificationCaseType)
  matching on parent's ownerID (OID of AccessCertificationCampaignType) + its own CID (AccessCertificationCaseType)
EXISTS({http://prism.evolveum.com/xml/ns/public/types-3}parent, AND(IN OID (for owner): e8c07a7a-1b11-11e8-9b32-1715a2e8273b, IN OID: 1))

parent vs ..?

4. part of AND in container query, used on sub-container (workItem) with nested AND condition
CertificationTest.test730CurrentUnansweredCases >>> Q{
AND(
  EQUAL: stageNumber, {http://prism.evolveum.com/xml/ns/public/types-3}parent/stageNumber;
  EQUAL: ../state, PPV(AccessCertificationCampaignStateType:IN_REVIEW_STAGE);
  EXISTS(workItem,
    AND(
      EQUAL: closeTimestamp, ;
      EQUAL: output/outcome, )))
, null paging}

5. EXISTS with .. (another way how to say parent)
EXISTS(..,
  OR(
    IN OID: c0c010c0-d34d-b33f-f00d-111111111111; ,
    REF: ownerRef,PRV(oid=c0c010c0-d34d-b33f-f00d-111111111111, targetType={.../common/common-3}UserType)))

6. nothing new, nested in OR, EXISTS(workItem) with AND

OR(
  IN OID: af69e388-88bd-43f9-9259-73676124c196; ,
  EXISTS(workItem,
    AND(
      EQUAL: closeTimestamp,,
      REF: assigneeRef, PRV(oid=af69e388-88bd-43f9-9259-73676124c196, targetType=null), PRV(oid=c0c010c0-d34d-b33f-f00d-111111111111, targetType=null))))

OR(
  NONE,
  LESS-OR-EQUAL: activation/validFrom,PPV(XMLGregorianCalendarImpl:2021-05-21T15:44:41.955+02:00),
  LESS-OR-EQUAL: activation/validTo,PPV(XMLGregorianCalendarImpl:2021-05-21T15:44:41.955+02:00),
  EXISTS(assignment,
    OR(
      LESS-OR-EQUAL: activation/validFrom,PPV(XMLGregorianCalendarImpl:2021-05-21T15:44:41.955+02:00),
      LESS-OR-EQUAL: activation/validTo,PPV(XMLGregorianCalendarImpl:2021-05-21T15:44:41.955+02:00))))

8. AND(2x EXISTS with multiple ref values) - why are both exists branches the same?
main >>> Q{
AND(
  EXISTS(assignment,
    REF: targetRef,
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}default),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}manager),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}approver),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}owner));
  EXISTS(assignment,
    REF: targetRef,
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}default),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}manager),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}approver),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}owner)))
, null paging}
*/
    // endregion

    // region refs and dereferencing
    @Test
    public void test400SearchObjectHavingSpecifiedRef() throws SchemaException {
        when("searching users by parent org ref (one of multi-value)");
        OperationResult operationResult = createOperationResult();
        SearchResultList<UserType> result = searchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_PARENT_ORG_REF)
                        .ref(ref(org11Oid, OrgType.COMPLEX_TYPE, PrismConstants.Q_ANY))
                        .build(),
                operationResult);

        then("the users having parent org ref with specified OID are returned");
        assertThat(result).hasSize(1)
                .anyMatch(o -> o.getOid().equals(user2Oid));
    }

    @Test
    public void test401SearchObjectNotHavingSpecifiedRef() throws SchemaException {
        when("searching users not having specified value of parent org ref");
        OperationResult operationResult = createOperationResult();
        SearchResultList<UserType> result = searchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .not()
                        .item(UserType.F_PARENT_ORG_REF)
                        .ref(ref(org11Oid, OrgType.COMPLEX_TYPE, PrismConstants.Q_ANY))
                        .build(),
                operationResult);

        then("the users having parent org ref with specified OID are returned");
        assertThat(result).hasSize(3)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(user1Oid, user3Oid, user4Oid);
    }

    // TODO tests with ref/@/...
    // endregion

    // region extension queries
    // basic string tests
    @Test
    public void test500SearchObjectHavingSpecifiedStringExtension() throws SchemaException {
        searchUsersTest("having extension string item equal to value",
                f -> f.item(UserType.F_EXTENSION, new QName("string")).eq("string-value"),
                user1Oid);
    }

    @Test
    public void test501SearchObjectNotHavingSpecifiedStringExtension() throws SchemaException {
        searchUsersTest("not having extension string item equal to value (can be null)",
                f -> f.not().item(UserType.F_EXTENSION, new QName("string")).eq("string-value"),
                user2Oid, user3Oid, user4Oid);
    }

    @Test
    public void test502SearchObjectWithoutExtensionStringItem() throws SchemaException {
        searchUsersTest("not having extension item (is null)",
                f -> f.item(UserType.F_EXTENSION, new QName("string")).isNull(),
                user3Oid, user4Oid);
    }

    @Test(enabled = false) // TODO missing feature order by complex paths, see SqlQueryContext.processOrdering
    public void test503SearchObjectWithAnyValueForExtensionItemOrderedByIt() throws SchemaException {
        when("searching for users with extension string item with any value ordered by that item");
        OperationResult operationResult = createOperationResult();
        SearchResultList<UserType> result = searchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .not()
                        .item(UserType.F_EXTENSION, new QName("string")).isNull()
                        .asc(UserType.F_EXTENSION, new QName("string"))
                        .build(),
                operationResult);

        then("users with extension item are returned, ordered by the item");
        assertThat(result)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(user2Oid, user1Oid);
    }

    @Test
    public void test505SearchObjectUsingExtensionStringComparison() throws SchemaException {
        searchUsersTest("having extension string greater than specified value",
                f -> f.item(UserType.F_EXTENSION, new QName("string")).gt("a"),
                user1Oid, user2Oid);
    }

    @Test
    public void test506SearchObjectWithExtensionItemContainingString() throws SchemaException {
        searchUsersTest("extension string item containing string",
                f -> f.item(UserType.F_EXTENSION, new QName("string")).contains("val"),
                user1Oid, user2Oid);
    }

    @Test
    public void test507SearchObjectWithExtensionItemContainingStringIgnoreCase() throws SchemaException {
        searchUsersTest("extension string item containing string ignore-case",
                f -> f.item(UserType.F_EXTENSION, new QName("string"))
                        .contains("VAL").matchingCaseIgnore(),
                user1Oid, user2Oid);
    }

    @Test
    public void test508SearchObjectWithExtensionStringEndsWithIgnoreCase() throws SchemaException {
        searchUsersTest("extension string item ending with string ignore-case",
                f -> f.item(UserType.F_EXTENSION, new QName("string"))
                        .endsWith("VaLuE").matchingCaseIgnore(),
                user1Oid);
    }

    @Test
    public void test509SearchObjectWithExtensionStringStartsWithIgnoreCase() throws SchemaException {
        searchUsersTest("extension string item starting with string ignore-case",
                f -> f.item(UserType.F_EXTENSION, new QName("string"))
                        .startsWith("OTHER").matchingCaseIgnore(),
                user2Oid);
    }

    // multi-value string tests
    @Test
    public void test510SearchObjectHavingSpecifiedMultivalueStringExtension() throws SchemaException {
        searchUsersTest("having extension multi-string item equal to value",
                f -> f.item(UserType.F_EXTENSION, new QName("string-mv")).eq("string-value2"),
                user1Oid, user2Oid);
    }

    @Test
    public void test511SearchObjectNotHavingSpecifiedMultivalueStringExtension() throws SchemaException {
        searchUsersTest("not having extension multi-string item equal to value",
                f -> f.not().item(UserType.F_EXTENSION, new QName("string-mv")).eq("string-value1"),
                user2Oid, user3Oid, user4Oid);
    }

    @Test
    public void test512SearchObjectWithoutExtensionMultivalueStringItem() throws SchemaException {
        searchUsersTest("not having extension multi-string item (is null)",
                f -> f.item(UserType.F_EXTENSION, new QName("string-mv")).isNull(),
                user3Oid, user4Oid);
    }

    @Test
    public void test515SearchObjectHavingSpecifiedMultivalueStringExtension() {
        given("query for multi-value extension string item with non-equal operation");
        OperationResult operationResult = createOperationResult();
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_EXTENSION, new QName("string-mv")).gt("string-value2")
                .build();

        expect("searchObjects throws exception because of unsupported filter");
        assertThatThrownBy(() -> searchObjects(UserType.class, query, operationResult))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("supported");
    }

    // integer tests
    @Test
    public void test520SearchObjectHavingSpecifiedIntegerExtension() throws SchemaException {
        searchUsersTest("having extension integer item equal to value",
                f -> f.item(UserType.F_EXTENSION, new QName("int")).eq(1),
                user1Oid);
    }

    @Test
    public void test521SearchObjectNotHavingSpecifiedIntegerExtension() throws SchemaException {
        searchUsersTest("not having extension int item equal to value",
                f -> f.not().item(UserType.F_EXTENSION, new QName("int")).eq("1"),
                user2Oid, user3Oid, user4Oid);
    }

    @Test
    public void test522SearchObjectWithoutExtensionIntegerItem() throws SchemaException {
        searchUsersTest("not having extension item (is null)",
                f -> f.item(UserType.F_EXTENSION, new QName("int")).isNull(),
                user4Oid);
    }

    @Test
    public void test523SearchObjectHavingSpecifiedIntegerExtensionItemGreaterThanValue() throws SchemaException {
        searchUsersTest("not having extension int item equal to value",
                f -> f.item(UserType.F_EXTENSION, new QName("int")).gt(1),
                user2Oid, user3Oid);
    }

    @Test
    public void test528SearchObjectHavingSpecifiedMultivalueIntegerExtension() throws SchemaException {
        searchUsersTest("having extension multi-integer item equal to value",
                f -> f.item(UserType.F_EXTENSION, new QName("int-mv")).eq(47),
                user1Oid);
    }

    @Test
    public void test529SearchObjectHavingSpecifiedMultivalueIntegerExtension() {
        given("query for multi-value extension integer item with non-equal operation");
        OperationResult operationResult = createOperationResult();
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_EXTENSION, new QName("int-mv")).gt(40)
                .build();

        expect("searchObjects throws exception because of unsupported filter");
        assertThatThrownBy(() -> searchObjects(UserType.class, query, operationResult))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("supported");
    }

    // other numeric types tests + wilder conditions
    @Test
    public void test530SearchObjectHavingSpecifiedIntegerExtension() throws SchemaException {
        searchUsersTest("having extension integer item equal to big decimal value",
                f -> f.item(UserType.F_EXTENSION, new QName("int")).eq(new BigDecimal(1)),
                user1Oid);
    }

    @Test
    public void test531SearchObjectHavingSpecifiedIntegerExtension() throws SchemaException {
        searchUsersTest("having extension decimal item equal to big decimal value",
                f -> f.item(UserType.F_EXTENSION, new QName("decimal"))
                        .eq(new BigDecimal("12345678901234567890.12345678901234567890")),
                user1Oid);
    }

    @Test
    public void test532BigDecimalRepresentationDoesNotMatterToDbOnlyValue() throws SchemaException {
        searchUsersTest("having extension decimal item equal to big decimal of different form",
                f -> f.item(UserType.F_EXTENSION, new QName("decimal"))
                        .eq(new BigDecimal("1234567890123456789012345678901234567890E-20")),
                user1Oid);
    }

    @Test
    public void test533SearchObjectHavingSpecifiedDoubleExtension() throws SchemaException {
        searchUsersTest("having extension double item equal to big decimal value",
                f -> f.item(UserType.F_EXTENSION, new QName("double")).eq(Double.MAX_VALUE),
                user1Oid);
    }

    @Test
    public void test534SearchObjectHavingNumericExtItemBetweenTwoValues() throws SchemaException {
        searchUsersTest("having extension numeric item between two value",
                f -> f.item(UserType.F_EXTENSION, new QName("int")).gt(0)
                        .and().item(UserType.F_EXTENSION, new QName("int")).lt(3),
                user1Oid, user2Oid);
    }

    @Test
    public void test535SearchObjectHavingNumericExtItemUsingGoeAndBigInteger() throws SchemaException {
        searchUsersTest("having extension double item equal to big decimal value",
                f -> f.item(UserType.F_EXTENSION, new QName("double")).ge(
                        new BigInteger("17976931348623157000000000000000000000000000000000000000000"
                                + "0000000000000000000000000000000000000000000000000000000000000000"
                                + "0000000000000000000000000000000000000000000000000000000000000000"
                                + "0000000000000000000000000000000000000000000000000000000000000000"
                                + "0000000000000000000000000000000000000000000000000000000000")),
                user1Oid);
    }

    @Test
    public void test536SearchObjectHavingDoubleExtItemBetweenTwoValues() throws SchemaException {
        searchUsersTest("having extension double item between two values",
                f -> f.item(UserType.F_EXTENSION, new QName("double")).gt(0)
                        .and().item(UserType.F_EXTENSION, new QName("double")).lt(3d),
                user2Oid);
    }

    @Test
    public void test537SearchObjectHavingFloatOrShortExtItem() throws SchemaException {
        searchUsersTest("having either float or short extension with specified conditions",
                f -> f.item(UserType.F_EXTENSION, new QName("float")).gt(-1f)
                        .or().item(UserType.F_EXTENSION, new QName("short")).lt((short) 4),
                user1Oid, user2Oid);
    }

    @Test
    public void test538SearchObjectHavingShortGoe() throws SchemaException {
        searchUsersTest("having extension short item greater than or equal to value",
                f -> f.item(UserType.F_EXTENSION, new QName("short")).ge((short) 3),
                user1Oid);
    }

    @Test
    public void test539SearchObjectHavingShortGoe() throws SchemaException {
        /*
        This test assures that users with non-null ext column without any "short" item are found
        too, even for operations not using containment operators (@> or ?).
        If the condition inside the NOT is "(u.ext->'3')::numeric >= $1" than there are two ways
        how to inverse the NOT properly:
        - default, but naive approach: not ((u.ext->'3')::numeric >= $1 and (u.ext->'3')::numeric is not null)
        - better, requiring a fix: not ((u.ext->'3')::numeric >= $1 and ext ? '3' and ext is not null)
        Good thing is that by default it works using the first approach, although not ideal.
        */
        searchUsersTest("having extension short item greater than or equal to value",
                f -> f.not().item(UserType.F_EXTENSION, new QName("short")).ge((short) 3),
                user2Oid, user3Oid, user4Oid);
    }

    // enum tests
    @Test
    public void test540SearchObjectHavingSpecifiedEnumExtension() throws SchemaException {
        searchUsersTest("having extension enum item equal to value",
                f -> f.item(UserType.F_EXTENSION, new QName("enum")).eq(BeforeAfterType.AFTER),
                user1Oid);
    }

    @Test
    public void test541SearchObjectNotHavingSpecifiedEnumExtension() throws SchemaException {
        searchUsersTest("not having extension enum item equal to value (can be null)",
                f -> f.not()
                        .item(UserType.F_EXTENSION, new QName("enum")).eq(BeforeAfterType.BEFORE),
                user1Oid, user2Oid, user3Oid, user4Oid);
    }

    @Test
    public void test542SearchObjectWithoutExtensionEnumItem() throws SchemaException {
        searchUsersTest("not having extension item (is null)",
                f -> f.item(UserType.F_EXTENSION, new QName("enum")).isNull(),
                user2Oid, user3Oid, user4Oid);
    }

    @Test
    public void test543SearchObjectByEnumExtensionWithNonEqOperationFails() {
        given("query for multi-value extension enum item with non-equal operation");
        OperationResult operationResult = createOperationResult();
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_EXTENSION, new QName("enum")).gt(OperationResultStatusType.SUCCESS)
                .build();

        expect("searchObjects throws exception because of unsupported filter");
        assertThatThrownBy(() -> searchObjects(UserType.class, query, operationResult))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("supported");
    }

    @Test
    public void test545SearchObjectHavingSpecifiedMultiValueEnumExtension() throws SchemaException {
        searchUsersTest("having extension multi-value enum item with specified value",
                f -> f.item(UserType.F_EXTENSION, new QName("enum-mv"))
                        .eq(OperationResultStatusType.SUCCESS),
                user1Oid, user2Oid);
    }

    // boolean tests
    @Test
    public void test548SearchObjectByBooleanExtension() throws SchemaException {
        searchUsersTest("having extension boolean item with specified value",
                f -> f.item(UserType.F_EXTENSION, new QName("boolean"))
                        .eq(true),
                user1Oid);
    }

    // date-time tests
    @Test
    public void test550SearchObjectByDateTimeExtension() throws SchemaException {
        searchUsersTest("having extension date-time item with specified value",
                f -> f.item(UserType.F_EXTENSION, new QName("dateTime"))
                        .eq(asXMLGregorianCalendar(Instant.ofEpochMilli(1633_100_000_000L))),
                user2Oid);
    }

    @Test
    public void test551SearchObjectByDateTimeExtensionBetween() throws SchemaException {
        searchUsersTest("having extension date-time item between specified values",
                f -> f.item(UserType.F_EXTENSION, new QName("dateTime"))
                        .gt(asXMLGregorianCalendar(Instant.ofEpochMilli(1633_000_000_000L)))
                        .and().item(UserType.F_EXTENSION, new QName("dateTime"))
                        .lt(asXMLGregorianCalendar(Instant.ofEpochMilli(1634_000_000_000L))),
                user2Oid, user3Oid);
    }

    @Test
    public void test552SearchObjectByDateTimeExtensionOrCondition() throws SchemaException {
        searchUsersTest("having extension date-time item matching either condition (OR)",
                f -> f.item(UserType.F_EXTENSION, new QName("dateTime"))
                        .le(asXMLGregorianCalendar(Instant.ofEpochMilli(1633_000_000_000L)))
                        .or().item(UserType.F_EXTENSION, new QName("dateTime"))
                        .ge(asXMLGregorianCalendar(Instant.ofEpochMilli(1633_200_000_000L))),
                user1Oid, user3Oid);
    }

    // date-time uses the same code as string, no need for more tests, only EQ works for multi-value

    @Test
    public void test560SearchObjectWithExtensionPolyStringByValue() throws SchemaException {
        searchUsersTest("with extension poly-string item equal to value",
                f -> f.item(UserType.F_EXTENSION, new QName("poly"))
                        .eq(new PolyString("poly-value")),
                user1Oid);
    }

    @Test
    public void test561SearchObjectWithExtensionPolyStringByOrigValue() throws SchemaException {
        searchUsersTest("with extension poly-string item orig equal to value",
                f -> f.item(UserType.F_EXTENSION, new QName("poly"))
                        .eq(new PolyString("poly-value")).matchingOrig(),
                user1Oid);
    }

    @Test
    public void test562SearchObjectWithExtensionPolyStringByOrigValueAsString()
            throws SchemaException {
        // Not sure how real is to use String parameter, but Prism doesn't fail and repo can handle it.
        // This will surely not work properly for default matching where both norm and orig are compared.
        searchUsersTest("with extension poly-string item orig equal to String value",
                f -> f.item(UserType.F_EXTENSION, new QName("poly"))
                        .eq("poly-value").matchingOrig(),
                user1Oid);
    }

    @Test
    public void test563SearchObjectWithExtensionPolyStringByNormValue() throws SchemaException {
        searchUsersTest("with extension poly-string item norm equal to value",
                f -> f.item(UserType.F_EXTENSION, new QName("poly"))
                        .eq(new PolyString("poly-value")).matchingNorm(),
                user1Oid);
    }

    @Test
    public void test564SearchObjectWithExtensionPolyStringByNormValueAsString()
            throws SchemaException {
        searchUsersTest("with extension poly-string item norm equal to String value",
                f -> f.item(UserType.F_EXTENSION, new QName("poly"))
                        .eq("polyvalue").matchingNorm(),
                user1Oid);
    }

    @Test
    public void test565SearchObjectWithExtensionPolyStringGreaterThan()
            throws SchemaException {
        searchUsersTest("with extension poly-string item greater than value",
                f -> f.item(UserType.F_EXTENSION, new QName("poly"))
                        .gt(new PolyString("aa-aa")),
                user1Oid);
    }

    @Test
    public void test566SearchObjectWithExtensionPolyStringLowerThan()
            throws SchemaException {
        searchUsersTest("with extension poly-string item greater than value",
                f -> f.item(UserType.F_EXTENSION, new QName("poly"))
                        .lt(new PolyString("aa-aa")));
        // nothing matches
    }

    @Test
    public void test567SearchObjectWithExtensionPolyStringNormLoeThan()
            throws SchemaException {
        searchUsersTest("with extension poly-string item greater than value",
                f -> f.item(UserType.F_EXTENSION, new QName("poly"))
                        .le(new PolyString("poly-value")).matchingNorm(),
                user1Oid);
    }

    @Test
    public void test568SearchObjectWithExtensionPolyStringComplexIgnoreCaseComparison()
            throws SchemaException {
        searchUsersTest("with extension poly-string item matching complex ignore-case comparison",
                f -> f.not().block()
                        // both AND parts must match user1, this ine is norm, so -- is ignored
                        .item(UserType.F_EXTENSION, new QName("poly")).ge("pOlY--vAlUe")
                        .matching(new QName(PolyStringItemFilterProcessor.NORM_IGNORE_CASE))
                        .and()
                        .item(UserType.F_EXTENSION, new QName("poly")).le("pOlY-vAlUe")
                        .matching(new QName(PolyStringItemFilterProcessor.ORIG_IGNORE_CASE))
                        .endBlock(),
                user2Oid, user3Oid, user4Oid);
    }

    @Test
    public void test569SearchObjectWithExtensionMultiValuePolyString()
            throws SchemaException {
        searchUsersTest("with extension poly-string multi-value item",
                f -> f.item(UserType.F_EXTENSION, new QName("poly-mv"))
                        .eq(new PolyString("poly-value1")),
                user2Oid);
    }

    @Test
    public void test570SearchObjectWithExtensionMultiValuePolyStringNorm()
            throws SchemaException {
        searchUsersTest("with extension poly-string multi-value item matching norm",
                f -> f.item(UserType.F_EXTENSION, new QName("poly-mv"))
                        // orig of provided value doesn't match, but norm should
                        .eq(new PolyString("poly--value1")).matchingNorm(),
                user2Oid);
    }

    @Test
    public void test571SearchObjectWithExtensionMultiValuePolyStringCaseIgnoreFails() {
        given("query for poly-string multi-value extension item matching ignore-case");
        OperationResult operationResult = createOperationResult();
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_EXTENSION, new QName("poly-mv"))
                .eq(new PolyString("poly-value1")).matchingCaseIgnore()
                .build();

        expect("searchObjects throws exception because of unsupported filter");
        assertThatThrownBy(() -> searchObjects(UserType.class, query, operationResult))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("supported");
    }

    @Test
    public void test580SearchObjectWithExtensionRef() throws SchemaException {
        searchUsersTest("with extension ref item matching",
                f -> f.item(UserType.F_EXTENSION, new QName("ref"))
                        .ref(ref(org21Oid, OrgType.COMPLEX_TYPE, relation1)),
                user1Oid);
    }

    @Test
    public void test581SearchObjectWithExtensionRefByOidOnly() throws SchemaException {
        searchUsersTest("with extension ref item matching by OID only (implies default relation)",
                f -> f.item(UserType.F_EXTENSION, new QName("ref"))
                        .ref(org21Oid));
        // used ref has non-default relation, so, correctly, it is not found
    }

    @Test
    public void test582SearchObjectWithExtensionRefByOidOnly() throws SchemaException {
        searchUsersTest("with extension ref item matching by OID only",
                f -> f.item(UserType.F_EXTENSION, new QName("ref"))
                        .ref(ref(org21Oid, null, PrismConstants.Q_ANY)),
                user1Oid);
    }

    @Test
    public void test583SearchObjectWithExtensionRefByUnusedOid() throws SchemaException {
        searchUsersTest("with extension ref item matching by unused OID",
                f -> f.item(UserType.F_EXTENSION, new QName("ref"))
                        .ref(org12Oid));
    }

    @Test
    public void test584SearchObjectWithExtensionRefMatchingAnyOfVals() throws SchemaException {
        searchUsersTest("with extension ref item matching by unused OID",
                f -> f.item(UserType.F_EXTENSION, new QName("ref")).ref(
                        ref(org21Oid, null, PrismConstants.Q_ANY),
                        ref(orgXOid, null, PrismConstants.Q_ANY)),
                user1Oid, user2Oid);
    }

    @Test
    public void test590SearchObjectWithAssignmentExtension() throws SchemaException {
        searchUsersTest("with assignment extension item equal to value",
                f -> f.item(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION,
                        new QName("integer")).eq(47),
                user1Oid);
    }

    @Test
    public void test591SearchShadowWithAttribute() throws SchemaException {
        searchObjectTest("with assignment extension item equal to value", ShadowType.class,
                f -> f.itemWithDef(shadowAttributeDefinition,
                        ShadowType.F_ATTRIBUTES, new QName("http://example.com/p", "string-mv"))
                        .eq("string-value2"),
                shadow1Oid);
    }

    @Test
    public void test595SearchObjectByNotIndexedExtensionFails() {
        given("query for not-indexed extension");
        OperationResult operationResult = createOperationResult();
        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(UserType.F_EXTENSION, new QName("string-ni")).eq("whatever")
                .build();

        expect("searchObjects throws exception because of not-indexed item");
        assertThatThrownBy(() -> searchObjects(UserType.class, query, operationResult))
                .isInstanceOf(SystemException.class)
                .hasMessageContaining("not indexed");
    }

    @Test
    public void test600SearchContainersByOwnerId() throws SchemaException {
        SearchResultList<AssignmentType> result = searchContainerTest(
                "assignments by owner OID", AssignmentType.class, f -> f.ownerId(user1Oid));
        assertThat(result)
                .extracting(a -> a.getLifecycleState())
                .containsExactlyInAnyOrder("assignment1-1", "assignment1-2", "assignment1-3-ext");
        // more extensive test of one assignment instance
        assertThat(result).filteredOn(a -> a.getLifecycleState().equals("assignment1-1"))
                .singleElement()
                .matches(a -> a.getConstruction() == null)
                .matches(a -> a.getActivation().getValidFrom().equals(
                        createXMLGregorianCalendar("2021-03-01T00:00:00Z")))
                .matches(a -> a.getActivation().getValidTo().equals(
                        createXMLGregorianCalendar("2022-07-04T00:00:00Z")))
                .matches(a -> a.getOrgRef().getOid().equals(org1Oid))
                .matches(a -> a.getOrgRef().getType().equals(OrgType.COMPLEX_TYPE))
                .matches(a -> a.getOrgRef().getRelation().equals(relation1))
                .matches(a -> a.getMetadata() == null);

    }

    // TODO multi-value EQ filter (IN semantics) is not supported YET (except for refs)
    // endregion

    // region special cases
    @Test
    public void test900SearchByWholeContainerIsNotPossible() {
        expect("creating query with embedded container equality fails");
        assertThatThrownBy(
                () -> prismContext.queryFor(UserType.class)
                        .item(UserType.F_METADATA).eq(new MetadataType())
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Unsupported item definition: PCD:");

        // even if query was possible this would fail in the actual repo search, which is expected
    }

    @Test
    public void test920SearchObjectTypeFindsAllObjects() throws SchemaException {
        OperationResult operationResult = createOperationResult();

        given("query without any filter");
        ObjectQuery query = prismContext.queryFor(ObjectType.class).build();

        when("search is called with ObjectType");
        SearchResultList<ObjectType> result =
                searchObjects(ObjectType.class, query, operationResult);

        then("all repository objects are returned");
        assertThat(result).hasSize((int) count(QObject.CLASS));
    }

    @Test
    public void test921SearchAssignmentHolderTypeFindsAllObjectsExceptShadows()
            throws SchemaException {
        OperationResult operationResult = createOperationResult();

        given("query without any filter");
        ObjectQuery query = prismContext.queryFor(ObjectType.class).build();

        when("search is called with AssignmentHolderType");
        SearchResultList<AssignmentHolderType> result =
                searchObjects(AssignmentHolderType.class, query, operationResult);

        then("all repository objects except shadows are returned");
        QObject<MObject> o = aliasFor(QObject.CLASS);
        assertThat(result).hasSize((int) count(o, o.objectType.ne(MObjectType.SHADOW)));
        assertThat(result).hasSize((int) count(QAssignmentHolder.CLASS));
        // without additional objects the test would be meaningless
        assertThat(result).hasSizeLessThan((int) count(o));
    }

    @Test
    public void test922SearchFocusTypeFindsOnlyFocusObjects()
            throws SchemaException {
        OperationResult operationResult = createOperationResult();

        given("query without any filter");
        ObjectQuery query = prismContext.queryFor(ObjectType.class).build();

        when("search is called with FocusType");
        SearchResultList<FocusType> result =
                searchObjects(FocusType.class, query, operationResult);

        then("only focus objects from repository are returned");
        assertThat(result).hasSize((int) count(QFocus.CLASS));
        // without additional objects the test would be meaningless
        assertThat(result).hasSizeLessThan((int) count(QAssignmentHolder.CLASS));
    }

    @Test
    public void test950SearchOperationUpdatesPerformanceMonitor() throws SchemaException {
        OperationResult operationResult = createOperationResult();

        given("cleared performance information");
        SqlPerformanceMonitorImpl pm = repositoryService.getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();
        assertThat(pm.getGlobalPerformanceInformation().getAllData()).isEmpty();

        when("search is called on the repository");
        searchObjects(FocusType.class,
                prismContext.queryFor(FocusType.class).build(),
                operationResult);

        then("performance monitor is updated");
        assertThatOperationResult(operationResult).isSuccess();
        assertSingleOperationRecorded(pm, RepositoryService.OP_SEARCH_OBJECTS);
    }

    @Test(enabled = false)
    public void test960SearchByAxiomQueryLanguage() throws SchemaException {
        OperationResult operationResult = createOperationResult();
        SearchResultList<FocusType> focusTypes = searchObjects(FocusType.class,
                ". type UserType and employeeNumber startsWith \"5\"",
                operationResult);
        System.out.println("focusTypes = " + focusTypes);
    }
    // endregion

    // support methods

    /** Search objects using Axiom query language. */
    @SafeVarargs
    @NotNull
    private <T extends ObjectType> SearchResultList<T> searchObjects(
            @NotNull Class<T> type,
            String query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {
        ObjectFilter objectFilter = prismContext.createQueryParser().parseQuery(type, query);
        ObjectQuery objectQuery = prismContext.queryFactory().createQuery(objectFilter);
        return searchObjects(type, objectQuery, operationResult, selectorOptions);
    }

    private void searchUsersTest(String description,
            Function<S_FilterEntryOrEmpty, S_FilterExit> filter, String... expectedOids)
            throws SchemaException {
        searchObjectTest(description, UserType.class, filter, expectedOids);
    }

    private <T extends ObjectType> void searchObjectTest(String description, Class<T> type,
            Function<S_FilterEntryOrEmpty, S_FilterExit> filter, String... expectedOids)
            throws SchemaException {
        String typeName = type.getSimpleName().replaceAll("Type$", "").toLowerCase();
        when("searching for " + typeName + "(s) " + description);
        OperationResult operationResult = createOperationResult();
        SearchResultList<T> result = searchObjects(type,
                filter.apply(prismContext.queryFor(type)).build(),
                operationResult);

        then(typeName + "(s) " + description + " are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(expectedOids);
    }

    /** Search objects using {@link ObjectQuery}. */
    @SafeVarargs
    @NotNull
    private <T extends ObjectType> SearchResultList<T> searchObjects(
            @NotNull Class<T> type,
            ObjectQuery query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {
        display("QUERY: " + query);
        QueryType queryType = prismContext.getQueryConverter().createQueryType(query);
        String serializedQuery = prismContext.xmlSerializer().serializeAnyData(
                queryType, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        display("Serialized QUERY: " + serializedQuery);

        // sanity check if it's re-parsable
        assertThat(prismContext.parserFor(serializedQuery).parseRealValue(QueryType.class))
                .isNotNull();
        return repositoryService.searchObjects(
                type,
                query,
                Arrays.asList(selectorOptions),
                operationResult)
                .map(p -> p.asObjectable());
    }

    private <T extends Containerable> SearchResultList<T> searchContainerTest(
            String description, Class<T> type, Function<S_FilterEntryOrEmpty, S_FilterExit> filter)
            throws SchemaException {
        String typeName = type.getSimpleName().replaceAll("Type$", "").toLowerCase();
        when("searching for " + typeName + "(s) " + description);
        OperationResult operationResult = createOperationResult();
        SearchResultList<T> result = searchContainers(type,
                filter.apply(prismContext.queryFor(type)).build(),
                operationResult);

        then(typeName + "(s) " + description + " are returned");
        assertThatOperationResult(operationResult).isSuccess();
        return result;
    }

    /** Search containers using {@link ObjectQuery}. */
    @SafeVarargs
    @NotNull
    private <T extends Containerable> SearchResultList<T> searchContainers(
            @NotNull Class<T> type,
            ObjectQuery query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {
        display("QUERY: " + query);
        QueryType queryType = prismContext.getQueryConverter().createQueryType(query);
        String serializedQuery = prismContext.xmlSerializer().serializeAnyData(
                queryType, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        display("Serialized QUERY: " + serializedQuery);

        // sanity check if it's re-parsable
        assertThat(prismContext.parserFor(serializedQuery).parseRealValue(QueryType.class))
                .isNotNull();
        return repositoryService.searchContainers(
                type,
                query,
                Arrays.asList(selectorOptions),
                operationResult);
    }
}
