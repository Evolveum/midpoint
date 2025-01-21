/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ROLE_MEMBERSHIP_REF;

import static org.assertj.core.api.Assertions.*;

import static com.evolveum.midpoint.prism.xml.XmlTypeConverter.createXMLGregorianCalendar;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.util.MiscUtil.asXMLGregorianCalendar;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ASSIGNMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.query.OrderDirection;

import org.jetbrains.annotations.Nullable;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.prism.path.ParentPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.AggregateQuery;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("ConstantConditions")
public class SqaleRepoSearchAggregateTest extends SqaleRepoBaseTest {

    // org structure
    private String org1Oid; // one root
    private String org11Oid;
    private String org111Oid;
    private String org112Oid;
    private String org12Oid;
    private String org2Oid; // second root
    private String org21Oid;
    private String orgXOid; // under two orgs

    private String creatorOid, modifierOid; // special users used in refs
    private String user1Oid; // user without org
    private String user2Oid; // different user, this one is in org
    private String user3Oid; // another user in org
    private String user4Oid; // another user in org
    private String task1Oid; // task has more item type variability
    private String task2Oid;
    private String shadow1Oid; // shadow with owner
    private String case1Oid; // Closed case, two work items
    private String accCertCampaign1Oid;
    private String connector1Oid;
    private String connector2Oid;
    private String roleAvIOid; // role for assignment-vs-inducement tests
    private String roleOtherOid;
    private String roleOneMoreOid;

    // other info used in queries
    private final QName relation1 = QName.valueOf("{https://random.org/ns}rel-1");
    private final QName relation2 = QName.valueOf("{https://random.org/ns}rel-2");
    private final String resourceOid = UUID.randomUUID().toString();
    private final String connectorHostOid = UUID.randomUUID().toString();

    private String markProtectedOid;

    private ItemDefinition<?> shadowAttributeStringMvDefinition;

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

        markProtectedOid = repositoryService.addObject(
                new MarkType().name("protected").asPrismObject(), null, result);

        repositoryService.addObject(
                new MarkType().name("do-not-touch").asPrismObject(), null, result);

        roleAvIOid = repositoryService.addObject(
                new RoleType()
                        .name("role-ass-vs-ind")
                        .assignment(new AssignmentType()
                                .lifecycleState("role-ass-lc"))
                        .inducement(new AssignmentType()
                                .lifecycleState("role-ind-lc"))
                        .asPrismObject(), null, result);

        roleOtherOid = repositoryService.addObject(
                new RoleType().name("role-other")
                        .asPrismObject(), null, result);
        roleOneMoreOid = repositoryService.addObject(
                new RoleType().name("role-one-more")
                        .asPrismObject(), null, result);

