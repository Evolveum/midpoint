/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.test;

import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.ENABLED;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Very simple certification test.
 * Tests just the basic functionality, along with security features.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-certification-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestEscalation extends AbstractCertificationTest {

    private static final File CERT_DEF_FILE = new File(COMMON_DIR, "certification-of-eroot-user-assignments-escalations.xml");
    //protected static final String CERT_DEF_OID = "399e117a-baaa-4e59-b845-21bb838cb7bc";

    private AccessCertificationDefinitionType certificationDefinition;

    private String campaignOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        certificationDefinition = repoAddObjectFromFile(CERT_DEF_FILE, AccessCertificationDefinitionType.class, initResult).asObjectable();
        importTriggerTask(initResult);
    }

    @Test
    public void test010CreateCampaign() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(certificationDefinition.getOid(), task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();

        campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign", campaign);
        assertSanityAfterCampaignCreate(campaign, certificationDefinition);
        assertPercentCompleteAll(campaign, 100, 100, 100);      // no cases, no problems
    }

    @Test
    public void test013SearchAllCases() throws Exception {
        searchWithNoCasesExpected();
    }

    @SuppressWarnings("SameParameterValue")
    private void searchWithNoCasesExpected() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, CertCampaignTypeUtil.createCasesForCampaignQuery(campaignOid, prismContext),
                null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Unexpected cases in caseList", 0, caseList.size());
    }

    @Test
    public void test021OpenFirstStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationService.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);

        assertSanityAfterCampaignStart(campaign, certificationDefinition, 7);
        checkAllCasesSanity(campaign.getCase());
        List<AccessCertificationCaseType> caseList = campaign.getCase();
        // no responses -> NO_RESPONSE in all cases
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);

        assertPercentCompleteAll(campaign, 0, 0, 0);

        assertEquals("Wrong # of triggers", 2, campaign.getTrigger().size());           // completion + timed-action
        displayDumpable("dummy transport", dummyTransport);
    }

    @Test
    public void test032SearchAllCases() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, null, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        checkAllCasesSanity(caseList);
    }

    @Test
    public void test050SearchWorkItems() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(
                        CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid, prismContext),
                        false, null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification work items", 7, workItems.size());
        checkAllWorkItemsSanity(workItems);
    }

    @Test
    public void test100RecordDecision() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        AccessCertificationCaseType superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);

        // WHEN
        when();
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(superuserCase, 1, 1, USER_ADMINISTRATOR_OID);
        long id = superuserCase.asPrismContainerValue().getId();
        certificationService.recordDecision(campaignOid, id, workItem.getId(), ACCEPT, "no comment", task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);
        checkAllCasesSanity(caseList);

        superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);
        assertEquals("changed case ID", Long.valueOf(id), superuserCase.asPrismContainerValue().getId());
        assertSingleDecision(superuserCase, ACCEPT, "no comment", 1, 1, USER_ADMINISTRATOR_OID, ACCEPT, false);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, Math.round(100.0f/7.0f), Math.round(100.0f/7.0f), Math.round(100.0f/7.0f));      // 1 reviewer per case (always administrator)
    }


    @Test
    public void test110Escalate() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyTransport.clearMessages();

        // WHEN
        when();

        clock.resetOverride();
        clock.overrideDuration("P2D");          // first escalation is at P1D
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);
        checkAllCasesSanity(caseList);

        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        display("CEO case after escalation", ceoCase);

        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(ceoCase, 1, 1, USER_ADMINISTRATOR_OID);
        assertObjectRefs("assignees", false, workItem.getAssigneeRef(), USER_JACK_OID, USER_ADMINISTRATOR_OID);
        assertEquals("Wrong originalAssignee OID", USER_ADMINISTRATOR_OID, workItem.getOriginalAssigneeRef().getOid());
        final WorkItemEscalationLevelType NEW_ESCALATION_LEVEL = new WorkItemEscalationLevelType().number(1).name("jack-level");
        assertEquals("Wrong escalation info", NEW_ESCALATION_LEVEL, workItem.getEscalationLevel());
        assertEquals("Wrong # of events", 1, ceoCase.getEvent().size());
        WorkItemEscalationEventType event = (WorkItemEscalationEventType) ceoCase.getEvent().get(0);
        assertNotNull("No timestamp in event", event.getTimestamp());
        assertEquals("Wrong initiatorRef OID", USER_ADMINISTRATOR_OID, event.getInitiatorRef().getOid());
        assertEquals("Wrong workItemId", workItem.getId(), event.getWorkItemId());
        assertObjectRefs("assigneeBefore", false, event.getAssigneeBefore(), USER_ADMINISTRATOR_OID);
        assertObjectRefs("delegatedTo", false, event.getDelegatedTo(), USER_JACK_OID);
        assertEquals("Wrong delegationMethod", WorkItemDelegationMethodType.ADD_ASSIGNEES, event.getDelegationMethod());
        assertEquals("Wrong new escalation level", NEW_ESCALATION_LEVEL, event.getNewEscalationLevel());

        AccessCertificationCaseType superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);
        AccessCertificationWorkItemType superuserWorkItem = CertCampaignTypeUtil.findWorkItem(superuserCase, 1, 1,
                USER_ADMINISTRATOR_OID);
        //noinspection SimplifiedTestNGAssertion
        assertEquals("Escalation info present even if it shouldn't be", null, superuserWorkItem.getEscalationLevel());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, Math.round(100.0f/7.0f), Math.round(100.0f/7.0f), Math.round(100.0f/7.0f));      // 1 reviewer per case (always administrator)

        AccessCertificationStageType currentStage = CertCampaignTypeUtil.getCurrentStage(campaign);
        assertNotNull(currentStage);
        assertEquals("Wrong new stage escalation level", NEW_ESCALATION_LEVEL, currentStage.getEscalationLevel());

        display("campaign after escalation", campaign);
        assertEquals("Wrong # of triggers", 2, campaign.getTrigger().size());           // completion + timed-action (P3D)

        displayDumpable("dummy transport", dummyTransport);
        List<Message> messages = dummyTransport.getMessages("dummy:simpleReviewerNotifier");
        assertEquals("Wrong # of dummy notifications", 3, messages.size());            // original + new approver + deputy of administrator
    }

    @Test
    public void test120EscalateAgain() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyTransport.clearMessages();

        // WHEN
        when();

        clock.resetOverride();
        clock.overrideDuration("P4D");          // second escalation is at P3D
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);
        checkAllCasesSanity(caseList);

        AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        display("CEO case after escalation", ceoCase);
        AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(ceoCase, 1, 1, USER_ELAINE_OID);
        assertNotNull("No work item found", workItem);
        assertObjectRefs("assignees", false, workItem.getAssigneeRef(), USER_ELAINE_OID);
        assertEquals("Wrong originalAssignee OID", USER_ADMINISTRATOR_OID, workItem.getOriginalAssigneeRef().getOid());
        final WorkItemEscalationLevelType OLD_ESCALATION_LEVEL = new WorkItemEscalationLevelType().number(1).name("jack-level");
        final WorkItemEscalationLevelType NEW_ESCALATION_LEVEL = new WorkItemEscalationLevelType().number(2).name("elaine-level");
        assertEquals("Wrong escalation info", NEW_ESCALATION_LEVEL, workItem.getEscalationLevel());
        assertEquals("Wrong # of events", 2, ceoCase.getEvent().size());
        WorkItemEscalationEventType event = (WorkItemEscalationEventType) ceoCase.getEvent().get(1);
        assertNotNull("No timestamp in event", event.getTimestamp());
        assertEquals("Wrong initiatorRef OID", USER_ADMINISTRATOR_OID, event.getInitiatorRef().getOid());
        assertEquals("Wrong workItemId", workItem.getId(), event.getWorkItemId());
        assertObjectRefs("assigneeBefore", false, event.getAssigneeBefore(), USER_ADMINISTRATOR_OID, USER_JACK_OID);
        assertObjectRefs("delegatedTo", false, event.getDelegatedTo(), USER_ELAINE_OID);
        assertEquals("Wrong delegationMethod", WorkItemDelegationMethodType.REPLACE_ASSIGNEES, event.getDelegationMethod());
        assertEquals("Wrong old escalation level", OLD_ESCALATION_LEVEL, event.getEscalationLevel());
        assertEquals("Wrong new escalation level", NEW_ESCALATION_LEVEL, event.getNewEscalationLevel());

        AccessCertificationCaseType superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);
        AccessCertificationWorkItemType superuserWorkItem = CertCampaignTypeUtil.findWorkItem(superuserCase, 1, 1,
                USER_ADMINISTRATOR_OID);
        //noinspection SimplifiedTestNGAssertion
        assertEquals("Escalation info present even if it shouldn't be", null, superuserWorkItem.getEscalationLevel());

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentCompleteAll(campaign, Math.round(100.0f/7.0f), Math.round(100.0f/7.0f), Math.round(100.0f/7.0f));      // 1 reviewer per case (always administrator)

        AccessCertificationStageType currentStage = CertCampaignTypeUtil.getCurrentStage(campaign);
        assertNotNull(currentStage);
        assertEquals("Wrong new stage escalation level", NEW_ESCALATION_LEVEL, currentStage.getEscalationLevel());

        display("campaign after escalation", campaign);
        assertEquals("Wrong # of triggers", 1, campaign.getTrigger().size());           // completion

        displayDumpable("dummy transport", dummyTransport);
        List<Message> messages = dummyTransport.getMessages("dummy:simpleReviewerNotifier");
        assertEquals("Wrong # of dummy notifications", 1, messages.size());            // new approver
    }

    @Test
    public void test130Remediation() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        dummyTransport.clearMessages();

        // WHEN
        when();

        clock.resetOverride();
        clock.overrideDuration("P15D");          // stage ends at P14D
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after remediation", campaign);
        assertStateAndStage(campaign, AccessCertificationCampaignStateType.IN_REMEDIATION, 2);
    }

    @Test
    public void test140Close() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        dummyTransport.clearMessages();

        // WHEN
        when();

        clock.resetOverride();
        clock.overrideDuration("P16D");
        certificationManager.closeCampaign(campaignOid, true, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after close", campaign);
        assertStateAndStage(campaign, AccessCertificationCampaignStateType.CLOSED, 2);
        assertEquals("Wrong # of triggers", 1, campaign.getTrigger().size());           // reiterate
    }

    @Test
    public void test200AutomaticReiteration() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        dummyTransport.clearMessages();

        // WHEN
        when();

        clock.resetOverride();
        clock.overrideDuration("P18D");          // campaign ends at P16D, reiteration scheduled to P17D
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);

        assertSanityAfterCampaignStart(campaign, certificationDefinition, 7, 2, 2, new Date(clock.currentTimeMillis()));
        List<AccessCertificationCaseType> caseList = campaign.getCase();
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, null);  // from iteration 1
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);

        // current iteration/stage:
        // - 6 cases (all except admin->super), 6 work items, no decisions
        assertPercentCompleteCurrent(campaign, 0, 0, 0);

        // current stage (all iterations):
        // - 7 cases, 7 work items; one case is complete/decided, one work item is done
        assertPercentCompleteCurrentStage(campaign, 14, 14, 14);

        // current iteration (all stages)
        // - 6 cases, 6 work items, no decisions
        assertPercentCompleteCurrentIteration(campaign, 0, 0, 0);

        // all stages, all iterations
        // - 7 cases, 1 complete, 1 decided
        // - 1 of 7 work items done
        assertPercentCompleteAll(campaign, 14, 14, 14);

        assertEquals("Wrong # of triggers", 2, campaign.getTrigger().size());           // completion + timed-action
        displayDumpable("dummy transport", dummyTransport);
    }

    @Test
    public void test300Close() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        dummyTransport.clearMessages();

        // WHEN
        when();

        clock.resetOverride();
        clock.overrideDuration("P19D");         // +1 day relative to previous test
        certificationManager.closeCampaign(campaignOid, true, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after close", campaign);
        assertStateAndStage(campaign, AccessCertificationCampaignStateType.CLOSED, 2);
        assertEquals("Wrong # of triggers", 0, campaign.getTrigger().size());           // no more automated reiterations
    }

    @Test
    public void test310ManualReiteration() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        dummyTransport.clearMessages();

        // WHEN
        when();

        clock.resetOverride();
        clock.overrideDuration("P20D");          // +1 day relative to previous test
        certificationManager.reiterateCampaign(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after reiteration", campaign);
        assertStateStageIteration(campaign, AccessCertificationCampaignStateType.CREATED, 0, 3);

        // current iteration/stage:
        // - 6 cases (all except admin->super), 0 work items
        // - so, these cases are all complete but none is decided
        // - no work items so they are all OK
        assertPercentCompleteCurrent(campaign, 100, 0, 100);

        // current stage (all iterations):
        // - 6 cases, 0 work items => the same numbers
        assertPercentCompleteCurrentStage(campaign, 100, 0, 100);

        // current iteration (all stages) -> the same
        assertPercentCompleteCurrentIteration(campaign, 100, 0, 100);

        // all stages, all iterations
        // - 7 cases, 1 complete, 1 decided
        // - 1 of 7 work items done
        assertPercentCompleteAll(campaign, 14, 14, 14);
    }

    @Test
    public void test320OpenFirstStage() throws Exception {
        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        when();
        certificationService.openNextStage(campaignOid, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);

        assertSanityAfterCampaignStart(campaign, certificationDefinition, 7, 3, 3, new Date(clock.currentTimeMillis()));
        List<AccessCertificationCaseType> caseList = campaign.getCase();
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, ACCEPT, ACCEPT, null);  // from iteration 1
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);

        // current iteration/stage:
        // - 6 cases (all except admin->super), 6 work items, no decisions
        assertPercentCompleteCurrent(campaign, 0, 0, 0);

        // current stage (all iterations):
        // - 7 cases, 7 work items; one case is complete/decided, one work item is done
        assertPercentCompleteCurrentStage(campaign, 14, 14, 14);

        // current iteration (all stages)
        // - 6 cases, 6 work items, no decisions
        assertPercentCompleteCurrentIteration(campaign, 0, 0, 0);

        // all stages, all iterations
        // - 7 cases, 1 complete, 1 decided
        // - 1 of 7 work items done
        assertPercentCompleteAll(campaign, 14, 14, 14);

        assertEquals("Wrong # of triggers", 2, campaign.getTrigger().size());           // completion + timed-action
        displayDumpable("dummy transport", dummyTransport);
    }

    @Test
    public void test400Close() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        dummyTransport.clearMessages();

        // WHEN
        when();

        clock.resetOverride();
        clock.overrideDuration("P21D");         // +1 day relative to previous test
        certificationManager.closeCampaign(campaignOid, true, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign after close", campaign);
        assertStateAndStage(campaign, AccessCertificationCampaignStateType.CLOSED, 2);
        assertEquals("Wrong # of triggers", 0, campaign.getTrigger().size());           // no more automated reiterations
    }

    @Test
    public void test410ManualReiterationUnavailable() throws Exception {
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = getTestTask();
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        dummyTransport.clearMessages();

        // WHEN
        when();

        clock.resetOverride();
        clock.overrideDuration("P22D");          // +1 day relative to previous test
        try {
            certificationManager.reiterateCampaign(campaignOid, task, result);
            fail("unexpected success");
        } catch (IllegalStateException e) {
            // THEN
            System.err.println("got expected exception: " + e.getMessage());
            e.printStackTrace();
            assertTrue("wrong exception message", e.getMessage().contains("maximum number of iterations (3) was reached"));
        }
    }

    @SuppressWarnings("Duplicates")
    private void checkAllCasesSanity(Collection<AccessCertificationCaseType> caseList) {
        assertEquals("Wrong number of certification cases", 7, caseList.size());
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator);
        checkCaseSanity(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator);
        checkCaseSanity(caseList, USER_JACK_OID, ROLE_CEO_OID, userJack, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
        checkCaseSanity(caseList, USER_JACK_OID, ORG_EROOT_OID, userJack);
    }

    @SuppressWarnings("Duplicates")
    private void checkAllWorkItemsSanity(Collection<AccessCertificationWorkItemType> workItems) {
        assertEquals("Wrong number of certification work items", 7, workItems.size());
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator);
        checkWorkItemSanity(workItems, USER_JACK_OID, ROLE_CEO_OID, userJack, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
        checkWorkItemSanity(workItems, USER_JACK_OID, ORG_EROOT_OID, userJack);
    }
}
