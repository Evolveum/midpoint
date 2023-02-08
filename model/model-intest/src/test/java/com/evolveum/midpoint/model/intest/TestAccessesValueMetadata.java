/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ASSIGNMENT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ROLE_MEMBERSHIP_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType.F_ROLE_MANAGEMENT;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.ValueSelector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.UserAsserter;
import com.evolveum.midpoint.test.asserter.predicates.AssertionPredicate;
import com.evolveum.midpoint.test.asserter.predicates.SimplifiedGenericAssertionPredicate;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

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
    }

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
        // @formatter:off
        assertUser(userOid, "after")
            .displayXml() // XML also shows the metadata
            .assertRoleMembershipRefs(3)
            .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(businessRole1Oid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                        .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                            assertAssignmentPathSegments(businessRole1Oid))
                        .end()
                    .end()
                .end()
            .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(appRole1Oid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                        .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                            assertAssignmentPathSegments(businessRole1Oid, appRole1Oid))
                        .end()
                    .end()
                .end()
            .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(appService1Oid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                        .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                            assertAssignmentPathSegments(businessRole1Oid, appRole1Oid, appService1Oid));
        // @formatter:on
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
        // @formatter:off
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
            .displayXml() // XML also shows the metadata
            .assertRoleMembershipRefs(5);
        userAsserter.valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(businessRole1Oid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                        .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                            assertAssignmentPathSegments(businessRole1Oid))
                        .end()
                    .end()
                .end()
            .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(appRole1Oid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                        .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                            assertAssignmentPathSegments(businessRole1Oid, appRole1Oid))
                        .end()
                    .end()
                .end()
            .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(businessRole1bOid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                        .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                            assertAssignmentPathSegments(businessRole1bOid))
                        .end()
                    .end()
                .end()
            .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(appRole1bOid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                        .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                            assertAssignmentPathSegments(businessRole1bOid, appRole1bOid));
        // @formatter:on

        // TODO figure out how to assert the two paths in two different metadata containers
        // 5th value is checked differently, as the selection of the value for match is just as complicated as the match.
        Collection<ValueMetadataType> metadataValues = userAsserter
                .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(appService1Oid))
                .getRealValues();
        Assertions.assertThat(metadataValues)
                .hasSize(2)
                .allMatch(m -> m.getStorage() != null && m.getStorage().getCreateTimestamp() != null)
                .extracting(m -> m.getProvenance().getAssignmentPath())
                .anySatisfy(assignmentPathSegmentsCheck(businessRole1Oid, appRole1Oid, appService1Oid))
                .anySatisfy(assignmentPathSegmentsCheck(businessRole1bOid, appRole1bOid, appService1Oid));
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

        when("user is modified and business role 1b is removed");
        executeChanges(prismContext.deltaFor(UserType.class)
                        .item(F_ASSIGNMENT).delete(new AssignmentType()
                                .targetRef(createObjectReference(businessRole1bOid,
                                        RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)))
                        .<UserType>asObjectDelta(userOid),
                null, task, result);

        // @formatter:off
        assertUser(userOid, "after")
            .displayXml() // XML also shows the metadata
            .assertRoleMembershipRefs(3)
            .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(businessRole1Oid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                        .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                            assertAssignmentPathSegments(businessRole1Oid))
                        .end()
                    .end()
                .end()
            .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(appRole1Oid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                        .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                            assertAssignmentPathSegments(businessRole1Oid, appRole1Oid))
                        .end()
                    .end()
                .end()
            .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(appService1Oid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                        .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                            assertAssignmentPathSegments(businessRole1Oid, appRole1Oid, appService1Oid));
        // @formatter:on
    }

    @Test
    public void test900AccessesMetadataNotStoredWithoutSysconfigOption() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("sysconfig with accesses metadata option missing");
        ObjectDelta<SystemConfigurationType> delta = prismContext.deltaFor(SystemConfigurationType.class)
                .item(F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_ACCESSES_METADATA_ENABLED).replace()
                .asObjectDelta(SystemObjectsType.SYSTEM_CONFIGURATION.value());
        executeChanges(delta, null, task, result);

        when("new user with assignment is added");
        String userOid = addObject(newUserWithBusinessRole1(), task, result);

        then("roleMembershipRefs have no value metadata for accesses");
        assertNoRoleMembershipRefMetadata(userOid, businessRole1Oid, appRole1Oid, appService1Oid);
    }

    @Test
    public void test910AccessesMetadataAreAddedAfterRecompute() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("sysconfig with accesses metadata option missing");
        executeChanges(prismContext.deltaFor(SystemConfigurationType.class)
                        .item(F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_ACCESSES_METADATA_ENABLED).replace()
                        .<SystemConfigurationType>asObjectDelta(SystemObjectsType.SYSTEM_CONFIGURATION.value()),
                null, task, result);

        and("new user with assignment without accesses metadata");
        String userOid = addObject(newUserWithBusinessRole1(), task, result);
        assertNoRoleMembershipRefMetadata(userOid, businessRole1Oid, appRole1Oid, appService1Oid);

        when("accesses metadata is enabled in sysconfig");
        executeChanges(prismContext.deltaFor(SystemConfigurationType.class)
                        .item(F_ROLE_MANAGEMENT, RoleManagementConfigurationType.F_ACCESSES_METADATA_ENABLED).replace(true)
                        .<SystemConfigurationType>asObjectDelta(SystemObjectsType.SYSTEM_CONFIGURATION.value()),
                null, task, result);

        and("user is recomputed");
        // TODO currently this does NOT add metadata
        // The reason is that FocusChangeExecution.execute() L92 if considers the delta empty and doesn't call the modification.
        // The change removing the old roleMembershipRefs before "reconcile(Focus)" would force the metadata creation.