        // org structure
        org1Oid = repositoryService.addObject(
                new OrgType().name("org-1").asPrismObject(),
                null, result);
        org11Oid = repositoryService.addObject(
                new OrgType().name("org-1-1")
                        .parentOrgRef(org1Oid, OrgType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        org111Oid = repositoryService.addObject(
                new OrgType().name("org-1-1-1")
                        .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE, relation2)
                        .subtype("newWorkers")
                        .asPrismObject(),
                null, result);
        org112Oid = repositoryService.addObject(
                new OrgType().name("org-1-1-2")
                        .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE, relation1)
                        .subtype("secret")
                        .asPrismObject(),
                null, result);
        org12Oid = repositoryService.addObject(
                new OrgType().name("org-1-2")
                        .parentOrgRef(org1Oid, OrgType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        org2Oid = repositoryService.addObject(
                new OrgType().name("org-2").asPrismObject(),
                null, result);
        org21Oid = repositoryService.addObject(
                new OrgType().name("org-2-1")
                        .costCenter("5")
                        .parentOrgRef(org2Oid, OrgType.COMPLEX_TYPE)
                        .policySituation("situationC")
                        .asPrismObject(),
                null, result);
        orgXOid = repositoryService.addObject(
                new OrgType().name("org-X")
                        .displayOrder(30)
                        .parentOrgRef(org12Oid, OrgType.COMPLEX_TYPE)
                        .parentOrgRef(org21Oid, OrgType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER)
                        .asPrismObject(),
                null, result);

        // shadow, owned by user-3
        ShadowType shadow1 = new ShadowType().name("shadow-1")
                .effectiveMarkRef(markProtectedOid, MarkType.COMPLEX_TYPE)
                .pendingOperation(new PendingOperationType().attemptNumber(1))
                .pendingOperation(new PendingOperationType().attemptNumber(2))
                .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE) // what relation is used for shadow->resource?
                .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                .kind(ShadowKindType.ACCOUNT)
                .intent("intent")
                .tag("tag")
                .extension(new ExtensionType());
        addExtensionValue(shadow1.getExtension(), "string", "string-value");
        ItemName shadowAttributeName = new ItemName("https://example.com/p", "string-mv");
        ShadowAttributesHelper attributesHelper = new ShadowAttributesHelper(shadow1)
                .set(shadowAttributeName, DOMUtil.XSD_STRING, "string-value1", "string-value2");
        shadowAttributeStringMvDefinition = attributesHelper.getDefinition(shadowAttributeName);
        shadow1Oid = repositoryService.addObject(shadow1.asPrismObject(), null, result);
        // another shadow just to check we don't select shadow1 accidentally/randomly
        repositoryService.addObject(
                new ShadowType().name("shadow-2")
                        .resourceRef(resourceOid, ResourceType.COMPLEX_TYPE) // what relation is used for shadow->resource?
                        .objectClass(SchemaConstants.RI_ACCOUNT_OBJECT_CLASS)
                        .asPrismObject(), null, result);

        // tasks
        task1Oid = repositoryService.addObject(
                new TaskType().name("task-1")
                        .executionState(TaskExecutionStateType.RUNNABLE)
                        .asPrismObject(),
                null, result);
        task2Oid = repositoryService.addObject(
                new TaskType().name("task-2")
                        .executionState(TaskExecutionStateType.CLOSED)
                        .schedule(new ScheduleType()
                                .recurrence(TaskRecurrenceType.RECURRING))
                        .asPrismObject(),
                null, result);

        // users
        creatorOid = repositoryService.addObject(
                new UserType().name("creator").familyName("Creator").asPrismObject(),
                null, result);
        modifierOid = repositoryService.addObject(
                new UserType().name("modifier").givenName("Modifier").asPrismObject(),
                null, result);

        UserType user1 = new UserType().name("user-1")
                .fullName("User Name 1")
                .givenName("First")
                .familyName("Adams")
                .metadata(new MetadataType()
                        .creatorRef(creatorOid, UserType.COMPLEX_TYPE, relation1)
                        .createChannel("create-channel")
                        .createTimestamp(asXMLGregorianCalendar(1L))
                        .modifierRef(modifierOid, UserType.COMPLEX_TYPE, relation2)
                        .modifyChannel("modify-channel")
                        .modifyTimestamp(asXMLGregorianCalendar(2L)))
                .subtype("workerA")
                .subtype("workerC")
                .employeeNumber("user1")
                .policySituation("situationA")
                .policySituation("situationC")
                .activation(new ActivationType()
                        .validFrom("2021-07-04T00:00:00Z")
                        .validTo("2022-07-04T00:00:00Z")
                        .disableTimestamp("2020-01-01T00:00:00Z"))
                .assignment(new AssignmentType()
                        .lifecycleState("assignment1-1")
                        .orgRef(org1Oid, OrgType.COMPLEX_TYPE, relation1)
                        // similar target ref like for user3, but to different role
                        .targetRef(roleOtherOid, RoleType.COMPLEX_TYPE, relation2)
                        .activation(new ActivationType()
                                .validFrom("2021-03-01T00:00:00Z")
                                .validTo("2022-07-04T00:00:00Z"))
                        .subtype("ass-subtype-2"))
                .assignment(new AssignmentType()
                        .lifecycleState("assignment1-2")
                        .order(1))
                .operationExecution(new OperationExecutionType()
                        .taskRef(task2Oid, TaskType.COMPLEX_TYPE)
                        .status(OperationResultStatusType.FATAL_ERROR)
                        .timestamp("2021-09-01T00:00:00Z"))
                .operationExecution(new OperationExecutionType()
                        .taskRef(task1Oid, TaskType.COMPLEX_TYPE)
                        .status(OperationResultStatusType.SUCCESS)
                        .timestamp("2021-10-01T00:00:00Z"))
                .roleMembershipRef(createTestRefWithMetadata(roleOtherOid, RoleType.COMPLEX_TYPE, relation2))
                .extension(new ExtensionType());
        ExtensionType user1Extension = user1.getExtension();
        addExtensionValue(user1Extension, "string", "string-value");
        addExtensionValue(user1Extension, "int", 1);
        addExtensionValue(user1Extension, "long", 2L);
        addExtensionValue(user1Extension, "short", (short) 3);
        addExtensionValue(user1Extension, "decimal",
                new BigDecimal("12345678901234567890.12345678901234567890"));
        addExtensionValue(user1Extension, "double", Double.MAX_VALUE);
        addExtensionValue(user1Extension, "float", Float.MAX_VALUE);
        addExtensionValue(user1Extension, "boolean", true);
        addExtensionValue(user1Extension, "enum", BeforeAfterType.AFTER);
        addExtensionValue(user1Extension, "dateTime", // 2021-09-30 before noon
                asXMLGregorianCalendar(1633_000_000_000L));
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
        addExtensionValue(user1Extension, "dateTime-mv",
                createXMLGregorianCalendar("2022-03-10T23:00:00Z"),
                createXMLGregorianCalendar("2021-01-01T00:00:00Z"));

        ExtensionType user1AssignmentExtension = new ExtensionType();
        user1.assignment(new AssignmentType()
                .lifecycleState("assignment1-3-ext")
                .extension(user1AssignmentExtension));
        addExtensionValue(user1AssignmentExtension, "integer", BigInteger.valueOf(47));
        user1Oid = repositoryService.addObject(user1.asPrismObject(), null, result);

        UserType user2 = new UserType().name("user-2")
                .familyName("Adams")
                .parentOrgRef(orgXOid, OrgType.COMPLEX_TYPE)
                .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE, relation1)
                .subtype("workerA")
                .activation(new ActivationType()
                        .validFrom("2021-03-01T00:00:00Z")
                        .validTo("2022-07-04T00:00:00Z"))
                .metadata(new MetadataType()
                        .createTimestamp(asXMLGregorianCalendar(2L))
                        .createApproverRef(user1Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT))
                .operationExecution(new OperationExecutionType()
                        .taskRef(task1Oid, TaskType.COMPLEX_TYPE)
                        .status(OperationResultStatusType.FATAL_ERROR)
                        .timestamp("2021-11-01T00:00:00Z"))
                .operationExecution(new OperationExecutionType()
                        .taskRef(task1Oid, TaskType.COMPLEX_TYPE)
                        .status(OperationResultStatusType.SUCCESS)
                        .timestamp("2021-12-01T00:00:00Z"))
                .operationExecution(new OperationExecutionType()
                        .taskRef(task1Oid, TaskType.COMPLEX_TYPE)
                        .status(OperationResultStatusType.PARTIAL_ERROR)
                        .timestamp("2022-01-01T00:00:00Z"))
                .extension(new ExtensionType());
        ExtensionType user2Extension = user2.getExtension();
        addExtensionValue(user2Extension, "string", "other-value...");
        addExtensionValue(user2Extension, "dateTime", // 2021-10-01 ~15PM
                asXMLGregorianCalendar(1633_100_000_000L));
        addExtensionValue(user2Extension, "int", 2);
        addExtensionValue(user2Extension, "double", Double.MIN_VALUE); // positive, close to zero
        addExtensionValue(user2Extension, "float", 0f);
        addExtensionValue(user2Extension, "ref", ref(orgXOid, OrgType.COMPLEX_TYPE));
        addExtensionValue(user2Extension, "string-mv", "string-value2", "string-value3");
        addExtensionValue(user2Extension, "poly", PolyString.fromOrig("poly-value-user2"));
        addExtensionValue(user2Extension, "enum-mv",
                OperationResultStatusType.UNKNOWN, OperationResultStatusType.SUCCESS);
        addExtensionValue(user2Extension, "poly-mv",
                PolyString.fromOrig("poly-value1"), PolyString.fromOrig("poly-value2"));
        user2Oid = repositoryService.addObject(user2.asPrismObject(), null, result);

