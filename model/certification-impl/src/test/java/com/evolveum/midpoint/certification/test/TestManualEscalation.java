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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
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
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.ACCEPT;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType.NO_RESPONSE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType.ENABLED;
import static org.testng.AssertJUnit.*;

/**
 * Very simple certification test.
 * Tests just the basic functionality, along with security features.
 *
 * @author mederly
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestManualEscalation extends AbstractCertificationTest {

    protected static final File CERT_DEF_USER_ASSIGNMENT_BASIC_FILE = new File(COMMON_DIR, "certification-of-eroot-user-assignments.xml");
    protected static final String CERT_DEF_USER_ASSIGNMENT_BASIC_OID = "33333333-0000-0000-0000-000000000001";

    protected AccessCertificationDefinitionType certificationDefinition;

    private String campaignOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

		DebugUtil.setPrettyPrintBeansAs(PrismContext.LANG_YAML);

        certificationDefinition = repoAddObjectFromFile(CERT_DEF_USER_ASSIGNMENT_BASIC_FILE,
                AccessCertificationDefinitionType.class, initResult).asObjectable();

        // to test MID-3838
		assignFocus(UserType.class, USER_JACK_OID, UserType.COMPLEX_TYPE, USER_GUYBRUSH_OID, SchemaConstants.ORG_DEPUTY, null, initTask, initResult);
    }

    @Test
    public void test010CreateCampaign() throws Exception {
        final String TEST_NAME = "test010CreateCampaign";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestManualEscalation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_OID));

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
    public void test013SearchAllCasesAllowed() throws Exception {
        final String TEST_NAME = "test013SearchAllCasesAllowed";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_BOB_OID));

        searchWithNoCasesExpected(TEST_NAME);
    }

    protected void searchWithNoCasesExpected(String TEST_NAME) throws Exception {
        // GIVEN
        Task task = taskManager.createTaskInstance(TestManualEscalation.class.getName() + "." + TEST_NAME);
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
    public void test021OpenFirstStageAllowed() throws Exception {
        final String TEST_NAME = "test021OpenFirstStageAllowed";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestManualEscalation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        login(getUserFromRepo(USER_BOB_OID));

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
    }

    @Test
    public void test032SearchAllCasesAllowed() throws Exception {
        final String TEST_NAME = "test032SearchAllCasesAllowed";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_BOB_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestManualEscalation.class.getName() + "." + TEST_NAME);
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
    public void test050SearchWorkItemsAsAdministrator() throws Exception {
        final String TEST_NAME = "test050SearchWorkItemsAsAdministrator";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestManualEscalation.class.getName() + "." + TEST_NAME);
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
        login(getUserFromRepo(USER_ADMINISTRATOR_OID));

        // GIVEN
        Task task = taskManager.createTaskInstance(TestManualEscalation.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestManualEscalation.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		EscalateWorkItemActionType action = new EscalateWorkItemActionType()
				.approverRef(USER_JACK_OID, UserType.COMPLEX_TYPE)
				.delegationMethod(WorkItemDelegationMethodType.ADD_ASSIGNEES)
				.escalationLevelName("ESC-1");
        updateHelper.escalateCampaign(campaignOid, action, null, task, result);

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
		final WorkItemEscalationLevelType NEW_ESCALATION_LEVEL = new WorkItemEscalationLevelType().number(1).name("ESC-1");
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

		AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
        assertPercentComplete(campaign, Math.round(100.0f/7.0f), Math.round(100.0f/7.0f), Math.round(100.0f/7.0f));
    }

	@Test
	public void test120EscalateAgain() throws Exception {
		final String TEST_NAME = "test120EscalateAgain";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(getUserFromRepo(USER_ADMINISTRATOR_OID));

		// GIVEN
		Task task = taskManager.createTaskInstance(TestManualEscalation.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		EscalateWorkItemActionType action = new EscalateWorkItemActionType()
				.approverRef(USER_ELAINE_OID, UserType.COMPLEX_TYPE)
				.escalationLevelName("ESC-2");
		updateHelper.escalateCampaign(campaignOid, action, null, task, result);

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
		final WorkItemEscalationLevelType OLD_ESCALATION_LEVEL = new WorkItemEscalationLevelType().number(1).name("ESC-1");
		final WorkItemEscalationLevelType NEW_ESCALATION_LEVEL = new WorkItemEscalationLevelType().number(2).name("ESC-2");
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

		AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
		assertPercentComplete(campaign, Math.round(100.0f/7.0f), Math.round(100.0f/7.0f), Math.round(100.0f/7.0f));
	}

	@Test
	public void test130Delegate() throws Exception {
		final String TEST_NAME = "test130Delegate";
		TestUtil.displayTestTile(this, TEST_NAME);
		login(getUserFromRepo(USER_ADMINISTRATOR_OID));

		// GIVEN
		Task task = taskManager.createTaskInstance(TestManualEscalation.class.getName() + "." + TEST_NAME);
		OperationResult result = task.getResult();

		List<AccessCertificationCaseType> caseList = queryHelper.searchCases(campaignOid, null, null, result);
		AccessCertificationCaseType ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
		display("CEO case", ceoCase);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		AccessCertificationWorkItemType workItem = CertCampaignTypeUtil.findWorkItem(ceoCase, 1, USER_ELAINE_OID);
		long id = ceoCase.asPrismContainerValue().getId();
		DelegateWorkItemActionType action = new DelegateWorkItemActionType()
				.approverRef(USER_ADMINISTRATOR_OID, UserType.COMPLEX_TYPE)
				.approverRef(USER_JACK_OID, UserType.COMPLEX_TYPE);
		certificationManager.delegateWorkItems(campaignOid, Collections.singletonList(workItem), action, task, result);

		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		caseList = queryHelper.searchCases(campaignOid, null, null, result);
		display("caseList", caseList);
		checkAllCases(caseList, campaignOid);

		ceoCase = findCase(caseList, USER_JACK_OID, ROLE_CEO_OID);
		display("CEO case after escalation", ceoCase);
		assertEquals("changed case ID", Long.valueOf(id), ceoCase.asPrismContainerValue().getId());
		workItem = CertCampaignTypeUtil.findWorkItem(ceoCase, 1, USER_ADMINISTRATOR_OID);
		assertNotNull("No work item found", workItem);
		assertObjectRefs("assignees", false, workItem.getAssigneeRef(), USER_ADMINISTRATOR_OID, USER_JACK_OID);
		assertEquals("Wrong originalAssignee OID", USER_ADMINISTRATOR_OID, workItem.getOriginalAssigneeRef().getOid());
		final WorkItemEscalationLevelType OLD_ESCALATION_LEVEL = new WorkItemEscalationLevelType().number(2).name("ESC-2");
		assertEquals("Wrong escalation info", OLD_ESCALATION_LEVEL, workItem.getEscalationLevel());
		assertEquals("Wrong # of events", 3, ceoCase.getEvent().size());
		WorkItemDelegationEventType event = (WorkItemDelegationEventType) ceoCase.getEvent().get(2);
		assertFalse("Event is Escalation one, although it shouldn't", event instanceof WorkItemEscalationEventType);
		assertNotNull("No timestamp in event", event.getTimestamp());
		assertEquals("Wrong initiatorRef OID", USER_ADMINISTRATOR_OID, event.getInitiatorRef().getOid());
		assertEquals("Wrong workItemId", workItem.getId(), event.getWorkItemId());
		assertObjectRefs("assigneeBefore", false, event.getAssigneeBefore(), USER_ELAINE_OID);
		assertObjectRefs("delegatedTo", false, event.getDelegatedTo(), USER_ADMINISTRATOR_OID, USER_JACK_OID);
		assertEquals("Wrong delegationMethod", WorkItemDelegationMethodType.REPLACE_ASSIGNEES, event.getDelegationMethod());
		assertEquals("Wrong old escalation level", OLD_ESCALATION_LEVEL, event.getEscalationLevel());

		AccessCertificationCampaignType campaign = getCampaignWithCases(campaignOid);
		assertPercentComplete(campaign, Math.round(100.0f/7.0f), Math.round(100.0f/7.0f), Math.round(100.0f/7.0f));
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
