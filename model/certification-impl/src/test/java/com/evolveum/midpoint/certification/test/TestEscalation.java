/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.ENABLED;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * Very simple certification test.
 * Tests just the basic functionality, along with security features.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-certification-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestEscalation extends AbstractCertificationTest {

    protected static final File CERT_DEF_FILE = new File(COMMON_DIR, "certification-of-eroot-user-assignments-escalations.xml");
    protected static final String CERT_DEF_OID = "399e117a-baaa-4e59-b845-21bb838cb7bc";

    protected AccessCertificationDefinitionType certificationDefinition;

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
        final String TEST_NAME = "test010CreateCampaign";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEscalation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        AccessCertificationCampaignType campaign =
                certificationService.createCampaign(certificationDefinition.getOid(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNotNull("Created campaign is null", campaign);

        campaignOid = campaign.getOid();

        campaign = getObject(AccessCertificationCampaignType.class, campaignOid).asObjectable();
        display("campaign", campaign);
        assertAfterCampaignCreate(campaign, certificationDefinition);
        assertPercentComplete(campaign, 100, 100, 100);      // no cases, no problems
	}

    @Test
    public void test013SearchAllCases() throws Exception {
        final String TEST_NAME = "test013SearchAllCases";
        TestUtil.displayTestTile(this, TEST_NAME);

        searchWithNoCasesExpected(TEST_NAME);
    }

    protected void searchWithNoCasesExpected(String TEST_NAME) throws Exception {
        // GIVEN
        Task task = taskManager.createTaskInstance(TestEscalation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, CertCampaignTypeUtil.createCasesForCampaignQuery(campaignOid, prismContext),
                null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        assertEquals("Unexpected cases in caseList", 0, caseList.size());
    }

    @Test
    public void test021OpenFirstStage() throws Exception {
        final String TEST_NAME = "test021OpenFirstStage";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEscalation.class.getName() + "." + TEST_NAME);
        task.setOwner(userAdministrator.asPrismObject());
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        certificationService.openNextStage(campaignOid, 1, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        display("campaign in stage 1", campaign);

        assertAfterCampaignStart(campaign, certificationDefinition, 7);
        checkAllCases(campaign.getCase(), campaignOid);
        List<AccessCertificationCaseType> caseList = campaign.getCase();
        // no responses -> NO_RESPONSE in all cases
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ROLE_CEO_OID, NO_RESPONSE, NO_RESPONSE, null);
        assertCaseOutcome(caseList, USER_JACK_OID, ORG_EROOT_OID, NO_RESPONSE, NO_RESPONSE, null);

        assertPercentComplete(campaign, 0, 0, 0);

        assertEquals("Wrong # of triggers", 2, campaign.getTrigger().size());           // completion + timed-action
		display("dummy transport", dummyTransport);
    }

    @Test
    public void test032SearchAllCases() throws Exception {
        final String TEST_NAME = "test032SearchAllCases";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEscalation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationCaseType> caseList = modelService.searchContainers(
                AccessCertificationCaseType.class, null, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("caseList", caseList);
        checkAllCases(caseList, campaignOid);
    }

    @Test
    public void test050SearchWorkItems() throws Exception {
        final String TEST_NAME = "test050SearchWorkItems";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEscalation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        List<AccessCertificationWorkItemType> workItems =
                certificationService.searchOpenWorkItems(
                        CertCampaignTypeUtil.createWorkItemsForCampaignQuery(campaignOid, prismContext),
                        false, null, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        display("workItems", workItems);
        assertEquals("Wrong number of certification work items", 7, workItems.size());
        checkAllWorkItems(workItems, campaignOid);
    }

	@Test
	public void test100RecordDecision() throws Exception {
		final String TEST_NAME = "test100RecordDecision";
		TestUtil.displayTestTile(this, TEST_NAME);

		// GIVEN
		Task task = taskManager.createTaskInstance(TestCertificationBasic.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
		AccessCertificationCaseType superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(superuserCase, 1, USER_ADMINISTRATOR_OID);
		long id = superuserCase.asPrismContainerValue().getId();
		certificationService.recordDecision(campaignOid, id, workItem.getId(), ACCEPT, "no comment", task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		caseList = queryHelper.searchCases(campaignOid, null, null, result);
		display("caseList", caseList);
		checkAllCases(caseList, campaignOid);

		superuserCase = findCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID);
		assertEquals("changed case ID", Long.valueOf(id), superuserCase.asPrismContainerValue().getId());
		assertSingleDecision(superuserCase, ACCEPT, "no comment", 1, USER_ADMINISTRATOR_OID, ACCEPT, false);

		AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
		assertPercentComplete(campaign, Math.round(100.0f/7.0f), Math.round(100.0f/7.0f), Math.round(100.0f/7.0f));      // 1 reviewer per case (always administrator)
	}


	@Test
    public void test110Escalate() throws Exception {
        final String TEST_NAME = "test110Escalate";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEscalation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

		dummyTransport.clearMessages();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        clock.resetOverride();
        clock.overrideDuration("P2D");          // first escalation is at P1D
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

		List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
        display("caseList", caseList);
        checkAllCases(caseList, campaignOid);

		AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
        display("CEO case after escalation", ceoCase);

		AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(ceoCase, 1, USER_ADMINISTRATOR_OID);
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
		AccessCertificationWorkItemType superuserWorkItem = CertCampaignTypeUtil.findWorkItem(superuserCase, 1, USER_ADMINISTRATOR_OID);
		assertEquals("Escalation info present even if it shouldn't be", null, superuserWorkItem.getEscalationLevel());

		AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
		assertPercentComplete(campaign, Math.round(100.0f/7.0f), Math.round(100.0f/7.0f), Math.round(100.0f/7.0f));      // 1 reviewer per case (always administrator)

		AccessCertificationStageType currentStage = CertCampaignTypeUtil.getCurrentStage(campaign);
		assertEquals("Wrong new stage escalation level", NEW_ESCALATION_LEVEL, currentStage.getEscalationLevel());

		display("campaign after escalation", campaign);
        assertEquals("Wrong # of triggers", 2, campaign.getTrigger().size());           // completion + timed-action (P3D)

		display("dummy transport", dummyTransport);
		List<Message> messages = dummyTransport.getMessages("dummy:simpleReviewerNotifier");
		assertEquals("Wrong # of dummy notifications", 3, messages.size());			// original + new approver + deputy of administrator
	}

	@Test
	public void test120EscalateAgain() throws Exception {
		final String TEST_NAME = "test120EscalateAgain";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(getUserFromRepo(USER_ADMINISTRATOR_OID));

		// GIVEN
		Task task = taskManager.createTaskInstance(TestEscalation.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		dummyTransport.clearMessages();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

        clock.resetOverride();
        clock.overrideDuration("P4D");          // second escalation is at P3D
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
		display("caseList", caseList);
		checkAllCases(caseList, campaignOid);

		AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
		display("CEO case after escalation", ceoCase);
		AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(ceoCase, 1, USER_ELAINE_OID);
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
		AccessCertificationWorkItemType superuserWorkItem = CertCampaignTypeUtil.findWorkItem(superuserCase, 1, USER_ADMINISTRATOR_OID);
		assertEquals("Escalation info present even if it shouldn't be", null, superuserWorkItem.getEscalationLevel());

		AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
		assertPercentComplete(campaign, Math.round(100.0f/7.0f), Math.round(100.0f/7.0f), Math.round(100.0f/7.0f));      // 1 reviewer per case (always administrator)

		AccessCertificationStageType currentStage = CertCampaignTypeUtil.getCurrentStage(campaign);
		assertEquals("Wrong new stage escalation level", NEW_ESCALATION_LEVEL, currentStage.getEscalationLevel());

		display("campaign after escalation", campaign);
        assertEquals("Wrong # of triggers", 1, campaign.getTrigger().size());           // completion

		display("dummy transport", dummyTransport);
		List<Message> messages = dummyTransport.getMessages("dummy:simpleReviewerNotifier");
		assertEquals("Wrong # of dummy notifications", 1, messages.size());			// new approver
    }

	@Test
	public void test130Remediation() throws Exception {
		final String TEST_NAME = "test130Remediation";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(getUserFromRepo(USER_ADMINISTRATOR_OID));

		// GIVEN
		Task task = taskManager.createTaskInstance(TestEscalation.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		dummyTransport.clearMessages();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

        clock.resetOverride();
        clock.overrideDuration("P15D");          // stage ends at P14D
        waitForTaskNextRun(TASK_TRIGGER_SCANNER_OID, true, 20000, true);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
		display("campaign after escalation", campaign);
		assertStateAndStage(campaign, AccessCertificationCampaignStateType.IN_REMEDIATION, 2);
    }

	protected void checkAllCases(Collection<AccessCertificationCaseType> caseList, String campaignOid) {
        assertEquals("Wrong number of certification cases", 7, caseList.size());
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator, campaignOid);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator, campaignOid);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator, campaignOid);
        checkCase(caseList, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator, campaignOid);
        checkCase(caseList, USER_JACK_OID, ROLE_CEO_OID, userJack, campaignOid, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
        checkCase(caseList, USER_JACK_OID, ORG_EROOT_OID, userJack, campaignOid);
    }

    protected void checkAllWorkItems(Collection<AccessCertificationWorkItemType> workItems, String campaignOid) {
        assertEquals("Wrong number of certification work items", 7, workItems.size());
        checkWorkItem(workItems, USER_ADMINISTRATOR_OID, ROLE_SUPERUSER_OID, userAdministrator, campaignOid);
        checkWorkItem(workItems, USER_ADMINISTRATOR_OID, ROLE_COO_OID, userAdministrator, campaignOid);
        checkWorkItem(workItems, USER_ADMINISTRATOR_OID, ROLE_CEO_OID, userAdministrator, campaignOid);
        checkWorkItem(workItems, USER_ADMINISTRATOR_OID, ORG_EROOT_OID, userAdministrator, campaignOid);
        checkWorkItem(workItems, USER_JACK_OID, ROLE_CEO_OID, userJack, campaignOid, ORG_GOVERNOR_OFFICE_OID, ORG_SCUMM_BAR_OID, ENABLED);
        checkWorkItem(workItems, USER_JACK_OID, ORG_EROOT_OID, userJack, campaignOid);
    }


}