        UserType user3 = new UserType().name("user-3")
                .givenName("Third")
                .familyName("Adams")
                .costCenter("50")
                .parentOrgRef(orgXOid, OrgType.COMPLEX_TYPE, relation2)
                .parentOrgRef(org21Oid, OrgType.COMPLEX_TYPE, relation1)
                .policySituation("situationA")
                .linkRef(shadow1Oid, ShadowType.COMPLEX_TYPE)
                .assignment(new AssignmentType()
                        .lifecycleState("ls-user3-ass2")
                        .targetRef(roleAvIOid, RoleType.COMPLEX_TYPE, relation2)
                        .metadata(new MetadataType()
                                .creatorRef(user1Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT))
                        .activation(new ActivationType()
                                .validTo("2022-01-01T00:00:00Z")))
                .operationExecution(new OperationExecutionType()
                        .taskRef(task1Oid, TaskType.COMPLEX_TYPE)
                        .status(OperationResultStatusType.WARNING)
                        .timestamp("2021-08-01T00:00:00Z"))
                .organization("org-33")
                .roleMembershipRef(roleAvIOid, RoleType.COMPLEX_TYPE, relation2)
                .roleMembershipRef(createTestRefWithMetadata(roleOneMoreOid, RoleType.COMPLEX_TYPE, ORG_DEFAULT))
                .extension(new ExtensionType());
        ExtensionType user3Extension = user3.getExtension();
        addExtensionValue(user3Extension, "int", 10);
        addExtensionValue(user3Extension, "dateTime", // 2021-10-02 ~19PM
                asXMLGregorianCalendar(1633_200_000_000L));
        ExtensionType user3AssignmentExtension = new ExtensionType();
        user3.assignment(new AssignmentType()
                .lifecycleState("ls-user3-ass1")
                .metadata(new MetadataType()
                        .creatorRef(user2Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT)
                        .createApproverRef(user1Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT))
                .activation(new ActivationType()
                        .validFrom("2021-01-01T00:00:00Z"))
                .subtype("ass-subtype-1")
                .subtype("ass-subtype-2")
                .extension(user3AssignmentExtension));
        addExtensionValue(user3AssignmentExtension, "integer", BigInteger.valueOf(48));
        user3Oid = repositoryService.addObject(user3.asPrismObject(), null, result);