//        executeChanges(prismContext.deltaFor(UserType.class)
//                        .item(F_ROLE_MEMBERSHIP_REF).replace()
//                        .<UserType>asObjectDelta(userOid),
//                null, task, result);
        executeChanges(prismContext.deltaFor(UserType.class)
                        .<UserType>asObjectDelta(userOid),
                ModelExecuteOptions.create().reconcileFocus(),
                task, result);

        then("roleMembershipRefs have now value metadata for accesses");
        assertUser(userOid, "after")
                .displayXml()
        // TODO asserts, when the test scenario is finalized
        ;
    }

    private UserType newUserWithBusinessRole1() {
        return new UserType()
                .name("user-" + getTestNumber())
                .assignment(new AssignmentType()
                        .targetRef(createObjectReference(businessRole1Oid,
                                RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)));
    }

    private AssertionPredicate<AssignmentPathMetadataType> assertAssignmentPathSegments(String... targetOids) {
        // A bit of a hack, we're using AssertionPredicate, but actually leaving failure to the AssertJ here.
        return new SimplifiedGenericAssertionPredicate<>(ap -> {
            SoftAssertions check = new SoftAssertions();
            check.assertThat(ap.getSegment())
                    .extracting(s -> s.getTargetRef().getOid())
                    .containsExactly(targetOids);

            return check.wasSuccess() ? null
                    : "Assignment path segments error " + check.assertionErrorsCollected();
        });
    }

    private Consumer<AssignmentPathMetadataType> assignmentPathSegmentsCheck(String... expectedTargetOids) {
        return ap -> Assertions.assertThat(ap.getSegment())
                .extracting(s -> s.getTargetRef().getOid())
                .containsExactly(expectedTargetOids);
    }

    private void assertNoRoleMembershipRefMetadata(String userOid, String... roleMembershipRefTargetOids)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        UserAsserter<Void> userAsserter = assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .assertRoleMembershipRefs(roleMembershipRefTargetOids.length);
        for (String targetOid : roleMembershipRefTargetOids) {
            userAsserter.valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(targetOid))
                    .assertNullOrNoValues();
        }
    }
}
