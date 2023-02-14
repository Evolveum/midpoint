/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.certification.test.complex;

import static java.util.Collections.*;
import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.certification.test.AbstractUninitializedCertificationTest;
import com.evolveum.midpoint.model.api.CaseService;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.RecordingProgressListener;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.schema.util.cases.ApprovalUtils;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.TestResource;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * A complex policy-drive role lifecycle scenario (see https://docs.evolveum.com/midpoint/devel/design/policy-constraints/sample-scenario/).
 */
@ContextConfiguration(locations = { "classpath:ctx-certification-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPolicyDrivenRoleLifecycle extends AbstractUninitializedCertificationTest {

    private static final File TEST_DIR = new File("src/test/resources/complex");
    public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    private static final TestResource<RoleType> ROLE_EMPTY =
            new TestResource<>(TEST_DIR, "role-empty.xml", "d985878b-9cd7-449f-94f4-9d8fc2534229");

    private static final TestResource<RoleType> ROLE_HIGH_RISK_EMPTY =
            new TestResource<>(TEST_DIR, "role-high-risk-empty.xml", "c8e88dd2-5e3b-40e5-a1c7-2af8686e1857");

    private static final TestResource<RoleType> ROLE_CORRECT =
            new TestResource<>(TEST_DIR, "role-correct.xml", "ab0e3dd4-3cdf-4c5e-b349-e7b4904730a1");

    private static final File ROLE_CORRECT_HIGH_RISK_FILE = new File(TEST_DIR, "role-correct-high-risk.xml");

    private static final String SITUATION_INCOMPLETE_ROLE = "http://sample.org/situations#incomplete-role-c1-to-c4";
    private static final String SITUATION_ACTIVE_ROLE_WITH_NO_IDENTIFIER = "http://sample.org/situations#active-role-with-no-identifier";

    private static String roleCorrectHighRiskOid;
    private static String userJackOid;

    private static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");

    @Autowired private CaseService caseService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        userJackOid = addAndRecompute(USER_JACK_FILE, initTask, initResult);
        addAndRecompute(ROLE_EMPTY, initTask, initResult);
        addAndRecompute(ROLE_HIGH_RISK_EMPTY, initTask, initResult);
        addAndRecompute(ROLE_CORRECT, initTask, initResult);
        roleCorrectHighRiskOid = addAndRecompute(ROLE_CORRECT_HIGH_RISK_FILE, initTask, initResult);
    }

    @NotNull
    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    /**
     * Trying to activate role that cannot be activated e.g. because of rule R1.
     * (Among others.)
     */
    @Test
    public void test010AttemptToActivateIncompleteRoleC1345() throws Exception {
        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        Holder<LensContext<?>> contextHolder = new Holder<>();

        expect();
        activateRoleAssertFailure(ROLE_EMPTY.oid, contextHolder, result, task);

        dumpRules(contextHolder);

        assertRoleAfter(ROLE_EMPTY.oid)
                .assertPolicySituations(SITUATION_INCOMPLETE_ROLE); // see rule RE1
    }

    private void activateRoleAssertFailure(String roleOid, Holder<LensContext<?>> contextHolder, OperationResult result, Task task)
            throws SchemaException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException,
            SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        try {
            activateRole(roleOid, contextHolder, task, result);
            fail("unexpected success");
        } catch (PolicyViolationException e) {
            System.out.println("Got expected exception:");
            e.printStackTrace(System.out);
        }
    }

    /**
     * Trying to activate `role-high-risk-empty` that does not meet e.g. C2 (missing description) but others as well.
     */
    @Test
    public void test020AttemptToActivateIncompleteRoleC234() throws Exception {
        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        Holder<LensContext<?>> contextHolder = new Holder<>();

        expect();
        activateRoleAssertFailure(ROLE_HIGH_RISK_EMPTY.oid, contextHolder, result, task);

        dumpRules(contextHolder);

        assertRoleAfter(ROLE_HIGH_RISK_EMPTY.oid)
                .assertPolicySituations(SITUATION_INCOMPLETE_ROLE); // see rule RE1
    }

    /**
     * Trying to activate `role-correct` that meets C1-C3 but has no owner (C4).
     */
    @Test
    public void test030AttemptToActivateCorrectRoleC34() throws Exception {
        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        Holder<LensContext<?>> contextHolder = new Holder<>();

        expect();
        activateRoleAssertFailure(ROLE_CORRECT.oid, contextHolder, result, task);

        dumpRules(contextHolder);

        assertRoleAfter(ROLE_CORRECT.oid)
                .assertPolicySituations(SITUATION_INCOMPLETE_ROLE); // see rule RE1
    }

    private void dumpRules(Holder<LensContext<?>> contextHolder) {
        displayValue("focus policy rules",
                contextHolder.getValue().dumpObjectPolicyRules(0, true));
    }

    /**
     * Now assigning owner and approver to `role-correct`, making it eligible for activation.
     */
    @Test
    public void test040AssignOwnerAndApproverToCorrectRole() throws Exception {
        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        when();

        // We don't want to activate approval policy rules for these operations.
        ModelExecuteOptions noApprovals = executeOptions()
                .partialProcessing(new PartialProcessingOptionsType().approvals(SKIP));
        assignRole(USER_ADMINISTRATOR_OID, ROLE_CORRECT.oid, SchemaConstants.ORG_APPROVER, noApprovals, task, result);
        assignRole(USER_ADMINISTRATOR_OID, ROLE_CORRECT.oid, SchemaConstants.ORG_OWNER, noApprovals, task, result);

        // recompute the role to set correct policy situation
        recomputeFocus(RoleType.class, ROLE_CORRECT.oid, task, result);

        then();
        assertRoleAfter(ROLE_CORRECT.oid);
    }

    /**
     * Now let's try to activate the `role-correct` again.
     */
    @Test
    public void test050ActivateCorrectRole() throws Exception {
        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        Holder<LensContext<?>> contextHolder = new Holder<>();

        when();
        activateRole(ROLE_CORRECT.oid, contextHolder, task, result);

        then();

        dumpRules(contextHolder);

        var roleAfter = assertRoleAfter(ROLE_CORRECT.oid)
                .assertLifecycleState(SchemaConstants.LIFECYCLE_DRAFT) // should be unchanged
                .assertPolicySituations() // no situations should be there
                .assertNoTriggeredPolicyRules()
                .getObjectable();

        List<PrismObject<CaseType>> cases =
                getCasesForObject(ROLE_CORRECT.oid, RoleType.COMPLEX_TYPE, null, task, result);
        display("cases for role", cases);
        assertEquals("Wrong # of approval cases for role", 2, cases.size());

        CaseType approvalCase = getApprovalCase(cases);
        CaseType rootCase = getRootCase(cases);
        ApprovalContextType wfc = approvalCase.getApprovalContext();
        assertEquals("wrong # of work items", 1, approvalCase.getWorkItem().size());
        CaseWorkItemType workItem = approvalCase.getWorkItem().get(0);
        assertEquals("wrong # of approval stages", 1, wfc.getApprovalSchema().getStage().size());
        assertEquals("wrong # of attached policy rules", 1, wfc.getPolicyRules().getEntry().size());
        EvaluatedPolicyRuleType rule = wfc.getPolicyRules().getEntry().get(0).getRule();
        List<EvaluatedPolicyRuleTriggerType> triggers = rule.getTrigger();

        // TODO check trigger

        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(),
                task, result);
        waitForCaseClose(rootCase, 60000);

        PrismObject<RoleType> roleAfterApproval = getRole(ROLE_CORRECT.oid);
        display("role after approval", roleAfterApproval);
        assertEquals("Wrong (unchanged) lifecycle state", SchemaConstants.LIFECYCLE_ACTIVE, roleAfterApproval.asObjectable().getLifecycleState());

        assertEquals("Wrong policy situation", emptyList(), roleAfter.getPolicySituation());
        assertEquals("Wrong triggered policy rules", emptyList(), roleAfter.getTriggeredPolicyRule());
    }

    @Test
    public void test060AssignOwnerAndApproverToCorrectHighRiskRole() throws Exception {
        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        expect();
        ModelExecuteOptions noApprovals = executeOptions().partialProcessing(new PartialProcessingOptionsType().approvals(SKIP));
        assignRole(USER_ADMINISTRATOR_OID, roleCorrectHighRiskOid, SchemaConstants.ORG_APPROVER, noApprovals, task, result);
        assignRole(userJackOid, roleCorrectHighRiskOid, SchemaConstants.ORG_APPROVER, noApprovals, task, result);
        assignRole(USER_ADMINISTRATOR_OID, roleCorrectHighRiskOid, SchemaConstants.ORG_OWNER, noApprovals, task, result);

        // recompute the role to set correct policy situation
        recomputeFocus(RoleType.class, roleCorrectHighRiskOid, task, result);
    }

    @Test
    public void test070ActivateCorrectHighRiskRole() throws Exception {
        given();
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        when();
        Holder<LensContext<?>> contextHolder = new Holder<>();
        activateRole(roleCorrectHighRiskOid, contextHolder, task, result);

        then();
        PrismObject<RoleType> roleAfter = getRole(roleCorrectHighRiskOid);
        display("role after", roleAfter);
        assertEquals("Wrong (changed) lifecycle state", SchemaConstants.LIFECYCLE_DRAFT, roleAfter.asObjectable().getLifecycleState());

        dumpRules(contextHolder);

        assertEquals("Wrong policy situation", emptyList(), roleAfter.asObjectable().getPolicySituation());
        assertEquals("Wrong triggered policy rules", emptyList(), roleAfter.asObjectable().getTriggeredPolicyRule());

        Collection<SelectorOptions<GetOperationOptions>> options = schemaService.getOperationOptionsBuilder()
                .build();
        List<PrismObject<CaseType>> cases = getCasesForObject(roleCorrectHighRiskOid, RoleType.COMPLEX_TYPE, options, task, result);
        display("cases for role", cases);
        assertEquals("Wrong # of approval cases for role", 2, cases.size());

        CaseType approvalCase = getApprovalCase(cases);
        ApprovalContextType wfc = approvalCase.getApprovalContext();
        // assertEquals("Modification of correct-high-risk", wfc.getProcessInstanceName());             // MID-4200
        assertEquals("wrong # of work items", 1, approvalCase.getWorkItem().size());
        CaseWorkItemType workItem = approvalCase.getWorkItem().get(0);
        assertEquals("wrong # of approval stages", 2, wfc.getApprovalSchema().getStage().size());
        assertEquals("wrong # of attached policy rules", 2, wfc.getPolicyRules().getEntry().size());

        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(),
                task, result);

        approvalCase = modelService.getObject(CaseType.class, approvalCase.getOid(), options, task, result).asObjectable();
        List<CaseWorkItemType> openWorkItems = approvalCase.getWorkItem().stream().filter(i -> i.getCloseTimestamp() == null)
                .collect(Collectors.toList());
        assertEquals("wrong # of open work items", 1, openWorkItems.size());
        workItem = openWorkItems.get(0);
        caseService.completeWorkItem(
                WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(),
                task, result);

        CaseType rootCase = getRootCase(cases);
        waitForCaseClose(rootCase, 60000);

        PrismObject<RoleType> roleAfterApproval = getRole(roleCorrectHighRiskOid);
        display("role after approval", roleAfterApproval);
        assertEquals("Wrong (unchanged) lifecycle state", SchemaConstants.LIFECYCLE_ACTIVE, roleAfterApproval.asObjectable().getLifecycleState());

        assertEquals("Wrong policy situation", singletonList(SITUATION_ACTIVE_ROLE_WITH_NO_IDENTIFIER), roleAfterApproval.asObjectable().getPolicySituation());
        assertEquals("Wrong triggered policy rules", emptyList(), roleAfterApproval.asObjectable().getTriggeredPolicyRule());   // recording rules = none
    }

    private void activateRole(String oid, Holder<LensContext<?>> contextHolder, Task task, OperationResult result)
            throws SchemaException, CommunicationException, ObjectAlreadyExistsException, ExpressionEvaluationException,
            PolicyViolationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException {
        ObjectDelta<RoleType> delta = prismContext.deltaFor(RoleType.class)
                .item(RoleType.F_LIFECYCLE_STATE)
                .replace(SchemaConstants.LIFECYCLE_ACTIVE)
                .asObjectDelta(oid);
        RecordingProgressListener listener = new RecordingProgressListener();
        try {
            modelService.executeChanges(singleton(delta), null, task, singleton(listener), result);
        } finally {
            if (contextHolder != null) {
                contextHolder.setValue((LensContext<?>) listener.getModelContext());
            }
        }
    }
}