        user4Oid = repositoryService.addObject(
                new UserType().name("user-4")
                        .givenName("John")
                        .fullName("John")
                        .costCenter("51")
                        .parentOrgRef(org111Oid, OrgType.COMPLEX_TYPE)
                        .subtype("workerB")
                        .policySituation("situationB")
                        .organization("org-1") // orgs and ous are polys stored in JSONB arrays
                        .organization("org-2")
                        .organizationalUnit("ou-1")
                        .organizationalUnit("ou-2")
                        .roleMembershipRef(createTestRefWithMetadata(
                                roleOneMoreOid, RoleType.COMPLEX_TYPE, ORG_DEFAULT))
                        .asPrismObject(),
                null, result);

        // other objects
        case1Oid = repositoryService.addObject(
                new CaseType().name("case-1")
                        .state("closed")
                        .closeTimestamp(asXMLGregorianCalendar(321L))
                        .workItem(new CaseWorkItemType()
                                .id(41L)
                                .createTimestamp(asXMLGregorianCalendar(10000L))
                                .closeTimestamp(asXMLGregorianCalendar(10100L))
                                .deadline(asXMLGregorianCalendar(10200L))
                                .originalAssigneeRef(user3Oid, UserType.COMPLEX_TYPE)
                                .performerRef(user3Oid, UserType.COMPLEX_TYPE)
                                .stageNumber(1)
                                .assigneeRef(user1Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT)
                                .assigneeRef(user2Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT)
                                .output(new AbstractWorkItemOutputType().outcome("OUTCOME one")))
                        .workItem(new CaseWorkItemType()
                                .id(42L)
                                .createTimestamp(asXMLGregorianCalendar(20000L))
                                .closeTimestamp(asXMLGregorianCalendar(20100L))
                                .deadline(asXMLGregorianCalendar(20200L))
                                .originalAssigneeRef(user1Oid, UserType.COMPLEX_TYPE)
                                .performerRef(user1Oid, UserType.COMPLEX_TYPE)
                                .stageNumber(2)
                                .output(new AbstractWorkItemOutputType().outcome("OUTCOME two")))
                        .asPrismObject(),
                null, result);

        accCertCampaign1Oid = repositoryService.addObject(
                new AccessCertificationCampaignType().name("acc-1")
                        .stageNumber(0) // mandatory, also for containers
                        .iteration(1) // mandatory with default 1, also for containers
                        ._case(new AccessCertificationCaseType()
                                .id(1L)
                                .stageNumber(1)
                                .iteration(1)
                                .workItem(new AccessCertificationWorkItemType()
                                        .assigneeRef(user1Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT)
                                        .stageNumber(11)
                                        .iteration(1)
                                        .assigneeRef(user1Oid, UserType.COMPLEX_TYPE))
                                .workItem(new AccessCertificationWorkItemType()
                                        .assigneeRef(user1Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT)
                                        .stageNumber(12)
                                        .iteration(1)))
                        ._case(new AccessCertificationCaseType()
                                .id(2L)
                                .stageNumber(2)
                                .iteration(2)
                                .targetRef(user1Oid, UserType.COMPLEX_TYPE)
                                .workItem(new AccessCertificationWorkItemType()
                                        .assigneeRef(user1Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT)
                                        .assigneeRef(user2Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT)
                                        .output(new AbstractWorkItemOutputType().outcome("OUTCOME one"))
                                        .stageNumber(21)
                                        .iteration(1))
                                .workItem(new AccessCertificationWorkItemType()
                                        .assigneeRef(user1Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT)
                                        .assigneeRef(user2Oid, UserType.COMPLEX_TYPE, ORG_DEFAULT)
                                        .output(new AbstractWorkItemOutputType().outcome("OUTCOME one"))
                                        .stageNumber(22)
                                        .iteration(1)))
                        .asPrismObject(),
                null, result);

