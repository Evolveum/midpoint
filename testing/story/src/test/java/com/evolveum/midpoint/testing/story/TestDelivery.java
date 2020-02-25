/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.testing.story;

import com.evolveum.midpoint.model.api.WorkflowService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ApprovalContextUtil;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.wf.util.ApprovalUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;

import static org.testng.AssertJUnit.assertNotNull;

/**
 *
 * @author mederly
 *
 */

@SuppressWarnings("FieldCanBeLocal")
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestDelivery extends AbstractStoryTest {

    @Autowired private WorkflowService workflowService;
    //@Autowired private DummyTransport dummyTransport;

    private static final String TEST_DIR = "src/test/resources/delivery";
    private static final String ORG_DIR = TEST_DIR + "/orgs";
    private static final String ROLES_DIR = TEST_DIR + "/roles";
    private static final String RULES_DIR = TEST_DIR + "/rules";
    private static final String USERS_DIR = TEST_DIR + "/users";

    public static final String NS_EXT = "http://midpoint.evolveum.com/xml/ns/story/delivery/ext";

    private static final File ORG_MONKEY_ISLAND_FILE = new File(ORG_DIR, "0-org-monkey-island-modified.xml");

    private static final File ROLE_END_USER_FILE = new File(ROLES_DIR, "role-end-user.xml");
    private static String roleEndUserOid;
    private static final File RULE_K10_FILE = new File(RULES_DIR, "k10.xml");
    private static String ruleK10_oid;
    private static final File RULE_K11_FILE = new File(RULES_DIR, "k11.xml");
    private static String ruleK11_oid;
    private static final File RULE_K20_FILE = new File(RULES_DIR, "k20.xml");
    private static String ruleK20_oid;
    private static final File RULE_K21_FILE = new File(RULES_DIR, "k21.xml");
    private static String ruleK21_oid;
    private static final File RULE_K23_FILE = new File(RULES_DIR, "k23.xml");
    private static String ruleK23_oid;
    private static final File RULE_K10_TPU_10_FILE = new File(RULES_DIR, "k10-tpu-10.xml");
    private static String ruleK10_tpu_10_oid;
    private static final File RULE_K10_TPU_10_REM_ELAINE_FILE = new File(RULES_DIR, "k10-tpu-10-rem-elaine.xml");
    private static String ruleK10_tpu_10_rem_elaine_oid;
    private static final File RULE_K10_CC_1900_REM_ADMINISTRATOR_FILE = new File(RULES_DIR, "k10-cc-1900-rem-administrator.xml");
    private static String ruleK10_cc_1900_rem_administrator_oid;
    private static final File RULE_K11_TPU_10_REM_ELAINE_FILE = new File(RULES_DIR, "k11-tpu-10-rem-elaine.xml");
    private static String ruleK11_tpu_10_rem_elaine_oid;
    private static final File RULE_K20_IT_1_REM_ELAINE_FILE = new File(RULES_DIR, "k20-it-1-rem-elaine.xml");
    private static String ruleK20_it_1_rem_elaine_oid;
    private static final File RULE_K21_IT_1_REM_ELAINE_FILE = new File(RULES_DIR, "k21-it-1-rem-elaine.xml");
    private static String ruleK21_it_1_rem_elaine_oid;
    private static final File RULE_K23_REM_ELAINE_FILE = new File(RULES_DIR, "k23-rem-elaine.xml");
    private static String ruleK23_rem_elaine_oid;

    private static final File LIBRARY_FILE = new File(RULES_DIR, "library.xml");

    private static final File ROLE_IT_1_FILE = new File(ROLES_DIR, "role-it-1-for-tpu-10-elaine.xml");
    private static String roleIt1Oid;
    private static final File ROLE_IT_2_FILE = new File(ROLES_DIR, "role-it-2-for-tpu-10.xml");
    private static String roleIt2Oid;
    private static final File ROLE_IT_3_FILE = new File(ROLES_DIR, "role-it-3-for-tpu-10-and-cc-1900-elaine-administrator.xml");
    private static String roleIt3Oid;
    private static final File ROLE_IT_4_FILE = new File(ROLES_DIR, "role-it-4-not-for-tpu-10-elaine.xml");
    private static String roleIt4Oid;
    private static final File ROLE_IT_5_FILE = new File(ROLES_DIR, "role-it-5-requires-it-1-elaine.xml");
    private static String roleIt5Oid;

    private static final File USER_BARKEEPER_FILE = new File(USERS_DIR, "barkeeper.xml");
    private static String userBarkeeperOid;
    private static final File USER_BOB_FILE = new File(USERS_DIR, "bob.xml");
    private static String userBobOid;
    private static final File USER_CARLA_FILE = new File(USERS_DIR, "carla.xml");
    private static String userCarlaOid;
    private static final File USER_CHEESE_FILE = new File(USERS_DIR, "cheese.xml");
    private static String userCheeseOid;
    private static final File USER_CHEF_FILE = new File(USERS_DIR, "chef.xml");
    private static String userChefOid;
    private static final File USER_ELAINE_FILE = new File(USERS_DIR, "elaine.xml");
    private static String userElaineOid;
    private static final File USER_GUYBRUSH_FILE = new File(USERS_DIR, "guybrush.xml");
    private static String userGuybrushOid;
    private static final File USER_LECHUCK_FILE = new File(USERS_DIR, "lechuck.xml");
    private static String userLechuckOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectFromFile(LIBRARY_FILE, initResult);

//        transplantGlobalPolicyRulesAdd(CONFIG_WITH_GLOBAL_RULES_FILE, initTask, initResult);

        // we don't need these
        taskManager.suspendAndDeleteTasks(Arrays.asList(TASK_VALIDITY_SCANNER_OID, TASK_TRIGGER_SCANNER_OID), 60000L, true, initResult);

        // import of story objects
        repoAddObjectsFromFile(ORG_MONKEY_ISLAND_FILE, OrgType.class, initResult);

        roleEndUserOid = repoAddObjectFromFile(ROLE_END_USER_FILE, initResult).getOid();
        ruleK10_oid = addAndRecompute(RULE_K10_FILE, initTask, initResult);
        ruleK11_oid = addAndRecompute(RULE_K11_FILE, initTask, initResult);
        ruleK20_oid = addAndRecompute(RULE_K20_FILE, initTask, initResult);
        ruleK21_oid = addAndRecompute(RULE_K21_FILE, initTask, initResult);
        ruleK23_oid = addAndRecompute(RULE_K23_FILE, initTask, initResult);
        ruleK10_tpu_10_oid = addAndRecompute(RULE_K10_TPU_10_FILE, initTask, initResult);
        ruleK10_tpu_10_rem_elaine_oid = addAndRecompute(RULE_K10_TPU_10_REM_ELAINE_FILE, initTask, initResult);
        ruleK10_cc_1900_rem_administrator_oid = addAndRecompute(RULE_K10_CC_1900_REM_ADMINISTRATOR_FILE, initTask, initResult);
        ruleK11_tpu_10_rem_elaine_oid = addAndRecompute(RULE_K11_TPU_10_REM_ELAINE_FILE, initTask, initResult);
        ruleK20_it_1_rem_elaine_oid = addAndRecompute(RULE_K20_IT_1_REM_ELAINE_FILE, initTask, initResult);
        ruleK21_it_1_rem_elaine_oid = addAndRecompute(RULE_K21_IT_1_REM_ELAINE_FILE, initTask, initResult);
        ruleK23_rem_elaine_oid = addAndRecompute(RULE_K23_REM_ELAINE_FILE, initTask, initResult);
        roleIt1Oid = addAndRecompute(ROLE_IT_1_FILE, initTask, initResult);
        roleIt2Oid = addAndRecompute(ROLE_IT_2_FILE, initTask, initResult);
        roleIt3Oid = addAndRecompute(ROLE_IT_3_FILE, initTask, initResult);
        roleIt4Oid = addAndRecompute(ROLE_IT_4_FILE, initTask, initResult);
        roleIt5Oid = addAndRecompute(ROLE_IT_5_FILE, initTask, initResult);

        userBarkeeperOid = addAndRecomputeUser(USER_BARKEEPER_FILE, initTask, initResult);
        userBobOid = addAndRecomputeUser(USER_BOB_FILE, initTask, initResult);
        userCarlaOid = addAndRecomputeUser(USER_CARLA_FILE, initTask, initResult);
        userCheeseOid = addAndRecomputeUser(USER_CHEESE_FILE, initTask, initResult);
        userChefOid = addAndRecomputeUser(USER_CHEF_FILE, initTask, initResult);
        userElaineOid = addAndRecomputeUser(USER_ELAINE_FILE, initTask, initResult);
        userGuybrushOid = addAndRecomputeUser(USER_GUYBRUSH_FILE, initTask, initResult);
        userLechuckOid = addAndRecomputeUser(USER_LECHUCK_FILE, initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

//        setGlobalTracingOverride(createModelAndWorkflowLoggingTracingProfile());
    }

    //    @Override
//    protected TracingProfileType getTestMethodTracingProfile() {
//        return createModelAndWorkflowLoggingTracingProfile()
//                .fileNamePattern(TEST_METHOD_TRACING_FILENAME_PATTERN);
//    }

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Test
    public void test100Assign_IT_2_failure() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

        try {
            assignRole(userBobOid, roleIt2Oid, task, result);         // hard constraint
            fail("unexpected success");
        } catch (PolicyViolationException e) {
            System.out.println("Got expected exception: " + e);
        }
    }

    @Test
    public void test110Assign_IT_2_success() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

        assignRole(userBarkeeperOid, roleIt2Oid, task, result);         // hard constraint
        result.computeStatus();
        assertSuccess(result);

        assertAssignedRole(userBarkeeperOid, roleIt2Oid, task, result);
    }

    @Test
    public void test120Assign_IT_1() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

        assignRole(userBobOid, roleIt1Oid, task, result);         // approval constraint

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("work item", workItem);
        ApprovalContextType actx = ApprovalContextUtil.getApprovalContext(workItem);
        display("workflow context", actx);

        CaseType rootCase = getRootCase(result);
        display("root case", rootCase);

        workflowService.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext),
                task, result);

        waitForCaseClose(rootCase, 60000);
        assertAssignedRole(userBobOid, roleIt1Oid, task, result);
    }

    /**
     * IT-3 = k10-tpu-10-rem-elaine, k10-cc-1900-rem-administrator
     */
    @Test
    public void test130Assign_IT_3() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

        assignRole(userCarlaOid, roleIt3Oid, task, result);         // two approval constraints

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("work item", workItem);
        ApprovalContextType actx = ApprovalContextUtil.getApprovalContext(workItem);
        display("workflow context", actx);

        CaseType rootCase = getRootCase(result);
        display("root case", rootCase);

        workflowService.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext),
                task, result);

        CaseWorkItemType workItem2 = getWorkItem(task, result);
        display("work item2", workItem2);
        ApprovalContextType actx2 = ApprovalContextUtil.getApprovalContext(workItem2);
        display("workflow context2", actx2);

        workflowService.completeWorkItem(WorkItemId.of(workItem2),
                ApprovalUtils.createApproveOutput(prismContext),
                task, result);

        waitForCaseClose(rootCase, 60000);
        assertAssignedRole(userCarlaOid, roleIt3Oid, task, result);
    }

    /**
     * IT-4 = not for tpu-10 (barkeeper has TPU of 10)
     */
    @Test
    public void test140Assign_IT_4() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

        assignRole(userBarkeeperOid, roleIt4Oid, task, result);         // approval constraint

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("work item", workItem);
        ApprovalContextType actx = ApprovalContextUtil.getApprovalContext(workItem);
        display("workflow context", actx);

        CaseType rootCase = getRootCase(result);
        display("root case", rootCase);

        workflowService.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext),
                task, result);

        waitForCaseClose(rootCase, 60000);
        assertAssignedRole(userBarkeeperOid, roleIt4Oid, task, result);
    }

    /**
     * IT-5 = requires IT-1 (barkeeper does not have it)
     */
    @Test
    public void test150Assign_IT_5() throws Exception {
        Task task = getTask();
        OperationResult result = getResult();

        assignRole(userBarkeeperOid, roleIt5Oid, task, result);         // approval constraint

        CaseWorkItemType workItem = getWorkItem(task, result);
        display("work item", workItem);
        ApprovalContextType actx = ApprovalContextUtil.getApprovalContext(workItem);
        display("workflow context", actx);

        CaseType rootCase = getRootCase(result);
        display("root case", rootCase);

        workflowService.completeWorkItem(WorkItemId.of(workItem),
                ApprovalUtils.createApproveOutput(prismContext),
                task, result);

        waitForCaseClose(rootCase, 60000);
        assertAssignedRole(userBarkeeperOid, roleIt5Oid, task, result);
    }

    @NotNull
    public CaseType getRootCase(OperationResult result) throws ObjectNotFoundException, SchemaException {
        String caseOid = OperationResult.referenceToCaseOid(result.findAsynchronousOperationReference());
        assertNotNull("Case OID is not set in operation result", caseOid);
        return repositoryService.getObject(CaseType.class, caseOid, null, result).asObjectable();
    }
}
