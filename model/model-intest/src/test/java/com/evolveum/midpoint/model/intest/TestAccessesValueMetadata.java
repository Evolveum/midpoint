/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.*;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.ValueSelector;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestAccessesValueMetadata extends AbstractEmptyModelIntegrationTest {

    // business role 1 inducing app role 1 inducing app service 1
    private String businessRole1Oid;
    private String appRole1Oid;
    private String appService1Oid;

    // alternative business role 1b inducing app role 1b inducing the app service 1 from above
    private String businessRole1bOid;
    private String appRole1bOid;
    // alternative business role that induces two app roles, both inducing a single service ("diamond")
    private String businessRole1cOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult)
            throws Exception {
        super.initSystem(initTask, initResult);

        appService1Oid = addObject(new ServiceType().name("app-service-1"), initTask, initResult);
        appRole1Oid = addObject(new RoleType()
                        .name("app-role-1")
                        .inducement(new AssignmentType()
                                .targetRef(createObjectReference(appService1Oid,
                                        ServiceType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT))),
                initTask, initResult);
        businessRole1Oid = addObject(new RoleType()
                        .name("business-role-1")
                        .inducement(new AssignmentType()
                                .targetRef(createObjectReference(appRole1Oid,
                                        RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT))),
                initTask, initResult);

        appRole1bOid = addObject(new RoleType()
                        .name("app-role-1b")
                        .inducement(new AssignmentType()
                                .targetRef(createObjectReference(appService1Oid,
                                        ServiceType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT))),
                initTask, initResult);
        businessRole1bOid = addObject(new RoleType()
                        .name("business-role-1b")
                        .inducement(new AssignmentType()
                                .targetRef(createObjectReference(appRole1bOid,
                                        RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT))),
                initTask, initResult);
        businessRole1cOid = addObject(new RoleType()
                        .name("business-role-1c")
                        .inducement(new AssignmentType()
                                .targetRef(createObjectReference(appRole1Oid,
                                        RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)))
                        .inducement(new AssignmentType()
                                .targetRef(createObjectReference(appRole1bOid,
                                        RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT))),
                initTask, initResult);
    }

    /**
     * Sysconfig is loaded only before the first test in the class.
     * Some tests switch the toggle ON/OFF, but let's try to keep it ON at the end,
     * so each test can be run both in sequence and/or separately.
     */
    @Override
    protected File getSystemConfigurationFile() {
        return new File(COMMON_DIR, "system-configuration-enabled-accesses-metadata.xml");
    }

    @Test
    public void test100AddUserWithAssignmentToBusinessRole1() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("new user with assignment to business role");
        UserType user = newUserWithBusinessRole1();

        when("user is added");
        String userOid = addObject(user, task, result);

        then("roleMembershipRefs contain value metadata with accesses information");
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(3);
        assertAssignmentPath(userAsserter, businessRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid));
        assertAssignmentPath(userAsserter, appRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid));
        assertAssignmentPath(userAsserter, appService1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid, appService1Oid));
    }

    @Test
    public void test200AddUserWithTwoAssignmentsInducingTheSameRole() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("new user with assignments to business role 1 and 1b");
        UserType user = newUserWithBusinessRole1()
                .assignment(new AssignmentType()
                        .targetRef(createObjectReference(businessRole1bOid,
                                RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)));

        when("user is added");
        String userOid = addObject(user, task, result);

        then("roleMembershipRefs contain value metadata with accesses information");
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(5);
        assertAssignmentPath(userAsserter, businessRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid));
        assertAssignmentPath(userAsserter, appRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid));
        assertAssignmentPath(userAsserter, businessRole1bOid,
                new ExpectedAssignmentPath(businessRole1bOid));
        assertAssignmentPath(userAsserter, appRole1bOid,
                new ExpectedAssignmentPath(businessRole1bOid, appRole1bOid));
        assertAssignmentPath(userAsserter, appService1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid, appService1Oid),
                new ExpectedAssignmentPath(businessRole1bOid, appRole1bOid, appService1Oid));
    }

    @Test
    public void test250AddUserWithOneRoleInducingOneServiceViaTwoPaths() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("new user with assignments to business role 1c");
        UserType user = new UserType()
                .name("user-" + getTestNumber())
                .assignment(new AssignmentType()
                        .targetRef(createObjectReference(businessRole1cOid,
                                RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)));

        when("user is added");
        String userOid = addObject(user, task, result);

        then("roleMembershipRefs contain value metadata with accesses information");
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(4);
        assertAssignmentPath(userAsserter, businessRole1cOid,
                new ExpectedAssignmentPath(businessRole1cOid));
        assertAssignmentPath(userAsserter, appRole1Oid,
                new ExpectedAssignmentPath(businessRole1cOid, appRole1Oid));
        assertAssignmentPath(userAsserter, appRole1bOid,
                new ExpectedAssignmentPath(businessRole1cOid, appRole1bOid));
        assertAssignmentPath(userAsserter, appService1Oid,
                new ExpectedAssignmentPath(businessRole1cOid, appRole1Oid, appService1Oid),
                new ExpectedAssignmentPath(businessRole1cOid, appRole1bOid, appService1Oid));
    }

    @Test
    public void test300RemovingOneAssignmentFromUser() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with assignments to business role 1 and 1b");
        UserType user = newUserWithBusinessRole1()
                .assignment(new AssignmentType()
                        .targetRef(createObjectReference(businessRole1bOid,
                                RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)));
        String userOid = addObject(user, task, result);

        and("all storage/createTimestamps are set");
        long afterAddTs = System.currentTimeMillis();
        UserType addedUser = assertUser(userOid, "before").displayXml().getObjectable();
        assertAllStorageTimestampsAreBefore(addedUser, afterAddTs);

        when("user is modified and business role 1b is removed");
        executeChanges(prismContext.deltaFor(UserType.class)
                        .item(F_ASSIGNMENT).delete(new AssignmentType()
                                .targetRef(createObjectReference(businessRole1bOid,
                                        RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)))
                        .<UserType>asObjectDelta(userOid),
                null, task, result);

        then("metadata is still present on the left refs");
        UserAsserter<Void> modifiedUser = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(3);
        assertAssignmentPath(modifiedUser, businessRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid));
        assertAssignmentPath(modifiedUser, appRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid));
        assertAssignmentPath(modifiedUser, appService1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid, appService1Oid));

        and("original storage/createTimestamp of the value metadata is preserved");
        assertAllStorageTimestampsAreBefore(modifiedUser.getObjectable(), afterAddTs);
    }

    @Test
    public void test400AddingOneAssignmentFromUser() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("a user with assignments to business role 1");
        UserType user = newUserWithBusinessRole1();
        String userOid = addObject(user, task, result);
        long afterAddTs = System.currentTimeMillis();
        assertUser(userOid, "before").displayXml();

        when("user is modified and business role 1b is added");
        executeChanges(prismContext.deltaFor(UserType.class)
                        .item(F_ASSIGNMENT).add(new AssignmentType()
                                .targetRef(createObjectReference(businessRole1bOid,
                                        RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)))
                        .<UserType>asObjectDelta(userOid),
                null, task, result);

        then("metadata are modified as needed with original timestamp preserved for existing values");
        UserAsserter<Void> modifiedUser = assertUser(userOid, "after")
                .displayXml()
                .assertRoleMembershipRefs(5);
        assertAssignmentPath(modifiedUser, businessRole1Oid,
                new ExpectedAssignmentPath(ts -> ts < afterAddTs, businessRole1Oid));
        assertAssignmentPath(modifiedUser, appRole1Oid,
                new ExpectedAssignmentPath(ts -> ts < afterAddTs, businessRole1Oid, appRole1Oid));
        assertAssignmentPath(modifiedUser, appService1Oid,
                new ExpectedAssignmentPath(ts -> ts < afterAddTs, businessRole1Oid, appRole1Oid, appService1Oid),
                // added path to existing ref
                new ExpectedAssignmentPath(ts -> ts > afterAddTs, businessRole1bOid, appRole1bOid, appService1Oid));
        // added refs
        assertAssignmentPath(modifiedUser, businessRole1bOid,
                new ExpectedAssignmentPath(ts -> ts > afterAddTs, businessRole1bOid));
        assertAssignmentPath(modifiedUser, appRole1bOid,
                new ExpectedAssignmentPath(ts -> ts > afterAddTs, businessRole1bOid, appRole1bOid));
    }

    @Test
    public void test500AddUserWithManagerAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("new user with assignment to a role with manager relation");
        UserType user = new UserType()
                .name("user-" + getTestNumber())
                .assignment(new AssignmentType().targetRef(
                        createObjectReference(appRole1Oid, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER)));

        when("user is added");
        String userOid = addObject(user, task, result);

        then("roleMembershipRefs are created");
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(2); // details are not interesting for this test

        and("first segments of assignment path metadata have targetRef with manager relation");
        segmentsHaveExpectedRelations(userAsserter, appRole1Oid,
                SchemaConstants.ORG_MANAGER);
        segmentsHaveExpectedRelations(userAsserter, appService1Oid,
                SchemaConstants.ORG_MANAGER, SchemaConstants.ORG_DEFAULT);
    }

    @Test
    public void test550AddUserWithApproverAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("new user with assignment to a role with approver relation");
        UserType user = new UserType()
                .name("user-" + getTestNumber())
                .assignment(new AssignmentType().targetRef(
                        createObjectReference(appRole1Oid, RoleType.COMPLEX_TYPE, SchemaConstants.ORG_APPROVER)));

        when("user is added");
        String userOid = addObject(user, task, result);

        then("first segments of assignment path metadata have targetRef with approver relation");
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(1); // details are not interesting for this test
        segmentsHaveExpectedRelations(userAsserter, appRole1Oid, SchemaConstants.ORG_APPROVER);
    }

    @Test
    public void test560AddUserAndThenAddApproverAssignment() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("user with business role 1 exists");
        UserType user = newUserWithBusinessRole1();
        String userOid = addObject(user, task, result);
        assertUser(userOid, "before")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(3);

        when("business role 1 with approver relation is added");
        executeChanges(prismContext.deltaFor(UserType.class)
                        .item(F_ASSIGNMENT).add(new AssignmentType()
                                .targetRef(createObjectReference(businessRole1Oid,
                                        RoleType.COMPLEX_TYPE, SchemaConstants.ORG_APPROVER)))
                        .<UserType>asObjectDelta(userOid),
                null, task, result);

        then("role membership ref with approver relation is added, including value metadata");
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(4);
        ValueMetadataType metadata = (ValueMetadataType) userAsserter
                .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(businessRole1Oid, SchemaConstants.ORG_APPROVER))
                .assertSize(1)
                .getRealValue();
        AssignmentPathMetadataType assignmentPathMetadata = metadata.getProvenance().getAssignmentPath();
        assertThat(assignmentPathMetadata.getSegment()).hasSize(1);
        assertThat(assignmentPathMetadata.getSegment().get(0).getTargetRef().getRelation())
                .isEqualTo(SchemaConstants.ORG_APPROVER);
    }

    @Test
    public void test600AddUserWithDefaultAssignmentWithTargetRefFilter() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("new user with assignment with target ref filter");
        SearchFilterType targetSearchFilter = getQueryConverter().createSearchFilterType(
                prismContext.queryFor(RoleType.class)
                        .id(businessRole1Oid)
                        .buildFilter());
        ObjectReferenceType targetRef = new ObjectReferenceType()
                .filter(targetSearchFilter)
                .type(RoleType.COMPLEX_TYPE)
                .relation(SchemaConstants.ORG_DEFAULT);
        UserType user = new UserType()
                .name("user-" + getTestNumber())
                .assignment(new AssignmentType().targetRef(targetRef));

        when("user is added");
        String userOid = addObject(user, task, result);

        then("roleMembershipRefs are created");
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(3);

        and("their metadata are populated");
        // This first case proves that AssignmentProcessor#processMembershipAndDelegatedRefs is too late
        // for metadata creation because assignment/targetRef/oid is null there.
        assertAssignmentPath(userAsserter, businessRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid));
        assertAssignmentPath(userAsserter, appRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid));
        assertAssignmentPath(userAsserter, appService1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid, appService1Oid));
    }

    @Test
    public void test610AddUserWithApproverAssignmentWithTargetRefFilter() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("new user with assignment with target ref filter");
        SearchFilterType targetSearchFilter = getQueryConverter().createSearchFilterType(
                prismContext.queryFor(RoleType.class)
                        .id(businessRole1Oid)
                        .buildFilter());
        ObjectReferenceType targetRef = new ObjectReferenceType()
                .filter(targetSearchFilter)
                .type(RoleType.COMPLEX_TYPE)
                .relation(SchemaConstants.ORG_APPROVER);
        UserType user = new UserType()
                .name("user-" + getTestNumber())
                .assignment(new AssignmentType().targetRef(targetRef));

        when("user is added");
        String userOid = addObject(user, task, result);

        then("roleMembershipRefs are created");
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(1);

        and("their metadata are populated");
        assertAssignmentPath(userAsserter, businessRole1Oid, new ExpectedAssignmentPath(businessRole1Oid));
        segmentsHaveExpectedRelations(userAsserter, businessRole1Oid, SchemaConstants.ORG_APPROVER);
    }

    @Test(description = "MID-8664")
    public void test700ValueMetadataShouldNotBeAddedToArchetypeRefs() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("existing archetype");
        String archetypeOid = addObject(new ArchetypeType().name("archetype-" + getTestNumber()), task, result);

        when("user with the archetype is added");
        String userOid = addObject(
                new UserType()
                        .name("user-" + getTestNumber())
                        .assignment(new AssignmentType()
                                .targetRef(createObjectReference(archetypeOid,
                                        ArchetypeType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT))),
                task, result);

        then("roleMembershipRef and archetypeRef is populated");
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(1)
                .assertArchetypeRefs(1);

        and("only roleMembershipRef has value metadata, archetypeRef does not have any");
        assertAssignmentPath(userAsserter, archetypeOid, new ExpectedAssignmentPath(archetypeOid));
        userAsserter.valueMetadata(F_ARCHETYPE_REF, ValueSelector.refEquals(archetypeOid))
                .assertSize(0);
    }

    @Test
    public void test900AccessesMetadataAreOnByDefault() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("sysconfig with accesses metadata option missing");
        switchAccessesMetadata(null, task, result);

        when("new user with assignment is added");
        String userOid = addObject(newUserWithBusinessRole1(), task, result);

        then("roleMembershipRefs has metadata filled in");
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(3);
        assertAssignmentPath(userAsserter, businessRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid));
        assertAssignmentPath(userAsserter, appRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid));
        assertAssignmentPath(userAsserter, appService1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid, appService1Oid));

        // Fixing the state to initial for this test class:
        switchAccessesMetadata(true, task, result);
    }

    @Test
    public void test905AccessesMetadataAreRemovedAfterDisableAndRecompute() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("sysconfig with accesses metadata option on");
        switchAccessesMetadata(true, task, result);

        and("added user has metadata");
        String userOid = addObject(newUserWithBusinessRole1(), task, result);
        assertUser(userOid, "initial").assertRoleMembershipRefs(3);

        when("option is turned off and user is recomputed");
        switchAccessesMetadata(false, task, result);
        recomputeUser(userOid, task, result);

        then("roleMembershipRefs have no value metadata for accesses");
        assertNoRoleMembershipRefMetadata(userOid, businessRole1Oid, appRole1Oid, appService1Oid);

        // Fixing the state to initial for this test class:
        switchAccessesMetadata(true, task, result);
    }

    @Test
    public void test910AccessesMetadataDisabledThenEnabledAndAddedAfterRecompute() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("sysconfig with accesses metadata option missing");
        switchAccessesMetadata(false, task, result);

        and("new user with assignment without accesses metadata");
        String userOid = addObject(newUserWithBusinessRole1(), task, result);
        assertNoRoleMembershipRefMetadata(userOid, businessRole1Oid, appRole1Oid, appService1Oid);
        // Assignment (or whole object) creation is before this timestamp.
        long afterAddTs = System.currentTimeMillis();

        when("accesses metadata is enabled in sysconfig");
        switchAccessesMetadata(true, task, result);

        and("user is recomputed");
        recomputeUser(userOid, task, result);

        then("roleMembershipRefs have now value metadata for accesses");
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(3);
        assertAssignmentPath(userAsserter, businessRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid));
        assertAssignmentPath(userAsserter, appRole1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid));
        assertAssignmentPath(userAsserter, appService1Oid,
                new ExpectedAssignmentPath(businessRole1Oid, appRole1Oid, appService1Oid));
        assertAllStorageTimestampsAreBefore(userAsserter.getObjectable(), afterAddTs);
    }

    private UserType newUserWithBusinessRole1() {
        return new UserType()
                .name("user-" + getTestNumber())
                .assignment(new AssignmentType()
                        .targetRef(createObjectReference(businessRole1Oid,
                                RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)));
    }

    private void assertNoRoleMembershipRefMetadata(String userOid, String... roleMembershipRefTargetOids)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        UserAsserter<Void> userAsserter = assertUser(userOid, "no-value-metadata")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(roleMembershipRefTargetOids.length);
        for (String targetOid : roleMembershipRefTargetOids) {
            userAsserter.valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(targetOid))
                    .assertNullOrNoValues();
        }
    }

    /** Checks that the ref with the first OID parameter has the specified assignment paths with expected target OIDs. */
    private void assertAssignmentPath(UserAsserter<Void> userAsserter,
            String roleMembershipTargetOid, ExpectedAssignmentPath... expectedAssignmentPaths)
            throws SchemaException {
        Collection<ValueMetadataType> metadataValues = userAsserter
                .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(roleMembershipTargetOid))
                .assertSize(expectedAssignmentPaths.length)
                .getRealValues();
        userAsserter.end(); // to fix the state of asserter back after valueMetadata() call
        var listAsserter = assertThat(metadataValues)
                .hasSize(expectedAssignmentPaths.length);
        // Now we check if any of the values match the expected value - for each expected value.
        for (ExpectedAssignmentPath expectedAssignmentPath : expectedAssignmentPaths) {
            listAsserter
                    .withFailMessage("No value metadata for roleMembershipRef with target OID "
                            + roleMembershipTargetOid + " match the expected assignment path "
                            + Arrays.toString(expectedAssignmentPath.targetRefOids) + " (or its timestamp predicate).")
                    .anySatisfy(m -> {
                        assertThat(m)
                                .extracting(ValueMetadataType::getStorage)
                                .extracting(StorageMetadataType::getCreateTimestamp)
                                .extracting(MiscUtil::asMillis)
                                .isNotNull()
                                .matches(ts -> expectedAssignmentPath.storageCreateTimestampPredicate.test(ts));
                        assertThat(m)
                                .extracting(ValueMetadataType::getProvenance)
                                .extracting(ProvenanceMetadataType::getAssignmentPath)
                                .extracting(ap -> ap.getSegment(), listAsserterFactory(AssignmentPathSegmentMetadataType.class))
                                .extracting(s -> s.getTargetRef().getOid())
                                .containsExactly(expectedAssignmentPath.targetRefOids);
                    });
        }

        // We also want to check that each path has assignmentId in its first segment.
        for (ValueMetadataType metadataValue : metadataValues) {
            AssignmentPathMetadataType assignmentPath = metadataValue.getProvenance().getAssignmentPath();
            assertThat(assignmentPath.getSegment().get(0).getAssignmentId())
                    .withFailMessage(() -> "assignmentId must not be null in the first segment for path " + assignmentPath)
                    .isNotNull();
        }
    }

    private void assertAllStorageTimestampsAreBefore(UserType user, long referenceMillis) {
        for (ObjectReferenceType ref : user.getRoleMembershipRef()) {
            for (PrismContainerValue<Containerable> metadataValue : ref.asReferenceValue().getValueMetadata().getValues()) {
                ValueMetadataType metadata = metadataValue.getRealValue();
                assertThat(metadata)
                        .extracting(m -> m.getStorage())
                        .extracting(s -> s.getCreateTimestamp())
                        .extracting(ts -> MiscUtil.asMillis(ts))
                        .satisfies(l -> assertThat(l)
                                .as("storage/createTimestamp of value metadata for ref " + ref)
                                .isLessThan(referenceMillis));
            }
        }
    }

    private void segmentsHaveExpectedRelations(
            UserAsserter<Void> userAsserter, String membershipTargetOid, QName... expectedRelations)
            throws SchemaException {
        ValueMetadataType metadata = (ValueMetadataType) userAsserter
                .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(membershipTargetOid))
                .assertSize(1)
                .getRealValue();
        List<AssignmentPathSegmentMetadataType> segments = metadata.getProvenance().getAssignmentPath().getSegment();
        assertThat(segments).hasSameSizeAs(expectedRelations);
        for (int i = 0; i < expectedRelations.length; i++) {
            assertThat(segments.get(i).getTargetRef().getRelation())
                    .describedAs("segment #%d for role membership ref to %s", i, membershipTargetOid)
                    .isEqualTo(expectedRelations[i]);
        }
    }

    static class ExpectedAssignmentPath {
        String[] targetRefOids; // in the order in the path

        /** Additional condition on timestamp, no need to check not null, which is always done where needed. */
        Predicate<Long> storageCreateTimestampPredicate;

        public ExpectedAssignmentPath(String... targetRefOids) {
            this.targetRefOids = targetRefOids;
            this.storageCreateTimestampPredicate = ts -> true; // by default no special condition
        }

        public ExpectedAssignmentPath(
                Predicate<Long> storageCreateTimestampPredicate, String... targetRefOids) {
            this.targetRefOids = targetRefOids;
            this.storageCreateTimestampPredicate = storageCreateTimestampPredicate;
        }
    }
}
