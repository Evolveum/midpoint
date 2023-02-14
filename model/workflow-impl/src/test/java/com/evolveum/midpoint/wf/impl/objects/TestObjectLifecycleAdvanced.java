/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.objects;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.util.Collections;
import java.util.List;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.RecordingProgressListener;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.DeltaFactory;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.asserter.prism.PrismObjectAsserter;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.wf.impl.AbstractWfTestPolicy;
import com.evolveum.midpoint.wf.impl.ExpectedTask;
import com.evolveum.midpoint.wf.impl.ExpectedWorkItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Testing approvals of various triggered object-level constraints.
 * In a way it's an extension of role lifecycle tests.
 */
@ContextConfiguration(locations = { "classpath:ctx-workflow-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestObjectLifecycleAdvanced extends AbstractWfTestPolicy {

    private static final File TEST_OBJECT_RESOURCE_DIR = new File("src/test/resources/objects-advanced");

    private static final File METAROLE_CONSTRAINTS_FILE = new File(TEST_OBJECT_RESOURCE_DIR, "metarole-constraints.xml");
    protected static final File ROLE_EMPLOYEE_FILE = new File(TEST_OBJECT_RESOURCE_DIR, "role-employee.xml");
    protected static final File USER_EMPLOYEE_OWNER_FILE = new File(TEST_OBJECT_RESOURCE_DIR, "user-employee-owner.xml");
    protected static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_OBJECT_RESOURCE_DIR, "system-configuration.xml");

    protected String metaroleConstraintsOid;
    protected String userEmployeeOwnerOid;

    String roleEmployeeOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        metaroleConstraintsOid = addAndRecompute(METAROLE_CONSTRAINTS_FILE, initTask, initResult);
        userEmployeeOwnerOid = addAndRecomputeUser(USER_EMPLOYEE_OWNER_FILE, initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Test
    public void test010CreateRoleEmployee() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        PrismObject<RoleType> employee = prismContext.parseObject(ROLE_EMPLOYEE_FILE);
        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<RoleType> lensContext = createLensContext(RoleType.class);
                addFocusDeltaToContext(lensContext, DeltaFactory.Object.createAddDelta(employee));
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) throws Exception {
//                        ModelContext taskModelContext = temporaryHelper.getModelContext(rootCase, opTask, result);
//                        ObjectDelta realDelta0 = taskModelContext.getFocusContext().getPrimaryDelta();
//                        assertTrue("Non-empty primary focus delta: " + realDelta0.debugDump(), realDelta0.isEmpty());
                assertNoObject(employee);
                ExpectedTask expectedTask = new ExpectedTask(null, "Adding role \"" + employee.asObjectable().getName().getOrig() + "\"");
                ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(userEmployeeOwnerOid, null, expectedTask);
                assertWfContextAfterClockworkRun(rootCase, subcases, workItems,
                        null,
                        Collections.singletonList(expectedTask),
                        Collections.singletonList(expectedWorkItem));

                CaseType subcase = subcases.get(0);
                ApprovalContextType wfc = subcase.getApprovalContext();
                assertEquals("Wrong # of attached policy rules entries", 1, wfc.getPolicyRules().getEntry().size());
                SchemaAttachedPolicyRuleType attachedRule = wfc.getPolicyRules().getEntry().get(0);
                assertEquals(1, attachedRule.getStageMin().intValue());
                assertEquals(1, attachedRule.getStageMax().intValue());
                assertEquals("Wrong # of attached triggers", 1, attachedRule.getRule().getTrigger().size());
                EvaluatedPolicyRuleTriggerType trigger = attachedRule.getRule().getTrigger().get(0);
                assertEquals("Wrong constraintKind in trigger", PolicyConstraintKindType.OBJECT_MODIFICATION, trigger.getConstraintKind());

                CaseWorkItemType workItem = subcases.get(0).getWorkItem().get(0);
                assertEquals("Wrong # of additional information", 0, workItem.getAdditionalInformation().size());
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask,
                    OperationResult result) throws Exception {
                assertNoObject(employee);
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) {
                new PrismObjectAsserter<>(employee).assertSanity();
            }

            @Override
            protected boolean executeImmediately() {
                return false;
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) throws Exception {
                login(getUser(userEmployeeOwnerOid));
                return true;
            }
        }, 1);

        roleEmployeeOid = searchObjectByName(RoleType.class, "employee").getOid();

        PrismReferenceValue employeeOwner = getPrismContext().itemFactory().createReferenceValue(roleEmployeeOid, RoleType.COMPLEX_TYPE).relation(SchemaConstants.ORG_OWNER);
        executeChanges(prismContext.deltaFor(UserType.class)
                        .item(UserType.F_ASSIGNMENT).add(ObjectTypeUtil.createAssignmentTo(employeeOwner, prismContext))
                        .asObjectDelta(userEmployeeOwnerOid),
                null, task, result);
        display("Employee role", getRole(roleEmployeeOid));
        display("Employee owner", getUser(userEmployeeOwnerOid));
    }

    @Test
    public void test020ActivateIncompleteRole() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        @SuppressWarnings({ "raw" })
        ObjectDelta<RoleType> activateRoleDelta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_ACTIVE)
                .asObjectDelta(roleEmployeeOid);

        RecordingProgressListener recordingListener = new RecordingProgressListener();
        try {
            modelService.executeChanges(Collections.singleton(activateRoleDelta), null, task,
                    Collections.singleton(recordingListener), result);
            fail("unexpected success");
        } catch (PolicyViolationException e) {
            System.out.println("Got expected exception: " + e.getMessage());
        }

        //noinspection unchecked
        LensContext<RoleType> context = (LensContext<RoleType>) recordingListener.getModelContext();
        System.out.println(context.dumpObjectPolicyRules(0));
        EvaluatedPolicyRule incompleteActivationRule = context.getFocusContext().getObjectPolicyRules().stream()
                .filter(rule -> "disallow-incomplete-role-activation".equals(rule.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("rule not found"));
        assertEquals("Wrong # of triggers in incompleteActivationRule", 2, incompleteActivationRule.getTriggers().size());  // objectState + or
    }

    /**
     * This time let's fill-in the description as well.
     */
    @Test
    public void test030ActivateIncompleteRoleAgain() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        @SuppressWarnings({ "raw" })
        ObjectDelta<RoleType> activateRoleDelta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_ACTIVE)
                .item(RoleType.F_DESCRIPTION).replace("hi")
                .asObjectDelta(roleEmployeeOid);

        RecordingProgressListener recordingListener = new RecordingProgressListener();
        try {
            modelService.executeChanges(Collections.singleton(activateRoleDelta), null, task,
                    Collections.singleton(recordingListener), result);
            fail("unexpected success");
        } catch (PolicyViolationException e) {
            System.out.println("Got expected exception: " + e.getMessage());
        }

        //noinspection unchecked
        LensContext<RoleType> context = (LensContext<RoleType>) recordingListener.getModelContext();
        System.out.println(context.dumpObjectPolicyRules(0));
        EvaluatedPolicyRule incompleteActivationRule = context.getFocusContext().getObjectPolicyRules().stream()
                .filter(rule -> "disallow-incomplete-role-activation".equals(rule.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("rule not found"));
        assertEquals("Wrong # of triggers in incompleteActivationRule", 2, incompleteActivationRule.getTriggers().size());
    }

    @Test
    public void test040AddApprover() throws Exception {
        login(userAdministrator);

        Task task = getTestTask();
        OperationResult result = getTestOperationResult();

        assignRole(userEmployeeOwnerOid, roleEmployeeOid, SchemaConstants.ORG_APPROVER, task, result);
        result.computeStatus();
        assertSuccess(result);
    }

    @Test
    public void test045ActivateCompleteRole() throws Exception {
        login(userAdministrator);

        ObjectDelta<RoleType> activateRoleDelta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_LIFECYCLE_STATE).replace(SchemaConstants.LIFECYCLE_ACTIVE)
                .item(RoleType.F_DESCRIPTION).replace("hi")
                .asObjectDelta(roleEmployeeOid);

        executeTest(new TestDetails() {
            @Override
            protected LensContext createModelContext(OperationResult result) throws Exception {
                LensContext<RoleType> lensContext = createLensContext(RoleType.class);
                addFocusDeltaToContext(lensContext, activateRoleDelta);
                return lensContext;
            }

            @Override
            protected void afterFirstClockworkRun(CaseType rootCase,
                    CaseType case0, List<CaseType> subcases,
                    List<CaseWorkItemType> workItems,
                    Task opTask, OperationResult result) throws Exception {
                ExpectedTask expectedTask = new ExpectedTask(null, "Matching state: after operation (\"active lifecycleState\")");
                ExpectedWorkItem expectedWorkItem = new ExpectedWorkItem(userEmployeeOwnerOid, null, expectedTask);
                assertWfContextAfterClockworkRun(rootCase, subcases, workItems,
                        null,
                        Collections.singletonList(expectedTask),
                        Collections.singletonList(expectedWorkItem));

                ApprovalContextType wfc = subcases.get(0).getApprovalContext();
                assertEquals("Wrong # of attached policy rules entries", 1, wfc.getPolicyRules().getEntry().size());
                SchemaAttachedPolicyRuleType attachedRule = wfc.getPolicyRules().getEntry().get(0);
                assertEquals(1, attachedRule.getStageMin().intValue());
                assertEquals(1, attachedRule.getStageMax().intValue());
                assertEquals("Wrong # of attached triggers", 1, attachedRule.getRule().getTrigger().size());
                EvaluatedPolicyRuleTriggerType trigger = attachedRule.getRule().getTrigger().get(0);
                assertEquals("Wrong constraintKind in trigger", PolicyConstraintKindType.TRANSITION, trigger.getConstraintKind());

                CaseWorkItemType workItem = subcases.get(0).getWorkItem().get(0);
                assertEquals("Wrong # of additional information", 0, workItem.getAdditionalInformation().size());
            }

            @Override
            protected void afterCase0Finishes(CaseType rootCase, Task opTask, OperationResult result) {
                //assertNoObject(employee);
            }

            @Override
            protected void afterRootCaseFinishes(CaseType rootCase, List<CaseType> subcases,
                    Task opTask, OperationResult result) {
                //assertObject(employee);
            }

            @Override
            protected boolean executeImmediately() {
                return false;
            }

            @Override
            protected Boolean decideOnApproval(CaseWorkItemType caseWorkItem) throws Exception {
                login(getUser(userEmployeeOwnerOid));
                return true;
            }
        }, 1);
    }
}
