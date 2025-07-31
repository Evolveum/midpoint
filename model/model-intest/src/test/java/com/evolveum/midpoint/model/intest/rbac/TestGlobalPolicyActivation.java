/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.rbac;

import java.io.File;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.test.util.OperationResultAssert;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestGlobalPolicyActivation extends AbstractInitializedModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(TestGlobalPolicyActivation.class);

    private static final File TEST_DIR = new File("./src/test/resources/rbac");

    private static final TestObject<SystemConfigurationType> GLOBAL_POLICY_RULE =
            TestObject.file(TEST_DIR, "global-policy-rule-activation.xml", SystemObjectsType.SYSTEM_CONFIGURATION.value());

    private static final TestObject<SystemConfigurationType> GLOBAL_POLICY_ASSIGNMENT_RELATION =
            TestObject.file(TEST_DIR, "global-policy-assignment-relation.xml", SystemObjectsType.SYSTEM_CONFIGURATION.value());

    private static final TestObject<ArchetypeType> ARCHETYPE_SERVICE =
            TestObject.file(TEST_DIR, "archetype-service.xml", "fefefefe-2223-4e2d-975a-bce0fc69b305");

    /**
     * Test for MID-10743
     */
    @Test
    public void test100PolicyActivationOnUserDelete() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();

        // create empty role
        RoleType sampleRole = new RoleType();
        sampleRole.setName(PrismTestUtil.createPolyStringType("remove_policy_test"));
        final String sampleRoleOid = addObject(sampleRole, task, result);

        UserType user = new UserType();
        user.setName(PrismTestUtil.createPolyStringType("first"));
        user.beginAssignment()
                .targetRef(sampleRoleOid, RoleType.COMPLEX_TYPE)
                .end();

        String userOid = addObject(user, task, result);

        when();

        modifyObjectReplaceContainer(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), SystemConfigurationType.F_GLOBAL_POLICY_RULE, task, result);

        TestObject.FileBasedTestObjectSource file = (TestObject.FileBasedTestObjectSource) GLOBAL_POLICY_RULE.source;
        transplantGlobalPolicyRulesAdd(file.getFile(), task, result);

        deleteObject(UserType.class, userOid, task, result);

        then();

        new OperationResultAssert(result)
                .isSuccess();
    }

    /**
     * Test for MID-10779
     */
    @Test(enabled = false)
    public void test200AssignmentRelationGlobalPolicyActivation() throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();

        // remove any existing global policy rules
        modifyObjectReplaceContainer(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), SystemConfigurationType.F_GLOBAL_POLICY_RULE, task, result);

        addObject(ARCHETYPE_SERVICE, task, result);

        ServiceType service = new ServiceType();
        service.setName(PrismTestUtil.createPolyStringType("test200Service"));
        service.beginAssignment()
                .targetRef(ARCHETYPE_SERVICE.oid, ArchetypeType.COMPLEX_TYPE);

        final String serviceOid = addObject(service, task, result);

        TestObject.FileBasedTestObjectSource file = (TestObject.FileBasedTestObjectSource) GLOBAL_POLICY_ASSIGNMENT_RELATION.source;
        transplantGlobalPolicyRulesAdd(file.getFile(), task, result);

        UserType user = new UserType();
        final String userName = "test200User";
        user.setName(PrismTestUtil.createPolyStringType(userName));

        final String userOid = addObject(user, task, result);

        when("Add owner assignment to the user");

        ObjectDelta<UserType> addDelta =
                PrismTestUtil.getPrismContext().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(
                                new AssignmentType()
                                        .targetRef(serviceOid, ServiceType.COMPLEX_TYPE, SchemaConstants.ORG_OWNER))
                        .asObjectDelta(userOid);

        traced(() -> executeChanges(addDelta, null, task, result));

        then();

        // user should have the owner assignment to the service
        user = getObject(UserType.class, userOid).asObjectable();
        Assertions.assertThat(user.getAssignment())
                .hasSize(1);

        Assertions.assertThat(user.getRoleMembershipRef()).hasSize(1);

        ObjectReferenceType ref = user.getRoleMembershipRef().get(0);
        Assertions.assertThat(ref.getOid()).isEqualTo(serviceOid);
        Assertions.assertThat(ref.getType()).isEqualTo(ServiceType.COMPLEX_TYPE);
        Assertions.assertThat(ref.getRelation()).isEqualTo(SchemaConstants.ORG_OWNER);

        // service should have user name in description
        service = getObject(ServiceType.class, serviceOid).asObjectable();
        Assertions.assertThat(service.getDescription()).isEqualTo(userName);  // todo enable

        when("Delete owner assignment from the user");

        user = getObject(UserType.class, userOid).asObjectable();
        Assertions.assertThat(user.getAssignment()).hasSize(1);

        ObjectDelta<UserType> deleteDelta = PrismTestUtil.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).delete(user.getAssignment().get(0).clone())
                .asObjectDelta(userOid);

        traced(() -> executeChanges(deleteDelta, null, task, result));

        user = getObject(UserType.class, userOid).asObjectable();
        // no assignment, no role membership
        Assertions.assertThat(user.getAssignment()).hasSize(0);
        Assertions.assertThat(user.getRoleMembershipRef()).hasSize(0);

        // service should have no description
        service = getObject(ServiceType.class, serviceOid).asObjectable();
        Assertions.assertThat(service.getDescription()).isNull();
    }
}
