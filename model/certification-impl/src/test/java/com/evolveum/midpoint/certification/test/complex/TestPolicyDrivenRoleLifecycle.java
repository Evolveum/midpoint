/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.test.complex;

import com.evolveum.midpoint.certification.test.AbstractUninitializedCertificationTest;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.WorkflowService;
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
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.testng.AssertJUnit.assertEquals;

/**
 * A complex policy-drive role lifecycle scenario (see https://wiki.evolveum.com/display/midPoint/Sample+scenario).
 */
@ContextConfiguration(locations = {"classpath:ctx-certification-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestPolicyDrivenRoleLifecycle extends AbstractUninitializedCertificationTest {

    protected static final File TEST_DIR = new File("src/test/resources/complex");
    public static final File SYSTEM_CONFIGURATION_FILE = new File(TEST_DIR, "system-configuration.xml");

    protected static final File ROLE_EMPTY_FILE = new File(TEST_DIR, "role-empty.xml");
    protected static final File ROLE_HIGH_RISK_EMPTY_FILE = new File(TEST_DIR, "role-high-risk-empty.xml");
    protected static final File ROLE_CORRECT_FILE = new File(TEST_DIR, "role-correct.xml");
    protected static final File ROLE_CORRECT_HIGH_RISK_FILE = new File(TEST_DIR, "role-correct-high-risk.xml");

    protected static final String SITUATION_INCOMPLETE_ROLE = "http://sample.org/situations#incomplete-role-c1-to-c4";
    protected static final String SITUATION_ACTIVE_ROLE_WITH_NO_IDENTIFIER = "http://sample.org/situations#active-role-with-no-identifier";

    protected static String roleEmptyOid;
    protected static String roleHighRiskEmptyOid;
    protected static String roleCorrectOid;
    protected static String roleCorrectHighRiskOid;
    protected static String userJackOid;

    protected static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");

    protected static final File ASSIGNMENT_CERT_DEF_FILE = new File(TEST_DIR, "adhoc-certification-assignment.xml");
    protected static final String ASSIGNMENT_CERT_DEF_OID = "540940e9-4ac5-4340-ba85-fd7e8b5e6686";

    protected static final File MODIFICATION_CERT_DEF_FILE = new File(TEST_DIR, "adhoc-certification-modification.xml");
    protected static final String MODIFICATION_CERT_DEF_OID = "83a16584-bb2a-448c-aee1-82fc6d577bcb";

    protected static final File ORG_LABORATORY_FILE = new File(TEST_DIR, "org-laboratory.xml");
    protected static final String ORG_LABORATORY_OID = "027faec7-7763-4b26-ab92-c5c0acbb1173";

    protected static final File USER_INDIGO_FILE = new File(TEST_DIR, "user-indigo.xml");
    protected static final String USER_INDIGO_OID = "11b35bd2-9b2f-4a00-94fa-7ed0079a7500";

    protected AccessCertificationDefinitionType assignmentCertificationDefinition;
    protected AccessCertificationDefinitionType modificationCertificationDefinition;

    @Autowired private WorkflowService workflowService;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        userJackOid = addAndRecompute(USER_JACK_FILE, initTask, initResult);
        roleEmptyOid = addAndRecompute(ROLE_EMPTY_FILE, initTask, initResult);
        roleHighRiskEmptyOid = addAndRecompute(ROLE_HIGH_RISK_EMPTY_FILE, initTask, initResult);
        roleCorrectOid = addAndRecompute(ROLE_CORRECT_FILE, initTask, initResult);
        roleCorrectHighRiskOid = addAndRecompute(ROLE_CORRECT_HIGH_RISK_FILE, initTask, initResult);
    }

    @NotNull
    @Override
    protected File getSystemConfigurationFile() {
        return SYSTEM_CONFIGURATION_FILE;
    }

    @Test
    public void test010AttemptToActivateIncompleteRoleC1345() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN+THEN
        when();
        then();
        Holder<LensContext<?>> contextHolder = new Holder<>();
        activateRoleAssertFailure(roleEmptyOid, contextHolder, result, task);

        PrismObject<RoleType> role = getRole(roleEmptyOid);
        display("role after", role);

        dumpRules(contextHolder);
        assertEquals("Wrong policy situation", singletonList(SITUATION_INCOMPLETE_ROLE), role.asObjectable().getPolicySituation());
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

    @Test
    public void test020AttemptToActivateIncompleteRoleC234() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN+THEN
        when();
        then();
        Holder<LensContext<?>> contextHolder = new Holder<>();
        activateRoleAssertFailure(roleHighRiskEmptyOid, contextHolder, result, task);

        PrismObject<RoleType> role = getRole(roleHighRiskEmptyOid);
        display("role after", role);

        dumpRules(contextHolder);
        assertEquals("Wrong policy situation", singletonList(SITUATION_INCOMPLETE_ROLE), role.asObjectable().getPolicySituation());
    }

    @Test
    public void test030AttemptToActivateCorrectRoleC34() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN+THEN
        when();
        then();
        Holder<LensContext<?>> contextHolder = new Holder<>();
        activateRoleAssertFailure(roleCorrectOid, contextHolder, result, task);

        PrismObject<RoleType> role = getRole(roleCorrectOid);
        display("role after", role);

        dumpRules(contextHolder);
        assertEquals("Wrong policy situation", singletonList(SITUATION_INCOMPLETE_ROLE), role.asObjectable().getPolicySituation());
    }

    private void dumpRules(Holder<LensContext<?>> contextHolder) {
        System.out.println(contextHolder.getValue().dumpFocusPolicyRules(0, true));
    }

    @Test
    public void test040AssignOwnerAndApproverToCorrectRole() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN+THEN
        when();
        then();
        ModelExecuteOptions noApprovals = executeOptions().partialProcessing(new PartialProcessingOptionsType().approvals(SKIP));
        assignRole(USER_ADMINISTRATOR_OID, roleCorrectOid, SchemaConstants.ORG_APPROVER, noApprovals, task, result);
        assignRole(USER_ADMINISTRATOR_OID, roleCorrectOid, SchemaConstants.ORG_OWNER, noApprovals, task, result);

        // recompute the role to set correct policy situation
        recomputeFocus(RoleType.class, roleCorrectOid, task, result);
    }

    @Test
    public void test050ActivateCorrectRole() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        Holder<LensContext<?>> contextHolder = new Holder<>();
        activateRole(roleCorrectOid, contextHolder, task, result);

        // THEN
        then();

        PrismObject<RoleType> roleAfter = getRole(roleCorrectOid);
        display("role after", roleAfter);
        assertEquals("Wrong (changed) lifecycle state", SchemaConstants.LIFECYCLE_DRAFT, roleAfter.asObjectable().getLifecycleState());

        dumpRules(contextHolder);
        assertEquals("Wrong policy situation", emptyList(), roleAfter.asObjectable().getPolicySituation());
        assertEquals("Wrong triggered policy rules", emptyList(), roleAfter.asObjectable().getTriggeredPolicyRule());

