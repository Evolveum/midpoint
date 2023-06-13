/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.test;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.PolicyRuleTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.CLOSED;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignStateType.IN_REMEDIATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.*;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.*;

@ContextConfiguration(locations = {"classpath:ctx-certification-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSoDCertification extends AbstractCertificationTest {

    private static final File TEST_DIR = new File("src/test/resources/sod");

    private AccessCertificationDefinitionType certificationDefinition;

    private static final File SOD_CERTIFICATION_DEF_FILE = new File(TEST_DIR, "sod-certification.xml");

    private String campaignOid;

    private static final File ROLE_A_TEST_2A = new File(TEST_DIR, "a-test-2a.xml");
    private static String roleATest2aOid;
    private static final File ROLE_A_TEST_2B = new File(TEST_DIR, "a-test-2b.xml");
    private static String roleATest2bOid;
    private static final File ROLE_A_TEST_2C = new File(TEST_DIR, "a-test-2c.xml");
    private static String roleATest2cOid;
    private static final File ROLE_A_TEST_3A = new File(TEST_DIR, "a-test-3a.xml");
    private static String roleATest3aOid;
    private static final File ROLE_A_TEST_3B = new File(TEST_DIR, "a-test-3b.xml");
    private static String roleATest3bOid;
    private static final File ROLE_A_TEST_3X = new File(TEST_DIR, "a-test-3x.xml");
    private static final File ROLE_A_TEST_3Y = new File(TEST_DIR, "a-test-3y.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        roleATest2aOid = addAndRecompute(ROLE_A_TEST_2A, initTask, initResult);
        roleATest2bOid = addAndRecompute(ROLE_A_TEST_2B, initTask, initResult);
        roleATest2cOid = addAndRecompute(ROLE_A_TEST_2C, initTask, initResult);
        roleATest3aOid = addAndRecompute(ROLE_A_TEST_3A, initTask, initResult);
        roleATest3bOid = addAndRecompute(ROLE_A_TEST_3B, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3X, initTask, initResult);
        addAndRecompute(ROLE_A_TEST_3Y, initTask, initResult);

        assignOrg(USER_JACK_OID, ORG_SECURITY_TEAM_OID, initTask, initResult);

        assignRole(USER_JACK_OID, roleATest2aOid);
        assignRole(USER_JACK_OID, roleATest2bOid);
        assignRole(USER_JACK_OID, roleATest2cOid);
        assignRole(USER_JACK_OID, roleATest3aOid);
        assignRole(USER_JACK_OID, roleATest3bOid);
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        display("jack", jack);

        AssignmentType a2a = findAssignmentByTargetRequired(jack, roleATest2aOid);
        display("assignment 2a", a2a);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);
    }

    @Test
    public void test001Triggers() throws Exception {
        // GIVEN, WHEN
        PrismObject<UserType> jack = getUser(USER_JACK_OID);
        display("jack", jack);

        // THEN
        AssignmentType a2a = findAssignmentByTargetRequired(jack, roleATest2aOid);
        display("assignment 2a", a2a);
        assertTriggers(a2a, 2, 1);
        AssignmentType a2b = findAssignmentByTargetRequired(jack, roleATest2bOid);
        display("assignment 2b", a2b);
        assertTriggers(a2b, 2, 1);
        AssignmentType a2c = findAssignmentByTargetRequired(jack, roleATest2cOid);
        display("assignment 2c", a2c);
        assertTriggers(a2c, 2, 1);
        AssignmentType a3a = findAssignmentByTargetRequired(jack, roleATest3aOid);
        display("assignment 3a", a3a);
        assertTriggers(a3a, 1, 1);
        AssignmentType a3b = findAssignmentByTargetRequired(jack, roleATest3bOid);
        display("assignment 3b", a3b);
        assertTriggers(a3b, 1, 1);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertTriggers(AssignmentType assignment, int exclusionExpected, int situationExpected) {
        int exclusion = 0, situation = 0;
        for (EvaluatedPolicyRuleType rule : assignment.getTriggeredPolicyRule()) {
            for (EvaluatedPolicyRuleTriggerType trigger : rule.getTrigger()) {
                //assertNotNull("Identifier not null in base trigger: " + trigger, trigger.getTriggerId());
                if (trigger instanceof EvaluatedSituationTriggerType) {
                    EvaluatedSituationTriggerType situationTrigger = (EvaluatedSituationTriggerType) trigger;
                    int sourceTriggers = 0;
                    for (EvaluatedPolicyRuleType sourceRule : situationTrigger.getSourceRule()) {
                        for (EvaluatedPolicyRuleTriggerType ignored : sourceRule.getTrigger()) {
                            sourceTriggers++;
                            //assertNotNull("Ref not null in situation source trigger: " + sourceTrigger, sourceTrigger.getRef());
                        }
                    }
                    assertEquals("Wrong # of exclusion triggers in situation trigger", exclusionExpected, sourceTriggers);
                    situation++;
                } else if (trigger instanceof EvaluatedExclusionTriggerType) {
                    exclusion++;
                } else {
                    fail("Unexpected trigger: " + trigger);
                }
            }
        }
        assertEquals("Wrong # of exclusion triggers", 0, exclusion);
        assertEquals("Wrong # of situation triggers", situationExpected, situation);

        List<EvaluatedExclusionTriggerType> exclusionTriggers = PolicyRuleTypeUtil
                .getAllExclusionTriggers(assignment.getTriggeredPolicyRule());
        display("Exclusion triggers for " + assignment, exclusionTriggers);
        assertEquals("Wrong # of extracted exclusion triggers", exclusionExpected, exclusionTriggers.size());
    }

    @Test
    public void test010CreateCampaign() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        certificationDefinition = repoAddObjectFromFile(SOD_CERTIFICATION_DEF_FILE,
                AccessCertificationDefinitionType.class, result).asObjectable();

        // WHEN
        when();
        AccessCertificationCampaignType campaign =
                certificationManager.createCampaign(certificationDefinition.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();

        campaign = getCampaignWithCases(campaignOid);
        display("campaign", campaign);
        assertSanityAfterCampaignCreate(campaign, certificationDefinition);
        assertPercentCompleteAll(campaign, 100, 100, 100);
    }

    @Test
    public void test012SearchAllCases() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Unexpected cases in caseList", 0, caseList.size());
    }

    @Test
    public void test014Statistics() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        AccessCertificationCasesStatisticsType stat =
                certificationManager.getCampaignStatistics(campaignOid, true, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        displayDumpable("statistics", stat.asPrismContainerValue());
        assertEquals(0, stat.getMarkedAsAccept());
        assertEquals(0, stat.getMarkedAsRevoke());
        assertEquals(0, stat.getMarkedAsRevokeAndRemedied());
        assertEquals(0, stat.getMarkedAsReduce());
        assertEquals(0, stat.getMarkedAsReduceAndRemedied());
        assertEquals(0, stat.getMarkedAsNotDecide());
        assertEquals(0, stat.getWithoutResponse());
    }

    @Test
    public void test020OpenFirstStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        display("jack", getUser(USER_JACK_OID));

        // WHEN
        when();
        certificationManager.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);
        assertSanityAfterCampaignStart(campaign, certificationDefinition, 5);
        checkAllCases(campaign.getCase());

        List<AccessCertificationCaseType> caseList = campaign.getCase();
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest2aOid, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest2bOid, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest2cOid, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest3aOid, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest3bOid, ACCEPT, ACCEPT, null);
        assertPercentCompleteAll(campaign, 0, 100, 0);     // preliminary outcomes for all cases are "ACCEPT"
    }

    private void checkAllCases(Collection<AccessCertificationCaseType> caseList)
            throws ConfigurationException, ObjectNotFoundException, SchemaException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        assertEquals("Wrong number of certification cases", 5, caseList.size());
        UserType jack = getUser(USER_JACK_OID).asObjectable();
        checkCaseSanity(caseList, USER_JACK_OID, roleATest2aOid, jack);
        checkCaseSanity(caseList, USER_JACK_OID, roleATest2bOid, jack);
        checkCaseSanity(caseList, USER_JACK_OID, roleATest2cOid, jack);
        checkCaseSanity(caseList, USER_JACK_OID, roleATest3aOid, jack);
        checkCaseSanity(caseList, USER_JACK_OID, roleATest3bOid, jack);
    }

    @Test
    public void test030SearchAllCases() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        checkAllCases(caseList);
        UserType jack = getUser(USER_JACK_OID).asObjectable();
        AccessCertificationCaseType _case = checkCaseSanity(caseList, USER_JACK_OID, roleATest2aOid, jack);
        assertEquals("Unexpected number of reviewers in a-test-2a case", 1, CertCampaignTypeUtil.getCurrentReviewers(_case).size());
    }

    @Test
    public void test100RecordDecisions() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);

        // WHEN
        when();

        AccessCertificationCaseType test2aCase = findCase(caseList, USER_JACK_OID, roleATest2aOid);
        AccessCertificationCaseType test2bCase = findCase(caseList, USER_JACK_OID, roleATest2bOid);
        AccessCertificationCaseType test2cCase = findCase(caseList, USER_JACK_OID, roleATest2cOid);
        AccessCertificationCaseType test3aCase = findCase(caseList, USER_JACK_OID, roleATest3aOid);
        AccessCertificationCaseType test3bCase = findCase(caseList, USER_JACK_OID, roleATest3bOid);

        recordDecision(campaignOid, test2aCase, REVOKE, "no way", USER_JACK_OID, task, result);
        recordDecision(campaignOid, test2bCase, ACCEPT, null, USER_JACK_OID, task, result);
        recordDecision(campaignOid, test2cCase, ACCEPT, null, USER_JACK_OID, task, result);
        recordDecision(campaignOid, test3aCase, ACCEPT, "OK", USER_JACK_OID, task, result);
        recordDecision(campaignOid, test3bCase, NOT_DECIDED, "dunno", USER_JACK_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, result);
        displayContainerablesCollection("caseList", caseList);
        checkAllCases(caseList);

        test2aCase = findCase(caseList, USER_JACK_OID, roleATest2aOid);
        test2bCase = findCase(caseList, USER_JACK_OID, roleATest2bOid);
        test2cCase = findCase(caseList, USER_JACK_OID, roleATest2cOid);
        test3aCase = findCase(caseList, USER_JACK_OID, roleATest3aOid);
        test3bCase = findCase(caseList, USER_JACK_OID, roleATest3bOid);

        assertSingleDecision(test2aCase, REVOKE, "no way", 1, 1, USER_JACK_OID, REVOKE, false);
        assertSingleDecision(test2bCase, ACCEPT, null, 1, 1, USER_JACK_OID, ACCEPT, false);
        assertSingleDecision(test2cCase, ACCEPT, null, 1, 1, USER_JACK_OID, ACCEPT, false);
        assertSingleDecision(test3aCase, ACCEPT, "OK", 1, 1, USER_JACK_OID, ACCEPT, false);
        assertSingleDecision(test3bCase, NOT_DECIDED, "dunno", 1, 1, USER_JACK_OID, ACCEPT, false);

        assertCaseOutcome(caseList, USER_JACK_OID, roleATest2aOid, REVOKE, REVOKE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest2bOid, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest2cOid, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest3aOid, ACCEPT, ACCEPT, null);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest3bOid, ACCEPT, ACCEPT, null);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, 100, 100, 100);
    }

    @Test
    public void test150CloseFirstStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationManager.closeCurrentStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);
        assertSanityAfterStageClose(campaign, certificationDefinition, 1);
        checkAllCases(campaign.getCase());

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        AccessCertificationCaseType test2aCase = findCase(caseList, USER_JACK_OID, roleATest2aOid);
        AccessCertificationCaseType test2bCase = findCase(caseList, USER_JACK_OID, roleATest2bOid);
        AccessCertificationCaseType test2cCase = findCase(caseList, USER_JACK_OID, roleATest2cOid);
        AccessCertificationCaseType test3aCase = findCase(caseList, USER_JACK_OID, roleATest3aOid);
        AccessCertificationCaseType test3bCase = findCase(caseList, USER_JACK_OID, roleATest3bOid);

        assertSingleDecision(test2aCase, REVOKE, "no way", 1, 1, USER_JACK_OID, REVOKE, true);
        assertSingleDecision(test2bCase, ACCEPT, null, 1, 1, USER_JACK_OID, ACCEPT, true);
        assertSingleDecision(test2cCase, ACCEPT, null, 1, 1, USER_JACK_OID, ACCEPT, true);
        assertSingleDecision(test3aCase, ACCEPT, "OK", 1, 1, USER_JACK_OID, ACCEPT, true);
        assertSingleDecision(test3bCase, NOT_DECIDED, "dunno", 1, 1, USER_JACK_OID, ACCEPT, true);

        assertCaseOutcome(caseList, USER_JACK_OID, roleATest2aOid, REVOKE, REVOKE, 1);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest2bOid, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest2cOid, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest3aOid, ACCEPT, ACCEPT, 1);
        assertCaseOutcome(caseList, USER_JACK_OID, roleATest3bOid, ACCEPT, ACCEPT, 1);

        assertPercentCompleteAll(campaign, 100, 100, 100);
    }

    @Test
    public void test200StartRemediation() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationManager.startRemediation(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertInProgressOrSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after remediation start", campaign);
        assertTrue("wrong campaign state: " + campaign.getState(), campaign.getState() == CLOSED || campaign.getState() == IN_REMEDIATION);

        ObjectQuery query = prismContext.queryFor(TaskType.class)
                .item(TaskType.F_OBJECT_REF).ref(campaign.getOid())
                .build();
        List<PrismObject<TaskType>> tasks = taskManager.searchObjects(TaskType.class, query, null, result);
        assertEquals("unexpected number of related tasks", 1, tasks.size());
        waitForTaskFinish(tasks.get(0).getOid(), true);

        campaign = getCampaignWithCases(campaignOid);
        display("campaign after remediation finished", campaign);
        assertEquals("wrong campaign state", CLOSED, campaign.getState());
        assertEquals("wrong campaign stage", 2, campaign.getStageNumber());
        assertDefinitionAndOwner(campaign, certificationDefinition);
        assertApproximateTime("end time", new Date(), campaign.getEndTimestamp());
        assertEquals("wrong # of stages", 1, campaign.getStage().size());

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, result);
        assertEquals("wrong # of cases", 5, caseList.size());
        AccessCertificationCaseType test2aCase = findCase(caseList, USER_JACK_OID, roleATest2aOid);
        assertApproximateTime("test2aCase.remediedTimestamp", new Date(), test2aCase.getRemediedTimestamp());

        userJack = getUser(USER_JACK_OID).asObjectable();
        display("userJack after remediation", userJack);
        assertNotAssignedRole(userJack.asPrismObject(), roleATest2aOid);

        PrismObject<AccessCertificationDefinitionType> def = getObject(AccessCertificationDefinitionType.class, certificationDefinition.getOid());
        assertApproximateTime("last campaign closed", new Date(), def.asObjectable().getLastCampaignClosedTimestamp());

        assertPercentCompleteAll(campaign, 100, 100, 100);
    }

    @Test
    public void test210CheckAfterClose() throws Exception {
        login(userAdministrator.asPrismObject());

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        waitForCampaignTasks(campaignOid, 20000, result);

        // THEN
        userJack = getUser(USER_JACK_OID).asObjectable();
        display("jack", userJack);
        assertCertificationMetadata(findAssignmentByTargetRequired(userJack.asPrismObject(), roleATest2bOid).getMetadata(),
                SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT, singleton(USER_JACK_OID), emptySet());
        assertCertificationMetadata(findAssignmentByTargetRequired(userJack.asPrismObject(), roleATest2cOid).getMetadata(),
                SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT, singleton(USER_JACK_OID), emptySet());
        assertCertificationMetadata(findAssignmentByTargetRequired(userJack.asPrismObject(), roleATest3aOid).getMetadata(),
                SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT, singleton(USER_JACK_OID), singleton("jack: OK"));
        assertCertificationMetadata(findAssignmentByTargetRequired(userJack.asPrismObject(), roleATest3bOid).getMetadata(),
                SchemaConstants.MODEL_CERTIFICATION_OUTCOME_ACCEPT, singleton(USER_JACK_OID), singleton("jack: dunno"));
    }

}