        connector1Oid = repositoryService.addObject(
                new ConnectorType()
                        .name("conn-1")
                        .connectorBundle("com.connector.package")
                        .connectorType("ConnectorTypeClass")
                        .connectorVersion("1.2.3")
                        .framework(SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN)
                        .connectorHostRef(connectorHostOid, ConnectorHostType.COMPLEX_TYPE)
                        .targetSystemType("type1")
                        .targetSystemType("type2")
                        .asPrismObject(), null, result);
        connector2Oid = repositoryService.addObject(
                new ConnectorType()
                        .name("conn-2")
                        .connectorBundle("com.connector.package")
                        .connectorType("ConnectorTypeClass")
                        .connectorVersion("1.2.3")
                        .framework(SchemaConstants.UCF_FRAMEWORK_URI_BUILTIN)
                        .asPrismObject(), null, result);

        // objects for OID range tests
        List.of("00000000-1000-0000-0000-000000000000",
                "00000000-1000-0000-0000-000000000001",
                "10000000-1000-0000-0000-000000000000",
                "10000000-1000-0000-0000-100000000000",
                "10ffffff-ffff-ffff-ffff-ffffffffffff",
                "11000000-0000-0000-0000-000000000000",
                "11000000-1000-0000-0000-100000000000",
                "11000000-1000-0000-0000-100000000001",
                "11ffffff-ffff-ffff-ffff-fffffffffffe",
                "11ffffff-ffff-ffff-ffff-ffffffffffff",
                "20ffffff-ffff-ffff-ffff-ffffffffffff",
                "ff000000-0000-0000-0000-000000000000",
                "ffffffff-ffff-ffff-ffff-ffffffffffff").forEach(oid -> {
            try {
                repositoryService.addObject(
                        new ServiceType().oid(oid).name(oid)
                                .costCenter("OIDTEST")
                                .asPrismObject(),
                        null, result);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertThatOperationResult(result).isSuccess();
    }

    private ObjectReferenceType createTestRefWithMetadata(
            String targetOid, QName targetType, QName relation) throws SchemaException {
        ObjectReferenceType ref = new ObjectReferenceType()
                .oid(targetOid)
                .type(targetType)
                .relation(relation);
        ref.asReferenceValue().setValueMetadata(new ValueMetadataType()
                .provenance(new ProvenanceMetadataType()
                        .acquisition(new ProvenanceAcquisitionType()
                                .channel("acquisition-channel"))));
        return ref;
    }

    // Disabled, since when used schema search in query API
    @Test(enabled = false)
    public void test100AggregateAssignmentOwnerName() throws Exception {
        var opResult = createOperationResult();
        queryRecorder.startRecording();
        try {
            var spec = AggregateQuery.forType(AssignmentType.class);
            spec.retrieve(F_NAME, ItemPath.create(new ParentPathSegment(), F_NAME))
                    .count(F_ASSIGNMENT, ItemPath.SELF_PATH)
                    .groupBy(ItemPath.create(new ParentPathSegment(), F_NAME));

            SearchResultList<PrismContainerValue<?>> result = repositoryService.searchAggregate(spec, opResult);

            assertThat(result)
                    .isNotEmpty();
            ;
        } finally {
            queryRecorder.dumpQueryBuffer();
        }

    }

    @Test
    public void test101AggregateAssignmentTargetName() throws Exception {
        var opResult = createOperationResult();
        queryRecorder.startRecording();
        try {
            var spec = AggregateQuery.forType(AssignmentType.class);

            spec.retrieve(F_NAME, ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(), F_NAME))
                    .count(F_ASSIGNMENT, ItemPath.SELF_PATH);
                    // .groupBy(ItemPath.create(new ParentPathSegment(), F_NAME));

            SearchResultList<PrismContainerValue<?>> result = repositoryService.searchAggregate(spec, opResult);

            assertThat(result)
                    .isNotEmpty();
            ;
        } finally {
            queryRecorder.dumpQueryBuffer();
        }

    }

    @Test
    public void test102AggregateAssignmentTargetRef() throws Exception {
        var opResult = createOperationResult();
        queryRecorder.startRecording();

        try {
            var spec = AggregateQuery.forType(AssignmentType.class);
            spec.retrieve(AssignmentType.F_TARGET_REF)
                .count(F_ASSIGNMENT, ItemPath.SELF_PATH)
            ;

            SearchResultList<PrismContainerValue<?>> result = repositoryService.searchAggregate(spec, opResult);

            assertThat(result)
                    .isNotEmpty();
        } finally {
            queryRecorder.dumpQueryBuffer();
        }

    }

    @Test
    public void test200AggregateAssignmentTargetRefRetrieveFullObject() throws Exception {
        // owner name
        var opResult = createOperationResult();
        var dereferencedName = ItemPath.create(AssignmentType.F_TARGET_REF, new ObjectReferencePathSegment(), F_NAME);
        queryRecorder.startRecording();
        try {
            var spec = AggregateQuery.forType(AssignmentType.class);
            spec.retrieveFullObject(AssignmentType.F_TARGET_REF)
                    .retrieve(F_NAME, dereferencedName)
                    .count(F_ASSIGNMENT, ItemPath.SELF_PATH)
            ;


            spec.orderBy(spec.getResultItem(F_ASSIGNMENT), OrderDirection.DESCENDING);

            SearchResultList<PrismContainerValue<?>> result = repositoryService.searchAggregate(spec, opResult);

            assertThat(result)
                    .isNotEmpty();
            ;
        } finally {
            queryRecorder.dumpQueryBuffer();
        }

    }

    @Test
    public void test300AggregateWorkItems() throws Exception {
        // owner name
        var opResult = createOperationResult();
        var dereferencedName = ItemPath.create(AccessCertificationWorkItemType.F_ASSIGNEE_REF, new ObjectReferencePathSegment(), F_NAME);
        var outcomePath = ItemPath.create(AccessCertificationWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME);
        queryRecorder.startRecording();
        try {
            var spec = AggregateQuery.forType(AccessCertificationWorkItemType.class)
                    .resolveNames()
                    .retrieve(AccessCertificationWorkItemType.F_ASSIGNEE_REF) // Resolver assignee
                    .retrieve(AbstractWorkItemOutputType.F_OUTCOME, outcomePath)
                    .count(AccessCertificationCaseType.F_WORK_ITEM, ItemPath.SELF_PATH);

            spec.filter(PrismContext.get().queryFor(AbstractWorkItemType.class)
                    .ownerId(accCertCampaign1Oid)
                    .buildFilter()
            );
            spec.orderBy(spec.getResultItem(AccessCertificationWorkItemType.F_ASSIGNEE_REF), OrderDirection.DESCENDING);

            SearchResultList<PrismContainerValue<?>> result = repositoryService.searchAggregate(spec, opResult);

            assertThat(result)
                    .isNotEmpty();
            ;
        } finally {
            queryRecorder.dumpQueryBuffer();
        }

    }

    @Test
    public void test400AggregateRoleMembershipRefs() throws SchemaException {
        var opResult = createOperationResult();
        var spec = AggregateQuery.forType(UserType.class);
        spec.retrieve(F_ROLE_MEMBERSHIP_REF)
                .count(new ItemName("count"), F_ROLE_MEMBERSHIP_REF)
                .groupBy(F_ROLE_MEMBERSHIP_REF);
        queryRecorder.startRecording();
        try {
            SearchResultList<PrismContainerValue<?>> result = repositoryService.searchAggregate(spec, opResult);
            assertThat(result)
                    .isNotEmpty();
        } finally {
            queryRecorder.dumpQueryBuffer();
        }
    }




    // endregion
    private boolean refMatches(ObjectReferenceType ref,
            @Nullable String ownerOid, String targetOid, QName relation) {
        PrismObject<?> parentObject = PrismValueUtil.getParentObject(ref.asReferenceValue());
        return (ownerOid == null || parentObject != null && parentObject.getOid().equals(ownerOid))
                && ref.getOid().equals(targetOid)
                && QNameUtil.match(ref.getRelation(), relation);
    }
}