//        Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
//                .item(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_WORK_ITEM).retrieve()
//                .build();
        List<PrismObject<CaseType>> cases = getCasesForObject(roleCorrectOid, RoleType.COMPLEX_TYPE, null, task, result);
        display("cases for role", cases);
        assertEquals("Wrong # of approval cases for role", 2, cases.size());

        CaseType approvalCase = getApprovalCase(cases);
        CaseType rootCase = getRootCase(cases);
        ApprovalContextType wfc = approvalCase.getApprovalContext();
        //assertEquals("Modification of correct", wfc.getProcessInstanceName());            // MID-4200
        assertEquals("wrong # of work items", 1, approvalCase.getWorkItem().size());
        CaseWorkItemType workItem = approvalCase.getWorkItem().get(0);
        assertEquals("wrong # of approval stages", 1, wfc.getApprovalSchema().getStage().size());
        assertEquals("wrong # of attached policy rules", 1, wfc.getPolicyRules().getEntry().size());
        EvaluatedPolicyRuleType rule = wfc.getPolicyRules().getEntry().get(0).getRule();
        List<EvaluatedPolicyRuleTriggerType> triggers = rule.getTrigger();

        // TODO check trigger

        workflowService.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext),
                task, result);
        waitForCaseClose(rootCase, 60000);

        PrismObject<RoleType> roleAfterApproval = getRole(roleCorrectOid);
        display("role after approval", roleAfterApproval);
        assertEquals("Wrong (unchanged) lifecycle state", SchemaConstants.LIFECYCLE_ACTIVE, roleAfterApproval.asObjectable().getLifecycleState());

        assertEquals("Wrong policy situation", emptyList(), roleAfter.asObjectable().getPolicySituation());
        assertEquals("Wrong triggered policy rules", emptyList(), roleAfter.asObjectable().getTriggeredPolicyRule());
    }

    @Test
    public void test060AssignOwnerAndApproverToCorrectHighRiskRole() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN+THEN
        when();
        then();
        ModelExecuteOptions noApprovals = executeOptions().partialProcessing(new PartialProcessingOptionsType().approvals(SKIP));
        assignRole(USER_ADMINISTRATOR_OID, roleCorrectHighRiskOid, SchemaConstants.ORG_APPROVER, noApprovals, task, result);
        assignRole(userJackOid, roleCorrectHighRiskOid, SchemaConstants.ORG_APPROVER, noApprovals, task, result);
        assignRole(USER_ADMINISTRATOR_OID, roleCorrectHighRiskOid, SchemaConstants.ORG_OWNER, noApprovals, task, result);

        // recompute the role to set correct policy situation
        recomputeFocus(RoleType.class, roleCorrectHighRiskOid, task, result);
    }

    @Test
    public void test070ActivateCorrectHighRiskRole() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        Holder<LensContext<?>> contextHolder = new Holder<>();
        activateRole(roleCorrectHighRiskOid, contextHolder, task, result);

        // THEN
        then();

        PrismObject<RoleType> roleAfter = getRole(roleCorrectHighRiskOid);
        display("role after", roleAfter);
        assertEquals("Wrong (changed) lifecycle state", SchemaConstants.LIFECYCLE_DRAFT, roleAfter.asObjectable().getLifecycleState());

        dumpRules(contextHolder);

        assertEquals("Wrong policy situation", emptyList(), roleAfter.asObjectable().getPolicySituation());
        assertEquals("Wrong triggered policy rules", emptyList(), roleAfter.asObjectable().getTriggeredPolicyRule());

        Collection<SelectorOptions<GetOperationOptions>> options = schemaHelper.getOperationOptionsBuilder()
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

        workflowService.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext),
                task, result);

        approvalCase = modelService.getObject(CaseType.class, approvalCase.getOid(), options, task, result).asObjectable();
        List<CaseWorkItemType> openWorkItems = approvalCase.getWorkItem().stream().filter(i -> i.getCloseTimestamp() == null)
                .collect(Collectors.toList());
        assertEquals("wrong # of open work items", 1, openWorkItems.size());
        workItem = openWorkItems.get(0);
        workflowService.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext),
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
                .asObjectDeltaCast(oid);
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
