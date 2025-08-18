package com.evolveum.midpoint.model.intest.rbac;

import java.io.File;
import java.util.List;
import javax.xml.namespace.QName;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
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

    private static final TestObject<SystemConfigurationType> GLOBAL_POLICY_ASSIGNMENT_RELATION =
            TestObject.file(TEST_DIR, "global-policy-assignment-relation.xml", SystemObjectsType.SYSTEM_CONFIGURATION.value());

    private static final TestObject<ArchetypeType> ARCHETYPE_SERVICE =
            TestObject.file(TEST_DIR, "archetype-service.xml", "fefefefe-2223-4e2d-975a-bce0fc69b305");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObject(ARCHETYPE_SERVICE, initTask, initResult);
    }

    /**
     * Test for MID-10779
     *
     * todo disabled now because fix in {@link com.evolveum.midpoint.model.impl.lens.assignments.TargetsEvaluation}
     *  caused performance issues.
     */
    @Test(enabled = false)
    public void test200AssignmentRelationGlobalPolicyActivationOwner() throws Exception {
        test200AssignmentRelationGlobalPolicyActivation(SchemaConstants.ORG_OWNER);
    }

    /**
     * Test for MID-10779
     *
     * todo disabled now because fix in {@link com.evolveum.midpoint.model.impl.lens.assignments.TargetsEvaluation}
     *      *  caused performance issues.
     */
    @Test(enabled = false)
    public void test200AssignmentRelationGlobalPolicyActivationDefault() throws Exception {
        test200AssignmentRelationGlobalPolicyActivation(SchemaConstants.ORG_DEFAULT);
    }

    private void test200AssignmentRelationGlobalPolicyActivation(QName relation) throws Exception {
        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        given();

        ServiceType service = new ServiceType();
        service.setOid("abababab-2223-4e2d-975a-bce0fc69b300");
        service.setName(PrismTestUtil.createPolyStringType("test200Service"));
        service.beginAssignment()
                .targetRef(ARCHETYPE_SERVICE.oid, ArchetypeType.COMPLEX_TYPE);

        final String serviceOid = addObject(service.asPrismObject(), ModelExecuteOptions.create().overwrite(), task, result);

        UserType user = new UserType();
        user.setOid("cdcdcdcd-2223-4e2d-975a-bce0fc69b301");
        final String userName = "test200User";
        user.setName(PrismTestUtil.createPolyStringType(userName));

        final String userOid = addObject(user.asPrismObject(), ModelExecuteOptions.create().overwrite(), task, result);

        replaceGlobalPolicyRuleWithRelation(relation, task, result);

        when("Add owner assignment to the user");

        ObjectDelta<UserType> addDelta =
                PrismTestUtil.getPrismContext().deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(
                                new AssignmentType()
                                        .targetRef(serviceOid, ServiceType.COMPLEX_TYPE, relation))
                        .asObjectDelta(userOid);

        executeChanges(addDelta, null, task, result);

        then("Check that the user has the owner assignment to the service");

        // user should have the owner assignment to the service
        user = getObject(UserType.class, userOid).asObjectable();
        Assertions.assertThat(user.getAssignment())
                .hasSize(1);

        Assertions.assertThat(user.getRoleMembershipRef()).hasSize(1);

        ObjectReferenceType ref = user.getRoleMembershipRef().get(0);
        Assertions.assertThat(ref.getOid()).isEqualTo(serviceOid);
        Assertions.assertThat(ref.getType()).isEqualTo(ServiceType.COMPLEX_TYPE);
        Assertions.assertThat(ref.getRelation()).isEqualTo(relation);

        // service should have user name in description
        service = getObject(ServiceType.class, serviceOid).asObjectable();
        Assertions.assertThat(service.getDescription()).isEqualTo(userName);

        when("Delete owner assignment from the user");

        user = getObject(UserType.class, userOid).asObjectable();
        Assertions.assertThat(user.getAssignment()).hasSize(1);

        ObjectDelta<UserType> deleteDelta = PrismTestUtil.getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT).delete(user.getAssignment().get(0).clone())
                .asObjectDelta(userOid);

        executeChanges(deleteDelta, null, task, result);

        then("Check that the user has no owner assignment to the service");

        user = getObject(UserType.class, userOid).asObjectable();
        // no assignment, no role membership
        Assertions.assertThat(user.getAssignment()).hasSize(0);
        Assertions.assertThat(user.getRoleMembershipRef()).hasSize(0);

        // service should have no description
        service = getObject(ServiceType.class, serviceOid).asObjectable();
        Assertions.assertThat(service.getDescription()).isNull();
    }

    private void replaceGlobalPolicyRuleWithRelation(QName relation, Task task, OperationResult result) throws Exception {
        GlobalPolicyRuleType rule = GLOBAL_POLICY_ASSIGNMENT_RELATION.getObjectable().getGlobalPolicyRule().get(0).clone();

        List<QName> assignment = rule.getPolicyConstraints().getAssignment().get(0).getRelation();
        assignment.clear();
        assignment.add(relation);

        List<QName> linkTargetRelations = rule.getPolicyActions().getScriptExecution().get(0).getObject().getLinkTarget().get(0).getRelation();
        linkTargetRelations.clear();
        linkTargetRelations.add(relation);

        modifyObjectReplaceContainer(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), SystemConfigurationType.F_GLOBAL_POLICY_RULE, task, result, rule);
    }
}
