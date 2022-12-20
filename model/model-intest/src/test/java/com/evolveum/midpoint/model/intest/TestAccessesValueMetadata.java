/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest;

import java.io.File;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.ValueSelector;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.predicates.AssertionPredicate;
import com.evolveum.midpoint.test.asserter.predicates.SimplifiedGenericAssertionPredicate;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestAccessesValueMetadata extends AbstractEmptyModelIntegrationTest {

    private String businessRole1Oid;
    private String appRole1Oid;
    private String appService1Oid;

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
    }

    @Override
    protected File getSystemConfigurationFile() {
        return new File(COMMON_DIR, "system-configuration-enabled-accesses-metadata.xml");
    }

    @Test
    public void test100AddUserWithAssignmentToBusinessRole1() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given();

        when();
        String user1Oid = addObject(new UserType()
                        .name("user-1")
                        .assignment(new AssignmentType()
                                .targetRef(createObjectReference(businessRole1Oid,
                                        RoleType.COMPLEX_TYPE, SchemaConstants.ORG_DEFAULT))),
                task, result);

        then();
        // @formatter:off
        assertUser(user1Oid, "after")
            .displayXml() // XML also shows the metadata
            .valueMetadata(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(businessRole1Oid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                            .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                                    assertAssignmentPathSegments(businessRole1Oid))
                        .getReturnAsserter()
                    .getReturnAsserter()
                .getReturnAsserter()
            .valueMetadata(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(appRole1Oid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                            .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                                    assertAssignmentPathSegments(businessRole1Oid, appRole1Oid))
                        .getReturnAsserter()
                    .getReturnAsserter()
                .getReturnAsserter()
            .valueMetadata(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF, ValueSelector.refEquals(appService1Oid))
                .singleValue()
                    .provenance()
                        .assertItemsExactly(ProvenanceMetadataType.F_ASSIGNMENT_PATH)
                            .assertItemValueSatisfies(ProvenanceMetadataType.F_ASSIGNMENT_PATH,
                                    assertAssignmentPathSegments(businessRole1Oid, appRole1Oid, appService1Oid));
        // @formatter:on
    }

    private AssertionPredicate<AssignmentPathType> assertAssignmentPathSegments(String... targetOids) {
        // A bit of a hack, we're using AssertionPredicate, but actually leaving failure to the AssertJ here.
        return new SimplifiedGenericAssertionPredicate<>(ap -> {
            Assertions.assertThat(ap.getSegment())
                    .extracting(s -> s.getTargetRef().getOid())
                    .containsExactly(targetOids);

            return null;
        });
    }

    // TODO test200AddUserWithTwoAssignmentsInducingTheSameRole

    // TODO test900AccessesMetadataNotStoredWithoutSysconfigOption
}
