/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType.F_ROLE_MEMBERSHIP_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType.F_ROLE_MANAGEMENT;

import java.io.File;

import org.assertj.core.api.SoftAssertions;
import org.jetbrains.annotations.NotNull;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.ValueSelector;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.predicates.AssertionPredicate;
import com.evolveum.midpoint.test.asserter.predicates.SimplifiedGenericAssertionPredicate;
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
        UserType user = new UserType()
                .name("user-100")
                .assignment(new AssignmentType()
                        .targetRef(createObjectReference(businessRole1Oid,
                                RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)));

        when("user is added");
        String userOid = addObject(user, task, result);

        then("roleMembershipRefs contain value metadata with accesses information");
        // @formatter:off
        assertUser(userOid, "after")
            .displayXml() // XML also shows the metadata
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
        UserType user = new UserType()
                .name("user-200")
                .assignment(new AssignmentType()
                        .targetRef(createObjectReference(businessRole1Oid,
                                RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)))
                .assignment(new AssignmentType()
                        .targetRef(createObjectReference(businessRole1bOid,
                                RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT)));

        when("user is added");
        String userOid = addObject(user, task, result);

        then("roleMembershipRefs contain value metadata with accesses information");
        // @formatter:off
        assertUser(userOid, "after")
            .displayXml() // XML also shows the metadata
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
                        .container(ProvenanceMetadataType.F_ASSIGNMENT_PATH, AssignmentPathType.class)
                            .assertSize(2)
                            .value(assignmentPathByFirstTargetOidSelector(businessRole1Oid))
                                .assertValue(assertAssignmentPathSegments(businessRole1Oid, appRole1Oid, appService1Oid))
                                .end()
                            .value(assignmentPathByFirstTargetOidSelector(businessRole1bOid))
                                .assertValue(assertAssignmentPathSegments(businessRole1bOid, appRole1bOid, appService1Oid));
        // @formatter:on
    }

    @NotNull
    private ValueSelector<PrismContainerValue<AssignmentPathType>> assignmentPathByFirstTargetOidSelector(
            String firstSegmentTargetOid) {
        return pcv -> pcv.asContainerable().getSegment().get(0)
                .getTargetRef().getOid().equals(firstSegmentTargetOid);
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
        String userOid = addObject(new UserType()
                        .name("user-900")
                        .assignment(new AssignmentType()
                                .targetRef(createObjectReference(businessRole1Oid,
                                        RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT))),
                task, result);

        then("roleMembershipRefs have no value metadata for accesses");
        assertUser(userOid, "after")
                .displayXml() // XML also shows the metadata
                .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(businessRole1Oid))
                .assertNullOrNoValues().end()
                .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(appRole1Oid))
                .assertNullOrNoValues().end()
                .valueMetadata(F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(appService1Oid))
                .assertNullOrNoValues();
    }

    private AssertionPredicate<AssignmentPathType> assertAssignmentPathSegments(String... targetOids) {
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
}
